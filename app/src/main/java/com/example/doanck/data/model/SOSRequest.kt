package com.example.doanck.data.model

import java.util.UUID


data class SOSRequest(
    val id: String = UUID.randomUUID().toString(),
    val userId: String = "",
    val email: String = "",
    val phone: String = "",
    val message: String = "",
    val lat: Double = 0.0,
    val lon: Double = 0.0,
    val province: String? = null,
    val status: String = "sos",
    val timestamp: Long = System.currentTimeMillis()
)