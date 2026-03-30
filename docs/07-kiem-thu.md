# 07 - Kế Hoạch Kiểm Thử (Test Plan)

> **Vai trò:** Kiến Trúc Sư QA
> **Timestamp:** 2026-03-29
> **Phiên bản tài liệu:** 1.0.0

---

## 1. Chiến Lược Kiểm Thử Tổng Thể

Ứng dụng áp dụng **Testing Pyramid** với 3 tầng:

```
         /\
        /  \
       / E2E \        ← Ít nhất (tốn kém, chậm)
      /  Tests \
     /──────────\
    /Integration \   ← Vừa phải
   /   Tests      \
  /────────────────\
 /   Unit Tests     \  ← Nhiều nhất (nhanh, rẻ)
/────────────────────\
```

| Tầng | Công cụ | Coverage mục tiêu | Chạy khi nào |
|---|---|---|---|
| Unit Tests | JUnit 4/5, Mockito, Turbine | 80%+ | Mỗi commit |
| Integration Tests | JUnit 4 + Supabase Test | 60%+ | Mỗi PR |
| E2E Tests | Espresso / UI Automator | Critical paths | Trước release |

---

## 2. Unit Tests

### 2.1 Setup Test Dependencies

```kotlin
// app/build.gradle.kts
dependencies {
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.mockito.kotlin:mockito-kotlin:5.2.1")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.10.1")
    testImplementation("app.cash.turbine:turbine:1.2.0")  // Test StateFlow/Flow
    testImplementation("com.google.truth:truth:1.4.2")    // Fluent assertions
    testImplementation("io.mockk:mockk:1.13.9")           // Mock Kotlin objects
}
```

### 2.2 AuthViewModel Unit Tests

```kotlin
// test/AuthViewModelTest.kt
@OptIn(ExperimentalCoroutinesApi::class)
class AuthViewModelTest {

    // Kotlin Coroutines Test Rule
    @get:Rule
    val coroutineRule = CoroutineTestRule()

    // Mock dependencies
    private val mockAuthRepository = mockk<AuthRepository>()
    private lateinit var viewModel: AuthViewModel

    @Before
    fun setup() {
        viewModel = AuthViewModel(mockAuthRepository)
    }

    // ✅ Test: Đăng nhập thành công
    @Test
    fun `signIn with valid credentials emits Authenticated state`() = runTest {
        // Arrange
        val email = "test@example.com"
        val password = "password123"
        coEvery { mockAuthRepository.signIn(email, password) } returns Result.success(Unit)

        // Act + Assert (dùng Turbine để test StateFlow)
        viewModel.authState.test {
            viewModel.signIn(email, password)

            // Initial state
            assertThat(awaitItem()).isInstanceOf(AuthState.Unauthenticated::class.java)

            // Loading state
            val loadingState = awaitItem()
            assertThat(loadingState).isInstanceOf(AuthState.Loading::class.java)

            // Authenticated state
            val authState = awaitItem()
            assertThat(authState).isInstanceOf(AuthState.Authenticated::class.java)

            cancelAndConsumeRemainingEvents()
        }
    }

    // ❌ Test: Đăng nhập thất bại - sai mật khẩu
    @Test
    fun `signIn with invalid credentials emits error state`() = runTest {
        // Arrange
        val email = "test@example.com"
        val wrongPassword = "wrongpassword"
        val errorMessage = "Sai email hoặc mật khẩu"
        coEvery {
            mockAuthRepository.signIn(email, wrongPassword)
        } returns Result.failure(Exception(errorMessage))

        // Act + Assert
        viewModel.uiState.test {
            // Skip initial state
            skipItems(1)

            viewModel.signIn(email, wrongPassword)

            // Loading
            val loadingState = awaitItem()
            assertThat(loadingState.isLoading).isTrue()

            // Error state
            val errorState = awaitItem()
            assertThat(errorState.isLoading).isFalse()
            assertThat(errorState.error).contains("Sai email")

            cancelAndConsumeRemainingEvents()
        }
    }

    // ✅ Test: Đăng ký tài khoản mới thành công
    @Test
    fun `signUp with valid data succeeds`() = runTest {
        // Arrange
        coEvery {
            mockAuthRepository.signUp(any(), any(), any())
        } returns Result.success(Unit)

        // Act + Assert
        viewModel.uiState.test {
            skipItems(1)
            viewModel.signUp("new@example.com", "password123", "Nguyễn Văn A")

            val loadingState = awaitItem()
            assertThat(loadingState.isLoading).isTrue()

            val successState = awaitItem()
            assertThat(successState.isLoading).isFalse()
            assertThat(successState.error).isNull()

            cancelAndConsumeRemainingEvents()
        }
    }

    // Test: Validation - email rỗng
    @Test
    fun `signIn with empty email does not call repository`() = runTest {
        viewModel.signIn("", "password123")

        coVerify(exactly = 0) { mockAuthRepository.signIn(any(), any()) }
    }
}
```

### 2.3 TransactionViewModel Unit Tests

```kotlin
@OptIn(ExperimentalCoroutinesApi::class)
class TransactionViewModelTest {

    @get:Rule
    val coroutineRule = CoroutineTestRule()

    private val mockTransactionRepo = mockk<TransactionRepository>()
    private val mockCategoryRepo = mockk<CategoryRepository>()
    private lateinit var viewModel: TransactionViewModel

    private val testTransactions = listOf(
        Transaction(
            id = "1",
            userId = "user1",
            amount = 50000.0,
            type = "expense",
            description = "Ăn sáng",
            date = "2026-03-29"
        ),
        Transaction(
            id = "2",
            userId = "user1",
            amount = 5000000.0,
            type = "income",
            description = "Lương tháng 3",
            date = "2026-03-28"
        )
    )

    @Before
    fun setup() {
        coEvery { mockTransactionRepo.getTransactions() } returns testTransactions
        coEvery { mockCategoryRepo.getCategories() } returns emptyList()
        viewModel = TransactionViewModel(mockTransactionRepo, mockCategoryRepo)
    }

    // ✅ Test: Load giao dịch khi khởi tạo
    @Test
    fun `init loads transactions successfully`() = runTest {
        viewModel.uiState.test {
            val state = awaitItem()
            assertThat(state.transactions).hasSize(2)
            assertThat(state.isLoading).isFalse()
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ✅ Test: Tạo giao dịch mới
    @Test
    fun `createTransaction adds transaction to list`() = runTest {
        val newTransaction = Transaction(
            id = "3",
            userId = "user1",
            amount = 30000.0,
            type = "expense",
            description = "Coffee",
            date = "2026-03-29"
        )

        coEvery {
            mockTransactionRepo.createTransaction(any())
        } returns newTransaction

        coEvery {
            mockTransactionRepo.getTransactions()
        } returns testTransactions + newTransaction

        viewModel.uiState.test {
            skipItems(1) // Skip initial loaded state

            viewModel.createTransaction(newTransaction)

            val loadingState = awaitItem()
            assertThat(loadingState.isLoading).isTrue()

            val updatedState = awaitItem()
            assertThat(updatedState.transactions).hasSize(3)
            assertThat(updatedState.isLoading).isFalse()

            cancelAndIgnoreRemainingEvents()
        }
    }

    // ✅ Test: Xóa giao dịch
    @Test
    fun `deleteTransaction removes transaction from list`() = runTest {
        coEvery { mockTransactionRepo.deleteTransaction("1") } just Runs
        coEvery {
            mockTransactionRepo.getTransactions()
        } returns testTransactions.filter { it.id != "1" }

        viewModel.uiState.test {
            skipItems(1)

            viewModel.deleteTransaction("1")

            skipItems(1) // loading state

            val updatedState = awaitItem()
            assertThat(updatedState.transactions).hasSize(1)
            assertThat(updatedState.transactions.none { it.id == "1" }).isTrue()

            cancelAndIgnoreRemainingEvents()
        }
    }

    // Test: Filter giao dịch theo loại
    @Test
    fun `transactions are sorted by date descending`() = runTest {
        viewModel.uiState.test {
            val state = awaitItem()
            val dates = state.transactions.map { it.date }
            assertThat(dates).isInOrder(Comparator.reverseOrder())
            cancelAndIgnoreRemainingEvents()
        }
    }
}
```

### 2.4 ChatViewModel Unit Tests

```kotlin
@OptIn(ExperimentalCoroutinesApi::class)
class ChatViewModelTest {

    @get:Rule
    val coroutineRule = CoroutineTestRule()

    private val mockGeminiService = mockk<GeminiService>()
    private val mockTransactionRepo = mockk<TransactionRepository>()
    private lateinit var viewModel: ChatViewModel

    @Before
    fun setup() {
        coEvery { mockTransactionRepo.getTransactions() } returns emptyList()
        viewModel = ChatViewModel(mockGeminiService, mockTransactionRepo)
    }

    // ✅ Test: Gửi tin nhắn và nhận response
    @Test
    fun `sendMessage adds user message and AI response`() = runTest {
        val userMessage = "Tháng này tôi chi tiêu bao nhiêu?"
        val aiResponse = "Tháng này bạn chi tiêu tổng cộng 500,000 VND."

        coEvery {
            mockGeminiService.generateResponse(any())
        } returns aiResponse

        viewModel.uiState.test {
            skipItems(1) // initial state

            viewModel.sendMessage(userMessage)

            // User message added + isTyping = true
            val typingState = awaitItem()
            assertThat(typingState.messages).hasSize(1)
            assertThat(typingState.messages[0].content).isEqualTo(userMessage)
            assertThat(typingState.messages[0].isFromUser).isTrue()
            assertThat(typingState.isTyping).isTrue()

            // AI response received + isTyping = false
            val responseState = awaitItem()
            assertThat(responseState.messages).hasSize(2)
            assertThat(responseState.messages[1].content).isEqualTo(aiResponse)
            assertThat(responseState.messages[1].isFromUser).isFalse()
            assertThat(responseState.isTyping).isFalse()

            cancelAndIgnoreRemainingEvents()
        }
    }

    // ✅ Test: buildContext bao gồm dữ liệu tài chính
    @Test
    fun `buildContext includes financial summary`() = runTest {
        val transactions = listOf(
            Transaction(id = "1", userId = "u", amount = 1000000.0, type = "income", date = "2026-03-01"),
            Transaction(id = "2", userId = "u", amount = 200000.0, type = "expense", date = "2026-03-02")
        )
        coEvery { mockTransactionRepo.getTransactions() } returns transactions

        // Reinitialize để load transactions
        viewModel = ChatViewModel(mockGeminiService, mockTransactionRepo)

        val context = viewModel.buildFinancialContext()
        assertThat(context).contains("1,000,000")
        assertThat(context).contains("200,000")
    }

    // ❌ Test: Xử lý lỗi khi Gemini API fails
    @Test
    fun `sendMessage handles API error gracefully`() = runTest {
        coEvery {
            mockGeminiService.generateResponse(any())
        } throws Exception("Network error")

        viewModel.uiState.test {
            skipItems(1)

            viewModel.sendMessage("Test message")
            skipItems(1) // user message + typing

            val errorState = awaitItem()
            assertThat(errorState.isTyping).isFalse()
            // AI message with error fallback
            assertThat(errorState.messages).hasSize(2)
            assertThat(errorState.messages[1].isFromUser).isFalse()

            cancelAndIgnoreRemainingEvents()
        }
    }
}
```

---

## 3. Integration Tests

### 3.1 Supabase Integration Tests

```kotlin
// androidTest/SupabaseIntegrationTest.kt
// QUAN TRỌNG: Chạy với test Supabase project riêng biệt!
@RunWith(AndroidJUnit4::class)
class TransactionIntegrationTest {

    private lateinit var supabase: SupabaseClient
    private lateinit var repository: TransactionRepository

    private val testUserId = "test-user-uuid" // Test user trong Supabase test project

    @Before
    fun setup() {
        supabase = createSupabaseClient(
            supabaseUrl = BuildConfig.TEST_SUPABASE_URL,
            supabaseKey = BuildConfig.TEST_SUPABASE_KEY
        ) {
            install(Auth)
            install(Postgrest)
        }
        repository = TransactionRepositoryImpl(supabase)
    }

    @Test
    fun createAndRetrieveTransaction() = runBlocking {
        // Tạo giao dịch test
        val newTransaction = Transaction(
            userId = testUserId,
            amount = 50000.0,
            type = "expense",
            description = "Integration test transaction",
            date = "2026-03-29"
        )

        val created = repository.createTransaction(newTransaction)
        assertThat(created.id).isNotEmpty()
        assertThat(created.amount).isEqualTo(50000.0)

        // Verify có thể tìm thấy trong danh sách
        val transactions = repository.getTransactions()
        assertThat(transactions.any { it.id == created.id }).isTrue()

        // Cleanup
        repository.deleteTransaction(created.id)
    }

    @Test
    fun updateTransactionAmount() = runBlocking {
        val transaction = repository.createTransaction(
            Transaction(
                userId = testUserId,
                amount = 100000.0,
                type = "income",
                date = "2026-03-29"
            )
        )

        val updated = transaction.copy(amount = 200000.0)
        repository.updateTransaction(updated)

        val fetched = repository.getTransactions().find { it.id == transaction.id }
        assertThat(fetched?.amount).isEqualTo(200000.0)

        // Cleanup
        repository.deleteTransaction(transaction.id)
    }
}
```

---

## 4. E2E User Story Tests

### 4.1 User Story: Đăng Ký và Đăng Nhập

```kotlin
@RunWith(AndroidJUnit4::class)
class AuthE2ETest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun userCanRegisterAndLogin() {
        val testEmail = "e2etest_${System.currentTimeMillis()}@test.com"
        val testPassword = "TestPass123!"

        // 1. Từ LoginScreen, click "Đăng ký ngay"
        composeTestRule.onNodeWithText("Chưa có tài khoản? Đăng ký ngay")
            .performClick()

        // 2. Điền thông tin đăng ký
        composeTestRule.onNodeWithText("Họ và tên")
            .performTextInput("Nguyễn Test")
        composeTestRule.onNodeWithText("Email")
            .performTextInput(testEmail)
        composeTestRule.onNodeWithText("Mật khẩu")
            .performTextInput(testPassword)

        // 3. Submit form
        composeTestRule.onNodeWithText("Đăng Ký")
            .performClick()

        // 4. Verify redirect về Login (hoặc Dashboard nếu auto-login)
        composeTestRule.waitUntilAtLeastOneExists(
            hasText("Đăng Nhập"), timeoutMillis = 5000
        )
    }

    @Test
    fun userCanAddTransaction() {
        // Đăng nhập trước
        loginAsTestUser()

        // Navigate to Transactions
        composeTestRule.onNodeWithContentDescription("Giao dịch")
            .performClick()

        // Click FAB
        composeTestRule.onNodeWithContentDescription("Thêm giao dịch")
            .performClick()

        // Điền form
        composeTestRule.onNodeWithText("Số tiền")
            .performTextInput("50000")
        composeTestRule.onNodeWithText("Mô tả")
            .performTextInput("Ăn trưa test")

        // Save
        composeTestRule.onNodeWithText("Lưu").performClick()

        // Verify giao dịch xuất hiện trong danh sách
        composeTestRule.onNodeWithText("Ăn trưa test")
            .assertIsDisplayed()
    }
}
```

---

## 5. CI/CD Pipeline

### 5.1 GitHub Actions Workflow

```yaml
# .github/workflows/android-ci.yml
name: Android CI/CD Pipeline

on:
  push:
    branches: [ main, develop ]
  pull_request:
    branches: [ main ]

jobs:
  # ============================
  # Job 1: Unit Tests
  # ============================
  unit-tests:
    name: Unit Tests
    runs-on: ubuntu-latest

    steps:
      - name: Checkout code
        uses: actions/checkout@v4

      - name: Set up JDK 17
        uses: actions/setup-java@v4
        with:
          java-version: '17'
          distribution: 'temurin'
          cache: gradle

      - name: Setup Android SDK
        uses: android-actions/setup-android@v3

      - name: Create local.properties
        run: |
          echo "SUPABASE_URL=${{ secrets.SUPABASE_URL }}" >> local.properties
          echo "SUPABASE_ANON_KEY=${{ secrets.SUPABASE_ANON_KEY }}" >> local.properties
          echo "GEMINI_API_KEY=${{ secrets.GEMINI_API_KEY }}" >> local.properties

      - name: Grant execute permission for gradlew
        run: chmod +x gradlew

      - name: Run Unit Tests
        run: ./gradlew test

      - name: Upload Test Reports
        uses: actions/upload-artifact@v4
        if: always()
        with:
          name: unit-test-results
          path: app/build/reports/tests/

  # ============================
  # Job 2: Lint Check
  # ============================
  lint:
    name: Lint Check
    runs-on: ubuntu-latest

    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with:
          java-version: '17'
          distribution: 'temurin'
          cache: gradle

      - name: Create local.properties
        run: |
          echo "SUPABASE_URL=${{ secrets.SUPABASE_URL }}" >> local.properties
          echo "SUPABASE_ANON_KEY=${{ secrets.SUPABASE_ANON_KEY }}" >> local.properties
          echo "GEMINI_API_KEY=${{ secrets.GEMINI_API_KEY }}" >> local.properties

      - run: chmod +x gradlew

      - name: Run Lint
        run: ./gradlew lint

      - name: Upload Lint Report
        uses: actions/upload-artifact@v4
        if: always()
        with:
          name: lint-report
          path: app/build/reports/lint-results*.html

  # ============================
  # Job 3: Build APK
  # ============================
  build:
    name: Build Debug APK
    runs-on: ubuntu-latest
    needs: [unit-tests, lint]

    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with:
          java-version: '17'
          distribution: 'temurin'
          cache: gradle

      - name: Create local.properties
        run: |
          echo "SUPABASE_URL=${{ secrets.SUPABASE_URL }}" >> local.properties
          echo "SUPABASE_ANON_KEY=${{ secrets.SUPABASE_ANON_KEY }}" >> local.properties
          echo "GEMINI_API_KEY=${{ secrets.GEMINI_API_KEY }}" >> local.properties

      - run: chmod +x gradlew

      - name: Build Debug APK
        run: ./gradlew assembleDebug

      - name: Upload Debug APK
        uses: actions/upload-artifact@v4
        with:
          name: debug-apk
          path: app/build/outputs/apk/debug/app-debug.apk

  # ============================
  # Job 4: Release Build (chỉ khi merge vào main)
  # ============================
  release:
    name: Build Release APK
    runs-on: ubuntu-latest
    needs: build
    if: github.ref == 'refs/heads/main' && github.event_name == 'push'

    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with:
          java-version: '17'
          distribution: 'temurin'
          cache: gradle

      - name: Create local.properties
        run: |
          echo "SUPABASE_URL=${{ secrets.SUPABASE_URL }}" >> local.properties
          echo "SUPABASE_ANON_KEY=${{ secrets.SUPABASE_ANON_KEY }}" >> local.properties
          echo "GEMINI_API_KEY=${{ secrets.GEMINI_API_KEY }}" >> local.properties

      - run: chmod +x gradlew

      - name: Build Release APK
        run: ./gradlew assembleRelease

      - name: Sign APK
        uses: r0adkll/sign-android-release@v1
        with:
          releaseDirectory: app/build/outputs/apk/release
          signingKeyBase64: ${{ secrets.SIGNING_KEY }}
          alias: ${{ secrets.KEY_ALIAS }}
          keyStorePassword: ${{ secrets.KEY_STORE_PASSWORD }}
          keyPassword: ${{ secrets.KEY_PASSWORD }}
```

### 5.2 GitHub Secrets Cần Thiết

| Secret | Mô tả |
|---|---|
| `SUPABASE_URL` | URL Supabase project |
| `SUPABASE_ANON_KEY` | Supabase anonymous key |
| `GEMINI_API_KEY` | Google Gemini API key |
| `SIGNING_KEY` | Base64 encoded keystore (cho release) |
| `KEY_ALIAS` | Keystore alias |
| `KEY_STORE_PASSWORD` | Keystore password |
| `KEY_PASSWORD` | Key password |

---

## 6. Coverage Targets

| Module | Mục tiêu | Hiện tại |
|---|---|---|
| AuthViewModel | 90% | Cần đo |
| TransactionViewModel | 85% | Cần đo |
| ChatViewModel | 80% | Cần đo |
| Repositories | 70% | Cần đo |
| UI Composables | 40% | Cần đo |

---

*Tài liệu này được tạo bởi Kiến Trúc Sư QA - 2026-03-29*
