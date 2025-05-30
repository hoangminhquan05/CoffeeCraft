package com.example.dacs3

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.userProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import com.example.dacs3.Model.User
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddStaffScreen(navController: NavController) {
    var name by remember { mutableStateOf("") }
    var role by remember { mutableStateOf("Nhân Viên") }
    var phone by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var successMessage by remember { mutableStateOf<String?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(successMessage) {
        successMessage?.let {
            snackbarHostState.showSnackbar(it)
            kotlinx.coroutines.delay(1000)
            navController.popBackStack()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Thêm nhân viên mới") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Quay lại")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Tên nhân viên") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = role,
                onValueChange = { role = it },
                label = { Text("Chức vụ") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = phone,
                onValueChange = { phone = it },
                label = { Text("Số điện thoại") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                modifier = Modifier.fillMaxWidth()
            )

            // Hiển thị lỗi
            errorMessage?.let {
                Text(
                    text = it,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
            }

            Button(
                onClick = {
                    if (validateInput(name, role, phone, email)) {
                        val password = generateRandomPassword()
                        addStaffToFirebase(
                            name = name,
                            role = role,
                            phone = phone,
                            email = email,
                            password = password,
                            onSuccess = {
                                sendWelcomeEmail(
                                    staffEmail = email,
                                    staffName = name,
                                    tempPassword = password,
                                    onSuccess = {
                                        successMessage = "Thêm nhân viên thành công! Mật khẩu đã gửi đến email $email"
                                    },
                                    onError = { error ->
                                        errorMessage = "Thêm nhân viên thành công nhưng gửi email thất bại: $error"
                                    }
                                )
                            },
                            onError = { error -> errorMessage = error },
                            isLoading = { loading -> isLoading = loading }
                        )
                    } else {
                        errorMessage = "Vui lòng điền đầy đủ thông tin"
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                enabled = !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.onPrimary)
                } else {
                    Text("Thêm nhân viên")
                }
            }
        }
    }
}


// lỗi 403)
private fun validateInput(name: String, role: String, phone: String, email: String): Boolean {
    return name.isNotBlank() && role.isNotBlank() && phone.isNotBlank() && email.isNotBlank()
}

private fun addStaffToFirebase(
    name: String,
    role: String,
    phone: String,
    email: String,
    password: String,
    onSuccess: () -> Unit,
    onError: (String) -> Unit,
    isLoading: (Boolean) -> Unit
) {
    isLoading(true)

    FirebaseAuth.getInstance().createUserWithEmailAndPassword(email, password)
        .addOnCompleteListener { authTask ->
            if (authTask.isSuccessful) {
                val uid = authTask.result.user?.uid ?: ""

                val profileUpdates = userProfileChangeRequest {
                    displayName = name
                }

                authTask.result.user?.updateProfile(profileUpdates)
                    ?.addOnCompleteListener { profileTask ->
                        if (profileTask.isSuccessful) {
                            val user = User(
                                uid = uid,
                                avtUrl = "",
                                email = email,
                                name = name,
                                role = role,
                                soDienThoai = phone
                            )

                            FirebaseFirestore.getInstance()
                                .collection("uSer")
                                .document(uid)
                                .set(user)
                                .addOnCompleteListener { firestoreTask ->
                                    isLoading(false)
                                    if (firestoreTask.isSuccessful) {
                                        onSuccess()
                                    } else {
                                        onError(firestoreTask.exception?.message ?: "Lỗi khi lưu thông tin nhân viên")
                                    }
                                }
                        } else {
                            isLoading(false)
                            onError(profileTask.exception?.message ?: "Lỗi khi cập nhật thông tin người dùng")
                        }
                    }
            } else {
                isLoading(false)
                onError(authTask.exception?.message ?: "Lỗi khi tạo tài khoản")
            }
        }
}

private fun generateRandomPassword(): String {
    val chars = ('a'..'z') + ('A'..'Z') + ('0'..'9')
    return (1..6).map { chars.random() }.joinToString("")
}

private fun sendWelcomeEmail(
    staffEmail: String,
    staffName: String,
    tempPassword: String,
    onSuccess: () -> Unit,
    onError: (String) -> Unit
) {
    val client = OkHttpClient()
    val jsonMediaType = "application/json".toMediaType()
    val privateKey = "o-NRyKKQlugn8hKlsQLbv"

    val requestBody = """
        {
            "service_id": "service_g1xvqgh",
            "template_id": "template_lwn0w6b",
            "user_id": "o-UJ44uTePQoULWnP",
            "template_params": {
                "to_email": "$staffEmail",
                "name": "$staffName",
                "password": "$tempPassword"
            }
        }
    """.trimIndent()

    val request = Request.Builder()
        .url("https://api.emailjs.com/api/v1.0/email/send")
        .post(requestBody.toRequestBody(jsonMediaType))
        .addHeader("Content-Type", "application/json")
        .addHeader("Authorization", "Bearer $privateKey")
        .addHeader("Origin", "http://yourdomain.com") // chua thay domain
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