package com.example.dacs3

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraEnhance
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import com.cloudinary.Cloudinary
import com.cloudinary.android.MediaManager
import com.cloudinary.android.callback.ErrorInfo
import com.cloudinary.android.callback.UploadCallback
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditUserInfo(navController: NavController, userId: String?, cloudinary: Cloudinary) {
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var avtUrl by remember { mutableStateOf("") }
    var imageUri by remember { mutableStateOf<Uri?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isUploading by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }
    var showSnackbar by remember { mutableStateOf(false) }
    var snackbarMessage by remember { mutableStateOf("") }

    val scope = rememberCoroutineScope()
    val firestore = FirebaseFirestore.getInstance()

    fun loadUserData() {
        if (userId == null) {
            errorMessage = "Không có ID người dùng"
            snackbarMessage = errorMessage!!
            showSnackbar = true
            isLoading = false
            return
        }

        firestore.collection("uSer").document(userId).get()
            .addOnSuccessListener { doc ->
                if (doc.exists()) {
                    name = doc.getString("name") ?: ""
                    phone = doc.getString("soDienThoai") ?: ""
                    avtUrl = doc.getString("avtUrl") ?: ""
                    snackbarMessage = "Thông tin người dùng đã được tải"
                } else {
                    errorMessage = "Không tìm thấy thông tin người dùng"
                    snackbarMessage = errorMessage!!
                }
                showSnackbar = true
                isLoading = false
            }
            .addOnFailureListener {
                errorMessage = "Lỗi khi tải dữ liệu: ${it.message}"
                snackbarMessage = errorMessage!!
                showSnackbar = true
                isLoading = false
            }
    }

    LaunchedEffect(true) { loadUserData() }

    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            imageUri = it
            snackbarMessage = "Ảnh đã được chọn"
            showSnackbar = true
        }
    }

    LaunchedEffect(showSnackbar) {
        if (showSnackbar) {
            snackbarHostState.showSnackbar(snackbarMessage)
            showSnackbar = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Chỉnh Sửa Thông Tin", color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF6200EA))
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            when {
                isLoading -> CircularProgressIndicator()
                errorMessage != null -> {
                    Text(errorMessage!!, color = Color.Red)
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = {
                        isLoading = true
                        errorMessage = null
                        loadUserData()
                    }) {
                        Text("Thử lại")
                    }
                }
                else -> {
                    Box(contentAlignment = Alignment.BottomEnd, modifier = Modifier.size(120.dp)) {
                        Image(
                            painter = rememberImagePainter(
                                data = imageUri ?: avtUrl,
                                builder = {
                                    crossfade(true)
                                    error(R.drawable.user_df)
                                }
                            ),
                            contentDescription = "Avatar",
                            modifier = Modifier
                                .size(120.dp)
                                .clip(CircleShape)
                                .background(Color.LightGray)
                        )

                        Icon(
                            Icons.Default.CameraEnhance,
                            contentDescription = "Change Avatar",
                            tint = Color.White,
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Color(0x80000000))
                                .clickable { imagePicker.launch("image/*") }
                                .padding(6.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                    OutlinedTextField(name, { name = it }, label = { Text("Tên") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(phone, { phone = it }, label = { Text("Số điện thoại") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    Spacer(modifier = Modifier.height(24.dp))

                    Button(
                        onClick = {
                            if (name.isBlank()) {
                                snackbarMessage = "Vui lòng điền đầy đủ thông tin"
                                showSnackbar = true
                                return@Button
                            }

                            if (imageUri != null) {
                                isUploading = true
                                uploadToCloudinary(
                                    uri = imageUri!!,
                                    userId = userId,
                                    oldAvtUrl = avtUrl,
                                    onSuccess = { url ->
                                        updateUserInfo(userId, name, phone, url,
                                            onSuccess = {
                                                snackbarMessage = "Cập nhật thành công"
                                                showSnackbar = true
                                                navController.popBackStack()
                                            },
                                            onFailure = {
                                                snackbarMessage = "Lỗi cập nhật: $it"
                                                showSnackbar = true
                                                isUploading = false
                                            }
                                        )
                                    },
                                    onFailure = {
                                        snackbarMessage = "Lỗi tải ảnh: $it"
                                        showSnackbar = true
                                        isUploading = false
                                    }
                                )
                            } else {
                                updateUserInfo(userId, name, phone, avtUrl,
                                    onSuccess = {
                                        snackbarMessage = "Cập nhật thành công"
                                        showSnackbar = true
                                        navController.popBackStack()
                                    },
                                    onFailure = {
                                        snackbarMessage = "Lỗi cập nhật: $it"
                                        showSnackbar = true
                                    }
                                )
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        enabled = !isUploading,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6200EA))
                    ) {
                        if (isUploading) CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White)
                        else Text("Lưu Thay Đổi", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// ----------------------------
// Helper functions
// ----------------------------

private fun uploadToCloudinary(
    uri: Uri,
    userId: String?,
    oldAvtUrl: String?,
    onSuccess: (String) -> Unit,
    onFailure: (String) -> Unit
) {
    val publicId = "user_${userId ?: System.currentTimeMillis()}"
    deleteOldImage(publicId) {
        MediaManager.get().upload(uri)
            .option("folder", "avt_user")
            .option("public_id", publicId)
            .callback(object : UploadCallback {
                override fun onSuccess(requestId: String, resultData: Map<Any?, Any?>) {
                    onSuccess(resultData["secure_url"].toString())
                }

                override fun onError(requestId: String, error: ErrorInfo) {
                    onFailure(error.description ?: "Không xác định")
                }

                override fun onProgress(requestId: String, bytes: Long, totalBytes: Long) {}
                override fun onReschedule(requestId: String, error: ErrorInfo) {}
                override fun onStart(requestId: String) {}
            })
            .dispatch()
    }
}

private fun deleteOldImage(publicId: String, onSuccess: () -> Unit) {
    try {
        val cloudinary = MediaManager.get().cloudinary
        cloudinary.uploader().destroy("avt_user/$publicId", mapOf("invalidate" to true))
    } catch (e: Exception) {
        e.printStackTrace() // Ghi log nếu có lỗi
    } finally {
        onSuccess() // Luôn gọi callback dù có lỗi hay không
    }
}


private fun updateUserInfo(
    userId: String?,
    name: String,
    phone: String,
    avtUrl: String,
    onSuccess: () -> Unit,
    onFailure: (String) -> Unit
) {
    if (userId == null) {
        onFailure("Không có ID người dùng")
        return
    }

    val updates = mapOf(
        "name" to name,
        "soDienThoai" to phone,
        "avtUrl" to avtUrl
    )

    // Update Firestore document
    FirebaseFirestore.getInstance().collection("uSer").document(userId)
        .update(updates)
        .addOnSuccessListener {
            onSuccess() // Notify success if update succeeded
        }
        .addOnFailureListener { onFailure(it.message ?: "Lỗi không xác định") }
}
