package com.myapplication.app

import org.vosk.LibVosk
import org.vosk.LogLevel
import org.vosk.Model
import org.vosk.Recognizer
import org.vosk.android.RecognitionListener
import org.vosk.android.SpeechService
import org.vosk.android.SpeechStreamService
import org.vosk.android.StorageService
import java.io.IOException
import android.os.Handler
import android.os.Looper
import android.content.Context
import org.json.JSONObject


class VoiceRecognitionManager (
    private val context: Context,
    private val onTextRecognized: (String) -> (Unit),
    private val errorState: (String) -> (Unit)) : RecognitionListener {

    private var model: Model? = null
    private var modelReady = false
    private var speechService: SpeechService? = null
    private var speechStreamService: SpeechStreamService? = null  //for WAV FILES
    private val handler = Handler(Looper.getMainLooper())
    private var isContinuousMonitoringActive = false
    var isPostFallMonitoringActive = false

    private val stopPostFallMonitoringRunnable = Runnable {
        isPostFallMonitoringActive =false
        stopPostFallMonitoring()
    }


    init {
        LibVosk.setLogLevel(LogLevel.INFO)
        initModel()

    }

    private fun initModel() {

        StorageService.unpack(
            context, "vosk-model-small-en-us-0.15", "model",StorageService.Callback { loadedModel ->
                model = loadedModel
                modelReady = true

                if (isContinuousMonitoringActive || isPostFallMonitoringActive){
                    startListening()
                }
            }, {exception -> modelReady = false
                errorState(exception?.message ?: "Failed to unpack Vosk Model")

            }
        )
    }

    private fun startListening(){

        if (!modelReady || model == null) {
            return
        }

        speechService?.stop()
        speechService?.shutdown()
        speechService = null

        try {
            val recognizer = Recognizer(model, 16000.0f)
            speechService = SpeechService(recognizer, 16000.0f)
            speechService?.startListening(this)
        } catch (e: IOException) {
            onError(e)
        }
    }



    fun startContinuousVoiceMonitoring() {

        isContinuousMonitoringActive = true
        isPostFallMonitoringActive= false
        handler.removeCallbacks (stopPostFallMonitoringRunnable)
        startListening()
    }


    //Post Fall Monitoring remains active for 30 MINUTES after a fall is detected.
    // Sends VoiceAssisted SOS Alarm if SOS Keywords are detected.
    // Stops Post Fall Monitoring when Alright Keyword detected.
    fun monitorVoiceAfterFallDetected() {

        if(isPostFallMonitoringActive){
            return
        }
        isContinuousMonitoringActive = false
        isPostFallMonitoringActive = true

        handler.removeCallbacks (stopPostFallMonitoringRunnable)

        startListening()
        handler.postDelayed( stopPostFallMonitoringRunnable, 30 * 60 * 1000L)
    }

    fun stopPostFallMonitoring(){

        isPostFallMonitoringActive = false

        handler.removeCallbacks (stopPostFallMonitoringRunnable)

        speechService?.stop()
        speechService?.shutdown()
        speechService = null
    }

    fun stopContinuousVoiceMonitoring() {

        isContinuousMonitoringActive = false
        isPostFallMonitoringActive = false

        handler.removeCallbacks(stopPostFallMonitoringRunnable)

        speechService?.stop()
        speechService?.shutdown()
        speechService = null

    }

    override fun onPartialResult(hypothesis: String?) {
        if (hypothesis.isNullOrBlank()){
            return
        }

        try {
            val json = JSONObject(hypothesis)
            val text = json.optString("partial")
            if (text.isNotBlank()){
                onTextRecognized(text.lowercase())
            }
        } catch (e : Exception){
            errorState(e.message?: "Vosk Partial Result Error!")
        }
    }

    override fun onResult(hypothesis: String?) {
        if (hypothesis.isNullOrBlank()){
            return
        }

        try {
            val json = JSONObject(hypothesis)
            val text = json.optString("text")

            if (text.isNotBlank()){
                onTextRecognized(text.lowercase())
            }
        } catch (e : Exception){
            errorState(e.message?: "Vosk Result Parsing Error!")
        }
    }

    override fun onFinalResult(hypothesis: String?) {
        if (speechStreamService != null) {
            speechStreamService = null
        }
    }


    override fun onError(e: Exception) {
        errorState(e.message?: "Unknown Vosk Error")
    }

    override fun onTimeout() {
        if (isContinuousMonitoringActive || isPostFallMonitoringActive){
            startListening()
        }

    }

    fun release(){

        isContinuousMonitoringActive = false
        isPostFallMonitoringActive = false

        handler.removeCallbacksAndMessages(null)

        if (speechService != null) {
            speechService?.stop()
            speechService?.shutdown()
            speechService = null
        }

        if (speechStreamService != null) {
            speechStreamService?.stop()
            speechStreamService = null
        }

        model?.close()
        model = null
    }

}