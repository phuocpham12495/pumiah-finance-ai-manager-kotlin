# 06 - Tài Liệu UI/UX Screens

> **Vai trò:** Mentor Lập Trình Viên Cao Cấp
> **Timestamp:** 2026-04-01
> **Phiên bản tài liệu:** 1.1.0

---

## 1. Tổng Quan UI Architecture

Ứng dụng sử dụng **Jetpack Compose** với **Material Design 3** hoàn toàn. Không có Fragment hay XML layout. Toàn bộ UI được xây dựng bằng Composable functions.

### 1.1 Theme Configuration

```kotlin
// ui/theme/Theme.kt
@Composable
fun PumiahTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,  // MD3 Dynamic Color
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (darkTheme) dynamicDarkColorScheme(LocalContext.current)
            else dynamicLightColorScheme(LocalContext.current)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
```

---

## 2. AppNavigation

### 2.1 Route Map

```kotlin
// ui/navigation/AppNavigation.kt — sealed class Screen
sealed class Screen(val route: String) {
    object Login : Screen("login")
    object Register : Screen("register")
    object Dashboard : Screen("dashboard")
    object Transactions : Screen("transactions")
    object Categories : Screen("categories")
    object Budgets : Screen("budgets")
    object Goals : Screen("goals")
    object Chat : Screen("chat")
    object Profile : Screen("profile")
    object Wallets : Screen("wallets")   // v1.1 — ví cá nhân + ví chung
}
```

### 2.2 Navigation Graph với Auth Guard (v1.1)

Điểm khác biệt so với v1.0:
- `sessionReady` StateFlow thay cho `LaunchedEffect(authState)` — tránh flash Login khi cold start
- `startDestination` được xác định động (Dashboard nếu đã đăng nhập, Login nếu chưa)
- `popUpTo(Screen.Dashboard.route)` thay vì `findStartDestination()` — tránh bug Login node

```kotlin
@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val authViewModel: AuthViewModel = hiltViewModel()
    val sessionReady by authViewModel.sessionReady.collectAsState()

    // Splash indicator cho đến khi Supabase session khởi tạo xong
    if (!sessionReady) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    val startDestination = if (authViewModel.isLoggedIn) Screen.Dashboard.route else Screen.Login.route

    Scaffold(
        bottomBar = { /* NavigationBar chỉ hiện trên 8 app screens */ }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Login.route) { LoginScreen(...) }
            composable(Screen.Register.route) { RegisterScreen(...) }
            composable(Screen.Dashboard.route) { DashboardScreen(navController) }
            composable(Screen.Transactions.route) { TransactionListScreen() }
            composable(Screen.Categories.route) { CategoryScreen() }
            composable(Screen.Budgets.route) { BudgetScreen() }
            composable(Screen.Goals.route) { GoalScreen() }
            composable(Screen.Chat.route) { ChatScreen() }
            composable(Screen.Wallets.route) { WalletScreen() }
            composable(Screen.Profile.route) {
                ProfileScreen(
                    onSignOut = { navController.navigate(Screen.Login.route) { popUpTo(0) { inclusive = true } } },
                    onNavigateToCategories = { navController.navigate(Screen.Categories.route) },
                    onNavigateToBudgets = { navController.navigate(Screen.Budgets.route) },
                    onNavigateToGoals = { navController.navigate(Screen.Goals.route) }
                )
            }
        }
    }
}
```

### 2.3 Bottom Navigation Bar (v1.1)

5 tabs: **Tổng quan · Giao dịch · AI Chat · Ví · Hồ sơ**

```kotlin
val bottomNavItems = listOf(
    BottomNavItem(Screen.Dashboard, "Tổng quan") { Icon(Icons.Default.Home, null) },
    BottomNavItem(Screen.Transactions, "Giao dịch") { Icon(Icons.Default.Receipt, null) },
    BottomNavItem(Screen.Chat, "AI Chat") { Icon(Icons.Default.Chat, null) },
    BottomNavItem(Screen.Wallets, "Ví") { Icon(Icons.Default.AccountBalanceWallet, null) },
    BottomNavItem(Screen.Profile, "Hồ sơ") { Icon(Icons.Default.Person, null) },
)

// onClick — fix popUpTo bug (ADR-008)
navController.navigate(item.screen.route) {
    popUpTo(Screen.Dashboard.route) { saveState = true }
    launchSingleTop = true
    restoreState = item.screen != Screen.Profile  // Profile clear sub-screens
}
```

---

## 3. Auth Screens

### 3.1 LoginScreen

**Mục đích:** Cho phép người dùng đăng nhập bằng email/password.

**State Flow:**
```
AuthUiState(isLoading, error) ←── AuthViewModel ←── AuthRepository
         │
         ▼  collectAsState()
LoginScreen composable
```

```kotlin
// ui/auth/LoginScreen.kt
@Composable
fun LoginScreen(
    navController: NavHostController,
    viewModel: AuthViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    // Local state cho input fields
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Logo / App name
        Text(
            text = "Pumiah Finance",
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Email field
        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Password field với toggle visibility
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Mật khẩu") },
            visualTransformation = if (passwordVisible)
                VisualTransformation.None
            else
                PasswordVisualTransformation(),
            trailingIcon = {
                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Icon(
                        if (passwordVisible) Icons.Filled.VisibilityOff
                        else Icons.Filled.Visibility,
                        contentDescription = "Toggle password"
                    )
                }
            },
            modifier = Modifier.fillMaxWidth()
        )

        // Error message
        uiState.error?.let { error ->
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = error,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Login button
        Button(
            onClick = { viewModel.signIn(email, password) },
            enabled = !uiState.isLoading && email.isNotBlank() && password.isNotBlank(),
            modifier = Modifier.fillMaxWidth()
        ) {
            if (uiState.isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = MaterialTheme.colorScheme.onPrimary
                )
            } else {
                Text("Đăng Nhập")
            }
        }

        // Link to Register
        TextButton(onClick = { navController.navigate(Screen.Register.route) }) {
            Text("Chưa có tài khoản? Đăng ký ngay")
        }
    }
}
```

### 3.2 RegisterScreen

**Mục đích:** Đăng ký tài khoản mới với email, mật khẩu và họ tên.

**Key composables:** `OutlinedTextField` × 4, `Button`, `TextButton`

---

## 4. DashboardScreen

**Mục đích:** Tổng quan tài chính: thu/chi/số dư tháng hiện tại + giao dịch gần đây.

```kotlin
// ui/dashboard/DashboardScreen.kt
@Composable
fun DashboardScreen(viewModel: DashboardViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsState()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header với tháng hiện tại
        item {
            Text(
                text = "Tháng ${currentMonth}/${currentYear}",
                style = MaterialTheme.typography.titleLarge
            )
        }

        // Summary Cards - 3 card: Thu, Chi, Số dư
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SummaryCard(
                    title = "Thu nhập",
                    amount = uiState.totalIncome,
                    color = Color(0xFF4CAF50),
                    modifier = Modifier.weight(1f)
                )
                SummaryCard(
                    title = "Chi tiêu",
                    amount = uiState.totalExpense,
                    color = Color(0xFFF44336),
                    modifier = Modifier.weight(1f)
                )
                SummaryCard(
                    title = "Số dư",
                    amount = uiState.balance,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Giao dịch gần đây
        item {
            Text("Giao dịch gần đây", style = MaterialTheme.typography.titleMedium)
        }

        items(uiState.recentTransactions, key = { it.id }) { transaction ->
            TransactionRow(transaction = transaction)
        }
    }
}

// SummaryCard composable
@Composable
fun SummaryCard(
    title: String,
    amount: Double,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = color.copy(alpha = 0.1f)
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                color = color
            )
            Text(
                text = formatCurrency(amount),
                style = MaterialTheme.typography.titleSmall,
                color = color,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

// TransactionRow composable
@Composable
fun TransactionRow(transaction: Transaction) {
    ListItem(
        headlineContent = {
            Text(transaction.description ?: "Không có mô tả")
        },
        supportingContent = {
            Text(transaction.date)
        },
        trailingContent = {
            Text(
                text = "${if (transaction.type == "income") "+" else "-"}${formatCurrency(transaction.amount)}",
                color = if (transaction.type == "income")
                    Color(0xFF4CAF50) else Color(0xFFF44336),
                fontWeight = FontWeight.Bold
            )
        },
        leadingContent = {
            Icon(
                imageVector = getCategoryIcon(transaction.categoryId),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        }
    )
}
```

---

## 5. TransactionListScreen + TransactionFormDialog

**Mục đích:** Xem, thêm, sửa, xóa giao dịch. FAB để thêm mới.

```kotlin
@Composable
fun TransactionListScreen(viewModel: TransactionViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    var showForm by remember { mutableStateOf(false) }
    var selectedTransaction by remember { mutableStateOf<Transaction?>(null) }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = {
                selectedTransaction = null
                showForm = true
            }) {
                Icon(Icons.Filled.Add, "Thêm giao dịch")
            }
        }
    ) { padding ->
        LazyColumn(contentPadding = padding) {
            items(uiState.transactions, key = { it.id }) { transaction ->
                TransactionRow(
                    transaction = transaction,
                    onEdit = {
                        selectedTransaction = transaction
                        showForm = true
                    },
                    onDelete = { viewModel.deleteTransaction(transaction.id) }
                )
                HorizontalDivider()
            }
        }
    }

    // Dialog thêm/sửa
    if (showForm) {
        TransactionFormDialog(
            transaction = selectedTransaction,
            categories = uiState.categories,
            onDismiss = { showForm = false },
            onSave = { transaction ->
                if (selectedTransaction == null)
                    viewModel.createTransaction(transaction)
                else
                    viewModel.updateTransaction(transaction)
                showForm = false
            }
        )
    }
}

@Composable
fun TransactionFormDialog(
    transaction: Transaction?,
    categories: List<Category>,
    onDismiss: () -> Unit,
    onSave: (Transaction) -> Unit
) {
    // Form state
    var amount by remember { mutableStateOf(transaction?.amount?.toString() ?: "") }
    var description by remember { mutableStateOf(transaction?.description ?: "") }
    var selectedType by remember { mutableStateOf(transaction?.type ?: "expense") }
    var selectedCategory by remember { mutableStateOf(transaction?.categoryId) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(if (transaction == null) "Thêm Giao Dịch" else "Sửa Giao Dịch")
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                // Toggle thu/chi
                Row {
                    FilterChip(
                        selected = selectedType == "income",
                        onClick = { selectedType = "income" },
                        label = { Text("Thu nhập") }
                    )
                    Spacer(Modifier.width(8.dp))
                    FilterChip(
                        selected = selectedType == "expense",
                        onClick = { selectedType = "expense" },
                        label = { Text("Chi tiêu") }
                    )
                }
                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    label = { Text("Số tiền") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Mô tả") }
                )
                // Category Dropdown...
            }
        },
        confirmButton = {
            Button(onClick = {
                onSave(Transaction(
                    amount = amount.toDoubleOrNull() ?: 0.0,
                    type = selectedType,
                    description = description,
                    categoryId = selectedCategory,
                    date = LocalDate.now().toString()
                ))
            }) { Text("Lưu") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Hủy") }
        }
    )
}
```

---

## 6. BudgetScreen + BudgetItem

**Mục đích:** Hiển thị và quản lý ngân sách với progress indicator.

```kotlin
@Composable
fun BudgetItem(
    budget: Budget,
    spentAmount: Double,
    categoryName: String,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val progress = (spentAmount / budget.amount).coerceIn(0.0, 1.0).toFloat()
    val isOverBudget = spentAmount > budget.amount

    Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = categoryName,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = "${formatCurrency(spentAmount)} / ${formatCurrency(budget.amount)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isOverBudget)
                        MaterialTheme.colorScheme.error
                    else
                        MaterialTheme.colorScheme.onSurface
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // LinearProgressIndicator cho tiến độ ngân sách
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                color = if (isOverBudget)
                    MaterialTheme.colorScheme.error
                else if (progress > 0.8f)
                    Color(0xFFFF9800) // Warning: cam
                else
                    MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = if (isOverBudget)
                    "Vượt ngân sách ${formatCurrency(spentAmount - budget.amount)}"
                else
                    "Còn lại: ${formatCurrency(budget.amount - spentAmount)}",
                style = MaterialTheme.typography.bodySmall,
                color = if (isOverBudget)
                    MaterialTheme.colorScheme.error
                else
                    MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
```

---

## 7. GoalScreen + ContributionDialog

**Mục đích:** Theo dõi mục tiêu tiết kiệm, nạp tiền vào mục tiêu.

```kotlin
@Composable
fun GoalCard(goal: Goal, onContribute: () -> Unit) {
    val progress = (goal.currentAmount / goal.targetAmount).coerceIn(0.0, 1.0).toFloat()

    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.Flag,
                    contentDescription = null,
                    tint = Color(goal.color.toColorInt())
                )
                Spacer(Modifier.width(8.dp))
                Text(goal.name, style = MaterialTheme.typography.titleMedium)
            }
            Spacer(Modifier.height(12.dp))
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("${(progress * 100).toInt()}%")
                Text("${formatCurrency(goal.currentAmount)} / ${formatCurrency(goal.targetAmount)}")
            }
            goal.deadline?.let { deadline ->
                Text(
                    "Hạn: $deadline",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Button(
                onClick = onContribute,
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
            ) {
                Text("Nạp Tiền")
            }
        }
    }
}
```

---

## 8. ChatScreen + ChatBubble (v1.1 — Voice Input)

**Mục đích:** Chat với AI Gemini để nhận tư vấn tài chính. Hỗ trợ nhập văn bản và giọng nói tiếng Việt.

**Quyền cần thiết:** `android.permission.RECORD_AUDIO` (AndroidManifest.xml)

```kotlin
@Composable
fun ChatScreen(viewModel: ChatViewModel = hiltViewModel()) {
    val messages by viewModel.messages.collectAsState()
    val sendState by viewModel.sendState.collectAsState()
    var inputText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    // Voice input launcher
    val speechLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val text = result.data
                ?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                ?.firstOrNull()
            if (!text.isNullOrBlank()) inputText = text
        }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("PFAM AI") }, actions = { /* Delete history */ }) },
        bottomBar = {
            Row(modifier = Modifier.padding(12.dp)) {
                // Nút mic — khởi động Google Speech Recognition
                IconButton(onClick = {
                    val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                        putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                        putExtra(RecognizerIntent.EXTRA_LANGUAGE, "vi-VN")
                        putExtra(RecognizerIntent.EXTRA_PROMPT, "Nói câu hỏi của bạn...")
                    }
                    speechLauncher.launch(intent)
                }) { Icon(Icons.Default.Mic, "Nhập giọng nói") }

                OutlinedTextField(value = inputText, onValueChange = { inputText = it }, ...)

                // Nút gửi — disabled khi đang loading
                IconButton(
                    onClick = { viewModel.sendMessage(inputText); inputText = "" },
                    enabled = inputText.isNotBlank() && sendState !is UiState.Loading
                ) {
                    if (sendState is UiState.Loading) CircularProgressIndicator(...)
                    else Icon(Icons.Default.Send, null)
                }
            }
        }
    ) { padding ->
        LazyColumn(state = listState, ...) {
            items(messages, key = { it.id }) { msg -> ChatBubble(msg) }
        }
    }
}

@Composable
fun ChatBubble(message: Message) {
    val isUser = message.isFromUser

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        if (!isUser) {
            // AI avatar
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.secondaryContainer,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    Icons.Filled.SmartToy,
                    contentDescription = "AI",
                    modifier = Modifier.padding(8.dp),
                    tint = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
            Spacer(Modifier.width(8.dp))
        }

        Surface(
            shape = RoundedCornerShape(
                topStart = if (isUser) 16.dp else 4.dp,
                topEnd = if (isUser) 4.dp else 16.dp,
                bottomStart = 16.dp,
                bottomEnd = 16.dp
            ),
            color = if (isUser)
                MaterialTheme.colorScheme.primary
            else
                MaterialTheme.colorScheme.secondaryContainer,
            modifier = Modifier.widthIn(max = 280.dp)
        ) {
            if (message.isLoading == true) {
                CircularProgressIndicator(
                    modifier = Modifier.padding(16.dp).size(20.dp)
                )
            } else {
                Text(
                    text = message.content,
                    modifier = Modifier.padding(12.dp),
                    color = if (isUser)
                        MaterialTheme.colorScheme.onPrimary
                    else
                        MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
        }
    }
}
```

---

## 9. ProfileScreen

**Mục đích:** Hiển thị và chỉnh sửa thông tin cá nhân, đăng xuất.

```kotlin
@Composable
fun ProfileScreen(
    navController: NavHostController,
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Avatar
        AsyncImage(
            model = uiState.profile?.avatarUrl ?: R.drawable.default_avatar,
            contentDescription = "Avatar",
            modifier = Modifier.size(96.dp).clip(CircleShape)
        )

        Spacer(Modifier.height(16.dp))

        Text(
            text = uiState.profile?.fullName ?: "Người dùng",
            style = MaterialTheme.typography.headlineSmall
        )
        Text(
            text = uiState.email ?: "",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(Modifier.height(32.dp))

        // Settings items
        ListItem(
            headlineContent = { Text("Đơn vị tiền tệ") },
            trailingContent = { Text(uiState.profile?.currency ?: "VND") },
            leadingContent = { Icon(Icons.Filled.AttachMoney, null) }
        )

        HorizontalDivider()

        // Logout button
        Spacer(Modifier.height(32.dp))
        OutlinedButton(
            onClick = {
                viewModel.signOut()
                // Navigation được handle bởi AuthState observer trong AppNavigation
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = MaterialTheme.colorScheme.error
            ),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.error)
        ) {
            Icon(Icons.Filled.Logout, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Đăng Xuất")
        }
    }
}
```

---

## 10. Checklist UI/UX

| Tiêu chí | Trạng thái |
|---|---|
| Loading states cho tất cả async operations | Đã implement |
| Error messages thân thiện người dùng | Đã implement |
| Empty states (khi không có dữ liệu) | Đã implement |
| Pull-to-refresh | Cần thêm |
| Offline support | Chưa implement |
| Accessibility (content descriptions) | Một phần |
| Dark mode | Đã implement (MD3 dynamic color) |
| Landscape orientation | Cơ bản |

---

*Tài liệu này được tạo bởi Mentor Lập Trình Viên Cao Cấp - 2026-03-29*
