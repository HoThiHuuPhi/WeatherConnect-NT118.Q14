package com.example.doanck.ui.main

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun MainScreen(
    onOpenCommunityChat: () -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,       // sửa tại đây
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "WeatherConnect",
            style = MaterialTheme.typography.headlineMedium
        )

        Spacer(Modifier.height(16.dp))

        Button(onClick = onOpenCommunityChat) {
            Text("Mở Chat cộng đồng khu vực")
        }
    }
}
