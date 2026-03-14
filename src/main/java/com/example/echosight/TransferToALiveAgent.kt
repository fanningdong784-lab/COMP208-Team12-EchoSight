package com.example.echosight

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.view.SurfaceView
import android.widget.Toast
import androidx.core.app.ActivityCompat
import io.agora.rtc2.*
import io.agora.rtc2.video.VideoCanvas
import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController


object TransferToALiveAgent {

    private const val AGORA_APP_ID = "66237d518645488883063ce520179d9d"
    private const val AGORA_TOKEN = "007eJxTYFB8aizyorli4c7yxN2LW6LqT/MzSxxSey18QL47zKKzdLkCg5mZkbF5iqmhhZmJqYkFEBgbmBknp5oaGRiaW6ZYpuxcvDGzIZCRYe4pRxZGBggE8aUYknIy81J0E4uLM4tLdBPTU/NKdJMzEvPyUnMYGADUZCZT" // 测试环境可留空，正式环境需服务端生成
    private const val AGENT_CHANNEL_NAME = "blind-assist-agent-channel"

    private var rtcEngine: RtcEngine? = null

    private var currentUid: Int = 0

    private var callStateListener: CallStateListener? = null

    enum class CallState {
        IDLE,
        INITIALIZING,
        REQUEST_PERMISSION,
        CONNECTING,
        CONNECTED,
        DISCONNECTED,
        ERROR
    }


    var currentState: CallState = CallState.IDLE
        private set


    private val requiredPermissions = arrayOf(
        Manifest.permission.CAMERA,
        Manifest.permission.RECORD_AUDIO
    )


    interface CallStateListener {
        fun onStateChanged(state: CallState)
        fun onError(errorCode: Int, errorMsg: String)
        fun onUserJoined(uid: Int)
    }


    private fun initAgoraEngine(context: Context) {
        if (rtcEngine != null) return

        currentState = CallState.INITIALIZING
        callStateListener?.onStateChanged(currentState)

        try {
            val config = RtcEngineConfig().apply {
                mContext = context.applicationContext
                mAppId = AGORA_APP_ID
                mChannelProfile = Constants.CHANNEL_PROFILE_COMMUNICATION
                mEventHandler = object : IRtcEngineEventHandler() {
                    override fun onJoinChannelSuccess(channel: String, uid: Int, elapsed: Int) {
                        currentState = CallState.CONNECTING
                        callStateListener?.onStateChanged(currentState)
                        Toast.makeText(context, "Connecting to human customer service ..", Toast.LENGTH_SHORT).show()
                    }


                    override fun onError(err: Int) {
                        currentState = CallState.ERROR
                        callStateListener?.onStateChanged(currentState)
                        callStateListener?.onError(err, "AgoraErrorCode：$err")
                        Toast.makeText(context, "Connection failed: error code $err", Toast.LENGTH_SHORT).show()
                        cleanup()
                    }


                    override fun onUserJoined(uid: Int, elapsed: Int) {
                        currentState = CallState.CONNECTED
                        callStateListener?.onStateChanged(currentState)
                        callStateListener?.onUserJoined(uid)
                        Toast.makeText(context, "Human customer service has been connected", Toast.LENGTH_SHORT).show()
                    }


                    override fun onUserOffline(uid: Int, reason: Int) {
                        currentState = CallState.DISCONNECTED
                        callStateListener?.onStateChanged(currentState)
                        Toast.makeText(context, "Customer service has hung up the call", Toast.LENGTH_SHORT).show()
                        cleanup()
                    }


                    override fun onLeaveChannel(stats: RtcStats) {
                        currentState = CallState.DISCONNECTED
                        callStateListener?.onStateChanged(currentState)
                        cleanup()
                    }
                }
            }


            rtcEngine = RtcEngine.create(config)
            rtcEngine?.enableVideo()
            rtcEngine?.enableLocalVideo(true)

        } catch (e: Exception) {
            currentState = CallState.ERROR
            callStateListener?.onStateChanged(currentState)
            callStateListener?.onError(-1, e.message ?: "Engine initialization failed")
            Toast.makeText(context, "Engine initialization failed：${e.message}", Toast.LENGTH_SHORT).show()
        }
    }


    private fun checkAndRequestPermissions(context: Context): Boolean {
        currentState = CallState.REQUEST_PERMISSION
        callStateListener?.onStateChanged(currentState)

        val lacksPermissions = requiredPermissions.any {
            ActivityCompat.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED
        }

        if (lacksPermissions) {
            Toast.makeText(context, "Camera and microphone permissions are required to use video customer service", Toast.LENGTH_SHORT)
                .show()
            return false
        }
        return true
    }

    fun startVideoCall(
        context: Context,
        uid: Int = (Math.random() * 1000000).toInt(), // 随机生成用户ID
        listener: CallStateListener? = null
    ): Boolean {
        if (currentState != CallState.IDLE) {
            Toast.makeText(context, "There is currently a call, please end it first", Toast.LENGTH_SHORT).show()
            return false
        }

        callStateListener = listener
        currentUid = uid

        if (!checkAndRequestPermissions(context)) {
            currentState = CallState.ERROR
            callStateListener?.onStateChanged(currentState)
            return false
        }


        try {
            initAgoraEngine(context)

            val localSurfaceView = SurfaceView(context)
            localSurfaceView.setZOrderOnTop(true)

            rtcEngine?.setupLocalVideo(
                VideoCanvas(
                    localSurfaceView,
                    VideoCanvas.RENDER_MODE_HIDDEN,
                    0
                )
            )

            rtcEngine?.startPreview()

            rtcEngine?.joinChannel(AGORA_TOKEN, AGENT_CHANNEL_NAME, "", currentUid)

            return true
        } catch (e: Exception) {
            currentState = CallState.ERROR
            callStateListener?.onStateChanged(currentState)
            callStateListener?.onError(-2, e.message ?: "Call initiation failed")
            Toast.makeText(context, "Call initiation failed：${e.message}", Toast.LENGTH_SHORT).show()
            cleanup()
            return false
        }
    }


    fun endVideoCall(context: Context) {
        if (currentState == CallState.IDLE) return

        currentState = CallState.DISCONNECTED
        callStateListener?.onStateChanged(currentState)

        rtcEngine?.stopPreview()
        rtcEngine?.leaveChannel()
        RtcEngine.destroy()
        rtcEngine = null

        Toast.makeText(context, "The call has ended", Toast.LENGTH_SHORT).show()
        currentUid = 0
        callStateListener = null
    }


    private fun cleanup() {
        rtcEngine?.stopPreview()
        rtcEngine?.leaveChannel()
        RtcEngine.destroy()
        rtcEngine = null
        currentUid = 0
        currentState = CallState.IDLE
        callStateListener?.onStateChanged(currentState)
    }


    fun getLocalPreviewView(context: Context): SurfaceView {
        val surfaceView = SurfaceView(context)
        surfaceView.setZOrderOnTop(true)
        rtcEngine?.setupLocalVideo(VideoCanvas(surfaceView, VideoCanvas.RENDER_MODE_HIDDEN, 0))
        return surfaceView
    }


    fun setupRemoteVideoView(context: Context, remoteUid: Int): SurfaceView {
        val surfaceView = SurfaceView(context)
        rtcEngine?.setupRemoteVideo(
            VideoCanvas(
                surfaceView,
                VideoCanvas.RENDER_MODE_HIDDEN,
                remoteUid
            )
        )
        return surfaceView
    }
}



@Composable
fun AppNavigation() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = "second_screen"
    ) {

        composable("second_screen") {
            SecondScreen(
                onBack = { navController.popBackStack() },
                onNavigateToThirdScreen = { navController.navigate("third_screen") }
            )
        }


        composable("third_screen") {
            ThirdScreen(
                onBack = { navController.popBackStack() }
            )
        }
    }
}
