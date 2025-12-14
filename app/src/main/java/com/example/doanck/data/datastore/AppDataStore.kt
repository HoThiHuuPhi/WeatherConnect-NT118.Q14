package com.example.doanck.data.datastore

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.example.doanck.data.model.PendingSOS // 🟢 MỚI: Import model SOS
import com.google.gson.Gson // 🟢 MỚI: Import Gson
import com.google.gson.reflect.TypeToken // 🟢 MỚI: Để xử lý List
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "app_settings")

class AppDataStore(private val context: Context) {

    private val gson = Gson() // 🟢 MỚI: Khởi tạo Gson

    companion object {
        val ENABLE_ANIMATION = booleanPreferencesKey("enable_animation")
        val TEMP_UNIT = stringPreferencesKey("temp_unit")

        // ✅ Session hiện tại
        val CURRENT_UID = stringPreferencesKey("current_uid")
        val CURRENT_EMAIL = stringPreferencesKey("current_email")

        // 🟢 MỚI: Key lưu danh sách SOS chờ gửi (Lưu dạng chuỗi JSON)
        val SOS_QUEUE_KEY = stringPreferencesKey("sos_queue_json")
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

    val userAvatar: Flow<String?> = context.dataStore.data
        .catch { emit(emptyPreferences()) }
        .map { prefs ->
            val uid = prefs[CURRENT_UID].orEmpty()
            if (uid.isBlank()) null else prefs[avatarKey(uid)]
        }

    val userDob: Flow<String> = context.dataStore.data
        .catch { emit(emptyPreferences()) }
        .map { prefs ->
            val uid = prefs[CURRENT_UID].orEmpty()
            if (uid.isBlank()) "01/01/2000" else prefs[dobKey(uid)] ?: "01/01/2000"
        }

    val userPhone: Flow<String> = context.dataStore.data
        .catch { emit(emptyPreferences()) }
        .map { prefs ->
            val uid = prefs[CURRENT_UID].orEmpty()
            if (uid.isBlank()) "Chưa cập nhật" else prefs[phoneKey(uid)] ?: "Chưa cập nhật"
        }

    val userGender: Flow<String> = context.dataStore.data
        .catch { emit(emptyPreferences()) }
        .map { prefs ->
            val uid = prefs[CURRENT_UID].orEmpty()
            if (uid.isBlank()) "Nam" else prefs[genderKey(uid)] ?: "Nam"
        }

    // 🟢 MỚI: Đọc danh sách SOS đang chờ (từ JSON -> List)
    val sosQueue: Flow<List<PendingSOS>> = context.dataStore.data
        .catch { emit(emptyPreferences()) }
        .map { prefs ->
            val json = prefs[SOS_QUEUE_KEY] ?: "[]"
            val type = object : TypeToken<List<PendingSOS>>() {}.type
            try {
                gson.fromJson(json, type)
            } catch (e: Exception) {
                emptyList() // Tránh crash nếu JSON lỗi
            }
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

    suspend fun setCurrentUser(uid: String, email: String) {
        context.dataStore.edit {
            it[CURRENT_UID] = uid
            it[CURRENT_EMAIL] = email
        }
    }

    suspend fun saveAvatarForCurrentUser(uri: String) {
        context.dataStore.edit { prefs ->
            val uid = prefs[CURRENT_UID].orEmpty()
            if (uid.isNotBlank()) {
                prefs[avatarKey(uid)] = uri
            }
        }
    }

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

    suspend fun clearSession() {
        context.dataStore.edit {
            it.remove(CURRENT_UID)
            it.remove(CURRENT_EMAIL)
        }
    }

    // 🟢 MỚI: Thêm SOS vào hàng chờ (Lưu Offline)
    suspend fun addToQueue(sos: PendingSOS) {
        context.dataStore.edit { prefs ->
            val json = prefs[SOS_QUEUE_KEY] ?: "[]"
            val type = object : TypeToken<List<PendingSOS>>() {}.type
            val currentList: MutableList<PendingSOS> = try {
                gson.fromJson(json, type)
            } catch (e: Exception) {
                mutableListOf()
            }

            currentList.add(sos) // Thêm tin mới vào
            prefs[SOS_QUEUE_KEY] = gson.toJson(currentList) // Lưu lại thành JSON
        }
    }

    // 🟢 MỚI: Xóa sạch hàng chờ (Sau khi đã gửi online thành công)
    suspend fun clearQueue() {
        context.dataStore.edit { prefs ->
            prefs[SOS_QUEUE_KEY] = "[]"
        }
    }
}