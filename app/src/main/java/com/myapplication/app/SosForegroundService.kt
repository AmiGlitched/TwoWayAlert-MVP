//package com.myapplication.app
//
//import android.app.Notification
//import android.app.NotificationChannel
//import android.app.NotificationManager
//import android.app.PendingIntent
//import android.app.Service
//import android.content.Context
//import android.content.Intent
//import android.hardware.Sensor
//import android.hardware.SensorEvent
//import android.hardware.SensorEventListener
//import android.hardware.SensorManager
//import android.os.Build
//import android.os.IBinder
//import androidx.core.app.NotificationCompat
//import com.myapplication.app.MainActivity
//import kotlin.math.sqrt
//
//class SosForegroundService : Service(), SensorEventListener {
//
//    private lateinit var sensorManager: SensorManager
//    private var accelerometer: Sensor? = null
//    private val CHANNEL_ID = "SOS_SERVICE_CHANNEL"
//
//    override fun onCreate() {
//        super.onCreate()
//        createNotificationChannel()
//        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
//        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
//    }
//
//    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
//        val action = intent?.action
//
//        // If the user tapped the "SEND SOS" button from the notification
//        if (action == "ACTION_MANUAL_SOS") {
//            triggerManualSos()
//            return START_STICKY
//        }
//
//        // Start the foreground notification shield
//        val notification = buildNotification()
//        if (Build.VERSION.SDK_INT >= 34) {
//            startForeground(1, notification, android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
//        } else {
//            startForeground(1, notification)
//        }
//
//        // ONLY register the accelerometer if Fall Detection is explicitly enabled
//        val isFallDetectionEnabled = intent?.getBooleanExtra("ENABLE_FALL_DETECTION", false) ?: false
//        if (isFallDetectionEnabled) {
//            accelerometer?.let {
//                sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
//            }
//        } else {
//            sensorManager.unregisterListener(this) // Keep sensor off to save battery
//        }
//
//        return START_STICKY
//    }
//
//    override fun onSensorChanged(event: SensorEvent?) {
//        if (event?.sensor?.type == Sensor.TYPE_ACCELEROMETER) {
//            val x = event.values[0]
//            val y = event.values[1]
//            val z = event.values[2]
//            val gForce = sqrt((x * x + y * y + z * z).toDouble()).toFloat()
//
//            // If a massive drop is detected, wake up the MainActivity!
//            if (gForce > 25.0f) {
//                sensorManager.unregisterListener(this) // Pause sensor to avoid spamming
//
//                val wakeIntent = Intent(this, MainActivity::class.java).apply {
//                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
//                    putExtra("FALL_DETECTED", true)
//                }
//                startActivity(wakeIntent)
//
//                // Resume sensor after 12 seconds (giving them time to cancel)
//                android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
//                    accelerometer?.let { sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL) }
//                }, 12000)
//            }
//        }
//    }
//
//    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
//
//    private fun triggerManualSos() {
//        // Force MainActivity to open and instantly fire the SOS
//        val sosIntent = Intent(this, MainActivity::class.java).apply {
//            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
//            putExtra("MANUAL_SOS_TRIGGERED", true)
//        }
//        startActivity(sosIntent)
//    }
//
//    private fun buildNotification(): Notification {
//        // Intent to open the app normally
//        val pendingIntent = PendingIntent.getActivity(
//            this, 0, Intent(this, MainActivity::class.java),
//            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
//        )
//
//        // Intent for the giant "SEND SOS" button on the notification
//        val sosIntent = Intent(this, SosForegroundService::class.java).apply { action = "ACTION_MANUAL_SOS" }
//        val sosPendingIntent = PendingIntent.getService(
//            this, 1, sosIntent,
//            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
//        )
//
//        return NotificationCompat.Builder(this, CHANNEL_ID)
//            .setContentTitle("Two-Way Alert Active")
//            .setContentText("Fall detection is running in the background.")
//            .setSmallIcon(android.R.drawable.ic_dialog_alert) // Replace with your app icon later
//            .setContentIntent(pendingIntent)
//            .addAction(android.R.drawable.ic_menu_call, "🚨 SEND SOS NOW", sosPendingIntent)
//            .setOngoing(true) // Makes it un-swipeable
//            .build()
//    }
//
//    private fun createNotificationChannel() {
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//            val channel = NotificationChannel(CHANNEL_ID, "SOS Background Service", NotificationManager.IMPORTANCE_HIGH)
//            val manager = getSystemService(NotificationManager::class.java)
//            manager.createNotificationChannel(channel)
//        }
//    }
//
//    override fun onBind(intent: Intent?): IBinder? = null
//
//    override fun onDestroy() {
//        super.onDestroy()
//        sensorManager.unregisterListener(this)
//    }
//}
package com.myapplication.app

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.google.firebase.BuildConfig
import com.myapplication.app.MainActivity
import com.myapplication.app.ml.FallDetectionModel
import com.myapplication.app.utils.Preprocessor
import kotlin.math.sqrt
//import com.myapplication.app. BuildConfig
import android.widget.Toast


class SosForegroundService : Service(), SensorEventListener {
////
    companion object{
        const val ACTION_MANUAL_SOS="ACTION_MANUAL_SOS"
    const val ACTION_SIMULATE_FALL="ACTION_SIMULATE_FALL"
    }

    ///////
    private lateinit var sensorManager: SensorManager
    private var accelerometer: Sensor? = null
    private val CHANNEL_ID = "SOS_SERVICE_CHANNEL"

    private lateinit var fallModel: FallDetectionModel

    // Sliding window buffer
    private val windowSize = 100   // number of samples per window
    private val buffer = mutableListOf<FloatArray>()

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        fallModel = FallDetectionModel(this)
    }




    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action

        when (action) {
            ACTION_MANUAL_SOS -> {
                triggerManualSos()
                return START_STICKY
            }
            ACTION_SIMULATE_FALL -> {
                simulateFallEvent()
                return START_STICKY
            }
        }

        val notification = buildNotification()
        if (Build.VERSION.SDK_INT >= 34) {
            startForeground(
                1,
                notification,
                android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            )
        } else {
            startForeground(1, notification)
        }

        val isFallDetectionEnabled = intent?.getBooleanExtra("ENABLE_FALL_DETECTION", false) ?: false
        if (isFallDetectionEnabled) {
            accelerometer?.let {
                sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME)
            }
        } else {
            sensorManager.unregisterListener(this)
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

            // Add sample to buffer
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

private fun buildNotification(): Notification {
    val pendingIntent = PendingIntent.getActivity(
        this, 0, Intent(this, MainActivity::class.java),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
    )

    // 🚨 Manual SOS button
    val sosIntent = Intent(this, SosForegroundService::class.java).apply { action = ACTION_MANUAL_SOS }
    val sosPendingIntent = PendingIntent.getService(
        this, 1, sosIntent,
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
    )

    // 🧪 Simulate Fall button
    val simulateIntent = Intent(this, SosForegroundService::class.java).apply { action = ACTION_SIMULATE_FALL }
    val simulatePendingIntent = PendingIntent.getService(
        this, 2, simulateIntent,
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
    )

    return NotificationCompat.Builder(this, CHANNEL_ID)
        .setContentTitle("Two-Way Alert Active")
        .setContentText("Fall detection is running in the background.")
        .setSmallIcon(android.R.drawable.ic_dialog_alert)
        .setContentIntent(pendingIntent)
        .addAction(android.R.drawable.ic_menu_call, "🚨 SEND SOS NOW", sosPendingIntent)
        .addAction(android.R.drawable.ic_menu_info_details, "🧪 Simulate Fall", simulatePendingIntent)
        .setOngoing(true)
        .build()
}

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(CHANNEL_ID, "SOS Background Service", NotificationManager.IMPORTANCE_HIGH)
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        sensorManager.unregisterListener(this)}




}