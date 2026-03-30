package com.phuocpham.pumiah.ui.screen.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.phuocpham.pumiah.data.model.FinancialSummary
import com.phuocpham.pumiah.data.model.Transaction
import com.phuocpham.pumiah.data.model.UiState
import com.phuocpham.pumiah.ui.navigation.Screen
import com.phuocpham.pumiah.ui.theme.Green
import com.phuocpham.pumiah.ui.theme.Red
import com.phuocpham.pumiah.ui.utils.formatVnd
import com.phuocpham.pumiah.ui.utils.formatVndSigned
import com.phuocpham.pumiah.viewmodel.CategoryAmount
import com.phuocpham.pumiah.viewmodel.DashboardPeriod
import com.phuocpham.pumiah.viewmodel.DashboardViewModel
import com.phuocpham.pumiah.viewmodel.PeriodBar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    navController: NavController,
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val summaryState by viewModel.summary.collectAsState()
    val recentTransactions by viewModel.recentTransactions.collectAsState()
    val expenseByCategory by viewModel.expenseByCategory.collectAsState()
    val incomeByCategory by viewModel.incomeByCategory.collectAsState()
    val periodBars by viewModel.periodBars.collectAsState()
    val period by viewModel.period.collectAsState()
    val wallets by viewModel.wallets.collectAsState()
    val selectedWalletId by viewModel.selectedWalletId.collectAsState()
    var expandedWallet by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Tổng quan", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = { viewModel.loadData() }) {
                        Icon(Icons.Default.Refresh, "Làm mới")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Wallet selector + period chips
            item {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Wallet dropdown
                    if (wallets.isNotEmpty()) {
                        ExposedDropdownMenuBox(expanded = expandedWallet, onExpandedChange = { expandedWallet = it }) {
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
                    // Period chips
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        DashboardPeriod.entries.forEach { p ->
                            FilterChip(
                                selected = period == p,
                                onClick = { viewModel.setPeriod(p) },
                                label = {
                                    Text(when (p) {
                                        DashboardPeriod.WEEK  -> "Tuần"
                                        DashboardPeriod.MONTH -> "Tháng"
                                        DashboardPeriod.YEAR  -> "Năm"
                                    })
                                }
                            )
                        }
                    }
                }
            }

            when (val state = summaryState) {
                is UiState.Loading -> item {
                    Box(Modifier.fillMaxWidth().height(150.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                is UiState.Success -> {
                    item { SummaryCards(state.data) }
                }
                is UiState.Error -> item { Text(state.message, color = MaterialTheme.colorScheme.error) }
                else -> {}
            }

            // Expense pie chart
            if (expenseByCategory.isNotEmpty()) {
                item {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Chi tiêu theo danh mục", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                            Spacer(Modifier.height(12.dp))
                            SimplePieChart(data = expenseByCategory, totalColor = Red)
                        }
                    }
                }
            }

            // Income pie chart
            if (incomeByCategory.isNotEmpty()) {
                item {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Thu nhập theo danh mục", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                            Spacer(Modifier.height(12.dp))
                            SimplePieChart(data = incomeByCategory, totalColor = Green)
                        }
                    }
                }
            }

            // Bar chart
            if (periodBars.isNotEmpty()) {
                item {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Thu nhập vs Chi tiêu", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                            Spacer(Modifier.height(12.dp))
                            SimpleBarChart(data = periodBars)
                        }
                    }
                }
            }

            // Spending trend line chart
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Xu Hướng Chi Tiêu", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                        Spacer(Modifier.height(12.dp))
                        SpendingTrendChart(data = periodBars)
                    }
                }
            }

            // Recent transactions
            if (recentTransactions.isNotEmpty()) {
                item {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Giao dịch gần đây", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                        TextButton(onClick = { navController.navigate(Screen.Transactions.route) }) {
                            Text("Xem tất cả")
                        }
                    }
                }
                items(recentTransactions) { t -> TransactionRow(t) }
            }

            item { Spacer(Modifier.height(16.dp)) }
        }
    }
}

@Composable
fun SimplePieChart(data: List<CategoryAmount>, totalColor: Color) {
    val total = data.sumOf { it.amount }.takeIf { it > 0 } ?: return
    val colors = data.map { item ->
        try { Color(android.graphics.Color.parseColor(item.color)) }
        catch (e: Exception) { totalColor }
    }

    Row(verticalAlignment = Alignment.CenterVertically) {
        // Pie
        androidx.compose.foundation.Canvas(modifier = Modifier.size(120.dp)) {
            var startAngle = -90f
            data.forEachIndexed { i, item ->
                val sweep = (item.amount / total * 360.0).toFloat()
                drawArc(
                    color = colors[i],
                    startAngle = startAngle,
                    sweepAngle = sweep,
                    useCenter = true,
                    topLeft = Offset(4.dp.toPx(), 4.dp.toPx()),
                    size = Size(size.width - 8.dp.toPx(), size.height - 8.dp.toPx())
                )
                startAngle += sweep
            }
            // White center hole
            drawCircle(color = Color.White, radius = size.minDimension / 4)
        }

        Spacer(Modifier.width(12.dp))

        // Legend
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            data.take(5).forEachIndexed { i, item ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier.size(10.dp).clip(CircleShape)
                            .background(colors[i])
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        item.name,
                        fontSize = 12.sp,
                        modifier = Modifier.weight(1f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        formatVnd(item.amount),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }
            }
            if (data.size > 5) {
                Text(
                    "+${data.size - 5} danh mục khác",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            }
        }
    }
}

@Composable
fun SimpleBarChart(data: List<PeriodBar>) {
    val maxVal = data.maxOf { maxOf(it.income, it.expense) }.takeIf { it > 0 } ?: 1.0
    val incColor = Green
    val expColor = Red
    val chartHeight = 120.dp

    Column {
        // Legend
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(10.dp).clip(CircleShape).background(incColor))
                Spacer(Modifier.width(4.dp))
                Text("Thu nhập", fontSize = 11.sp)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(10.dp).clip(CircleShape).background(expColor))
                Spacer(Modifier.width(4.dp))
                Text("Chi tiêu", fontSize = 11.sp)
            }
        }
        Spacer(Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth().height(chartHeight),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.Bottom
        ) {
            data.forEach { bar ->
                val incFrac = (bar.income / maxVal).toFloat().coerceIn(0f, 1f)
                val expFrac = (bar.expense / maxVal).toFloat().coerceIn(0f, 1f)
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Bottom,
                    modifier = Modifier.weight(1f).height(chartHeight)
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        if (bar.income > 0) {
                            Box(
                                Modifier.width(6.dp)
                                    .fillMaxHeight(incFrac)
                                    .background(incColor)
                            )
                        }
                        Spacer(Modifier.width(2.dp))
                        if (bar.expense > 0) {
                            Box(
                                Modifier.width(6.dp)
                                    .fillMaxHeight(expFrac)
                                    .background(expColor)
                            )
                        }
                    }
                    if (data.size <= 14) {
                        Text(bar.label, fontSize = 8.sp, maxLines = 1)
                    }
                }
            }
        }
    }
}

@Composable
fun SummaryCards(summary: FinancialSummary) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text("Số dư", color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f), fontSize = 13.sp)
                Text(
                    formatVnd(summary.balance),
                    color = MaterialTheme.colorScheme.onPrimary,
                    fontSize = 28.sp, fontWeight = FontWeight.Bold
                )
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Card(modifier = Modifier.weight(1f), colors = CardDefaults.cardColors(containerColor = Green.copy(alpha = 0.1f))) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Icon(Icons.Default.TrendingUp, null, tint = Green, modifier = Modifier.size(18.dp))
                        Text("Thu nhập", fontSize = 12.sp, color = Green)
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(formatVnd(summary.totalIncome), fontWeight = FontWeight.Bold, color = Green)
                }
            }
            Card(modifier = Modifier.weight(1f), colors = CardDefaults.cardColors(containerColor = Red.copy(alpha = 0.1f))) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Icon(Icons.Default.TrendingDown, null, tint = Red, modifier = Modifier.size(18.dp))
                        Text("Chi tiêu", fontSize = 12.sp, color = Red)
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(formatVnd(summary.totalExpense), fontWeight = FontWeight.Bold, color = Red)
                }
            }
        }
    }
}

@Composable
fun SpendingTrendChart(data: List<PeriodBar>) {
    if (data.isEmpty() || data.all { it.expense == 0.0 && it.income == 0.0 }) {
        Box(
            modifier = Modifier.fillMaxWidth().height(120.dp),
            contentAlignment = Alignment.Center
        ) {
            Text("Chưa có dữ liệu", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f), fontSize = 13.sp)
        }
        return
    }

    val incColor = Green
    val expColor = Red
    val gridColor = Color.LightGray.copy(alpha = 0.4f)
    val maxVal = data.maxOf { maxOf(it.income, it.expense) }.takeIf { it > 0 } ?: 1.0

    // Legend
    Row(horizontalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.padding(bottom = 8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(10.dp).clip(CircleShape).background(incColor))
            Spacer(Modifier.width(4.dp))
            Text("Thu nhập", fontSize = 11.sp)
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(10.dp).clip(CircleShape).background(expColor))
            Spacer(Modifier.width(4.dp))
            Text("Chi tiêu", fontSize = 11.sp)
        }
    }

    val chartHeight = 130.dp
    val labelHeight = 18.dp

    androidx.compose.foundation.Canvas(
        modifier = Modifier.fillMaxWidth().height(chartHeight + labelHeight)
    ) {
        val w = size.width
        val h = chartHeight.toPx()
        val n = data.size
        if (n < 2) return@Canvas

        val stepX = w / (n - 1).toFloat()

        // Horizontal grid lines (3 lines)
        for (i in 0..2) {
            val y = h * (1f - i / 2f)
            drawLine(color = gridColor, start = Offset(0f, y), end = Offset(w, y), strokeWidth = 1.dp.toPx())
        }

        fun xOf(i: Int) = i * stepX
        fun yOf(v: Double) = h * (1f - (v / maxVal).toFloat()).coerceIn(0f, 1f)

        // Income path
        val incPath = Path()
        data.forEachIndexed { i, bar ->
            val x = xOf(i); val y = yOf(bar.income)
            if (i == 0) incPath.moveTo(x, y) else incPath.lineTo(x, y)
        }
        drawPath(incPath, color = incColor, style = Stroke(width = 2.dp.toPx()))

        // Expense path
        val expPath = Path()
        data.forEachIndexed { i, bar ->
            val x = xOf(i); val y = yOf(bar.expense)
            if (i == 0) expPath.moveTo(x, y) else expPath.lineTo(x, y)
        }
        drawPath(expPath, color = expColor, style = Stroke(width = 2.dp.toPx()))

        // Dots
        data.forEachIndexed { i, bar ->
            val x = xOf(i)
            if (bar.income > 0)
                drawCircle(incColor, radius = 3.dp.toPx(), center = Offset(x, yOf(bar.income)))
            if (bar.expense > 0)
                drawCircle(expColor, radius = 3.dp.toPx(), center = Offset(x, yOf(bar.expense)))
        }
    }

    // X-axis labels (show subset to avoid overlap)
    val step = when {
        data.size <= 7  -> 1
        data.size <= 14 -> 2
        data.size <= 31 -> 5
        else            -> data.size / 6
    }
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        data.forEachIndexed { i, bar ->
            if (i % step == 0 || i == data.lastIndex) {
                Text(bar.label, fontSize = 8.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
            } else {
                Spacer(Modifier.width(1.dp))
            }
        }
    }
}

@Composable
fun TransactionRow(transaction: Transaction) {
    val isIncome = transaction.type == "income"
    val amountColor = if (isIncome) Green else Red
    val sign = if (isIncome) "+" else "-"
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.padding(12.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(transaction.category?.name ?: "Giao dịch", fontWeight = FontWeight.Medium)
                if (!transaction.notes.isNullOrBlank())
                    Text(transaction.notes, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                Text(transaction.transactionDate.take(10), fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
            }
            Text(formatVndSigned(transaction.amount, isIncome), color = amountColor, fontWeight = FontWeight.SemiBold)
        }
    }
}
