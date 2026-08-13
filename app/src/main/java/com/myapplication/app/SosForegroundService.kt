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
import android.util.Log

class SosForegroundService : Service(), SensorEventListener {

    companion object {
        const val ACTION_MANUAL_SOS = "ACTION_MANUAL_SOS"
        const val ACTION_SIMULATE_FALL = "ACTION_SIMULATE_FALL"

        private const val SHAKE_THRESHOLD_GFORCE = 18.0f // separate (lower/simpler) threshold than the ML fall model
        private const val SHAKE_WINDOW_MS = 1200L
        private const val SHAKE_COUNT_NEEDED = 3

        private const val POWER_PRESS_WINDOW_MS = 2500L
        private const val POWER_PRESS_COUNT_NEEDED = 3

        const val ACTION_START_CONTINUOUS_VOICE_MONITORING = "com.myapplication.app.START_CONTINUOUS_VOICE"
        const val ACTION_STOP_CONTINUOUS_VOICE_MONITORING = "com.myapplication.app.STOP_CONTINUOUS_VOICE"
        const val ACTION_POST_FALL_MONITORING_ONLY = "com.myapplication.app.ACTION_POST_FALL_MONITORING_ONLY"
        const val ACTION_STOP_SERVICE = "com.myapplication.app.ACTION_STOP_SERVICE"

        private const val NOTIFICATION_ID = 1
        const val NOTIFICATION_ID_FALL_DETECTION = 2
        const val NOTIFICATION_ID_POST_FALL_VOICE_MONITORING = 3
        const val NOTIFICATION_ID_CONTINUOUS_VOICE_MONITORING = 4
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

    //For Voice Monitoring for SOS Keywords
    private lateinit var voiceRecognitionManager: VoiceRecognitionManager
    private var sensorRegistered = false
    private var pause = false
    private var postFallPause = false


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

        voiceRecognitionManager = VoiceRecognitionManager (context = applicationContext, onTextRecognized = {
            text -> handleVoiceText(text) },
            errorState = {
                error-> Log.e("VOSK", error)
            }
        )
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

            ACTION_START_CONTINUOUS_VOICE_MONITORING -> {
                voiceRecognitionManager.isPostFallMonitoringActive = false
                voiceRecognitionManager.startContinuousVoiceMonitoring()
                val manager = getSystemService(NotificationManager::class.java)
                manager.cancel(NOTIFICATION_ID_POST_FALL_VOICE_MONITORING)
            }

            ACTION_STOP_CONTINUOUS_VOICE_MONITORING -> {
                voiceRecognitionManager.stopContinuousVoiceMonitoring()
            }

            ACTION_POST_FALL_MONITORING_ONLY -> {
                voiceRecognitionManager.stopContinuousVoiceMonitoring()
                val prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE)
                if (prefs.getBoolean("isFallDetectionRunning", false)) {
                    displayPostFallMonitoringNotification()
                }
            }

            ACTION_STOP_SERVICE -> {
                val manager = getSystemService(NotificationManager::class.java)
                manager.cancel(NOTIFICATION_ID_POST_FALL_VOICE_MONITORING)
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
                return START_NOT_STICKY
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
            accelerometer?.let { sensor ->
                if (!sensorRegistered) {
                    sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_NORMAL)
                }
            }
        } else {
            if (sensorRegistered) {
                sensorManager.unregisterListener(this) // Keep sensor off to save battery
            }
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
            voiceRecognitionManager.monitorVoiceAfterFallDetected()
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
                    voiceRecognitionManager.monitorVoiceAfterFallDetected()
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

    private fun triggerVoiceAssistedSOS(text: String) {
        val sosIntent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            putExtra("VOICE_SOS_TRIGGERED", true)
            putExtra("SOS_REASON", "Voice Assisted SOS Alarm")
            putExtra("SOS_KEYWORD_TEXT", text)
        }
        startActivity(sosIntent)
    }

    private fun cancelAutomaticSOS() {
        val cancelIntent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            putExtra("VOICE_SOS_CANCELLED", true)
        }
        startActivity(cancelIntent)
    }

    private fun handleVoiceText(text: String) {
        val detector = KeywordDetector()
        val prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE)
        val alwaysListen = prefs.getBoolean("listeningForKeyword", false)


        if (alwaysListen) {

            // MONITORS FOR SOS KEYWORDS ONLY DURING 24/7 CONTINUOUS VOICE ASSISTANCE
            if (detector.containsSOSKeyword(text) && !pause ){
                pause = true
                Toast.makeText(this, "Detected: \"$text\". Voice Assisted SOS Triggered!", Toast.LENGTH_LONG).show()
                triggerVoiceAssistedSOS(text)
                android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                    pause = false },
                    5000L
                )
            }

            return  //Ignores Alright Keywords in 24/7 Continuous Voice Monitoring
        }

        if (!voiceRecognitionManager.isPostFallMonitoringActive) {
            return
        }




        // MONITORS FOR BOTH SOS AND ALRIGHT KEYWORDS AFTER A FALL IS DETECTED
        if (detector.containsSOSKeyword(text) && !postFallPause) {
            postFallPause = true
            Toast.makeText(this, "Detected: \"$text\". Voice Assisted SOS Triggered!", Toast.LENGTH_LONG).show()

            triggerVoiceAssistedSOS(text)
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                pause = false
            }, 5000L)

        }

        if (detector.containsAlrightKeyword(text)) {
            cancelAutomaticSOS()
            Toast.makeText(this, "Detected: \"$text\". Voice Assisted SOS Cancelled!", Toast.LENGTH_LONG).show()
            voiceRecognitionManager.stopPostFallMonitoring()

        }
    }
    private fun buildNotification(): Notification {

        val prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE)

        val fallDetection = prefs.getBoolean("isFallDetectionRunning", false)
        val voiceAssistance = prefs.getBoolean("listeningForKeyword", false)

        val title: String
        val text: String
        val ongoing : Boolean

        when {
            fallDetection && voiceAssistance -> {
                title = "Two-Way Alert Active"
                text = "Fall Detection and Continuous Voice Assistance are active."
                ongoing = true
            }

            voiceAssistance -> {
                title = "Continuous Voice Assistance Active"
                text = "Continuously monitoring for SOS keywords in the background."
                ongoing = true
            }

            fallDetection -> {
                title = "24/7 Fall Detection Active"
                text = "Monitoring for falls in the background."
                ongoing = true

            }

            else -> {
                title = "Two-Way Alert Active"
                text = "Background monitoring is active."
                ongoing = false
            }
        }

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
            .setContentTitle(title)
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentIntent(pendingIntent)
            .addAction(android.R.drawable.ic_menu_call, "SEND SOS NOW", sosPendingIntent)
            .addAction(android.R.drawable.ic_menu_info_details, "Simulate Fall", simulatePendingIntent)
            .setOngoing(ongoing)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "SOS Background Service", NotificationManager.IMPORTANCE_HIGH)
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }
    private fun displayPostFallMonitoringNotification() {
        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("POST FALL VOICE MONITORING READY")
            .setContentText("Voice assistance will activate automatically when a fall is detected.")
            .setOngoing(false)
            .setAutoCancel(false)
            .build()

        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(NOTIFICATION_ID_POST_FALL_VOICE_MONITORING, notification)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        sensorManager.unregisterListener(this)
        fallModel?.close()
        voiceRecognitionManager.release()
        sensorRegistered = false
        if (screenReceiverRegistered) {
            try { unregisterReceiver(screenStateReceiver) } catch (e: Exception) { e.printStackTrace() }
            screenReceiverRegistered = false
        }
    }
}