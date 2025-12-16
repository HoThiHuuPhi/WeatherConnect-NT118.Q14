package com.example.doanck.data.model

import com.google.firebase.firestore.DocumentId

data class SOSRequest(
    // 👇 ĐỔI TÊN THÀNH docId ĐỂ TRÁNH XUNG ĐỘT
    @DocumentId
    val docId: String = "",

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