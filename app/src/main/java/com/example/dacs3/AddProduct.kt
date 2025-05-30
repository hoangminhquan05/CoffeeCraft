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
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.ArrowBack
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
fun AddProductScreen(navController: NavController, cloudinary: Cloudinary) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // State for form fields
    var productName by remember { mutableStateOf("") }
    var productPrice by remember { mutableStateOf("") }
    var productDescription by remember { mutableStateOf("") }
    var productCategory by remember { mutableStateOf("") }
    var imageUri by remember { mutableStateOf<Uri?>(null) }
    var isUploading by remember { mutableStateOf(false) }

    // Image picker launcher
    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { imageUri = it }
    }

    // Function to handle product submission
    fun submitProduct() {
        if (!validateForm(productName, productPrice, productDescription, imageUri, productCategory)) {
            scope.launch {
                snackbarHostState.showSnackbar("Vui lòng điền đầy đủ thông tin bắt buộc (*)")
            }
            return
        }

        isUploading = true
        imageUri?.let { uri ->
            uploadToCloudinary(
                uri = uri,
                folder = "product_cf",
                onSuccess = { imageUrl ->
                    saveProductToFirestore(
                        name = productName,
                        price = productPrice.toDouble(),
                        description = productDescription,
                        category = productCategory,
                        imageUrl = imageUrl,
                        onSuccess = {
                            scope.launch {
                                snackbarHostState.showSnackbar("Thêm sản phẩm thành công!")
                                navController.popBackStack()
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
        } ?: run {
            isUploading = false
            scope.launch {
                snackbarHostState.showSnackbar("Vui lòng chọn ảnh sản phẩm")
            }
        }
    }

    // UI
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Thêm sản phẩm mới") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Quay lại")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
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
                        color = if (imageUri == null && !isUploading)
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
                            contentDescription = "Ảnh sản phẩm",
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
                value = productPrice,
                onValueChange = { if (it.matches(Regex("^\\d*\\.?\\d*\$"))) productPrice = it },
                label = { Text("Giá *") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                trailingIcon = { Text("VND", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                isError = productPrice.isEmpty()
            )

            // Product Category
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

            // Submit Button
            Button(
                onClick = { submitProduct() },
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
                    Text("THÊM SẢN PHẨM", style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }
}

// upload ảnh lên Cloudinary ở folder product_cf
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
    imageUri: Uri?,
    category: String
): Boolean {
    return name.isNotBlank() &&
            price.isNotBlank() &&
            description.isNotBlank() &&
            imageUri != null &&
            category.isNotBlank()
}

// Hàm lưu sản phẩm vào Firestore
private fun saveProductToFirestore(
    name: String,
    price: Double,
    description: String,
    category: String,
    imageUrl: String,
    onSuccess: () -> Unit,
    onFailure: (Exception) -> Unit
) {
    // Tạo reference đến collection trước
    val productsCollection = FirebaseFirestore.getInstance().collection("sanPham")

    // Tạo document mới với ID tự động
    val newProductRef = productsCollection.document()

    // Tạo dữ liệu sản phẩm, bao gồm cả id
    val product = hashMapOf(
        "id" to newProductRef.id, // Lấy ID của document
        "name" to name,
        "price" to price,
        "description" to description,
        "category" to category,
        "imageUrl" to imageUrl
    )

    // Set dữ liệu vào document
    newProductRef.set(product)
        .addOnSuccessListener {
            onSuccess()
        }
        .addOnFailureListener { e ->
            onFailure(e)
        }
}