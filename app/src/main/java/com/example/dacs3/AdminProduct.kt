package com.example.dacs3

import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import com.cloudinary.Cloudinary
import com.example.dacs3.Model.Product
import com.google.firebase.firestore.FirebaseFirestore
import java.text.Normalizer

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminProduct(navController: NavController, cloudinary: Cloudinary) {
    var productList by remember { mutableStateOf<List<Product>>(emptyList()) }
    var filteredList by remember { mutableStateOf<List<Product>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedProduct by remember { mutableStateOf<Product?>(null) }
    var showDialog by remember { mutableStateOf(false) }

    // Fetch products from Firestore
    LaunchedEffect(Unit) {
        FirebaseFirestore.getInstance().collection("sanPham")
            .get()
            .addOnSuccessListener { result ->
                productList = result.map { document ->
                    Product(
                        id = document.id,
                        imageUrl = document.getString("imageUrl") ?: "",
                        name = document.getString("name") ?: "",
                        price = document.getDouble("price") ?: 0.0,
                        description = document.getString("description") ?: "",
                        category = document.getString("category") ?: ""
                    )
                }
                filteredList = productList
                isLoading = false
            }
            .addOnFailureListener { exception ->
                Log.e("AdminProduct", "Error loading products", exception)
                isLoading = false
            }
    }

    // Filter products based on search query
    LaunchedEffect(searchQuery, productList) {
        filteredList = if (searchQuery.isEmpty()) {
            productList
        } else {
            productList.filter { product ->
                normalizeText(product.name).contains(normalizeText(searchQuery), ignoreCase = true)
            }
        }
    }

    // Delete product function
    fun deleteProduct(productId: String) {
        FirebaseFirestore.getInstance().collection("sanPham")
            .document(productId)
            .delete()
            .addOnSuccessListener {
                productList = productList.filter { it.id != productId }
                filteredList = filteredList.filter { it.id != productId }
            }
            .addOnFailureListener { e ->
                Log.e("AdminProduct", "Error deleting product", e)
            }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Quản lý sản phẩm") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Quay lại")
                    }
                },
                actions = {
                    IconButton(onClick = { navController.navigate("add_product") }) {
                        Icon(Icons.Default.Add, contentDescription = "Thêm sản phẩm")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding)) {
            // Search bar
            SearchBar(
                query = searchQuery,
                onQueryChange = { searchQuery = it },
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )

            when {
                isLoading -> FullScreenLoading()
                filteredList.isEmpty() -> EmptyProductList(hasQuery = searchQuery.isNotEmpty())
                else -> LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filteredList) { product ->
                        ProductCard(
                            product = product,
                            onClick = {
                                selectedProduct = product
                                showDialog = true
                            }
                        )
                    }
                }
            }
        }
    }

// Edit/Delete Dialog
    var showDeleteConfirmation by remember { mutableStateOf(false) }
    var productToDelete by remember { mutableStateOf<Product?>(null) }

    if (showDialog && selectedProduct != null) {
        AlertDialog(
            onDismissRequest = { showDialog = false }, // bấm ra ngoài dialog tắt
            title = { Text("Tùy chọn sản phẩm") },
            text = { Text("Chọn thao tác cho ${selectedProduct?.name}") },
            confirmButton = {
                Button(
                    onClick = {
                        navController.navigate("edit_product/${selectedProduct?.id}")
                        showDialog = false
                    }
                ) {
                    Text("Chỉnh sửa")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDialog = false } // chỉ tắt dialog khi bấm Hủy
                ) {
                    Text("Hủy")
                }

                // Nút Xóa
                Button(
                    onClick = {
                        productToDelete = selectedProduct
                        showDialog = false
                        showDeleteConfirmation = true
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
                    modifier = Modifier.padding(start = 8.dp)
                ) {
                    Text("Xóa")
                }
            }
        )
    }

    if (showDeleteConfirmation && productToDelete != null) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmation = false },
            title = { Text("Xác nhận xóa") },
            text = { Text("Bạn có chắc chắn muốn xóa sản phẩm \"${productToDelete?.name}\" không?") },
            confirmButton = {
                Button(
                    onClick = {
                        productToDelete?.id?.let { deleteProduct(it) }
                        showDeleteConfirmation = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                ) {
                    Text("Xóa")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmation = false }) {
                    Text("Hủy")
                }
            }
        )
    }

}


@Composable
private fun ProductCard(
    product: Product,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                painter = rememberAsyncImagePainter(product.imageUrl),
                contentDescription = product.name,
                modifier = Modifier
                    .size(80.dp)
                    .clip(RoundedCornerShape(8.dp)))

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = product.name,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = product.getFormattedPrice(),
                    color = Color.Gray
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = product.category,
                    color = Color.Blue,
                    fontSize = 14.sp
                )
            }

            Icon(
                imageVector = Icons.Default.Edit,
                contentDescription = "Chỉnh sửa",
                tint = Color.Gray
            )
        }
    }
}

@Composable
private fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = modifier.fillMaxWidth(),
        placeholder = { Text("Tìm kiếm sản phẩm...") },
        leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(Icons.Default.Close, contentDescription = "Clear")
                }
            }
        },
        shape = RoundedCornerShape(16.dp),
        singleLine = true,
        colors = TextFieldDefaults.colors(
            focusedContainerColor = Color.Transparent,
            unfocusedContainerColor = Color.Transparent
        )
    )
}

private fun normalizeText(text: String): String {
    return Normalizer.normalize(text, Normalizer.Form.NFD)
        .replace("[^\\p{ASCII}]".toRegex(), "")
        .lowercase()
}