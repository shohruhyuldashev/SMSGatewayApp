package com.cyberbro.smsgateway.api

import android.content.Context
import android.os.Build
import com.cyberbro.smsgateway.device.BatteryMonitor
import com.cyberbro.smsgateway.device.DeviceInfo
import com.cyberbro.smsgateway.device.LocalNetworkInfo
import com.cyberbro.smsgateway.device.SimController
import com.cyberbro.smsgateway.security.ApiKeyStore
import com.cyberbro.smsgateway.storage.SmsDatabase
import com.cyberbro.smsgateway.storage.entities.SmsTaskEntity
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.util.UUID
import java.util.concurrent.TimeUnit
import io.ktor.server.plugins.ratelimit.*
import io.ktor.server.plugins.statuspages.*
import kotlin.time.Duration.Companion.seconds

// ─── In-memory webhook store (reset on restart) ───────────────────────────────
private val webhookUrls = mutableListOf<String>()

// ─── Startup timestamp ────────────────────────────────────────────────────────
private val startTimeMs = System.currentTimeMillis()

object KtorServer {
    private var server: ApplicationEngine? = null
    private var secure = false

    fun start(context: Context) {
        if (server != null) return

        val keyStoreFile = findKeyStore(context)
        try {
            val password = "changeit".toCharArray()
            val keyStoreType = if (keyStoreFile.extension.equals("p12", true)) "PKCS12" else "JKS"
            val keyStore = java.security.KeyStore.getInstance(keyStoreType)
            java.io.FileInputStream(keyStoreFile).use { keyStore.load(it, password) }
            val alias = keyStore.aliases().toList().firstOrNull() ?: "cyberbro"
            secure = true
            server = embeddedServer(Netty, applicationEngineEnvironment {
                module { routingModule(context) }
                sslConnector(
                    keyStore = keyStore,
                    keyAlias = alias,
                    keyStorePassword = { password },
                    privateKeyPassword = { password }
                ) {
                    port = 8443
                    host = "0.0.0.0"
                }
            })
        } catch (t: Throwable) {
            android.util.Log.e("KtorServer", "TLS setup failed, falling back to HTTP", t)
            secure = false
            server = embeddedServer(Netty, port = 8080) { routingModule(context) }
        }
        server?.start(wait = false)
    }

    fun stop() {
        server?.stop(1000, 2000, TimeUnit.MILLISECONDS)
        server = null
        secure = false
    }

    fun isRunning(): Boolean = server != null
    fun isSecure(): Boolean = secure
    fun getPort(): Int = if (secure) 8443 else 8080
    fun getUptimeSeconds(): Long = (System.currentTimeMillis() - startTimeMs) / 1000

    private fun findKeyStore(context: Context): java.io.File {
        val p12 = java.io.File(context.filesDir, "keystore.p12")
        if (!p12.exists()) {
            try {
                context.assets.open("keystore.p12").use { inputStream ->
                    java.io.FileOutputStream(p12).use { outputStream ->
                        val buffer = ByteArray(4096)
                        var read: Int
                        while (inputStream.read(buffer).also { read = it } != -1) {
                            outputStream.write(buffer, 0, read)
                        }
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("KtorServer", "Failed to copy keystore from assets", e)
            }
        }
        return p12
    }

    private fun Application.routingModule(context: Context) {
        install(RateLimit) {
            global {
                rateLimiter(limit = 60, refillPeriod = 60.seconds)
            }
        }
        
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true; prettyPrint = false })
        }

        // Global exception handler — never return raw stack traces
        install(StatusPages) {
            exception<Throwable> { call, cause ->
                android.util.Log.e("KtorServer", "Unhandled exception on ${call.request.uri}", cause)
                call.respond(HttpStatusCode.InternalServerError, ErrorResponse(cause.message ?: "internal error"))
            }
        }

        routing {
            // ── Public ──────────────────────────────────────────────────────
            get("/health") {
                call.respond(HealthResponse("ok", "2.0", getUptimeSeconds()))
            }

            // ── Auth intercept ───────────────────────────────────────────────
            intercept(ApplicationCallPipeline.Plugins) {
                val path = call.request.uri.substringBefore("?")
                if (path == "/health") return@intercept
                if (!AuthenticationMiddleware.authorize(call, context)) {
                    call.respond(HttpStatusCode.Unauthorized, ErrorResponse("invalid api key"))
                    finish()
                }
            }

            // ── SMS ──────────────────────────────────────────────────────────
            post("/send-sms") {
                val req = call.receive<SmsRequest>()
                SmsGatewayApi.sendSms(req, context)
                call.respond(HttpStatusCode.Accepted, AcceptedResponse(req.id, "queued"))
            }

            get("/messages") {
                val page  = call.parameters["page"]?.toIntOrNull()?.coerceAtLeast(1) ?: 1
                val limit = call.parameters["limit"]?.toIntOrNull()?.coerceIn(1, 200) ?: 50
                val status = call.parameters["status"]
                val dao = SmsDatabase.getInstance(context).smsTaskDao()
                val tasks = if (status != null) {
                    dao.getTasksByStatus(status)
                } else {
                    dao.getTasksPaged(limit, (page - 1) * limit)
                }
                call.respond(tasks.map { it.toResponse() })
            }

            get("/messages/{id}") {
                val id   = call.parameters["id"]
                val task = id?.let { SmsDatabase.getInstance(context).smsTaskDao().getTaskById(it) }
                if (task == null) {
                    call.respond(HttpStatusCode.NotFound, ErrorResponse("message not found"))
                    return@get
                }
                call.respond(task.toResponse())
            }

            delete("/messages/{id}") {
                val id = call.parameters["id"]
                if (id.isNullOrBlank()) {
                    call.respond(HttpStatusCode.BadRequest, ErrorResponse("id missing"))
                    return@delete
                }
                val deleted = SmsDatabase.getInstance(context).smsTaskDao().deleteTaskById(id)
                if (deleted == 0) {
                    call.respond(HttpStatusCode.NotFound, ErrorResponse("message not found"))
                    return@delete
                }
                call.respond(StatusMessageResponse(status = "deleted", messageId = id))
            }

            // ── Stats ────────────────────────────────────────────────────────
            get("/stats") {
                call.respond(buildStats(context, sinceMs = 0L))
            }

            get("/stats/today") {
                val midnight = todayMidnightMs()
                call.respond(buildStats(context, sinceMs = midnight))
            }

            get("/stats/week") {
                val weekAgo = System.currentTimeMillis() - 7 * 24 * 3600_000L
                call.respond(buildStats(context, sinceMs = weekAgo))
            }

            get("/stats/month") {
                val monthAgo = System.currentTimeMillis() - 30 * 24 * 3600_000L
                call.respond(buildStats(context, sinceMs = monthAgo))
            }

            // ── SIM management ───────────────────────────────────────────────
            get("/sims") {
                val sim = SimController(context)
                val sims = sim.getSimSlots()
                call.respond(sims)
            }

            post("/sim/default") {
                val req = call.receive<SimDefaultRequest>()
                SimController(context).setDefaultSlot(req.slot)
                call.respond(StatusMessageResponse(status = "ok", slot = req.slot))
            }

            // ── System ───────────────────────────────────────────────────────
            get("/system") {
                val deviceInfo    = DeviceInfo(context)
                val batteryMonitor = BatteryMonitor(context)
                val networkInfo   = LocalNetworkInfo(context)
                val simController = SimController(context)
                val actManager = context.getSystemService(Context.ACTIVITY_SERVICE) as android.app.ActivityManager
                val memInfo = android.app.ActivityManager.MemoryInfo()
                actManager.getMemoryInfo(memInfo)
                val totalMb = memInfo.totalMem / 1_048_576
                val availMb = memInfo.availMem / 1_048_576
                val usedMb  = totalMb - availMb
                call.respond(SystemInfoResponse(
                    device        = deviceInfo.getDeviceSummary(),
                    android       = Build.VERSION.RELEASE,
                    battery       = batteryMonitor.getBatteryLevelInt(),
                    localIp       = networkInfo.getLocalIpAddress(),
                    networkType   = networkInfo.getConnectionType(),
                    simCarrier    = simController.getCarrierName(),
                    simState      = simController.getSimState(),
                    ramUsed       = "${usedMb}MB",
                    ramTotal      = "${totalMb}MB",
                    uptime        = getUptimeSeconds(),
                    https         = isSecure()
                ))
            }

            // ── Security ─────────────────────────────────────────────────────
            get("/security") {
                call.respond(SecurityResponse(
                    https = isSecure(),
                    rateLimit = 60,
                    ipWhitelist = false,
                    auditLogging = true
                ))
            }

            get("/apikey") {
                val key = ApiKeyStore(context).getApiKey()
                call.respond(ApiKeyResponse(key))
            }

            post("/apikey/regenerate") {
                val newKey = UUID.randomUUID().toString().replace("-", "")
                ApiKeyStore(context).setApiKey(newKey)
                call.respond(ApiKeyResponse(newKey))
            }

            // ── Logs ──────────────────────────────────────────────────────────
            get("/logs") {
                val dao = SmsDatabase.getInstance(context).smsTaskDao()
                val tasks = dao.getAllTasks().take(100)
                val fmt = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault())
                call.respond(tasks.map {
                    LogEntry(
                        time = fmt.format(java.util.Date(it.createdAt)),
                        event = "SMS_${it.status.uppercase()}",
                        phone = it.phoneNumber,
                        id = it.id
                    )
                })
            }

            get("/logs/errors") {
                val dao = SmsDatabase.getInstance(context).smsTaskDao()
                val tasks = dao.getTasksByStatus("failed")
                val fmt = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault())
                call.respond(tasks.map {
                    LogEntry(
                        time = fmt.format(java.util.Date(it.createdAt)),
                        event = "SMS_FAILED",
                        phone = it.phoneNumber,
                        id = it.id
                    )
                })
            }

            get("/logs/security") {
                val fmt = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault())
                call.respond(listOf(
                    LogEntry(time = fmt.format(java.util.Date()), event = "SERVER_STARTED")
                ))
            }

            // ── Incoming SMS (stub – populated by BroadcastReceiver) ──────────
            get("/incoming") {
                call.respond(emptyList<Map<String, String>>())
            }

            // ── Webhooks ─────────────────────────────────────────────────────
            post("/webhooks") {
                val req = call.receive<WebhookRequest>()
                if (req.url.isNotBlank() && !webhookUrls.contains(req.url)) {
                    webhookUrls.add(req.url)
                }
                call.respond(WebhookRegisteredResponse(status = "registered", url = req.url, total = webhookUrls.size))
            }

            get("/webhooks") {
                call.respond(webhookUrls)
            }

            // ── Queue management ─────────────────────────────────────────────
            delete("/queue") {
                val cleared = SmsDatabase.getInstance(context).smsTaskDao().clearQueue()
                call.respond(StatusMessageResponse(status = "ok", cleared = cleared))
            }
        }
    }

    private suspend fun buildStats(context: Context, sinceMs: Long): StatsResponse {
        val dao = SmsDatabase.getInstance(context).smsTaskDao()
        return if (sinceMs == 0L) {
            StatsResponse(
                queued    = dao.getCountByStatus("queued"),
                sending   = dao.getCountByStatus("sending"),
                sent      = dao.getCountByStatus("sent"),
                delivered = dao.getCountByStatus("delivered"),
                failed    = dao.getCountByStatus("failed"),
                retry     = dao.getCountByStatus("retry")
            )
        } else {
            StatsResponse(
                queued    = dao.getCountByStatusSince("queued", sinceMs),
                sending   = dao.getCountByStatusSince("sending", sinceMs),
                sent      = dao.getCountByStatusSince("sent", sinceMs),
                delivered = dao.getCountByStatusSince("delivered", sinceMs),
                failed    = dao.getCountByStatusSince("failed", sinceMs),
                retry     = dao.getCountByStatusSince("retry", sinceMs)
            )
        }
    }

    private fun todayMidnightMs(): Long {
        val cal = java.util.Calendar.getInstance()
        cal.set(java.util.Calendar.HOUR_OF_DAY, 0)
        cal.set(java.util.Calendar.MINUTE, 0)
        cal.set(java.util.Calendar.SECOND, 0)
        cal.set(java.util.Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }
}

// ── Request / Response data classes ──────────────────────────────────────────

@Serializable
data class SmsRequest(
    val id: String = UUID.randomUUID().toString(),
    val phone: String,
    val message: String,
    val sim: Int = 1,
    val priority: String = "normal"
)

@Serializable
data class SimDefaultRequest(val slot: Int)

@Serializable
data class WebhookRequest(val url: String)

@Serializable
data class StatsResponse(
    val queued: Int,
    val sending: Int,
    val sent: Int,
    val delivered: Int,
    val failed: Int,
    val retry: Int
)

@Serializable
data class MessageResponse(
    val id: String,
    val phoneNumber: String,
    val message: String,
    val status: String,
    val priority: String,
    val simSlot: Int,
    val createdAt: Long,
    val updatedAt: Long
)

@Serializable
data class SystemInfoResponse(
    val device: String,
    val android: String,
    val battery: Int,
    val localIp: String,
    val networkType: String,
    val simCarrier: String,
    val simState: String,
    val ramUsed: String,
    val ramTotal: String,
    val uptime: Long,
    val https: Boolean
)

@Serializable
data class HealthResponse(val status: String, val version: String, val uptime: Long)

@Serializable
data class ErrorResponse(val error: String)

@Serializable
data class AcceptedResponse(val id: String, val status: String)

@Serializable
data class StatusMessageResponse(val status: String, val messageId: String? = null, val slot: Int? = null, val cleared: Int? = null)

@Serializable
data class SecurityResponse(val https: Boolean, val rateLimit: Int, val ipWhitelist: Boolean, val auditLogging: Boolean)

@Serializable
data class ApiKeyResponse(val apiKey: String)

@Serializable
data class WebhookRegisteredResponse(val status: String, val url: String, val total: Int)

@Serializable
data class LogEntry(val time: String, val event: String, val phone: String? = null, val id: String? = null)

// ── Extension ────────────────────────────────────────────────────────────────

private fun SmsTaskEntity.toResponse() = MessageResponse(
    id          = id,
    phoneNumber = phoneNumber,
    message     = message,
    status      = status,
    priority    = priority,
    simSlot     = simSlot,
    createdAt   = createdAt,
    updatedAt   = updatedAt
)
