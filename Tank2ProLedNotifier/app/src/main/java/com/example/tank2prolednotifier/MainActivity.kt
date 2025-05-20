package com.example.tank2prolednotifier

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.*
import java.io.DataOutputStream

class MainActivity : AppCompatActivity() {

    private val ledRedPath = "/sys/bus/platform/drivers/redblue_leddev/red_led_duty"
    private val ledGreenPath = "/sys/bus/platform/drivers/redblue_leddev/green_led_duty"
    private val ledBluePath = "/sys/bus/platform/drivers/redblue_leddev/blue_led_duty"

    private var flashingJob: Job? = null
    private val REQUEST_PERMISSIONS = 123

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val btnStart = findViewById<Button>(R.id.btnStart)
        val btnStop = findViewById<Button>(R.id.btnStop)
        val btnTestNotification = findViewById<Button>(R.id.btnTestNotification)

        btnStart.setOnClickListener { 
            if (testRootAccess()) {
                startFlashing()
            } else {
                showToast("Root access required for LED control")
            }
        }
        btnStop.setOnClickListener { stopFlashing() }
        btnTestNotification.setOnClickListener { testLedSequence() }

        checkPermissions()
        checkNotificationListenerPermission()
    }

    private fun checkPermissions() {
        val needed = arrayOf(
            Manifest.permission.READ_SMS,
            Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.RECEIVE_SMS,
            Manifest.permission.READ_CALL_LOG
        )
        val missing = needed.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        if (missing.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, missing.toTypedArray(), REQUEST_PERMISSIONS)
        }
    }

    private fun checkNotificationListenerPermission() {
        val enabledListeners = Settings.Secure.getString(
            contentResolver,
            "enabled_notification_listeners"
        )
        val packageName = packageName
        
        if (enabledListeners == null || !enabledListeners.contains(packageName)) {
            AlertDialog.Builder(this)
                .setTitle("Notification Access Required")
                .setMessage("This app needs notification access to detect WhatsApp messages and other notifications.")
                .setPositiveButton("Open Settings") { _, _ ->
                    startActivity(Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS"))
                }
                .setNegativeButton("Cancel", null)
                .show()
        }
    }

    private fun testRootAccess(): Boolean {
        return try {
            val process = Runtime.getRuntime().exec("su -c 'echo test'")
            process.waitFor() == 0
        } catch (e: Exception) {
            Log.e("RootTest", "Root access test failed", e)
            false
        }
    }

    private fun startFlashing() {
        if (flashingJob?.isActive == true) return

        flashingJob = lifecycleScope.launch(Dispatchers.IO) {
            try {
                while (isActive) {
                    setLed(255, 0, 0)  // red
                    delay(300)
                    setLed(0, 255, 0)  // green
                    delay(300)
                    setLed(0, 0, 255)  // blue
                    delay(300)
                    setLed(0, 0, 0)    // off
                    delay(300)
                }
            } catch (e: Exception) {
                Log.e("LEDFlashing", "Error during flashing", e)
                withContext(Dispatchers.Main) {
                    showToast("LED flashing stopped due to error")
                }
            }
        }
        showToast("LED flashing started")
    }

    private fun stopFlashing() {
        flashingJob?.cancel()
        setLed(0, 0, 0)
        showToast("LED flashing stopped")
    }

    private fun testLedSequence() {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                // Test sequence: Red -> Green -> Blue -> White -> Off
                setLed(255, 0, 0)
                delay(500)
                setLed(0, 255, 0)
                delay(500)
                setLed(0, 0, 255)
                delay(500)
                setLed(255, 255, 255)
                delay(500)
                setLed(0, 0, 0)
                
                withContext(Dispatchers.Main) {
                    showToast("LED test sequence completed")
                }
            } catch (e: Exception) {
                Log.e("LEDTest", "LED test failed", e)
                withContext(Dispatchers.Main) {
                    showToast("LED test failed - check root access")
                }
            }
        }
    }

    private fun setLed(red: Int, green: Int, blue: Int) {
        try {
            // Validate input values
            val r = red.coerceIn(0, 255)
            val g = green.coerceIn(0, 255)
            val b = blue.coerceIn(0, 255)
            
            val process = Runtime.getRuntime().exec("su")
            val os = DataOutputStream(process.outputStream)
            os.writeBytes("echo $r > $ledRedPath\n")
            os.writeBytes("echo $g > $ledGreenPath\n")
            os.writeBytes("echo $b > $ledBluePath\n")
            os.writeBytes("exit\n")
            os.flush()
            
            val exitCode = process.waitFor()
            if (exitCode != 0) {
                Log.w("LEDControl", "LED command returned non-zero exit code: $exitCode")
            }
        } catch (e: Exception) {
            Log.e("LEDControl", "Failed to set LED colors (R:$red, G:$green, B:$blue)", e)
            throw e
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_PERMISSIONS) {
            val deniedPermissions = permissions.filterIndexed { index, _ ->
                grantResults[index] != PackageManager.PERMISSION_GRANTED
            }
            if (deniedPermissions.isNotEmpty()) {
                showToast("Some permissions were denied: ${deniedPermissions.joinToString()}")
            } else {
                showToast("All permissions granted!")
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        stopFlashing()
    }

    // Public method to be called by notification services
    fun triggerNotificationFlash(type: String) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                when (type.lowercase()) {
                    "whatsapp" -> {
                        // Green flash for WhatsApp
                        repeat(3) {
                            setLed(0, 255, 0)
                            delay(200)
                            setLed(0, 0, 0)
                            delay(200)
                        }
                    }
                    "sms" -> {
                        // Blue flash for SMS
                        repeat(3) {
                            setLed(0, 0, 255)
                            delay(200)
                            setLed(0, 0, 0)
                            delay(200)
                        }
                    }
                    "call" -> {
                        // Red flash for calls
                        repeat(5) {
                            setLed(255, 0, 0)
                            delay(300)
                            setLed(0, 0, 0)
                            delay(300)
                        }
                    }
                    else -> {
                        // Default white flash
                        repeat(2) {
                            setLed(255, 255, 255)
                            delay(250)
                            setLed(0, 0, 0)
                            delay(250)
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("NotificationFlash", "Failed to flash LED for $type", e)
            }
        }
    }
}