package com.example.petproject

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.util.Log

/**
 * BroadcastReceiver for automatically detecting SMS messages containing OTP codes
 * and forwarding them to the OtpVerificationActivity.
 */
class SmsReceiver : BroadcastReceiver() {
    private val TAG = "SmsReceiver"
    
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Telephony.Sms.Intents.SMS_RECEIVED_ACTION) {
            val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
            
            for (message in messages) {
                val messageBody = message.messageBody
                val sender = message.originatingAddress
                
                Log.d(TAG, "SMS received from: $sender")
                
                // Extract OTP from the message
                val otp = extractOtpFromMessage(messageBody)
                
                if (otp != null) {
                    Log.d(TAG, "OTP extracted: $otp")
                    
                    // Forward the OTP to the OtpVerificationActivity
                    val otpIntent = Intent("com.example.petproject.OTP_RECEIVED")
                    otpIntent.putExtra("otp", otp)
                    context.sendBroadcast(otpIntent)
                }
            }
        }
    }
    
    /**
     * Extracts a 6-digit OTP code from the SMS message.
     * 
     * @param message The SMS message body
     * @return The extracted OTP or null if no OTP found
     */
    private fun extractOtpFromMessage(message: String): String? {
        // Look for a 6-digit number in the message
        val otpPattern = Regex("\\b(\\d{6})\\b")
        val matchResult = otpPattern.find(message)
        
        return matchResult?.groupValues?.get(1)
    }
}