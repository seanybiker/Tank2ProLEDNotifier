package com.example.tank2prolednotifier

import android.util.Log
import kotlinx.coroutines.*
import java.io.DataOutputStream

object LedController {
    
    private val ledRedPath = "/sys/bus/platform/drivers/redblue_leddev/red_led_duty"
    private val ledGreenPath = "/sys/bus/platform/drivers/redblue_leddev/green_led_duty"
    private val ledBluePath = "/sys/bus/platform/drivers/redblue_leddev/blue_led_duty"
    
    private var flashingJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    fun flashForNotification(type: String) {
        // Cancel any existing flashing
        flashingJob?.cancel()
        
        flashingJob = scope.launch {
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
                        // Red flash for calls (continuous until stopped)
                        while (isActive) {
                            setLed(255, 0, 0)
                            delay(500)
                            setLed(0, 0, 0)
                            delay(500)
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
                Log.e("LedController", "Failed to flash LED for $type", e)
            }
        }
    }
    
    fun stopFlashing() {
        flashingJob?.cancel()
        setLed(0, 0, 0)
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
                Log.w("LedController", "LED command returned non-zero exit code: $exitCode")
            }
        } catch (e: Exception) {
            Log.e("LedController", "Failed to set LED colors (R:$red, G:$green, B:$blue)", e)
        }
    }
}