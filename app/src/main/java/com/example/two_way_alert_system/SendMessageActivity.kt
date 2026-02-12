package com.example.two_way_alert_system

import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import android.widget.Button
import androidx.activity.ComponentActivity

class SendMessageActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContentView(R.layout.activity_send_message)

        val buttonSendAlert = findViewById<Button>(R.id.button_SendAlert)
        buttonSendAlert.setOnClickListener{
            Toast.makeText(this, "Alert Sent Successfully!", Toast.LENGTH_LONG).show()
        }
    }
}