package com.phuocpham.pumiah.ui.screen.goal

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.phuocpham.pumiah.data.model.Goal
import com.phuocpham.pumiah.data.model.UiState
import com.phuocpham.pumiah.ui.theme.Green
import com.phuocpham.pumiah.ui.theme.Purple
import com.phuocpham.pumiah.ui.utils.formatVnd
import com.phuocpham.pumiah.viewmodel.GoalViewModel
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoalScreen(viewModel: GoalViewModel = hiltViewModel()) {
    val goalsState by viewModel.goals.collectAsState()
    val saveState by viewModel.saveState.collectAsState()
    var showForm by remember { mutableStateOf(false) }
    var contributionGoal by remember { mutableStateOf<Goal?>(null) }
    var pendingDeleteGoal by remember { mutableStateOf<Goal?>(null) }

    LaunchedEffect(saveState) {
        if (saveState is UiState.Success) { showForm = false; viewModel.resetSaveState() }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Mục tiêu tiết kiệm", fontWeight = FontWeight.Bold) }) },
        floatingActionButton = {
            FloatingActionButton(onClick = { showForm = true }) { Icon(Icons.Default.Add, null) }
        }
    ) { padding ->
        when (val state = goalsState) {
            is UiState.Loading -> Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator() }
            is UiState.Success -> {
                if (state.data.isEmpty()) {
                    Box(Modifier.fillMaxSize().padding(padding), Alignment.Center) {
                        Text("Chưa có mục tiêu nào",
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                    }
                } else {
                    LazyColumn(
                        Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        item { Spacer(Modifier.height(4.dp)) }
                        items(state.data, key = { it.id }) { goal ->
                            GoalItem(
                                goal = goal,
                                onContribute = { contributionGoal = goal },
                                onDelete = { pendingDeleteGoal = goal }
                            )
                        }
                        item { Spacer(Modifier.height(80.dp)) }
                    }
                }
            }
            is UiState.Error -> Box(Modifier.fillMaxSize().padding(padding), Alignment.Center) {
                Text(state.message, color = MaterialTheme.colorScheme.error)
            }
            else -> {}
        }
    }

    if (showForm) {
        GoalFormDialog(
            onDismiss = { showForm = false },
            onSave = { name, target, date -> viewModel.createGoal(name, target, date) },
            isLoading = saveState is UiState.Loading
        )
    }

    contributionGoal?.let { goal ->
        ContributionDialog(
            goal = goal,
            onDismiss = { contributionGoal = null },
            onSave = { amount -> viewModel.addContribution(goal, amount); contributionGoal = null }
        )
    }

    // Delete confirmation
    pendingDeleteGoal?.let { goal ->
        AlertDialog(
            onDismissRequest = { pendingDeleteGoal = null },
            title = { Text("Xác nhận xóa") },
            text = { Text("Bạn có chắc muốn xóa mục tiêu \"${goal.name}\"?") },
            confirmButton = {
                Button(
                    onClick = { viewModel.deleteGoal(goal.id); pendingDeleteGoal = null },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Xóa") }
            },
            dismissButton = { TextButton(onClick = { pendingDeleteGoal = null }) { Text("Huỷ") } }
        )
    }
}

@Composable
fun GoalItem(goal: Goal, onContribute: () -> Unit, onDelete: () -> Unit) {
    val progress = if (goal.targetAmount > 0)
        (goal.currentAmount / goal.targetAmount).coerceIn(0.0, 1.0).toFloat() else 0f
    val statusColor = when (goal.status) { "achieved" -> Green else -> Purple }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text(goal.name, fontWeight = FontWeight.SemiBold)
                    Text("Hạn: ${goal.targetDate}", fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                }
                Text(
                    if (goal.status == "achieved") "✓ Đạt" else "${(progress * 100).toInt()}%",
                    color = statusColor, fontWeight = FontWeight.Medium
                )
                IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Delete, null,
                        tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                }
            }
            Spacer(Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth().height(8.dp),
                color = statusColor,
                trackColor = statusColor.copy(alpha = 0.2f)
            )
            Spacer(Modifier.height(4.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(formatVnd(goal.currentAmount), fontSize = 13.sp)
                Text(formatVnd(goal.targetAmount), fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
            }
            if (goal.status == "active") {
                Spacer(Modifier.height(8.dp))
                OutlinedButton(onClick = onContribute, modifier = Modifier.fillMaxWidth()) {
                    Text("Thêm tiết kiệm")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoalFormDialog(
    onDismiss: () -> Unit,
    onSave: (name: String, target: Double, date: String) -> Unit,
    isLoading: Boolean
) {
    var name by remember { mutableStateOf("") }
    var target by remember { mutableStateOf("") }
    var selectedDateMillis by remember { mutableStateOf<Long?>(null) }
    var showDatePicker by remember { mutableStateOf(false) }

    val fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    val selectedDateText = selectedDateMillis?.let {
        Instant.ofEpochMilli(it).atZone(ZoneId.of("UTC")).toLocalDate().format(fmt)
    } ?: ""

    // DatePickerDialog shown on top of the form dialog
    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = selectedDateMillis
                ?: LocalDate.now().plusMonths(1)
                    .atStartOfDay(ZoneId.of("UTC")).toInstant().toEpochMilli()
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    selectedDateMillis = datePickerState.selectedDateMillis
                    showDatePicker = false
                }) { Text("Chọn") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Huỷ") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Tạo mục tiêu") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name, onValueChange = { name = it },
                    label = { Text("Tên mục tiêu") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = target, onValueChange = { target = it },
                    label = { Text("Số tiền mục tiêu (₫)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                // Date picker field
                OutlinedTextField(
                    value = selectedDateText,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Ngày mục tiêu") },
                    placeholder = { Text("Chọn ngày") },
                    trailingIcon = {
                        IconButton(onClick = { showDatePicker = true }) {
                            Icon(Icons.Default.CalendarMonth, "Chọn ngày")
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(name, target.toDoubleOrNull() ?: 0.0, selectedDateText) },
                enabled = !isLoading && name.isNotBlank() && target.isNotBlank() && selectedDateText.isNotBlank()
            ) {
                if (isLoading) CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                else Text("Tạo")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Huỷ") } }
    )
}

@Composable
fun ContributionDialog(goal: Goal, onDismiss: () -> Unit, onSave: (Double) -> Unit) {
    var amount by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Thêm tiết kiệm — ${goal.name}") },
        text = {
            OutlinedTextField(
                value = amount, onValueChange = { amount = it },
                label = { Text("Số tiền (₫)") },
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            Button(
                onClick = { onSave(amount.toDoubleOrNull() ?: 0.0) },
                enabled = amount.isNotBlank()
            ) { Text("Lưu") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Huỷ") } }
    )
}
