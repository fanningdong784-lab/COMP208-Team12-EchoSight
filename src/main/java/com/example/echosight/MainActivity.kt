package com.example.echosight

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import java.util.Locale

class MainActivity : ComponentActivity(), TextToSpeech.OnInitListener {

    private val PERMISSION_REQUEST_CODE = 1001

    private lateinit var fusedLocationClient: FusedLocationProviderClient

    private lateinit var tts: TextToSpeech

    private val requiredPermissions = arrayOf(
        Manifest.permission.CAMERA,
        Manifest.permission.SEND_SMS,
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION
    )

    @OptIn(ExperimentalGetImage::class)
    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)

        fusedLocationClient =
            LocationServices.getFusedLocationProviderClient(this)

        tts = TextToSpeech(this, this)

        checkAndRequestPermissions()

        setContent {

            MaterialTheme {

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    val speakFunction: (String) -> Unit = { text ->

                        if (::tts.isInitialized) {

                            Log.d("TTS", "Speak: $text")

                            tts.speak(
                                text,
                                TextToSpeech.QUEUE_FLUSH,
                                null,
                                "echo_speech"
                            )
                        }
                    }

                    NavHost(
                        navController = navController,
                        startDestination = "main"
                    ) {

                        composable("main") {

                            EchoMainUI(

                                navigateToSecondScreen = {

                                    navController.navigate("second")

                                },

                                navigateToSettings = {

                                    navController.navigate("settings")

                                },

                                onSpeak = speakFunction
                            )
                        }

                        composable("second") {

                            SecondScreen(

                                onBack = {

                                    navController.popBackStack()
                                },
                                onNavigateToThirdScreen = {
                                    navController.navigate("third")
                                }
                            )
                        }


                        composable("third") {
                            ThirdScreen(
                                onBack =  {navController.popBackStack() }
                            )
                        }

                        composable("settings") {

                            SetScreen(

                                onBack = {

                                    navController.popBackStack()

                                }
                            )
                        }
                    }
                }
            }
        }
    }

    // TTS 初始化
    override fun onInit(status: Int) {

        if (status == TextToSpeech.SUCCESS) {

            val result = tts.setLanguage(Locale.US)

            if (result == TextToSpeech.LANG_MISSING_DATA ||
                result == TextToSpeech.LANG_NOT_SUPPORTED
            ) {

                Log.e("TTS", "Language not supported")

                Toast.makeText(
                    this,
                    "TTS language not supported",
                    Toast.LENGTH_SHORT
                ).show()

            } else {

                tts.setSpeechRate(0.9f)

                Log.d("TTS", "TTS initialized successfully")
            }

        } else {

            Log.e("TTS", "TTS init failed")

            Toast.makeText(
                this,
                "TTS failed to initialize",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    override fun onDestroy() {

        if (::tts.isInitialized) {

            tts.stop()
            tts.shutdown()

        }

        super.onDestroy()
    }

    // 权限检查
    private fun checkAndRequestPermissions() {

        val permissionsToRequest = requiredPermissions.filter {

            ContextCompat.checkSelfPermission(
                this,
                it
            ) != PackageManager.PERMISSION_GRANTED
        }

        if (permissionsToRequest.isNotEmpty()) {

            ActivityCompat.requestPermissions(
                this,
                permissionsToRequest.toTypedArray(),
                PERMISSION_REQUEST_CODE
            )

        } else {

            initLocationAndSOSManager()
        }
    }

    // 初始化 SOS
    private fun initLocationAndSOSManager() {

        SOS1.setFusedLocationProvider(fusedLocationClient)

        Toast.makeText(
            this,
            "SOS ready",
            Toast.LENGTH_SHORT
        ).show()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {

        super.onRequestPermissionsResult(
            requestCode,
            permissions,
            grantResults
        )

        if (requestCode == PERMISSION_REQUEST_CODE) {

            if (grantResults.isNotEmpty() &&
                grantResults.all { it == PackageManager.PERMISSION_GRANTED }
            ) {

                initLocationAndSOSManager()

            } else {

                Toast.makeText(
                    this,
                    "Permissions denied, SOS may not work",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }
}