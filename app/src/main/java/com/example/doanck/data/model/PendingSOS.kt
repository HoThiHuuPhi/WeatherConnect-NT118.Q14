package com.example.doanck.data.model

import java.util.UUID

data class PendingSOS(
    val id: String = UUID.randomUUID().toString(),
    val userId: String,          // Khớp với SOSDialog
    val email: String,           // ✅ Đã thêm trường này
    val phone: String,
    val message: String,
    val lat: Double,
    val lon: Double,
    val province: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val status: String = "sos"
)