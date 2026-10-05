package com.smsremote

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Telephony
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.smsremote.ui.theme.SmsRemoteTheme

class MainActivity : ComponentActivity() {

    private val requiredPermissions = mutableListOf(
        Manifest.permission.SEND_SMS,
        Manifest.permission.READ_SMS,
        Manifest.permission.RECEIVE_SMS,
        Manifest.permission.READ_PHONE_STATE,
        Manifest.permission.READ_PHONE_NUMBERS,
        Manifest.permission.POST_NOTIFICATIONS
    )

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions.values.all { it }) {
            startSmsService()
        } else {
            Toast.makeText(this, "需要短信权限才能正常使用", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            SmsRemoteTheme {
                val context = LocalContext.current
                var isServiceRunning by remember { mutableStateOf(false) }

                MainScreen(
                    isServiceRunning = isServiceRunning,
                    onStartService = {
                        startSmsService()
                        isServiceRunning = true
                    },
                    onRequestPermissions = { requestPermissions() },
                    onSetDefaultSmsApp = { setDefaultSmsApp() }
                )
            }
        }

        checkPermissions()
    }

    private fun checkPermissions() {
        val missing = requiredPermissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (missing.isEmpty()) {
            startSmsService()
        } else {
            permissionLauncher.launch(missing.toTypedArray())
        }
    }

    private fun requestPermissions() {
        permissionLauncher.launch(requiredPermissions.toTypedArray())
    }

    private fun startSmsService() {
        val serviceIntent = Intent(this, SmsService::class.java)
        ContextCompat.startForegroundService(this, serviceIntent)
    }

    private fun setDefaultSmsApp() {
        if (isDefaultSmsApp()) return

        val intent = Intent(Telephony.Sms.Intents.ACTION_CHANGE_DEFAULT).apply {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                putExtra(Telephony.Sms.Intents.EXTRA_PACKAGE_NAME, packageName)
            } else {
                @Suppress("DEPRECATION")
                putExtra(Telephony.Sms.Intents.EXTRA_PACKAGE_NAME, packageName)
            }
        }

        try {
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "无法打开默认短信应用设置", Toast.LENGTH_LONG).show()
        }
    }

    private fun isDefaultSmsApp(): Boolean {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                Telephony.Sms.getDefaultSmsPackage(this) == packageName
            } else {
                @Suppress("DEPRECATION")
                Telephony.Sms.getDefaultSmsPackage(this) == packageName
            }
        } catch (e: Exception) {
            false
        }
    }
}
