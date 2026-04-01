# 01 — Tổng Quan Xây Dựng

> **Vai trò:** Người Quan Sát Xây Dựng
> **Ngày:** 2026-04-01
> **Phiên bản:** 1.1.0

## Mô tả dự án

Pumiah Finance AI Manager Android là ứng dụng quản lý tài chính cá nhân được chuyển đổi từ phiên bản React (Vite + Zustand + Ant Design) sang Kotlin Android (Jetpack Compose + Hilt + Supabase).

Ứng dụng cho phép người dùng:
- Ghi chép thu nhập và chi tiêu hàng ngày
- Xem báo cáo tổng quan theo tháng
- Đặt ngân sách theo danh mục và theo dõi mức chi tiêu
- Tạo mục tiêu tiết kiệm và theo dõi tiến độ
- Quản lý nhiều ví, bao gồm ví chung (shared wallet) với nhiều thành viên
- Trò chuyện với AI Gemini để phân tích tài chính (hỗ trợ nhập bằng giọng nói)

## Cấu trúc thư mục

```
pumiah-finance-ai-manager-kotlin/
├── gradle/
│   └── libs.versions.toml          # Version catalog
├── app/
│   ├── build.gradle.kts            # App dependencies & BuildConfig
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── java/com/phuocpham/pumiah/
│       │   ├── PumiahApplication.kt      # @HiltAndroidApp
│       │   ├── MainActivity.kt            # Entry point
│       │   ├── data/
│       │   │   ├── model/Models.kt        # Data classes + UiState
│       │   │   └── repository/            # 8 repositories
│       │   ├── di/
│       │   │   └── AppModule.kt          # Hilt DI providers
│       │   ├── ui/
│       │   │   ├── navigation/AppNavigation.kt
│       │   │   ├── theme/Theme.kt
│       │   │   └── screen/               # 10 screens
│       │   └── viewmodel/                # 9 ViewModels
│       └── res/
│           ├── values/strings.xml
│           ├── values/themes.xml
│           └── xml/network_security_config.xml
├── docs/                               # Tài liệu dự án
├── build.gradle.kts
├── settings.gradle.kts
└── local.properties.example
```

## Cây thành phần (Component Tree)

```
PumiahApplication (@HiltAndroidApp)
└── MainActivity (@AndroidEntryPoint)
    └── PumiahTheme
        └── AppNavigation
            ├── [Auth] LoginScreen
            │   └── AuthViewModel
            ├── [Auth] RegisterScreen
            │   └── AuthViewModel
            └── [Main] Scaffold + NavigationBar (5 tabs)
                ├── DashboardScreen
                │   └── DashboardViewModel
                │       ├── SummaryCards
                │       └── TransactionRow (x5)
                ├── TransactionListScreen
                │   └── TransactionViewModel
                │       ├── TransactionItem (xN, badge email ký tự đầu)
                │       └── TransactionFormDialog
                ├── ChatScreen
                │   └── ChatViewModel
                │       ├── ChatBubble (xN)
                │       └── VoiceInput (RecognizerIntent)
                ├── WalletScreen
                │   └── WalletViewModel
                │       ├── WalletCard (xN)
                │       └── ManageWalletDialog
                ├── ProfileScreen
                │   ├── ProfileViewModel
                │   └── AuthViewModel
                │       ├── → CategoryScreen
                │       │   └── CategoryViewModel
                │       │       ├── CategoryItem (xN)
                │       │       └── CategoryFormDialog (HSV picker + 50 icons)
                │       ├── → BudgetScreen
                │       │   └── BudgetViewModel
                │       │       ├── BudgetItem (xN)
                │       │       └── BudgetFormDialog
                │       └── → GoalScreen
                │           └── GoalViewModel
                │               ├── GoalCard (xN)
                │               └── ContributionDialog
```

## Luồng dữ liệu (Data Flow)

```
User Action
    ↓
Composable (UI Event)
    ↓
ViewModel (viewModelScope.launch)
    ↓
Repository (suspend fun)
    ↓
Supabase Client (postgrest / auth)
    ↓
Supabase Cloud (PostgreSQL + RLS)
    ↓
Result<T>
    ↓
StateFlow<UiState<T>>
    ↓
collectAsState() trong Composable
    ↓
Recomposition → UI cập nhật
```

## Build Log

| Thời điểm | Hoạt động |
|-----------|-----------|
| Phase 1 | Khởi tạo Gradle project, libs.versions.toml, app/build.gradle.kts |
| Phase 2 | Tạo data layer: Models.kt, 8 Repositories |
| Phase 3 | Tạo DI: AppModule.kt với Hilt |
| Phase 4 | Tạo 9 ViewModels |
| Phase 5 | Tạo UI Theme, AppNavigation |
| Phase 6 | Tạo 10 Compose Screens |
| Phase 7 | Viết tài liệu tiếng Việt (9 file) |
| Phase 8 | Fix & polish: session persistence, pull-to-refresh, voice chat, HSV color picker, 50 category icons, wallet screen, navigation bug fixes |

## Các quyết định quan trọng

1. **Wallet screen được thêm vào v1.1**: WalletViewModel + WalletRepository + WalletScreen với quản lý ví chung.
2. **Gemini via HttpURLConnection**: Tránh dependency `google-generativeai` nặng (~10MB), dùng trực tiếp REST API.
3. **Supabase URL hardcode**: URL không phải secret, chỉ ANON_KEY cần local.properties.
4. **SUPABASE_URL trong BuildConfig**: Cho phép thay đổi môi trường mà không sửa code.
5. **Session persistence qua `awaitSessionReady()`**: `SessionStatus.Initializing` check async thay vì `currentUserOrNull()` sync — giải quyết lỗi mất đăng nhập khi khởi động lại app.
6. **Navigation `popUpTo(Screen.Dashboard.route)`**: Dùng route cụ thể thay vì `findStartDestination()` — tránh bug Login node nằm trên back stack khi đã đăng nhập.
7. **HSV Color Picker bằng Canvas**: Không dùng dialog lồng nhau, picker inline trong CategoryFormDialog.
8. **Voice input dùng `RecognizerIntent`**: Không cần thư viện ngoài, chỉ cần permission `RECORD_AUDIO`.
