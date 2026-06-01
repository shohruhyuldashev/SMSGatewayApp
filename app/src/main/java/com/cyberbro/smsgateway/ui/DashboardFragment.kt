package com.cyberbro.smsgateway.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.cyberbro.smsgateway.R
import com.cyberbro.smsgateway.api.KtorServer
import com.cyberbro.smsgateway.device.BatteryMonitor
import com.cyberbro.smsgateway.device.DeviceInfo
import com.cyberbro.smsgateway.device.LocalNetworkInfo
import com.cyberbro.smsgateway.device.SimController
import com.cyberbro.smsgateway.security.ApiKeyStore
import com.cyberbro.smsgateway.service.ForegroundSmsService
import com.cyberbro.smsgateway.storage.SmsDatabase
import kotlinx.coroutines.launch
import java.util.UUID

class DashboardFragment : Fragment() {

    private lateinit var serverStatusText: TextView
    private lateinit var serviceStatusText: TextView
    private lateinit var serverStatusDot: View
    private lateinit var serviceStatusDot: View
    private lateinit var apiKeyText: TextView
    private lateinit var apiLockStatusText: TextView
    private lateinit var toggleApiKeyLockButton: ImageButton
    private lateinit var apiUrlText: TextView
    private lateinit var localIpText: TextView
    private lateinit var serverPortText: TextView
    private lateinit var queueCountText: TextView
    private lateinit var sentTodayText: TextView
    private lateinit var failedTodayText: TextView
    private lateinit var uptimeText: TextView
    private lateinit var deviceSummaryText: TextView
    private lateinit var networkTypeText: TextView
    private lateinit var batteryText: TextView
    private lateinit var simStatusText: TextView
    private lateinit var toggleServiceButton: Button

    private val refreshHandler = Handler(Looper.getMainLooper())
    private val refreshRunnable = object : Runnable {
        override fun run() {
            if (isAdded) {
                updateDashboard()
                refreshHandler.postDelayed(this, 2000)
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.fragment_dashboard, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        serverStatusText       = view.findViewById(R.id.serverStatusText)
        serviceStatusText      = view.findViewById(R.id.serviceStatusText)
        serverStatusDot        = view.findViewById(R.id.serverStatusDot)
        serviceStatusDot       = view.findViewById(R.id.serviceStatusDot)
        apiKeyText             = view.findViewById(R.id.apiKeyText)
        apiLockStatusText      = view.findViewById(R.id.apiLockStatusText)
        toggleApiKeyLockButton = view.findViewById(R.id.toggleApiKeyLockButton)
        apiUrlText             = view.findViewById(R.id.apiUrlText)
        localIpText            = view.findViewById(R.id.localIpText)
        serverPortText         = view.findViewById(R.id.serverPortText)
        queueCountText         = view.findViewById(R.id.queueCountText)
        sentTodayText          = view.findViewById(R.id.sentTodayText)
        failedTodayText        = view.findViewById(R.id.failedTodayText)
        uptimeText             = view.findViewById(R.id.uptimeText)
        deviceSummaryText      = view.findViewById(R.id.deviceSummaryText)
        networkTypeText        = view.findViewById(R.id.networkTypeText)
        batteryText            = view.findViewById(R.id.batteryText)
        simStatusText          = view.findViewById(R.id.simStatusText)
        toggleServiceButton    = view.findViewById(R.id.toggleServiceButton)

        // ── Service toggle (Start / Stop in one button) ──────────────────────
        toggleServiceButton.setOnClickListener {
            val isRunning = ForegroundSmsService.isRunning(requireContext())
            if (isRunning) {
                requireContext().stopService(
                    Intent(requireContext(), ForegroundSmsService::class.java)
                )
                // Wait a moment before refreshing so the service has time to stop
                refreshHandler.postDelayed({ updateDashboard() }, 500)
            } else {
                ContextCompat.startForegroundService(
                    requireContext(),
                    Intent(requireContext(), ForegroundSmsService::class.java)
                )
                updateDashboard()
            }
        }

        // ── Regenerate API Key ───────────────────────────────────────────────
        view.findViewById<Button>(R.id.generateApiKeyButton).setOnClickListener {
            val prefs = requireContext().getSharedPreferences("cyberbro_settings", Context.MODE_PRIVATE)
            if (prefs.getBoolean("api_key_locked", false)) {
                Toast.makeText(requireContext(), "API Key is locked", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val newKey = UUID.randomUUID().toString().replace("-", "")
            ApiKeyStore(requireContext()).setApiKey(newKey)
            apiKeyText.text = newKey
            Toast.makeText(requireContext(), "New API key generated", Toast.LENGTH_SHORT).show()
        }

        // ── API Key lock toggle ──────────────────────────────────────────────
        toggleApiKeyLockButton.setOnClickListener {
            val prefs = requireContext().getSharedPreferences("cyberbro_settings", Context.MODE_PRIVATE)
            val currentlyLocked = prefs.getBoolean("api_key_locked", false)
            prefs.edit().putBoolean("api_key_locked", !currentlyLocked).apply()
            updateApiLockUI(!currentlyLocked)
            Toast.makeText(
                requireContext(),
                if (!currentlyLocked) "API Key locked" else "API Key unlocked",
                Toast.LENGTH_SHORT
            ).show()
        }

        // ── Copy buttons ─────────────────────────────────────────────────────
        view.findViewById<Button>(R.id.copyApiUrlButton).setOnClickListener {
            copyToClipboard("API URL", apiUrlText.text.toString())
        }
        view.findViewById<Button>(R.id.copyApiKeyButton).setOnClickListener {
            copyToClipboard("API Key", apiKeyText.text.toString())
        }

        updateDashboard()
    }

    override fun onResume() {
        super.onResume()
        refreshHandler.post(refreshRunnable)
    }

    override fun onPause() {
        super.onPause()
        refreshHandler.removeCallbacks(refreshRunnable)
    }

    // ── Dashboard refresh ────────────────────────────────────────────────────

    private fun updateDashboard() {
        if (!isAdded) return
        val ctx = requireContext()

        val serverRunning  = KtorServer.isRunning()
        val serviceRunning = ForegroundSmsService.isRunning(ctx)

        serverStatusText.text = if (serverRunning) "Online" else "Offline"
        serverStatusDot.setBackgroundResource(
            if (serverRunning) R.drawable.dot_green else R.drawable.dot_red
        )

        serviceStatusText.text = if (serviceRunning) "Running" else "Stopped"
        serviceStatusDot.setBackgroundResource(
            if (serviceRunning) R.drawable.dot_green else R.drawable.dot_red
        )

        // Toggle button: reflects current state
        if (serviceRunning) {
            toggleServiceButton.text = "Stop Service"
            toggleServiceButton.setTextColor(android.graphics.Color.parseColor("#F87171"))
            toggleServiceButton.setBackgroundResource(R.drawable.btn_stop_bg)
        } else {
            toggleServiceButton.text = "Start Service"
            toggleServiceButton.setTextColor(android.graphics.Color.WHITE)
            toggleServiceButton.setBackgroundResource(R.drawable.btn_start_bg)
        }

        val prefs = ctx.getSharedPreferences("cyberbro_settings", Context.MODE_PRIVATE)
        apiKeyText.text = ApiKeyStore(ctx).getApiKey()
        updateApiLockUI(prefs.getBoolean("api_key_locked", false))

        val networkInfo = LocalNetworkInfo(ctx)
        localIpText.text    = networkInfo.getLocalIpAddress()
        apiUrlText.text     = networkInfo.getApiEndpoint(KtorServer.getPort(), KtorServer.isSecure())
        networkTypeText.text = networkInfo.getConnectionType()
        serverPortText.text  = KtorServer.getPort().toString()
        batteryText.text     = BatteryMonitor(ctx).getBatteryStatus()
        simStatusText.text   = SimController(ctx).getCarrierName()
        uptimeText.text      = getUptimeText()
        deviceSummaryText.text = DeviceInfo(ctx).getDeviceSummary()

        lifecycleScope.launch {
            val dao = SmsDatabase.getInstance(ctx).smsTaskDao()
            queueCountText.text  = dao.getPendingTaskCount().toString()
            sentTodayText.text   = dao.getSentTaskCount().toString()
            failedTodayText.text = dao.getFailedTaskCount().toString()
        }
    }

    private fun updateApiLockUI(isLocked: Boolean) {
        apiLockStatusText.text = if (isLocked) "Locked" else "Unlocked"
        toggleApiKeyLockButton.setImageResource(
            if (isLocked) android.R.drawable.ic_lock_idle_lock else android.R.drawable.ic_lock_lock
        )
    }

    private fun copyToClipboard(label: String, text: String) {
        val clipboard = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText(label, text))
        Toast.makeText(requireContext(), "$label copied", Toast.LENGTH_SHORT).show()
    }

    private fun getUptimeText(): String {
        val s = android.os.SystemClock.elapsedRealtime() / 1000
        return String.format("%dh %02dm", s / 3600, (s % 3600) / 60)
    }
}
