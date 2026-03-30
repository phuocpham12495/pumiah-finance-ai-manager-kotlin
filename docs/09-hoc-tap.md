# 09 - Tiến Độ Học Tập và Phát Triển Kỹ Năng

> **Vai trò:** Điều Phối Viên Giáo Dục
> **Timestamp:** 2026-03-29
> **Phiên bản tài liệu:** 1.0.0

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

---

## 4. Khoảng Trống Kiến Thức (Knowledge Gaps)

### 4.1 Ưu Tiên Cao - Cần Học Sớm

| Chủ đề | Lý do cần học | Tài liệu tham khảo |
|---|---|---|
| **Paging 3 Library** | Khi giao dịch > 100 items, LazyColumn load hết sẽ chậm | [developer.android.com/topic/libraries/architecture/paging](https://developer.android.com/topic/libraries/architecture/paging) |
| **Unit Testing với Turbine** | Hiện tại chưa có test coverage, cần thiết cho production | [github.com/cashapp/turbine](https://github.com/cashapp/turbine) |
| **Compose Performance Tracing** | Chưa biết cách detect recomposition không cần thiết | [developer.android.com/jetpack/compose/performance](https://developer.android.com/jetpack/compose/performance) |
| **Offline-first với DataStore** | App hiện tại không hoạt động offline | [developer.android.com/topic/libraries/architecture/datastore](https://developer.android.com/topic/libraries/architecture/datastore) |

### 4.2 Ưu Tiên Trung Bình

| Chủ đề | Lý do cần học |
|---|---|
| **Compose Animation API** | UI hiện tại thiếu transitions và animations |
| **WorkManager** | Nhắc nhở tài chính định kỳ, sync background |
| **Room Database** | Local caching cho offline support |
| **Supabase Realtime** | Đồng bộ dữ liệu real-time giữa devices |
| **Compose Canvas** | Vẽ biểu đồ thu chi (pie chart, bar chart) |

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

**Bài 1: Thêm Pull-to-Refresh**
```kotlin
// Thêm vào TransactionListScreen
val pullRefreshState = rememberPullToRefreshState()

Box(Modifier.nestedScroll(pullRefreshState.nestedScrollConnection)) {
    LazyColumn { ... }
    PullToRefreshContainer(
        state = pullRefreshState,
        modifier = Modifier.align(Alignment.TopCenter)
    )
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
  "lastUpdated": "2026-03-29",
  "overallProgress": 72,

  "concepts": {
    "MVVM": {
      "understanding": 90,
      "implementation": 85,
      "testing": 40,
      "notes": "Implemented across all 8 ViewModels. Testing coverage low."
    },
    "DependencyInjection_Hilt": {
      "understanding": 85,
      "implementation": 90,
      "testing": 60,
      "notes": "Singleton scope well understood. Testing with fakes/mocks partially done."
    },
    "JetpackCompose": {
      "understanding": 80,
      "implementation": 85,
      "performance": 50,
      "notes": "Core composables mastered. Performance optimization and animations pending."
    },
    "StateFlow_Coroutines": {
      "understanding": 85,
      "implementation": 90,
      "testing": 30,
      "notes": "StateFlow well used. Turbine testing for flows not yet implemented."
    },
    "SupabaseIntegration": {
      "understanding": 80,
      "implementation": 85,
      "RLS_security": 80,
      "notes": "SDK usage good. RLS configured. Realtime not yet explored."
    },
    "GeminiAI_Integration": {
      "understanding": 75,
      "implementation": 70,
      "promptEngineering": 60,
      "notes": "Basic integration done. Context building could be improved."
    },
    "NavigationCompose": {
      "understanding": 85,
      "implementation": 90,
      "deepLinks": 0,
      "notes": "Navigation working well. Deep links not needed yet."
    }
  },

  "skills": {
    "Kotlin": 82,
    "Android_Fundamentals": 78,
    "Compose_UI": 83,
    "REST_API": 72,
    "Database_Design": 75,
    "Testing": 35,
    "CI_CD": 40,
    "Security": 70,
    "Performance": 55,
    "Documentation": 85
  },

  "completedFeatures": {
    "count": 11,
    "list": [
      "Authentication (Login/Register/Logout)",
      "Dashboard with Summary Cards",
      "Transaction CRUD",
      "Category Management",
      "Budget Tracking with Progress",
      "Goal Saving with Contributions",
      "AI Chatbot (Gemini)",
      "Profile Screen",
      "Navigation with Bottom Bar",
      "RLS Database Security",
      "17 Default Categories Seed"
    ]
  },

  "pendingFeatures": {
    "count": 9,
    "list": [
      "Pull-to-refresh",
      "Search/Filter transactions",
      "Spending charts",
      "CSV Export",
      "Offline support",
      "Push notifications",
      "Unit test coverage",
      "Integration tests",
      "Performance optimization"
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
      "estimatedLearningTime": "2 days"
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
    "sprint4_points_estimated": 34,
    "averageVelocity": 33
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

1. **Kiến trúc rõ ràng:** MVVM với Repository pattern được áp dụng nhất quán
2. **Bảo mật:** API keys không bao giờ hardcode, RLS được setup đúng
3. **Code organization:** Cấu trúc thư mục theo feature rõ ràng
4. **Material Design 3:** UI nhất quán với MD3 components

### 8.2 Điều Cần Cải Thiện

1. **Testing:** Coverage gần như 0%, cần prioritize viết tests
2. **Error handling:** Một số edge cases chưa được handle tốt
3. **Performance:** Chưa profile recomposition, chưa test với large datasets
4. **Offline support:** App hoàn toàn phụ thuộc internet

### 8.3 Quyết Định Đáng Tự Hào

Sử dụng **HttpURLConnection** thay vì SDK cho Gemini là quyết định đúng - đơn giản, kiểm soát được, và không thêm dependency không cần thiết. Đây là ví dụ tốt về nguyên tắc "YAGNI" (You Ain't Gonna Need It).

---

*Tài liệu này được tạo bởi Điều Phối Viên Giáo Dục - 2026-03-29*
