package com.myapplication.app

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.telephony.SmsManager
import android.widget.Toast
import androidx.core.app.ActivityCompat
import com.google.firebase.firestore.FirebaseFirestore

object SmsSender {
    fun getSmsManager(context: Context): SmsManager {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            context.getSystemService(SmsManager::class.java)
        } else {
            SmsManager.getDefault()
        }
    }

    fun sendSms(
        context: Context,
        message: String,
        contacts: List<String>,
        alertId: String,
        sentContact: String
    ): Boolean {

        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
            return false
        }

        val smsManager = getSmsManager(context)
        var sent = false

        val deliveryIntent = Intent(context, SmsDeliveryReceiver::class.java).apply {
            action = "com.myapplication.app.SMS_DELIVERED"
            putExtra("alertId", alertId)
            putExtra("sentContact", sentContact)
        }

        val deliveryPendingIntent = PendingIntent.getBroadcast(
            context, "$alertId-$sentContact".hashCode(), deliveryIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        for (number in contacts) {
            if (number.isBlank()){
                Toast.makeText(context, "Contact is Empty!", Toast.LENGTH_SHORT).show()
                continue}

            try {
                val parts = smsManager.divideMessage(message)
                val deliveryIntents = ArrayList<PendingIntent>().apply {
                    repeat(parts.size) {
                        add(deliveryPendingIntent)
                    }
                }
                smsManager.sendMultipartTextMessage(number, null, parts, null, deliveryIntents)
                sent = true
            } catch (e: Exception) {
                Toast.makeText(context, "Error: $e", Toast.LENGTH_SHORT).show()
            }
        }

        if (sent) {
            val sentField = when (sentContact) {
                "primary" -> "primarySentAt"
                "secondary" -> "secondarySentAt"
                else -> null
            }

            if (sent && sentField != null) {
                FirebaseFirestore.getInstance()
                    .collection("alerts")
                    .document(alertId)
                    .update(sentField, System.currentTimeMillis())
            }
        }

        return sent }


}