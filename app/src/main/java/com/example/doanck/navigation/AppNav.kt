package com.example.doanck.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.doanck.data.datastore.AppDataStore
import com.example.doanck.ui.auth.ForgotPasswordScreen
import com.example.doanck.ui.chat.CommunityChatScreen
import com.example.doanck.ui.login.LoginScreen
import com.example.doanck.ui.main.MainScreen
import com.example.doanck.ui.main.RescueMapScreen      // ✅ Import Màn hình Bản đồ tổng quan (MỚI)
import com.example.doanck.ui.main.SOSMapScreen       // ✅ Import màn hình Bản đồ chi tiết (1 người)
import com.example.doanck.ui.main.SOSMonitorScreen   // ✅ Import màn hình Danh sách SOS
import com.example.doanck.ui.main.SearchScreen
import com.example.doanck.ui.main.SettingsScreen
import com.example.doanck.ui.main.WeatherMapScreen
import com.example.doanck.ui.register.RegisterScreen
import com.google.firebase.auth.FirebaseAuth

@Composable
fun AppNav(
    navController: NavHostController = rememberNavController()
) {
    val context = LocalContext.current
    val auth = FirebaseAuth.getInstance()
    val appDataStore = remember { AppDataStore(context.applicationContext) }

    LaunchedEffect(auth.currentUser?.uid) {
        val user = auth.currentUser
        if (user != null) {
            appDataStore.setCurrentUser(user.uid, user.email ?: "")
        }
    }

    val startDestination = if (auth.currentUser != null) "main" else "login"

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        // --- NHÓM AUTH ---
        composable("login") {
            LoginScreen(
                appDataStore = appDataStore,
                onLoginSuccess = { navController.navigate("main") { popUpTo("login") { inclusive = true } } },
                onNavigateToRegister = { navController.navigate("register") },
                onNavigateToForgotPassword = { navController.navigate("forgot_password") }
            )
        }
        composable("register") {
            RegisterScreen(onRegisterSuccess = { navController.popBackStack() }, onBackToLogin = { navController.popBackStack() })
        }
        composable("forgot_password") { ForgotPasswordScreen(onBack = { navController.popBackStack() }) }

        // --- MÀN HÌNH CHÍNH ---
        composable("main") {
            MainScreen(
                onOpenCommunityChat = { navController.navigate("chat") },
                onOpenSettings = { navController.navigate("settings") },
                onOpenSearch = { navController.navigate("search") },
                onOpenWeatherMap = { navController.navigate("weather_map") },

                // ✅ Sự kiện mở danh sách cứu trợ
                onOpenRescueMap = { navController.navigate("rescue_list") },

                // 🔴 Xử lý nút bấm "Xem bản đồ" trong Dialog của MainScreen
                onNavigateToSOSMap = { lat, lon, name ->
                    val safeName = if (name.isNotBlank()) name else "SOS"
                    val cleanName = safeName.replace("/", "-")
                    navController.navigate("sos_map/$lat/$lon/$cleanName")
                }
            )
        }

        // --- CÁC TÍNH NĂNG ---
        composable("settings") {
            SettingsScreen(
                appDataStore = appDataStore,
                onBack = { navController.popBackStack() },
                onLogout = {
                    auth.signOut()
                    navController.navigate("login") { popUpTo("main") { inclusive = true } }
                }
            )
        }

        composable("chat") { CommunityChatScreen(onBack = { navController.popBackStack() }) }
        composable("search") { SearchScreen(onBack = { navController.popBackStack() }) }
        composable("weather_map") { WeatherMapScreen(onBack = { navController.popBackStack() }) }

        // ==========================================
        // 🔥 CÁC ROUTE MỚI CHO HỆ THỐNG CỨU TRỢ 🔥
        // ==========================================

        // 1. Danh sách người cần cứu (SOS List)
        composable("rescue_list") {
            SOSMonitorScreen(
                onBack = { navController.popBackStack() },
                // Khi bấm nút "Xem bản đồ" trên từng thẻ SOS
                onNavigateToMap = { lat, lon, name ->
                    val safeName = if (name.isNotBlank()) name else "SOS"
                    val cleanName = safeName.replace("/", "-")
                    navController.navigate("sos_map/$lat/$lon/$cleanName")
                },
                // 🟢 SỰ KIỆN MỚI: Mở bản đồ tổng quan (Nút trên thanh tìm kiếm)
                onOpenMapOverview = {
                    navController.navigate("rescue_map_overview")
                }
            )
        }

        // 2. Màn hình bản đồ tổng quan (Hiển thị tất cả chấm đỏ) - MỚI
        composable("rescue_map_overview") {
            RescueMapScreen(
                onBack = { navController.popBackStack() }
            )
        }

        // 3. Màn hình bản đồ chi tiết (Chỉ đường cho 1 người)
        composable(
            route = "sos_map/{lat}/{lon}/{name}",
            arguments = listOf(
                navArgument("lat") { type = NavType.StringType },
                navArgument("lon") { type = NavType.StringType },
                navArgument("name") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            // Lấy dữ liệu từ đường dẫn
            val latStr = backStackEntry.arguments?.getString("lat") ?: "0.0"
            val lonStr = backStackEntry.arguments?.getString("lon") ?: "0.0"
            val name = backStackEntry.arguments?.getString("name") ?: "Người cần cứu"

            SOSMapScreen(
                lat = latStr.toDoubleOrNull() ?: 0.0,
                lon = lonStr.toDoubleOrNull() ?: 0.0,
                name = name,
                onBack = { navController.popBackStack() }
            )
        }
    }
}