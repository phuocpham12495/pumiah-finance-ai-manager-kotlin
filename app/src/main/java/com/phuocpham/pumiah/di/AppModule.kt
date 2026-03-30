package com.phuocpham.pumiah.di

import com.phuocpham.pumiah.BuildConfig
import com.phuocpham.pumiah.data.repository.AuthRepository
import com.phuocpham.pumiah.data.repository.BudgetRepository
import com.phuocpham.pumiah.data.repository.CategoryRepository
import com.phuocpham.pumiah.data.repository.ChatRepository
import com.phuocpham.pumiah.data.repository.GoalRepository
import com.phuocpham.pumiah.data.repository.ProfileRepository
import com.phuocpham.pumiah.data.repository.TransactionRepository
import com.phuocpham.pumiah.data.repository.WalletRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.storage.Storage
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideSupabaseClient(): SupabaseClient = createSupabaseClient(
        supabaseUrl = BuildConfig.SUPABASE_URL,
        supabaseKey = BuildConfig.SUPABASE_ANON_KEY
    ) {
        install(Auth)
        install(Postgrest)
        install(Storage)
    }

    @Provides
    @Singleton
    fun provideAuthRepository(client: SupabaseClient) = AuthRepository(client)

    @Provides
    @Singleton
    fun provideProfileRepository(client: SupabaseClient) = ProfileRepository(client)

    @Provides
    @Singleton
    fun provideCategoryRepository(client: SupabaseClient) = CategoryRepository(client)

    @Provides
    @Singleton
    fun provideTransactionRepository(client: SupabaseClient) = TransactionRepository(client)

    @Provides
    @Singleton
    fun provideBudgetRepository(client: SupabaseClient) = BudgetRepository(client)

    @Provides
    @Singleton
    fun provideGoalRepository(client: SupabaseClient) = GoalRepository(client)

    @Provides
    @Singleton
    fun provideWalletRepository(client: SupabaseClient) = WalletRepository(client)

    @Provides
    @Singleton
    fun provideChatRepository() = ChatRepository()
}
