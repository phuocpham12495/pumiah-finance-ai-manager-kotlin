package com.phuocpham.pumiah.ui.screen.category

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.phuocpham.pumiah.data.model.Category
import com.phuocpham.pumiah.data.model.UiState
import com.phuocpham.pumiah.ui.utils.friendlyDeleteError
import com.phuocpham.pumiah.viewmodel.CategoryViewModel

// ── Icon registry ─────────────────────────────────────────────────────────────

val categoryIcons: List<Pair<String, ImageVector>> = listOf(
    "restaurant"          to Icons.Default.Restaurant,
    "shopping_cart"       to Icons.Default.ShoppingCart,
    "directions_car"      to Icons.Default.DirectionsCar,
    "home"                to Icons.Default.Home,
    "local_hospital"      to Icons.Default.LocalHospital,
    "school"              to Icons.Default.School,
    "laptop"              to Icons.Default.Laptop,
    "sports_soccer"       to Icons.Default.SportsSoccer,
    "movie"               to Icons.Default.Movie,
    "savings"             to Icons.Default.Savings,
    "attach_money"        to Icons.Default.AttachMoney,
    "work"                to Icons.Default.Work,
    "card_giftcard"       to Icons.Default.CardGiftcard,
    "flight"              to Icons.Default.Flight,
    "fitness_center"      to Icons.Default.FitnessCenter,
    "local_cafe"          to Icons.Default.LocalCafe,
    "pets"                to Icons.Default.Pets,
    "child_care"          to Icons.Default.ChildCare,
    "local_pharmacy"      to Icons.Default.LocalPharmacy,
    "spa"                 to Icons.Default.Spa,
    "beach_access"        to Icons.Default.BeachAccess,
    "hotel"               to Icons.Default.Hotel,
    "local_gas_station"   to Icons.Default.LocalGasStation,
    "train"               to Icons.Default.Train,
    "directions_bus"      to Icons.Default.DirectionsBus,
    "phone"               to Icons.Default.Phone,
    "wifi"                to Icons.Default.Wifi,
    "music_note"          to Icons.Default.MusicNote,
    "brush"               to Icons.Default.Brush,
    "menu_book"           to Icons.Default.MenuBook,
    "shopping_bag"        to Icons.Default.ShoppingBag,
    "local_pizza"         to Icons.Default.LocalPizza,
    "credit_card"         to Icons.Default.CreditCard,
    "receipt_long"        to Icons.Default.ReceiptLong,
    "sports_basketball"   to Icons.Default.SportsBasketball,
    "sports_tennis"       to Icons.Default.SportsTennis,
    "outdoor_grill"       to Icons.Default.OutdoorGrill,
    "park"                to Icons.Default.Park,
    "pedal_bike"          to Icons.Default.PedalBike,
    "electric_bolt"       to Icons.Default.ElectricBolt,
    "water_drop"          to Icons.Default.WaterDrop,
    "volunteer_activism"  to Icons.Default.VolunteerActivism,
    "security"            to Icons.Default.Security,
    "tv"                  to Icons.Default.Tv,
    "mic"                 to Icons.Default.Mic,
    "local_atm"           to Icons.Default.LocalAtm,
    "celebration"         to Icons.Default.Celebration,
    "two_wheeler"         to Icons.Default.TwoWheeler,
    "fastfood"            to Icons.Default.Fastfood,
    "diamond"             to Icons.Default.Diamond,
)

fun iconVectorFor(key: String): ImageVector =
    categoryIcons.find { it.first == key }?.second ?: Icons.Default.AttachMoney

// ── HSV helpers ───────────────────────────────────────────────────────────────

private fun Color.toHsv(): FloatArray {
    val hsv = FloatArray(3)
    android.graphics.Color.colorToHSV(this.toArgb(), hsv)
    return hsv
}

private fun hsvColor(h: Float, s: Float, v: Float): Color =
    Color(android.graphics.Color.HSVToColor(floatArrayOf(h, s, v)))

// ── Screen ────────────────────────────────────────────────────────────────────

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
    var activeTab by remember { mutableStateOf("expense") }
    var isRefreshing by remember { mutableStateOf(false) }

    LaunchedEffect(saveState) {
        when (saveState) {
            is UiState.Success -> { showForm = false; editingCategory = null; viewModel.resetSaveState() }
            is UiState.Error   -> { errorMessage = friendlyDeleteError((saveState as UiState.Error).message); viewModel.resetSaveState() }
            else -> {}
        }
    }
    LaunchedEffect(categoriesState) {
        if (categoriesState !is UiState.Loading) isRefreshing = false
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Danh mục", fontWeight = FontWeight.Bold) }) },
        floatingActionButton = {
            FloatingActionButton(onClick = { editingCategory = null; showForm = true }) {
                Icon(Icons.Default.Add, null)
            }
        }
    ) { padding ->
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = { isRefreshing = true; viewModel.loadCategories() },
            modifier = Modifier.fillMaxSize().padding(padding)
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
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

// ── Category list item ────────────────────────────────────────────────────────

@Composable
fun CategoryItem(category: Category, onEdit: () -> Unit, onDelete: () -> Unit) {
    val color = runCatching { Color(android.graphics.Color.parseColor(category.color)) }.getOrDefault(MaterialTheme.colorScheme.primary)
    val iconVector = iconVectorFor(category.icon)
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(36.dp).clip(CircleShape).background(color.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(iconVector, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
            }
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

// ── HSV Color Picker ──────────────────────────────────────────────────────────

@Composable
fun HsvColorPicker(
    color: Color,
    onColorChange: (Color) -> Unit
) {
    val initialHsv = remember(color) { color.toHsv() }
    var hue by remember { mutableStateOf(initialHsv[0]) }
    var sat by remember { mutableStateOf(initialHsv[1]) }
    var value by remember { mutableStateOf(initialHsv[2]) }

    val hueColor = hsvColor(hue, 1f, 1f)
    val currentColor = hsvColor(hue, sat, value)

    LaunchedEffect(hue, sat, value) { onColorChange(currentColor) }

    var svSize by remember { mutableStateOf(IntSize.Zero) }
    var hueSize by remember { mutableStateOf(IntSize.Zero) }

    val presetColors = listOf(
        "#6C5CE7", "#00B894", "#FDCB6E", "#E17055", "#74B9FF",
        "#FD79A8", "#00CEC9", "#636E72", "#D63031", "#E84393",
        "#F9CA24", "#6AB04C", "#22A6B3", "#BE2EDD", "#4834D4"
    )

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        // Preset quick swatches
        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            items(presetColors) { c ->
                val parsed = runCatching { Color(android.graphics.Color.parseColor(c)) }.getOrDefault(Color.Gray)
                val isSelected = currentColor.toArgb() == parsed.toArgb()
                Box(
                    modifier = Modifier
                        .size(26.dp)
                        .clip(CircleShape)
                        .background(parsed)
                        .then(if (isSelected) Modifier.border(2.dp, MaterialTheme.colorScheme.onSurface, CircleShape) else Modifier)
                        .clickable {
                            val hsv = parsed.toHsv()
                            hue = hsv[0]; sat = hsv[1]; value = hsv[2]
                        }
                )
            }
        }

        // SV picker (2D gradient canvas)
        androidx.compose.foundation.Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp)
                .clip(RoundedCornerShape(8.dp))
                .onSizeChanged { svSize = it }
                .pointerInput(Unit) {
                    detectTapGestures { offset ->
                        if (svSize.width > 0 && svSize.height > 0) {
                            sat = (offset.x / svSize.width).coerceIn(0f, 1f)
                            value = (1f - offset.y / svSize.height).coerceIn(0f, 1f)
                        }
                    }
                }
                .pointerInput(Unit) {
                    detectDragGestures { change, _ ->
                        if (svSize.width > 0 && svSize.height > 0) {
                            sat = (change.position.x / svSize.width).coerceIn(0f, 1f)
                            value = (1f - change.position.y / svSize.height).coerceIn(0f, 1f)
                        }
                    }
                }
        ) {
            // White → hue (horizontal saturation)
            drawRect(brush = Brush.horizontalGradient(listOf(Color.White, hueColor)))
            // Transparent → black (vertical value)
            drawRect(brush = Brush.verticalGradient(listOf(Color.Transparent, Color.Black)))
            // Selector circle
            val cx = sat * size.width
            val cy = (1f - value) * size.height
            drawCircle(Color.White, radius = 10.dp.toPx(), center = Offset(cx, cy))
            drawCircle(Color.Black, radius = 8.dp.toPx(), center = Offset(cx, cy), style = Stroke(2.dp.toPx()))
        }

        // Hue slider
        androidx.compose.foundation.Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(24.dp)
                .clip(RoundedCornerShape(12.dp))
                .onSizeChanged { hueSize = it }
                .pointerInput(Unit) {
                    detectTapGestures { offset ->
                        if (hueSize.width > 0)
                            hue = (offset.x / hueSize.width * 360f).coerceIn(0f, 360f)
                    }
                }
                .pointerInput(Unit) {
                    detectDragGestures { change, _ ->
                        if (hueSize.width > 0)
                            hue = (change.position.x / hueSize.width * 360f).coerceIn(0f, 360f)
                    }
                }
        ) {
            // Hue rainbow gradient
            val hueColors = (0..12).map { hsvColor(it * 30f, 1f, 1f) }
            drawRect(brush = Brush.horizontalGradient(hueColors))
            // Hue selector circle
            val cx = hue / 360f * size.width
            val cy = size.height / 2f
            drawCircle(Color.White, radius = cy, center = Offset(cx, cy))
            drawCircle(Color.Black, radius = cy - 2, center = Offset(cx, cy), style = Stroke(2f))
        }

        // Color preview bar
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(32.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(currentColor)
            )
            Text(
                text = "#%06X".format(currentColor.toArgb() and 0xFFFFFF),
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }
    }
}

// ── Category form dialog ──────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CategoryFormDialog(
    category: Category?,
    onDismiss: () -> Unit,
    onSave: (name: String, color: String, icon: String, type: String) -> Unit,
    isLoading: Boolean
) {
    val isEdit = category != null
    var name by remember { mutableStateOf(category?.name ?: "") }
    var selectedColor by remember {
        val c = runCatching { Color(android.graphics.Color.parseColor(category?.color ?: "#6C5CE7")) }.getOrDefault(Color(0xFF6C5CE7))
        mutableStateOf(c)
    }
    var selectedIcon by remember { mutableStateOf(category?.icon ?: "attach_money") }
    var selectedType by remember { mutableStateOf(category?.type ?: "expense") }
    var showPicker by remember { mutableStateOf(false) }

    val colorHex = "#%06X".format(selectedColor.toArgb() and 0xFFFFFF)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isEdit) "Sửa danh mục" else "Thêm danh mục") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Name
                OutlinedTextField(
                    value = name, onValueChange = { name = it },
                    label = { Text("Tên danh mục") }, singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Type (new only)
                if (!isEdit) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(selected = selectedType == "expense", onClick = { selectedType = "expense" }, label = { Text("Chi tiêu") })
                        FilterChip(selected = selectedType == "income", onClick = { selectedType = "income" }, label = { Text("Thu nhập") })
                    }
                }

                // Color picker header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Màu sắc", style = MaterialTheme.typography.labelMedium)
                    // Clickable color preview chip
                    Box(
                        modifier = Modifier
                            .size(width = 72.dp, height = 32.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(selectedColor)
                            .border(
                                1.dp,
                                MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                                RoundedCornerShape(16.dp)
                            )
                            .clickable { showPicker = !showPicker },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            colorHex,
                            fontSize = 10.sp,
                            color = if (selectedColor.luminance() > 0.4f) Color.Black else Color.White
                        )
                    }
                }

                // Inline HSV color picker (toggled)
                if (showPicker) {
                    HsvColorPicker(
                        color = selectedColor,
                        onColorChange = { selectedColor = it }
                    )
                }

                // Icon picker
                Text("Biểu tượng", style = MaterialTheme.typography.labelMedium)
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    categoryIcons.forEach { (key, vector) ->
                        val isSelected = selectedIcon == key
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    if (isSelected) selectedColor.copy(alpha = 0.25f)
                                    else MaterialTheme.colorScheme.surfaceVariant
                                )
                                .then(
                                    if (isSelected) Modifier.border(2.dp, selectedColor, RoundedCornerShape(8.dp))
                                    else Modifier
                                )
                                .clickable { selectedIcon = key },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = vector,
                                contentDescription = key,
                                tint = if (isSelected) selectedColor else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(name, colorHex, selectedIcon, selectedType) },
                enabled = !isLoading && name.isNotBlank()
            ) {
                if (isLoading) CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                else Text(if (isEdit) "Cập nhật" else "Lưu")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Huỷ") } }
    )
}
