package com.example.dacs3

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InvoiceScreen(
    navController: NavController,
    viewModel: CartViewModel,
    userId: String?,
    userName: String,
    currentDate: String,
    paymentMethod: String
) {
    val primaryColor = Color(0xFFA50064)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("HÓA ĐƠN THANH TOÁN", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.navigate("cart") }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Quay lại")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = primaryColor,
                    titleContentColor = Color.White
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // Header
            Text("COFFEE CRAFT", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = primaryColor)
            Text("Địa chỉ: 83 Hai Bà Trưng, Hoàn Kiếm, TP.Hà Nội", fontSize = 14.sp)
            Text("Điện thoại: 032 8386 66", fontSize = 14.sp)
            Spacer(Modifier.height(16.dp))
            Divider()

            // Thông tin đơn hàng
            Spacer(Modifier.height(12.dp))
            Text("👩‍💼 Nhân viên: $userName", fontSize = 16.sp)
            Text("📅 Ngày bán: $currentDate", fontSize = 16.sp)
            Spacer(Modifier.height(12.dp))
            Divider()

            // Danh sách sản phẩm
            Spacer(Modifier.height(16.dp))
            Text("🛍️ Sản phẩm:", fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(8.dp))
            viewModel.cartItems.forEach {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("${it.product.name} x${it.quantity}")
                    Text("%,.0f VND".format(it.totalPrice).replace(",", "."))
                }
                Spacer(Modifier.height(6.dp))
            }

            Spacer(Modifier.height(16.dp))
            Divider()

            // Tổng tiền
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("💵 Tổng tiền:", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Text(
                    "%,.0f VND".format(viewModel.totalPrice).replace(",", "."),
                    color = primaryColor,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            }

            Spacer(Modifier.height(12.dp))
            Text("💳 Phương thức thanh toán: $paymentMethod", fontSize = 16.sp)
            Spacer(Modifier.height(24.dp))

            // Nút hành động
            Row(modifier = Modifier.fillMaxWidth()) {
                Button(
                    onClick = {
                        // TODO: Thêm chức năng export PDF nếu cần
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = primaryColor),
                ) {
                    Text("Xuất Hóa Đơn", fontWeight = FontWeight.Bold, color = Color.White)
                }

                Spacer(Modifier.width(12.dp))

                Button(
                    onClick = { navController.navigate("cart") },
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Gray),
                ) {
                    Text("Về Giỏ Hàng", fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }
    }
}
