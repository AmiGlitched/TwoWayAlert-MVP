package com.myapplication.app

import com.google.firebase.firestore.FirebaseFirestore

class SosAlertManager {
    private val db = FirebaseFirestore.getInstance()
    fun createAnAlert(
        senderId: String,
        message: String,
        latitude: Double,
        longitude: Double,
        onSuccess: (String) -> Unit,
        onError: (Exception) -> Unit
    ) {

        val alertReference = db.collection("alerts").document()
        if (alertReference.id.isBlank()){
            return
        }

        val alert = EmergencyAlert(
            alertId = alertReference.id,
            senderId = senderId,
            message = message,
            latitude = latitude,
            longitude = longitude,
            primaryStatus = "PENDING",
            primarySentAt = 0L,
            secondaryStatus = "NOT_SENT",
            createdAt = System.currentTimeMillis()
        )


        alertReference
            .set(alert)
            .addOnSuccessListener {
                onSuccess(alertReference.id)
            }
            .addOnFailureListener {
                    exception -> onError(exception)
            }
    }
}