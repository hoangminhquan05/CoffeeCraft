package com.example.dacs3

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.rememberImagePainter
import com.google.firebase.firestore.FirebaseFirestore

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThongTinCaNhan(navController: NavController, userId: String?) {
    // Khai báo state để lưu thông tin người dùng
    var userData by remember { mutableStateOf<Map<String, String>?>(null) }
    var avtUrl by remember { mutableStateOf<String?>(null) } // Avatar URL state
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Khởi tạo Firestore
    val firestore = FirebaseFirestore.getInstance()

    // Truy vấn dữ liệu khi userId thay đổi
    LaunchedEffect(userId) {
        if (userId != null) {
            try {
                firestore.collection("uSer").document(userId).get()
                    .addOnSuccessListener { document ->
                        if (document.exists()) {
                            userData = document.data as? Map<String, String>
                            avtUrl = document.getString("avtUrl") // Lấy URL ảnh avatar
                        } else {
                            errorMessage = "Không tìm thấy thông tin người dùng"
                        }
                        isLoading = false
                    }
                    .addOnFailureListener { e ->
                        errorMessage = "Lỗi khi tải dữ liệu: ${e.message}"
                        isLoading = false
                    }
            } catch (e: Exception) {
                errorMessage = "Lỗi: ${e.message}"
                isLoading = false
            }
        } else {
            errorMessage = "Không có ID người dùng"
            isLoading = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Thông Tin Cá Nhân", color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF6200EA)
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            when {
                isLoading -> {
                    CircularProgressIndicator()
                }
                errorMessage != null -> {
                    Text(
                        text = errorMessage!!,
                        color = Color.Red,
                        modifier = Modifier.padding(16.dp)
                    )
                }
                userData != null -> {
                    // Hiển thị avatar nếu có URL
                    avtUrl?.let {
                        Image(
                            painter = rememberImagePainter(it), // Fetch the image from URL
                            contentDescription = "Avatar",
                            modifier = Modifier
                                .size(120.dp)
                                .clip(CircleShape)
                                .background(Color.LightGray)
                        )
                    } ?: run {
                        // Hiển thị hình ảnh mặc định nếu không có URL
                        Image(
                            painter = painterResource(id = R.drawable.coffee_login),
                            contentDescription = "Avatar",
                            modifier = Modifier
                                .size(120.dp)
                                .clip(CircleShape)
                                .background(Color.LightGray)
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Tên
                    Text(
                        text = userData?.get("name") ?: "Không có tên",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Email
                    InfoCard(
                        title = "Email",
                        value = userData?.get("email") ?: "Không có email"
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Số điện thoại
                    InfoCard(
                        title = "Số điện thoại",
                        value = userData?.get("soDienThoai") ?: "Không có số điện thoại"
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Chức vụ (role)
                    InfoCard(
                        title = "Chức vụ",
                        value = when (userData?.get("role")) {
                            "Nhân Viên" -> "Nhân viên"
                            "ADMIN" -> "Quản lý"
                            else -> "Không xác định"
                        }
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Button(
                        onClick = {
                            navController.navigate("editUserInfo/${userId}")
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6200EA))
                    ) {
                        Text(
                            text = "Chỉnh sửa thông tin",
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun InfoCard(title: String, value: String) {
    Card(
        modifier = Modifier
            .fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFF5F5F5)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                fontSize = 14.sp,
                color = Color.Gray
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}
