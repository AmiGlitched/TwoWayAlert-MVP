package com.myapplication.app

data class EmergencyAlert(
    val alertId: String = "",
    val senderId: String = "",
    val message: String = "",
    val link: String ="",
    val createdAt: Long = 0L,

    val latitude: Double = 0.0,
    val longitude: Double = 0.0,

    val primaryStatus: String = "NOT_SENT",
    val primarySentAt: Long = 0L,
    val primaryDeliveredAt: Long = 0L,
    val primarySeenAt: Long = 0L,
    val primaryRespondedAt: Long = 0L,
    val primaryResponse: String = "",
    val primaryAlertedSecondaryContactAt: Long = 0L,
    val primaryCall112At: Long = 0L,
    val primaryHelpOnTheWayAt: Long = 0L,
    val primaryEmergencyResolvedAt: Long = 0L,

    val secondaryStatus: String = "NOT_SENT",
    val secondarySentAt: Long = 0L,
    val secondaryDeliveredAt: Long = 0L,
    val secondarySeenAt: Long = 0L,
    val secondaryRespondedAt: Long = 0L,
    val secondaryResponse : String = "",
    val secondaryCall112At: Long = 0L,
    val secondaryHelpOnTheWayAt: Long = 0L,
    val secondaryEmergencyResolvedAt: Long = 0L
)