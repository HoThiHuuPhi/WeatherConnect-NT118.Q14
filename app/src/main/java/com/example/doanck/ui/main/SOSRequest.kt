package com.example.doanck.ui.main

data class SOSRequest(
    val userId: String = "",
    val email: String = "", // ✅ Thêm email vào đây để hiển thị
    val phone: String = "",
    val message: String = "",
    val lat: Double = 0.0,
    val lon: Double = 0.0,
    val province: String = "",
    val status: String = "sos",
    val timestamp: Long = 0L
)

