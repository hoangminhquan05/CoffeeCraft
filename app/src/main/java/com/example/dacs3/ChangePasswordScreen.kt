package com.example.dacs3

import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.delay
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChangePasswordScreen(navController: NavController) {
    var currentEmail by remember { mutableStateOf("") }
    var otp by remember { mutableStateOf("") }
    var generatedOtp by remember { mutableStateOf("") }
    var currentPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var successMessage by remember { mutableStateOf<String?>(null) }
    var showSuccessDialog by remember { mutableStateOf(false) }
    var currentPasswordVisible by remember { mutableStateOf(false) }
    var newPasswordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }
    var showEmailScreen by remember { mutableStateOf(true) }
    var showOtpScreen by remember { mutableStateOf(false) }
    var showPasswordScreen by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    val auth = Firebase.auth
    val currentUser = auth.currentUser

    // Animation values
    val infiniteTransition = rememberInfiniteTransition()
    val angle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(10000, easing = LinearEasing)
        )
    )
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.9f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    LaunchedEffect(errorMessage) {
        errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            errorMessage = null
        }
    }

    LaunchedEffect(successMessage) {
        successMessage?.let {
            snackbarHostState.showSnackbar(it)
            successMessage = null
        }
    }

    // Success Dialog
    if (showSuccessDialog) {
        AlertDialog(
            onDismissRequest = { /* Disabled */ },
            title = {
                Text(
                    "Thành công!",
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF4E342E)
                )
            },
            text = {
                Column {
                    Text("Mật khẩu đã được thay đổi thành công!")
                    Spacer(modifier = Modifier.height(8.dp))
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .background(
                                color = Color(0xFF8BC34A).copy(alpha = 0.2f),
                                shape = CircleShape
                            )
                            .border(
                                width = 2.dp,
                                color = Color(0xFF8BC34A),
                                shape = CircleShape
                            )
                            .align(Alignment.CenterHorizontally),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "✓",
                            fontSize = 40.sp,
                            color = Color(0xFF8BC34A),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showSuccessDialog = false
                        navController.popBackStack()
                        navController.navigate("home") {
                            popUpTo("home") { inclusive = true }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF6D4C41),
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("OK", fontSize = 16.sp)
                }
            },
            containerColor = Color.White,
            shape = RoundedCornerShape(20.dp)
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F5F5)) // Light skin tone background
    ) {
        // Top right decorative element (coffee cup)
        Box(
            modifier = Modifier
                .size(120.dp)
                .rotate(angle)
                .align(Alignment.TopEnd)
                .offset(x = 30.dp, y = (-30).dp)
                .background(
                    color = Color(0xFF6D4C41).copy(alpha = 0.05f),
                    shape = CircleShape
                )
                .border(
                    width = 1.dp,
                    color = Color(0xFF6D4C41).copy(alpha = 0.1f),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                "☕",
                fontSize = 40.sp,
                color = Color(0xFF6D4C41).copy(alpha = 0.2f)
            )
        }

        // Bottom left decorative element (lock) - now rotates like the coffee cup
        Box(
            modifier = Modifier
                .size(100.dp)
                .rotate(angle) // Same rotation animation as the coffee cup
                .align(Alignment.BottomStart)
                .offset(x = (-20).dp, y = (-20).dp)
                .background(
                    color = Color(0xFF6D4C41).copy(alpha = 0.05f),
                    shape = CircleShape // Changed to CircleShape to match the coffee cup
                )
                .border(
                    width = 1.dp,
                    color = Color(0xFF6D4C41).copy(alpha = 0.1f),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                "🔒",
                fontSize = 30.sp,
                color = Color(0xFF6D4C41).copy(alpha = 0.2f)
            )
        }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            "Đổi Mật Khẩu",
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
                        )
                    },
                    navigationIcon = {
                        IconButton(
                            onClick = { navController.popBackStack() },
                            modifier = Modifier
                                .background(
                                    color = Color(0xFF6D4C41).copy(alpha = 0.1f),
                                    shape = CircleShape
                                )
                                .padding(8.dp)
                        ) {
                            Icon(
                                Icons.Default.ArrowBack,
                                contentDescription = "Quay lại",
                                tint = Color(0xFF6D4C41)
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                        titleContentColor = Color(0xFF4E342E),
                        actionIconContentColor = Color(0xFF4E342E)
                    ),
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            },
            containerColor = Color.Transparent,
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 24.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Top
            ) {
                Spacer(modifier = Modifier.height(20.dp))

                // Header with decorative box instead of icon
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .background(
                            color = Color(0xFF6D4C41).copy(alpha = 0.1f),
                            shape = RoundedCornerShape(20.dp)
                        )
                        .border(
                            width = 1.dp,
                            color = Color(0xFF6D4C41).copy(alpha = 0.2f),
                            shape = RoundedCornerShape(20.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "🔑",
                        fontSize = 40.sp,
                        color = Color(0xFF6D4C41)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (showEmailScreen) {
                    // Email verification screen
                    Text(
                        "Xác thực Email",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF4E342E)
                    )

                    Text(
                        "Vui lòng nhập email đã đăng ký tài khoản",
                        fontSize = 14.sp,
                        color = Color(0xFF6D4C41).copy(alpha = 0.8f),
                        modifier = Modifier.padding(top = 8.dp)
                    )

                    Spacer(modifier = Modifier.height(32.dp))

                    OutlinedTextField(
                        value = currentEmail,
                        onValueChange = { currentEmail = it },
                        label = { Text("Email") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp)),
                        colors = TextFieldDefaults.outlinedTextFieldColors(
                            focusedTextColor = Color(0xFF4E342E),
                            focusedBorderColor = Color(0xFF6D4C41),
                            unfocusedBorderColor = Color(0xFFBCAAA4),
                            cursorColor = Color(0xFF6D4C41)
                        ),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            if (currentEmail.isBlank()) {
                                errorMessage = "Vui lòng nhập email"
                                return@Button
                            }

                            // Validate email format
                            val emailPattern = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\$"
                            if (!currentEmail.matches(emailPattern.toRegex())) {
                                errorMessage = "Định dạng email không hợp lệ"
                                return@Button
                            }

                            if (currentEmail != currentUser?.email) {
                                errorMessage = "Email không khớp với tài khoản đã đăng ký"
                                return@Button
                            }

                            isLoading = true
                            generatedOtp = generateRandomOtp()
                            sendOtpEmail(
                                staffEmail = currentEmail,
                                otp = generatedOtp,
                                onSuccess = {
                                    isLoading = false
                                    successMessage = "Mã OTP đã được gửi đến email của bạn"
                                    showEmailScreen = false
                                    showOtpScreen = true
                                },
                                onError = { error ->
                                    isLoading = false
                                    errorMessage = "Gửi OTP thất bại: $error"
                                }
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .clip(RoundedCornerShape(12.dp)),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF6D4C41),
                            contentColor = Color.White
                        ),
                        enabled = !isLoading
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                color = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        } else {
                            Text(
                                "Gửi OTP",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                } else if (showOtpScreen) {
                    // OTP verification screen
                    Text(
                        "Xác thực OTP",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF4E342E)
                    )

                    Text(
                        "Vui lòng nhập mã OTP đã gửi đến email của bạn",
                        fontSize = 14.sp,
                        color = Color(0xFF6D4C41).copy(alpha = 0.8f),
                        modifier = Modifier.padding(top = 8.dp)
                    )

                    Spacer(modifier = Modifier.height(32.dp))

                    OutlinedTextField(
                        value = otp,
                        onValueChange = { otp = it },
                        label = { Text("Mã OTP") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp)),
                        colors = TextFieldDefaults.outlinedTextFieldColors(
                            focusedTextColor = Color(0xFF4E342E),
                            focusedBorderColor = Color(0xFF6D4C41),
                            unfocusedBorderColor = Color(0xFFBCAAA4),
                            cursorColor = Color(0xFF6D4C41)
                        ),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            if (otp.isBlank()) {
                                errorMessage = "Vui lòng nhập mã OTP"
                                return@Button
                            }

                            if (otp != generatedOtp) {
                                errorMessage = "Mã OTP không chính xác, vui lòng kiểm tra lại"
                                return@Button
                            }

                            showOtpScreen = false
                            showPasswordScreen = true
                            errorMessage = null
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .clip(RoundedCornerShape(12.dp)),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF6D4C41),
                            contentColor = Color.White
                        ),
                        enabled = !isLoading
                    ) {
                        Text(
                            "Xác nhận OTP",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    TextButton(
                        onClick = {
                            showOtpScreen = false
                            showEmailScreen = true
                            otp = ""
                        }
                    ) {
                        Text("Quay lại nhập email")
                    }
                } else if (showPasswordScreen) {
                    // Password change screen
                    Text(
                        "Đổi Mật Khẩu",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF4E342E)
                    )

                    Text(
                        "Vui lòng nhập mật khẩu mới",
                        fontSize = 14.sp,
                        color = Color(0xFF6D4C41).copy(alpha = 0.8f),
                        modifier = Modifier.padding(top = 8.dp)
                    )

                    Spacer(modifier = Modifier.height(32.dp))

                    // Current Password Field
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            "Mật khẩu hiện tại",
                            color = Color(0xFF4E342E).copy(alpha = 0.8f),
                            fontSize = 14.sp,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                        OutlinedTextField(
                            value = currentPassword,
                            onValueChange = { currentPassword = it },
                            visualTransformation = if (currentPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp)),
                            colors = TextFieldDefaults.outlinedTextFieldColors(
                                focusedTextColor = Color(0xFF4E342E),
                                focusedBorderColor = Color(0xFF6D4C41),
                                unfocusedBorderColor = Color(0xFFBCAAA4),
                                cursorColor = Color(0xFF6D4C41)
                            ),
                            trailingIcon = {
                                IconButton(onClick = { currentPasswordVisible = !currentPasswordVisible }) {
                                    Icon(
                                        imageVector = if (currentPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                        contentDescription = if (currentPasswordVisible) "Hide password" else "Show password",
                                        tint = Color(0xFF6D4C41).copy(alpha = 0.6f)
                                    )
                                }
                            },
                            singleLine = true
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // New Password Field
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            "Mật khẩu mới",
                            color = Color(0xFF4E342E).copy(alpha = 0.8f),
                            fontSize = 14.sp,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                        OutlinedTextField(
                            value = newPassword,
                            onValueChange = { newPassword = it },
                            visualTransformation = if (newPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp)),
                            colors = TextFieldDefaults.outlinedTextFieldColors(
                                focusedTextColor = Color(0xFF4E342E),
                                focusedBorderColor = Color(0xFF6D4C41),
                                unfocusedBorderColor = Color(0xFFBCAAA4),
                                cursorColor = Color(0xFF6D4C41)
                            ),
                            trailingIcon = {
                                IconButton(onClick = { newPasswordVisible = !newPasswordVisible }) {
                                    Icon(
                                        imageVector = if (newPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                        contentDescription = if (newPasswordVisible) "Hide password" else "Show password",
                                        tint = Color(0xFF6D4C41).copy(alpha = 0.6f)
                                    )
                                }
                            },
                            singleLine = true
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Confirm Password Field
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            "Nhập lại mật khẩu mới",
                            color = Color(0xFF4E342E).copy(alpha = 0.8f),
                            fontSize = 14.sp,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                        OutlinedTextField(
                            value = confirmPassword,
                            onValueChange = { confirmPassword = it },
                            visualTransformation = if (confirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp)),
                            colors = TextFieldDefaults.outlinedTextFieldColors(
                                focusedTextColor = Color(0xFF4E342E),
                                focusedBorderColor = Color(0xFF6D4C41),
                                unfocusedBorderColor = Color(0xFFBCAAA4),
                                cursorColor = Color(0xFF6D4C41)
                            ),
                            trailingIcon = {
                                IconButton(onClick = { confirmPasswordVisible = !confirmPasswordVisible }) {
                                    Icon(
                                        imageVector = if (confirmPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                        contentDescription = if (confirmPasswordVisible) "Hide password" else "Show password",
                                        tint = Color(0xFF6D4C41).copy(alpha = 0.6f)
                                    )
                                }
                            },
                            singleLine = true
                        )
                    }

                    // Error message
                    errorMessage?.let {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = it,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(8.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(32.dp))

                    // Submit Button
                    Button(
                        onClick = {
                            if (newPassword != confirmPassword) {
                                errorMessage = "Mật khẩu mới không khớp"
                                return@Button
                            }

                            if (newPassword.length < 6) {
                                errorMessage = "Mật khẩu phải có ít nhất 6 ký tự"
                                return@Button
                            }

                            isLoading = true
                            errorMessage = null

                            val credential = EmailAuthProvider
                                .getCredential(currentUser?.email ?: "", currentPassword)

                            currentUser?.reauthenticate(credential)?.addOnCompleteListener { reauthTask ->
                                if (reauthTask.isSuccessful) {
                                    currentUser.updatePassword(newPassword).addOnCompleteListener { updateTask ->
                                        isLoading = false
                                        if (updateTask.isSuccessful) {
                                            currentPassword = ""
                                            newPassword = ""
                                            confirmPassword = ""
                                            showSuccessDialog = true
                                        } else {
                                            errorMessage = "Đổi mật khẩu thất bại: ${updateTask.exception?.message}"
                                        }
                                    }
                                } else {
                                    isLoading = false
                                    errorMessage = "Mật khẩu hiện tại không đúng"
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .clip(RoundedCornerShape(12.dp)),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF6D4C41),
                            contentColor = Color.White
                        ),
                        enabled = !isLoading && currentPassword.isNotEmpty()
                                && newPassword.isNotEmpty() && confirmPassword.isNotEmpty()
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                color = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        } else {
                            Text(
                                "Đổi Mật Khẩu",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Footer decoration
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 32.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    repeat(5) {
                        Text(
                            "•",
                            fontSize = 20.sp,
                            color = Color(0xFF6D4C41).copy(alpha = 0.2f),
                            modifier = Modifier.padding(horizontal = 4.dp)
                        )
                    }
                }
            }
        }
    }
}

private fun generateRandomOtp(): String {
    val chars = ('0'..'9')
    return (1..6).map { chars.random() }.joinToString("")
}

private fun sendOtpEmail(
    staffEmail: String,
    otp: String,
    onSuccess: () -> Unit,
    onError: (String) -> Unit
) {
    val client = OkHttpClient()
    val jsonMediaType = "application/json".toMediaType()
    val privateKey = "o-NRyKKQlugn8hKlsQLbv" // Thay bằng key của bạn

    val requestBody = """
        {
            "service_id": "service_g1xvqgh",
            "template_id": "template_u5hihka",
            "user_id": "o-UJ44uTePQoULWnP",
            "template_params": {
                "to_email": "$staffEmail",
                "name": "$staffEmail",
                "otp": "$otp"
            }
        }
    """.trimIndent()

    val request = Request.Builder()
        .url("https://api.emailjs.com/api/v1.0/email/send")
        .post(requestBody.toRequestBody(jsonMediaType))
        .addHeader("Content-Type", "application/json")
        .addHeader("Authorization", "Bearer $privateKey")
        .addHeader("Origin", "http://yourdomain.com")
        .build()

    client.newCall(request).enqueue(object : Callback {
        override fun onFailure(call: Call, e: IOException) {
            onError("Lỗi kết nối: ${e.message}")
        }

        override fun onResponse(call: Call, response: Response) {
            if (!response.isSuccessful) {
                val errorBody = response.body?.string() ?: "Không có thông tin lỗi"
                onError("Mã lỗi: ${response.code}. Chi tiết: $errorBody")
            } else {
                onSuccess()
            }
        }
    })
}