package com.example.tank2prolednotifier

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == "android.intent.action.BOOT_COMPLETED") {
            Log.d("BootReceiver", "Device booted — restarting SMS listener")

            // Optional: Restart any background services or listeners here
            // For now, just logging that the receiver is active
        }
    }
}