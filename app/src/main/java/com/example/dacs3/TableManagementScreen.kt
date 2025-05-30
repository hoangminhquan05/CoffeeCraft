package com.example.dacs3

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.dacs3.Model.Table
import com.example.dacs3.Model.User
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TableManagementScreen(navController: NavController) {
    val tables = remember { mutableStateListOf<Table>() }
    val isLoading = remember { mutableStateOf(true) }
    val selectedTableId = remember { mutableStateOf<String?>(null) }
    val showAddDialog = remember { mutableStateOf(false) }
    val showEditDialog = remember { mutableStateOf(false) }
    val tableToEdit = remember { mutableStateOf<Table?>(null) }
    val tableToDelete = remember { mutableStateOf<Table?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var listenerRegistration by remember { mutableStateOf<ListenerRegistration?>(null) }
    val currentUser = remember { mutableStateOf<User?>(null) }

    var tableName by remember { mutableStateOf("") }
    var seats by remember { mutableStateOf("") }
    var editTableName by remember { mutableStateOf("") }
    var editSeats by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        val userId = Firebase.auth.currentUser?.uid
        if (userId != null) {
            Firebase.firestore.collection("uSer").document(userId).get()
                .addOnSuccessListener { document ->
                    currentUser.value = document.toObject(User::class.java)
                }
        }
    }

    LaunchedEffect(Unit) {
        val db = Firebase.firestore
        listenerRegistration = db.collection("ql_ban")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.w("TableManagement", "Listen failed.", error)
                    isLoading.value = false
                    scope.launch {
                        snackbarHostState.showSnackbar("Lỗi khi tải dữ liệu bàn")
                    }
                    return@addSnapshotListener
                }

                val tempTables = mutableListOf<Table>()
                snapshot?.documents?.forEach { document ->
                    val table = document.toObject(Table::class.java)
                    table?.id_ban = document.id
                    table?.let { tempTables.add(it) }
                }
                tables.clear()
                tables.addAll(tempTables)
                isLoading.value = false
            }
    }

    DisposableEffect(Unit) {
        onDispose {
            listenerRegistration?.remove()
        }
    }

    fun updateTableStatus(tableId: String, newStatus: String) {
        Firebase.firestore.collection("ql_ban")
            .document(tableId)
            .update("trang_thai", newStatus)
            .addOnSuccessListener {
                scope.launch {
                    snackbarHostState.showSnackbar("Đã cập nhật trạng thái bàn")
                }
            }
            .addOnFailureListener { e ->
                Log.w("TableManagement", "Error updating status", e)
                scope.launch {
                    snackbarHostState.showSnackbar("Lỗi khi cập nhật trạng thái bàn")
                }
            }
    }

    fun addTable(table: Table) {
        val newDoc = Firebase.firestore.collection("ql_ban").document()
        val newTable = table.copy(id_ban = newDoc.id)
        newDoc.set(newTable)
            .addOnSuccessListener {
                scope.launch {
                    snackbarHostState.showSnackbar("Đã thêm bàn mới")
                }
            }
            .addOnFailureListener { e ->
                Log.w("TableManagement", "Error adding table", e)
                scope.launch {
                    snackbarHostState.showSnackbar("Lỗi khi thêm bàn")
                }
            }
    }

    fun updateTable(table: Table) {
        Firebase.firestore.collection("ql_ban")
            .document(table.id_ban)
            .update(mapOf(
                "so_ban" to table.so_ban,
                "cho_ngoi" to table.cho_ngoi
            ))
            .addOnSuccessListener {
                scope.launch {
                    snackbarHostState.showSnackbar("Đã cập nhật bàn")
                    selectedTableId.value = null
                }
            }
            .addOnFailureListener { e ->
                Log.w("TableManagement", "Error updating table", e)
                scope.launch {
                    snackbarHostState.showSnackbar("Lỗi khi cập nhật bàn")
                }
            }
    }

    fun deleteTable(tableId: String) {
        Firebase.firestore.collection("ql_ban")
            .document(tableId)
            .delete()
            .addOnSuccessListener {
                scope.launch {
                    snackbarHostState.showSnackbar("Đã xóa bàn")
                    selectedTableId.value = null
                }
            }
            .addOnFailureListener { e ->
                Log.w("TableManagement", "Error deleting table", e)
                scope.launch {
                    snackbarHostState.showSnackbar("Lỗi khi xóa bàn")
                }
            }
    }

    if (showAddDialog.value) {
        AlertDialog(
            onDismissRequest = { showAddDialog.value = false },
            title = { Text("Thêm bàn mới") },
            containerColor = Color.White,
            tonalElevation = 4.dp,
            shape = MaterialTheme.shapes.large,
            text = {
                Column(modifier = Modifier.padding(4.dp)) {
                    OutlinedTextField(
                        value = tableName,
                        onValueChange = { tableName = it },
                        label = { Text("Tên bàn") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = seats,
                        onValueChange = { seats = it },
                        label = { Text("Số ghế") },
                        keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    val seatsNum = seats.toIntOrNull() ?: 0
                    if (tableName.isNotBlank() && seatsNum > 0) {
                        addTable(Table(so_ban = tableName, cho_ngoi = seatsNum))
                        showAddDialog.value = false
                        tableName = ""
                        seats = ""
                    } else {
                        scope.launch {
                            snackbarHostState.showSnackbar("Vui lòng nhập đầy đủ thông tin")
                        }
                    }
                }) {
                    Text("Thêm")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog.value = false }) {
                    Text("Hủy")
                }
            }
        )
    }

    if (showEditDialog.value && tableToEdit.value != null) {
        LaunchedEffect(showEditDialog.value) {
            if (showEditDialog.value) {
                editTableName = tableToEdit.value?.so_ban ?: ""
                editSeats = tableToEdit.value?.cho_ngoi.toString()
            }
        }

        AlertDialog(
            onDismissRequest = { showEditDialog.value = false },
            title = { Text("Chỉnh sửa bàn") },
            containerColor = Color.White,
            tonalElevation = 4.dp,
            shape = MaterialTheme.shapes.large,
            text = {
                Column(modifier = Modifier.padding(4.dp)) {
                    OutlinedTextField(
                        value = editTableName,
                        onValueChange = { editTableName = it },
                        label = { Text("Tên bàn") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = editSeats,
                        onValueChange = { editSeats = it },
                        label = { Text("Số ghế") },
                        keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    val seatsNum = editSeats.toIntOrNull() ?: 0
                    if (editTableName.isNotBlank() && seatsNum > 0) {
                        val updatedTable = tableToEdit.value!!.copy(
                            so_ban = editTableName,
                            cho_ngoi = seatsNum
                        )
                        updateTable(updatedTable)
                        showEditDialog.value = false
                    } else {
                        scope.launch {
                            snackbarHostState.showSnackbar("Vui lòng nhập đầy đủ thông tin")
                        }
                    }
                }) {
                    Text("Lưu")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditDialog.value = false }) {
                    Text("Hủy")
                }
            }
        )
    }

    tableToDelete.value?.let { table ->
        AlertDialog(
            onDismissRequest = { tableToDelete.value = null },
            title = { Text("Xác nhận xóa") },
            text = { Text("Bạn có chắc chắn muốn xóa ${table.so_ban}?") },
            confirmButton = {
                Button(onClick = {
                    deleteTable(table.id_ban)
                    tableToDelete.value = null
                }) {
                    Text("Xóa")
                }
            },
            dismissButton = {
                TextButton(onClick = { tableToDelete.value = null }) {
                    Text("Hủy")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Quản lý bàn") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (currentUser.value?.role == "ADMIN") {
                        IconButton(onClick = { showAddDialog.value = true }) {
                            Icon(Icons.Filled.Add, contentDescription = "Add Table")
                        }
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        if (isLoading.value) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 150.dp),
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize(),
                contentPadding = PaddingValues(8.dp)
            ) {
                items(tables) { table ->
                    TableItem(
                        table = table,
                        isSelected = selectedTableId.value == table.id_ban,
                        isAdmin = currentUser.value?.role == "ADMIN",
                        onTableClick = {
                            selectedTableId.value = if (selectedTableId.value == table.id_ban) null else table.id_ban
                        },
                        onStatusChange = { newStatus ->
                            updateTableStatus(table.id_ban, newStatus)
                            selectedTableId.value = null
                        },
                        onEditClick = {
                            tableToEdit.value = table
                            showEditDialog.value = true
                        },
                        onDeleteClick = {
                            tableToDelete.value = table
                        }
                    )
                }
            }
        }
    }
}


@Composable
fun TableItem(
    table: Table,
    isSelected: Boolean,
    isAdmin: Boolean,
    onTableClick: () -> Unit,
    onStatusChange: (String) -> Unit,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    val backgroundColor = when (table.trang_thai) {
        "Trống" -> Color.Green.copy(alpha = 0.2f)
        "Đang hoạt động" -> Color.Red.copy(alpha = 0.2f)
        else -> Color.Gray.copy(alpha = 0.2f)
    }

    Column(
        modifier = Modifier.padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = backgroundColor),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
            onClick = onTableClick
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Filled.TableRestaurant,
                    contentDescription = "Bàn",
                    modifier = Modifier.size(48.dp),
                    tint = Color.Black
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = table.so_ban,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = table.trang_thai,
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = "Chỗ ngồi: ${table.cho_ngoi}",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }

        if (isSelected) {
            Spacer(modifier = Modifier.height(8.dp))
            when (table.trang_thai) {
                "Trống" -> {
                    Button(
                        onClick = { onStatusChange("Đang hoạt động") },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Hoạt động")
                    }
                }
                "Đang hoạt động" -> {
                    Button(
                        onClick = { onStatusChange("Trống") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Gray)
                    ) {
                        Text("Trống")
                    }
                }
            }

            if (isAdmin) {
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Button(
                        onClick = onEditClick,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Blue)
                    ) {
                        Text("Sửa")
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Button(
                        onClick = onDeleteClick,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                    ) {
                        Text("Xóa")
                    }
                }
            }
        }
    }
}