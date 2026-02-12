package com.example.two_way_alert_system

import android.content.Intent
import android.os.Bundle
import android.widget.EditText
import android.widget.Button
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge

class LoginPageActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContentView(R.layout.activity_login_page)

        val name = findViewById<EditText>(R.id.et_name)
        val email = findViewById<EditText>(R.id.et_emailID)
        val password = findViewById<EditText>(R.id.et_password)

        val login_button = findViewById<Button>(R.id.button_login)
        login_button.setOnClickListener{
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent) }
    }
}