package com.example.dacs3

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.os.Environment
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Print
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

// Hàm tạo và lưu hóa đơn dưới dạng PDF
fun createInvoicePdf(
    context: Context,
    invoiceCartItems: List<CartItem>,
    invoiceTotalPrice: Double,
    userName: String,
    currentDate: String,
    paymentMethod: String,
    snackbarHostState: SnackbarHostState,
    coroutineScope: CoroutineScope
) {
    try {
        val pageWidth = 595 // A4 width in points
        val pageHeight = 842 // A4 height in points
        val pdfDocument = PdfDocument()
        val paint = Paint()
        val titlePaint = Paint().apply {
            textSize = 20f
            isFakeBoldText = true
            color = android.graphics.Color.BLACK
        }
        val normalPaint = Paint().apply {
            textSize = 14f
            color = android.graphics.Color.BLACK
        }
        val boldPaint = Paint().apply {
            textSize = 14f
            isFakeBoldText = true
            color = android.graphics.Color.BLACK
        }

        // Tạo trang đầu tiên
        var pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create()
        var page = pdfDocument.startPage(pageInfo)
        var canvas = page.canvas
        var yPosition = 50f // Vị trí bắt đầu vẽ
        var pageNumber = 1

        // Hàm chuyển sang trang mới nếu cần
        fun startNewPage() {
            pdfDocument.finishPage(page)
            pageNumber++
            pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
            page = pdfDocument.startPage(pageInfo)
            canvas = page.canvas
            yPosition = 50f
        }

        // Vẽ nội dung hóa đơn
        canvas.drawText("HÓA ĐƠN THANH TOÁN", (pageWidth / 2 - 80).toFloat(), yPosition, titlePaint)
        yPosition += 30f

        canvas.drawText("COFFEE CRAFT", 50f, yPosition, boldPaint)
        yPosition += 20f
        canvas.drawText("Địa chỉ: 83 Hai Bà Trưng, Hoàn Kiếm, TP.Hà Nội", 50f, yPosition, normalPaint)
        yPosition += 20f
        canvas.drawText("Điện thoại: 032 8386 66", 50f, yPosition, normalPaint)
        yPosition += 20f
        canvas.drawLine(50f, yPosition, pageWidth - 50f, yPosition, normalPaint) // Divider
        yPosition += 20f

        canvas.drawText("Nhân viên: $userName", 50f, yPosition, normalPaint)
        yPosition += 20f
        canvas.drawText("Ngày: $currentDate", 50f, yPosition, normalPaint)
        yPosition += 20f
        canvas.drawLine(50f, yPosition, pageWidth - 50f, yPosition, normalPaint) // Divider
        yPosition += 20f

        // Tiêu đề bảng sản phẩm
        canvas.drawText("Sản phẩm", 50f, yPosition, boldPaint)
        canvas.drawText("SL", (pageWidth / 2).toFloat(), yPosition, boldPaint)
        canvas.drawText("Thành tiền", pageWidth - 150f, yPosition, boldPaint)
        yPosition += 20f

        // Danh sách sản phẩm
        invoiceCartItems.forEach { item ->
            if (yPosition > pageHeight - 100) startNewPage()
            canvas.drawText(item.product.name.take(30), 50f, yPosition, normalPaint) // Giới hạn tên
            canvas.drawText("x${item.quantity}", (pageWidth / 2).toFloat(), yPosition, normalPaint)
            canvas.drawText(
                "%,.0f VND".format(item.product.price * item.quantity).replace(",", "."),
                pageWidth - 150f, yPosition, normalPaint
            )
            yPosition += 20f
        }

        if (yPosition > pageHeight - 100) startNewPage()
        yPosition += 10f
        canvas.drawLine(50f, yPosition, pageWidth - 50f, yPosition, normalPaint) // Divider
        yPosition += 20f

        // Tổng tiền
        canvas.drawText("Tổng tiền:", 50f, yPosition, boldPaint)
        canvas.drawText(
            "%,.0f VND".format(invoiceTotalPrice).replace(",", "."),
            pageWidth - 150f, yPosition, boldPaint
        )
        yPosition += 20f

        // Phương thức thanh toán
        canvas.drawText("Phương thức thanh toán: $paymentMethod", 50f, yPosition, normalPaint)
        yPosition += 20f

        // Lời cảm ơn
        canvas.drawText("Cảm ơn quý khách!", 50f, yPosition, normalPaint)

        // Hoàn tất trang
        pdfDocument.finishPage(page)

        // Lưu file PDF
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val storageDir = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)
        val pdfFile = File(storageDir, "INVOICE_${timeStamp}.pdf")

        FileOutputStream(pdfFile).use { out ->
            pdfDocument.writeTo(out)
        }

        pdfDocument.close()

        coroutineScope.launch {
            snackbarHostState.showSnackbar(
                message = "Đã lưu hóa đơn PDF vào ${pdfFile.absolutePath}",
                actionLabel = "Đóng",
                duration = SnackbarDuration.Long
            )
        }
    } catch (e: Exception) {
        coroutineScope.launch {
            snackbarHostState.showSnackbar(
                message = "Lỗi khi lưu hóa đơn PDF: ${e.localizedMessage}",
                actionLabel = "Đóng",
                duration = SnackbarDuration.Long
            )
        }
    }
}

// Hàm tạo URL VietQR
fun createVietQRUrl(
    bankBin: String, // Mã BIN ngân hàng, ví dụ: "970436" cho Techcombank
    accountNumber: String, // Số tài khoản
    amount: Int, // Số tiền
    note: String, // Nội dung thanh toán
    accountName: String // Tên tài khoản
): String {
    val encodedNote = note.replace(" ", "%20")
    val encodedAccountName = accountName.replace(" ", "%20")
    return "https://api.vietqr.io/image/$bankBin-$accountNumber-g3nbcFJ.jpg?accountName=$encodedAccountName&amount=$amount&addInfo=$encodedNote"
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@SuppressLint("SimpleDateFormat")
@Composable
fun PaymentScreen(navController: NavController, viewModel: CartViewModel, userId: String?) {
    var userName by remember { mutableStateOf("Đang tải...") }
    val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm")
    val currentDate = dateFormat.format(Date())
    var paymentMethod by remember { mutableStateOf("Tiền mặt") }
    var isLoading by remember { mutableStateOf(false) }
    var showQrDialog by remember { mutableStateOf(false) }
    var showCompletionDialog by remember { mutableStateOf(false) }
    var showInvoice by remember { mutableStateOf(false) }
    var invoiceCartItems by remember { mutableStateOf<List<CartItem>>(emptyList()) }
    var invoiceTotalPrice by remember { mutableStateOf(0.0) }
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    // Tạo URL VietQR
    val vietQrUrl by remember(viewModel.totalPrice, currentDate) {
        derivedStateOf {
            val bankBin = "970436" // Mã BIN của Techcombank
            val accountNumber = "1035196249" // Số tài khoản của bạn
            val amount = viewModel.totalPrice.toInt()
            val note = "COFFEE${currentDate.replace("/", "").replace(":", "").replace(" ", "")}"
            val accountName = "NGUYEN THI THANH THAO"
            createVietQRUrl(bankBin, accountNumber, amount, note, accountName)
        }
    }

    val storagePermissionState = rememberPermissionState(
        if (android.os.Build.VERSION.SDK_INT >= 33) {
            Manifest.permission.READ_MEDIA_IMAGES
        } else {
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        }
    )
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            createInvoicePdf(
                context, invoiceCartItems, invoiceTotalPrice,
                userName, currentDate, paymentMethod, snackbarHostState, coroutineScope
            )
        } else {
            coroutineScope.launch {
                snackbarHostState.showSnackbar(
                    message = "Cần quyền lưu trữ để lưu hóa đơn",
                    actionLabel = "Đóng",
                    duration = SnackbarDuration.Short
                )
            }
        }
    }

    val primaryColor = Color(0xFFA50064)
    val lightGray = Color(0xFFF9F9F9)

    LaunchedEffect(userId) {
        userId?.let { uid ->
            val db = FirebaseFirestore.getInstance()
            db.collection("uSer").document(uid).get()
                .addOnSuccessListener { document ->
                    userName = document.getString("name") ?: "Không rõ"
                }
                .addOnFailureListener {
                    userName = "Lỗi tải tên"
                }
        }
    }

    fun processPayment() {
        if (viewModel.cartItems.isEmpty()) {
            coroutineScope.launch {
                snackbarHostState.showSnackbar(
                    message = "Giỏ hàng trống, không thể thanh toán!",
                    actionLabel = "Đóng",
                    duration = SnackbarDuration.Short
                )
            }
            return
        }

        isLoading = true
        invoiceCartItems = viewModel.cartItems.toList()
        invoiceTotalPrice = viewModel.totalPrice

        val currentTime = dateFormat.format(Date())
        val products = viewModel.cartItems.map { item ->
            hashMapOf(
                "product_id" to item.product.id,
                "product_name" to item.product.name,
                "price" to item.product.price,
                "quantity" to item.quantity
            )
        }

        val orderRef = FirebaseFirestore.getInstance().collection("order_history").document()
        val order = hashMapOf(
            "order_id" to orderRef.id,
            "paymentMethod" to paymentMethod,
            "total" to viewModel.totalPrice,
            "time" to currentTime,
            "seller_id" to userId,
            "user_name" to userName,
            "products" to products
        )

        orderRef.set(order)
            .addOnSuccessListener {
                isLoading = false
                showInvoice = true // Hiển thị hóa đơn trước
            }
            .addOnFailureListener { e ->
                isLoading = false
                Log.e("PaymentScreen", "Error processing payment", e)
                coroutineScope.launch {
                    snackbarHostState.showSnackbar(
                        message = "Lỗi thanh toán: ${e.localizedMessage}",
                        actionLabel = "Đóng",
                        duration = SnackbarDuration.Long
                    )
                }
            }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Hóa đơn thanh toán", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Quay lại")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = primaryColor,
                    titleContentColor = Color.White,
                    actionIconContentColor = Color.White
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "COFFEE CRAFT",
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    color = primaryColor
                )
                Spacer(Modifier.height(6.dp))
                Text("Địa chỉ: 83 Hai Bà Trưng, Hoàn Kiếm, TP.Hà Nội", fontSize = 14.sp)
                Text("Điện thoại: 032 8386 66", fontSize = 14.sp)
                Spacer(Modifier.height(14.dp))
                Divider()
            }

            Column(Modifier.padding(vertical = 6.dp)) {
                Text("👩‍💼 Nhân viên: $userName", fontSize = 16.sp)
                Text("📅 Ngày bán: $currentDate", fontSize = 16.sp)
                Spacer(Modifier.height(8.dp))
                Divider()
            }

            Spacer(Modifier.height(12.dp))
            Text("🛍️ Sản phẩm:", fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
            Column {
                viewModel.cartItems.forEach { item ->
                    ProductItemCard(item, lightGray, primaryColor)
                }
            }

            Spacer(Modifier.height(24.dp))
            Divider()
            Spacer(Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("💵 Tổng tiền:", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Text(
                    "%,.0f VND".format(viewModel.totalPrice).replace(",", "."),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = primaryColor
                )
            }

            Spacer(Modifier.height(20.dp))
            Text("💳 Phương thức thanh toán:", fontSize = 16.sp, fontWeight = FontWeight.Medium)
            Spacer(Modifier.height(8.dp))

            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.Start
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(
                        selected = paymentMethod == "Tiền mặt",
                        onClick = { paymentMethod = "Tiền mặt" },
                        colors = RadioButtonDefaults.colors(
                            selectedColor = primaryColor,
                            unselectedColor = Color.Gray
                        )
                    )
                    Text("Tiền mặt", fontSize = 14.sp)
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(
                        selected = paymentMethod == "VietQR",
                        onClick = { paymentMethod = "VietQR" },
                        colors = RadioButtonDefaults.colors(
                            selectedColor = primaryColor,
                            unselectedColor = Color.Gray
                        )
                    )
                    Text("Thanh Toán Qua VietQR (QR Code)", fontSize = 14.sp)
                }

                if (paymentMethod == "VietQR") {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Vui lòng mở ứng dụng ngân hàng và quét mã QR",
                        fontSize = 12.sp,
                        color = Color.Gray,
                        modifier = Modifier.padding(start = 32.dp)
                    )
                }
            }

            Spacer(Modifier.height(28.dp))
            PaymentButton(
                isLoading = isLoading,
                onConfirm = {
                    if (paymentMethod == "VietQR") {
                        showQrDialog = true
                    } else {
                        processPayment()
                    }
                },
                primaryColor = primaryColor
            )

            Spacer(Modifier.height(32.dp))
        }
    }

    if (showQrDialog) {
        AlertDialog(
            onDismissRequest = { showQrDialog = false },
            title = {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "THANH TOÁN BẰNG VIETQR",
                        fontWeight = FontWeight.Bold,
                        color = primaryColor,
                        textAlign = TextAlign.Center,
                    )
                }
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Image(
                        painter = rememberAsyncImagePainter(vietQrUrl),
                        contentDescription = "Mã QR VietQR",
                        modifier = Modifier.size(400.dp)
                    )

//                    Spacer(Modifier.height(2.dp))
                    Text(
                        "Số tiền: ${"%,.0f VND".format(viewModel.totalPrice).replace(",", ".")}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Nội dung: COFFEE $currentDate",
                        fontStyle = FontStyle.Italic,
                        fontSize = 14.sp
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Hướng dẫn:",
                        fontWeight = FontWeight.Bold,
                        color = primaryColor
                    )
                    Text("Mở ứng dụng thanh toán và quét mã QR")


                }
            },
            confirmButton = {
                Column(modifier = Modifier.padding(top = 4.dp)) {
                    Button(
                        onClick = {
                            showQrDialog = false
                            isLoading = true
                            processPayment()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = primaryColor),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("XÁC NHẬN ĐÃ THANH TOÁN", fontWeight = FontWeight.Bold)
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showQrDialog = false },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("HỦY BỎ", color = primaryColor, fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    if (showInvoice) {
        AlertDialog(
            onDismissRequest = {
                showInvoice = false
                viewModel.clearCart() // Xóa giỏ hàng khi đóng dialog
                showCompletionDialog = true // Hiển thị dialog hoàn tất
            },
            modifier = Modifier
                .clip(RoundedCornerShape(16.dp))
                .border(1.dp, primaryColor.copy(alpha = 0.2f), RoundedCornerShape(16.dp))
                .heightIn(max = 500.dp),
            containerColor = Color.White,
            title = {
                Text(
                    "HÓA ĐƠN THANH TOÁN",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 22.sp,
                    color = primaryColor,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("COFFEE CRAFT", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = primaryColor)
                    Spacer(Modifier.height(4.dp))
                    Text("Địa chỉ: 83 Hai Bà Trưng, Hoàn Kiếm, TP.Hà Nội", fontSize = 14.sp)
                    Text("Điện thoại: 032 8386 66", fontSize = 14.sp)
                    Spacer(Modifier.height(12.dp))
                    Divider()

                    Text("Nhân viên: $userName", fontSize = 16.sp)
                    Text("Ngày: $currentDate", fontSize = 16.sp)
                    Spacer(Modifier.height(12.dp))
                    Divider()

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Sản phẩm", fontWeight = FontWeight.Bold, modifier = Modifier.weight(1.5f))
                        Text("SL", fontWeight = FontWeight.Bold, modifier = Modifier.weight(0.5f), textAlign = TextAlign.Center)
                        Text("Thành tiền", fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f), textAlign = TextAlign.End)
                    }
                    Spacer(Modifier.height(8.dp))

                    if (invoiceCartItems.isEmpty()) {
                        Text("Không có sản phẩm nào", fontSize = 14.sp, color = Color.Gray)
                    } else {
                        invoiceCartItems.forEach { item ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    item.product.name,
                                    modifier = Modifier.weight(1.5f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    fontSize = 14.sp
                                )
                                Text(
                                    "x${item.quantity}",
                                    modifier = Modifier.weight(0.5f),
                                    textAlign = TextAlign.Center,
                                    fontSize = 14.sp
                                )
                                Text(
                                    "%,.0f VND".format(item.product.price * item.quantity).replace(",", "."),
                                    modifier = Modifier.weight(1f),
                                    textAlign = TextAlign.End,
                                    fontSize = 14.sp
                                )
                            }
                            Spacer(Modifier.height(6.dp))
                        }
                    }

                    Spacer(Modifier.height(12.dp))
                    Divider()

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Tổng tiền:", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text(
                            "%,.0f VND".format(invoiceTotalPrice).replace(",", "."),
                            fontWeight = FontWeight.Bold,
                            color = primaryColor,
                            fontSize = 16.sp
                        )
                    }

                    Spacer(Modifier.height(12.dp))
                    Text("Phương thức thanh toán: $paymentMethod", fontSize = 16.sp)
                    Spacer(Modifier.height(16.dp))
                    Text("Cảm ơn quý khách!", fontStyle = FontStyle.Italic, fontSize = 14.sp)
                }
            },
            confirmButton = {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Button(
                        onClick = {
                            if (storagePermissionState.status.isGranted) {
                                createInvoicePdf(
                                    context, invoiceCartItems, invoiceTotalPrice,
                                    userName, currentDate, paymentMethod, snackbarHostState, coroutineScope
                                )
                            } else {
                                permissionLauncher.launch(
                                    if (android.os.Build.VERSION.SDK_INT >= 33) {
                                        Manifest.permission.READ_MEDIA_IMAGES
                                    } else {
                                        Manifest.permission.WRITE_EXTERNAL_STORAGE
                                    }
                                )
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = primaryColor),
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 4.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Print,
                            contentDescription = "Lưu hóa đơn",
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text("Lưu PDF", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }

                    Button(
                        onClick = {
                            showInvoice = false
                            viewModel.clearCart() // Xóa giỏ hàng khi nhấn "Hoàn tất"
                            showCompletionDialog = true // Hiển thị dialog hoàn tất
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.LightGray),
                        modifier = Modifier
                            .weight(1f)
                            .padding(start = 4.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Hoàn tất", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                }
            }
        )
    }

    if (showCompletionDialog) {
        AlertDialog(
            onDismissRequest = {
                showCompletionDialog = false
                navController.navigate("cart")
            },
            modifier = Modifier
                .clip(RoundedCornerShape(16.dp))
                .border(1.dp, primaryColor.copy(alpha = 0.2f), RoundedCornerShape(16.dp)),
            title = {
                Text(
                    "THANH TOÁN THÀNH CÔNG",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 22.sp,
                    color = primaryColor,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    val scale by animateFloatAsState(
                        targetValue = if (showCompletionDialog) 1f else 0f,
                        animationSpec = tween(durationMillis = 500),
                        label = "Checkmark Scale"
                    )
                    Icon(
                        imageVector = Icons.Filled.CheckCircle,
                        contentDescription = "Thành công",
                        tint = Color(0xFF4CAF50),
                        modifier = Modifier
                            .size(64.dp)
                            .scale(scale)
                    )
                    Spacer(Modifier.height(16.dp))
                    Text(
                        "Hoàn Tất Đơn Hàng",
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        color = Color.Black
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "Cảm Ơn Quý Khách Đã Ghé Thăm Coffee Craft",
                        fontWeight = FontWeight.Medium,
                        fontSize = 16.sp,
                        color = Color.DarkGray,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Xin Chào Và Hẹn Gặp Lại Quý Khách!",
                        fontWeight = FontWeight.Medium,
                        fontSize = 16.sp,
                        color = Color.DarkGray,
                        textAlign = TextAlign.Center
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showCompletionDialog = false
                        navController.navigate("cart")
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = primaryColor),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        "OK",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
            }
        )
    }
}

@Composable
private fun ProductItemCard(item: CartItem, backgroundColor: Color, priceColor: Color) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .border(1.dp, Color.LightGray, MaterialTheme.shapes.medium),
        elevation = CardDefaults.cardElevation(4.dp),
        colors = CardDefaults.cardColors(
            containerColor = backgroundColor,
        ),
        shape = MaterialTheme.shapes.medium
    ) {
        Column(Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = item.product.name,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "x${item.quantity}",
                    fontSize = 16.sp,
                    color = Color.Gray
                )
            }
            Spacer(Modifier.height(4.dp))
            Text(
                text = String.format(Locale("vi", "VN"), "%,.0f VND", item.totalPrice).replace(",", "."),
                fontWeight = FontWeight.Bold,
                color = priceColor,
                fontSize = 16.sp
            )
        }
    }
}

@Composable
private fun PaymentButton(
    isLoading: Boolean,
    onConfirm: () -> Unit,
    primaryColor: Color
) {
    Button(
        onClick = onConfirm,
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp),
        shape = MaterialTheme.shapes.large,
        colors = ButtonDefaults.buttonColors(containerColor = primaryColor),
        elevation = ButtonDefaults.buttonElevation(
            defaultElevation = 8.dp,
            pressedElevation = 4.dp
        ),
        enabled = !isLoading
    ) {
        if (isLoading) {
            CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp)
        } else {
            Text(
                "XÁC NHẬN THANH TOÁN",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
    }
}

