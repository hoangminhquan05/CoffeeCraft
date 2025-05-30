package com.example.dacs3

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.cloudinary.Cloudinary
import com.cloudinary.android.MediaManager
import com.cloudinary.android.callback.ErrorInfo
import com.cloudinary.android.callback.UploadCallback
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProductScreen(navController: NavController, productId: String?, cloudinary: Cloudinary) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // State for form fields
    var productName by remember { mutableStateOf("") }
    var productPrice by remember { mutableStateOf("") }
    var productDescription by remember { mutableStateOf("") }
    var productCategory by remember { mutableStateOf("Sữa") }
    var imageUri by remember { mutableStateOf<Uri?>(null) }
    var currentImageUrl by remember { mutableStateOf("") }
    var isUploading by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(true) }

    // Khởi tạo Cloudinary
//    LaunchedEffect(Unit) {
//        val config = mapOf(
//            "cloud_name" to "draj7ffsn",
//            "api_key" to "761479597976773",
//            "api_secret" to "IjlgioCJixcyvRutJwPGvX5QHjo",
//            "secure" to true
//        )
//        MediaManager.init(context, config)
//    }

    // Load product data
    LaunchedEffect(productId) {
        if (productId != null) {
            FirebaseFirestore.getInstance().collection("sanPham")
                .document(productId)
                .get()
                .addOnSuccessListener { document ->
                    document?.let {
                        productName = it.getString("name") ?: ""
                        productPrice = it.getDouble("price")?.toString() ?: ""
                        productDescription = it.getString("description") ?: ""
                        productCategory = it.getString("category") ?: "Sữa"
                        currentImageUrl = it.getString("imageUrl") ?: ""
                        isLoading = false
                    }
                }
                .addOnFailureListener { e ->
                    scope.launch {
                        snackbarHostState.showSnackbar("Lỗi khi tải sản phẩm: ${e.message}")
                    }
                    isLoading = false
                }
        } else {
            isLoading = false
        }
    }

    // Image picker launcher
    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { imageUri = it }
    }

    // Function to handle product update
    fun updateProduct() {
        if (!validateForm(productName, productPrice, productDescription, imageUri ?: currentImageUrl)) {
            scope.launch {
                snackbarHostState.showSnackbar("Vui lòng điền đầy đủ thông tin bắt buộc (*)")
            }
            return
        }

        isUploading = true

        if (imageUri != null) {
            // Upload new image first
            uploadToCloudinary(
                uri = imageUri!!,
                folder = "product_cf",
                onSuccess = { imageUrl ->
                    updateProductInFirestore(
                        productId = productId,
                        name = productName,
                        price = productPrice.toDouble(),
                        description = productDescription,
                        category = productCategory,
                        imageUrl = imageUrl,
                        onSuccess = {
                            scope.launch {
                                snackbarHostState.showSnackbar("Cập nhật sản phẩm thành công!")
                                navController.navigate("adminproduct") {
                                    popUpTo("adminproduct") { inclusive = true }
                                }
                            }
                        },
                        onFailure = { error ->
                            isUploading = false
                            scope.launch {
                                snackbarHostState.showSnackbar("Lỗi: ${error.message}")
                            }
                        }
                    )
                },
                onError = { error ->
                    isUploading = false
                    scope.launch {
                        snackbarHostState.showSnackbar("Lỗi upload ảnh: $error")
                    }
                }
            )
        } else {
            // Use existing image
            updateProductInFirestore(
                productId = productId,
                name = productName,
                price = productPrice.toDouble(),
                description = productDescription,
                category = productCategory,
                imageUrl = currentImageUrl,
                onSuccess = {
                    scope.launch {
                        snackbarHostState.showSnackbar("Cập nhật sản phẩm thành công!")
                        navController.navigate("adminproduct") {
                            popUpTo("adminproduct") { inclusive = true }
                        }
                    }
                },
                onFailure = { error ->
                    isUploading = false
                    scope.launch {
                        snackbarHostState.showSnackbar("Lỗi: ${error.message}")
                    }
                }
            )
        }
    }

    // UI
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Chỉnh sửa sản phẩm") },
                navigationIcon = {
                    IconButton(onClick = { navController.navigate("adminproduct") {
                        popUpTo("adminproduct") { inclusive = true }
                    } }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Quay lại")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Product Image Section
                Text("Ảnh sản phẩm *", style = MaterialTheme.typography.titleMedium)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .clickable(enabled = !isUploading) { imagePicker.launch("image/*") }
                        .border(
                            width = 1.dp,
                            color = if (imageUri == null && currentImageUrl.isEmpty() && !isUploading)
                                MaterialTheme.colorScheme.error
                            else
                                MaterialTheme.colorScheme.outline,
                            shape = RoundedCornerShape(8.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    when {
                        isUploading -> Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            CircularProgressIndicator()
                            Text("Đang tải lên...", style = MaterialTheme.typography.bodySmall)
                        }
                        imageUri != null -> {
                            Image(
                                painter = rememberAsyncImagePainter(
                                    ImageRequest.Builder(context).data(imageUri).build()
                                ),
                                contentDescription = "Ảnh sản phẩm mới",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        }
                        currentImageUrl.isNotEmpty() -> {
                            Image(
                                painter = rememberAsyncImagePainter(
                                    ImageRequest.Builder(context).data(currentImageUrl).build()
                                ),
                                contentDescription = "Ảnh sản phẩm hiện tại",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        }
                        else -> Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.AddAPhoto,
                                contentDescription = "Thêm ảnh",
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text("Nhấn để chọn ảnh", style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }

                // Product Information Section
                Text("Thông tin sản phẩm", style = MaterialTheme.typography.titleMedium)

                // Product Name
                OutlinedTextField(
                    value = productName,
                    onValueChange = { productName = it },
                    label = { Text("Tên sản phẩm *") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    isError = productName.isEmpty()
                )

                // Product Price
                OutlinedTextField(
                    value = productPrice.replace(".0", ""), // Loại bỏ .0 nếu có
                    onValueChange = {
                        if (it.matches(Regex("^\\d*\\.?\\d*\$"))) {
                            productPrice = it
                        }
                    },
                    label = { Text("Giá *") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    trailingIcon = { Text("VND", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                    isError = productPrice.isEmpty()
                )

                // Product Category (changed from Dropdown to TextField)
                OutlinedTextField(
                    value = productCategory,
                    onValueChange = { productCategory = it },
                    label = { Text("Danh mục *") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    isError = productCategory.isEmpty()
                )

                // Product Description
                OutlinedTextField(
                    value = productDescription,
                    onValueChange = { productDescription = it },
                    label = { Text("Mô tả sản phẩm *") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp),
                    maxLines = 5,
                    isError = productDescription.isEmpty()
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Update Button
                Button(
                    onClick = { updateProduct() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    enabled = !isUploading,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    if (isUploading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text("CẬP NHẬT SẢN PHẨM", style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
        }
    }
}

// Hàm upload ảnh lên Cloudinary với folder chỉ định
private fun uploadToCloudinary(
    uri: Uri,
    folder: String,
    onSuccess: (String) -> Unit,
    onError: (String) -> Unit
) {
    MediaManager.get().upload(uri)
        .option("folder", folder)
        .callback(object : UploadCallback {
            override fun onSuccess(requestId: String, resultData: Map<Any?, Any?>) {
                val url = resultData["secure_url"].toString()
                onSuccess(url)
            }
            override fun onError(requestId: String, error: ErrorInfo) {
                onError(error.description)
            }
            override fun onProgress(requestId: String, bytes: Long, totalBytes: Long) {}
            override fun onReschedule(requestId: String, error: ErrorInfo) {}
            override fun onStart(requestId: String) {}
        })
        .dispatch()
}

// Hàm validate form
private fun validateForm(
    name: String,
    price: String,
    description: String,
    image: Any? // Nhận Uri hoặc String URL
): Boolean {
    return name.isNotBlank() &&
            price.isNotBlank() &&
            description.isNotBlank() &&
            image != null
}

// Hàm cập nhật sản phẩm trong Firestore (đã bỏ stock và updatedAt)
private fun updateProductInFirestore(
    productId: String?,
    name: String,
    price: Double,
    description: String,
    category: String,
    imageUrl: String,
    onSuccess: () -> Unit,
    onFailure: (Exception) -> Unit
) {
    if (productId == null) {
        onFailure(IllegalArgumentException("ID sản phẩm không hợp lệ"))
        return
    }

    val productUpdates = hashMapOf<String, Any>(
        "name" to name,
        "price" to price,
        "description" to description,
        "category" to category,
        "imageUrl" to imageUrl
    )

    FirebaseFirestore.getInstance().collection("sanPham")
        .document(productId)
        .update(productUpdates)
        .addOnSuccessListener {
            onSuccess()
        }
        .addOnFailureListener { e ->
            onFailure(e)
        }
}