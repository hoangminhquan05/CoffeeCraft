package com.example.dacs3


import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.SearchOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import com.example.dacs3.Model.Product
import com.google.firebase.firestore.FirebaseFirestore
import java.text.Normalizer
import kotlinx.coroutines.launch


@Composable
fun ProductScreen(
    navController: NavController,
    cartViewModel: CartViewModel
) {
    var productList by remember { mutableStateOf<List<Product>>(emptyList()) }
    var query by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(true) }
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()


    // Gradient background colors
    val gradientColors = listOf(
        MaterialTheme.colorScheme.primaryContainer,
        MaterialTheme.colorScheme.secondaryContainer
    )


    // Load products from Firestore
    LaunchedEffect(Unit) {
        FirebaseFirestore.getInstance().collection("sanPham")
            .get()
            .addOnSuccessListener { result ->
                productList = result.map { document ->
                    Product(
                        id = document.getString("id") ?: "",
                        imageUrl = document.getString("imageUrl") ?: "",
                        name = document.getString("name") ?: "",
                        price = document.getDouble("price") ?: 0.0,
                        description = document.getString("description") ?: "",
                        category = document.getString("category") ?: "",
                    )
                }
                isLoading = false
            }
            .addOnFailureListener { exception ->
                Log.e("Firestore", "Error loading products", exception)
                isLoading = false
            }
    }


    // Filter products based on search query
    val filteredProducts = remember(productList, query) {
        if (query.isEmpty()) productList else {
            productList.filter { product ->
                normalizeText(product.name).contains(normalizeText(query), ignoreCase = true)
            }
        }
    }


    // Scaffold with Snackbar
    Scaffold(
        bottomBar = { BottomNavigationBar(navController = navController) },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        colors = gradientColors,
                        startY = 0f,
                        endY = 1000f
                    )
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                // Search Bar
                SearchBar(
                    query = query,
                    onQueryChange = { query = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 16.dp)
                )


                // Header Section
                ProductHeaderSection()


                // Product List
                when {
                    isLoading -> FullScreenLoading()
                    filteredProducts.isEmpty() -> EmptyProductList(hasQuery = query.isNotEmpty())
                    else -> ProductList(
                        products = filteredProducts,
                        onAddToCart = { product ->
                            cartViewModel.addToCart(product)
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar(
                                    message = "Đã thêm ${product.name} vào giỏ hàng",
                                    actionLabel = "Đóng",
                                    duration = SnackbarDuration.Short
                                )
                            }
                        },
                        onProductClick = { product ->
                            navController.navigate("product_detail/${product.id}")
                        }
                    )
                }
            }
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .shadow(
                elevation = 8.dp,
                shape = RoundedCornerShape(16.dp),
                spotColor = MaterialTheme.colorScheme.primary
            )
    ) {
        TextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = {
                Text(
                    "Tìm kiếm sản phẩm...",
                    style = MaterialTheme.typography.bodyLarge
                )
            },
            leadingIcon = {
                Icon(
                    Icons.Default.Search,
                    contentDescription = "Tìm kiếm",
                    tint = MaterialTheme.colorScheme.primary
                )
            },
            trailingIcon = {
                if (query.isNotEmpty()) {
                    IconButton(onClick = { onQueryChange("") }) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Xóa",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            },
            colors = TextFieldDefaults.textFieldColors(
                containerColor = MaterialTheme.colorScheme.surface,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                unfocusedTextColor = MaterialTheme.colorScheme.onSurface
            ),
            shape = RoundedCornerShape(16.dp),
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyLarge
        )
    }
}


@Composable
private fun ProductHeaderSection() {
    Column(
        modifier = Modifier.padding(horizontal = 24.dp)
    ) {
        Text(
            text = "Khám Phá Sản Phẩm",
            style = MaterialTheme.typography.headlineLarge.copy(
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        )


        Text(
            text = "Danh mục sản phẩm chất lượng",
            style = MaterialTheme.typography.bodyMedium.copy(
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                fontSize = 14.sp
            ),
            modifier = Modifier.padding(vertical = 4.dp)
        )
        Spacer(modifier = Modifier.height(8.dp))
    }
}


@Composable
private fun ProductList(
    products: List<Product>,
    onAddToCart: (Product) -> Unit,
    onProductClick: (Product) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items(products) { product ->
            ProductCard(
                product = product,
                onAddToCart = { onAddToCart(product) },
                onClick = { onProductClick(product) }
            )
        }
    }
}


@Composable
private fun ProductCard(
    product: Product,
    onAddToCart: () -> Unit,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp,
            pressedElevation = 8.dp
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Product Image
            Box(
                modifier = Modifier
                    .size(70.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(10.dp)
                    )
            ) {
                Image(
                    painter = rememberAsyncImagePainter(product.imageUrl),
                    contentDescription = product.name,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }


            // Product Info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = product.name,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 18.sp
                    ),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface
                )


                Spacer(modifier = Modifier.height(4.dp))


                Text(
                    text = product.getFormattedPrice(),
                    style = MaterialTheme.typography.titleMedium.copy(
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                )


                Spacer(modifier = Modifier.height(8.dp))


                Text(
                    text = product.description.take(60) + "...",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }


            // Add to Cart Button
            IconButton(
                onClick = onAddToCart,
                modifier = Modifier
                    .size(48.dp)
                    .background(
                        color = MaterialTheme.colorScheme.primary,
                        shape = CircleShape
                    )
            ) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = "Thêm vào giỏ hàng",
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}


@Composable
fun FullScreenLoading() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.7f)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            CircularProgressIndicator(
                color = MaterialTheme.colorScheme.primary,
                strokeWidth = 4.dp,
                modifier = Modifier.size(48.dp)
            )
            Text(
                text = "Đang tải sản phẩm...",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}


@Composable
fun EmptyProductList(hasQuery: Boolean) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Outlined.SearchOff,
            contentDescription = "Không tìm thấy",
            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
            modifier = Modifier.size(80.dp)
        )


        Spacer(modifier = Modifier.height(24.dp))


        Text(
            text = if (hasQuery) "Không tìm thấy sản phẩm phù hợp" else "Hiện chưa có sản phẩm nào",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )


        Spacer(modifier = Modifier.height(8.dp))


        Text(
            text = if (hasQuery) "Hãy thử từ khóa khác hoặc kiểm tra chính tả"
            else "Sản phẩm mới sẽ sớm được cập nhật",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            textAlign = TextAlign.Center
        )
    }
}


private fun normalizeText(text: String): String {
    return Normalizer.normalize(text, Normalizer.Form.NFD)
        .replace("[^\\p{ASCII}]".toRegex(), "")
        .lowercase()
}



