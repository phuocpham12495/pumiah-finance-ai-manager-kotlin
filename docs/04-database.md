# 04 - Tài Liệu Database Schema

> **Vai trò:** Người Viết Kỹ Thuật (kết hợp DBA + Backend)
> **Timestamp:** 2026-03-29
> **Database:** PostgreSQL (Supabase)
> **Supabase Project URL:** https://nlfsyjqkcgswdatpdaee.supabase.co

---

## 1. Tổng Quan Database

Ứng dụng sử dụng **PostgreSQL** thông qua Supabase với **Row Level Security (RLS)** để đảm bảo mỗi người dùng chỉ truy cập được dữ liệu của mình. Toàn bộ schema được thiết kế với `user_id` là foreign key liên kết với Supabase Auth.

### 1.1 Sơ Đồ ER (Entity Relationship)

```
┌─────────────────────────────────────────────────────────────────┐
│                        auth.users (Supabase)                    │
│  id (UUID, PK)  │  email  │  created_at                         │
└─────────────────────────────┬───────────────────────────────────┘
                              │ 1
                              │
          ┌───────────────────┼───────────────────┐
          │                   │                   │
          │ 1:N               │ 1:N               │ 1:N
          ▼                   ▼                   ▼
┌─────────────────┐  ┌────────────────┐  ┌───────────────────┐
│  public.users   │  │ public.budgets │  │  public.goals     │
│  (profile)      │  │                │  │                   │
│  id (UUID, FK)  │  │  id (UUID, PK) │  │  id (UUID, PK)    │
│  full_name      │  │  user_id (FK)  │  │  user_id (FK)     │
│  avatar_url     │  │  category_id   │  │  name             │
│  currency       │  │  amount        │  │  target_amount    │
│  created_at     │  │  period        │  │  current_amount   │
└─────────────────┘  │  start_date    │  │  deadline         │
                     │  end_date      │  │  created_at       │
                     └────────────────┘  └───────────────────┘
                              │
          ┌───────────────────┘
          │
          ▼
┌───────────────────────────┐
│   public.categories       │
│  id (UUID, PK)            │
│  user_id (FK, nullable)   │◄── NULL = default category
│  name                     │
│  icon                     │
│  color                    │
│  type (income/expense)    │
│  created_at               │
└─────────────┬─────────────┘
              │ 1:N
              ▼
┌─────────────────────────────┐
│    public.transactions      │
│  id (UUID, PK)              │
│  user_id (FK)               │
│  category_id (FK)           │
│  amount (DECIMAL)           │
│  type (income/expense)      │
│  description                │
│  date (DATE)                │
│  created_at (TIMESTAMPTZ)   │
└─────────────────────────────┘
```

---

## 2. Chi Tiết Từng Bảng

### 2.1 Bảng `public.users` (Profile)

Bảng này lưu thông tin profile người dùng, được tạo tự động khi user đăng ký.

```sql
CREATE TABLE public.users (
    id          UUID PRIMARY KEY REFERENCES auth.users(id) ON DELETE CASCADE,
    full_name   TEXT,
    avatar_url  TEXT,
    currency    TEXT DEFAULT 'VND',
    created_at  TIMESTAMPTZ DEFAULT NOW()
);
```

| Cột | Kiểu | Constraints | Mô tả |
|---|---|---|---|
| `id` | UUID | PK, FK → auth.users | Liên kết với Supabase Auth |
| `full_name` | TEXT | NULLABLE | Họ và tên đầy đủ |
| `avatar_url` | TEXT | NULLABLE | URL ảnh đại diện |
| `currency` | TEXT | DEFAULT 'VND' | Đơn vị tiền tệ |
| `created_at` | TIMESTAMPTZ | DEFAULT NOW() | Thời gian tạo |

**Kotlin Data Class:**
```kotlin
@Serializable
data class UserProfile(
    val id: String,
    @SerialName("full_name") val fullName: String? = null,
    @SerialName("avatar_url") val avatarUrl: String? = null,
    val currency: String = "VND",
    @SerialName("created_at") val createdAt: String? = null
)
```

---

### 2.2 Bảng `public.categories`

Lưu các danh mục thu/chi. Danh mục mặc định có `user_id = NULL`, danh mục tùy chỉnh có `user_id` của người dùng.

```sql
CREATE TABLE public.categories (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     UUID REFERENCES auth.users(id) ON DELETE CASCADE,
    name        TEXT NOT NULL,
    icon        TEXT DEFAULT 'category',
    color       TEXT DEFAULT '#6750A4',
    type        TEXT NOT NULL CHECK (type IN ('income', 'expense')),
    created_at  TIMESTAMPTZ DEFAULT NOW()
);

-- Index để tăng tốc query
CREATE INDEX idx_categories_user_id ON public.categories(user_id);
CREATE INDEX idx_categories_type ON public.categories(type);
```

| Cột | Kiểu | Constraints | Mô tả |
|---|---|---|---|
| `id` | UUID | PK, DEFAULT gen_random_uuid() | ID tự động |
| `user_id` | UUID | FK → auth.users, NULLABLE | NULL = danh mục mặc định |
| `name` | TEXT | NOT NULL | Tên danh mục |
| `icon` | TEXT | DEFAULT 'category' | Tên Material Icon |
| `color` | TEXT | DEFAULT '#6750A4' | Hex color code |
| `type` | TEXT | CHECK (income/expense) | Loại: thu nhập hoặc chi tiêu |
| `created_at` | TIMESTAMPTZ | DEFAULT NOW() | Thời gian tạo |

---

### 2.3 Bảng `public.transactions`

Bảng trung tâm lưu tất cả giao dịch tài chính.

```sql
CREATE TABLE public.transactions (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    category_id UUID REFERENCES public.categories(id) ON DELETE SET NULL,
    amount      DECIMAL(15, 2) NOT NULL CHECK (amount > 0),
    type        TEXT NOT NULL CHECK (type IN ('income', 'expense')),
    description TEXT,
    date        DATE NOT NULL DEFAULT CURRENT_DATE,
    created_at  TIMESTAMPTZ DEFAULT NOW()
);

-- Indexes
CREATE INDEX idx_transactions_user_id ON public.transactions(user_id);
CREATE INDEX idx_transactions_date ON public.transactions(date DESC);
CREATE INDEX idx_transactions_type ON public.transactions(type);
CREATE INDEX idx_transactions_category ON public.transactions(category_id);
```

| Cột | Kiểu | Constraints | Mô tả |
|---|---|---|---|
| `id` | UUID | PK | ID giao dịch |
| `user_id` | UUID | NOT NULL, FK | Chủ sở hữu giao dịch |
| `category_id` | UUID | FK, NULLABLE | Danh mục (NULL nếu danh mục bị xóa) |
| `amount` | DECIMAL(15,2) | NOT NULL, > 0 | Số tiền (tối đa 15 chữ số) |
| `type` | TEXT | CHECK income/expense | Loại giao dịch |
| `description` | TEXT | NULLABLE | Mô tả tùy chọn |
| `date` | DATE | DEFAULT CURRENT_DATE | Ngày giao dịch |
| `created_at` | TIMESTAMPTZ | DEFAULT NOW() | Thời gian nhập liệu |

**Kotlin Data Class:**
```kotlin
@Serializable
data class Transaction(
    val id: String = "",
    @SerialName("user_id") val userId: String = "",
    @SerialName("category_id") val categoryId: String? = null,
    val amount: Double = 0.0,
    val type: String = "expense",
    val description: String? = null,
    val date: String = "",
    @SerialName("created_at") val createdAt: String? = null
)
```

---

### 2.4 Bảng `public.budgets`

Quản lý ngân sách theo danh mục và khoảng thời gian.

```sql
CREATE TABLE public.budgets (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    category_id UUID REFERENCES public.categories(id) ON DELETE CASCADE,
    amount      DECIMAL(15, 2) NOT NULL CHECK (amount > 0),
    period      TEXT NOT NULL CHECK (period IN ('monthly', 'weekly', 'yearly')),
    start_date  DATE NOT NULL,
    end_date    DATE,
    created_at  TIMESTAMPTZ DEFAULT NOW()
);

CREATE INDEX idx_budgets_user_id ON public.budgets(user_id);
```

| Cột | Kiểu | Constraints | Mô tả |
|---|---|---|---|
| `id` | UUID | PK | ID ngân sách |
| `user_id` | UUID | NOT NULL, FK | Chủ sở hữu |
| `category_id` | UUID | FK, NULLABLE | Danh mục áp dụng |
| `amount` | DECIMAL(15,2) | NOT NULL, > 0 | Hạn mức ngân sách |
| `period` | TEXT | CHECK monthly/weekly/yearly | Chu kỳ ngân sách |
| `start_date` | DATE | NOT NULL | Ngày bắt đầu |
| `end_date` | DATE | NULLABLE | Ngày kết thúc (null = vô hạn) |
| `created_at` | TIMESTAMPTZ | DEFAULT NOW() | Thời gian tạo |

---

### 2.5 Bảng `public.goals`

Lưu mục tiêu tiết kiệm tài chính.

```sql
CREATE TABLE public.goals (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id         UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    name            TEXT NOT NULL,
    target_amount   DECIMAL(15, 2) NOT NULL CHECK (target_amount > 0),
    current_amount  DECIMAL(15, 2) DEFAULT 0 CHECK (current_amount >= 0),
    deadline        DATE,
    icon            TEXT DEFAULT 'flag',
    color           TEXT DEFAULT '#6750A4',
    created_at      TIMESTAMPTZ DEFAULT NOW()
);

CREATE INDEX idx_goals_user_id ON public.goals(user_id);
```

| Cột | Kiểu | Constraints | Mô tả |
|---|---|---|---|
| `id` | UUID | PK | ID mục tiêu |
| `user_id` | UUID | NOT NULL, FK | Chủ sở hữu |
| `name` | TEXT | NOT NULL | Tên mục tiêu |
| `target_amount` | DECIMAL(15,2) | NOT NULL, > 0 | Số tiền mục tiêu |
| `current_amount` | DECIMAL(15,2) | DEFAULT 0, >= 0 | Số tiền đã tích lũy |
| `deadline` | DATE | NULLABLE | Hạn hoàn thành |
| `icon` | TEXT | DEFAULT 'flag' | Icon hiển thị |
| `color` | TEXT | DEFAULT '#6750A4' | Màu hiển thị |
| `created_at` | TIMESTAMPTZ | DEFAULT NOW() | Thời gian tạo |

---

## 3. Row Level Security (RLS) Policies

### 3.1 Policy cho Bảng `users`

```sql
-- Bật RLS
ALTER TABLE public.users ENABLE ROW LEVEL SECURITY;

-- Policy: User chỉ xem profile của mình
CREATE POLICY "users_select_own"
    ON public.users FOR SELECT
    USING (auth.uid() = id);

-- Policy: User chỉ cập nhật profile của mình
CREATE POLICY "users_update_own"
    ON public.users FOR UPDATE
    USING (auth.uid() = id)
    WITH CHECK (auth.uid() = id);

-- Policy: Trigger handle_new_user được phép insert (service role)
CREATE POLICY "users_insert_on_signup"
    ON public.users FOR INSERT
    WITH CHECK (auth.uid() = id);
```

### 3.2 Policy cho Bảng `categories`

```sql
ALTER TABLE public.categories ENABLE ROW LEVEL SECURITY;

-- Xem: danh mục mặc định (user_id IS NULL) HOẶC danh mục của mình
CREATE POLICY "categories_select"
    ON public.categories FOR SELECT
    USING (user_id IS NULL OR auth.uid() = user_id);

-- Thêm: chỉ danh mục của mình
CREATE POLICY "categories_insert"
    ON public.categories FOR INSERT
    WITH CHECK (auth.uid() = user_id);

-- Cập nhật: chỉ danh mục của mình
CREATE POLICY "categories_update"
    ON public.categories FOR UPDATE
    USING (auth.uid() = user_id);

-- Xóa: chỉ danh mục của mình (không xóa được default categories)
CREATE POLICY "categories_delete"
    ON public.categories FOR DELETE
    USING (auth.uid() = user_id);
```

### 3.3 Policy cho Bảng `transactions`

```sql
ALTER TABLE public.transactions ENABLE ROW LEVEL SECURITY;

CREATE POLICY "transactions_all_own"
    ON public.transactions FOR ALL
    USING (auth.uid() = user_id)
    WITH CHECK (auth.uid() = user_id);
```

### 3.4 Policy cho Bảng `budgets`

```sql
ALTER TABLE public.budgets ENABLE ROW LEVEL SECURITY;

CREATE POLICY "budgets_all_own"
    ON public.budgets FOR ALL
    USING (auth.uid() = user_id)
    WITH CHECK (auth.uid() = user_id);
```

### 3.5 Policy cho Bảng `goals`

```sql
ALTER TABLE public.goals ENABLE ROW LEVEL SECURITY;

CREATE POLICY "goals_all_own"
    ON public.goals FOR ALL
    USING (auth.uid() = user_id)
    WITH CHECK (auth.uid() = user_id);
```

---

## 4. Auto-Trigger: handle_new_user()

Khi người dùng đăng ký qua Supabase Auth, trigger này tự động tạo profile trong `public.users`.

```sql
-- Function tạo profile khi user đăng ký
CREATE OR REPLACE FUNCTION public.handle_new_user()
RETURNS TRIGGER
LANGUAGE plpgsql
SECURITY DEFINER SET search_path = public
AS $$
BEGIN
    INSERT INTO public.users (id, full_name, avatar_url)
    VALUES (
        NEW.id,
        NEW.raw_user_meta_data ->> 'full_name',
        NEW.raw_user_meta_data ->> 'avatar_url'
    );
    RETURN NEW;
END;
$$;

-- Trigger: chạy sau khi INSERT vào auth.users
CREATE TRIGGER on_auth_user_created
    AFTER INSERT ON auth.users
    FOR EACH ROW
    EXECUTE PROCEDURE public.handle_new_user();
```

**Luồng hoạt động:**
```
User đăng ký (RegisterScreen)
    │
    ▼
AuthRepository.signUp(email, password, fullName)
    │
    ▼
Supabase Auth → INSERT INTO auth.users
    │
    ▼  (Trigger tự động)
handle_new_user() → INSERT INTO public.users
    │
    ▼
Profile được tạo tự động với full_name từ metadata
```

---

## 5. Seed Dữ Liệu: 17 Danh Mục Mặc Định

```sql
-- Seed default categories (user_id = NULL)
INSERT INTO public.categories (name, icon, color, type) VALUES
-- Thu nhập (Income) - 5 danh mục
('Lương', 'work', '#4CAF50', 'income'),
('Thưởng', 'star', '#8BC34A', 'income'),
('Đầu tư', 'trending_up', '#2196F3', 'income'),
('Kinh doanh', 'store', '#00BCD4', 'income'),
('Khác (Thu)', 'add_circle', '#9C27B0', 'income'),

-- Chi tiêu (Expense) - 12 danh mục
('Ăn uống', 'restaurant', '#F44336', 'expense'),
('Di chuyển', 'directions_car', '#FF9800', 'expense'),
('Mua sắm', 'shopping_cart', '#E91E63', 'expense'),
('Giải trí', 'movie', '#9C27B0', 'expense'),
('Sức khỏe', 'local_hospital', '#F44336', 'expense'),
('Giáo dục', 'school', '#3F51B5', 'expense'),
('Nhà ở', 'home', '#795548', 'expense'),
('Điện nước', 'bolt', '#FF5722', 'expense'),
('Bảo hiểm', 'security', '#607D8B', 'expense'),
('Du lịch', 'flight', '#00BCD4', 'expense'),
('Quà tặng', 'card_giftcard', '#E91E63', 'expense'),
('Khác (Chi)', 'more_horiz', '#9E9E9E', 'expense');
```

### 5.1 Phân Bổ Danh Mục

| Loại | Số lượng | Danh mục |
|---|---|---|
| Thu nhập (income) | 5 | Lương, Thưởng, Đầu tư, Kinh doanh, Khác |
| Chi tiêu (expense) | 12 | Ăn uống, Di chuyển, Mua sắm, Giải trí, Sức khỏe, Giáo dục, Nhà ở, Điện nước, Bảo hiểm, Du lịch, Quà tặng, Khác |
| **Tổng** | **17** | |

---

## 6. Useful SQL Queries

### 6.1 Tổng Thu/Chi Theo Tháng

```sql
SELECT
    type,
    SUM(amount) as total,
    EXTRACT(MONTH FROM date) as month,
    EXTRACT(YEAR FROM date) as year
FROM public.transactions
WHERE user_id = auth.uid()
    AND date >= DATE_TRUNC('month', CURRENT_DATE)
    AND date < DATE_TRUNC('month', CURRENT_DATE) + INTERVAL '1 month'
GROUP BY type, month, year;
```

### 6.2 Chi Tiêu Theo Danh Mục

```sql
SELECT
    c.name as category_name,
    c.icon,
    c.color,
    SUM(t.amount) as total_spent
FROM public.transactions t
JOIN public.categories c ON t.category_id = c.id
WHERE t.user_id = auth.uid()
    AND t.type = 'expense'
    AND t.date >= DATE_TRUNC('month', CURRENT_DATE)
GROUP BY c.name, c.icon, c.color
ORDER BY total_spent DESC;
```

### 6.3 Tiến Độ Ngân Sách

```sql
SELECT
    b.id,
    b.amount as budget_limit,
    c.name as category_name,
    COALESCE(SUM(t.amount), 0) as spent,
    (b.amount - COALESCE(SUM(t.amount), 0)) as remaining
FROM public.budgets b
LEFT JOIN public.categories c ON b.category_id = c.id
LEFT JOIN public.transactions t ON
    t.category_id = b.category_id
    AND t.user_id = b.user_id
    AND t.date BETWEEN b.start_date AND COALESCE(b.end_date, CURRENT_DATE)
WHERE b.user_id = auth.uid()
GROUP BY b.id, b.amount, c.name;
```

---

*Tài liệu này được tạo bởi Người Viết Kỹ Thuật - 2026-03-29*
