package com.example.doanck.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope // Nhớ import
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
import com.example.doanck.ui.main.*
import com.example.doanck.ui.register.RegisterScreen
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

@Composable
fun AppNav(
    navController: NavHostController = rememberNavController(),
    appDataStore: AppDataStore,
    startDestination: String
) {
    val auth = FirebaseAuth.getInstance()

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
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

        composable("main") {
            MainScreen(
                onOpenCommunityChat = { navController.navigate("chat") },
                onOpenSettings = { navController.navigate("settings") },
                onOpenSearch = { navController.navigate("search") },
                onOpenWeatherMap = { navController.navigate("weather_map") },
                onOpenRescueMap = { navController.navigate("rescue_map_overview") },
                onOpenRescueList = { navController.navigate("rescue_list") }
            )
        }

        composable("settings") {
            val scope = rememberCoroutineScope() // Scope cho coroutine
            SettingsScreen(
                appDataStore = appDataStore,
                onBack = { navController.popBackStack() },
                onLogout = {
                    // 1. Đăng xuất Firebase
                    auth.signOut()

                    // 2. Xóa Session (UID) -> Để App biết là đã thoát
                    scope.launch {
                        appDataStore.clearSession()
                    }

                    // 3. Về Login
                    navController.navigate("login") {
                        popUpTo("main") { inclusive = true }
                    }
                }
            )
        }

        composable("chat") { CommunityChatScreen(onBack = { navController.popBackStack() }) }
        composable("search") { SearchScreen(onBack = { navController.popBackStack() }) }
        composable("weather_map") { WeatherMapScreen(onBack = { navController.popBackStack() }) }

        composable("rescue_map_overview") {
            SOSOverviewMapScreen(
                onBack = { navController.popBackStack() },
                onOpenList = { navController.navigate("rescue_list") },
                onOpenRescueMap = { lat, lon, name ->
                    val safeName = name.ifBlank { "SOS" }.replace("/", "-")
                    navController.navigate("rescue_map/$lat/$lon/$safeName")
                }
            )
        }

        composable("rescue_list") {
            SOSMonitorScreen(
                onBack = { navController.popBackStack() },
                onNavigateToMap = { lat, lon, name ->
                    val safeName = name.ifBlank { "SOS" }.replace("/", "-")
                    navController.navigate("rescue_map/$lat/$lon/$safeName")
                },
                onOpenMapOverview = { navController.popBackStack("rescue_map_overview", false) }
            )
        }

        composable(
            route = "rescue_map/{lat}/{lon}/{name}",
            arguments = listOf(
                navArgument("lat") { type = NavType.StringType },
                navArgument("lon") { type = NavType.StringType },
                navArgument("name") { type = NavType.StringType }
            )
        ) { back ->
            val lat = back.arguments?.getString("lat")?.toDoubleOrNull() ?: 0.0
            val lon = back.arguments?.getString("lon")?.toDoubleOrNull() ?: 0.0
            val name = back.arguments?.getString("name") ?: "SOS"
            RescueMapScreen(lat, lon, name, { navController.popBackStack() }, { navController.popBackStack("rescue_map_overview", false) })
        }
    }
}