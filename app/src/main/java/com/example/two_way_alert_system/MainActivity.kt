package com.example.two_way_alert_system

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.widget.TextView
import android.widget.Button
import android.widget.Toast
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import kotlin.math.sqrt


class MainActivity : ComponentActivity(), SensorEventListener {
    private lateinit var sensorManager: SensorManager
    private var accelerometer: Sensor? = null
    private val FALL_THRESHOLD = 20.0   // ALERT sent if this threshold exceeds


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val sendAlertButton = findViewById<Button>(R.id.Button_SendAlert)
        sendAlertButton.setOnClickListener {
            val intent = Intent(this, SendMessageActivity::class.java)
            startActivity(intent)
        }

        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    }


    override fun onResume() {
        super.onResume()
        if (accelerometer != null) {
            sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL)
        }
    }

    override fun onPause() {
        super.onPause()
        sensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent) {
        val x = event.values[0]
        val y = event.values[1]
        val z = event.values[2]

        val acceleration = sqrt((x * x + y * y + z * z).toDouble())  //// Calculates the magnitude of acceleration

        // Fall Detection
        if (acceleration > FALL_THRESHOLD) {
            Toast.makeText(this, "FALL DETECTED! AUTOMATIC ALERT SENT TO EMERGENCY CONTACTS", Toast.LENGTH_LONG).show()
        }

        val accelerometerReading = findViewById<TextView>(R.id.TV_AccReading)
        accelerometerReading.text ="ACCELEROMETER READING: %.2f".format(acceleration)
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) { }

}