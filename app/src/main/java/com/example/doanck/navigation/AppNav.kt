package com.example.doanck.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.doanck.ui.login.LoginScreen
import com.example.doanck.ui.main.MainScreen
import com.example.doanck.ui.register.RegisterScreen
import com.example.doanck.ui.chat.CommunityChatScreen
import com.example.doanck.ui.auth.ForgotPasswordScreen // <--- 1. Import màn hình mới

@Composable
fun AppNav(navController: NavHostController = rememberNavController()) {

    NavHost(navController = navController, startDestination = "login") {

        composable("login") {
            LoginScreen(
                onLoginSuccess = {
                    navController.navigate("main") {
                        popUpTo("login") { inclusive = true }
                    }
                },
                onNavigateToRegister = { navController.navigate("register") },

                // 2. Thêm sự kiện bấm nút Quên Pass -> Chuyển hướng
                onNavigateToForgotPassword = {
                    navController.navigate("forgot_password")
                }
            )
        }

        composable("register") {
            RegisterScreen(
                onRegisterSuccess = { navController.popBackStack() },
                onBackToLogin = { navController.popBackStack() }
            )
        }

        composable("main") {
            MainScreen(
                onOpenCommunityChat = {
                    navController.navigate("chat")
                }
            )
        }

        composable("chat") {
            CommunityChatScreen(
                onBack = { navController.popBackStack() }
            )
        }

        // 3. Khai báo màn hình Quên Mật Khẩu ở đây
        composable("forgot_password") {
            ForgotPasswordScreen(
                onBack = { navController.popBackStack() } // Bấm nút back thì quay lại Login
            )
        }
    }
}