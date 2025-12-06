package com.example.doanck.ui.chat

data class CommunityMessage(
    val id: String,
    val userId: String,
    val message: String,
    val severity: String, // "info", "warning", "emergency"
    val anonymous: Boolean,
    val timestamp: Long,
    val realUserId: String? = null,
    // Thêm 2 trường này để lưu vị trí (có thể null nếu không muốn gửi)
    val latitude: Double? = null,
    val longitude: Double? = null,
    val imageUrl: String? = null

)