package com.example.echosight

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.*
import android.graphics.ImageFormat
import android.speech.tts.TextToSpeech
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.platform.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import coil.compose.rememberAsyncImagePainter
import kotlinx.coroutines.*
import java.io.ByteArrayOutputStream
import java.util.*
import java.util.concurrent.Executors

data class RecognitionResult(
    val objectName: String,
    val confidence: Float,
    val timestamp: Long
)

enum class TransferState {
    IDLE,
    REQUESTING_PERMISSION,
    TRANSFERRING
}

@Composable
fun SecondScreen(
    onBack: () -> Unit = {},
    onNavigateToThirdScreen: () -> Unit = {}
) {

    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val coroutineScope = rememberCoroutineScope()

    var transferState by remember { mutableStateOf(TransferState.IDLE) }
    var naturalDescriptions by remember { mutableStateOf<List<String>>(emptyList()) }
    var lastSpeakTime by remember { mutableStateOf(0L) }
    var showDialog by remember { mutableStateOf(false) }

    val previewView = remember {
        PreviewView(context).apply {
            scaleType = PreviewView.ScaleType.FILL_CENTER
        }
    }

    val analyzerExecutor = remember { Executors.newSingleThreadExecutor() }

// TTS

    val tts = remember { mutableStateOf<TextToSpeech?>(null) }

    LaunchedEffect(Unit) {

        tts.value = TextToSpeech(context) { status ->

            if (status == TextToSpeech.SUCCESS) {

                tts.value?.language = Locale.US
                tts.value?.setSpeechRate(0.9f)

            } else {

                Log.e("TTS", "Initialization failed")
            }
        }
    }

    DisposableEffect(Unit) {

        onDispose {

            tts.value?.stop()
            tts.value?.shutdown()
        }
    }

    val permissionLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->

            val cameraGranted = permissions[Manifest.permission.CAMERA] ?: false
            val audioGranted = permissions[Manifest.permission.RECORD_AUDIO] ?: false

            if (cameraGranted && audioGranted) {
                onNavigateToThirdScreen()
            } else {
                Toast.makeText(
                    context,
                    "Camera and microphone permissions required",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

    fun speak(text: String) {

        val now = System.currentTimeMillis()

        if (now - lastSpeakTime > 2000) {

            lastSpeakTime = now

            tts.value?.speak(
                text,
                TextToSpeech.QUEUE_FLUSH,
                null,
                "yolo"
            )

            Log.d("TTS", text)
        }
    }

    fun triggerTransferToAgent() {

        val cameraOk =
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED

        val audioOk =
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED

        if (cameraOk && audioOk) {

            onNavigateToThirdScreen()

        } else {

            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.CAMERA,
                    Manifest.permission.RECORD_AUDIO
                )
            )
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF222429))
    ) {

        AndroidView(
            factory = { previewView },
            modifier = Modifier.fillMaxSize()
        )

        LaunchedEffect(Unit) {

            val cameraProvider =
                ProcessCameraProvider.getInstance(context).get()

            val preview = Preview.Builder().build()

            preview.setSurfaceProvider(previewView.surfaceProvider)

            val imageAnalyzer =
                ImageAnalysis.Builder()
                    .setBackpressureStrategy(
                        ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST
                    )
                    .build()

            val detector =
                try {
                    YOLOv8Detector.create(context)
                } catch (e: Exception) {
                    Log.e("YOLO", "Model load failed", e)
                    null
                }

            var lastInference = 0L

            imageAnalyzer.setAnalyzer(analyzerExecutor) { imageProxy ->

                val bitmap = imageProxyToBitmap(imageProxy)

                if (bitmap != null && detector != null) {

                    if (System.currentTimeMillis() - lastInference > 800) {

                        lastInference = System.currentTimeMillis()

                        try {

                            val detections = detector.run(bitmap)

                            val descriptions =
                                detections
                                    .filter { it.confidence > 0.25f }
                                    .take(3)
                                    .map { detectionToNaturalLanguage(it) }
                                    .distinct()

                            if (descriptions.isNotEmpty()) {

                                coroutineScope.launch(Dispatchers.Main) {

                                    naturalDescriptions = descriptions

                                    speak(descriptions.first())
                                }
                            }

                        } catch (e: Exception) {

                            Log.e("YOLO", "Inference error", e)
                        }
                    }
                }

                imageProxy.close()
            }

            cameraProvider.unbindAll()

            cameraProvider.bindToLifecycle(
                lifecycleOwner,
                CameraSelector.DEFAULT_BACK_CAMERA,
                preview,
                imageAnalyzer
            )
        }

        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 80.dp)
        ) {

            naturalDescriptions.forEach {

                Text(
                    text = it,
                    color = Color.Yellow,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(4.dp)
                )
            }
        }

        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .offset(y = -30.dp)
                .clickable {

                    Log.d("SOS", "SOS pressed")

                    SOS1.startSOS(context)
                }
        ) {

            Box(
                modifier = Modifier
                    .size(300.dp)
                    .border(
                        2.dp,
                        Color.White.copy(alpha = 0.5f)
                    )
            )
        }

        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .offset(y = (-50).dp)
        ) {

            Box(
                modifier = Modifier
                    .size(164.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            listOf(
                                Color(0xFFFF3232),
                                Color(0xFF7F0000)
                            ),
                            start = Offset.Zero,
                            end = Offset.Infinite
                        )
                    )
                    .clickable { showDialog = true }
            )

            Text(
                text = "SOS",
                fontSize = 48.sp,
                color = Color.Yellow,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.Center)
            )
        }

        Button(
            onClick = onBack,
            modifier = Modifier
                .padding(16.dp)
                .align(Alignment.TopStart),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.Transparent
            )
        ) {

            Text(
                text = "<",
                color = Color.White,
                fontSize = 24.sp
            )
        }

        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(end = 16.dp, top = 20.dp)
                .clickable {
                    triggerTransferToAgent()
                }
        ) {

            Box(
                modifier = Modifier
                    .size(100.dp)
                    .background(
                        color = Color(0xFFCFCECE),
                        shape = CircleShape
                    )
                    .align(Alignment.Center)
            )

            Box(
                modifier = Modifier
                    .size(90.dp)
                    .background(
                        color = Color(0xFFFCCC13),
                        shape = CircleShape
                    )
                    .align(Alignment.Center)
            )

            Image(
                painter = rememberAsyncImagePainter(R.drawable.people),
                contentDescription = "People",
                modifier = Modifier
                    .size(80.dp)
                    .align(Alignment.Center)
            )
        }

        if (showDialog) {

            AlertDialog(
                onDismissRequest = { showDialog = false },

                title = { Text("Confirm SOS?") },

                text = {
                    Text("Location will be sent with emergency message.")
                },

                confirmButton = {

                    TextButton(
                        onClick = {

                            showDialog = false

                            SOS1.startSOS(context)
                        }
                    ) {

                        Text("Send", color = Color.Red)
                    }
                },

                dismissButton = {

                    TextButton(
                        onClick = { showDialog = false }
                    ) {

                        Text("Cancel")
                    }
                }
            )
        }
    }
}

@OptIn(ExperimentalGetImage::class)
fun imageProxyToBitmap(imageProxy: ImageProxy): Bitmap? {

    val image = imageProxy.image ?: return null

    val yBuffer = image.planes[0].buffer
    val uBuffer = image.planes[1].buffer
    val vBuffer = image.planes[2].buffer

    val ySize = yBuffer.remaining()
    val uSize = uBuffer.remaining()
    val vSize = vBuffer.remaining()

    val nv21 = ByteArray(ySize + uSize + vSize)

    yBuffer.get(nv21, 0, ySize)
    uBuffer.get(nv21, ySize, uSize)
    vBuffer.get(nv21, ySize + uSize, vSize)

    val yuvImage =
        YuvImage(
            nv21,
            ImageFormat.NV21,
            imageProxy.width,
            imageProxy.height,
            null
        )

    val out = ByteArrayOutputStream()

    yuvImage.compressToJpeg(
        Rect(0, 0, imageProxy.width, imageProxy.height),
        90,
        out
    )

    val imageBytes = out.toByteArray()

    return BitmapFactory.decodeByteArray(
        imageBytes,
        0,
        imageBytes.size
    )
}

fun detectionToNaturalLanguage(detection: Detection): String {

    val box = detection.boundingBox

    val className =
        detection.className.replaceFirstChar {
            if (it.isLowerCase())
                it.titlecase(Locale.getDefault())
            else it.toString()
        }

    val centerX = (box.left + box.right) / 2f
    val centerY = (box.top + box.bottom) / 2f

    val horizontal =
        when {
            centerX < 0.33f -> "left"
            centerX > 0.66f -> "right"
            else -> "center"
        }

    val vertical =
        when {
            centerY < 0.33f -> "upper"
            centerY > 0.66f -> "lower"
            else -> ""
        }

    val position =
        when {
            vertical.isEmpty() && horizontal == "center" -> "center"
            vertical.isEmpty() -> horizontal
            else -> "$vertical $horizontal"
        }

    return "$className detected in the $position"
}