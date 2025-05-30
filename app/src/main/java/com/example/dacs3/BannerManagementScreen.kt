package com.example.dacs3

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Collections
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import coil.compose.rememberImagePainter
import com.example.dacs3.Model.Banner
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.rememberAsyncImagePainter
import kotlinx.coroutines.launch


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BannerManagementScreen(navController: NavController, viewModel: BannerViewModel = viewModel()) {
    val banners by viewModel.banners.collectAsState()
    val selectedBanner by viewModel.selectedBanner.collectAsState()

    var showAddDialog by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Quản lý Banner") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Quay lại"
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { showAddDialog = true }) {
                        Icon(Icons.Filled.Add, contentDescription = "Thêm banner")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)) {

            // Banner Grid với hiệu ứng chọn
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 180.dp),
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(8.dp)
            ) {
                items(banners) { banner ->
                    val isSelected = selectedBanner?.id == banner.id
                    BannerItem(
                        banner = banner,
                        isSelected = isSelected,
                        onSelect = { viewModel.selectBanner(banner) }
                    )
                }
            }

            // Panel hành động cho banner được chọn
            AnimatedVisibility(
                visible = selectedBanner != null,
                enter = slideInVertically { it } + fadeIn(),
                exit = slideOutVertically { it } + fadeOut()
            ) {
                selectedBanner?.let { banner ->
                    ActionPanel(
                        banner = banner,
                        onEdit = { showEditDialog = true },
                        onDelete = { viewModel.deleteBanner(banner.id) },
                        modifier = Modifier.padding(8.dp)
                    )
                }
            }
        }
    }

    // Dialog thêm mới
    if (showAddDialog) {
        BannerEditDialog(
            banner = Banner(),
            onDismiss = { showAddDialog = false },
            onSave = { newBanner ->
                viewModel.addBanner(newBanner)
                showAddDialog = false
            }
        )
    }

    // Dialog chỉnh sửa
    if (showEditDialog && selectedBanner != null) {
        BannerEditDialog(
            banner = selectedBanner!!,
            onDismiss = { showEditDialog = false },
            onSave = { updatedBanner ->
                viewModel.updateBanner(updatedBanner)
                showEditDialog = false
            }
        )
    }
}

@Composable
fun BannerItem(banner: Banner, isSelected: Boolean, onSelect: () -> Unit) {
    Card(
        onClick = onSelect,
        modifier = Modifier
            .padding(8.dp)
            .fillMaxWidth()
            .border(
                width = if (isSelected) 2.dp else 0.dp,
                color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                shape = MaterialTheme.shapes.medium
            ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 8.dp else 4.dp)
    ) {
        Column {
            Image(
                painter = rememberAsyncImagePainter(banner.banner_url),
                contentDescription = banner.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp)
            )

            Text(
                text = banner.name,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier
                    .padding(8.dp)
                    .fillMaxWidth(),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun ActionPanel(
    banner: Banner,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(20.dp)
        ) {
            Text(
                text = "🎯 Đang chọn: ${banner.name}",
                style = MaterialTheme.typography.titleMedium.copy(
                    color = MaterialTheme.colorScheme.primary
                ),
                modifier = Modifier
                    .padding(bottom = 16.dp)
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Button(
                    onClick = onEdit,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        Icons.Filled.Edit,
                        contentDescription = "Sửa",
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Chỉnh sửa")
                }

                Button(
                    onClick = onDelete,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        Icons.Filled.Delete,
                        contentDescription = "Xóa",
                        tint = MaterialTheme.colorScheme.onError
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Xóa")
                }
            }
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BannerEditDialog(
    banner: Banner,
    onDismiss: () -> Unit,
    onSave: (Banner) -> Unit,
    viewModel: BannerViewModel = viewModel()
) {
    val context = LocalContext.current
    var name by remember { mutableStateOf(banner.name) }
    var imageUri by remember { mutableStateOf<Uri?>(null) }
    val isUploading by viewModel.isUploading.collectAsState()
    val uploadProgress by viewModel.uploadProgress.collectAsState()

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { imageUri = it }
    }

    AlertDialog(
        onDismissRequest = { if (!isUploading) onDismiss() },
        title = { Text(text = if (banner.id.isEmpty()) "Thêm Banner" else "Chỉnh sửa Banner") },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Tên banner") },
                    modifier = Modifier.fillMaxWidth(),
                    isError = name.isBlank()
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Hiển thị tiến trình upload
                if (isUploading) {
                    Column {
                        LinearProgressIndicator(
                            progress = { uploadProgress / 100f },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Text(
                            text = "Đang tải lên: $uploadProgress%",
                            modifier = Modifier.align(Alignment.CenterHorizontally)
                        )
                    }
                } else {
                    Button(
                        onClick = { launcher.launch("image/*") },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Collections, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Chọn ảnh mới")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Hiển thị ảnh hiện tại hoặc ảnh mới chọn
                when {
                    imageUri != null -> {
                        Image(
                            painter = rememberAsyncImagePainter(imageUri),
                            contentDescription = "Ảnh mới",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp)
                        )
                    }
                    banner.banner_url.isNotEmpty() -> {
                        Column {
                            Text("Ảnh hiện tại:", style = MaterialTheme.typography.labelMedium)
                            Spacer(modifier = Modifier.height(4.dp))
                            Image(
                                painter = rememberAsyncImagePainter(banner.banner_url),
                                contentDescription = "Ảnh hiện tại",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(200.dp)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (imageUri != null) {
                        viewModel.viewModelScope.launch {
                            try {
                                val url = viewModel.uploadImageToCloudinary(context, imageUri!!)
                                onSave(banner.copy(name = name, banner_url = url))
                            } catch (e: Exception) {
                                // Xử lý lỗi
                            }
                        }
                    } else {
                        onSave(banner.copy(name = name))
                    }
                },
                enabled = name.isNotBlank() && !isUploading,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Lưu thay đổi")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isUploading
            ) {
                Text("Hủy")
            }
        }
    )
}