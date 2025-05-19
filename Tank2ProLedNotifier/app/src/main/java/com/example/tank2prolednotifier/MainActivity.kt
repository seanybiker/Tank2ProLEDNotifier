package com.example.tank2proledapp

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
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

        btnStart.setOnClickListener { startFlashing() }
        btnStop.setOnClickListener { stopFlashing() }

        checkPermissions()
    }

    private fun checkPermissions() {
        val needed = arrayOf(Manifest.permission.READ_SMS, Manifest.permission.READ_PHONE_STATE)
        val missing = needed.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        if (missing.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, missing.toTypedArray(), REQUEST_PERMISSIONS)
        }
    }

    private fun startFlashing() {
        if (flashingJob?.isActive == true) return

        flashingJob = CoroutineScope(Dispatchers.IO).launch {
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
        }
    }

    private fun stopFlashing() {
        flashingJob?.cancel()
        setLed(0, 0, 0)
    }

    private fun setLed(red: Int, green: Int, blue: Int) {
        try {
            val process = Runtime.getRuntime().exec("su")
            val os = DataOutputStream(process.outputStream)
            os.writeBytes("echo $red > $ledRedPath\n")
            os.writeBytes("echo $green > $ledGreenPath\n")
            os.writeBytes("echo $blue > $ledBluePath\n")
            os.writeBytes("exit\n")
            os.flush()
            process.waitFor()
        } catch (e: Exception) {
            Log.e("LEDControl", "Failed to set LED colors", e)
        }
    }

    // TODO: Add notification listener code here for WhatsApp, SMS, call to trigger flashing automatically
}
