# 03 - Hướng Dẫn Cài Đặt và Thiết Lập Môi Trường

> **Vai trò:** Kỹ Sư DevOps
> **Timestamp:** 2026-03-29
> **Phiên bản tài liệu:** 1.0.0

---

## 1. Yêu Cầu Tiên Quyết (Prerequisites)

### 1.1 Phần Mềm Bắt Buộc

| Phần mềm | Phiên bản tối thiểu | Ghi chú |
|---|---|---|
| Android Studio | Ladybug (2024.2.1) trở lên | IDE chính thức |
| JDK (Java Development Kit) | 17 (LTS) | Bắt buộc cho AGP 8.7.3+ |
| Android SDK | API 35 (Android 15) | Target SDK của dự án |
| Git | 2.30+ | Quản lý source code |
| Gradle | 8.9+ | Managed by wrapper, không cần cài thủ công |

### 1.2 Android SDK Components

Trong Android Studio SDK Manager, đảm bảo đã cài:
- **SDK Platforms:** Android 15 (API 35), Android 8.0 (API 26 - Min SDK)
- **SDK Tools:**
  - Android SDK Build-Tools 35.0.0
  - Android Emulator
  - Android SDK Platform-Tools
  - Google Play Services

### 1.3 Tài Khoản Cần Thiết

| Dịch vụ | Mục đích | URL |
|---|---|---|
| Supabase | Backend database, authentication | https://supabase.com |
| Google AI Studio | Lấy Gemini API key | https://aistudio.google.com |
| GitHub (tùy chọn) | Source code repository | https://github.com |

---

## 2. Kiểm Tra Môi Trường

### 2.1 Kiểm tra Java Version

```bash
java -version
# Output mong đợi:
# openjdk version "17.0.x" 2024-xx-xx
# OpenJDK Runtime Environment...
```

### 2.2 Kiểm tra Android SDK

```bash
# Kiểm tra adb (Android Debug Bridge)
adb version
# Output: Android Debug Bridge version 1.0.41
```

### 2.3 Kiểm tra ANDROID_HOME

```bash
# Windows (PowerShell)
echo $env:ANDROID_HOME
# Kết quả mong đợi: C:\Users\<username>\AppData\Local\Android\Sdk

# macOS/Linux
echo $ANDROID_HOME
# Kết quả mong đợi: /Users/<username>/Library/Android/sdk
```

---

## 3. Hướng Dẫn Clone và Setup

### 3.1 Clone Repository

```bash
# Clone dự án
git clone https://github.com/your-org/pumiah-finance-ai-manager-kotlin.git

# Di chuyển vào thư mục dự án
cd pumiah-finance-ai-manager-kotlin
```

### 3.2 Thiết Lập local.properties

File `local.properties` chứa các API key nhạy cảm và **KHÔNG được commit** lên git (đã được thêm vào `.gitignore`).

**Bước 1:** Copy file template

```bash
cp local.properties.example local.properties
```

**Bước 2:** Mở file `local.properties` và điền thông tin:

```properties
# local.properties
# File này chứa thông tin nhạy cảm - KHÔNG commit lên git

# Android SDK path (tự động tạo bởi Android Studio)
sdk.dir=C\:\\Users\\YourName\\AppData\\Local\\Android\\Sdk

# Supabase Configuration
SUPABASE_URL=https://nlfsyjqkcgswdatpdaee.supabase.co
SUPABASE_ANON_KEY=eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...

# Google Gemini AI
GEMINI_API_KEY=AIzaSyB...
```

### 3.3 Bảng Biến Môi Trường

| Biến | Ví dụ | Mô tả | Bắt buộc |
|---|---|---|---|
| `SUPABASE_URL` | `https://nlfsyjqkcgswdatpdaee.supabase.co` | URL project Supabase | Có |
| `SUPABASE_ANON_KEY` | `eyJhbGci...` (JWT dài) | Supabase anonymous/public key | Có |
| `GEMINI_API_KEY` | `AIzaSyB...` | Google Gemini API key | Có |
| `sdk.dir` | `C:\\Users\\...\\Android\\Sdk` | Android SDK path | Có (tự động) |

### 3.4 Cách Lấy SUPABASE_ANON_KEY

1. Đăng nhập vào [Supabase Dashboard](https://supabase.com/dashboard)
2. Chọn project **pumiah-finance-ai-manager-kotlin**
3. Vào **Settings** → **API**
4. Copy **Project API keys** → **anon (public)**

### 3.5 Cách Lấy GEMINI_API_KEY

1. Truy cập [Google AI Studio](https://aistudio.google.com/app/apikey)
2. Click **Create API Key**
3. Chọn project Google Cloud
4. Copy API key

---

## 4. Cấu Trúc Build Configuration

### 4.1 Cách API Keys được đưa vào BuildConfig

```kotlin
// app/build.gradle.kts
import java.util.Properties

val localProperties = Properties()
val localPropertiesFile = rootProject.file("local.properties")
if (localPropertiesFile.exists()) {
    localProperties.load(localPropertiesFile.inputStream())
}

android {
    defaultConfig {
        // BuildConfig fields từ local.properties
        buildConfigField(
            "String",
            "SUPABASE_URL",
            "\"${localProperties["SUPABASE_URL"] ?: ""}\""
        )
        buildConfigField(
            "String",
            "SUPABASE_ANON_KEY",
            "\"${localProperties["SUPABASE_ANON_KEY"] ?: ""}\""
        )
        buildConfigField(
            "String",
            "GEMINI_API_KEY",
            "\"${localProperties["GEMINI_API_KEY"] ?: ""}\""
        )
    }
    buildFeatures {
        buildConfig = true
    }
}
```

### 4.2 Sử Dụng Trong Code

```kotlin
// SupabaseClient.kt
val supabase = createSupabaseClient(
    supabaseUrl = BuildConfig.SUPABASE_URL,
    supabaseKey = BuildConfig.SUPABASE_ANON_KEY
) { ... }

// GeminiService.kt
private val apiKey = BuildConfig.GEMINI_API_KEY
```

---

## 5. Cấu Trúc Thư Mục Chi Tiết (Tree)

```
pumiah-finance-ai-manager-kotlin/
│
├── 📁 app/
│   ├── 📁 src/
│   │   ├── 📁 main/
│   │   │   ├── 📁 java/com/example/myapplication/
│   │   │   │   ├── 📁 data/
│   │   │   │   │   ├── 📁 model/
│   │   │   │   │   │   ├── User.kt
│   │   │   │   │   │   ├── Transaction.kt
│   │   │   │   │   │   ├── Category.kt
│   │   │   │   │   │   ├── Budget.kt
│   │   │   │   │   │   ├── Goal.kt
│   │   │   │   │   │   └── Message.kt
│   │   │   │   │   ├── 📁 repository/
│   │   │   │   │   │   ├── AuthRepository.kt (interface + impl)
│   │   │   │   │   │   ├── TransactionRepository.kt
│   │   │   │   │   │   ├── CategoryRepository.kt
│   │   │   │   │   │   ├── BudgetRepository.kt
│   │   │   │   │   │   └── GoalRepository.kt
│   │   │   │   │   └── 📁 remote/
│   │   │   │   │       ├── SupabaseClientProvider.kt
│   │   │   │   │       └── GeminiService.kt
│   │   │   │   ├── 📁 di/
│   │   │   │   │   └── AppModule.kt
│   │   │   │   ├── 📁 ui/
│   │   │   │   │   ├── 📁 auth/
│   │   │   │   │   │   ├── LoginScreen.kt
│   │   │   │   │   │   ├── RegisterScreen.kt
│   │   │   │   │   │   └── AuthViewModel.kt
│   │   │   │   │   ├── 📁 dashboard/
│   │   │   │   │   │   ├── DashboardScreen.kt
│   │   │   │   │   │   └── DashboardViewModel.kt
│   │   │   │   │   ├── 📁 transaction/
│   │   │   │   │   │   ├── TransactionListScreen.kt
│   │   │   │   │   │   ├── TransactionFormDialog.kt
│   │   │   │   │   │   └── TransactionViewModel.kt
│   │   │   │   │   ├── 📁 category/
│   │   │   │   │   │   ├── CategoryScreen.kt
│   │   │   │   │   │   ├── CategoryFormDialog.kt
│   │   │   │   │   │   └── CategoryViewModel.kt
│   │   │   │   │   ├── 📁 budget/
│   │   │   │   │   │   ├── BudgetScreen.kt
│   │   │   │   │   │   ├── BudgetItem.kt
│   │   │   │   │   │   └── BudgetViewModel.kt
│   │   │   │   │   ├── 📁 goal/
│   │   │   │   │   │   ├── GoalScreen.kt
│   │   │   │   │   │   ├── ContributionDialog.kt
│   │   │   │   │   │   └── GoalViewModel.kt
│   │   │   │   │   ├── 📁 chat/
│   │   │   │   │   │   ├── ChatScreen.kt
│   │   │   │   │   │   ├── ChatBubble.kt
│   │   │   │   │   │   └── ChatViewModel.kt
│   │   │   │   │   ├── 📁 profile/
│   │   │   │   │   │   ├── ProfileScreen.kt
│   │   │   │   │   │   └── ProfileViewModel.kt
│   │   │   │   │   ├── 📁 navigation/
│   │   │   │   │   │   ├── AppNavigation.kt
│   │   │   │   │   │   └── Screen.kt
│   │   │   │   │   └── 📁 theme/
│   │   │   │   │       ├── Color.kt
│   │   │   │   │       ├── Type.kt
│   │   │   │   │       └── Theme.kt
│   │   │   │   ├── MainActivity.kt
│   │   │   │   └── PumiahApplication.kt
│   │   │   ├── 📁 res/
│   │   │   │   ├── 📁 drawable/
│   │   │   │   ├── 📁 values/
│   │   │   │   │   ├── strings.xml
│   │   │   │   │   └── themes.xml
│   │   │   │   └── 📁 xml/
│   │   │   │       └── network_security_config.xml
│   │   │   └── AndroidManifest.xml
│   │   ├── 📁 test/
│   │   │   └── (Unit tests)
│   │   └── 📁 androidTest/
│   │       └── (Instrumented tests)
│   └── build.gradle.kts
│
├── 📁 gradle/
│   ├── 📁 wrapper/
│   │   ├── gradle-wrapper.jar
│   │   └── gradle-wrapper.properties
│   └── libs.versions.toml
│
├── 📁 docs/                    ← Bạn đang ở đây
│   ├── README.md
│   ├── 01-tong-quan.md
│   ├── 02-kien-truc.md
│   ├── 03-cai-dat.md
│   └── ...
│
├── build.gradle.kts
├── settings.gradle.kts
├── gradle.properties
├── local.properties            ← KHÔNG commit (trong .gitignore)
├── local.properties.example    ← Template an toàn để commit
└── .gitignore
```

---

## 6. Build Commands

### 6.1 Qua Android Studio (Khuyến nghị)

1. Mở Android Studio
2. **File** → **Open** → chọn thư mục `pumiah-finance-ai-manager-kotlin`
3. Chờ Gradle sync hoàn tất (có thể mất vài phút lần đầu)
4. Kết nối thiết bị Android hoặc khởi động Emulator
5. Click nút **Run** (▶) hoặc `Shift+F10`

### 6.2 Qua Command Line

```bash
# Sync Gradle dependencies
./gradlew --refresh-dependencies

# Build debug APK
./gradlew assembleDebug

# Build release APK (cần signing config)
./gradlew assembleRelease

# Cài lên thiết bị kết nối
./gradlew installDebug

# Chạy unit tests
./gradlew test

# Chạy instrumented tests
./gradlew connectedAndroidTest

# Clean build
./gradlew clean

# Check lint
./gradlew lint

# Full build with tests
./gradlew clean assembleDebug test
```

### 6.3 Output APK Location

```
app/build/outputs/apk/debug/app-debug.apk
app/build/outputs/apk/release/app-release.apk
```

---

## 7. Thiết Lập Supabase Database

### 7.1 Chạy Migration Scripts

Sau khi có Supabase project, chạy các SQL scripts trong Supabase SQL Editor:

```bash
# Thứ tự chạy:
# 1. Tạo bảng và RLS policies
# 2. Seed dữ liệu danh mục mặc định
# 3. Tạo trigger handle_new_user
```

Xem chi tiết trong [04-database.md](04-database.md).

### 7.2 Enable Row Level Security

```sql
-- Bật RLS cho tất cả tables
ALTER TABLE public.users ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.transactions ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.categories ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.budgets ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.goals ENABLE ROW LEVEL SECURITY;
```

---

## 8. Troubleshooting Thường Gặp

### 8.1 Lỗi "SDK not found"

```
Error: SDK location not found.
Define location with an ANDROID_SDK_ROOT environment variable
or by setting the sdk.dir path in your project's local properties file
```

**Giải pháp:** Thêm `sdk.dir` vào `local.properties`:
```properties
sdk.dir=C\:\\Users\\YourName\\AppData\\Local\\Android\\Sdk
```

### 8.2 Lỗi Gradle Sync

```
Could not resolve io.github.jan-tennert.supabase:postgrest-kt:3.1.4
```

**Giải pháp:** Kiểm tra kết nối internet, thử:
```bash
./gradlew --refresh-dependencies
```

### 8.3 Lỗi "BuildConfig.SUPABASE_ANON_KEY is empty"

**Giải pháp:** Kiểm tra `local.properties` đã có đủ keys, rebuild project:
```bash
./gradlew clean assembleDebug
```

### 8.4 Lỗi Network (Supabase không kết nối được)

Kiểm tra `AndroidManifest.xml` có permission:
```xml
<uses-permission android:name="android.permission.INTERNET" />
```

### 8.5 Lỗi JDK Version

```
Unsupported class file major version XX
```

**Giải pháp:** Đảm bảo Android Studio dùng JDK 17:
- **File** → **Project Structure** → **SDK Location** → **JDK Location**

---

## 9. Cấu Hình Emulator Khuyến Nghị

| Thông số | Giá trị khuyến nghị |
|---|---|
| Device | Pixel 7 |
| System Image | Android 14 (API 34) x86_64 |
| RAM | 4096 MB |
| Storage | 8 GB |
| Play Store | Enabled |

---

*Tài liệu này được tạo bởi Kỹ Sư DevOps - 2026-03-29*
