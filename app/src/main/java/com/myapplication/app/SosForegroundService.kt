package com.myapplication.app

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.google.firebase.BuildConfig
import android.widget.Toast
import com.myapplication.app.MainActivity
import com.myapplication.app.ml.FallDetectionModel
import com.myapplication.app.utils.Preprocessor
import kotlin.math.sqrt

class SosForegroundService : Service(), SensorEventListener {

    companion object {
        const val ACTION_MANUAL_SOS = "ACTION_MANUAL_SOS"
        const val ACTION_SIMULATE_FALL = "ACTION_SIMULATE_FALL"

        private const val SHAKE_THRESHOLD_GFORCE = 18.0f // separate (lower/simpler) threshold than the ML fall model
        private const val SHAKE_WINDOW_MS = 1200L
        private const val SHAKE_COUNT_NEEDED = 3

        private const val POWER_PRESS_WINDOW_MS = 2500L
        private const val POWER_PRESS_COUNT_NEEDED = 3
    }

    private lateinit var sensorManager: SensorManager
    private var accelerometer: Sensor? = null
    private val channelId = "SOS_SERVICE_CHANNEL"

    private lateinit var fallModel: FallDetectionModel

    // Sliding window buffer for the ML fall model
    private val windowSize = 100   // number of samples per window
    private val buffer = mutableListOf<FloatArray>()

    // shake trigger state
    private var shakeTriggerEnabled = false
    private val shakeTimestamps = mutableListOf<Long>()
    private var lastShakeSosTime = 0L

    // power button (screen off/on) trigger state
    private var powerButtonTriggerEnabled = false
    private val screenOffTimestamps = mutableListOf<Long>()
    private var screenReceiverRegistered = false

    private val screenStateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (!powerButtonTriggerEnabled) return
            if (intent?.action == Intent.ACTION_SCREEN_OFF) {
                val now = System.currentTimeMillis()
                screenOffTimestamps.add(now)
                screenOffTimestamps.removeAll { (now - it) > POWER_PRESS_WINDOW_MS }
                if (screenOffTimestamps.size >= POWER_PRESS_COUNT_NEEDED) {
                    screenOffTimestamps.clear()
                    triggerPowerButtonSos()
                }
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        fallModel = FallDetectionModel(this)
    }
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_MANUAL_SOS -> {
                triggerManualSos()
                return START_STICKY
            }
            ACTION_SIMULATE_FALL -> {
                simulateFallEvent()
                return START_STICKY
            }
        }

        // Start the foreground notification shield
        val notification = buildNotification()
        if (Build.VERSION.SDK_INT >= 34) {
            startForeground(1, notification, android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(1, notification)
        }

        val isFallDetectionEnabled = intent?.getBooleanExtra("ENABLE_FALL_DETECTION", false) ?: false
        shakeTriggerEnabled = intent?.getBooleanExtra("ENABLE_SHAKE_TRIGGER", false) ?: false
        powerButtonTriggerEnabled = intent?.getBooleanExtra("ENABLE_POWER_BUTTON_TRIGGER", false) ?: false

        // register/unregister accelerometer if either fall detection or shake trigger needs it
        if (isFallDetectionEnabled || shakeTriggerEnabled) {
            accelerometer?.let {
                sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
            }
        } else {
            sensorManager.unregisterListener(this) // Keep sensor off to save battery
        }

        // register/unregister the screen state receiver for the power-button trigger
        if (powerButtonTriggerEnabled && !screenReceiverRegistered) {
            val filter = IntentFilter(Intent.ACTION_SCREEN_OFF)
            registerReceiver(screenStateReceiver, filter)
            screenReceiverRegistered = true
        } else if (!powerButtonTriggerEnabled && screenReceiverRegistered) {
            try { unregisterReceiver(screenStateReceiver) } catch (e: Exception) { e.printStackTrace() }
            screenReceiverRegistered = false
        }

        return START_STICKY
    }

    private fun simulateFallEvent() {
        // Fake accelerometer data: sudden drop on Z-axis
        val fakeFall = FloatArray(windowSize * 3) { i ->
            when (i % 3) {
                0 -> 0f   // x-axis steady
                1 -> 0f   // y-axis steady
                else -> -9.8f // z-axis sudden drop
            }
        }

        val normalized = Preprocessor.normalize(fakeFall)
        val prediction = fallModel.predict(normalized)

        val classNames = arrayOf("ADL", "Fall", "Phone Drop")
        val result = classNames[prediction]

        android.util.Log.d("MLTest", "Simulated Prediction: $result")
//////
        Toast.makeText(this, "Simulated fall triggered", Toast.LENGTH_SHORT).show()

        ////////
        if (result == "Fall") {
            triggerSOS()
        }
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type == Sensor.TYPE_ACCELEROMETER) {
            val x = event.values[0]
            val y = event.values[1]
            val z = event.values[2]
            val gForce = sqrt((x * x + y * y + z * z).toDouble()).toFloat()

            // shake trigger check runs independently of the ML buffer below
            if (shakeTriggerEnabled) checkForShake(gForce)

            buffer.add(floatArrayOf(x, y, z))

            // When buffer reaches window size, flatten and run inference
            if (buffer.size >= windowSize) {
                val flattened = buffer.flatMap { it.toList() }.toFloatArray()
                buffer.clear() // reset buffer

                val input = Preprocessor.normalize(flattened)
                val prediction = fallModel.predict(input)

                val classNames = arrayOf("ADL", "Fall", "Phone Drop")
                val result = classNames[prediction]

                if (result == "Fall") {
                    triggerSOS()
                }
            }
        }
    }

    private fun checkForShake(gForce: Float) {
        if (gForce < SHAKE_THRESHOLD_GFORCE) return
        val now = System.currentTimeMillis()

        // cooldown so one shake gesture doesn't refire the SOS ten times in a row
        if (now - lastShakeSosTime < 5000L) return

        shakeTimestamps.add(now)
        shakeTimestamps.removeAll { now - it > SHAKE_WINDOW_MS }

        if (shakeTimestamps.size >= SHAKE_COUNT_NEEDED) {
            shakeTimestamps.clear()
            lastShakeSosTime = now
            triggerShakeSos()
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    private fun triggerManualSos() {
        val sosIntent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            putExtra("MANUAL_SOS_TRIGGERED", true)
        }
        startActivity(sosIntent)
    }

    private fun triggerSOS() {
        val wakeIntent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            putExtra("FALL_DETECTED", true)
        }
        startActivity(wakeIntent)
    }

    private fun triggerShakeSos() {
        val shakeIntent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            putExtra("SHAKE_SOS_TRIGGERED", true)
        }
        startActivity(shakeIntent)
    }

    private fun triggerPowerButtonSos() {
        val powerIntent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            putExtra("POWER_BUTTON_SOS_TRIGGERED", true)
        }
        startActivity(powerIntent)
    }

    private fun buildNotification(): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

        // Manual SOS button
        val sosIntent = Intent(this, SosForegroundService::class.java).apply { action = ACTION_MANUAL_SOS }
        val sosPendingIntent = PendingIntent.getService(
            this, 1, sosIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        // Simulate Fall button
        val simulateIntent = Intent(this, SosForegroundService::class.java).apply { action = ACTION_SIMULATE_FALL }
        val simulatePendingIntent = PendingIntent.getService(
            this, 2, simulateIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, channelId)
            .setContentTitle("Two-Way Alert Active")
            .setContentText("Background monitoring is running.")
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentIntent(pendingIntent)
            .addAction(android.R.drawable.ic_menu_call, "SEND SOS NOW", sosPendingIntent)
            .addAction(android.R.drawable.ic_menu_info_details, "Simulate Fall", simulatePendingIntent)
            .setOngoing(true)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "SOS Background Service", NotificationManager.IMPORTANCE_HIGH)
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        sensorManager.unregisterListener(this)
        fallModel?.close()
        if (screenReceiverRegistered) {
            try { unregisterReceiver(screenStateReceiver) } catch (e: Exception) { e.printStackTrace() }
            screenReceiverRegistered = false
        }
    }
}