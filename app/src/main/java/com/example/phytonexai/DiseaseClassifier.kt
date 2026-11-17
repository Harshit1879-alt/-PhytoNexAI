
package com.example.phytonexai

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel
import kotlin.math.exp

data class DiseaseResult(
    val disease: String,
    val confidence: Float
)

class DiseaseClassifier(context: Context) {

    companion object {
        private const val MODEL_NAME = "mango_disease_model.tflite"
        private const val LABELS_NAME = "labels.txt"
        private const val IMAGE_SIZE = 224
    }

    private val interpreter: Interpreter
    private val labels: List<String>

    init {
        val modelBuffer = context.assets.openFd(MODEL_NAME).use { fd ->
            FileInputStream(fd.fileDescriptor).channel.use { channel ->
                channel.map(
                    FileChannel.MapMode.READ_ONLY,
                    fd.startOffset,
                    fd.declaredLength
                )
            }
        }

        interpreter = Interpreter(modelBuffer)

        labels = context.assets.open(LABELS_NAME)
            .bufferedReader()
            .use { reader ->
                reader.readLines()
                    .map { it.trim() }
                    .filter { it.isNotEmpty() }
            }

        require(
            labels.size == interpreter.getOutputTensor(0).shape()[1]
        ) {
            "The number of labels does not match the model output."
        }
    }

    // Classify an image selected from the gallery.
    fun classify(uri: Uri, context: Context): DiseaseResult {
        val bitmap = context.contentResolver
            .openInputStream(uri)
            .use { stream ->
                requireNotNull(stream) {
                    "Unable to open selected image."
                }

                BitmapFactory.decodeStream(stream)
                    ?: throw IllegalArgumentException("Invalid image.")
            }

        return classify(bitmap, true)
    }

    // Classify a photo captured directly by the camera.
    fun classify(bitmap: Bitmap, context: Context): DiseaseResult {
        return classify(bitmap, false)
    }

    // Shared preprocessing and inference for both image sources.
    private fun classify(
        bitmap: Bitmap,
        recycleBitmap: Boolean
    ): DiseaseResult {

        val resized = Bitmap.createScaledBitmap(
            bitmap,
            IMAGE_SIZE,
            IMAGE_SIZE,
            true
        )

        try {
            val input = ByteBuffer.allocateDirect(
                IMAGE_SIZE * IMAGE_SIZE * 3 * 4
            ).order(ByteOrder.nativeOrder())

            for (y in 0 until IMAGE_SIZE) {
                for (x in 0 until IMAGE_SIZE) {
                    val pixel = resized.getPixel(x, y)

                    // RGB normalization: [0, 255] -> [-1, 1].
                    input.putFloat(
                        (((pixel shr 16) and 0xFF) / 127.5f) - 1f
                    )
                    input.putFloat(
                        (((pixel shr 8) and 0xFF) / 127.5f) - 1f
                    )
                    input.putFloat(
                        ((pixel and 0xFF) / 127.5f) - 1f
                    )
                }
            }

            input.rewind()

            val output = Array(1) {
                FloatArray(labels.size)
            }

            interpreter.run(input, output)

            val scores = output[0]
            val sum = scores.sum()

            // Preserve probabilities if the model already returns them.
            val probabilities = if (
                scores.all { it in 0f..1f } &&
                kotlin.math.abs(sum - 1f) < 0.01f
            ) {
                scores
            } else {
                // Convert logits to probabilities.
                val maxScore = scores.maxOrNull() ?: 0f
                val exponents = scores.map {
                    exp((it - maxScore).toDouble())
                }
                val total = exponents.sum()

                FloatArray(scores.size) { i ->
                    (exponents[i] / total).toFloat()
                }
            }

            val bestIndex = probabilities.indices.maxByOrNull {
                probabilities[it]
            } ?: 0

            return DiseaseResult(
                disease = labels[bestIndex],
                confidence = probabilities[bestIndex] * 100f
            )

        } finally {
            if (resized !== bitmap && !resized.isRecycled) {
                resized.recycle()
            }

            if (recycleBitmap && !bitmap.isRecycled) {
                bitmap.recycle()
            }
        }
    }

    fun close() {
        interpreter.close()
    }
}
