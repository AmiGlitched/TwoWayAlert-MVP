package com.myapplication.app.ml

import android.content.Context
import android.util.Log
import org.tensorflow.lite.Interpreter
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import java.io.FileInputStream
import android.content.res.AssetFileDescriptor

class FallDetectionModel(context: Context) {

    private var interpreter: Interpreter
    private val modelFileName = "fall_verification.tflite"
    private val windowSize = 100
    private val numClasses = 3

    init {
        interpreter = Interpreter(loadModelFile(context))
        // Verify input/output shapes (optional but recommended)
        val inputShape = interpreter.getInputTensor(0).shape()
        val outputShape = interpreter.getOutputTensor(0).shape()
        Log.d("FallModel", "Input shape: ${inputShape.contentToString()}")
        Log.d("FallModel", "Output shape: ${outputShape.contentToString()}")
        require(inputShape.contentEquals(intArrayOf(1, windowSize, 3))) {
            "Model input shape must be [1, 100, 3], got ${inputShape.contentToString()}"
        }
        require(outputShape.contentEquals(intArrayOf(1, numClasses))) {
            "Model output shape must be [1, 3], got ${outputShape.contentToString()}"
        }
    }

    private fun loadModelFile(context: Context): MappedByteBuffer {
        val fileDescriptor: AssetFileDescriptor = context.assets.openFd(modelFileName)
        val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
        val fileChannel = inputStream.channel
        val startOffset = fileDescriptor.startOffset
        val declaredLength = fileDescriptor.declaredLength
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
    }

    /**
     * Predict the class of a 2‑second accelerometer window.
     * param window Array of 100 samples, each FloatArray(x, y, z) in g.
     * return class index: 0 = ADL, 1 = Fall, 2 = Phone Drop
     */
    fun predict(window: Array<FloatArray>): Int {
        require(window.size == windowSize) {
            "Window must have exactly $windowSize samples, got ${window.size}"
        }
        val probabilities = predictProbabilities(window)
        return argMax(probabilities)
    }

    /**
     * Get the raw probabilities for the three classes.
     * param window Array of 100 samples, each FloatArray(x, y, z) in g.
     * return FloatArray of size 3: [ADL, Fall, Drop] probabilities (sum = 1.0).
     */
    fun predictProbabilities(window: Array<FloatArray>): FloatArray {
        require(window.size == windowSize) {
            "Window must have exactly $windowSize samples, got ${window.size}"
        }
        // The model expects shape [1, 100, 3]
        val input = arrayOf(window)
        val output = Array(1) { FloatArray(numClasses) }
        interpreter.run(input, output)

        // Optional: log probabilities for debugging
        val probs = output[0]
        Log.d("FallModel", "Probs: ADL=${probs[0]}, Fall=${probs[1]}, Drop=${probs[2]}")
        return probs
    }

    private fun argMax(array: FloatArray): Int {
        var maxIndex = 0
        for (i in array.indices) {
            if (array[i] > array[maxIndex]) maxIndex = i
        }
        return maxIndex
    }

    fun close() {
        interpreter.close()
    }
}