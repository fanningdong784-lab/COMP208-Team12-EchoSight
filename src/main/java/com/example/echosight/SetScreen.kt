package com.example.echosight

// Compose 基础布局相关导入
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
// Material3 组件相关导入
import androidx.compose.material3.*
// Compose 状态管理相关导入
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
// 布局对齐相关导入
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
// 资源和文本样式相关导入
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
// 自定义绘制相关导入
import androidx.compose.foundation.Canvas
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.draw.clip

data class PersonalInfo(
    val name: String = "",
    val gender: String = "",
    val bloodType: String = "",
    val medicalHistory: String = ""
)

@Composable
fun SetScreen(
    onBack: () -> Unit
) {

    val gradient = androidx.compose.ui.graphics.Brush.verticalGradient(
        colors = listOf(Color(0xFF23262D), Color(0xFF5B6174))
    )


    var showFeedbackDialog by remember { mutableStateOf(false) }
    var showPersonalInfoDialog by remember { mutableStateOf(false) }
    var personalInfo by remember { mutableStateOf(PersonalInfo()) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(brush = gradient)
            .padding(top = 48.dp, start = 20.dp, end = 20.dp),
        horizontalAlignment = Alignment.Start
    ) {

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {

            IconButton(onClick = onBack) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_arrow_back_white),
                    contentDescription = "Back",
                    tint = Color.White
                )
            }
            Text(
                text = "EchoSight",
                color = Color.White,
                fontSize = 48.sp,
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.Normal,
                modifier = Modifier.padding(start = 16.dp)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))
        SettingsItem(title = "Personal Information") {
            showPersonalInfoDialog = true
        }
        Spacer(modifier = Modifier.height(16.dp))
        SOSContactItem()
        Spacer(modifier = Modifier.height(16.dp))
        BroadcastSpeedSlider()
        Spacer(modifier = Modifier.height(16.dp))
        SettingsItem(title = "Help and Feedback") {
            showFeedbackDialog = true
        }
        Spacer(modifier = Modifier.height(16.dp))
        PrivacyPermissionToggle()
    }


    if (showPersonalInfoDialog) {
        PersonalInfoDialog(
            initialInfo = personalInfo,
            onDismiss = { showPersonalInfoDialog = false },
            onSave = { info ->
                personalInfo = info
                println("保存的个人信息：$info")
                showPersonalInfoDialog = false
            }
        )
    }


    if (showFeedbackDialog) {
        HelpAndFeedbackDialog(
            onDismiss = { showFeedbackDialog = false },
            onSubmit = { feedbackText ->

                println("用户反馈：$feedbackText")
                showFeedbackDialog = false
            }
        )
    }
}


@Composable
fun PersonalInfoDialog(
    initialInfo: PersonalInfo,
    onDismiss: () -> Unit,
    onSave: (PersonalInfo) -> Unit
) {
    var name by remember { mutableStateOf(initialInfo.name) }
    var selectedGender by remember { mutableStateOf(initialInfo.gender) }
    var selectedBloodType by remember { mutableStateOf(initialInfo.bloodType) }
    var medicalHistory by remember { mutableStateOf(initialInfo.medicalHistory) }
    val genderOptions = listOf("Male", "Female")


    AlertDialog(

        containerColor = Color(0xFF23262D),

        shape = RoundedCornerShape(20.dp),

        modifier = Modifier
            .fillMaxWidth(0.9f)
            .height(IntrinsicSize.Min),

        title = {
            Text(
                text = "Personal Information",
                color = Color.White,
                fontSize = 36.sp,
            )
        },

        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                TextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name", color = Color.White, fontSize = 22.sp) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(70.dp),
                    colors = TextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedContainerColor = Color(0x4DFFFFFF),
                        unfocusedContainerColor = Color(0x4DFFFFFF),
                        focusedLabelColor = Color(0xFFFCCC13),
                        unfocusedLabelColor = Color.White,
                        cursorColor = Color(0xFFFCCC13),
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    ),
                    textStyle = LocalTextStyle.current.copy(fontSize = 20.sp),
                    shape = RoundedCornerShape(10.dp)
                )


                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Gender",
                        color = Color.White,
                        fontSize = 28.sp,
                        fontFamily = FontFamily.Serif,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        genderOptions.forEach { gender ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.clickable {
                                    selectedGender = gender
                                }
                            ) {
                                RadioButton(
                                    selected = selectedGender == gender,
                                    onClick = { selectedGender = gender },
                                    colors = RadioButtonDefaults.colors(
                                        selectedColor = Color(0xFFFCCC13),
                                        unselectedColor = Color.White
                                    ),
                                    modifier = Modifier.size(30.dp)
                                )
                                Text(
                                    text = gender,
                                    color = Color.White,
                                    fontSize = 24.sp,
                                    modifier = Modifier.padding(start = 8.dp)
                                )
                            }
                        }
                    }
                }


                TextField(
                    value = selectedBloodType,
                    onValueChange = { selectedBloodType = it },
                    label = { Text("Blood Type", color = Color.White, fontSize = 22.sp) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(70.dp),
                    colors = TextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedContainerColor = Color(0x4DFFFFFF),
                        unfocusedContainerColor = Color(0x4DFFFFFF),
                        focusedLabelColor = Color(0xFFFCCC13),
                        unfocusedLabelColor = Color.White,
                        cursorColor = Color(0xFFFCCC13),
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    ),
                    textStyle = LocalTextStyle.current.copy(fontSize = 20.sp),
                    shape = RoundedCornerShape(10.dp),
                    placeholder = {
                        Text(
                            text = "Enter blood type (e.g. A, B, O, AB)",
                            color = Color(0x80FFFFFF),
                            fontSize = 18.sp
                        )
                    }
                )


                TextField(
                    value = medicalHistory,
                    onValueChange = { medicalHistory = it },
                    label = { Text("Medical History", color = Color.White, fontSize = 22.sp) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp),
                    colors = TextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedContainerColor = Color(0x4DFFFFFF),
                        unfocusedContainerColor = Color(0x4DFFFFFF),
                        focusedLabelColor = Color(0xFFFCCC13),
                        unfocusedLabelColor = Color.White,
                        cursorColor = Color(0xFFFCCC13),
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    ),
                    textStyle = LocalTextStyle.current.copy(fontSize = 20.sp),
                    maxLines = 4,
                    shape = RoundedCornerShape(10.dp),
                    placeholder = {
                        Text(
                            text = "Enter your medical history (e.g. hypertension, diabetes)...",
                            color = Color(0x80FFFFFF),
                            fontSize = 18.sp)

                    }
                )
            }
        },

        confirmButton = {
            TextButton(
                onClick = {
                    val newInfo = PersonalInfo(
                        name = name,
                        gender = selectedGender,
                        bloodType = selectedBloodType,
                        medicalHistory = medicalHistory
                    )
                    onSave(newInfo)
                },
                modifier = Modifier.height(60.dp),
                colors = ButtonDefaults.textButtonColors(
                    contentColor = Color(0xFFFCCC13)
                )
            ) {
                Text(
                    text = "Save",
                    fontSize = 30.sp,
                    fontFamily = FontFamily.Serif
                )
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.height(60.dp),
                colors = ButtonDefaults.textButtonColors(
                    contentColor = Color.White
                )
            ) {
                Text(
                    text = "Cancel",
                    fontSize = 30.sp,
                    fontFamily = FontFamily.Serif
                )
            }
        },
        onDismissRequest = onDismiss
    )
}
@Composable
fun SOSContactItem() {
    val context = androidx.compose.ui.platform.LocalContext.current
    var inputNumber by remember { mutableStateOf("") }
    var savedNumber by remember { mutableStateOf(SOS1.getEmergencyPhoneNumber(context)) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0x4DFFFFFF), shape = RoundedCornerShape(20.dp))
            .padding(20.dp)
    ) {
        Column {
            Text(
                text = "Emergency Contact Information",
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Serif
            )

            Spacer(modifier = Modifier.height(12.dp))

            androidx.compose.material3.TextField(
                value = inputNumber,
                onValueChange = { inputNumber = it },
                placeholder = { Text("Enter phone number...", color = Color.Gray) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .clip(RoundedCornerShape(10.dp)),
                singleLine = true,
                colors = androidx.compose.material3.TextFieldDefaults.colors(
                    focusedContainerColor = Color(0x33FFFFFF),
                    unfocusedContainerColor = Color(0x1AFFFFFF),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                )
            )

            Spacer(modifier = Modifier.height(10.dp))


            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (savedNumber.isEmpty()) "No number saved" else "Saved: $savedNumber",
                    color = Color(0xFFFCCC13),
                    fontSize = 14.sp
                )

                androidx.compose.material3.Button(
                    onClick = {
                        if (inputNumber.isNotEmpty()) {
                            SOS1.saveEmergencyPhoneNumber(context, inputNumber)
                            savedNumber = inputNumber
                            inputNumber = ""
                            android.widget.Toast.makeText(context, "Contact Saved!", android.widget.Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = Color(0xFFFCCC13)),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Save", color = Color.Black)
                }
            }
        }
    }
}

@Composable
fun HelpAndFeedbackDialog(
    onDismiss: () -> Unit,
    onSubmit: (String) -> Unit
) {
    var feedbackText by remember { mutableStateOf("") }

    AlertDialog(
        containerColor = Color(0xFF23262D),
        shape = RoundedCornerShape(20.dp),
        title = {
            Text(
                text = "Help & Feedback",
                color = Color.White,
                fontSize = 28.sp,
                fontFamily = FontFamily.Serif
            )
        },

        text = {
            TextField(
                value = feedbackText,
                onValueChange = { feedbackText = it },

                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp),

                colors = TextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedContainerColor = Color(0x4DFFFFFF),
                    unfocusedContainerColor = Color(0x4DFFFFFF),
                    focusedPlaceholderColor = Color(0x80FFFFFF),
                    unfocusedPlaceholderColor = Color(0x80FFFFFF),
                    cursorColor = Color(0xFFFCCC13),
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    disabledIndicatorColor = Color.Transparent
                ),
                placeholder = {
                    Text(
                        text = "Please enter your feedback...",
                        color = Color(0x80FFFFFF),
                        fontSize = 18.sp,
                        fontFamily = FontFamily.Serif
                    )
                },
                maxLines = 5,
                shape = RoundedCornerShape(10.dp)
            )
        },
        confirmButton = {
            TextButton(
                onClick = { onSubmit(feedbackText) },

                colors = ButtonDefaults.textButtonColors(
                    contentColor = Color(0xFFFCCC13)
                )
            ) {
                Text(
                    text = "Submit",
                    fontSize = 20.sp,
                    fontFamily = FontFamily.Serif
                )
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = Color.White
                )
            ) {
                Text(
                    text = "Cancel",
                    fontSize = 20.sp,
                    fontFamily = FontFamily.Serif
                )
            }
        },
        onDismissRequest = onDismiss
    )
}


@Composable
fun SettingsItem(
    title: String,
    onClick: () -> Unit
) {

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(84.dp)
            .background(Color(0x4DFFFFFF), shape = RoundedCornerShape(20.dp))
            .clickable { onClick() }
            .padding(horizontal = 23.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                color = Color.White,
                fontSize = 24.sp,
                fontFamily = FontFamily.Serif
            )
            Icon(
                painter = painterResource(id = R.drawable.ic_chevron_right),
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(32.dp)
            )
        }
    }
}


@Composable
fun BroadcastSpeedSlider() {
    var selectedValue by remember { mutableStateOf(5) }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(84.dp)
            .background(Color(0x4DFFFFFF), shape = RoundedCornerShape(20.dp))
            .padding(horizontal = 23.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Column(
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Broadcast Speed",
                color = Color.White,
                fontSize = 24.sp,
                fontFamily = FontFamily.Serif
            )
            Spacer(modifier = Modifier.height(12.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(40.dp)
                    .clickable {
                        selectedValue = when (selectedValue) {
                            3 -> 5
                            5 -> 8
                            else -> 3
                        }
                    }
            ) {
                Canvas(
                    modifier = Modifier.size(width = 273.dp, height = 8.dp)
                ) {
                    val trackWidth = size.width
                    val trackHeight = size.height
                    val trackY = trackHeight / 2f

                    drawLine(
                        color = Color(0xFFFCCC13),
                        strokeWidth = 3f,
                        start = Offset(0f, trackY),
                        end = Offset(trackWidth, trackY)
                    )

                    listOf(0f, 0.5f, 1f).forEach { ratio ->
                        val x = trackWidth * ratio
                        drawLine(
                            color = Color(0xFFFCCC13),
                            strokeWidth = 2f,
                            start = Offset(x, trackY - 5.dp.toPx()),
                            end = Offset(x, trackY + 5.dp.toPx())
                        )
                    }
                }
                Box(
                    modifier = Modifier
                        .offset(
                            x = when (selectedValue) {
                                3 -> (-10).dp
                                5 -> 126.5.dp
                                else -> 263.dp
                            },
                            y = 4.dp
                        )
                        .size(20.dp)
                        .background(Color.White, shape = CircleShape)
                )
                val labelOffsetY = 20.dp
                Text("3", color = Color.White, fontSize = 20.sp, modifier = Modifier.offset(x = 0.dp, y = labelOffsetY))
                Text("5", color = Color.White, fontSize = 20.sp, modifier = Modifier.offset(x = 136.5.dp, y = labelOffsetY))
                Text("8", color = Color.White, fontSize = 20.sp, modifier = Modifier.offset(x = 273.dp, y = labelOffsetY))
            }
        }
    }
}


@Composable
fun PrivacyPermissionToggle() {
    var isEnabled by remember { mutableStateOf(true) }
    ToggleItem(title = "Privacy Permission", isEnabled = isEnabled) { isEnabled = it }
}


@Composable
fun ToggleItem(
    title: String,
    isEnabled: Boolean,
    onToggle: (Boolean) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(84.dp)
            .background(Color(0x4DFFFFFF), shape = RoundedCornerShape(20.dp))
            .padding(horizontal = 23.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                color = Color.White,
                fontSize = 24.sp,
                fontFamily = FontFamily.Serif
            )
            Box(
                modifier = Modifier
                    .size(width = 109.dp, height = 52.dp)
                    .background(
                        color = Color(0xFFFCCC13),
                        shape = RoundedCornerShape(26.dp)
                    )
                    .clickable { onToggle(!isEnabled) }
            ) {
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .offset(x = if (isEnabled) (-25).dp else 25.dp)
                        .size(41.dp)
                        .background(Color.White, shape = CircleShape)
                )
            }
        }
    }
}