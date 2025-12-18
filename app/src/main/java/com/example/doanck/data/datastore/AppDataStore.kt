package com.example.doanck.data.datastore

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.example.doanck.data.model.SOSRequest
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "app_settings")

class AppDataStore(private val context: Context) {
    private val gson = Gson()
    companion object {
        val ENABLE_ANIMATION = booleanPreferencesKey("enable_animation")
        val TEMP_UNIT = stringPreferencesKey("temp_unit")
        val ENABLE_NOTIFICATIONS = booleanPreferencesKey("enable_notifications")

        val CURRENT_UID = stringPreferencesKey("current_uid")
        val CURRENT_EMAIL = stringPreferencesKey("current_email")

        val SOS_QUEUE_KEY = stringPreferencesKey("sos_queue_json")
        val sosRequestAdapter: Gson = Gson()
    }

    // keys động
    private fun avatarKey(uid: String) = stringPreferencesKey("user_avatar_uri_$uid")
    private fun dobKey(uid: String) = stringPreferencesKey("user_dob_$uid")
    private fun phoneKey(uid: String) = stringPreferencesKey("user_phone_$uid")
    private fun genderKey(uid: String) = stringPreferencesKey("user_gender_$uid")

    // Read
    val currentUid: Flow<String?> = context.dataStore.data.catch { emit(emptyPreferences()) }.map { it[CURRENT_UID] }

    val enableAnimation: Flow<Boolean> = context.dataStore.data.catch { emit(emptyPreferences()) }.map { it[ENABLE_ANIMATION] ?: true }
    val tempUnit: Flow<String> = context.dataStore.data.catch { emit(emptyPreferences()) }.map { it[TEMP_UNIT] ?: "C" }
    val enableNotifications: Flow<Boolean> = context.dataStore.data.catch { emit(emptyPreferences()) }.map { it[ENABLE_NOTIFICATIONS] ?: true }
    val userEmail: Flow<String> = context.dataStore.data.catch { emit(emptyPreferences()) }.map { it[CURRENT_EMAIL] ?: "" }
    val userAvatar: Flow<String?> = context.dataStore.data.catch { emit(emptyPreferences()) }.map { prefs -> val uid = prefs[CURRENT_UID].orEmpty(); if (uid.isBlank()) null else prefs[avatarKey(uid)] }
    val userDob: Flow<String> = context.dataStore.data.catch { emit(emptyPreferences()) }.map { prefs -> val uid = prefs[CURRENT_UID].orEmpty(); if (uid.isBlank()) "01/01/2000" else prefs[dobKey(uid)] ?: "01/01/2000" }
    val userPhone: Flow<String> = context.dataStore.data.catch { emit(emptyPreferences()) }.map { prefs -> val uid = prefs[CURRENT_UID].orEmpty(); if (uid.isBlank()) "Chưa cập nhật" else prefs[phoneKey(uid)] ?: "Chưa cập nhật" }
    val userGender: Flow<String> = context.dataStore.data.catch { emit(emptyPreferences()) }.map { prefs -> val uid = prefs[CURRENT_UID].orEmpty(); if (uid.isBlank()) "Nam" else prefs[genderKey(uid)] ?: "Nam" }
    val sosQueue: Flow<List<String>> = context.dataStore.data
        .catch { emit(emptyPreferences()) }
        .map { prefs ->
            val json = prefs[SOS_QUEUE_KEY] ?: "[]"
            val type = object : TypeToken<List<String>>() {}.type
            try {
                sosRequestAdapter.fromJson(json, type)
            } catch (e: Exception) {
                emptyList()
            }
        }

    // Write
    // Lưu phiên đăng nhập
    suspend fun setCurrentUser(uid: String, email: String) {
        context.dataStore.edit {
            it[CURRENT_UID] = uid
            it[CURRENT_EMAIL] = email
        }
    }

    // Xóa phiên đăng nhập (Logout)
    suspend fun clearSession() {
        context.dataStore.edit {
            it.remove(CURRENT_UID)
            it.remove(CURRENT_EMAIL)
        }
    }

    suspend fun setEnableAnimation(value: Boolean) { context.dataStore.edit { it[ENABLE_ANIMATION] = value } }
    suspend fun setEnableNotifications(value: Boolean) { context.dataStore.edit { it[ENABLE_NOTIFICATIONS] = value } }
    suspend fun setTempUnit(unit: String) { context.dataStore.edit { it[TEMP_UNIT] = unit } }
    suspend fun saveAvatarForCurrentUser(uri: String) { context.dataStore.edit { prefs -> val uid = prefs[CURRENT_UID].orEmpty(); if (uid.isNotBlank()) prefs[avatarKey(uid)] = uri } }
    suspend fun saveDob(dob: String) { context.dataStore.edit { prefs -> val uid = prefs[CURRENT_UID].orEmpty(); if (uid.isNotBlank()) prefs[dobKey(uid)] = dob } }
    suspend fun savePhone(phone: String) { context.dataStore.edit { prefs -> val uid = prefs[CURRENT_UID].orEmpty(); if (uid.isNotBlank()) prefs[phoneKey(uid)] = phone } }
    suspend fun saveGender(gender: String) { context.dataStore.edit { prefs -> val uid = prefs[CURRENT_UID].orEmpty(); if (uid.isNotBlank()) prefs[genderKey(uid)] = gender } }
    suspend fun addToQueue(sos: SOSRequest) {
        context.dataStore.edit { prefs ->
            val sosJsonString = sosRequestAdapter.toJson(sos) // Chuyển object thành JSON

            val currentQueueJson = prefs[SOS_QUEUE_KEY] ?: "[]"
            val type = object : TypeToken<MutableList<String>>() {}.type
            val currentList: MutableList<String> = try {
                sosRequestAdapter.fromJson(currentQueueJson, type)
            } catch (e: Exception) {
                mutableListOf()
            }

            currentList.add(sosJsonString) // Thêm chuỗi JSON vào danh sách
            prefs[SOS_QUEUE_KEY] = sosRequestAdapter.toJson(currentList)
        }
    }

    suspend fun removeFromQueue(sosRequestString: String) {
        context.dataStore.edit { prefs ->
            val currentQueueJson = prefs[SOS_QUEUE_KEY] ?: "[]"
            val type = object : TypeToken<MutableList<String>>() {}.type
            val currentList: MutableList<String> = try {
                sosRequestAdapter.fromJson(currentQueueJson, type)
            } catch (e: Exception) {
                mutableListOf()
            }

            currentList.remove(sosRequestString)
            prefs[SOS_QUEUE_KEY] = sosRequestAdapter.toJson(currentList)
        }
    }

    suspend fun clearQueue() { context.dataStore.edit { prefs -> prefs[SOS_QUEUE_KEY] = "[]" } }
}