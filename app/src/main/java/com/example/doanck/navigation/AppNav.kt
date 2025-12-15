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
import com.example.doanck.ui.main.RescueMapScreen
import com.example.doanck.ui.main.SOSMapScreen
import com.example.doanck.ui.main.SOSMonitorScreen
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
        // --- AUTH ---
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

                // Nút mở Dialog danh sách SOS (nếu logic MainScreen dùng Dialog thì dòng này có thể thừa hoặc thiếu tùy logic, nhưng cứ giữ nguyên)
                onOpenRescueMap = { navController.navigate("rescue_list") },

                // 🔴 KHẮC PHỤC LỖI TẠI ĐÂY: Thêm logic điều hướng cho SOS Map (1 người)
                onNavigateToSOSMap = { lat, lon, name ->
                    val safeName = if (name.isNotBlank()) name else "SOS"
                    val cleanName = safeName.replace("/", "-")
                    navController.navigate("sos_map/$lat/$lon/$cleanName")
                },

                // 🟢 QUAN TRỌNG: Thêm dòng này để nút "Map Overview" trong Dialog hoạt động
                onOpenRescueOverview = {
                    navController.navigate("rescue_map_overview")
                }
            )
        }

        // --- TÍNH NĂNG KHÁC ---
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

        // --- HỆ THỐNG CỨU TRỢ ---

        // 1. Danh sách SOS
        composable("rescue_list") {
            SOSMonitorScreen(
                onBack = { navController.popBackStack() },
                onNavigateToMap = { lat, lon, name ->
                    val safeName = if (name.isNotBlank()) name else "SOS"
                    val cleanName = safeName.replace("/", "-")
                    navController.navigate("sos_map/$lat/$lon/$cleanName")
                },
                onOpenMapOverview = {
                    navController.navigate("rescue_map_overview")
                }
            )
        }

        // 2. Bản đồ tổng quan (Map chứa tất cả chấm đỏ)
        composable("rescue_map_overview") {
            RescueMapScreen(
                onBack = { navController.popBackStack() }
            )
        }

        // 3. Bản đồ chi tiết (Chỉ đường cho 1 người)
        composable(
            route = "sos_map/{lat}/{lon}/{name}",
            arguments = listOf(
                navArgument("lat") { type = NavType.StringType },
                navArgument("lon") { type = NavType.StringType },
                navArgument("name") { type = NavType.StringType }
            )
        ) { backStackEntry ->
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