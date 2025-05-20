package com.example.tank2prolednotifier

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.telephony.TelephonyManager
import android.util.Log

class PhoneStateReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent?.action == TelephonyManager.ACTION_PHONE_STATE_CHANGED) {
            try {
                val state = intent.getStringExtra(TelephonyManager.EXTRA_STATE)
                val incomingNumber = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER)
                
                when (state) {
                    TelephonyManager.EXTRA_STATE_RINGING -> {
                        Log.d("PhoneStateReceiver", "Incoming call from: $incomingNumber")
                        LedController.flashForNotification("call")
                    }
                    TelephonyManager.EXTRA_STATE_OFFHOOK -> {
                        Log.d("PhoneStateReceiver", "Call answered")
                        // Stop any ongoing LED flashing when call is answered
                        LedController.stopFlashing()
                    }
                    TelephonyManager.EXTRA_STATE_IDLE -> {
                        Log.d("PhoneStateReceiver", "Call ended")
                        // Stop LED flashing when call ends
                        LedController.stopFlashing()
                    }
                }
            } catch (e: Exception) {
                Log.e("PhoneStateReceiver", "Error processing phone state change", e)
            }
        }
    }
}