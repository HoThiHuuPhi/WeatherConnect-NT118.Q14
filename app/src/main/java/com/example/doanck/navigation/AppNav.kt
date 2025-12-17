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
        // ============================================
        // AUTH
        // ============================================
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

        // ============================================
        // MÀN HÌNH CHÍNH
        // ============================================
        composable("main") {
            MainScreen(
                onOpenCommunityChat = { navController.navigate("chat") },
                onOpenSettings = { navController.navigate("settings") },
                onOpenSearch = { navController.navigate("search") },
                onOpenWeatherMap = { navController.navigate("weather_map") },

                // NÚT "BẢN ĐỒ CỨU TRỢ" → MỞ BẢN ĐỒ TỔNG QUAN
                onOpenRescueMap = { navController.navigate("rescue_map_overview") },

                onOpenRescueList = { navController.navigate("rescue_list")
                }
            )
        }

        // ============================================
        // CÁC TÍNH NĂNG KHÁC
        // ============================================
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

        composable("chat") {
            CommunityChatScreen(onBack = { navController.popBackStack() })
        }

        composable("search") {
            SearchScreen(onBack = { navController.popBackStack() })
        }

        composable("weather_map") {
            WeatherMapScreen(onBack = { navController.popBackStack() })
        }

        // ============================================
        // HỆ THỐNG CỨU TRỢ (3 ROUTES)
        // ============================================

        // 1. BẢN ĐỒ TỔNG QUAN - Hiển thị tất cả các ca SOS
        composable("rescue_map_overview") {
            RescueMapScreen(
                onBack = { navController.popBackStack() },
                onOpenList = { navController.navigate("rescue_list") },
                onOpenSOSDetail = { lat, lon, name ->
                    val cleanName = name.ifBlank { "SOS" }.replace("/", "-")
                    navController.navigate("sos_map/$lat/$lon/$cleanName")
                }
            )
        }


        // 2. DANH SÁCH CÁC CA CỨU HỘ
        composable("rescue_list") {
            SOSMonitorScreen(
                onBack = { navController.popBackStack() }, // Quay về Bản đồ
                onNavigateToMap = { lat, lon, name ->
                    val safeName = if (name.isNotBlank()) name else "SOS"
                    val cleanName = safeName.replace("/", "-")
                    navController.navigate("sos_map/$lat/$lon/$cleanName")
                },
                // NÚT "XEM BẢN ĐỒ" → QUAY VỀ BẢN ĐỒ TỔNG QUAN
                onOpenMapOverview = {
                    navController.popBackStack() // Quay về rescue_map_overview
                }
            )
        }

        // 3. BẢN ĐỒ CHI TIẾT - Chỉ đường cho 1 người cụ thể
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
                onBack = { navController.popBackStack() },
                onOpenRescueMap = { navController.navigate("rescue_map_overview") } // ✅ thêm dòng này
            )
        }
    }
}