package com.phuocpham.pumiah.ui.screen.category

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.phuocpham.pumiah.data.model.Category
import com.phuocpham.pumiah.data.model.UiState
import com.phuocpham.pumiah.ui.utils.friendlyDeleteError
import com.phuocpham.pumiah.viewmodel.CategoryViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryScreen(viewModel: CategoryViewModel = hiltViewModel()) {
    val categoriesState by viewModel.categories.collectAsState()
    val saveState by viewModel.saveState.collectAsState()
    val insight by viewModel.insight.collectAsState()
    var showForm by remember { mutableStateOf(false) }
    var editingCategory by remember { mutableStateOf<Category?>(null) }
    var pendingDeleteId by remember { mutableStateOf<String?>(null) }
    var pendingDeleteName by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var activeTab by remember { mutableStateOf("expense") }  // "expense" | "income" | "all"

    LaunchedEffect(saveState) {
        when (saveState) {
            is UiState.Success -> { showForm = false; editingCategory = null; viewModel.resetSaveState() }
            is UiState.Error   -> { errorMessage = friendlyDeleteError((saveState as UiState.Error).message); viewModel.resetSaveState() }
            else -> {}
        }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Danh mục", fontWeight = FontWeight.Bold) }) },
        floatingActionButton = {
            FloatingActionButton(onClick = { editingCategory = null; showForm = true }) {
                Icon(Icons.Default.Add, null)
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Insights
            insight?.let { ins ->
                item {
                    Spacer(Modifier.height(4.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ins.mostFrequent?.let { cat ->
                            Card(
                                modifier = Modifier.weight(1f),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Icon(Icons.Default.Whatshot, null, tint = Color(0xFFE17055), modifier = Modifier.size(16.dp))
                                        Text("Dùng nhiều nhất", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f))
                                    }
                                    Text(cat.name, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                    Text("${ins.mostFrequentCount} giao dịch", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.6f))
                                }
                            }
                        }
                        ins.fastestGrowing?.let { cat ->
                            Card(
                                modifier = Modifier.weight(1f),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Icon(Icons.Default.TrendingUp, null, tint = Color(0xFFE17055), modifier = Modifier.size(16.dp))
                                        Text("Tăng nhanh nhất", fontSize = 11.sp, color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.7f))
                                    }
                                    Text(cat.name, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                    Text("+${ins.growthPercent.toInt()}% tháng này", fontSize = 11.sp, color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.6f))
                                }
                            }
                        }
                    }
                }
            }

            // Tab filter
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = activeTab == "expense", onClick = { activeTab = "expense" }, label = { Text("Chi tiêu") })
                    FilterChip(selected = activeTab == "income", onClick = { activeTab = "income" }, label = { Text("Thu nhập") })
                    FilterChip(selected = activeTab == "all", onClick = { activeTab = "all" }, label = { Text("Tất cả") })
                }
            }

            when (val state = categoriesState) {
                is UiState.Loading -> item { Box(Modifier.fillMaxWidth().height(100.dp), Alignment.Center) { CircularProgressIndicator() } }
                is UiState.Success -> {
                    val userCats = state.data.filter { it.userId != null }
                        .filter { activeTab == "all" || it.type == activeTab || it.type == "all" }
                    if (userCats.isEmpty()) {
                        item {
                            Box(Modifier.fillMaxWidth().padding(top = 48.dp), Alignment.Center) {
                                Text("Chưa có danh mục tùy chỉnh", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                            }
                        }
                    } else {
                        items(userCats, key = { it.id }) { cat ->
                            CategoryItem(cat,
                                onEdit = { editingCategory = cat; showForm = true },
                                onDelete = { pendingDeleteId = cat.id; pendingDeleteName = cat.name })
                        }
                    }
                }
                is UiState.Error -> item { Text(state.message, color = MaterialTheme.colorScheme.error) }
                else -> {}
            }
            item { Spacer(Modifier.height(80.dp)) }
        }
    }

    if (showForm) {
        CategoryFormDialog(
            category = editingCategory,
            onDismiss = { showForm = false; editingCategory = null },
            onSave = { name, color, icon, type ->
                if (editingCategory != null) {
                    viewModel.updateCategory(editingCategory!!.id, name, color, icon)
                } else {
                    viewModel.createCategory(name, color, icon, type)
                }
            },
            isLoading = saveState is UiState.Loading
        )
    }

    if (pendingDeleteId != null) {
        AlertDialog(
            onDismissRequest = { pendingDeleteId = null },
            title = { Text("Xác nhận xóa") },
            text = { Text("Bạn có chắc muốn xóa danh mục \"$pendingDeleteName\"?\nLưu ý: không thể xóa nếu đang dùng trong giao dịch hoặc ngân sách.") },
            confirmButton = {
                Button(
                    onClick = { viewModel.deleteCategory(pendingDeleteId!!); pendingDeleteId = null },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Xóa") }
            },
            dismissButton = { TextButton(onClick = { pendingDeleteId = null }) { Text("Huỷ") } }
        )
    }

    errorMessage?.let { msg ->
        AlertDialog(
            onDismissRequest = { errorMessage = null },
            title = { Text("Không thể xóa") },
            text = { Text(msg) },
            confirmButton = { TextButton(onClick = { errorMessage = null }) { Text("Đóng") } }
        )
    }
}

@Composable
fun CategoryItem(category: Category, onEdit: () -> Unit, onDelete: () -> Unit) {
    val color = try { Color(android.graphics.Color.parseColor(category.color)) } catch (e: Exception) { MaterialTheme.colorScheme.primary }
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(36.dp).clip(CircleShape).background(color))
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(category.name, fontWeight = FontWeight.Medium)
                Text(
                    when (category.type) { "income" -> "Thu nhập"; "expense" -> "Chi tiêu"; else -> "Tất cả" },
                    fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
            IconButton(onClick = onEdit, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Default.Edit, null, modifier = Modifier.size(18.dp))
            }
            IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryFormDialog(
    category: Category?,
    onDismiss: () -> Unit,
    onSave: (name: String, color: String, icon: String, type: String) -> Unit,
    isLoading: Boolean
) {
    val isEdit = category != null
    var name by remember { mutableStateOf(category?.name ?: "") }
    var color by remember { mutableStateOf(category?.color ?: "#6C5CE7") }
    var selectedType by remember { mutableStateOf(category?.type ?: "expense") }
    val colors = listOf("#6C5CE7", "#00B894", "#FDCB6E", "#E17055", "#74B9FF", "#FD79A8", "#00CEC9", "#636E72")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isEdit) "Sửa danh mục" else "Thêm danh mục") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name, onValueChange = { name = it },
                    label = { Text("Tên danh mục") }, singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                if (!isEdit) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(selected = selectedType == "expense", onClick = { selectedType = "expense" }, label = { Text("Chi tiêu") })
                        FilterChip(selected = selectedType == "income", onClick = { selectedType = "income" }, label = { Text("Thu nhập") })
                    }
                }
                Text("Màu sắc", style = MaterialTheme.typography.labelMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    colors.forEach { c ->
                        val parsed = try { Color(android.graphics.Color.parseColor(c)) } catch (e: Exception) { Color.Gray }
                        val isSelected = color == c
                        Box(
                            modifier = Modifier
                                .size(if (isSelected) 34.dp else 28.dp)
                                .clip(CircleShape)
                                .background(parsed),
                            contentAlignment = Alignment.Center
                        ) {
                            FilledIconButton(
                                onClick = { color = c },
                                modifier = Modifier.fillMaxSize(),
                                colors = IconButtonDefaults.filledIconButtonColors(containerColor = parsed)
                            ) {}
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(name, color, "attach_money", selectedType) },
                enabled = !isLoading && name.isNotBlank()
            ) {
                if (isLoading) CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                else Text(if (isEdit) "Cập nhật" else "Lưu")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Huỷ") } }
    )
}
