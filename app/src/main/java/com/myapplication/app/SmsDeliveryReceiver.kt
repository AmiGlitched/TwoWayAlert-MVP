package com.myapplication.app

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast
import com.google.firebase.firestore.FirebaseFirestore

class SmsDeliveryReceiver : BroadcastReceiver() {

    fun CheckFunction(intent: Intent): String?  {
        val alertId = intent.getStringExtra("alertId")
        if (intent.action != "com.myapplication.app.SMS_DELIVERED" || alertId == null) {
            return null
        } else {
            return alertId
        }
    }

    override fun onReceive(context: Context, intent: Intent) {

        val alertId = CheckFunction(intent)
        if (alertId == null){
            return}

        val sentContact = intent.getStringExtra("sentContact")
        val deliveredField = when (sentContact){
            "primary"   -> "primaryDeliveredAt"
            "secondary" -> "secondaryDeliveredAt"
            else -> { return }
        }

        if (resultCode == Activity.RESULT_OK) {
            FirebaseFirestore.getInstance()
                .collection("alerts")
                .document(alertId)
                .update(deliveredField, System.currentTimeMillis())

        } else   {
            Toast.makeText(context, "Could not Confirm Delivery Confirmation.", Toast.LENGTH_LONG).show()
        }
    }
}