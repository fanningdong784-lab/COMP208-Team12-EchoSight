package com.example.echosight

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.echosight.TransferToALiveAgent.CallState

/**
 * 人工视频客服页面（仅页面逻辑，无导航代码）
 */
@Composable
fun ThirdScreen(
    onBack: () -> Unit = {} // 仅保留返回回调，无其他导航相关代码
) {
    val context = LocalContext.current
    var remoteAgentUid by remember { mutableStateOf(0) }
    var callStatus by remember { mutableStateOf("正在连接人工客服...") }

    // 进入页面自动启动通话
    LaunchedEffect(Unit) {
        val isSuccess = TransferToALiveAgent.startVideoCall(
            context = context,
            listener = object : TransferToALiveAgent.CallStateListener {
                override fun onStateChanged(state: CallState) {
                    when (state) {
                        CallState.CONNECTING -> callStatus = "正在连接人工客服..."
                        CallState.CONNECTED -> callStatus = "客服已接通，可正常沟通"
                        CallState.DISCONNECTED -> {
                            callStatus = "The call has ended"
                            remoteAgentUid = 0
                            onBack() // 挂断返回SecondScreen
                        }
                        CallState.ERROR -> {
                            callStatus = "Connection failed, please try again"
                            remoteAgentUid = 0
                            Toast.makeText(context, callStatus, Toast.LENGTH_SHORT).show()
                            onBack()
                        }
                        else -> {}
                    }
                }

                override fun onError(errorCode: Int, errorMsg: String) {
                    // 打印错误码到 Logcat（Android Studio 底部 Logcat 面板搜索 AgoraError）
                    android.util.Log.e("AgoraError", "ErrorCode：$errorCode，ErrorMessage：$errorMsg")
                    callStatus = "ErrorCode（Code：$errorCode）：$errorMsg"
                    Toast.makeText(context, callStatus, Toast.LENGTH_LONG).show()
                    onBack()
                }

                override fun onUserJoined(uid: Int) {
                    remoteAgentUid = uid
                    callStatus = "Customer service has been launched"
                }
            }
        )

        if (!isSuccess) {
            Toast.makeText(context, "Failed to initiate video call", Toast.LENGTH_SHORT).show()
            onBack()
        }
    }

    // 挂断通话
    fun endCall() {
        // 1. 先结束通话，释放声网资源
        TransferToALiveAgent.endVideoCall(context)
        // 2. 重置状态，避免残留视图
        remoteAgentUid = 0
        callStatus = "The call has ended"
        // 3. 延迟一小段时间再返回，给系统足够时间清理视图
        androidx.compose.ui.platform.ComposeView(context).postDelayed({
            onBack()
        }, 300)
    }

    // 页面UI
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // 远端客服视频（全屏）
        if (remoteAgentUid != 0) {
            AndroidView(
                factory = { ctx ->
                    TransferToALiveAgent.setupRemoteVideoView(ctx, remoteAgentUid).apply {
                        layoutParams = android.view.ViewGroup.LayoutParams(
                            android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                            android.view.ViewGroup.LayoutParams.MATCH_PARENT
                        )
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        }

        // 本地视频预览（右上角小窗）
        AndroidView(
            factory = { ctx ->
                TransferToALiveAgent.getLocalPreviewView(ctx).apply {
                    layoutParams = android.view.ViewGroup.LayoutParams(
                        250.dp.value.toInt(),
                        350.dp.value.toInt()
                    )
                    setZOrderOnTop(true)
                }
            },
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp)
                .size(250.dp, 350.dp)
        )

        // 通话状态提示
        Text(
            text = callStatus,
            color = Color.White,
            fontSize = 18.sp,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(16.dp)
                .background(Color.Black.copy(alpha = 0.5f))
                .padding(8.dp)
        )

        // 挂断按钮
        Button(
            onClick = { endCall() },
            modifier = Modifier
                .align(Alignment.Center) // 移到屏幕正中央
                .padding(24.dp)
                .size(300.dp), // 从80dp放大到150dp，点击区域更大
            shape = CircleShape,
            colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
            // 增加点击反馈（可选，提升体验）
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 10.dp)
        ) {
            Text(
                text = "Hang Up",
                color = Color.White,
                fontSize = 48.sp, // 字体也同步放大，保持比例
                fontWeight = FontWeight.Bold
            )
        }
    }
}