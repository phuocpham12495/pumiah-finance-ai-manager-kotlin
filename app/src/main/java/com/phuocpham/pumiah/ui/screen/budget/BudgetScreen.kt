package com.phuocpham.pumiah.ui.screen.budget

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.phuocpham.pumiah.data.model.Budget
import com.phuocpham.pumiah.data.model.Category
import com.phuocpham.pumiah.data.model.UiState
import com.phuocpham.pumiah.ui.theme.Green
import com.phuocpham.pumiah.ui.theme.Red
import com.phuocpham.pumiah.ui.theme.Yellow
import com.phuocpham.pumiah.ui.utils.formatVnd
import com.phuocpham.pumiah.viewmodel.BudgetViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetScreen(viewModel: BudgetViewModel = hiltViewModel()) {
    val budgetsState by viewModel.budgets.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val wallets by viewModel.wallets.collectAsState()
    val selectedWalletId by viewModel.selectedWalletId.collectAsState()
    val saveState by viewModel.saveState.collectAsState()
    var showForm by remember { mutableStateOf(false) }
    var editingBudget by remember { mutableStateOf<Budget?>(null) }
    var expandedWallet by remember { mutableStateOf(false) }
    var pendingDeleteId by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(saveState) {
        if (saveState is UiState.Success) { showForm = false; editingBudget = null; viewModel.resetSaveState() }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Ngân sách", fontWeight = FontWeight.Bold) }) },
        floatingActionButton = {
            if (wallets.isNotEmpty()) {
                FloatingActionButton(onClick = { editingBudget = null; showForm = true }) { Icon(Icons.Default.Add, null) }
            }
        }
    ) { padding ->
        if (wallets.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), Alignment.Center) {
                Text("Vui lòng tạo ví trước khi thêm ngân sách",
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    modifier = Modifier.padding(32.dp))
            }
            return@Scaffold
        }
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            // Wallet selector
            if (wallets.isNotEmpty()) {
                ExposedDropdownMenuBox(
                    expanded = expandedWallet,
                    onExpandedChange = { expandedWallet = it },
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp).fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = wallets.find { it.id == selectedWalletId }?.name ?: "",
                        onValueChange = {}, readOnly = true,
                        label = { Text("Ví") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expandedWallet) },
                        modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth()
                    )
                    ExposedDropdownMenu(expanded = expandedWallet, onDismissRequest = { expandedWallet = false }) {
                        wallets.forEach { w ->
                            DropdownMenuItem(
                                text = { Text(w.name) },
                                onClick = { viewModel.setWallet(w.id); expandedWallet = false }
                            )
                        }
                    }
                }
            }

            when (val state = budgetsState) {
                is UiState.Loading -> Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator() }
                is UiState.Success -> {
                    if (state.data.isEmpty()) {
                        Box(Modifier.fillMaxSize(), Alignment.Center) {
                            Text("Chưa có ngân sách nào", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                        }
                    } else {
                        LazyColumn(
                            Modifier.fillMaxSize().padding(horizontal = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            item { Spacer(Modifier.height(4.dp)) }
                            items(state.data, key = { it.id }) { budget ->
                                BudgetItem(
                                    budget = budget,
                                    onEdit = { editingBudget = budget; showForm = true },
                                    onDelete = { pendingDeleteId = budget.id }
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

    // Delete confirmation
    if (pendingDeleteId != null) {
        AlertDialog(
            onDismissRequest = { pendingDeleteId = null },
            title = { Text("Xóa ngân sách?") },
            text = { Text("Bạn có chắc muốn xóa ngân sách này không?") },
            confirmButton = {
                Button(
                    onClick = { viewModel.deleteBudget(pendingDeleteId!!); pendingDeleteId = null },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Xóa") }
            },
            dismissButton = { TextButton(onClick = { pendingDeleteId = null }) { Text("Huỷ") } }
        )
    }

    if (showForm) {
        BudgetFormDialog(
            budget = editingBudget,
            categories = categories,
            onDismiss = { showForm = false; editingBudget = null },
            onSave = { catId, amount ->
                if (editingBudget != null) viewModel.updateBudget(editingBudget!!.id, amount)
                else viewModel.createBudget(catId, amount)
            },
            isLoading = saveState is UiState.Loading
        )
    }
}

@Composable
fun BudgetItem(budget: Budget, onEdit: () -> Unit, onDelete: () -> Unit) {
    val progress = if (budget.amount > 0) (budget.spent / budget.amount).coerceIn(0.0, 1.0).toFloat() else 0f
    val percent = (progress * 100).toInt()
    val progressColor = when {
        progress >= 1.0f -> Red
        progress >= 0.8f -> Yellow
        else -> Green
    }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(budget.category?.name ?: "—", fontWeight = FontWeight.SemiBold)
                Row {
                    IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Edit, null, modifier = Modifier.size(18.dp))
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                    }
                }
            }

            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth().height(8.dp),
                color = progressColor,
                trackColor = progressColor.copy(alpha = 0.2f)
            )
            Spacer(Modifier.height(6.dp))

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    "Đã dùng: ${formatVnd(budget.spent)}",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
                Text("/ ${formatVnd(budget.amount)}", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
            }

            // Warning text — mirrors React behaviour
            if (percent >= 100) {
                Spacer(Modifier.height(4.dp))
                Text("Đã vượt ngân sách!", color = Red, fontSize = 13.sp, fontWeight = FontWeight.Medium)
            } else if (percent >= 80) {
                Spacer(Modifier.height(4.dp))
                Text("Sắp đạt giới hạn! ($percent%)", color = Yellow, fontSize = 13.sp, fontWeight = FontWeight.Medium)
            }

            Spacer(Modifier.height(4.dp))
            Text(
                "${budget.startDate} → ${budget.endDate}",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetFormDialog(
    budget: Budget? = null,
    categories: List<Category>,
    onDismiss: () -> Unit,
    onSave: (categoryId: String, amount: Double) -> Unit,
    isLoading: Boolean
) {
    val isEdit = budget != null
    var selectedCategoryId by remember { mutableStateOf(budget?.categoryId ?: "") }
    var amount by remember { mutableStateOf(budget?.amount?.toString() ?: "") }
    var expandedCat by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isEdit) "Sửa ngân sách" else "Tạo ngân sách") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    if (isEdit) "Chỉnh sửa giới hạn ngân sách tháng hiện tại"
                    else "Ngân sách sẽ áp dụng cho tháng hiện tại",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
                if (!isEdit) {
                    ExposedDropdownMenuBox(expanded = expandedCat, onExpandedChange = { expandedCat = it }) {
                        OutlinedTextField(
                            value = categories.find { it.id == selectedCategoryId }?.name ?: "Chọn danh mục",
                            onValueChange = {}, readOnly = true, label = { Text("Danh mục") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expandedCat) },
                            modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth()
                        )
                        ExposedDropdownMenu(expanded = expandedCat, onDismissRequest = { expandedCat = false }) {
                            categories.forEach { cat ->
                                DropdownMenuItem(
                                    text = { Text(cat.name) },
                                    onClick = { selectedCategoryId = cat.id; expandedCat = false }
                                )
                            }
                        }
                    }
                } else {
                    OutlinedTextField(
                        value = budget?.category?.name ?: categories.find { it.id == selectedCategoryId }?.name ?: "",
                        onValueChange = {}, readOnly = true, label = { Text("Danh mục") },
                        modifier = Modifier.fillMaxWidth(), enabled = false
                    )
                }
                OutlinedTextField(
                    value = amount, onValueChange = { amount = it },
                    label = { Text("Giới hạn (₫)") },
                    singleLine = true, modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(selectedCategoryId, amount.toDoubleOrNull() ?: 0.0) },
                enabled = !isLoading && selectedCategoryId.isNotBlank() && amount.isNotBlank()
            ) {
                if (isLoading) CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                else Text(if (isEdit) "Lưu" else "Tạo")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Huỷ") } }
    )
}
