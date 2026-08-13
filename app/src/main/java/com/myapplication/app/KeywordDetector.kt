package com.myapplication.app

class KeywordDetector {

    val sosKeywords = arrayOf(
        "help",
        "sos",
        "danger",
        "save me",
        "send alert",
        "emergency",
        "ambulance",
        "call family",
        "call hospital",
        "call police",
        "call ambulance"
    )

    private val alrightKeywords = arrayOf(
        "alright",
        "fine",
        "ok",
        "okay",
        "no problem",
        "cancel sos"
    )

    fun containsSOSKeyword(text: String): Boolean {
        val message = text.lowercase()
        for (keyword in sosKeywords) {
            if (message.contains(keyword)) {
                return true
            }
        }
        return false
    }

    fun containsAlrightKeyword(text: String): Boolean {
        val message = text.lowercase()
        for (keyword in alrightKeywords) {
            if (message.contains(keyword)) {
                return true
            }
        }
        return false
    }

    fun containsNoKeyword(text: String): Boolean {
        val message = text.lowercase()
        for (keyword in sosKeywords) {
            if (message.contains(keyword)) {
                return false
            }
        }

        for (keyword in alrightKeywords) {
            if (message.contains(keyword)) {
                return false
            }
        }

        return true
    }
}

