package com.example.doanck.ui.main

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.viewinterop.AndroidView

@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun WeatherMapScreen(onBack: () -> Unit) {
    var isLoading by remember { mutableStateOf(true) }
    var hasError by remember { mutableStateOf(false) }

    // DÙNG VENTUSKY: Nhẹ hơn, ít lỗi xám màn hình hơn Windy
    // l=temperature-2m: Lớp nhiệt độ (màu sắc đẹp)
    // l=rain-3h: Lớp mưa
    // p=16.0;106.0;5: Tọa độ Việt Nam, Zoom 5
    val mapUrl = "https://www.ventusky.com/?p=16.0;106.0;5&l=temperature-2m"

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Bản đồ mật độ Thời tiết", color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF191C2A))
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentAlignment = Alignment.Center
        ) {
            if (hasError) {
                Text("Không thể tải bản đồ. Kiểm tra kết nối mạng.", color = Color.White)
            } else {
                AndroidView(
                    factory = { context ->
                        WebView(context).apply {
                            // Cấu hình WebView tối đa để tránh lỗi hiển thị
                            settings.apply {
                                javaScriptEnabled = true
                                domStorageEnabled = true
                                databaseEnabled = true
                                loadWithOverviewMode = true
                                useWideViewPort = true
                                builtInZoomControls = false
                                displayZoomControls = false

                                // QUAN TRỌNG: Cho phép tải nội dung hỗn hợp (tránh lỗi xám xịt do thiếu ảnh)
                                mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                            }

                            // Bắt buộc dùng Hardware Acceleration cho bản đồ
                            setLayerType(android.view.View.LAYER_TYPE_HARDWARE, null)

                            webViewClient = object : WebViewClient() {
                                override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                                    isLoading = true
                                    hasError = false
                                }

                                override fun onPageFinished(view: WebView?, url: String?) {
                                    isLoading = false
                                }

                                override fun onReceivedError(view: WebView?, request: WebResourceRequest?, error: WebResourceError?) {
                                    // Chỉ hiện lỗi nếu là lỗi chính, bỏ qua lỗi nhỏ
                                    if (request?.isForMainFrame == true) {
                                        isLoading = false
                                        hasError = true
                                    }
                                }
                            }

                            loadUrl(mapUrl)
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }

            // Loading Indicator
            if (isLoading) {
                CircularProgressIndicator(color = Color.White)
            }
        }
    }
}