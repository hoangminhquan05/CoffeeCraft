package com.example.dacs3

import android.app.Activity
import android.widget.Toast
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.google.firebase.FirebaseException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ForgotPasswordScreen(navController: NavController) {
    var email by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var otp by remember { mutableStateOf("") }
    var isPhoneInputVisible by remember { mutableStateOf(false) }
    var isOtpVisible by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var canResend by remember { mutableStateOf(false) }
    var countdown by remember { mutableStateOf(60) }
    var userName by remember { mutableStateOf("") }
    var isButtonClicked by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val auth = FirebaseAuth.getInstance()
    val firestore = FirebaseFirestore.getInstance()
    val coroutineScope = rememberCoroutineScope()
    var verificationId by remember { mutableStateOf("") }

    // Animation for button scaling
    val buttonScale by animateFloatAsState(
        targetValue = if (isButtonClicked) 0.95f else 1f,
        animationSpec = tween(durationMillis = 100)
    )

    // Reset button animation
    LaunchedEffect(isButtonClicked) {
        if (isButtonClicked) {
            delay(100)
            isButtonClicked = false
        }
    }

    // Hàm đếm ngược để gửi lại OTP
    LaunchedEffect(canResend) {
        if (canResend) {
            while (countdown > 0) {
                delay(1000)
                countdown--
            }
            canResend = false
            countdown = 60
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White) // fallback nếu ảnh không load được
    ) {
        Image(
            painter = painterResource(id = R.drawable.background_login),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        // Decorative coffee beans in the background
        Image(
            painter = painterResource(id = R.drawable.coffee_login),
            contentDescription = "Coffee beans background",
            modifier = Modifier
                .fillMaxSize()
                .alpha(0.1f)
        )

        Box( // Sử dụng một Box để căn giữa Card
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier
                    .padding(horizontal = 24.dp, vertical = 16.dp)
                    .fillMaxWidth()
                    .shadow(
                        elevation = 24.dp,
                        shape = RoundedCornerShape(24.dp),
                        spotColor = Color(0x806E3C1B)
                    )
                    .clip(RoundedCornerShape(24.dp))
                    .border(2.dp, Color(0xFF6D597A), RoundedCornerShape(24.dp)),
                colors = CardDefaults.cardColors(
                    containerColor = Color.White.copy(alpha = 0.95f)
                )
            ) {
                Column(
                    modifier = Modifier
                        .padding(24.dp)
                        .fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center // Center vertically within the Card
                ) {
                    // Coffee Cup with animation
                    Image(
                        painter = painterResource(id = R.drawable.coffee_login),
                        contentDescription = "Coffee Login Image",
                        modifier = Modifier
                            .size(120.dp)
                            .scale(if (isButtonClicked) 1.05f else 1f)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Title with elegant typography
                    Text(
                        text = "Quên Mật Khẩu",
                        fontSize = 32.sp,
                        color = Color(0xFF6D597A),
                        fontWeight = FontWeight.ExtraBold,
                        textAlign = TextAlign.Center,
                        letterSpacing = 1.sp
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    if (!isPhoneInputVisible) {
                        OutlinedTextField(
                            value = email,
                            onValueChange = { email = it },
                            label = { Text("Email của bạn", color = Color(0xFF6D597A).copy(alpha = 0.8f)) },
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            colors = TextFieldDefaults.outlinedTextFieldColors(
                                focusedBorderColor = Color(0xFF6D597A),
                                unfocusedBorderColor = Color(0xFF6D597A).copy(alpha = 0.5f),
                                focusedLabelColor = Color(0xFF6D597A),
                                unfocusedLabelColor = Color(0xFF6D597A).copy(alpha = 0.5f),
                                cursorColor = Color(0xFF6D597A)
                            )
                        )
                    } else {
                        if (userName.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "User: $userName",
                                color = Color(0xFF6D597A),
                                fontWeight = FontWeight.Bold
                            )
                        }
                        OutlinedTextField(
                            value = phone,
                            onValueChange = { phone = it.filter { char -> char.isDigit() } },
                            label = { Text("Số điện thoại", color = Color(0xFF6D597A).copy(alpha = 0.8f)) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            singleLine = true,
                            colors = TextFieldDefaults.outlinedTextFieldColors(
                                focusedBorderColor = Color(0xFF6D597A),
                                unfocusedBorderColor = Color(0xFF6D597A).copy(alpha = 0.5f),
                                focusedLabelColor = Color(0xFF6D597A),
                                unfocusedLabelColor = Color(0xFF6D597A).copy(alpha = 0.5f),
                                cursorColor = Color(0xFF6D597A)
                            )
                        )
                    }

                    if (isOtpVisible) {
                        Spacer(modifier = Modifier.height(16.dp))
                        OutlinedTextField(
                            value = otp,
                            onValueChange = { otp = it },
                            label = { Text("Nhập OTP", color = Color(0xFF6D597A).copy(alpha = 0.8f)) },
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            colors = TextFieldDefaults.outlinedTextFieldColors(
                                focusedBorderColor = Color(0xFF6D597A),
                                unfocusedBorderColor = Color(0xFF6D597A).copy(alpha = 0.5f),
                                focusedLabelColor = Color(0xFF6D597A),
                                unfocusedLabelColor = Color(0xFF6D597A).copy(alpha = 0.5f),
                                cursorColor = Color(0xFF6D597A)
                            )
                        )

                        Spacer(modifier = Modifier.height(8.dp))
                        if (canResend) {
                            Text(
                                text = "Gửi lại OTP (${countdown}s)",
                                color = Color.Gray,
                                modifier = Modifier.clickable { /* Disabled */ }
                            )
                        } else {
                            Text(
                                text = "Gửi lại OTP",
                                color = Color(0xFF6D597A),
                                modifier = Modifier.clickable {
                                    canResend = true
                                    sendSms(phone, context,
                                        { id, success ->
                                            if (success) verificationId = id
                                        },
                                        { name ->
                                            userName = name
                                        }
                                    )
                                }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Button(
                        onClick = {
                            isButtonClicked = true
                            isLoading = true
                            if (isOtpVisible) {
                                verifyOtp(otp, verificationId, context, phone, navController) {
                                    isLoading = false
                                }
                            } else if (isPhoneInputVisible) {
                                if (!isValidPhone(phone)) {
                                    Toast.makeText(context, "Số điện thoại không hợp lệ", Toast.LENGTH_SHORT).show()
                                    isLoading = false
                                    return@Button
                                }
                                sendSms(phone, context,
                                    { id, success ->
                                        if (success) {
                                            verificationId = id
                                            isOtpVisible = true
                                        }
                                        isLoading = false
                                    },
                                    { name ->
                                        userName = name
                                    }
                                )
                            } else {
                                if (email.isEmpty() || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                                    Toast.makeText(context, "Email không hợp lệ", Toast.LENGTH_SHORT).show()
                                    isLoading = false
                                    return@Button
                                }
                                sendEmail(email, auth) { success ->
                                    if (success) {
                                        coroutineScope.launch {
                                            delay(2000)
                                            navController.navigate("login")
                                        }
                                    }
                                    isLoading = false
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .scale(buttonScale),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.Transparent
                        ),
                        contentPadding = PaddingValues(0.dp),
                        elevation = ButtonDefaults.buttonElevation(
                            defaultElevation = 8.dp,
                            pressedElevation = 4.dp
                        ),
                        enabled = !isLoading
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    brush = Brush.horizontalGradient(
                                        colors = listOf(
                                            Color(0xFF9F86C0),
                                            Color(0xFF6D597A)
                                        )
                                    )
                                )
                                .clip(RoundedCornerShape(16.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(color = Color.White)
                            } else {
                                Text(
                                    text = "Gửi",
                                    fontSize = 18.sp,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}


private fun sendEmail(email: String, auth: FirebaseAuth, callback: (Boolean) -> Unit) {
    auth.sendPasswordResetEmail(email)
        .addOnCompleteListener { task ->
            callback(task.isSuccessful)
        }
}

private fun sendSms(
    phone: String,
    context: android.content.Context,
    callback: (String, Boolean) -> Unit,
    onUserFound: (String) -> Unit
) {
    val formattedPhone = if (phone.startsWith("0")) "+84${phone.substring(1)}" else phone
    FirebaseFirestore.getInstance().collection("uSer")
        .whereEqualTo("soDienThoai", phone)
        .get()
        .addOnSuccessListener { documents ->
            if (documents.isEmpty) {
                Toast.makeText(context, "Không tìm thấy user với số điện thoại này", Toast.LENGTH_SHORT).show()
                callback("", false)
            } else {
                val name = documents.documents[0].getString("name") ?: ""
                onUserFound(name)
                val options = PhoneAuthOptions.newBuilder(FirebaseAuth.getInstance())
                    .setPhoneNumber(formattedPhone)
                    .setTimeout(60L, TimeUnit.SECONDS)
                    .setActivity(context as Activity)
                    .setCallbacks(object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                        override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                            callback(credential.smsCode ?: "", true)
                        }
                        override fun onVerificationFailed(e: FirebaseException) {
                            val errorMsg = when (e) {
                                is FirebaseAuthInvalidCredentialsException -> "Số điện thoại không hợp lệ"
                                is FirebaseTooManyRequestsException -> "Quá nhiều yêu cầu, thử lại sau"
                                else -> "Lỗi xác thực: ${e.message}"
                            }
                            Toast.makeText(context, errorMsg, Toast.LENGTH_LONG).show()
                            callback("", false)
                        }
                        override fun onCodeSent(verificationId: String, token: PhoneAuthProvider.ForceResendingToken) {
                            callback(verificationId, true)
                        }
                    })
                    .build()
                try {
                    PhoneAuthProvider.verifyPhoneNumber(options)
                } catch (e: Exception) {
                    callback("", false)
                    Toast.makeText(context, "Lỗi hệ thống: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
        .addOnFailureListener {
            Toast.makeText(context, "Lỗi khi tìm kiếm user", Toast.LENGTH_SHORT).show()
            callback("", false)
        }
}

private fun verifyOtp(
    otp: String,
    verificationId: String,
    context: android.content.Context,
    phone: String,
    navController: NavController,
    callback: (Boolean) -> Unit
) {
    val credential = PhoneAuthProvider.getCredential(verificationId, otp)
    FirebaseAuth.getInstance().signInWithCredential(credential)
        .addOnCompleteListener { task ->
            if (task.isSuccessful) {
                Toast.makeText(context, "Xác thực thành công!", Toast.LENGTH_SHORT).show()
                navController.navigate("reset_pass/$phone")
            } else {
                Toast.makeText(context, "OTP không hợp lệ hoặc đã hết hạn", Toast.LENGTH_SHORT).show()
            }
            callback(task.isSuccessful)
        }
}

private fun isValidPhone(phone: String): Boolean {
    val pattern = "^0[0-9]{9}\$".toRegex()
    return pattern.matches(phone)
}