# 05 - Tài Liệu API Reference

> **Vai trò:** Người Viết Kỹ Thuật
> **Timestamp:** 2026-03-29
> **Phiên bản tài liệu:** 1.0.0

---

## 1. Tổng Quan API

Ứng dụng Pumiah Finance AI Manager tích hợp hai nguồn API bên ngoài:

1. **Supabase API** - Quản lý authentication, database CRUD thông qua Supabase Kotlin SDK
2. **Google Gemini API** - AI chatbot tư vấn tài chính thông qua HTTP REST

```
┌─────────────────────────────────────────────────┐
│              Android App                        │
│                                                 │
│  ┌──────────────┐    ┌────────────────────────┐ │
│  │ Supabase SDK │    │ HttpURLConnection       │ │
│  └──────┬───────┘    └──────────┬─────────────┘ │
└─────────┼───────────────────────┼───────────────┘
          │                       │
          ▼                       ▼
┌──────────────────┐    ┌─────────────────────────┐
│  Supabase Cloud  │    │  Google Gemini API       │
│  (PostgreSQL)    │    │  (generativelanguage.    │
│  Auth + PostgREST│    │   googleapis.com)        │
└──────────────────┘    └─────────────────────────┘
```

---

## 2. Supabase API

### 2.1 Cấu Hình Base

```kotlin
// SupabaseClientProvider.kt
val supabase = createSupabaseClient(
    supabaseUrl = "https://nlfsyjqkcgswdatpdaee.supabase.co",
    supabaseKey = BuildConfig.SUPABASE_ANON_KEY
) {
    install(Auth) {
        scheme = "app"
        host = "pumiah.finance"
    }
    install(Postgrest) {
        defaultSchema = "public"
    }
}
```

**Base URL:** `https://nlfsyjqkcgswdatpdaee.supabase.co`
**Auth Header:** `Authorization: Bearer <access_token>` (tự động quản lý bởi SDK)

---

### 2.2 Supabase Auth API

#### 2.2.1 Đăng Nhập

```kotlin
// AuthRepository.kt
suspend fun signIn(email: String, password: String): Result<UserSession> {
    return try {
        supabase.auth.signInWith(Email) {
            this.email = email
            this.password = password
        }
        Result.success(supabase.auth.currentSessionOrNull()!!)
    } catch (e: Exception) {
        Result.failure(e)
    }
}
```

**SDK Call:** `supabase.auth.signInWith(Email) { email = ...; password = ... }`

**Tương đương HTTP Request:**
```http
POST https://nlfsyjqkcgswdatpdaee.supabase.co/auth/v1/token?grant_type=password
Content-Type: application/json
apikey: <SUPABASE_ANON_KEY>

{
  "email": "user@example.com",
  "password": "securepassword"
}
```

**Response thành công (200):**
```json
{
  "access_token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "token_type": "bearer",
  "expires_in": 3600,
  "refresh_token": "refresh_token_here",
  "user": {
    "id": "uuid-here",
    "email": "user@example.com",
    "created_at": "2026-01-01T00:00:00Z"
  }
}
```

#### 2.2.2 Đăng Ký

```kotlin
suspend fun signUp(
    email: String,
    password: String,
    fullName: String
): Result<Unit> {
    return try {
        supabase.auth.signUpWith(Email) {
            this.email = email
            this.password = password
            data = buildJsonObject {
                put("full_name", fullName)
            }
        }
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }
}
```

**Tương đương HTTP Request:**
```http
POST https://nlfsyjqkcgswdatpdaee.supabase.co/auth/v1/signup
Content-Type: application/json
apikey: <SUPABASE_ANON_KEY>

{
  "email": "newuser@example.com",
  "password": "securepassword",
  "data": {
    "full_name": "Nguyễn Văn A"
  }
}
```

#### 2.2.3 Đăng Xuất

```kotlin
suspend fun signOut() {
    supabase.auth.signOut()
}
```

**Tương đương HTTP:**
```http
POST https://nlfsyjqkcgswdatpdaee.supabase.co/auth/v1/logout
Authorization: Bearer <access_token>
apikey: <SUPABASE_ANON_KEY>
```

#### 2.2.4 Kiểm Tra Session Hiện Tại

```kotlin
fun getCurrentUser(): User? {
    return supabase.auth.currentUserOrNull()
}

fun isLoggedIn(): Boolean {
    return supabase.auth.currentSessionOrNull() != null
}
```

---

### 2.3 PostgREST CRUD API

Supabase SDK bọc PostgREST để cung cấp type-safe database access.

#### 2.3.1 Transactions API

**Lấy tất cả giao dịch:**
```kotlin
suspend fun getTransactions(): List<Transaction> {
    return supabase.postgrest["transactions"]
        .select {
            order("date", ascending = false)
            order("created_at", ascending = false)
        }
        .decodeList<Transaction>()
}
```

**Tương đương HTTP:**
```http
GET https://nlfsyjqkcgswdatpdaee.supabase.co/rest/v1/transactions
    ?order=date.desc,created_at.desc
    &select=*
Authorization: Bearer <token>
apikey: <anon_key>
```

**Tạo giao dịch mới:**
```kotlin
suspend fun createTransaction(transaction: Transaction): Transaction {
    return supabase.postgrest["transactions"]
        .insert(transaction)
        .decodeSingle<Transaction>()
}
```

**Tương đương HTTP:**
```http
POST https://nlfsyjqkcgswdatpdaee.supabase.co/rest/v1/transactions
Authorization: Bearer <token>
apikey: <anon_key>
Content-Type: application/json
Prefer: return=representation

{
  "user_id": "uuid-here",
  "category_id": "category-uuid",
  "amount": 150000.00,
  "type": "expense",
  "description": "Ăn trưa",
  "date": "2026-03-29"
}
```

**Cập nhật giao dịch:**
```kotlin
suspend fun updateTransaction(transaction: Transaction) {
    supabase.postgrest["transactions"]
        .update(transaction) {
            filter { eq("id", transaction.id) }
        }
}
```

**Xóa giao dịch:**
```kotlin
suspend fun deleteTransaction(id: String) {
    supabase.postgrest["transactions"]
        .delete {
            filter { eq("id", id) }
        }
}
```

#### 2.3.2 Categories API

```kotlin
// Lấy danh mục (cả default lẫn của user)
suspend fun getCategories(): List<Category> {
    return supabase.postgrest["categories"]
        .select {
            // RLS tự động lọc: user_id IS NULL OR user_id = current_user
            order("type", ascending = true)
            order("name", ascending = true)
        }
        .decodeList<Category>()
}

// Tạo danh mục mới
suspend fun createCategory(category: Category): Category {
    return supabase.postgrest["categories"]
        .insert(category)
        .decodeSingle<Category>()
}

// Xóa danh mục
suspend fun deleteCategory(id: String) {
    supabase.postgrest["categories"]
        .delete {
            filter { eq("id", id) }
        }
}
```

#### 2.3.3 Budgets API

```kotlin
suspend fun getBudgets(): List<Budget> {
    return supabase.postgrest["budgets"]
        .select {
            order("created_at", ascending = false)
        }
        .decodeList<Budget>()
}

suspend fun createBudget(budget: Budget): Budget {
    return supabase.postgrest["budgets"]
        .insert(budget)
        .decodeSingle<Budget>()
}

suspend fun updateBudget(budget: Budget) {
    supabase.postgrest["budgets"]
        .update(budget) {
            filter { eq("id", budget.id) }
        }
}

suspend fun deleteBudget(id: String) {
    supabase.postgrest["budgets"]
        .delete {
            filter { eq("id", id) }
        }
}
```

#### 2.3.4 Goals API

```kotlin
suspend fun getGoals(): List<Goal> {
    return supabase.postgrest["goals"]
        .select()
        .decodeList<Goal>()
}

// Nạp tiền vào mục tiêu
suspend fun contributeToGoal(goalId: String, amount: Double) {
    val goal = supabase.postgrest["goals"]
        .select { filter { eq("id", goalId) } }
        .decodeSingle<Goal>()

    val newAmount = goal.currentAmount + amount
    supabase.postgrest["goals"]
        .update({ set("current_amount", newAmount) }) {
            filter { eq("id", goalId) }
        }
}
```

#### 2.3.5 User Profile API

```kotlin
suspend fun getUserProfile(userId: String): UserProfile {
    return supabase.postgrest["users"]
        .select { filter { eq("id", userId) } }
        .decodeSingle<UserProfile>()
}

suspend fun updateProfile(profile: UserProfile) {
    supabase.postgrest["users"]
        .update(profile) {
            filter { eq("id", profile.id) }
        }
}
```

---

## 3. Gemini AI API

### 3.1 Thông Tin Endpoint

| Thuộc tính | Giá trị |
|---|---|
| Base URL | `https://generativelanguage.googleapis.com` |
| API Version | `v1beta` |
| Model | `gemini-2.5-flash` |
| Method | `POST` |
| Auth | API Key trong query parameter |

**Full Endpoint:**
```
POST https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key={GEMINI_API_KEY}
```

### 3.2 Request Format

```kotlin
// GeminiService.kt
private fun buildRequestBody(
    userMessage: String,
    context: String
): String {
    val fullPrompt = """
        Bạn là trợ lý tài chính thông minh của ứng dụng Pumiah Finance.
        Dữ liệu tài chính của người dùng:
        $context

        Câu hỏi của người dùng: $userMessage

        Hãy trả lời bằng tiếng Việt, ngắn gọn và hữu ích.
    """.trimIndent()

    return """
        {
            "contents": [
                {
                    "role": "user",
                    "parts": [
                        {
                            "text": ${JSONObject.quote(fullPrompt)}
                        }
                    ]
                }
            ],
            "generationConfig": {
                "temperature": 0.7,
                "topK": 40,
                "topP": 0.95,
                "maxOutputTokens": 1024,
                "stopSequences": []
            },
            "safetySettings": [
                {
                    "category": "HARM_CATEGORY_HARASSMENT",
                    "threshold": "BLOCK_MEDIUM_AND_ABOVE"
                },
                {
                    "category": "HARM_CATEGORY_HATE_SPEECH",
                    "threshold": "BLOCK_MEDIUM_AND_ABOVE"
                }
            ]
        }
    """.trimIndent()
}
```

**HTTP Request:**
```http
POST https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=AIzaSy...
Content-Type: application/json

{
  "contents": [
    {
      "role": "user",
      "parts": [
        {
          "text": "Tháng này tôi chi tiêu nhiều nhất vào danh mục gì?"
        }
      ]
    }
  ],
  "generationConfig": {
    "temperature": 0.7,
    "maxOutputTokens": 1024
  }
}
```

### 3.3 Response Format

**Response thành công (200):**
```json
{
  "candidates": [
    {
      "content": {
        "parts": [
          {
            "text": "Dựa trên dữ liệu của bạn, tháng này bạn chi tiêu nhiều nhất vào danh mục **Ăn uống** với tổng số tiền là 2,500,000 VND, chiếm 35% tổng chi tiêu..."
          }
        ],
        "role": "model"
      },
      "finishReason": "STOP",
      "index": 0,
      "safetyRatings": [
        {
          "category": "HARM_CATEGORY_SEXUALLY_EXPLICIT",
          "probability": "NEGLIGIBLE"
        }
      ]
    }
  ],
  "usageMetadata": {
    "promptTokenCount": 245,
    "candidatesTokenCount": 187,
    "totalTokenCount": 432
  }
}
```

### 3.4 Parse Response

```kotlin
private fun parseGeminiResponse(jsonResponse: String): String {
    return try {
        val json = JSONObject(jsonResponse)
        val candidates = json.getJSONArray("candidates")
        if (candidates.length() > 0) {
            val content = candidates
                .getJSONObject(0)
                .getJSONObject("content")
            val parts = content.getJSONArray("parts")
            if (parts.length() > 0) {
                parts.getJSONObject(0).getString("text")
            } else {
                "Xin lỗi, tôi không thể trả lời lúc này."
            }
        } else {
            "Xin lỗi, không có phản hồi từ AI."
        }
    } catch (e: JSONException) {
        "Lỗi xử lý phản hồi: ${e.message}"
    }
}
```

### 3.5 Context Building cho Chat

```kotlin
// ChatViewModel.kt
private fun buildFinancialContext(
    transactions: List<Transaction>,
    budgets: List<Budget>,
    goals: List<Goal>
): String {
    val totalIncome = transactions
        .filter { it.type == "income" }
        .sumOf { it.amount }
    val totalExpense = transactions
        .filter { it.type == "expense" }
        .sumOf { it.amount }

    return buildString {
        appendLine("=== DỮ LIỆU TÀI CHÍNH ===")
        appendLine("Tổng thu nhập tháng này: ${formatCurrency(totalIncome)} VND")
        appendLine("Tổng chi tiêu tháng này: ${formatCurrency(totalExpense)} VND")
        appendLine("Số dư: ${formatCurrency(totalIncome - totalExpense)} VND")
        appendLine()
        appendLine("Top 5 giao dịch gần đây:")
        transactions.take(5).forEach { t ->
            appendLine("- ${t.description ?: "N/A"}: ${t.amount} VND (${t.type})")
        }
        appendLine()
        if (budgets.isNotEmpty()) {
            appendLine("Ngân sách đang theo dõi: ${budgets.size} ngân sách")
        }
        if (goals.isNotEmpty()) {
            appendLine("Mục tiêu tài chính: ${goals.size} mục tiêu")
        }
    }
}
```

---

## 4. Mã Lỗi và Xử Lý

### 4.1 Supabase Error Codes

| Mã lỗi | Ý nghĩa | Xử lý trong app |
|---|---|---|
| `invalid_credentials` | Email/password sai | Hiển thị "Sai email hoặc mật khẩu" |
| `email_already_registered` | Email đã tồn tại | Hiển thị "Email đã được sử dụng" |
| `weak_password` | Password quá yếu | Hiển thị yêu cầu mật khẩu |
| `JWT expired` | Session hết hạn | Tự động refresh hoặc đăng xuất |
| `PGRST116` | Row not found | Xử lý null case |
| `23505` | Unique constraint violation | Hiển thị lỗi duplicate |
| `42501` | RLS policy violation | Không được phép (không hiển thị chi tiết) |

### 4.2 Gemini Error Codes

| HTTP Code | Ý nghĩa | Xử lý |
|---|---|---|
| `400` | Bad Request (invalid JSON) | Log và retry |
| `401` | API key không hợp lệ | Hiển thị cấu hình lỗi |
| `429` | Rate limit exceeded | Hiển thị "Thử lại sau" |
| `500` | Internal Server Error | Hiển thị thông báo lỗi chung |
| `503` | Service Unavailable | Hiển thị "AI tạm thời không khả dụng" |

### 4.3 Pattern Xử Lý Lỗi Chung

```kotlin
// Trong ViewModel
private fun handleError(e: Exception): String {
    return when (e) {
        is RestException -> when (e.error) {
            "invalid_credentials" -> "Sai email hoặc mật khẩu"
            "email_already_registered" -> "Email này đã được đăng ký"
            else -> "Lỗi server: ${e.message}"
        }
        is HttpRequestException -> "Lỗi kết nối mạng. Vui lòng kiểm tra internet."
        is SerializationException -> "Lỗi dữ liệu. Vui lòng thử lại."
        else -> e.message ?: "Đã xảy ra lỗi không xác định"
    }
}
```

### 4.4 Network Security Configuration

```xml
<!-- res/xml/network_security_config.xml -->
<?xml version="1.0" encoding="utf-8"?>
<network-security-config>
    <domain-config cleartextTrafficPermitted="false">
        <domain includeSubdomains="true">supabase.co</domain>
        <domain includeSubdomains="true">googleapis.com</domain>
    </domain-config>
</network-security-config>
```

---

## 5. Rate Limits và Quotas

### 5.1 Supabase Free Tier

| Resource | Giới hạn |
|---|---|
| Database | 500 MB |
| Auth users | 50,000 users |
| API requests | 500,000 requests/tháng |
| Bandwidth | 5 GB/tháng |

### 5.2 Gemini API Free Tier

| Metric | Giới hạn |
|---|---|
| Requests per minute | 15 RPM |
| Requests per day | 1,500 RPD |
| Tokens per minute | 1,000,000 TPM |

---

*Tài liệu này được tạo bởi Người Viết Kỹ Thuật - 2026-03-29*
