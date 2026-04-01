package com.phuocpham.pumiah.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.phuocpham.pumiah.ui.screen.auth.LoginScreen
import com.phuocpham.pumiah.ui.screen.auth.RegisterScreen
import com.phuocpham.pumiah.ui.screen.budget.BudgetScreen
import com.phuocpham.pumiah.ui.screen.category.CategoryScreen
import com.phuocpham.pumiah.ui.screen.chat.ChatScreen
import com.phuocpham.pumiah.ui.screen.dashboard.DashboardScreen
import com.phuocpham.pumiah.ui.screen.goal.GoalScreen
import com.phuocpham.pumiah.ui.screen.profile.ProfileScreen
import com.phuocpham.pumiah.ui.screen.transaction.TransactionListScreen
import com.phuocpham.pumiah.ui.screen.wallet.WalletScreen
import com.phuocpham.pumiah.viewmodel.AuthViewModel

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
    object Wallets : Screen("wallets")
}

data class BottomNavItem(val screen: Screen, val label: String, val icon: @Composable () -> Unit)

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val authViewModel: AuthViewModel = hiltViewModel()
    val sessionReady by authViewModel.sessionReady.collectAsState()

    if (!sessionReady) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    val startDestination = if (authViewModel.isLoggedIn) Screen.Dashboard.route else Screen.Login.route

    val bottomNavItems = listOf(
        BottomNavItem(Screen.Dashboard, "Tổng quan") { Icon(Icons.Default.Home, null) },
        BottomNavItem(Screen.Transactions, "Giao dịch") { Icon(Icons.Default.Receipt, null) },
        BottomNavItem(Screen.Chat, "AI Chat") { Icon(Icons.Default.Chat, null) },
        BottomNavItem(Screen.Wallets, "Ví") { Icon(Icons.Default.AccountBalanceWallet, null) },
        BottomNavItem(Screen.Profile, "Hồ sơ") { Icon(Icons.Default.Person, null) },
    )

    val appScreens = setOf(
        Screen.Dashboard.route, Screen.Transactions.route, Screen.Categories.route,
        Screen.Budgets.route, Screen.Goals.route, Screen.Chat.route,
        Screen.Profile.route, Screen.Wallets.route
    )

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val showBottomBar = currentRoute in appScreens

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    bottomNavItems.forEach { item ->
                        NavigationBarItem(
                            icon = item.icon,
                            label = { Text(item.label) },
                            selected = navBackStackEntry?.destination?.hierarchy?.any { it.route == item.screen.route } == true,
                            onClick = {
                                navController.navigate(item.screen.route) {
                                    // Always pop to Dashboard (the authenticated home), not findStartDestination()
                                    // which could return Login node when graph startDestination is login
                                    popUpTo(Screen.Dashboard.route) { saveState = true }
                                    launchSingleTop = true
                                    // Profile tab never restores saved state so sub-screens (Categories, Budgets, Goals) are cleared
                                    restoreState = item.screen != Screen.Profile
                                }
                            }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Login.route) {
                LoginScreen(
                    onLoginSuccess = { navController.navigate(Screen.Dashboard.route) { popUpTo(Screen.Login.route) { inclusive = true } } },
                    onNavigateToRegister = { navController.navigate(Screen.Register.route) }
                )
            }
            composable(Screen.Register.route) {
                RegisterScreen(
                    onRegisterSuccess = { navController.navigate(Screen.Dashboard.route) { popUpTo(Screen.Login.route) { inclusive = true } } },
                    onNavigateToLogin = { navController.popBackStack() }
                )
            }
            composable(Screen.Dashboard.route) { DashboardScreen(navController) }
            composable(Screen.Transactions.route) { TransactionListScreen() }
            composable(Screen.Categories.route) { CategoryScreen() }
            composable(Screen.Budgets.route) { BudgetScreen() }
            composable(Screen.Goals.route) { GoalScreen() }
            composable(Screen.Chat.route) { ChatScreen() }
            composable(Screen.Wallets.route) { WalletScreen() }
            composable(Screen.Profile.route) {
                ProfileScreen(
                    onSignOut = { navController.navigate(Screen.Login.route) { popUpTo(0) { inclusive = true } } },
                    onNavigateToCategories = { navController.navigate(Screen.Categories.route) },
                    onNavigateToBudgets = { navController.navigate(Screen.Budgets.route) },
                    onNavigateToGoals = { navController.navigate(Screen.Goals.route) }
                )
            }
        }
    }
}
