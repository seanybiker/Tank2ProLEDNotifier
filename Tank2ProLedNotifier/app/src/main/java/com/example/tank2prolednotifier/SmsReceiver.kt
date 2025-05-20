package com.example.tank2prolednotifier

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.telephony.SmsMessage
import android.util.Log

class SmsReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent?.action == "android.provider.Telephony.SMS_RECEIVED") {
            try {
                val extras = intent.extras
                if (extras != null) {
                    val pdus = extras.get("pdus") as Array<*>
                    
                    for (pdu in pdus) {
                        val smsMessage = SmsMessage.createFromPdu(pdu as ByteArray)
                        val sender = smsMessage.displayOriginatingAddress
                        val messageBody = smsMessage.messageBody
                        
                        Log.d("SMSReceiver", "SMS from: $sender, Message: $messageBody")
                        
                        // Trigger LED flash for SMS
                        LedController.flashForNotification("sms")
                    }
                }
            } catch (e: Exception) {
                Log.e("SMSReceiver", "Error processing SMS", e)
            }
        }
    }
}