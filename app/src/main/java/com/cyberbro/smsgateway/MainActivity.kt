package com.cyberbro.smsgateway

import android.Manifest
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.cyberbro.smsgateway.ui.DashboardFragment
import com.cyberbro.smsgateway.ui.DeviceStatusFragment
import com.cyberbro.smsgateway.ui.SmsLogsFragment
import com.cyberbro.smsgateway.ui.SettingsFragment

class MainActivity : AppCompatActivity() {

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { grantResults ->
        if (grantResults[Manifest.permission.SEND_SMS] == true) {
            Toast.makeText(this, "SEND_SMS permission granted", Toast.LENGTH_SHORT).show()
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            grantResults[Manifest.permission.POST_NOTIFICATIONS] == true) {
            Toast.makeText(this, "Notifications permission granted", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        requestPermissionsIfNeeded()

        findViewById<BottomNavigationView>(R.id.bottomNavigation).setOnItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_home     -> openFragment(DashboardFragment())
                R.id.nav_device   -> openFragment(DeviceStatusFragment())
                R.id.nav_messages -> openFragment(SmsLogsFragment())
                R.id.nav_settings -> openFragment(SettingsFragment())
                else -> return@setOnItemSelectedListener false
            }
            true
        }

        if (savedInstanceState == null) {
            findViewById<BottomNavigationView>(R.id.bottomNavigation).selectedItemId = R.id.nav_home
        }
    }

    private fun requestPermissionsIfNeeded() {
        val permissions = mutableListOf(
            Manifest.permission.SEND_SMS,
            Manifest.permission.RECEIVE_SMS,
            Manifest.permission.READ_SMS,
            Manifest.permission.READ_PHONE_STATE
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions += Manifest.permission.POST_NOTIFICATIONS
        }
        permissionLauncher.launch(permissions.toTypedArray())
    }

    private fun openFragment(fragment: androidx.fragment.app.Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
            .commit()
    }
}
