package com.myapplication.app.model

data class SosHistoryEntry(
    val timestamp: Long,
    val type: String,      // "Manual SOS", "Fall Detected", "Shake Trigger", "Volume Trigger", "Power Button Trigger" etc
    val location: String   // maps link or "Location unavailable."
)