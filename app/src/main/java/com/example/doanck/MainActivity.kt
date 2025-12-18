package com.example.doanck

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.navigation.compose.rememberNavController
import com.example.doanck.data.datastore.AppDataStore
import com.example.doanck.navigation.AppNav
import com.google.firebase.messaging.FirebaseMessaging

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)

        setContent {
            val navController = rememberNavController()
            val context = LocalContext.current
            val appDataStore = remember { AppDataStore(context) }

            // Lấy UID từ DataStore
            val currentUid by appDataStore.currentUid.collectAsState(initial = null)

            // --- Logic Check Session ---
            // Nếu có UID -> Vào Main. Ngược lại -> Vào Login.
            // Checkbox không ảnh hưởng ở đây.
            val startDest = if (!currentUid.isNullOrBlank()) "main" else "login"

            // --- Service & Permission (Code cũ) ---
            fun startMySOSService() {
                val intent = Intent(context, SOSService::class.java)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) context.startForegroundService(intent)
                else context.startService(intent)
            }
            val permissionLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.RequestPermission(),
                onResult = { if (it) startMySOSService() }
            )
            LaunchedEffect(Unit) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                else startMySOSService()
                FirebaseMessaging.getInstance().subscribeToTopic("all_users")
            }

            AppNav(
                navController = navController,
                appDataStore = appDataStore,
                startDestination = startDest
            )
        }
    }
}