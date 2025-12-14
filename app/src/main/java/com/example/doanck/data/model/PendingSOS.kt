package com.example.doanck.data.model

import java.util.UUID

data class PendingSOS(
    val id: String = UUID.randomUUID().toString(),
    val uid: String,            // ✅ Firebase uid
    val email: String,          // để hiển thị
    val phone: String,
    val message: String,
    val lat: Double,
    val lon: Double,
    val timestamp: Long = System.currentTimeMillis(),
    val status: String = "sos"
)
