# 09 - Tiến Độ Học Tập và Phát Triển Kỹ Năng

> **Vai trò:** Điều Phối Viên Giáo Dục
> **Timestamp:** 2026-04-05
> **Phiên bản tài liệu:** 1.2.0

---

## 1. Tổng Quan Hành Trình Học Tập

Dự án Pumiah Finance AI Manager là một **dự án học tập thực hành** toàn diện, bao gồm nhiều công nghệ Android hiện đại. Tài liệu này theo dõi tiến độ học tập, kỹ năng đã thực hành, và xác định các khoảng trống kiến thức cần bổ sung.

---

## 2. Khái Niệm Được Minh Họa Trong Dự Án

### 2.1 MVVM Architecture Pattern

**Minh họa qua:** Tất cả 8 màn hình (AuthViewModel, DashboardViewModel, TransactionViewModel, v.v.)

```kotlin
// Ví dụ minh họa MVVM rõ nhất: TransactionViewModel
// MODEL: data class Transaction, Repository
// VIEWMODEL: TransactionViewModel (business logic, state management)
// VIEW: TransactionListScreen (chỉ render, không có logic)

class TransactionViewModel @Inject constructor(
    private val transactionRepo: TransactionRepository,
    private val categoryRepo: CategoryRepository
) : ViewModel() {

    // ViewModel giữ state (không phải View)
    private val _uiState = MutableStateFlow(TransactionUiState())
    val uiState: StateFlow<TransactionUiState> = _uiState.asStateFlow()

    // Business logic nằm trong ViewModel, không trong Composable
    fun deleteTransaction(id: String) {
        viewModelScope.launch {
            try {
                transactionRepo.deleteTransaction(id)
                loadTransactions() // Reload list
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }
}
```

**Bài học chính:**
- ViewModel không nên giữ tham chiếu đến `Context`, `View`, hoặc `Composable`
- `viewModelScope` tự động cancel coroutines khi ViewModel bị destroy
- Dữ liệu chảy một chiều: Repository → ViewModel → UI (Unidirectional Data Flow)

### 2.2 Dependency Injection với Hilt

**Minh họa qua:** `AppModule.kt`, `@HiltViewModel` annotations

```kotlin
// TRƯỚC khi có DI (manual injection - khó maintain)
class LoginScreen {
    val supabaseClient = SupabaseClient(url, key) // Tạo mới mỗi lần!
    val authRepo = AuthRepositoryImpl(supabaseClient)
    val viewModel = AuthViewModel(authRepo)
}

// SAU khi có Hilt DI (clean, testable)
@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository  // Hilt tự inject
) : ViewModel()

// Trong Composable:
val viewModel: AuthViewModel = hiltViewModel() // Hilt quản lý lifecycle
```

**Bài học chính:**
- `@Singleton` đảm bảo SupabaseClient chỉ tạo một lần
- DI cho phép swap implementation dễ dàng trong tests (dùng mock)
- `@InstallIn(SingletonComponent::class)` vs `@InstallIn(ViewModelComponent::class)` - hiểu scope

### 2.3 Compose State Management

**Minh họa qua:** Tất cả Composable screens

```kotlin
// Phân loại state trong Compose:

// 1. LOCAL STATE (chỉ dùng trong một Composable)
var email by remember { mutableStateOf("") }
var passwordVisible by remember { mutableStateOf(false) }

// 2. HOISTED STATE (được nâng lên ViewModel)
val uiState by viewModel.uiState.collectAsState()
// uiState.transactions, uiState.isLoading, uiState.error

// 3. DERIVED STATE (tính từ state khác)
val balance by remember(uiState.totalIncome, uiState.totalExpense) {
    derivedStateOf { uiState.totalIncome - uiState.totalExpense }
}

// 4. SIDE EFFECTS
LaunchedEffect(uiState.navigateToHome) {
    if (uiState.navigateToHome) {
        navController.navigate(Screen.Dashboard.route)
    }
}
```

**Bài học chính:**
- State hoisting: nâng state lên cấp cao nhất cần thiết
- `remember` giữ state qua recomposition
- `LaunchedEffect` cho side effects (navigation, API calls khi screen load)

### 2.4 Supabase Integration

**Minh họa qua:** 5 Repository classes

```kotlin
// Supabase Kotlin SDK - type-safe database access
class TransactionRepositoryImpl @Inject constructor(
    private val supabase: SupabaseClient
) : TransactionRepository {

    // Postgrest query với filter và sorting
    override suspend fun getTransactions(): List<Transaction> {
        return supabase.postgrest["transactions"]
            .select {
                order("date", ascending = false)
                // RLS tự động filter theo user_id!
            }
            .decodeList<Transaction>()
    }

    // Insert với return representation
    override suspend fun createTransaction(t: Transaction): Transaction {
        return supabase.postgrest["transactions"]
            .insert(t)
            .decodeSingle<Transaction>()
    }
}
```

**Bài học chính:**
- RLS (Row Level Security) là "invisible filter" - không cần WHERE user_id = ? trong query
- `@Serializable` + `@SerialName` để map giữa Kotlin field và database column
- `suspend` functions để không block main thread

### 2.5 Gemini AI Integration

**Minh họa qua:** `GeminiService.kt`, `ChatViewModel.kt`

```kotlin
// Context-aware AI: cung cấp dữ liệu tài chính thực tế cho AI
private fun buildPrompt(userMessage: String, context: String): String {
    return """
        Bạn là trợ lý tài chính thông minh.

        Dữ liệu tài chính hiện tại của người dùng:
        $context

        Câu hỏi: $userMessage

        Trả lời ngắn gọn, thực tế, dựa trên số liệu thực tế trên.
    """.trimIndent()
}
```

**Bài học chính:**
- "Prompt Engineering" - cách đặt câu hỏi ảnh hưởng lớn đến chất lượng AI response
- Context window: cần cân bằng giữa context đầy đủ vs token limit
- Xử lý async: AI response có thể mất 1-3 giây, cần loading state tốt

---

## 3. Kỹ Năng Đã Thực Hành

### 3.1 Kotlin Coroutines & Flow

| Kỹ năng | Được áp dụng ở | Mức độ thành thạo |
|---|---|---|
| `suspend` functions | Tất cả Repository | Thành thạo |
| `viewModelScope.launch` | Tất cả ViewModel | Thành thạo |
| `withContext(Dispatchers.IO)` | GeminiService, file ops | Khá |
| `StateFlow` + `MutableStateFlow` | Tất cả ViewModel | Thành thạo |
| `collectAsState()` | Tất cả Screen | Thành thạo |
| `Flow` operators (`map`, `filter`) | Repository | Cơ bản |
| `combine()` | Dashboard (tổng hợp data) | Cơ bản |
| Error handling với `try-catch` | Toàn bộ app | Khá |

### 3.2 Navigation Compose

| Kỹ năng | Được áp dụng ở | Mức độ thành thạo |
|---|---|---|
| `NavHostController` | AppNavigation | Thành thạo |
| `composable()` destinations | AppNavigation | Thành thạo |
| `navigate()` với options | Auth flow | Khá |
| `popUpTo()` với inclusive | Login → Dashboard | Khá |
| `launchSingleTop` | Bottom nav | Thành thạo |
| `restoreState` | Bottom nav | Thành thạo |
| Argument passing | Chưa dùng | Cần học |

### 3.3 REST API & HTTP

| Kỹ năng | Được áp dụng ở | Mức độ thành thạo |
|---|---|---|
| `HttpURLConnection` | GeminiService | Khá |
| JSON parsing thủ công | GeminiService | Khá |
| HTTP headers | GeminiService | Cơ bản |
| Error code handling | Toàn bộ app | Khá |
| Request/Response format | Gemini API | Khá |

### 3.4 Material Design 3 Composables

| Composable | Được dùng ở | Mức độ thành thạo |
|---|---|---|
| `Scaffold` | Tất cả screens | Thành thạo |
| `NavigationBar` / `NavigationBarItem` | AppNavigation | Thành thạo |
| `Card`, `ElevatedCard` | Budget, Goal, Dashboard | Thành thạo |
| `OutlinedTextField` | Forms | Thành thạo |
| `AlertDialog` | Form dialogs | Thành thạo |
| `LinearProgressIndicator` | Budget, Goal | Thành thạo |
| `LazyColumn` với keys | Tất cả lists | Thành thạo |
| `FloatingActionButton` | Transaction, Category | Thành thạo |
| `ListItem` | Transaction row | Khá |
| `FilterChip` | Type selector | Cơ bản |
| `PullToRefreshBox` | 6 màn hình chính | **Thành thạo** |
| `FlowRow` (ExperimentalLayout) | Category icon grid | **Khá** |

### 3.5 Compose Canvas & Advanced UI (v1.1)

| Kỹ năng | Được áp dụng ở | Mức độ thành thạo |
|---|---|---|
| `Canvas` + gradient brush | HSV SV box + Hue slider | Khá |
| `detectTapGestures` + `detectDragGestures` | Color picker tương tác | Khá |
| `android.graphics.Color.colorToHSV()` | Chuyển đổi Color ↔ HSV | Khá |
| Badge `CircleShape` + `Box` | Email initial badge ví chung | Thành thạo |

### 3.6 Android Platform APIs (v1.1)

| Kỹ năng | Được áp dụng ở | Mức độ thành thạo |
|---|---|---|
| `RecognizerIntent.ACTION_RECOGNIZE_SPEECH` | Voice input ChatScreen | Khá |
| `rememberLauncherForActivityResult` | Voice result callback | Khá |
| `SessionStatus` flow (`awaitSessionReady`) | Session persistence | Thành thạo |

---

## 4. Khoảng Trống Kiến Thức (Knowledge Gaps)

### 4.1 Ưu Tiên Cao - Cần Học Sớm

| Chủ đề | Lý do cần học | Tài liệu tham khảo |
|---|---|---|
| **Paging 3 Library** | Khi giao dịch > 100 items, LazyColumn load hết sẽ chậm | [developer.android.com/topic/libraries/architecture/paging](https://developer.android.com/topic/libraries/architecture/paging) |
| **Unit Testing với Turbine** | Hiện tại chưa có test coverage, cần thiết cho production | [github.com/cashapp/turbine](https://github.com/cashapp/turbine) |
| **Compose Performance Tracing** | Chưa biết cách detect recomposition không cần thiết | [developer.android.com/jetpack/compose/performance](https://developer.android.com/jetpack/compose/performance) |
| **Offline-first với DataStore** | App hiện tại không hoạt động offline | [developer.android.com/topic/libraries/architecture/datastore](https://developer.android.com/topic/libraries/architecture/datastore) |
| **Android Speech Recognition** | Đã implement cơ bản, cần xử lý edge case (từ chối quyền, thiết bị không hỗ trợ) | [developer.android.com/reference/android/speech/RecognizerIntent](https://developer.android.com/reference/android/speech/RecognizerIntent) |

### 4.2 Ưu Tiên Trung Bình

| Chủ đề | Lý do cần học |
|---|---|
| **Compose Animation API** | UI hiện tại thiếu transitions và animations |
| **WorkManager** | Nhắc nhở tài chính định kỳ, sync background |
| **Room Database** | Local caching cho offline support |
| **Supabase Realtime** | Đồng bộ dữ liệu real-time giữa devices |
| **Compose Canvas (Charts)** | Đã dùng Canvas cho HSV picker; cần học thêm drawArc/drawPath cho biểu đồ |

### 4.3 Ưu Tiên Thấp (Tương Lai)

| Chủ đề | Ứng dụng tiềm năng |
|---|---|
| **Jetpack Glance** | Home screen widget hiển thị số dư |
| **CameraX** | Scan receipt/hóa đơn |
| **ML Kit** | OCR từ hóa đơn |
| **Wear OS** | Companion app cho smartwatch |

---

## 5. Bài Tập Thực Hành Đề Xuất

### 5.1 Bài Tập Ngắn (1-2 giờ)

**Bài 1: Thêm Search/Filter cho Transactions** *(Pull-to-refresh đã hoàn thành)*
```kotlin
// Thêm search bar với debounce vào TransactionListScreen
var searchQuery by remember { mutableStateOf("") }
val filteredTransactions by remember(searchQuery, uiState.transactions) {
    derivedStateOf {
        if (searchQuery.isBlank()) uiState.transactions
        else uiState.transactions.filter {
            it.description?.contains(searchQuery, ignoreCase = true) == true
        }
    }
}
```

**Bài 2: Thêm Empty State**
```kotlin
// Khi danh sách rỗng, hiển thị thông báo friendly
if (uiState.transactions.isEmpty()) {
    EmptyState(
        icon = Icons.Filled.Receipt,
        message = "Chưa có giao dịch nào",
        actionText = "Thêm giao dịch đầu tiên",
        onAction = { showForm = true }
    )
}
```

**Bài 3: Format tiền tệ VND**
```kotlin
fun formatCurrency(amount: Double, currency: String = "VND"): String {
    return NumberFormat.getCurrencyInstance(Locale("vi", "VN"))
        .format(amount)
}
// Input: 1500000.0 → Output: "1.500.000 ₫"
```

### 5.2 Bài Tập Trung Bình (1 ngày)

**Bài 4: Implement Biểu Đồ Tròn (Pie Chart)**
Tự vẽ biểu đồ tỉ lệ chi tiêu theo danh mục bằng `Canvas` API:
```kotlin
@Composable
fun SpendingPieChart(categoryBreakdown: Map<String, Double>) {
    Canvas(modifier = Modifier.size(200.dp)) {
        var startAngle = 0f
        categoryBreakdown.forEach { (category, amount) ->
            val sweepAngle = (amount / total * 360).toFloat()
            drawArc(
                color = getCategoryColor(category),
                startAngle = startAngle,
                sweepAngle = sweepAngle,
                useCenter = true
            )
            startAngle += sweepAngle
        }
    }
}
```

**Bài 5: Thêm Search/Filter cho Transactions**
```kotlin
// Thêm search bar với debounce
var searchQuery by remember { mutableStateOf("") }
val filteredTransactions by remember(searchQuery, uiState.transactions) {
    derivedStateOf {
        if (searchQuery.isBlank()) uiState.transactions
        else uiState.transactions.filter {
            it.description?.contains(searchQuery, ignoreCase = true) == true
        }
    }
}
```

### 5.3 Bài Tập Nâng Cao (3-5 ngày)

**Bài 6: Export CSV**
```kotlin
suspend fun exportTransactionsToCsv(
    transactions: List<Transaction>,
    context: Context
): Uri {
    val fileName = "pumiah_export_${LocalDate.now()}.csv"
    val csvContent = buildString {
        appendLine("Date,Type,Category,Amount,Description")
        transactions.forEach { t ->
            appendLine("${t.date},${t.type},${t.categoryId},${t.amount},${t.description}")
        }
    }
    // Lưu vào Downloads folder...
}
```

**Bài 7: Recurring Transactions**
Thêm tính năng giao dịch định kỳ (lương hàng tháng, tiền thuê nhà):
- Thêm cột `is_recurring`, `recurrence_period` vào bảng transactions
- WorkManager để tự động tạo giao dịch định kỳ

**Bài 8: Multi-currency Support**
- Lưu tỷ giá từ ExchangeRate API
- Convert tất cả về currency của user khi display

---

## 6. Progress JSON

```json
{
  "project": "pumiah-finance-ai-manager-kotlin",
  "lastUpdated": "2026-04-05",
  "overallProgress": 87,

  "concepts": {
    "MVVM": {
      "understanding": 92,
      "implementation": 90,
      "testing": 40,
      "notes": "Implemented across all 9 ViewModels including WalletViewModel. Testing coverage still low."
    },
    "DependencyInjection_Hilt": {
      "understanding": 88,
      "implementation": 92,
      "testing": 60,
      "notes": "Singleton scope well understood. Testing with fakes/mocks partially done."
    },
    "JetpackCompose": {
      "understanding": 85,
      "implementation": 90,
      "performance": 58,
      "notes": "Canvas API applied for HSV color picker. PullToRefreshBox and FlowRow mastered. Animations still pending."
    },
    "StateFlow_Coroutines": {
      "understanding": 88,
      "implementation": 92,
      "testing": 30,
      "notes": "awaitSessionReady() pattern added. walletsLoading guard pattern learned. Turbine testing not yet implemented."
    },
    "SupabaseIntegration": {
      "understanding": 83,
      "implementation": 88,
      "RLS_security": 82,
      "notes": "Shared wallet RLS added. SessionStatus.Initializing handling implemented. Realtime not yet explored."
    },
    "GeminiAI_Integration": {
      "understanding": 78,
      "implementation": 75,
      "promptEngineering": 65,
      "notes": "Voice input added via RecognizerIntent. Context building could still be improved."
    },
    "NavigationCompose": {
      "understanding": 90,
      "implementation": 92,
      "deepLinks": 0,
      "notes": "popUpTo(Screen.Dashboard.route) bug fixed. restoreState = false for Profile tab implemented."
    },
    "ComposeCanvas": {
      "understanding": 72,
      "implementation": 70,
      "notes": "Applied for HSV SV box and hue slider. detectTapGestures + detectDragGestures used."
    },
    "AndroidPlatformAPIs": {
      "understanding": 75,
      "implementation": 72,
      "notes": "RecognizerIntent for voice input. ActivityResultContracts.StartActivityForResult pattern learned."
    },
    "HiltEntryPoints": {
      "understanding": 78,
      "implementation": 75,
      "notes": "EntryPointAccessors.fromApplication() used to inject ThemeManager into Composable (composables can't @Inject directly)."
    },
    "ThemingAndDarkMode": {
      "understanding": 80,
      "implementation": 82,
      "notes": "Material3 darkColorScheme + lightColorScheme. Toggle persisted via SharedPreferences + StateFlow. Why SharedPrefs over DataStore: sync read avoids flash on cold start."
    }
  },

  "skills": {
    "Kotlin": 85,
    "Android_Fundamentals": 83,
    "Compose_UI": 88,
    "REST_API": 75,
    "Database_Design": 78,
    "Testing": 35,
    "CI_CD": 40,
    "Security": 73,
    "Performance": 60,
    "Documentation": 90
  },

  "completedFeatures": {
    "count": 24,
    "list": [
      "Authentication (Login/Register/Logout)",
      "Session Persistence across restarts (awaitSessionReady)",
      "Dashboard with Summary Cards",
      "Pull-to-refresh on Dashboard",
      "Transaction CRUD",
      "Pull-to-refresh on Transactions",
      "First-letter badge for shared wallet transactions",
      "Category Management",
      "HSV Color Picker for categories",
      "50 category icons (FlowRow grid)",
      "Pull-to-refresh on Categories",
      "Budget Tracking with Progress",
      "Pull-to-refresh on Budgets",
      "Wallet loading guard (walletsLoading StateFlow)",
      "Goal Saving with Contributions",
      "Pull-to-refresh on Goals",
      "AI Chatbot (Gemini)",
      "Voice Input in Chat (RecognizerIntent vi-VN)",
      "Wallet Screen (personal + shared wallets)",
      "Manage shared wallet participants (add/remove)",
      "Profile Screen",
      "Navigation with Bottom Bar (5 tabs)",
      "Navigation bugs fixed (popUpTo, Profile restoreState)",
      "Dark Mode toggle (ThemeManager Singleton + SharedPreferences)"
    ]
  },

  "pendingFeatures": {
    "count": 6,
    "list": [
      "Search/Filter transactions",
      "Spending charts (pie, bar)",
      "CSV Export",
      "Offline support",
      "Push notifications",
      "Unit & Integration test coverage"
    ]
  },

  "knowledgeGaps": [
    {
      "topic": "Paging 3",
      "priority": "HIGH",
      "estimatedLearningTime": "2 days"
    },
    {
      "topic": "Unit Testing with Turbine",
      "priority": "HIGH",
      "estimatedLearningTime": "3 days"
    },
    {
      "topic": "Compose Canvas for Charts",
      "priority": "MEDIUM",
      "estimatedLearningTime": "1 day",
      "notes": "Basic Canvas already used in HSV picker. Charts are next step."
    },
    {
      "topic": "WorkManager for Background Tasks",
      "priority": "MEDIUM",
      "estimatedLearningTime": "1 day"
    },
    {
      "topic": "Compose Animations",
      "priority": "LOW",
      "estimatedLearningTime": "3 days"
    }
  ],

  "sprintVelocity": {
    "sprint1_points": 26,
    "sprint2_points": 42,
    "sprint3_points": 31,
    "sprint4_points": 47,
    "sprint5_points_estimated": 34,
    "averageVelocity": 37
  }
}
```

---

## 7. Tài Nguyên Học Tập Khuyến Nghị

### 7.1 Tài Liệu Chính Thức

| Tài liệu | URL | Ưu tiên |
|---|---|---|
| Android Developers | https://developer.android.com | Cao |
| Jetpack Compose Docs | https://developer.android.com/jetpack/compose | Cao |
| Kotlin Documentation | https://kotlinlang.org/docs | Cao |
| Supabase Kotlin SDK | https://supabase.com/docs/reference/kotlin | Cao |
| Material Design 3 | https://m3.material.io | Trung bình |
| Gemini API Docs | https://ai.google.dev/docs | Trung bình |

### 7.2 Khóa Học Đề Xuất

| Khóa học | Nền tảng | Chủ đề |
|---|---|---|
| Android Basics with Compose | Google Codelabs | Nền tảng Compose |
| Advanced Android with Compose | Google Codelabs | Nâng cao |
| Kotlin Coroutines | JetBrains Academy | Coroutines |
| Hilt in Android | Codelabs | Dependency Injection |

### 7.3 Kênh YouTube

- **Android Developers** - Official Google channel
- **Philipp Lackner** - Kotlin & Compose tutorials
- **CodingWithMitch** - Android architecture patterns

---

## 8. Phản Ánh và Bài Học Rút Ra

### 8.1 Điều Làm Tốt

1. **Kiến trúc rõ ràng:** MVVM với Repository pattern được áp dụng nhất quán trên 9 ViewModels
2. **Bảo mật:** API keys không bao giờ hardcode, RLS được setup đúng
3. **Code organization:** Cấu trúc thư mục theo feature rõ ràng
4. **Material Design 3:** UI nhất quán với MD3 components
5. **Session persistence:** `awaitSessionReady()` giải quyết đúng root cause, không dùng workaround
6. **Bug investigation:** Navigation `popUpTo` bug được debug đến tận nguyên nhân (Login node trong graph), không chỉ patch triệu chứng
7. **Canvas HSV picker:** Implement trực tiếp bằng Compose Canvas thay vì thêm thư viện màu — giữ APK nhỏ

### 8.2 Điều Cần Cải Thiện

1. **Testing:** Coverage gần như 0%, cần prioritize viết tests
2. **Error handling:** Edge cases voice recognition (từ chối quyền, thiết bị không hỗ trợ) chưa được handle
3. **Performance:** Chưa profile recomposition, chưa test với large datasets
4. **Offline support:** App hoàn toàn phụ thuộc internet

### 8.3 Quyết Định Đáng Tự Hào

1. **HttpURLConnection cho Gemini** — không thêm dependency nặng, kiểm soát hoàn toàn request, YAGNI principle.
2. **`sessionStatus.first { it !is SessionStatus.Initializing }`** — dùng Flow operator để chờ async event, không dùng polling/delay.
3. **`restoreState = item.screen != Screen.Profile`** — giải quyết Category tab persist chỉ bằng một dòng logic.
4. **SharedPreferences cho Dark Mode thay vì DataStore** — chọn đúng công cụ: DataStore async có thể gây flash theme khi cold start, SharedPreferences sync read cho boolean là lựa chọn đơn giản và đúng mục đích.
5. **`EntryPointAccessors` cho ThemeManager trong Composable** — nhận ra `hiltViewModel()` không phù hợp (ThemeManager không phải ViewModel), dùng pattern chính thức của Hilt thay vì workaround như passing qua CompositionLocal thủ công.

---

*Tài liệu này được cập nhật bởi Điều Phối Viên Giáo Dục - 2026-04-05*
