package com.example.dacs3

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.navigation.NavController
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatisticsScreen(navController: NavController, userId: String?) {
    // State for statistics data
    var originalSalesData by remember { mutableStateOf<List<SaleDetail>>(emptyList()) }
    var filteredSalesData by remember { mutableStateOf<List<SaleDetail>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var userRole by remember { mutableStateOf<String?>(null) }
    var showContent by remember { mutableStateOf(false) }
    var showFilterMenu by remember { mutableStateOf(false) }

    // Filter states
    var staffList by remember { mutableStateOf<List<Pair<String, String>>>(emptyList()) }
    var selectedStaffs by remember { mutableStateOf<Set<String>>(emptySet()) }
    var showStaffFilterDialog by remember { mutableStateOf(false) }

    var showDatePickerDialog by remember { mutableStateOf(false) }
    var showDateRangePickerDialog by remember { mutableStateOf(false) }
    var showPaymentMethodDialog by remember { mutableStateOf(false) }
    var selectedDate by remember { mutableStateOf<Long?>(null) }
    var selectedStartDate by remember { mutableStateOf<Long?>(null) }
    var selectedEndDate by remember { mutableStateOf<Long?>(null) }
    var selectedPaymentMethod by remember { mutableStateOf<String?>(null) }
    var selectedProduct by remember { mutableStateOf<String?>(null) }
    var productList by remember { mutableStateOf<Set<String>>(emptySet()) }
    var paymentMethodList by remember { mutableStateOf<Set<String>>(emptySet()) }

    // Initialize Firestore
    val firestore = FirebaseFirestore.getInstance()

    fun fetchStaffList() {
        firestore.collection("uSer")
            .whereEqualTo("role", "Nhân Viên")
            .get()
            .addOnSuccessListener { result ->
                val staff = result.documents.mapNotNull { doc ->
                    val id = doc.id
                    val name = doc.getString("name")
                    if (name != null) Pair(id, name) else null
                }
                staffList = staff
                showStaffFilterDialog = true
            }
            .addOnFailureListener { e ->
                errorMessage = "Lỗi khi tải danh sách nhân viên: ${e.message}"
            }
    }

    fun extractProductsAndPaymentMethods(sales: List<SaleDetail>) {
        val products = mutableSetOf<String>()
        val paymentMethods = mutableSetOf<String>()

        sales.forEach { sale ->
            sale.products.forEach { product ->
                product["product_name"]?.toString()?.let { products.add(it) }
            }
            sale.paymentMethod?.let {
                // Chuẩn hóa tên phương thức thanh toán
                when {
                    it.contains("Tian", ignoreCase = true) -> paymentMethods.add("Tiền Mặt")
                    it.contains("Chuyen", ignoreCase = true) -> paymentMethods.add("Chuyển Khoản")
                    else -> paymentMethods.add(it)
                }
            }
        }

        productList = products
        paymentMethodList = paymentMethods
    }

    fun applyFilters() {
        filteredSalesData = originalSalesData.filter { sale ->
            // Staff filter
            val staffFilter = selectedStaffs.isEmpty() || selectedStaffs.contains(sale.sellerId)

            // Single date filter
            val dateFilter = selectedDate?.let { date ->
                val calendar = Calendar.getInstance().apply { timeInMillis = date * 1000 }
                val selectedDateStr = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(calendar.time)
                sale.saleDate.startsWith(selectedDateStr)
            } ?: true

            // Date range filter
            val dateRangeFilter = if (selectedStartDate != null && selectedEndDate != null) {
                try {
                    val saleDate = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).parse(sale.saleDate)
                    saleDate?.let {
                        val saleTime = it.time
                        val startDate = Date(selectedStartDate!! * 1000)
                        val endDate = Date(selectedEndDate!! * 1000)

                        // Reset time portion to midnight
                        val calendar = Calendar.getInstance()
                        calendar.time = startDate
                        calendar.set(Calendar.HOUR_OF_DAY, 0)
                        calendar.set(Calendar.MINUTE, 0)
                        calendar.set(Calendar.SECOND, 0)
                        calendar.set(Calendar.MILLISECOND, 0)
                        val startTime = calendar.timeInMillis

                        calendar.time = endDate
                        calendar.set(Calendar.HOUR_OF_DAY, 23)
                        calendar.set(Calendar.MINUTE, 59)
                        calendar.set(Calendar.SECOND, 59)
                        calendar.set(Calendar.MILLISECOND, 999)
                        val endTime = calendar.timeInMillis

                        saleTime in startTime..endTime
                    } ?: false
                } catch (e: Exception) {
                    false
                }
            } else {
                true
            }

            // Payment method filter
            val paymentMethodFilter = selectedPaymentMethod?.let {
                when {
                    it == "Tiền Mặt" && (sale.paymentMethod?.contains("Tian", ignoreCase = true) == true) -> true
                    it == "Chuyển Khoản" && (sale.paymentMethod?.contains("Chuyen", ignoreCase = true) == true) -> true
                    else -> sale.paymentMethod == it
                }
            } ?: true

            // Product filter
            val productFilter = selectedProduct?.let { productName ->
                sale.products.any { it["product_name"] == productName }
            } ?: true

            staffFilter && dateFilter && dateRangeFilter && paymentMethodFilter && productFilter
        }
    }

    // Animation effect
    LaunchedEffect(Unit) {
        delay(300)
        showContent = true
    }

    // Fetch user role and then sales data
    LaunchedEffect(userId) {
        if (userId != null) {
            // First get user role
            firestore.collection("uSer").document(userId)
                .get()
                .addOnSuccessListener { userDocument ->
                    userRole = userDocument.getString("role")

                    // Then fetch sales data based on role
                    val query = if (userRole == "ADMIN") {
                        firestore.collection("order_history")
                    } else {
                        firestore.collection("order_history")
                            .whereEqualTo("seller_id", userId)
                    }

                    query.get()
                        .addOnSuccessListener { documents ->
                            originalSalesData = documents.map { document ->
                                SaleDetail(
                                    sellerId = document.getString("seller_id") ?: "",
                                    sellerName = document.getString("user_name") ?: "Không rõ",
                                    saleDate = document.getString("time") ?: "Không rõ",
                                    products = document.get("products") as? List<Map<String, Any>> ?: emptyList(),
                                    totalAmount = document.getDouble("total") ?: 0.0,
                                    paymentMethod = document.getString("paymentMethod")?.takeIf { it.isNotEmpty() }
                                )
                            }
                            extractProductsAndPaymentMethods(originalSalesData)
                            filteredSalesData = originalSalesData
                            isLoading = false
                        }
                        .addOnFailureListener { e ->
                            errorMessage = "Lỗi khi tải dữ liệu đơn hàng: ${e.message}"
                            isLoading = false
                        }
                }
                .addOnFailureListener { e ->
                    errorMessage = "Lỗi khi tải thông tin người dùng: ${e.message}"
                    isLoading = false
                }
        } else {
            errorMessage = "Không có ID người dùng"
            isLoading = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (userRole == "ADMIN") "Thống kê toàn hệ thống" else "Đơn hàng của bạn",
                        color = Color.White
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                },
                actions = {
                    if (userRole != null) {
                        IconButton(onClick = { showFilterMenu = true }) {
                            Icon(
                                imageVector = Icons.Default.FilterList,
                                contentDescription = "Filter",
                                tint = Color.White
                            )
                        }
                        DropdownMenu(
                            expanded = showFilterMenu,
                            onDismissRequest = { showFilterMenu = false }
                        ) {
                            if (userRole == "ADMIN") {
                                DropdownMenuItem(
                                    text = { Text("Lọc theo nhân viên") },
                                    onClick = {
                                        showFilterMenu = false
                                        fetchStaffList()
                                    }
                                )
                            }
                            DropdownMenuItem(
                                text = { Text("Lọc theo ngày cụ thể") },
                                onClick = {
                                    showFilterMenu = false
                                    showDatePickerDialog = true
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Lọc theo khoảng ngày") },
                                onClick = {
                                    showFilterMenu = false
                                    showDateRangePickerDialog = true
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Lọc theo phương thức thanh toán") },
                                onClick = {
                                    showFilterMenu = false
                                    if (paymentMethodList.isNotEmpty()) {
                                        showPaymentMethodDialog = true
                                    } else {
                                        errorMessage = "Không có phương thức thanh toán nào để lọc"
                                    }
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Lọc theo sản phẩm") },
                                onClick = {
                                    showFilterMenu = false
                                    if (productList.isNotEmpty()) {
                                        selectedProduct = productList.first()
                                    } else {
                                        errorMessage = "Không có sản phẩm nào để lọc"
                                    }
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Xóa bộ lọc") },
                                onClick = {
                                    showFilterMenu = false
                                    selectedStaffs = emptySet()
                                    selectedDate = null
                                    selectedStartDate = null
                                    selectedEndDate = null
                                    selectedPaymentMethod = null
                                    selectedProduct = null
                                    filteredSalesData = originalSalesData
                                }
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            )
        },
        containerColor = Color(0xFFF5F5F5)
    ) { paddingValues ->
        AnimatedVisibility(
            visible = showContent,
            enter = fadeIn(animationSpec = tween(500)),
            exit = fadeOut(animationSpec = tween(500))
        ) {
            Column(
                modifier = Modifier
                    .padding(paddingValues)
                    .padding(16.dp)
            ) {
                if (isLoading) {
                    LoadingIndicator()
                } else if (errorMessage != null) {
                    ErrorCard(message = errorMessage!!)
                } else {
                    // Summary cards
                    if (filteredSalesData.isNotEmpty()) {
                        val totalSales = filteredSalesData.size
                        val totalRevenue = filteredSalesData.sumOf { it.totalAmount }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            SummaryCard(
                                title = "Tổng đơn",
                                value = totalSales.toString(),
                                icon = Icons.Default.Receipt,
                                modifier = Modifier.weight(1f)
                            )
                            SummaryCard(
                                title = "Doanh thu",
                                value = "%,.0f đ".format(totalRevenue),
                                icon = Icons.Default.MonetizationOn,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    // Sales list
                    if (filteredSalesData.isEmpty()) {
                        EmptyState()
                    } else {
                        Text(
                            text = "Chi tiết đơn hàng",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )

                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(filteredSalesData) { sale ->
                                SaleCard(sale = sale, userRole = userRole)
                            }
                        }
                    }
                }
            }
        }
    }

    // Staff filter dialog with scroll
    if (showStaffFilterDialog) {
        AlertDialog(
            onDismissRequest = { showStaffFilterDialog = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        showStaffFilterDialog = false
                        applyFilters()
                    }
                ) {
                    Text("Áp dụng")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showStaffFilterDialog = false }
                ) {
                    Text("Hủy")
                }
            },
            title = { Text("Chọn nhân viên để lọc") },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState())
                ) {
                    if (staffList.isEmpty()) {
                        Text("Không có nhân viên nào")
                    } else {
                        staffList.forEach { (id, name) ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        selectedStaffs = if (selectedStaffs.contains(id)) {
                                            selectedStaffs - id
                                        } else {
                                            selectedStaffs + id
                                        }
                                    }
                                    .padding(vertical = 4.dp)
                            ) {
                                Checkbox(
                                    checked = selectedStaffs.contains(id),
                                    onCheckedChange = {
                                        selectedStaffs = if (it) selectedStaffs + id else selectedStaffs - id
                                    }
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(text = name)
                            }
                        }
                    }
                }
            }
        )
    }

    // Date picker dialog
    if (showDatePickerDialog) {
        val datePickerState = rememberDatePickerState()
        DatePickerDialog(
            onDismissRequest = { showDatePickerDialog = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        selectedDate = datePickerState.selectedDateMillis?.let { it / 1000 }
                        selectedStartDate = null
                        selectedEndDate = null
                        showDatePickerDialog = false
                        applyFilters()
                    }
                ) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDatePickerDialog = false }
                ) {
                    Text("Hủy")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    // Date range picker dialog with improved UI
// Thay thế đoạn code Date range picker dialog bằng code sau:
    if (showDateRangePickerDialog) {
        val startDateState = rememberDatePickerState()
        val endDateState = rememberDatePickerState()

        Dialog(
            onDismissRequest = { showDateRangePickerDialog = false }
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .padding(16.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(
                        text = "Chọn khoảng ngày",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    Text("Từ ngày:", style = MaterialTheme.typography.bodyMedium)
                    DatePicker(
                        state = startDateState,
                        title = null,
                        headline = null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(400.dp)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text("Đến ngày:", style = MaterialTheme.typography.bodyMedium)
                    DatePicker(
                        state = endDateState,
                        title = null,
                        headline = null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(400.dp)
                    )

                    if (startDateState.selectedDateMillis != null &&
                        endDateState.selectedDateMillis != null &&
                        startDateState.selectedDateMillis!! > endDateState.selectedDateMillis!!) {
                        Text(
                            text = "Ngày kết thúc phải sau ngày bắt đầu",
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(
                            onClick = { showDateRangePickerDialog = false }
                        ) {
                            Text("Hủy")
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        TextButton(
                            onClick = {
                                selectedStartDate = startDateState.selectedDateMillis?.let { it / 1000 }
                                selectedEndDate = endDateState.selectedDateMillis?.let { it / 1000 }
                                selectedDate = null
                                showDateRangePickerDialog = false
                                applyFilters()
                            },
                            enabled = startDateState.selectedDateMillis != null &&
                                    endDateState.selectedDateMillis != null &&
                                    (startDateState.selectedDateMillis!! <= endDateState.selectedDateMillis!!)
                        ) {
                            Text("Áp dụng")
                        }
                    }
                }
            }
        }
    }

    // Payment method filter dialog with scroll
    if (showPaymentMethodDialog) {
        AlertDialog(
            onDismissRequest = { showPaymentMethodDialog = false },
            title = { Text("Chọn phương thức thanh toán") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showPaymentMethodDialog = false
                        applyFilters()
                    }
                ) {
                    Text("Áp dụng")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        selectedPaymentMethod = null
                        showPaymentMethodDialog = false
                        applyFilters()
                    }
                ) {
                    Text("Hủy")
                }
            },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState())
                ) {
                    // Only use payment methods from paymentMethodList excluding "Tiền Mặt" and "Chuyển Khoản"
                    paymentMethodList.filter { it !in listOf("Tiền Mặt", "Chuyển Khoản") }.forEach { method ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selectedPaymentMethod = method
                                }
                                .padding(vertical = 4.dp)
                        ) {
                            RadioButton(
                                selected = selectedPaymentMethod == method,
                                onClick = { selectedPaymentMethod = method }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = method)
                        }
                    }
                    if (paymentMethodList.filter { it !in listOf("Tiền Mặt", "Chuyển Khoản") }.isEmpty()) {
                        Text("Không có phương thức thanh toán nào để lọc")
                    }
                }
            }
        )
    }

    // Product filter dialog with scroll
    if (selectedProduct != null) {
        AlertDialog(
            onDismissRequest = { selectedProduct = null },
            title = { Text("Chọn sản phẩm") },
            confirmButton = {
                TextButton(
                    onClick = {
                        applyFilters()
                        selectedProduct = null
                    }
                ) {
                    Text("Áp dụng")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { selectedProduct = null }
                ) {
                    Text("Hủy")
                }
            },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState())
                ) {
                    if (productList.isEmpty()) {
                        Text("Không có sản phẩm nào")
                    } else {
                        productList.forEach { product ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        selectedProduct = product
                                    }
                                    .padding(vertical = 4.dp)
                            ) {
                                RadioButton(
                                    selected = selectedProduct == product,
                                    onClick = { selectedProduct = product }
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(text = product)
                            }
                        }
                    }
                }
            }
        )
    }
}

@Composable
private fun LoadingIndicator() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgress()
            Spacer(modifier = Modifier.height(16.dp))
            Text("Đang tải dữ liệu...", color = MaterialTheme.colorScheme.onSurface)
        }
    }
}

@Composable
private fun CircularProgress() {
    Box(
        modifier = Modifier
            .size(48.dp)
            .border(
                width = 4.dp,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                shape = CircleShape
            )
            .padding(8.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .border(
                    width = 4.dp,
                    color = MaterialTheme.colorScheme.primary,
                    shape = CircleShape
                )
                .clip(CircleShape)
        )
    }
}

@Composable
private fun ErrorCard(message: String) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Lỗi",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = message,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
        }
    }
}

@Composable
private fun SummaryCard(title: String, value: String, icon: androidx.compose.ui.graphics.vector.ImageVector, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier
            .shadow(
                elevation = 4.dp,
                shape = RoundedCornerShape(12.dp)
            ),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primary,
                                MaterialTheme.colorScheme.secondary
                            )
                        ),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
                Text(
                    text = value,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun EmptyState() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.BarChart,
            contentDescription = "No data",
            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
            modifier = Modifier.size(64.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Không có đơn hàng nào",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
        Text(
            text = "Bạn chưa có đơn hàng nào được ghi nhận",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun SaleCard(sale: SaleDetail, userRole: String?) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { /* Handle click if needed */ },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Seller name and sale date (for all users)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = sale.sellerName,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = sale.saleDate,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))

            // Products list
            Column(
                modifier = Modifier.padding(vertical = 8.dp)
            ) {
                sale.products.forEach { product ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "• ${product["product_name"]}",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = "x${product["quantity"]}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                    }
                }
            }

            // Total amount
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Tổng cộng:",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = "%,.0f đ".format(sale.totalAmount),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

data class SaleDetail(
    val sellerId: String = "",
    val sellerName: String,
    val saleDate: String,
    val products: List<Map<String, Any>>,
    val totalAmount: Double,
    val paymentMethod: String?
)