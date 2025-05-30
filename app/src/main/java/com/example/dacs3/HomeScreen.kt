package com.example.dacs3

import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.rememberImagePainter
import com.example.dacs3.Model.Product
import com.google.firebase.firestore.FirebaseFirestore
import androidx.compose.ui.text.style.TextOverflow
import com.example.dacs3.Model.Banner
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch


@Composable
fun HomeScreen(navController: NavController) {
    val scrollState = rememberScrollState()

    // State để lưu trữ danh sách sản phẩm từ Firestore
    var products by remember { mutableStateOf<List<Product>>(emptyList()) }

    // Lấy dữ liệu từ Firestore khi composable được gọi
    LaunchedEffect(Unit) {
        val db = FirebaseFirestore.getInstance()

        // Lấy dữ liệu từ collection "sanPham"
        db.collection("sanPham")
            .get()
            .addOnSuccessListener { result ->
                // Log để kiểm tra dữ liệu Firestore
                Log.d("Firestore", "Loaded products: ${result.documents}")

                // Chuyển đổi dữ liệu Firestore thành danh sách các sản phẩm
                products = result.map { document ->
                    val id = document.getString("id") ?: ""
                    val name = document.getString("name") ?: ""
                    val price = document.getDouble("price") ?: 0.0
                    val description = document.getString("description") ?: ""
                    val imageUrl = document.getString("imageUrl") ?: ""
                    val category = document.getString("category") ?: ""
                    Product(id, imageUrl, name, price, description = description, category = category)
                }
            }
            .addOnFailureListener { exception ->
                // Xử lý lỗi nếu có
                Log.e("Firestore", "Error getting documents: ", exception)
            }
    }

    Scaffold(
        bottomBar = {
            BottomNavigationBar(navController = navController)
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(scrollState)
        ) {
            CoffeeCraftTitle()
            BannerSection()

            // Kiểm tra xem có sản phẩm chưa, nếu có thì hiển thị
            if (products.isEmpty()) {
                Text(text = "No products available", fontSize = 16.sp, color = Color.Gray)
            } else {
                CategorySection(
                    categoryName = "Sản phẩm bán chạy",
                    products = products,
                    navController = navController
                )
                CategorySection(
                    categoryName = "Trà Hoa Quả",
                    products = products.filter { it.category == "Trà Hoa Quả" },
                    navController = navController
                )

                CategorySection(
                    categoryName = "Trà Sữa",
                    products = products.filter { it.category == "Trà Sữa" },
                    navController = navController
                )
                CategorySection(
                    categoryName = "Sữa",
                    products = products.filter { it.category == "Sữa" },
                    navController = navController
                )
                CategorySection(
                    categoryName = "Sữa Chua",
                    products = products.filter { it.category == "Sữa Chua" },
                    navController = navController
                )
            }
        }
    }
}

@Composable
fun CategorySection(categoryName: String, products: List<Product>, navController: NavController) {
    Column(modifier = Modifier.padding(16.dp)) {
        Text(text = categoryName, fontSize = 20.sp, color = Color(0xFF6200EA))
        Spacer(modifier = Modifier.height(8.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            items(products) { product ->
                ProductItem(product = product, navController = navController)
            }
        }
    }
}

@Composable
fun ProductItem(product: Product, navController: NavController) {
    Card(
        modifier = Modifier
            .width(140.dp)
            .height(170.dp)
            .clickable {
                navController.navigate("product_detail/${product.id}")
            },
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(4.dp),
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp)
        ) {
            // Kiểm tra xem imageUrl có hợp lệ không
            Image(
                painter = rememberImagePainter(product.imageUrl),
                contentDescription = "Product Image",
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp)
                    .clip(RoundedCornerShape(8.dp))
            )
            Spacer(modifier = Modifier.height(8.dp))
            // Text cho tên sản phẩm, cắt ngắn và hiển thị dấu ba chấm nếu dài
            Text(
                text = product.name,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = product.getFormattedPrice(),
                fontSize = 12.sp,
                color = Color.Gray
            )
        }
    }
}


@Composable
fun BannerSection() {
    var banners by remember { mutableStateOf<List<Banner>>(emptyList()) }
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    // Lấy banner từ Firestore
    LaunchedEffect(Unit) {
        val db = FirebaseFirestore.getInstance()
        db.collection("banner")
            .get()
            .addOnSuccessListener { result ->
                banners = result.map { document ->
                    Banner(
                        id = document.getString("id") ?: "",
                        banner_url = document.getString("banner_url") ?: "",
                        name = document.getString("name") ?: ""
                    )
                }
            }
    }

    // Tự động scroll banner
    LaunchedEffect(banners) {
        if (banners.isNotEmpty()) {
            while (true) {
                delay(3000)
                val next = (listState.firstVisibleItemIndex + 1) % banners.size
                coroutineScope.launch {
                    listState.animateScrollToItem(next)
                }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp)
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        if (banners.isEmpty()) {
            // Placeholder khi đang tải
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.LightGray),
                contentAlignment = Alignment.Center
            ) {
                Text("Đang tải banner...", color = Color.Gray)
            }
        } else {
            LazyRow(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(horizontal = 16.dp)
            ) {
                items(banners) { banner ->
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillParentMaxWidth(1f)
                            .height(200.dp)
                    ) {
                        Image(
                            painter = rememberImagePainter(banner.banner_url),
                            contentDescription = banner.name,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CoffeeCraftTitle() {
    // Hiệu ứng alpha (mờ dần) và scale (phóng to)
    val alpha = remember { androidx.compose.animation.core.Animatable(0f) }
    val scale = remember { androidx.compose.animation.core.Animatable(0.8f) }

    LaunchedEffect(Unit) {
        alpha.animateTo(1f, animationSpec = androidx.compose.animation.core.tween(durationMillis = 1000))
        scale.animateTo(1f, animationSpec = androidx.compose.animation.core.tween(durationMillis = 1000))
    }

    Text(
        text = "Coffee Craft ☕",
        fontSize = 28.sp,
        fontWeight = FontWeight.ExtraBold,
        color = Color(0xFF6D4C41), 
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp, bottom = 8.dp)
            .graphicsLayer {
                this.alpha = alpha.value
                this.scaleX = scale.value
                this.scaleY = scale.value
            },
        letterSpacing = 2.sp
    )
}

