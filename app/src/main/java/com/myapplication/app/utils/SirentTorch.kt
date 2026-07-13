package com.myapplication.app.utils

import android.content.Context
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Handler
import android.os.Looper

// Loud Mode uses both of these together, Silent Mode skips calling them entirely
object SirenTorch {

    private var toneGen: ToneGenerator? = null
    private var strobeHandler: Handler? = null
    private var strobeRunnable: Runnable? = null
    private var strobing = false

    fun startSiren() {
        try {
            toneGen = ToneGenerator(AudioManager.STREAM_ALARM, 100)
            // 15s is plenty, gets cancelled early anyway if the user cancels the alert
            toneGen?.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 15000)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun stopSiren() {
        try {
            toneGen?.stopTone()
            toneGen?.release()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        toneGen = null
    }

    fun startTorchStrobe(context: Context) {
        if (strobing) return
        val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
        val camId = findFlashCameraId(cameraManager) ?: return

        strobing = true
        strobeHandler = Handler(Looper.getMainLooper())
        var torchOn = false
        strobeRunnable = object : Runnable {
            override fun run() {
                if (!strobing) return
                torchOn = !torchOn
                try {
                    cameraManager.setTorchMode(camId, torchOn)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                strobeHandler?.postDelayed(this, 400L)
            }
        }
        strobeHandler?.post(strobeRunnable!!)
    }

    fun stopTorchStrobe(context: Context) {
        strobing = false
        strobeRunnable?.let { strobeHandler?.removeCallbacks(it) }
        try {
            val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
            val camId = findFlashCameraId(cameraManager)
            if (camId != null) cameraManager.setTorchMode(camId, false)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun findFlashCameraId(cameraManager: CameraManager): String? {
        return try {
            cameraManager.cameraIdList.firstOrNull { id ->
                cameraManager.getCameraCharacteristics(id).get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}