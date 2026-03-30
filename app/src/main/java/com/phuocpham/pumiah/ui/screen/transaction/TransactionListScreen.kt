package com.phuocpham.pumiah.ui.screen.transaction

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.phuocpham.pumiah.data.model.Category
import com.phuocpham.pumiah.data.model.Transaction
import com.phuocpham.pumiah.data.model.UiState
import com.phuocpham.pumiah.data.model.Wallet
import com.phuocpham.pumiah.ui.theme.Green
import com.phuocpham.pumiah.ui.theme.Red
import com.phuocpham.pumiah.ui.utils.formatVndSigned
import com.phuocpham.pumiah.viewmodel.DashboardViewModel
import com.phuocpham.pumiah.viewmodel.TransactionViewModel
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionListScreen(viewModel: TransactionViewModel = hiltViewModel()) {
    val transactionsState by viewModel.transactions.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val wallets by viewModel.wallets.collectAsState()
    val saveState by viewModel.saveState.collectAsState()

    var showForm by remember { mutableStateOf(false) }
    var editingTransaction by remember { mutableStateOf<Transaction?>(null) }
    var pendingDeleteId by remember { mutableStateOf<String?>(null) }

    val selectedWalletId by viewModel.selectedWalletId.collectAsState()
    var expandedWalletFilter by remember { mutableStateOf(false) }

    // Filter state (client-side)
    var searchQuery by remember { mutableStateOf("") }
    var typeFilter by remember { mutableStateOf("all") }  // "all" | "income" | "expense"
    var categoryFilter by remember { mutableStateOf<String?>(null) }
    var expandedCategoryFilter by remember { mutableStateOf(false) }

    LaunchedEffect(saveState) {
        if (saveState is UiState.Success) {
            showForm = false
            editingTransaction = null
            viewModel.resetSaveState()
        }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Giao dịch", fontWeight = FontWeight.Bold) }) },
        floatingActionButton = {
            if (wallets.isNotEmpty()) {
                FloatingActionButton(onClick = { showForm = true; editingTransaction = null }) {
                    Icon(Icons.Default.Add, "Thêm giao dịch")
                }
            }
        }
    ) { padding ->
        if (wallets.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), Alignment.Center) {
                Text("Vui lòng tạo ví trước khi thêm giao dịch",
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    modifier = Modifier.padding(32.dp))
            }
            return@Scaffold
        }
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            // Filter bar
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)) {
                // Wallet selector
                if (wallets.isNotEmpty()) {
                    ExposedDropdownMenuBox(expanded = expandedWalletFilter, onExpandedChange = { expandedWalletFilter = it }) {
                        OutlinedTextField(
                            value = wallets.find { it.id == selectedWalletId }?.name ?: "",
                            onValueChange = {}, readOnly = true,
                            label = { Text("Ví") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expandedWalletFilter) },
                            modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth()
                        )
                        ExposedDropdownMenu(expanded = expandedWalletFilter, onDismissRequest = { expandedWalletFilter = false }) {
                            wallets.forEach { w ->
                                DropdownMenuItem(
                                    text = { Text(w.name) },
                                    onClick = { viewModel.setWallet(w.id); expandedWalletFilter = false }
                                )
                            }
                        }
                    }
                }
                // Search field
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    label = { Text("Tìm kiếm ghi chú...") },
                    leadingIcon = { Icon(Icons.Default.Search, null, modifier = Modifier.size(20.dp)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                // Type + category filters
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    FilterChip(selected = typeFilter == "all", onClick = { typeFilter = "all" }, label = { Text("Tất cả") })
                    FilterChip(selected = typeFilter == "income", onClick = { typeFilter = "income" }, label = { Text("Thu nhập") })
                    FilterChip(selected = typeFilter == "expense", onClick = { typeFilter = "expense" }, label = { Text("Chi tiêu") })
                }
                // Category filter
                if (categories.isNotEmpty()) {
                    ExposedDropdownMenuBox(expanded = expandedCategoryFilter, onExpandedChange = { expandedCategoryFilter = it }) {
                        OutlinedTextField(
                            value = categories.find { it.id == categoryFilter }?.name ?: "Tất cả danh mục",
                            onValueChange = {}, readOnly = true,
                            label = { Text("Danh mục") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expandedCategoryFilter) },
                            modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth()
                        )
                        ExposedDropdownMenu(expanded = expandedCategoryFilter, onDismissRequest = { expandedCategoryFilter = false }) {
                            DropdownMenuItem(
                                text = { Text("Tất cả danh mục") },
                                onClick = { categoryFilter = null; expandedCategoryFilter = false }
                            )
                            categories.forEach { cat ->
                                DropdownMenuItem(
                                    text = { Text(cat.name) },
                                    onClick = { categoryFilter = cat.id; expandedCategoryFilter = false }
                                )
                            }
                        }
                    }
                }
            }

            when (val state = transactionsState) {
                is UiState.Loading -> Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator() }
                is UiState.Success -> {
                    val filtered = state.data.filter { t ->
                        (typeFilter == "all" || t.type == typeFilter) &&
                        (categoryFilter == null || t.categoryId == categoryFilter) &&
                        (searchQuery.isBlank() || t.notes?.contains(searchQuery, ignoreCase = true) == true)
                    }
                    if (filtered.isEmpty()) {
                        Box(Modifier.fillMaxSize(), Alignment.Center) {
                            Text(
                                if (state.data.isEmpty()) "Chưa có giao dịch nào" else "Không tìm thấy giao dịch",
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                            )
                        }
                    } else {
                        LazyColumn(
                            Modifier.fillMaxSize().padding(horizontal = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            item { Spacer(Modifier.height(4.dp)) }
                            items(filtered, key = { it.id }) { t ->
                                TransactionItem(
                                    transaction = t,
                                    onEdit = { editingTransaction = t; showForm = true },
                                    onDelete = { pendingDeleteId = t.id }
                                )
                            }
                            item { Spacer(Modifier.height(80.dp)) }
                        }
                    }
                }
                is UiState.Error -> Box(Modifier.fillMaxSize(), Alignment.Center) {
                    Text(state.message, color = MaterialTheme.colorScheme.error)
                }
                else -> {}
            }
        }
    }

    if (pendingDeleteId != null) {
        AlertDialog(
            onDismissRequest = { pendingDeleteId = null },
            title = { Text("Xác nhận xóa") },
            text = { Text("Bạn có chắc muốn xóa giao dịch này không?") },
            confirmButton = {
                Button(
                    onClick = { viewModel.deleteTransaction(pendingDeleteId!!); pendingDeleteId = null },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Xóa") }
            },
            dismissButton = { TextButton(onClick = { pendingDeleteId = null }) { Text("Huỷ") } }
        )
    }

    if (showForm) {
        TransactionFormDialog(
            transaction = editingTransaction,
            categories = categories,
            wallets = wallets,
            onDismiss = { showForm = false; editingTransaction = null },
            onSave = { categoryId, amount, type, date, notes, walletId ->
                if (editingTransaction != null) {
                    viewModel.updateTransaction(editingTransaction!!.id, categoryId, amount, type, date, notes, walletId)
                } else {
                    viewModel.createTransaction(categoryId, amount, type, date, notes, walletId)
                }
            },
            isLoading = saveState is UiState.Loading
        )
    }
}

@Composable
fun TransactionItem(transaction: Transaction, onEdit: () -> Unit, onDelete: () -> Unit) {
    val isIncome = transaction.type == "income"
    val amountColor = if (isIncome) Green else Red
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(transaction.category?.name ?: "—", fontWeight = FontWeight.Medium)
                if (!transaction.notes.isNullOrBlank())
                    Text(transaction.notes, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(transaction.transactionDate.take(10), fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                    if (transaction.walletId != null) {
                        Text("• Ví chung", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f))
                    }
                }
            }
            Text(formatVndSigned(transaction.amount, isIncome),
                color = amountColor, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(end = 8.dp))
            IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Default.Edit, null, modifier = Modifier.size(18.dp))
            }
            IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionFormDialog(
    transaction: Transaction?,
    categories: List<Category>,
    wallets: List<Wallet>,
    onDismiss: () -> Unit,
    onSave: (categoryId: String, amount: Double, type: String, date: String, notes: String?, walletId: String?) -> Unit,
    isLoading: Boolean
) {
    val fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    val defaultWalletId = remember(wallets) {
        transaction?.walletId ?: DashboardViewModel.defaultWallet(wallets)
    }
    var selectedType by remember { mutableStateOf(transaction?.type ?: "expense") }
    var selectedCategoryId by remember { mutableStateOf(transaction?.categoryId ?: "") }
    var selectedWalletId by remember { mutableStateOf(defaultWalletId) }
    var amount by remember { mutableStateOf(transaction?.amount?.toString() ?: "") }
    var notes by remember { mutableStateOf(transaction?.notes ?: "") }
    var expandedCat by remember { mutableStateOf(false) }
    var expandedWallet by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }

    val initialMillis = remember {
        transaction?.transactionDate?.take(10)?.let {
            runCatching { LocalDate.parse(it, fmt).atStartOfDay(ZoneId.of("UTC")).toInstant().toEpochMilli() }.getOrNull()
        } ?: LocalDate.now().atStartOfDay(ZoneId.of("UTC")).toInstant().toEpochMilli()
    }
    var selectedDateMillis by remember { mutableStateOf<Long>(initialMillis) }
    val selectedDateText = Instant.ofEpochMilli(selectedDateMillis).atZone(ZoneId.of("UTC")).toLocalDate().format(fmt)

    val filteredCats = if (selectedType == "income") categories.filter { it.type == "income" || it.type == "all" }
                       else categories.filter { it.type == "expense" || it.type == "all" }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = selectedDateMillis)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { selectedDateMillis = it }
                    showDatePicker = false
                }) { Text("Chọn") }
            },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("Huỷ") } }
        ) { DatePicker(state = datePickerState) }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (transaction != null) "Sửa giao dịch" else "Thêm giao dịch") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = selectedType == "expense", onClick = { selectedType = "expense"; selectedCategoryId = "" }, label = { Text("Chi tiêu") })
                    FilterChip(selected = selectedType == "income", onClick = { selectedType = "income"; selectedCategoryId = "" }, label = { Text("Thu nhập") })
                }
                ExposedDropdownMenuBox(expanded = expandedCat, onExpandedChange = { expandedCat = it }) {
                    OutlinedTextField(
                        value = filteredCats.find { it.id == selectedCategoryId }?.name ?: "Chọn danh mục",
                        onValueChange = {}, readOnly = true, label = { Text("Danh mục") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expandedCat) },
                        modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth()
                    )
                    ExposedDropdownMenu(expanded = expandedCat, onDismissRequest = { expandedCat = false }) {
                        filteredCats.forEach { cat ->
                            DropdownMenuItem(text = { Text(cat.name) }, onClick = { selectedCategoryId = cat.id; expandedCat = false })
                        }
                    }
                }
                if (wallets.isNotEmpty()) {
                    ExposedDropdownMenuBox(expanded = expandedWallet, onExpandedChange = { expandedWallet = it }) {
                        OutlinedTextField(
                            value = wallets.find { it.id == selectedWalletId }?.name ?: "",
                            onValueChange = {}, readOnly = true, label = { Text("Ví") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expandedWallet) },
                            modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth()
                        )
                        ExposedDropdownMenu(expanded = expandedWallet, onDismissRequest = { expandedWallet = false }) {
                            wallets.forEach { w ->
                                DropdownMenuItem(text = { Text(w.name) }, onClick = { selectedWalletId = w.id; expandedWallet = false })
                            }
                        }
                    }
                }
                OutlinedTextField(value = amount, onValueChange = { amount = it }, label = { Text("Số tiền (₫)") },
                    singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(
                    value = selectedDateText, onValueChange = {}, readOnly = true,
                    label = { Text("Ngày giao dịch") },
                    trailingIcon = { IconButton(onClick = { showDatePicker = true }) { Icon(Icons.Default.CalendarMonth, "Chọn ngày") } },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(value = notes, onValueChange = { notes = it }, label = { Text("Ghi chú (tuỳ chọn)") },
                    modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(selectedCategoryId, amount.toDoubleOrNull() ?: 0.0, selectedType, selectedDateText, notes.ifBlank { null }, selectedWalletId) },
                enabled = !isLoading && selectedCategoryId.isNotBlank() && amount.isNotBlank()
            ) {
                if (isLoading) CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                else Text("Lưu")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Huỷ") } }
    )
}
