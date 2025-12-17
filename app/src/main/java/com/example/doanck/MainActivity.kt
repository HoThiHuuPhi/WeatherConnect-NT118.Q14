package com.example.doanck

import android.Manifest
import android.content.Intent // ⚠️ QUAN TRỌNG: Đã thêm thư viện Intent
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.navigation.compose.rememberNavController
import com.example.doanck.data.datastore.AppDataStore
import com.example.doanck.navigation.AppNav
import com.example.doanck.ui.login.LoginScreen
import com.google.firebase.messaging.FirebaseMessaging

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()

        super.onCreate(savedInstanceState)
        setContent {
            val navController = rememberNavController()
            val context = LocalContext.current

            // Hàm phụ trợ: Giúp khởi động Service gọn gàng hơn
            fun startMySOSService() {
                val intent = Intent(context, SOSService::class.java)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    // Android 8 trở lên bắt buộc dùng startForegroundService
                    context.startForegroundService(intent)
                } else {
                    // Android cũ
                    context.startService(intent)
                }
            }

            // 1. Cấu hình xin quyền (Android 13+)
            val permissionLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.RequestPermission(),
                onResult = { isGranted ->
                    if (isGranted) {
                        // Nếu được cấp quyền -> Kích hoạt Service chạy ngầm ngay
                        startMySOSService()
                    } else {
                        Toast.makeText(context, "Cần cấp quyền để nhận cảnh báo chạy ngầm!", Toast.LENGTH_LONG).show()
                    }
                }
            )

            LaunchedEffect(Unit) {
                // 2. Logic kiểm tra phiên bản Android
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    // Android 13+: Xin quyền trước
                    permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                } else {
                    // Android < 13: Chạy luôn không cần xin
                    startMySOSService()
                }

                // 3. Vẫn giữ code FCM (Để báo cáo với thầy là có dùng công nghệ này)
                FirebaseMessaging.getInstance().subscribeToTopic("all_users")
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            println("Report: Đã đăng ký FCM topic thành công")
                        }
                    }
            }

            AppNav(navController)
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewLoginScreen() {
    val context = LocalContext.current
    val appDataStore = remember { AppDataStore(context.applicationContext) }

    LoginScreen(
        appDataStore = appDataStore,
        onLoginSuccess = {},
        onNavigateToRegister = {},
        onNavigateToForgotPassword = {}
    )
}