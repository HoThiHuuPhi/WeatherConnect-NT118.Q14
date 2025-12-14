package com.example.doanck.data.datastore

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "app_settings")

class AppDataStore(private val context: Context) {

    companion object {
        val ENABLE_ANIMATION = booleanPreferencesKey("enable_animation")
        val TEMP_UNIT = stringPreferencesKey("temp_unit")

        // ✅ Session hiện tại
        val CURRENT_UID = stringPreferencesKey("current_uid")
        val CURRENT_EMAIL = stringPreferencesKey("current_email")
    }

    // ==========================================
    // 🔑 DYNAMIC KEYS (Key theo từng User ID)
    // ==========================================
    private fun avatarKey(uid: String) = stringPreferencesKey("user_avatar_uri_$uid")
    private fun dobKey(uid: String) = stringPreferencesKey("user_dob_$uid")
    private fun phoneKey(uid: String) = stringPreferencesKey("user_phone_$uid")
    private fun genderKey(uid: String) = stringPreferencesKey("user_gender_$uid")

    // ==========================================
    // 📖 READ (Đọc dữ liệu)
    // ==========================================

    val enableAnimation: Flow<Boolean> = context.dataStore.data
        .catch { emit(emptyPreferences()) }
        .map { it[ENABLE_ANIMATION] ?: true }

    val tempUnit: Flow<String> = context.dataStore.data
        .catch { emit(emptyPreferences()) }
        .map { it[TEMP_UNIT] ?: "C" }

    val userEmail: Flow<String> = context.dataStore.data
        .catch { emit(emptyPreferences()) }
        .map { it[CURRENT_EMAIL] ?: "" }

    // Lấy Avatar của user đang đăng nhập
    val userAvatar: Flow<String?> = context.dataStore.data
        .catch { emit(emptyPreferences()) }
        .map { prefs ->
            val uid = prefs[CURRENT_UID].orEmpty()
            if (uid.isBlank()) null else prefs[avatarKey(uid)]
        }

    // Lấy Ngày sinh (Mặc định 01/01/2000 nếu chưa có)
    val userDob: Flow<String> = context.dataStore.data
        .catch { emit(emptyPreferences()) }
        .map { prefs ->
            val uid = prefs[CURRENT_UID].orEmpty()
            if (uid.isBlank()) "01/01/2000" else prefs[dobKey(uid)] ?: "01/01/2000"
        }

    // Lấy SĐT (Mặc định "Chưa cập nhật")
    val userPhone: Flow<String> = context.dataStore.data
        .catch { emit(emptyPreferences()) }
        .map { prefs ->
            val uid = prefs[CURRENT_UID].orEmpty()
            if (uid.isBlank()) "Chưa cập nhật" else prefs[phoneKey(uid)] ?: "Chưa cập nhật"
        }

    // Lấy Giới tính (Mặc định "Nam")
    val userGender: Flow<String> = context.dataStore.data
        .catch { emit(emptyPreferences()) }
        .map { prefs ->
            val uid = prefs[CURRENT_UID].orEmpty()
            if (uid.isBlank()) "Nam" else prefs[genderKey(uid)] ?: "Nam"
        }

    // ==========================================
    // ✍️ WRITE (Ghi dữ liệu)
    // ==========================================

    suspend fun setEnableAnimation(value: Boolean) {
        context.dataStore.edit { it[ENABLE_ANIMATION] = value }
    }

    suspend fun setTempUnit(unit: String) {
        context.dataStore.edit { it[TEMP_UNIT] = unit }
    }

    /** ✅ Gọi sau khi login thành công */
    suspend fun setCurrentUser(uid: String, email: String) {
        context.dataStore.edit {
            it[CURRENT_UID] = uid
            it[CURRENT_EMAIL] = email
        }
    }

    /** ✅ Lưu avatar cho user hiện tại */
    suspend fun saveAvatarForCurrentUser(uri: String) {
        context.dataStore.edit { prefs ->
            val uid = prefs[CURRENT_UID].orEmpty()
            if (uid.isNotBlank()) {
                prefs[avatarKey(uid)] = uri
            }
        }
    }

    /** ✅ Lưu thông tin cá nhân cho user hiện tại */
    suspend fun saveDob(dob: String) {
        context.dataStore.edit { prefs ->
            val uid = prefs[CURRENT_UID].orEmpty()
            if (uid.isNotBlank()) prefs[dobKey(uid)] = dob
        }
    }

    suspend fun savePhone(phone: String) {
        context.dataStore.edit { prefs ->
            val uid = prefs[CURRENT_UID].orEmpty()
            if (uid.isNotBlank()) prefs[phoneKey(uid)] = phone
        }
    }

    suspend fun saveGender(gender: String) {
        context.dataStore.edit { prefs ->
            val uid = prefs[CURRENT_UID].orEmpty()
            if (uid.isNotBlank()) prefs[genderKey(uid)] = gender
        }
    }

    /** ✅ Logout: chỉ xóa session */
    suspend fun clearSession() {
        context.dataStore.edit {
            it.remove(CURRENT_UID)
            it.remove(CURRENT_EMAIL)
        }
    }
}