package com.example.dacs3


import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.dacs3.Model.Product
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductDetailScreen(
    navController: NavController,
    productId: String,
    cartViewModel: CartViewModel
) {
    var product by remember { mutableStateOf<Product?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var quantity by remember { mutableIntStateOf(1) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()


    LaunchedEffect(productId) {
        try {
            FirebaseFirestore.getInstance()
                .collection("sanPham")
                .document(productId)
                .get()
                .addOnSuccessListener { document ->
                    if (document.exists()) {
                        product = document.toObject(Product::class.java)?.apply {
                            id = document.id
                        }
                    } else {
                        error = "Product not found"
                    }
                    isLoading = false
                }
                .addOnFailureListener { e ->
                    error = "Failed to load product: ${e.localizedMessage}"
                    isLoading = false
                }
        } catch (e: Exception) {
            error = "Error: ${e.localizedMessage}"
            isLoading = false
        }
    }


    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Chi Tiết Sản Phẩm", color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            Icons.Filled.ArrowBack,
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
        when {
            isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }


            error != null -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(error!!, color = Color.Red, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = { navController.popBackStack() }) {
                            Text("Quay lại")
                        }
                    }
                }
            }


            product != null -> {
                ProductDetailContent(
                    modifier = Modifier.padding(innerPadding),
                    product = product!!,
                    quantity = quantity,
                    onQuantityChange = { quantity = it },
                    onAddToCart = {
                        scope.launch {
                            cartViewModel.addToCart(product!!, quantity)
                            snackbarHostState.showSnackbar(
                                message = "Đã thêm x${quantity} ${product!!.name} vào giỏ hàng",
                                duration = SnackbarDuration.Short
                            )
                            navController.popBackStack()
                        }
                    }
                )
            }
        }
    }
}


@Composable
private fun ProductDetailContent(
    modifier: Modifier = Modifier,
    product: Product,
    quantity: Int,
    onQuantityChange: (Int) -> Unit,
    onAddToCart: () -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFFDFCFB)) // pastel nền
            .padding(bottom = 16.dp)
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Phần hiển thị ảnh sản phẩm với sticker "Hot 🔥"
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1.3f) // Tỷ lệ khung hình giúp ảnh không bị méo và cân đối
                        .clip(RoundedCornerShape(20.dp)), // Bo góc khung ảnh
                    contentAlignment = Alignment.TopStart
                ) {
                    AsyncImage(
                        model = product.imageUrl,
                        contentDescription = "Product image",
                        contentScale = ContentScale.Crop, // Cắt vừa ảnh theo khung
                        placeholder = painterResource(R.drawable.banner),
                        error = painterResource(R.drawable.banner),
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(20.dp)) // Bo góc ảnh
                    )


                    // Sticker "Hot 🔥" ở góc trên trái
                    Text(
                        text = " \uD83E\uDD64 Yummy ",
                        modifier = Modifier
                            .padding(10.dp)
                            .background(
                                color = Color(0xFFFFC107), // Màu vàng nổi bật
                                shape = RoundedCornerShape(12.dp)
                            )
                            .padding(horizontal = 10.dp, vertical = 4.dp),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                }




                Spacer(modifier = Modifier.height(24.dp))


                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.Start
                ) {
                    Text(
                        text = product.name,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )


                    Text(
                        text = product.getFormattedPrice(),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF7B1FA2)
                    )


                    Spacer(modifier = Modifier.height(16.dp))


                    Text(
                        text = "✨ Mô tả sản phẩm",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF512DA8)
                    )
                    Text(
                        text = product.description,
                        fontSize = 14.sp,
                        color = Color.DarkGray,
                        lineHeight = 20.sp
                    )
                }


                Spacer(modifier = Modifier.height(24.dp))


                QuantitySelector(
                    quantity = quantity,
                    onDecrease = { if (quantity > 1) onQuantityChange(quantity - 1) },
                    onIncrease = { onQuantityChange(quantity + 1) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFFF3E5F5))
                        .padding(16.dp)
                )
            }
        }


        // ⭐️ Nút thêm giỏ hàng nổi bật
        Button(
            onClick = onAddToCart,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .padding(horizontal = 16.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF7B1FA2),
                contentColor = Color.White
            )
        ) {
            Text(
                text = "🛒 THÊM VÀO GIỎ - ${product.getFormattedPrice(quantity)}",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}




@Composable
private fun QuantitySelector(
    quantity: Int,
    onDecrease: () -> Unit,
    onIncrease: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            "Số lượng:",
            style = MaterialTheme.typography.titleMedium
        )


        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            IconButton(
                onClick = onDecrease,
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        if (quantity > 1) Color(0xFFF5F5F5)
                        else Color(0xFFEEEEEE)
                    ),
                enabled = quantity > 1
            ) {
                Icon(
                    Icons.Filled.Remove,
                    contentDescription = "Decrease",
                    tint = if (quantity > 1) Color(0xFF6200EA) else Color.Gray
                )
            }


            Text(
                text = "$quantity",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.width(32.dp),
                textAlign = TextAlign.Center
            )


            IconButton(
                onClick = onIncrease,
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFFF5F5F5))
            ) {
                Icon(
                    Icons.Filled.Add,
                    contentDescription = "Increase",
                    tint = Color(0xFF6200EA)
                )
            }
        }
    }
}


// Extension function for Product
fun Product.getFormattedPrice(quantity: Int = 1): String {
    val totalPrice = price * quantity
    return "%,.0f VND".format(totalPrice).replace(",", ".")
}

