# 02 - Kiểm Toán Kiến Trúc: Pumiah Finance AI Manager

> **Vai trò:** Kiểm Toán Kiến Trúc
> **Timestamp:** 2026-04-05
> **Phiên bản tài liệu:** 1.2.0

---

## 1. Tổng Quan Kiến Trúc

Pumiah Finance AI Manager áp dụng kiến trúc **MVVM (Model-View-ViewModel)** kết hợp với **Repository Pattern** và **Dependency Injection** thông qua Hilt. Đây là kiến trúc được Google Android team khuyến nghị chính thức trong Android Architecture Guide.

```
┌─────────────────────────────────────────────────────┐
│                    UI Layer                          │
│  Composable Screens ←── ViewModel (StateFlow)       │
└─────────────────────────┬───────────────────────────┘
                          │ suspend functions
┌─────────────────────────▼───────────────────────────┐
│                  Domain Layer                        │
│  Repository Interfaces + Use Cases (nếu cần)        │
└─────────────────────────┬───────────────────────────┘
                          │
┌─────────────────────────▼───────────────────────────┐
│                  Data Layer                          │
│  SupabaseClient │ GeminiService │ Local Prefs        │
└─────────────────────────────────────────────────────┘
```

---

## 2. Architecture Decision Records (ADR)

### ADR-001: Chọn MVVM Pattern

**Ngày quyết định:** Tháng 1/2026
**Trạng thái:** Được chấp nhận

#### Bối cảnh
Cần kiến trúc phân tách rõ ràng giữa business logic và UI để dễ test, maintain, và scale.

#### Quyết định
Sử dụng **MVVM (Model-View-ViewModel)** với các thành phần:
- **Model:** Data classes + Repository
- **View:** Jetpack Compose Composables
- **ViewModel:** AndroidX ViewModel với StateFlow

#### Lý do
- Android Architecture Components ViewModel tự động handle lifecycle
- StateFlow tích hợp hoàn hảo với Compose recomposition
- Dễ viết unit test cho ViewModel mà không cần Android framework
- Google chính thức khuyến nghị cho Android development

#### Hệ quả
- Mỗi màn hình có một ViewModel tương ứng
- ViewModel không giữ tham chiếu đến View/Context
- Dữ liệu chảy một chiều: Repository → ViewModel → UI

#### Thay thế đã xem xét
- **MVP (Model-View-Presenter):** Presenter giữ tham chiếu View, khó test hơn
- **MVI (Model-View-Intent):** Phức tạp hơn, overkill cho project quy mô này
- **Clean Architecture đầy đủ:** Thêm Use Case layer, phù hợp cho project lớn hơn

---

### ADR-002: Hilt cho Dependency Injection

**Ngày quyết định:** Tháng 1/2026
**Trạng thái:** Được chấp nhận

#### Bối cảnh
Cần cơ chế DI để quản lý lifecycle của dependencies (SupabaseClient, Repositories) và tránh coupling chặt giữa các class.

#### Quyết định
Sử dụng **Hilt** (built on top of Dagger 2) với annotation-based injection.

#### Cấu trúc DI

```kotlin
// AppModule.kt
@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideSupabaseClient(): SupabaseClient {
        return createSupabaseClient(
            supabaseUrl = BuildConfig.SUPABASE_URL,
            supabaseKey = BuildConfig.SUPABASE_ANON_KEY
        ) {
            install(Auth)
            install(Postgrest)
        }
    }

    @Provides
    @Singleton
    fun provideAuthRepository(client: SupabaseClient): AuthRepository =
        AuthRepositoryImpl(client)

    @Provides
    @Singleton
    fun provideTransactionRepository(client: SupabaseClient): TransactionRepository =
        TransactionRepositoryImpl(client)
}
```

#### Lý do
- Google first-party support với tích hợp sâu vào Android lifecycle
- `@HiltViewModel` tự động inject dependencies vào ViewModel
- `@Singleton` scope đảm bảo SupabaseClient chỉ khởi tạo một lần
- Giảm boilerplate so với Dagger thuần (không cần Component/SubComponent thủ công)

#### Hệ quả
- `PumiahApplication` phải annotate `@HiltAndroidApp`
- `MainActivity` phải annotate `@AndroidEntryPoint`
- Tất cả Screen Composable dùng `hiltViewModel()` thay vì `viewModel()`

#### Thay thế đã xem xét
- **Manual DI:** Không scale, tăng coupling
- **Koin:** Nhẹ hơn nhưng runtime error thay vì compile-time, không có Google official support

---

### ADR-003: Supabase Kotlin SDK v3.1.4

**Ngày quyết định:** Tháng 1/2026
**Trạng thái:** Được chấp nhận

#### Bối cảnh
Cần backend để lưu trữ dữ liệu người dùng, thực hiện authentication, và enforce security rules.

#### Quyết định
Sử dụng **Supabase** với Kotlin SDK v3.1.4.

```toml
# libs.versions.toml
[versions]
supabase = "3.1.4"

[libraries]
supabase-postgrest = { module = "io.github.jan-tennert.supabase:postgrest-kt", version.ref = "supabase" }
supabase-auth = { module = "io.github.jan-tennert.supabase:auth-kt", version.ref = "supabase" }
supabase-realtime = { module = "io.github.jan-tennert.supabase:realtime-kt", version.ref = "supabase" }
```

#### Lý do
- PostgreSQL thực sự với SQL query đầy đủ
- Row Level Security (RLS) bảo vệ dữ liệu ở database level
- Supabase Auth tích hợp với JWT, session management tự động
- Kotlin SDK type-safe với serialization
- Dashboard UI trực quan để quản lý database
- Free tier đủ dùng cho development và MVP

#### Cấu hình SupabaseClient

```kotlin
val supabase = createSupabaseClient(
    supabaseUrl = "https://nlfsyjqkcgswdatpdaee.supabase.co",
    supabaseKey = BuildConfig.SUPABASE_ANON_KEY
) {
    install(Auth) {
        scheme = "app"
        host = "pumiah.finance"
    }
    install(Postgrest)
}
```

#### Hệ quả
- Cần `SUPABASE_ANON_KEY` trong `local.properties`
- RLS policies phải được setup đúng để bảo mật
- Supabase URL được hardcode vì không nhạy cảm

---

### ADR-004: Gemini AI qua HttpURLConnection

**Ngày quyết định:** Tháng 2/2026
**Trạng thái:** Được chấp nhận

#### Bối cảnh
Cần tích hợp AI chatbot để tư vấn tài chính cá nhân dựa trên dữ liệu thu chi của người dùng.

#### Quyết định
Gọi Gemini API trực tiếp qua **HttpURLConnection** thay vì dùng SDK.

```kotlin
// GeminiService.kt
class GeminiService @Inject constructor() {

    private val apiKey = BuildConfig.GEMINI_API_KEY
    private val endpoint = "https://generativelanguage.googleapis.com/v1beta/" +
        "models/gemini-2.5-flash:generateContent?key=$apiKey"

    suspend fun generateResponse(prompt: String): String = withContext(Dispatchers.IO) {
        val url = URL(endpoint)
        val connection = url.openConnection() as HttpURLConnection
        connection.requestMethod = "POST"
        connection.setRequestProperty("Content-Type", "application/json")
        connection.doOutput = true

        val body = buildJsonBody(prompt)
        connection.outputStream.write(body.toByteArray())

        val response = connection.inputStream.bufferedReader().readText()
        parseResponse(response)
    }

    private fun buildJsonBody(prompt: String): String = """
        {
            "contents": [{
                "parts": [{"text": "$prompt"}]
            }],
            "generationConfig": {
                "temperature": 0.7,
                "maxOutputTokens": 1024
            }
        }
    """.trimIndent()
}
```

#### Lý do
- Gemini Android SDK (tại thời điểm build) còn experimental
- HttpURLConnection là standard Java, không thêm dependency
- Kiểm soát hoàn toàn request headers, timeout, retry logic
- Dễ debug với logging
- Model `gemini-2.5-flash` cân bằng tốt giữa speed và quality

#### Thay thế đã xem xét
- **Google AI SDK for Android:** Đang beta, API có thể thay đổi
- **Retrofit:** Overkill cho một endpoint duy nhất
- **OkHttp:** Thêm dependency không cần thiết

---

### ADR-005: Navigation Compose cho Routing

**Ngày quyết định:** Tháng 1/2026
**Trạng thái:** Được chấp nhận

#### Quyết định
Sử dụng **Navigation Compose** (androidx.navigation:navigation-compose) với sealed class routes.

```kotlin
// NavRoutes.kt
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
}

// AppNavigation.kt
@Composable
fun AppNavigation() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Screen.Login.route
    ) {
        composable(Screen.Login.route) { LoginScreen(navController) }
        composable(Screen.Dashboard.route) { DashboardScreen(navController) }
        // ...
    }
}
```

#### Lý do
- Type-safe, nhất quán với Compose-only architecture
- Back stack được quản lý tự động
- Deep link support khi cần
- Argument passing với NavArgument type-safe

---

### ADR-006: StateFlow cho Reactive State Management

**Ngày quyết định:** Tháng 1/2026
**Trạng thái:** Được chấp nhận

#### Quyết định
Sử dụng **StateFlow** (từ Kotlin Coroutines) thay vì LiveData hoặc MutableState trực tiếp trong ViewModel.

```kotlin
class TransactionViewModel @Inject constructor(
    private val repo: TransactionRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(TransactionUiState())
    val uiState: StateFlow<TransactionUiState> = _uiState.asStateFlow()

    // Trong Composable:
    // val state by viewModel.uiState.collectAsState()
}
```

#### So sánh với LiveData

| Tiêu chí | StateFlow | LiveData |
|---|---|---|
| Kotlin-first | Có | Không (Java-based) |
| Initial value | Bắt buộc | Không bắt buộc |
| Null safety | Type-safe | Nullable |
| Coroutine integration | Native | Cần extension |
| Compose support | `collectAsState()` | `observeAsState()` |
| Testing | Dễ hơn | Cần CoroutineScope |

---

### ADR-007: Session Persistence qua `awaitSessionReady()`

**Ngày quyết định:** Tháng 4/2026
**Trạng thái:** Được chấp nhận

#### Bối cảnh
Khi app khởi động lại, `client.auth.currentUserOrNull()` trả về `null` ngay lập tức vì Supabase SDK chưa hoàn tất việc load session từ `SharedPrefsSessionManager`. Điều này khiến app hiển thị màn hình Login dù người dùng đã đăng nhập trước đó.

#### Quyết định
Thêm hàm `awaitSessionReady()` trong `AuthRepository` dùng `sessionStatus.first { it !is SessionStatus.Initializing }` để chờ SDK hoàn tất khởi tạo bất đồng bộ. `AppNavigation` chỉ render `NavHost` sau khi `sessionReady = true`.

```kotlin
// AuthRepository.kt
suspend fun awaitSessionReady() {
    client.auth.sessionStatus.first { it !is SessionStatus.Initializing }
}

// AuthViewModel.kt
private val _sessionReady = MutableStateFlow(false)
val sessionReady: StateFlow<Boolean> = _sessionReady

init {
    viewModelScope.launch {
        authRepository.awaitSessionReady()
        _sessionReady.value = true
    }
}

// AppNavigation.kt
val sessionReady by authViewModel.sessionReady.collectAsState()
if (!sessionReady) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
    return
}
val startDestination = if (authViewModel.isLoggedIn) Screen.Dashboard.route else Screen.Login.route
```

#### Lý do
- `currentUserOrNull()` là synchronous, trả về null trước khi async session load xong
- `SessionStatus.Initializing` là trạng thái transient — flow đảm bảo chỉ proceed khi đã resolve
- Import đúng: `io.github.jan.supabase.auth.status.SessionStatus` (không phải `io.github.jan.supabase.auth.SessionStatus`)
- Splash indicator thay thế splash screen riêng: đơn giản hơn, không cần Activity thêm

#### Hệ quả
- App hiển thị `CircularProgressIndicator` ngắn (~100-300ms) khi cold start
- Đăng nhập được duy trì qua mọi lần restart app
- `startDestination` được xác định động thay vì hardcode

#### Thay thế đã xem xét
- **SplashScreen API:** Phức tạp hơn, cần Activity riêng hoặc XML theme
- **DataStore cache userID:** Lưu trạng thái đăng nhập vào DataStore; nhưng vẫn cần đồng bộ với Supabase session thực

---

### ADR-008: `popUpTo(Screen.Dashboard.route)` cho Bottom Navigation

**Ngày quyết định:** Tháng 4/2026
**Trạng thái:** Được chấp nhận

#### Bối cảnh
Bottom navigation dùng `popUpTo(navController.graph.findStartDestination().id)` gây bug: nhấn tab Dashboard lại chuyển sang Transactions. Nguyên nhân: `findStartDestination()` trả về node Login (declared startDestination của graph), không phải Dashboard. Vì Login không còn trong back stack sau khi đăng nhập, `popUpTo` không pop gì cả, tạo back stack `[Dashboard, Transactions, Dashboard]`.

#### Quyết định
Dùng `popUpTo(Screen.Dashboard.route)` — route cụ thể của authenticated home — thay vì `findStartDestination()`.

```kotlin
navController.navigate(item.screen.route) {
    popUpTo(Screen.Dashboard.route) { saveState = true }
    launchSingleTop = true
    restoreState = item.screen != Screen.Profile
}
```

#### Lý do
- Dashboard là "home" thực tế của authenticated flow, không phải Login
- `restoreState = item.screen != Screen.Profile`: Profile tab không restore state để sub-screens (Categories, Budgets, Goals) được clear khi quay lại Profile

---

### ADR-009: Dark Mode qua `ThemeManager` Singleton + SharedPreferences

**Ngày quyết định:** Tháng 4/2026
**Trạng thái:** Được chấp nhận

#### Bối cảnh
Người dùng yêu cầu chế độ tối với Switch ở màn hình Hồ sơ. Theme phải được áp dụng ngay từ lần render đầu tiên (không flash light mode), giữ nguyên qua restart app, và cập nhật tức thời khi toggle.

#### Quyết định
Tạo `ThemeManager` là Hilt `@Singleton` với:
- `StateFlow<Boolean>` cho trạng thái dark mode
- SharedPreferences để persist (sync read — không blocking vì một boolean nhỏ)
- `MainActivity` `@Inject ThemeManager`, `collectAsState()`, truyền vào `PumiahTheme(darkTheme)`
- `ProfileScreen` dùng `EntryPointAccessors.fromApplication(...)` để lấy instance vì composable không hỗ trợ `@Inject` trực tiếp

```kotlin
// ThemeManager.kt
@Singleton
class ThemeManager @Inject constructor(
    @ApplicationContext context: Context
) {
    private val prefs = context.getSharedPreferences("pumiah_theme", Context.MODE_PRIVATE)
    private val _darkMode = MutableStateFlow(prefs.getBoolean(KEY_DARK_MODE, false))
    val darkMode: StateFlow<Boolean> = _darkMode

    fun setDarkMode(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_DARK_MODE, enabled).apply()
        _darkMode.value = enabled
    }
}

// MainActivity.kt
@Inject lateinit var themeManager: ThemeManager
setContent {
    val darkMode by themeManager.darkMode.collectAsState()
    PumiahTheme(darkTheme = darkMode) { AppNavigation() }
}

// ProfileScreen.kt — EntryPoint vì composable không @Inject được
@EntryPoint
@InstallIn(SingletonComponent::class)
interface ThemeManagerEntryPoint {
    fun themeManager(): ThemeManager
}

val themeManager = remember(context) {
    EntryPointAccessors.fromApplication(
        context.applicationContext, ThemeManagerEntryPoint::class.java
    ).themeManager()
}
```

#### Lý do chọn SharedPreferences thay vì DataStore
- DataStore API là suspend/Flow — cần coroutine để đọc lần đầu → có thể gây flash light mode trước khi value emit
- Boolean đơn giản, không cần migration, không cần type-safe proto schema
- SharedPreferences `getBoolean()` sync, trả về ngay — theme áp dụng từ render đầu tiên

#### Lý do chọn EntryPointAccessors
- Composable không phải ViewModel, không dùng `@HiltViewModel` được
- `hiltViewModel()` không phù hợp vì ThemeManager không phải ViewModel
- `EntryPointAccessors.fromApplication()` là pattern chính thức của Hilt cho trường hợp này

#### Hệ quả
- Trạng thái theme nằm ngoài scope ViewModel — hợp lý vì là app-wide setting
- Toggle cập nhật tức thời vì StateFlow trigger recomposition toàn bộ tree qua MainActivity
- File mới: `data/preferences/ThemeManager.kt`

#### Thay thế đã xem xét
- **DataStore Preferences:** Flow API tốt hơn cho observability, nhưng async read gây flash
- **System dark mode (isSystemInDarkTheme()):** Không cho phép user override — không đáp ứng yêu cầu
- **Material Dynamic Color:** Chỉ hoạt động Android 12+, không đáp ứng toggle thủ công

---

## 3. Danh Sách Dependencies và Phiên Bản

### 3.1 Build Configuration

```toml
# gradle/libs.versions.toml

[versions]
agp = "8.7.3"
kotlin = "2.1.0"
compose-bom = "2025.01.01"
hilt = "2.54"
navigation = "2.8.5"
supabase = "3.1.4"
ktor = "3.0.3"
coroutines = "1.10.1"
lifecycle = "2.8.7"
```

### 3.2 Dependencies Đầy Đủ

| Dependency | Phiên bản | Mục đích |
|---|---|---|
| Android Gradle Plugin | 8.7.3 | Build tool |
| Kotlin | 2.1.0 | Ngôn ngữ lập trình |
| Compose BOM | 2025.01.01 | Quản lý version Compose |
| Compose UI | BOM managed | UI framework |
| Compose Material3 | BOM managed | Material Design 3 components |
| Hilt Android | 2.54 | Dependency Injection |
| Hilt Navigation Compose | 1.2.0 | Hilt + Navigation tích hợp |
| Navigation Compose | 2.8.5 | In-app navigation |
| Supabase BOM | 3.1.4 | Quản lý version Supabase |
| Supabase Auth KT | 3.1.4 | Authentication |
| Supabase Postgrest KT | 3.1.4 | Database CRUD |
| Ktor Client Android | 3.0.3 | HTTP client cho Supabase SDK |
| Kotlin Coroutines | 1.10.1 | Async programming |
| Lifecycle ViewModel | 2.8.7 | MVVM ViewModel |
| Coil Compose | 2.7.0 | Image loading |
| KotlinX Serialization | 1.7.3 | JSON serialization |

### 3.3 Build Features

```kotlin
// app/build.gradle.kts
android {
    buildFeatures {
        compose = true
        buildConfig = true  // Để đưa API keys vào BuildConfig
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.15"
    }
}
```

---

## 4. Sơ Đồ Kiến Trúc Chi Tiết

```
┌──────────────────────────────────────────────────────────────────┐
│                        UI Layer (Compose)                        │
│                                                                  │
│  LoginScreen   DashboardScreen   TransactionScreen   ChatScreen  │
│      │               │                  │               │        │
│  AuthViewModel  DashboardVM    TransactionVM       ChatViewModel │
│      │               │                  │               │        │
│  ────┼───────────────┼──────────────────┼───────────────┼──────  │
│                   StateFlow / collectAsState()                   │
└──────────────────────────┬───────────────────────────────────────┘
                           │ Hilt @Inject
┌──────────────────────────▼───────────────────────────────────────┐
│                      Repository Layer                            │
│                                                                  │
│  AuthRepository    TransactionRepository    BudgetRepository     │
│  CategoryRepository    GoalRepository                            │
└──────────────────────────┬───────────────────────────────────────┘
                           │
              ┌────────────┼────────────┐
              │            │            │
┌─────────────▼──┐  ┌──────▼───────┐  ┌▼──────────────┐
│  SupabaseClient│  │  GeminiService│  │  Local Prefs  │
│  (Singleton)   │  │  (HttpURLConn)│  │  (DataStore)  │
└────────────────┘  └───────────────┘  └───────────────┘
        │                   │
        ▼                   ▼
  Supabase Cloud      Gemini AI API
  PostgreSQL          (Google Cloud)
```

---

## 5. Nguyên Tắc Thiết Kế

### 5.1 Single Source of Truth
Mỗi loại dữ liệu có một nguồn duy nhất. Supabase là SSOT cho tất cả dữ liệu tài chính. ViewModel giữ state UI tạm thời.

### 5.2 Separation of Concerns
- Composable chỉ render UI và gửi events
- ViewModel xử lý business logic, không có Android context
- Repository trừu tượng hóa nguồn dữ liệu

### 5.3 Dependency Inversion
Repository layer định nghĩa interfaces, implementations được inject bởi Hilt. Điều này cho phép mock trong unit tests.

### 5.4 Fail Fast
Error handling thông qua sealed Result class, UI hiển thị error state rõ ràng thay vì silent fail.

---

*Tài liệu này được tạo bởi Kiểm Toán Kiến Trúc - 2026-03-29*
