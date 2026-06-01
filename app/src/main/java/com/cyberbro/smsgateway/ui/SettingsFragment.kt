package com.cyberbro.smsgateway.ui

import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.Switch
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.cyberbro.smsgateway.R
import com.cyberbro.smsgateway.api.SmsGatewayApi
import com.cyberbro.smsgateway.security.ApiKeyStore
import kotlinx.coroutines.launch

class SettingsFragment : Fragment() {
    private val settingsName = "cyberbro_settings"

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_settings, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val apiKeyText = view.findViewById<TextView>(R.id.settingsApiKeyText)
        val versionText = view.findViewById<TextView>(R.id.settingsVersionText)
        val portText = view.findViewById<TextView>(R.id.settingsPortText)
        val generateButton = view.findViewById<Button>(R.id.generateApiKeyButton)
        val apiKeyLockButton = view.findViewById<android.widget.ImageButton>(R.id.apiKeyLockButton)
        val sendTestButton = view.findViewById<Button>(R.id.sendTestSmsButton)
        val phoneInput = view.findViewById<EditText>(R.id.testPhoneInput)
        val messageInput = view.findViewById<EditText>(R.id.testMessageInput)
        
        portText.text = com.cyberbro.smsgateway.api.KtorServer.getPort().toString()
        val simSlotSpinner = view.findViewById<Spinner>(R.id.simSlotSpinner)
        val autoStartSwitch = view.findViewById<Switch>(R.id.autoStartSwitch)
        val darkModeSwitch = view.findViewById<Switch>(R.id.darkModeSwitch)
        val notificationsSwitch = view.findViewById<Switch>(R.id.notificationsSwitch)

        val prefs = requireContext().getSharedPreferences(settingsName, Context.MODE_PRIVATE)
        autoStartSwitch.isChecked = prefs.getBoolean("auto_start", false)
        darkModeSwitch.isChecked = prefs.getBoolean("dark_mode", false)
        notificationsSwitch.isChecked = prefs.getBoolean("notifications", true)

        val apiKey = ApiKeyStore(requireContext()).getApiKey()
        apiKeyText.text = apiKey.ifBlank { getString(R.string.settings_no_api_key) }

        val locked = prefs.getBoolean("api_key_locked", false)
        fun updateLockUI(isLocked: Boolean) {
            apiKeyLockButton.setImageResource(if (isLocked) android.R.drawable.ic_lock_idle_lock else android.R.drawable.ic_lock_lock)
            generateButton.isEnabled = !isLocked
        }
        updateLockUI(locked)

        val packageName = requireContext().packageName
        val versionName = try {
            requireContext().packageManager.getPackageInfo(packageName, 0).versionName
        } catch (exception: PackageManager.NameNotFoundException) {
            "unknown"
        }
        versionText.text = getString(R.string.settings_version_value, versionName)

        generateButton.setOnClickListener {
            val isLocked = prefs.getBoolean("api_key_locked", false)
            if (isLocked) {
                Toast.makeText(requireContext(), "API Key is locked", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val newKey = java.util.UUID.randomUUID().toString().replace("-", "")
            ApiKeyStore(requireContext()).setApiKey(newKey)
            apiKeyText.text = newKey
            Toast.makeText(requireContext(), "API key regenerated", Toast.LENGTH_SHORT).show()
        }

        apiKeyLockButton.setOnClickListener {
            val currently = prefs.getBoolean("api_key_locked", false)
            prefs.edit().putBoolean("api_key_locked", !currently).apply()
            updateLockUI(!currently)
            Toast.makeText(requireContext(), if (!currently) "API Key locked" else "API Key unlocked", Toast.LENGTH_SHORT).show()
        }

        val simOptions = listOf("Auto", "SIM 1", "SIM 2")
        simSlotSpinner.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, simOptions).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }

        sendTestButton.setOnClickListener {
            val phone = phoneInput.text.toString().trim()
            val body = messageInput.text.toString().trim()
            if (phone.isBlank() || body.isBlank()) {
                Toast.makeText(requireContext(), "Enter phone and message", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val selectedSlot = when (simSlotSpinner.selectedItemPosition) {
                1 -> 1
                2 -> 2
                else -> 1
            }

            lifecycleScope.launch {
                SmsGatewayApi.sendSms(
                    com.cyberbro.smsgateway.api.SmsRequest(
                        phone    = phone,
                        message  = body,
                        sim      = selectedSlot,
                        priority = "high"
                    ),
                    requireContext()
                )
                Toast.makeText(requireContext(), "Test SMS queued", Toast.LENGTH_SHORT).show()
            }
        }

        autoStartSwitch.setOnCheckedChangeListener { _, checked ->
            prefs.edit().putBoolean("auto_start", checked).apply()
        }
        darkModeSwitch.setOnCheckedChangeListener { _, checked ->
            prefs.edit().putBoolean("dark_mode", checked).apply()
        }
        notificationsSwitch.setOnCheckedChangeListener { _, checked ->
            prefs.edit().putBoolean("notifications", checked).apply()
        }
    }
}
