package com.example.echosight

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.AlertDialog
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import com.example.echosight.SOS1.startSOS
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.LocalContext

@Composable
fun EchoMainUI(navigateToSecondScreen: () -> Unit,
               navigateToSettings: () -> Unit,
               onSpeak: (String) -> Unit ){

    val context = LocalContext.current
    var showDialog by remember { mutableStateOf(false) }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text(text = "Confirm SOS", fontWeight = FontWeight.Bold) },
            text = { Text(text = "Are you sure you want to send an emergency message?") },
            confirmButton = {
                androidx.compose.material3.TextButton(
                    onClick = {
                        showDialog = false
                        startSOS(context)
                    }
                ) {
                    Text("SEND", color = Color.Red, fontWeight = FontWeight.ExtraBold)
                }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = { showDialog = false }) {
                    Text("Cancel", color = Color.Gray)
                }
            }
        )
    }

    Image(
        painter = painterResource(id = R.drawable.homepage1),
        contentDescription = null,
        modifier = Modifier
            .fillMaxWidth()
            .height(700.dp),
        contentScale = ContentScale.Crop
    )

    Column(modifier = Modifier.padding(start = 30.dp, top = 100.dp)) {
        Text(
            text = buildAnnotatedString {
                withStyle(style = SpanStyle(color = Color.White)) { append("Echo") }
                withStyle(style = SpanStyle(color = Color(0xFFFFD100))) { append("Sight") }
            },
            fontSize = 50.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Intelligent Visual assistant",
            color = Color.White.copy(0.7f),
            fontSize = 20.sp
        )
    }


    Box(
        modifier = Modifier
            .offset(y = (-20).dp)
            .size(270.dp)
            .clickable { navigateToSecondScreen() }, // 👈 使用传入的函数
        contentAlignment = Alignment.Center
    ) {

        Box(
            modifier = Modifier
                .size(265.dp)
                .background(Color.Black, CircleShape)
        )

        Box(
            modifier = Modifier
                .size(250.dp)
                .background(color = Color(0xFFDBAA3A), shape = CircleShape)
        )

        Box(
            modifier = Modifier
                .size(230.dp)
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(Color(0xFFFFE03D), Color(0xFFF4AF08))
                    ),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.camera),
                contentDescription = "Camera",
                modifier = Modifier.size(150.dp, 120.dp),
                contentScale = ContentScale.Fit
            )
        }
    }

        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 80.dp)
                    .align(Alignment.BottomCenter),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // SOS
                Box(
                    modifier = Modifier
                        .size(170.dp, 150.dp)
                        .background(Color.White, RoundedCornerShape(30.dp))
                        .clickable { showDialog = true},
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "SOS",
                        color = Color.Red,
                        fontSize = 55.sp,
                        fontWeight = FontWeight.Black
                    )
                }
                Box(
                    modifier = Modifier
                        .size(170.dp, 150.dp)
                        .background(Color.White, RoundedCornerShape(30.dp))
                        .clickable {
                            navigateToSettings()
                        }
                        .padding(8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.setout),
                        contentDescription = "Set Out",
                        modifier = Modifier.size(100.dp),
                        colorFilter = ColorFilter.tint(Color.Black)
                    )
                    Image(
                        painter = painterResource(id = R.drawable.setin),
                        contentDescription = "Set In",
                        modifier = Modifier.size(45.dp),
                        colorFilter = ColorFilter.tint(Color.Black)
                    )
                }
            }
        }
    }
