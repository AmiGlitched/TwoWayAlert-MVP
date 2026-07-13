package com.myapplication.app.model

// simple holder, name is optional-ish (falls back to "Contact" if left blank in the UI)
data class Contact(
    val name: String,
    val phone: String
)