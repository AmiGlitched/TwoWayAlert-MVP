package com.example.two_way_alert_system

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlin.math.sqrt

class FallDetector(context: Context, private val onFallDetected: () -> Unit) : SensorEventListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private var accelerometer: Sensor? = null

    // Thresholds for fall detection. These may need tuning for different devices.
    private val freefallThreshold = 1.0 // m/s^2 - A bit higher to be less sensitive
    private val impactThreshold = 20.0  // m/s^2 - Requires a significant jolt

    private var isFreefall = false
    private var lastFreefallTime: Long = 0

    init {
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    }

    fun start() {
        accelerometer?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
        }
    }

    fun stop() {
        sensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type != Sensor.TYPE_ACCELEROMETER) return

        val x = event.values[0]
        val y = event.values[1]
        val z = event.values[2]

        // Calculate the magnitude of the acceleration vector
        val magnitude = sqrt((x * x + y * y + z * z).toDouble())


        val currentTime = System.currentTimeMillis()

        // 1. Detect freefall phase
        if (magnitude < freefallThreshold) {
            isFreefall = true
            lastFreefallTime = currentTime
        }

        // 2. Detect impact phase if it follows a recent freefall
        if (isFreefall) {
            if (magnitude > impactThreshold) {
                // Check if impact happened within a short window after freefall (e.g., 500ms)
                if (currentTime - lastFreefallTime < 500) {
                    onFallDetected() // A fall is detected!
                }
            }
            // Reset freefall flag after a short period to avoid false triggers
            if (currentTime - lastFreefallTime > 500) {
                isFreefall = false
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Not used for this implementation, but required to be here.
    }
}
