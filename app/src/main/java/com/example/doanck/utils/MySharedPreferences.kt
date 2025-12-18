package com.example.doanck.utils

import android.content.Context

object MySharedPreferences {
    private const val PREF_NAME = "AppLoginPrefs"
    private const val KEY_EMAIL = "saved_email"
    private const val KEY_PASS = "saved_pass"
    private const val KEY_IS_CHECKED = "is_checked"

    // 1. Lưu thông tin (Khi bấm Đăng nhập + Tích)
    fun saveCredentials(context: Context, email: String, pass: String) {
        val pref = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val editor = pref.edit()
        editor.putString(KEY_EMAIL, email)
        editor.putString(KEY_PASS, pass)
        editor.putBoolean(KEY_IS_CHECKED, true) // Lưu trạng thái đã tích
        editor.apply()
    }

    // 2. Xóa thông tin (Khi bấm Đăng nhập + KHÔNG Tích)
    fun clearCredentials(context: Context) {
        val pref = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val editor = pref.edit()
        editor.remove(KEY_EMAIL)
        editor.remove(KEY_PASS)
        editor.putBoolean(KEY_IS_CHECKED, false)
        editor.apply()
    }

    // 3. Lấy thông tin ra (Để tự điền vào ô)
    fun getSavedCredentials(context: Context): Triple<String, String, Boolean> {
        val pref = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val email = pref.getString(KEY_EMAIL, "") ?: ""
        val pass = pref.getString(KEY_PASS, "") ?: ""
        val isChecked = pref.getBoolean(KEY_IS_CHECKED, false)
        return Triple(email, pass, isChecked)
    }
}