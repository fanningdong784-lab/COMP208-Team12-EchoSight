package com.example.echosight

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.util.Log
import org.tensorflow.lite.Interpreter
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ByteOrder

data class Detection(
    val className: String,
    val confidence: Float,
    val boundingBox: BoundingBox
)

data class BoundingBox(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float
)

class YOLOv8Detector private constructor(
    private val interpreter: Interpreter
) {


    companion object {

        private const val MODEL_PATH = "yolov8n.tflite"
        private const val INPUT_SIZE = 640
        private const val CONFIDENCE_THRESHOLD = 0.4f
        private const val IOU_THRESHOLD = 0.45f

        private val LABELS = listOf(
            "person","bicycle","car","motorcycle","airplane","bus","train","truck","boat",
            "traffic light","fire hydrant","stop sign","parking meter","bench","bird","cat",
            "dog","horse","sheep","cow","elephant","bear","zebra","giraffe","backpack",
            "umbrella","handbag","tie","suitcase","frisbee","skis","snowboard","sports ball",
            "kite","baseball bat","baseball glove","skateboard","surfboard","tennis racket",
            "bottle","wine glass","cup","fork","knife","spoon","bowl","banana","apple",
            "sandwich","orange","broccoli","carrot","hot dog","pizza","donut","cake","chair",
            "couch","potted plant","bed","dining table","toilet","tv","laptop","mouse",
            "remote","keyboard","cell phone","microwave","oven","toaster","sink","refrigerator",
            "book","clock","vase","scissors","teddy bear","hair drier","toothbrush"
        )

        fun create(context: Context): YOLOv8Detector {

            try {

                val model = context.assets.open(MODEL_PATH).readBytes()

                val options = Interpreter.Options().apply {
                    setNumThreads(4)
                }

                val buffer =
                    ByteBuffer.allocateDirect(model.size)
                        .order(ByteOrder.nativeOrder())

                buffer.put(model)
                buffer.rewind()

                val interpreter = Interpreter(buffer, options)

                Log.d("YOLO", "Model loaded successfully")

                return YOLOv8Detector(interpreter)

            } catch (e: IOException) {

                throw RuntimeException("Failed to load model", e)

            }
        }
    }

    fun run(bitmap: Bitmap): List<Detection> {

        val inputBitmap = letterbox(bitmap)

        val inputBuffer =
            ByteBuffer.allocateDirect(1 * INPUT_SIZE * INPUT_SIZE * 3 * 4)
                .order(ByteOrder.nativeOrder())

        val pixels = IntArray(INPUT_SIZE * INPUT_SIZE)

        inputBitmap.getPixels(
            pixels,
            0,
            INPUT_SIZE,
            0,
            0,
            INPUT_SIZE,
            INPUT_SIZE
        )

        for (pixel in pixels) {

            val r = ((pixel shr 16) and 0xFF) / 255f
            val g = ((pixel shr 8) and 0xFF) / 255f
            val b = (pixel and 0xFF) / 255f

            inputBuffer.putFloat(r)
            inputBuffer.putFloat(g)
            inputBuffer.putFloat(b)
        }

        inputBuffer.rewind()

        val output =
            Array(1) { Array(84) { FloatArray(8400) } }

        interpreter.run(inputBuffer, output)

        val detections = mutableListOf<Detection>()

        for (i in 0 until 8400) {

            val x = output[0][0][i]
            val y = output[0][1][i]
            val w = output[0][2][i]
            val h = output[0][3][i]

            var maxScore = 0f
            var classId = -1

            for (c in LABELS.indices) {

                val score = output[0][4 + c][i]

                if (score > maxScore) {
                    maxScore = score
                    classId = c
                }
            }

            if (maxScore > CONFIDENCE_THRESHOLD) {

                val left = (x - w / 2).coerceIn(0f, 1f)
                val top = (y - h / 2).coerceIn(0f, 1f)
                val right = (x + w / 2).coerceIn(0f, 1f)
                val bottom = (y + h / 2).coerceIn(0f, 1f)

                detections.add(
                    Detection(
                        LABELS[classId],
                        maxScore,
                        BoundingBox(left, top, right, bottom)
                    )
                )
            }
        }

        val finalDetections = nms(detections)

        Log.d("YOLO", "detections = ${finalDetections.size}")

        return finalDetections
    }

    private fun letterbox(bitmap: Bitmap): Bitmap {

        val result =
            Bitmap.createBitmap(
                INPUT_SIZE,
                INPUT_SIZE,
                Bitmap.Config.ARGB_8888
            )

        val canvas = Canvas(result)

        canvas.drawColor(Color.rgb(114,114,114))

        val ratio =
            minOf(
                INPUT_SIZE.toFloat() / bitmap.width,
                INPUT_SIZE.toFloat() / bitmap.height
            )

        val newW = (bitmap.width * ratio).toInt()
        val newH = (bitmap.height * ratio).toInt()

        val resized =
            Bitmap.createScaledBitmap(bitmap, newW, newH, true)

        val dx = (INPUT_SIZE - newW) / 2
        val dy = (INPUT_SIZE - newH) / 2

        canvas.drawBitmap(resized, dx.toFloat(), dy.toFloat(), null)

        return result
    }

    private fun nms(detections: List<Detection>): List<Detection> {

        val sorted =
            detections.sortedByDescending { it.confidence }

        val result = mutableListOf<Detection>()
        val removed = BooleanArray(sorted.size)

        for (i in sorted.indices) {

            if (removed[i]) continue

            val a = sorted[i]
            result.add(a)

            for (j in i + 1 until sorted.size) {

                if (removed[j]) continue

                val b = sorted[j]

                if (iou(a.boundingBox, b.boundingBox) > IOU_THRESHOLD) {

                    removed[j] = true
                }
            }
        }

        return result
    }

    private fun iou(a: BoundingBox, b: BoundingBox): Float {

        val left = maxOf(a.left, b.left)
        val top = maxOf(a.top, b.top)
        val right = minOf(a.right, b.right)
        val bottom = minOf(a.bottom, b.bottom)

        if (right < left || bottom < top) return 0f

        val inter = (right - left) * (bottom - top)

        val areaA = (a.right - a.left) * (a.bottom - a.top)
        val areaB = (b.right - b.left) * (b.bottom - b.top)

        return inter / (areaA + areaB - inter)
    }


}
