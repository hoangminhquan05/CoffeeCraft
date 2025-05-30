package com.example.dacs3

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Password
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ProductionQuantityLimits
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.StackedBarChart
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.filled.Collections
import androidx.compose.material.icons.filled.TableBar
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import coil.compose.rememberImagePainter
import com.google.firebase.firestore.FirebaseFirestore

@Composable
fun ProfileScreen(navController: NavController, onLogout: () -> Unit, userId: String? = null) {
    // State for user data
    var userData by remember { mutableStateOf<Map<String, String>?>(null) }
    var role by remember { mutableStateOf<String?>(null) }
    var avtUrl by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // State for logout confirmation dialog
    var showLogoutDialog by remember { mutableStateOf(false) }

    // Initialize Firestore
    val firestore = FirebaseFirestore.getInstance()

    // Fetch user data when userId changes
    LaunchedEffect(userId) {
        if (userId != null) {
            try {
                firestore.collection("uSer").document(userId).get()
                    .addOnSuccessListener { document ->
                        if (document.exists()) {
                            userData = document.data as? Map<String, String>
                            role = document.getString("role")
                            avtUrl = document.getString("avtUrl")
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

    Box(modifier = Modifier.fillMaxSize().background(Color(0xFFF5F5F5))) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
                .padding(bottom = 70.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Display UID if available
            userId?.let {
                Text(
                    text = "User ID: $it",
                    fontSize = 14.sp,
                    color = Color.Gray,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            // Display avatar if available
            avtUrl?.let {
                Image(
                    painter = rememberImagePainter(it),
                    contentDescription = "Avatar",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(150.dp)
                        .clip(CircleShape)
                        .background(Color.Gray)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Coffee Craft",
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF6D4C41)
            )

            // Display role (admin or staff)
            Text(
                text = role ?: "Đang tải dữ liệu...",
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
                color = if (role == "ADMIN") Color.Red else Color.Green
            )

            Spacer(modifier = Modifier.height(16.dp))

            val menuItems = mutableListOf(
                MenuItem("Thông tin tài khoản", Icons.Filled.Person) {
                    userId?.let { uid ->
                        navController.navigate("thongtincanhan/$uid")
                    }
                },
                MenuItem("Đổi mật khẩu", Icons.Filled.Password) {
                        navController.navigate("change_password")
                },
                        MenuItem("Quản lý bàn", Icons.Filled.TableBar) {
                navController.navigate("table_management")
            }
            )

            if (role == "ADMIN") {
                menuItems.addAll(
                    listOf(
                        MenuItem("Quản lý sản phẩm", Icons.Filled.ProductionQuantityLimits) {
                            navController.navigate("adminproduct")
                        },
                        MenuItem("Quản lý nhân viên", Icons.Filled.Person) {
                            navController.navigate("staff_management")
                        },
                        MenuItem("Quản lý banner", Icons.Filled.Collections) {
                            navController.navigate("bannerManagement")
                    }
                    )
                )
            }

            menuItems.addAll(
                listOf(
                    MenuItem("Thống kê doanh thu", Icons.Filled.StackedBarChart) {
                        navController.navigate("statistics")
                    },
                    MenuItem("Quản lý bàn", Icons.Filled.TableBar) {
                        navController.navigate("table_management")
                    },
                    MenuItem("Đăng Xuất", Icons.Filled.ExitToApp) {
                        showLogoutDialog = true
                    }
                )
            )

            menuItems.forEach { item ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                        .clickable { item.onClick() }
                        .border(1.dp, Color.Black, RoundedCornerShape(12.dp)),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    ListItem(
                        headlineContent = {
                            Text(
                                item.title,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF4E342E)
                            )
                        },
                        leadingContent = {
                            Icon(
                                item.icon,
                                contentDescription = null,
                                tint = Color(0xFF6D4C41)
                            )
                        },
                        modifier = Modifier.padding(8.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.weight(1f))
        }
        BottomNavigationBar(navController, Modifier.align(Alignment.BottomCenter))
    }

    // Logout confirmation dialog
    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text("Xác nhận đăng xuất") },
            text = { Text("Bạn có chắc chắn muốn đăng xuất không?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showLogoutDialog = false
                        onLogout()
                    }
                ) {
                    Text("Chắc chắn", color = Color.Red)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showLogoutDialog = false }
                ) {
                    Text("Hủy")
                }
            }
        )
    }
}

data class MenuItem(
    val title: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val onClick: () -> Unit
)

@Composable
fun BottomNavigationBar(navController: NavController, modifier: Modifier = Modifier) {
    val currentDestination = navController.currentBackStackEntry?.destination?.route

    NavigationBar(modifier = modifier.fillMaxWidth().background(Color.White)) {
        NavigationBarItem(
            icon = { Icon(Icons.Filled.Home, contentDescription = "Trang Chủ") },
            label = { Text("Trang Chủ") },
            selected = currentDestination == "home",
            onClick = { navController.navigate("home") }
        )
        NavigationBarItem(
            icon = { Icon(Icons.Filled.ProductionQuantityLimits, contentDescription = "Sản Phẩm") },
            label = { Text("Sản Phẩm") },
            selected = currentDestination == "product",
            onClick = { navController.navigate("product") }
        )
        NavigationBarItem(
            icon = { Icon(Icons.Filled.ShoppingCart, contentDescription = "Giỏ Hàng") },
            label = { Text("Giỏ Hàng") },
            selected = currentDestination == "cart",
            onClick = { navController.navigate("cart") }
        )
        NavigationBarItem(
            icon = { Icon(Icons.Filled.Person, contentDescription = "Tài Khoản") },
            label = { Text("Tài Khoản") },
            selected = currentDestination == "profile",
            onClick = { navController.navigate("profile") }
        )
    }
}

