package com.example.dacs3


import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.cloudinary.Cloudinary
import com.example.dacs3.Model.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StaffManagementScreen(navController: NavController, cloudinary: Cloudinary) {
    var staffList by remember { mutableStateOf<List<User>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    val firestore = FirebaseFirestore.getInstance()
    var showDeleteDialog by remember { mutableStateOf(false) }
    var userToDelete by remember { mutableStateOf<String?>(null) }


    // Hàm xóa nhân viên
    fun deleteStaff(uid: String) {
        firestore.collection("uSer").document(uid)
            .delete()
            .addOnSuccessListener {
                val auth = FirebaseAuth.getInstance()

                auth.currentUser?.let { currentUser ->
                    if (currentUser.uid == uid) {
                        currentUser.delete()
                            .addOnSuccessListener {
                                staffList = staffList.filter { it.uid != uid }
                            }
                            .addOnFailureListener { e ->
                                Log.e("Auth", "Error deleting user from Auth: ${e.message}")
                                staffList = staffList.filter { it.uid != uid }
                            }
                    } else {


                        staffList = staffList.filter { it.uid != uid }
                        Log.w("Auth", "Need admin privileges to delete other users")
                    }
                }
            }
            .addOnFailureListener { e ->
                Log.e("Firestore", "Error deleting staff: ${e.message}")
            }
    }


    // Hàm load lại danh sách nhân viên
    fun loadStaffList() {
        isLoading = true
        firestore.collection("uSer")
            .whereEqualTo("role", "Nhân Viên")
            .get()
            .addOnSuccessListener { documents ->
                staffList = documents.mapNotNull { it.toObject(User::class.java) }
                isLoading = false
            }
            .addOnFailureListener {
                isLoading = false
                Log.e("Firestore", "Lỗi: ${it.message}")
            }
    }


    // Xác nhận xóa dialog
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Xác nhận xóa") },
            text = { Text("Bạn có chắc chắn muốn xóa nhân viên này?") },
            confirmButton = {
                Button(
                    onClick = {
                        userToDelete?.let { deleteStaff(it) }
                        showDeleteDialog = false
                    }
                ) {
                    Text("Xóa")
                }
            },
            dismissButton = {
                Button(
                    onClick = { showDeleteDialog = false }
                ) {
                    Text("Hủy")
                }
            }
        )
    }


    LaunchedEffect(Unit) {
        loadStaffList()
    }


    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Quản lý nhân viên") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Quay lại")
                    }
                }
            )
        },
        bottomBar = {
            Button(
                onClick = { navController.navigate("add_staff") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text("Thêm nhân viên")
            }
        }
    ) { padding ->
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .padding(padding)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(staffList) { user ->
                    StaffItem(
                        user = user,
                        onDelete = { uid ->
                            userToDelete = uid
                            showDeleteDialog = true
                        },
                        navController = navController
                    )
                    Divider()
                }
            }
        }
    }
}


@Composable
fun StaffItem(user: User, onDelete: (String) -> Unit, navController: NavController) {
    var showMenu by remember { mutableStateOf(false) }


    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        onClick = { showMenu = true }
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = user.avtUrl.ifEmpty { "https://example.com/default_avatar.png" },
                contentDescription = "Avatar",
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop
            )


            Spacer(modifier = Modifier.width(16.dp))


            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = user.name,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = "Email: ${user.email}", fontSize = 14.sp)
                Text(text = "SĐT: ${user.soDienThoai}", fontSize = 14.sp)
                Text(
                    text = "Vai trò: ${user.role}",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.primary
                )
            }


            // Dropdown menu
            Box {
                IconButton(onClick = { showMenu = true }) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "Menu"
                    )
                }


                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
//                    DropdownMenuItem(
//                        text = { Text("Chỉnh sửa") },
//                        onClick = {
//                            showMenu = false
//                            navController.navigate("edit_staff/${user.uid}")
//                        }
//                    )
                    DropdownMenuItem(
                        text = { Text("Xóa") },
                        onClick = {
                            showMenu = false
                            onDelete(user.uid)
                        }
                    )
                }
            }
        }
    }
}

