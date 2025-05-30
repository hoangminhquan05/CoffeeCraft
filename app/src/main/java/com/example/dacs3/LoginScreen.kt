package com.example.dacs3


import android.widget.Toast
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import kotlinx.coroutines.delay


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(navController: NavController, auth: FirebaseAuth, onLoginSuccess: (String) -> Unit) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var emailError by remember { mutableStateOf<String?>(null) }
    var passwordError by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current
    var isButtonClicked by remember { mutableStateOf(false) }


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


    Box(
        modifier = Modifier
            .fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        // Ảnh nền thay thế cho gradient
        Image(
            painter = painterResource(id = R.drawable.background_login), // Thay bằng ID ảnh nền bạn có
            contentDescription = "Background Image",
            modifier = Modifier
                .fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        // Lớp phủ mờ (nếu muốn làm mờ ảnh nền)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White.copy(alpha = 0.2f)) // Tùy chỉnh độ mờ
        )

        // Nội dung card đăng nhập giữ nguyên
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
                    .padding(24.dp)  // Reduced padding to make the form smaller
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Coffee Cup without frame
                Image(
                    painter = painterResource(id = R.drawable.coffee_login),
                    contentDescription = "Coffee Login Image",
                    modifier = Modifier
                        .size(120.dp)  // Slightly smaller image
                        .scale(if (isButtonClicked) 1.05f else 1f)
                )


                Spacer(modifier = Modifier.height(16.dp))  // Reduced spacing


                // Title with elegant typography
                Text(
                    text = "Coffee Craft",
                    fontSize = 32.sp,  // Slightly smaller font
                    color = Color(0xFF6D597A),  // Soft purple
                    fontWeight = FontWeight.ExtraBold,
                    textAlign = TextAlign.Center,
                    letterSpacing = 1.sp
                )


                Text(
                    text = "Đăng nhập để khám phá thế giới cà phê",
                    fontSize = 14.sp,
                    color = Color.Black,  // Changed to black
                    modifier = Modifier.padding(top = 4.dp)
                )


                Spacer(modifier = Modifier.height(24.dp))  // Reduced spacing


                // Email TextField
                OutlinedTextField(
                    value = email,
                    onValueChange = {
                        email = it
                        emailError = null
                    },
                    label = {
                        Text("Email",
                            color = Color(0xFF6D597A).copy(alpha = 0.8f))
                    },
                    leadingIcon = {
                        Icon(
                            Icons.Filled.Email,
                            contentDescription = "Email Icon",
                            tint = Color(0xFF6D597A)
                        )
                    },
                    isError = emailError != null,
                    supportingText = {
                        emailError?.let {
                            Text(it, color = MaterialTheme.colorScheme.error)
                        }
                    },
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


                Spacer(modifier = Modifier.height(12.dp))  // Reduced spacing


                // Password TextField
                OutlinedTextField(
                    value = password,
                    onValueChange = {
                        password = it
                        passwordError = null
                    },
                    label = {
                        Text("Mật khẩu",
                            color = Color(0xFF6D597A).copy(alpha = 0.8f))
                    },
                    leadingIcon = {
                        Icon(
                            Icons.Filled.Lock,
                            contentDescription = "Password Icon",
                            tint = Color(0xFF6D597A)
                        )
                    },
                    visualTransformation = PasswordVisualTransformation(),
                    isError = passwordError != null,
                    supportingText = {
                        passwordError?.let {
                            Text(it, color = MaterialTheme.colorScheme.error)
                        }
                    },
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


                Spacer(modifier = Modifier.height(24.dp))  // Reduced spacing


                // Login Button with gradient and animation
                Button(
                    onClick = {
                        isButtonClicked = true
                        emailError = null
                        passwordError = null


                        when {
                            email.isBlank() -> emailError = "Vui lòng nhập email!"
                            password.isBlank() -> passwordError = "Vui lòng nhập mật khẩu!"
                            !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches() -> {
                                emailError = "Vui lòng nhập email hợp lệ!"
                            }
                            else -> {
                                auth.signInWithEmailAndPassword(email, password)
                                    .addOnCompleteListener { task ->
                                        if (task.isSuccessful) {
                                            val user = auth.currentUser
                                            user?.let {
                                                Toast.makeText(
                                                    context,
                                                    "☕ Đăng nhập thành công!",
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                                onLoginSuccess(it.uid)
                                            }
                                        } else {
                                            val errorMessage = when (task.exception) {
                                                is FirebaseAuthInvalidCredentialsException ->
                                                    "Email hoặc mật khẩu không đúng!"
                                                is FirebaseAuthUserCollisionException ->
                                                    "Tài khoản đã tồn tại!"
                                                is FirebaseAuthException ->
                                                    "Lỗi đăng nhập: ${task.exception?.message}"
                                                else -> "Đăng nhập thất bại!"
                                            }
                                            Toast.makeText(
                                                context,
                                                errorMessage,
                                                Toast.LENGTH_LONG
                                            ).show()
                                        }
                                    }
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
                    )
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                brush = Brush.horizontalGradient(
                                    colors = listOf(
                                        Color(0xFF9F86C0),  // Soft purple
                                        Color(0xFF6D597A)   // Muted purple
                                    )
                                )
                            )
                            .clip(RoundedCornerShape(16.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "ĐĂNG NHẬP",
                            fontSize = 18.sp,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                    }
                }


                Spacer(modifier = Modifier.height(16.dp))  // Reduced spacing


                // Forgot Password Link with elegant styling
                Text(
                    text = "Quên mật khẩu?",
                    color = Color.Black,  // Changed to black
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier
                        .clickable {
                            navController.navigate("forgot_pass")
                        }
                        .padding(8.dp)
                )


                // Removed the "hoặc" divider section completely
            }
        }
    }
}

