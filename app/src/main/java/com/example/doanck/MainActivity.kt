package com.example.doanck

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.compose.rememberNavController
import com.example.doanck.navigation.AppNav
import com.example.doanck.ui.login.LoginScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {

            // ▶️ DÙNG NAVIGATION CHÍNH THỨC
            val navController = rememberNavController()
            AppNav(navController)
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewLoginScreen() {
    LoginScreen(
        onLoginSuccess = {},
        onNavigateToRegister = {},

        // 👇 BẠN ĐANG THIẾU DÒNG NÀY TRONG PREVIEW NÊN NÓ BÁO LỖI
        onNavigateToForgotPassword = {}
    )
}