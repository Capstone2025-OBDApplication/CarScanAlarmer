package com.example.canstone2.onnx

import android.content.Context
import ai.onnxruntime.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.nio.FloatBuffer
import kotlin.collections.indexOf
import kotlin.io.path.Path


class OnnxModelRunner(context: Context) {
    private val ortEnv: OrtEnvironment = OrtEnvironment.getEnvironment()
    private val ortSession: OrtSession

    init {
        val modelBytes = context.assets.open("rf_model.onnx").readBytes()
        ortSession = ortEnv.createSession(modelBytes)
    }

    suspend fun predict(speed: Int, rpm: Float, accel: Int, brake: Int): Int = withContext(Dispatchers.IO) {
        // Float Array
        val inputArray = floatArrayOf(
            speed.toFloat(),
            rpm,
            accel.toFloat(),
            brake.toFloat()
        )

        // Convert FloatArray to FloatBuffer
        // The buffer needs to be a direct buffer for ONNX Runtime
        val inputBuffer: FloatBuffer = FloatBuffer.allocate(inputArray.size)
        inputBuffer.put(inputArray)
        inputBuffer.flip()

        val inputShape = longArrayOf(1, 4) // 1 행 4 열

        // It copies the data from the buffer into the tensor.
        val inputTensor = OnnxTensor.createTensor(ortEnv, inputBuffer, inputShape)

        val result = ortSession.run(mapOf(ortSession.inputNames.iterator().next() to inputTensor))
        val output = result[0].value

        // 결과가 float[] 형식일 경우
        if (output is FloatArray) {
            return@withContext output.indices.maxByOrNull { output[it] } ?: -1
        }

        // 결과가 long[] 형식일 경우
        if (output is LongArray) {
            return@withContext output[0].toInt()
        }

        // 그 외 예외 처리
        throw IllegalStateException("예상치 못한 ONNX 출력 형식: ${output?.javaClass}")
    }
}
