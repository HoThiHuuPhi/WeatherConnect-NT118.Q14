package com.example.doanck.ui.auth

import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

class ForgotPasswordViewModel : ViewModel() {
    private val auth = Firebase.auth

    fun sendPasswordResetEmail(email: String, context: Context, onSuccess: () -> Unit) {
        if (email.isBlank()) {
            Toast.makeText(context, "Vui lòng nhập email!", Toast.LENGTH_SHORT).show()
            return
        }

        auth.sendPasswordResetEmail(email)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Log.d("QuenMatKhau", "Firebase báo: Email đã được gửi!")
                    Toast.makeText(context, "Đã gửi! Kiểm tra hòm thư (cả Spam)", Toast.LENGTH_LONG).show()
                    onSuccess()
                } else {
                    val errorMsg = task.exception?.message ?: "Lỗi không xác định"
                    Log.e("QuenMatKhau", "Lỗi: $errorMsg")
                    Toast.makeText(context, "Lỗi: $errorMsg", Toast.LENGTH_LONG).show()
                }
            }
    }
}