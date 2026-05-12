package com.myapplication.app.ml

import android.content.Context
import org.tensorflow.lite.Interpreter
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import java.io.FileInputStream
import android.content.res.AssetFileDescriptor

class FallDetectionModel(context: Context) {

    private var interpreter: Interpreter

    init {
        interpreter = Interpreter(loadModelFile(context))
    }

    private fun loadModelFile(context: Context): MappedByteBuffer {
        val fileDescriptor: AssetFileDescriptor = context.assets.openFd("fall_verification.tflite")
        val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
        val fileChannel = inputStream.channel
        val startOffset = fileDescriptor.startOffset
        val declaredLength = fileDescriptor.declaredLength
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
    }

    fun predict(input: FloatArray): Int {
        val output = Array(1) { FloatArray(3) } // 3 classes: ADL, Fall, Drop
        interpreter.run(input, output)
        return argMax(output[0])
    }

    private fun argMax(array: FloatArray): Int {
        var maxIndex = 0
        for (i in array.indices) {
            if (array[i] > array[maxIndex]) maxIndex = i
        }
        return maxIndex}



}