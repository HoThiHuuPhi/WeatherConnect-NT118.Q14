package com.example.doanck.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.doanck.data.datastore.AppDataStore
import com.example.doanck.ui.auth.ForgotPasswordScreen
import com.example.doanck.ui.chat.CommunityChatScreen
import com.example.doanck.ui.login.LoginScreen
import com.example.doanck.ui.main.MainScreen
import com.example.doanck.ui.main.SearchScreen
import com.example.doanck.ui.main.SettingsScreen
import com.example.doanck.ui.main.WeatherMapScreen // ✅ Nhớ Import màn hình bản đồ
import com.example.doanck.ui.register.RegisterScreen
import com.google.firebase.auth.FirebaseAuth

@Composable
fun AppNav(
    navController: NavHostController = rememberNavController()
) {
    val context = LocalContext.current
    val auth = FirebaseAuth.getInstance()

    // DataStore dùng chung cho toàn app
    val appDataStore = remember { AppDataStore(context.applicationContext) }

    // Kiểm tra trạng thái đăng nhập để lưu vào DataStore (nếu cần dùng ở màn hình khác)
    LaunchedEffect(auth.currentUser?.uid) {
        val user = auth.currentUser
        if (user != null) {
            appDataStore.setCurrentUser(user.uid, user.email ?: "")
        }
    }

    // Xác định màn hình bắt đầu (nếu đã login thì vào thẳng Main)
    val startDestination = if (auth.currentUser != null) "main" else "login"

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        // --- NHÓM AUTHENTICATION ---
        composable("login") {
            LoginScreen(
                appDataStore = appDataStore,
                onLoginSuccess = {
                    navController.navigate("main") {
                        popUpTo("login") { inclusive = true }
                    }
                },
                onNavigateToRegister = { navController.navigate("register") },
                onNavigateToForgotPassword = { navController.navigate("forgot_password") }
            )
        }

        composable("register") {
            RegisterScreen(
                onRegisterSuccess = { navController.popBackStack() },
                onBackToLogin = { navController.popBackStack() }
            )
        }

        composable("forgot_password") {
            ForgotPasswordScreen(onBack = { navController.popBackStack() })
        }

        // --- MÀN HÌNH CHÍNH ---
        composable("main") {
            MainScreen(
                // Điều hướng sang các màn hình chức năng riêng biệt
                onOpenCommunityChat = { navController.navigate("chat") },
                onOpenSettings = { navController.navigate("settings") },
                onOpenSearch = { navController.navigate("search") },
                onOpenWeatherMap = { navController.navigate("weather_map") }
            )
        }

        // --- CÁC MÀN HÌNH CHỨC NĂNG ---

        // 1. Cài đặt
        composable("settings") {
            SettingsScreen(
                appDataStore = appDataStore,
                onBack = { navController.popBackStack() },
                onLogout = {
                    auth.signOut()
                    navController.navigate("login") {
                        popUpTo("main") { inclusive = true }
                    }
                }
            )
        }

        // 2. Chat Cộng Đồng
        composable("chat") {
            CommunityChatScreen(onBack = { navController.popBackStack() })
        }

        // 3. Tìm kiếm Thời tiết
        composable("search") {
            SearchScreen(onBack = { navController.popBackStack() })
        }

        // 4. Bản đồ Thời tiết (Windy)
        composable("weather_map") {
            WeatherMapScreen(onBack = { navController.popBackStack() })
        }
    }
}