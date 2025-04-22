package com.basic.petproject.utils

import android.content.Context
import android.content.SharedPreferences

/**
 * Utility class to manage user data in SharedPreferences
 */
class UserPreferences private constructor(context: Context) {
    private val sharedPreferences: SharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    companion object {
        private const val PREF_NAME = "UserPrefs"
        private const val KEY_USER_ID = "userId"
        private const val KEY_USER_NAME = "userName"
        private const val KEY_USER_EMAIL = "userEmail"
        private const val KEY_IS_ADMIN = "isAdmin"

        @Volatile
        private var instance: UserPreferences? = null

        fun getInstance(context: Context): UserPreferences {
            return instance ?: synchronized(this) {
                instance ?: UserPreferences(context.applicationContext).also { instance = it }
            }
        }
    }

    /**
     * Check if the user is an admin
     * @return true if the user is an admin, false otherwise
     */
    fun isAdmin(): Boolean {
        return sharedPreferences.getBoolean(KEY_IS_ADMIN, false)
    }

    /**
     * Get the user ID
     * @return the user ID, or null if not available
     */
    fun getUserId(): String? {
        return sharedPreferences.getString(KEY_USER_ID, null)
    }

    /**
     * Get the user name
     * @return the user name, or empty string if not available
     */
    fun getUserName(): String {
        return sharedPreferences.getString(KEY_USER_NAME, "") ?: ""
    }

    /**
     * Get the user email
     * @return the user email, or empty string if not available
     */
    fun getUserEmail(): String {
        return sharedPreferences.getString(KEY_USER_EMAIL, "") ?: ""
    }

    /**
     * Check if the user is logged in
     * @return true if the user is logged in, false otherwise
     */
    fun isLoggedIn(): Boolean {
        return !getUserId().isNullOrEmpty()
    }

    /**
     * Clear all user data (for logout)
     */
    fun clearUserData() {
        sharedPreferences.edit().clear().apply()
    }
} 