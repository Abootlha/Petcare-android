package com.basic.petproject.utils

import android.content.Context
import android.content.SharedPreferences

class SharedPreferencesManager(context: Context) {
    
    private val sharedPreferences: SharedPreferences = context.getSharedPreferences(
        PREFS_NAME, Context.MODE_PRIVATE
    )
    
    // User profile methods
    fun saveUserId(userId: String) {
        sharedPreferences.edit().putString(KEY_USER_ID, userId).apply()
    }
    
    fun getUserId(): String {
        return sharedPreferences.getString(KEY_USER_ID, "") ?: ""
    }
    
    fun saveUserName(name: String) {
        sharedPreferences.edit().putString(KEY_USER_NAME, name).apply()
    }
    
    fun getUserName(): String {
        return sharedPreferences.getString(KEY_USER_NAME, "") ?: ""
    }
    
    fun saveUserEmail(email: String) {
        sharedPreferences.edit().putString(KEY_USER_EMAIL, email).apply()
    }
    
    fun getUserEmail(): String {
        return sharedPreferences.getString(KEY_USER_EMAIL, "") ?: ""
    }
    
    fun saveProfileImageUrl(url: String) {
        sharedPreferences.edit().putString(KEY_PROFILE_IMAGE_URL, url).apply()
    }
    
    fun getProfileImageUrl(): String {
        return sharedPreferences.getString(KEY_PROFILE_IMAGE_URL, "") ?: ""
    }
    
    // Admin status methods
    fun saveIsAdmin(isAdmin: Boolean) {
        sharedPreferences.edit().putBoolean(KEY_IS_ADMIN, isAdmin).apply()
    }
    
    fun getIsAdmin(): Boolean {
        return sharedPreferences.getBoolean(KEY_IS_ADMIN, false)
    }
    
    // Theme preferences
    fun saveDarkModeEnabled(enabled: Boolean) {
        sharedPreferences.edit().putBoolean(KEY_DARK_MODE, enabled).apply()
    }
    
    fun getDarkModeEnabled(): Boolean {
        return sharedPreferences.getBoolean(KEY_DARK_MODE, false)
    }
    
    // Notification preferences
    fun saveNotificationsEnabled(enabled: Boolean) {
        sharedPreferences.edit().putBoolean(KEY_NOTIFICATIONS_ENABLED, enabled).apply()
    }
    
    fun getNotificationsEnabled(): Boolean {
        return sharedPreferences.getBoolean(KEY_NOTIFICATIONS_ENABLED, true)
    }
    
    // App settings
    fun saveLocation(location: String) {
        sharedPreferences.edit().putString(KEY_LOCATION, location).apply()
    }
    
    fun getLocation(): String {
        return sharedPreferences.getString(KEY_LOCATION, "") ?: ""
    }
    
    fun saveLanguage(language: String) {
        sharedPreferences.edit().putString(KEY_LANGUAGE, language).apply()
    }
    
    fun getLanguage(): String {
        return sharedPreferences.getString(KEY_LANGUAGE, "en") ?: "en"
    }
    
    // Session management
    fun clearUserSession() {
        sharedPreferences.edit()
            .remove(KEY_USER_ID)
            .remove(KEY_USER_NAME)
            .remove(KEY_USER_EMAIL)
            .remove(KEY_PROFILE_IMAGE_URL)
            .remove(KEY_IS_ADMIN)
            .apply()
    }
    
    companion object {
        private const val PREFS_NAME = "pet_project_prefs"
        
        // User keys
        private const val KEY_USER_ID = "user_id"
        private const val KEY_USER_NAME = "user_name"
        private const val KEY_USER_EMAIL = "user_email"
        private const val KEY_PROFILE_IMAGE_URL = "profile_image_url"
        private const val KEY_IS_ADMIN = "is_admin"
        
        // Settings keys
        private const val KEY_DARK_MODE = "dark_mode"
        private const val KEY_NOTIFICATIONS_ENABLED = "notifications_enabled"
        private const val KEY_LOCATION = "location"
        private const val KEY_LANGUAGE = "language"
    }
} 