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
import com.example.doanck.ui.login.LoginScreen
import com.example.doanck.ui.main.MainScreen
import com.example.doanck.ui.main.SettingsScreen
import com.example.doanck.ui.register.RegisterScreen
import com.example.doanck.ui.chat.CommunityChatScreen
import com.example.doanck.ui.auth.ForgotPasswordScreen
import com.google.firebase.auth.FirebaseAuth

@Composable
fun AppNav(
    navController: NavHostController = rememberNavController()
) {
    val context = LocalContext.current
    val auth = FirebaseAuth.getInstance()

    // ✅ DataStore dùng chung
    val appDataStore = remember { AppDataStore(context.applicationContext) }

    // ✅ Nếu user đã login sẵn (startDestination = main) thì vẫn setCurrentUser
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
        composable("login") {
            LoginScreen(
                appDataStore = appDataStore, // ✅ TRUYỀN VÀO
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

        composable("main") {
            MainScreen(
                onOpenCommunityChat = { navController.navigate("chat") },
                onOpenSettings = { navController.navigate("settings") }
            )
        }

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

        composable("forgot_password") {
            ForgotPasswordScreen(onBack = { navController.popBackStack() })
        }
    }
}
