package com.example.doanck.ui.chat

data class CommunityMessage(
    val id: String,
    val userId: String,
    val message: String,
    val severity: String, // "info", "warning", "emergency"
    val anonymous: Boolean,
    val timestamp: Long,
    val realUserId: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val imageUrl: String? = null

)