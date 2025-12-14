package com.example.doanck.ui.main

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Color

@Composable
fun ConnectScreen(onOpenSettings: () -> Unit = {}) {
    Box(
        modifier = Modifier.fillMaxSize().padding(top = 100.dp),
        contentAlignment = Alignment.TopCenter
    ) {
        Text("ConnectScreen chưa làm UI", color = Color.Black)
    }
}
