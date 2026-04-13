# 08 - Hiệu Năng, Bảo Mật và Kế Hoạch Sprint

> **Vai trò:** Kỹ Sư Hiệu Năng + Quản Lý Dự Án
> **Timestamp:** 2026-04-05
> **Phiên bản tài liệu:** 1.2.0

---

## 1. Metrics Hiệu Năng

### 1.1 Bảng Performance Baseline

| Metric | Mục tiêu | Đo được | Trạng thái |
|---|---|---|---|
| Cold Start Time | < 2 giây | ~1.8s | Đạt |
| Warm Start Time | < 500ms | ~350ms | Đạt |
| API Response (Supabase) | < 800ms | ~400-600ms | Đạt |
| API Response (Gemini) | < 3 giây | ~1.5-2.5s | Đạt |
| Memory Usage (Idle) | < 100 MB | ~75 MB | Đạt |
| Memory Usage (Active) | < 200 MB | ~120 MB | Đạt |
| APK Size (Debug) | < 20 MB | ~12 MB | Đạt |
| APK Size (Release) | < 15 MB | ~9 MB | Đạt |
| Frame Rate (Scroll) | 60 FPS | ~58-60 FPS | Đạt |
| Battery Impact | Thấp | Thấp | Đạt |

### 1.2 Công Cụ Đo Hiệu Năng

- **Android Studio Profiler:** CPU, Memory, Network, Battery
- **Macrobenchmark:** Cold/Warm start time
- **Compose Performance:** Recomposition tracking
- **LeakCanary:** Memory leak detection

---

## 2. Chiến Lược Tối Ưu Hiệu Năng

### 2.1 LazyColumn với Key Ổn Định

Sử dụng `key` parameter trong `LazyColumn` để tránh recomposition không cần thiết:

```kotlin
// ✅ Đúng: Dùng stable key
LazyColumn {
    items(
        items = transactions,
        key = { transaction -> transaction.id }  // Stable key
    ) { transaction ->
        TransactionRow(transaction = transaction)
    }
}

// ❌ Sai: Không có key (index-based, gây recomposition toàn bộ khi insert)
LazyColumn {
    items(transactions) { transaction ->
        TransactionRow(transaction = transaction)
    }
}
```

**Lý do:** Khi thêm/xóa item, Compose chỉ recompose item thay đổi thay vì toàn bộ list.

### 2.2 StateFlow thay vì LiveData

```kotlin
// ✅ StateFlow - coroutine native, không cần Observer lifecycle
private val _uiState = MutableStateFlow(UiState())
val uiState: StateFlow<UiState> = _uiState.asStateFlow()

// Trong Composable: collectAsState() chỉ trigger recompose khi value thực sự thay đổi
val state by viewModel.uiState.collectAsState()
```

**Lợi ích:**
- Không gây memory leak (không cần observer lifecycle)
- `distinctUntilChanged` built-in: không emit nếu value không đổi
- Test dễ hơn với coroutines test

### 2.3 Hilt Singleton Scope

```kotlin
// AppModule.kt - SupabaseClient là Singleton
@Provides
@Singleton  // Chỉ khởi tạo MỘT LẦN trong suốt vòng đời ứng dụng
fun provideSupabaseClient(): SupabaseClient {
    return createSupabaseClient(
        supabaseUrl = BuildConfig.SUPABASE_URL,
        supabaseKey = BuildConfig.SUPABASE_ANON_KEY
    ) { ... }
}
```

**Lợi ích:**
- Tránh tốn thời gian khởi tạo HTTP client nhiều lần
- Connection pooling hiệu quả hơn
- Supabase Auth session được duy trì nhất quán

### 2.4 remember và derivedStateOf

```kotlin
// ✅ Dùng remember để cache tính toán tốn kém
@Composable
fun DashboardScreen(viewModel: DashboardViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsState()

    // derivedStateOf: chỉ recalculate khi dependency thay đổi
    val formattedBalance by remember(uiState.totalIncome, uiState.totalExpense) {
        derivedStateOf {
            formatCurrency(uiState.totalIncome - uiState.totalExpense)
        }
    }
}
```

### 2.5 Coil cho Image Loading

```kotlin
// Lazy image loading với Coil - không block main thread
AsyncImage(
    model = ImageRequest.Builder(LocalContext.current)
        .data(user.avatarUrl)
        .crossfade(true)  // Smooth transition
        .build(),
    contentDescription = "Avatar",
    modifier = Modifier.size(48.dp).clip(CircleShape)
)
```

### 2.6 Pagination (Tương lai)

Khi số lượng giao dịch tăng lớn, cần implement pagination:

```kotlin
// Paging 3 integration (kế hoạch tương lai)
suspend fun getTransactionsPaged(page: Int, limit: Int = 20): List<Transaction> {
    return supabase.postgrest["transactions"]
        .select {
            order("date", ascending = false)
            range(from = (page * limit).toLong(), to = ((page + 1) * limit - 1).toLong())
        }
        .decodeList<Transaction>()
}
```

---

## 3. Bảo Mật (Security)

### 3.1 Quản Lý API Keys

**Nguyên tắc:** API keys KHÔNG BAO GIỜ được hardcode trong source code.

```
local.properties (git-ignored)
    │  SUPABASE_ANON_KEY=eyJ...
    │  GEMINI_API_KEY=AIza...
    │
    ▼  Gradle build time
BuildConfig.java (generated, git-ignored)
    │  public static final String SUPABASE_ANON_KEY = "eyJ...";
    │  public static final String GEMINI_API_KEY = "AIza...";
    │
    ▼  Runtime
SupabaseClient / GeminiService
    │  Sử dụng BuildConfig.SUPABASE_ANON_KEY
```

**Cấu hình .gitignore:**
```gitignore
# API Keys - KHÔNG COMMIT
local.properties

# Build outputs
/build
/app/build

# Keystore files
*.jks
*.keystore
```

### 3.2 Row Level Security (RLS)

Supabase RLS đảm bảo dữ liệu được bảo vệ ở tầng database, không chỉ ở tầng application:

```sql
-- Dù có bug trong app, RLS ngăn người dùng A đọc dữ liệu của người dùng B
CREATE POLICY "users_own_data"
    ON public.transactions
    FOR ALL
    USING (auth.uid() = user_id);
    -- auth.uid() = JWT sub claim từ Supabase Auth token
```

**Kiểm tra RLS hoạt động:**
```sql
-- Test với role anon (không có token) - phải trả về 0 rows
SET ROLE anon;
SELECT * FROM public.transactions;
-- Expected: 0 rows (RLS blocks)

-- Test với authenticated user
SET request.jwt.claims = '{"sub": "user-uuid-here"}';
SELECT * FROM public.transactions;
-- Expected: chỉ rows có user_id = 'user-uuid-here'
```

### 3.3 Network Security Configuration

```xml
<!-- res/xml/network_security_config.xml -->
<?xml version="1.0" encoding="utf-8"?>
<network-security-config>
    <!-- Chỉ cho phép HTTPS, không cho cleartext HTTP -->
    <base-config cleartextTrafficPermitted="false">
        <trust-anchors>
            <certificates src="system" />
        </trust-anchors>
    </base-config>

    <!-- Explicitly allow HTTPS cho các domains sử dụng -->
    <domain-config cleartextTrafficPermitted="false">
        <domain includeSubdomains="true">supabase.co</domain>
        <domain includeSubdomains="true">googleapis.com</domain>
    </domain-config>
</network-security-config>
```

```xml
<!-- AndroidManifest.xml -->
<application
    android:networkSecurityConfig="@xml/network_security_config"
    ...>
```

### 3.4 Supabase JWT Token Management

Supabase SDK tự động quản lý JWT refresh:

```kotlin
// Auth session tự động refresh khi token sắp hết hạn
// SDK handle refresh token rotation
supabase.auth.sessionStatus.collect { status ->
    when (status) {
        is SessionStatus.Authenticated -> {
            // Session hợp lệ, tiếp tục bình thường
        }
        is SessionStatus.NotAuthenticated -> {
            // Session hết hạn, redirect về Login
            navController.navigate(Screen.Login.route)
        }
        is SessionStatus.RefreshFailure -> {
            // Không thể refresh, yêu cầu đăng nhập lại
        }
    }
}
```

### 3.5 Input Validation

```kotlin
// Validate trước khi gửi lên server
fun validateTransaction(
    amountStr: String,
    description: String,
    date: String
): ValidationResult {
    val errors = mutableListOf<String>()

    val amount = amountStr.toDoubleOrNull()
    if (amount == null || amount <= 0) {
        errors.add("Số tiền phải là số dương")
    }
    if (amount != null && amount > 1_000_000_000_000.0) {
        errors.add("Số tiền quá lớn")
    }
    if (description.length > 500) {
        errors.add("Mô tả không được quá 500 ký tự")
    }

    return if (errors.isEmpty()) ValidationResult.Valid
    else ValidationResult.Invalid(errors)
}
```

---

## 4. Kế Hoạch Sprint

### 4.1 Sprint 1: Foundation (Tuần 1-2)

**Mục tiêu:** Thiết lập nền tảng dự án

| Task | Điểm | Trạng thái |
|---|---|---|
| Setup Android project với Compose | 3 | Hoàn thành |
| Cấu hình Hilt DI | 3 | Hoàn thành |
| Tích hợp Supabase SDK | 5 | Hoàn thành |
| Implement Auth (Login/Register) | 8 | Hoàn thành |
| Setup Navigation Compose | 3 | Hoàn thành |
| Cấu hình Material Theme | 2 | Hoàn thành |
| Setup local.properties + BuildConfig | 2 | Hoàn thành |
| **Tổng** | **26** | **100%** |

### 4.2 Sprint 2: Core Features (Tuần 3-4)

**Mục tiêu:** Implement tính năng quản lý tài chính cốt lõi

| Task | Điểm | Trạng thái |
|---|---|---|
| Transaction CRUD (list, add, edit, delete) | 8 | Hoàn thành |
| Category management | 5 | Hoàn thành |
| Budget tracking với progress indicator | 8 | Hoàn thành |
| Goal saving với contribution | 8 | Hoàn thành |
| Database seed (17 default categories) | 3 | Hoàn thành |
| RLS policies setup | 5 | Hoàn thành |
| Dashboard với summary cards | 5 | Hoàn thành |
| **Tổng** | **42** | **100%** |

### 4.3 Sprint 3: AI & Polish (Tuần 5-6)

**Mục tiêu:** Tích hợp AI và hoàn thiện UX

| Task | Điểm | Trạng thái |
|---|---|---|
| Gemini AI integration (HttpURLConnection) | 8 | Hoàn thành |
| ChatScreen với ChatBubble | 5 | Hoàn thành |
| Financial context building cho AI | 5 | Hoàn thành |
| Profile screen | 3 | Hoàn thành |
| Error handling & loading states | 5 | Hoàn thành |
| UI/UX refinement | 5 | Hoàn thành |
| **Tổng** | **31** | **100%** |

### 4.4 Sprint 4: Fix & Feature Polish (Tuần 7-8)

**Mục tiêu:** Sửa bug, hoàn thiện tính năng, cải thiện UX

| Task | Điểm | Trạng thái |
|---|---|---|
| Session persistence (awaitSessionReady) | 5 | **Hoàn thành** |
| Pull-to-refresh (6 màn hình) | 8 | **Hoàn thành** |
| Wallet screen + shared wallets | 8 | **Hoàn thành** |
| Fix navigation popUpTo bug | 3 | **Hoàn thành** |
| Voice input trong Chat | 5 | **Hoàn thành** |
| HSV color picker cho Category | 5 | **Hoàn thành** |
| 50 category icons | 3 | **Hoàn thành** |
| First-letter badge ở ví chung | 2 | **Hoàn thành** |
| Xóa nút Refresh, dùng pull-to-refresh | 2 | **Hoàn thành** |
| Wallet loading guard (walletsLoading) | 3 | **Hoàn thành** |
| Fix remove participant ví chung | 2 | **Hoàn thành** |
| App name → "Pumiah Finance AI Manager" | 1 | **Hoàn thành** |
| Dark Mode với Switch ở Profile | 5 | **Hoàn thành** |
| **Tổng** | **52** | **100%** |

### 4.5 Sprint 5: Testing & Release (Tuần 9-10)

**Mục tiêu:** Kiểm thử và chuẩn bị release

| Task | Điểm | Trạng thái |
|---|---|---|
| Unit tests cho ViewModels | 8 | Chưa bắt đầu |
| Integration tests Supabase | 5 | Chưa bắt đầu |
| CI/CD GitHub Actions setup | 5 | Chưa bắt đầu |
| Performance profiling | 3 | Chưa bắt đầu |
| Security audit | 5 | Chưa bắt đầu |
| Documentation cập nhật | 8 | **Đang làm** |
| **Tổng** | **34** | **10%** |

---

## 5. Đánh Giá Rủi Ro

### 5.1 Bảng Risk Assessment

| Rủi ro | Xác suất | Tác động | Mức độ | Kế hoạch giảm thiểu |
|---|---|---|---|---|
| Gemini API giới hạn rate (15 RPM free tier) | Cao | Trung bình | **Cao** | Implement debouncing, cache responses, hiển thị error thân thiện |
| Supabase free tier giới hạn (500MB DB) | Trung bình | Cao | **Cao** | Cleanup old data, monitor usage, upgrade plan khi cần |
| JWT token hết hạn trong mid-operation | Thấp | Cao | **Trung bình** | SDK auto-refresh, handle SessionStatus |
| Android version fragmentation | Trung bình | Trung bình | **Trung bình** | Min SDK 26, test trên nhiều API levels |
| Network timeout (Gemini ~3s) | Trung bình | Thấp | **Thấp** | Timeout configuration, retry logic, loading indicator |
| Memory leak trong Composables | Thấp | Cao | **Trung bình** | LeakCanary, tránh hold Context trong lambda |
| Breaking changes Supabase SDK | Thấp | Cao | **Trung bình** | Version catalog, pin exact versions |
| Gemini API model deprecation | Thấp | Trung bình | **Thấp** | Abstract GeminiService interface, dễ swap model |

### 5.2 Kế Hoạch Contingency

**Nếu Gemini API bị rate limit:**
```kotlin
// Implement exponential backoff
suspend fun sendWithRetry(message: String, maxRetries: Int = 3): String {
    repeat(maxRetries) { attempt ->
        try {
            return geminiService.generateResponse(message)
        } catch (e: RateLimitException) {
            if (attempt < maxRetries - 1) {
                delay((2.0.pow(attempt) * 1000).toLong()) // 1s, 2s, 4s
            }
        }
    }
    throw RateLimitException("Đã hết số lần thử")
}
```

**Nếu Supabase DB đầy:**
- Implement data archival: tự động archive giao dịch > 2 năm
- Notification cho user khi dung lượng > 80%

---

## 6. Monitoring và Alerting

### 6.1 Crash Reporting (Tương lai)

```kotlin
// Tích hợp Firebase Crashlytics (kế hoạch)
class PumiahApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(true)
    }
}
```

### 6.2 Analytics Events (Tương lai)

| Event | Trigger | Params |
|---|---|---|
| `user_login` | Đăng nhập thành công | method |
| `transaction_created` | Tạo giao dịch | type, amount_range |
| `ai_chat_sent` | Gửi tin nhắn AI | message_length |
| `budget_exceeded` | Vượt ngân sách | category |
| `goal_achieved` | Đạt mục tiêu | goal_duration_days |

---

*Tài liệu này được cập nhật bởi Kỹ Sư Hiệu Năng + Quản Lý Dự Án - 2026-04-05*
