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
import androidx.compose.ui.graphics.luminance
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

    errorMessage?.let { msg ->
        AlertDialog(
            onDismissRequest = { errorMessage = null },
            title = { Text("Lỗi") },
            text = { Text(msg) },
            confirmButton = { TextButton(onClick = { errorMessage = null }) { Text("OK") } }
        )
    }

    pendingDeleteId?.let { id ->
        AlertDialog(
            onDismissRequest = { pendingDeleteId = null },
            title = { Text("Xoá danh mục") },
            text = { Text("Bạn có chắc chắn muốn xoá danh mục '$pendingDeleteName'? Thao tác này không thể hoàn tác.") },
            confirmButton = {
                TextButton(onClick = { viewModel.deleteCategory(id); pendingDeleteId = null }) {
                    Text("Xoá", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDeleteId = null }) { Text("Huỷ") }
            }
        )
    }
}

@Composable
fun CategoryItem(category: Category, onEdit: () -> Unit, onDelete: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onEdit() },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(44.dp).clip(CircleShape).background(Color(android.graphics.Color.parseColor(category.color))),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = iconVectorFor(category.icon),
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(category.name, fontWeight = FontWeight.Bold)
                Text(if (category.type == "expense") "Chi tiêu" else "Thu nhập",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f))
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CategoryFormDialog(
    category: Category?,
    onDismiss: () -> Unit,
    onSave: (String, String, String, String) -> Unit,
    isLoading: Boolean
) {
    val isEdit = category != null
    var name by remember { mutableStateOf(category?.name ?: "") }
    var selectedColor by remember { mutableStateOf(category?.let { Color(android.graphics.Color.parseColor(it.color)) } ?: Color.Blue) }
    var selectedIcon by remember { mutableStateOf(category?.icon ?: "restaurant") }
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

@Composable
fun HsvColorPicker(
    color: Color,
    onColorChange: (Color) -> Unit
) {
    val hsv = remember(color) { color.toHsv() }
    var hue by remember { mutableStateOf(hsv[0]) }
    var saturation by remember { mutableStateOf(hsv[1]) }
    var value by remember { mutableStateOf(hsv[2]) }

    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Hue bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(16.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            Color.Red, Color.Yellow, Color.Green, Color.Cyan, Color.Blue, Color.Magenta, Color.Red
                        )
                    )
                )
                .pointerInput(Unit) {
                    detectTapGestures { offset ->
                        hue = (offset.x / size.width) * 360f
                        onColorChange(hsvColor(hue, saturation, value))
                    }
                }
                .pointerInput(Unit) {
                    detectDragGestures { change, _ ->
                        hue = (change.position.x / size.width).coerceIn(0f, 1f) * 360f
                        onColorChange(hsvColor(hue, saturation, value))
                    }
                }
        )

        // Saturation & Value area
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(Color.White, Color.Transparent)
                    )
                )
                .background(
                    brush = Brush.horizontalGradient(
                        colors = listOf(Color.Transparent, hsvColor(hue, 1f, 1f))
                    )
                )
                .background(Color.Black.copy(alpha = 1f - value))
                .pointerInput(Unit) {
                    detectTapGestures { offset ->
                        saturation = (offset.x / size.width).coerceIn(0f, 1f)
                        value = 1f - (offset.y / size.height).coerceIn(0f, 1f)
                        onColorChange(hsvColor(hue, saturation, value))
                    }
                }
                .pointerInput(Unit) {
                    detectDragGestures { change, _ ->
                        saturation = (change.position.x / size.width).coerceIn(0f, 1f)
                        value = 1f - (change.position.y / size.height).coerceIn(0f, 1f)
                        onColorChange(hsvColor(hue, saturation, value))
                    }
                }
        )
    }
}
