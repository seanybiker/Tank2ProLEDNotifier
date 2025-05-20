package com.example.tank2prolednotifier

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log

class NotificationListenerService : NotificationListenerService() {

    private val WHATSAPP_PACKAGE = "com.whatsapp"
    private val WHATSAPP_BUSINESS_PACKAGE = "com.whatsapp.w4b"
    
    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        
        sbn?.let { notification ->
            val packageName = notification.packageName
            Log.d("NotificationListener", "Notification from: $packageName")
            
            when (packageName) {
                WHATSAPP_PACKAGE, WHATSAPP_BUSINESS_PACKAGE -> {
                    triggerLedFlash("whatsapp")
                }
                // Add other apps as needed
                "com.android.mms", "com.google.android.apps.messaging" -> {
                    triggerLedFlash("sms")
                }
                else -> {
                    // Only flash for messaging/communication apps
                    if (isMessagingApp(packageName)) {
                        triggerLedFlash("default")
                    }
                }
            }
        }
    }

    private fun isMessagingApp(packageName: String): Boolean {
        val messagingApps = setOf(
            "com.telegram.messenger",
            "com.facebook.orca", // Facebook Messenger
            "com.viber.voip",
            "com.skype.raider",
            "com.discord",
            "org.signal.android",
            "com.snapchat.android"
        )
        return messagingApps.contains(packageName)
    }

    private fun triggerLedFlash(type: String) {
        try {
            // Get the MainActivity instance if it exists
            val activityClass = Class.forName("com.example.tank2prolednotifier.MainActivity")
            
            // For simplicity, we'll use the LED control directly here
            // In a real implementation, you might want to use a shared LED controller class
            LedController.flashForNotification(type)
            
        } catch (e: Exception) {
            Log.e("NotificationListener", "Failed to trigger LED flash", e)
        }
    }
}