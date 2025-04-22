package com.basic.petproject

import android.app.Application
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.ktx.Firebase
import com.google.firebase.ktx.initialize

class PetProjectApplication : Application() {
    companion object {
        private const val TAG = "PetProjectApplication"
    }

    override fun onCreate() {
        super.onCreate()
        
        try {
            // Check if Firebase is already initialized
            if (FirebaseApp.getApps(this).isEmpty()) {
                Log.d(TAG, "Initializing Firebase")
                FirebaseApp.initializeApp(this)
            } else {
                Log.d(TAG, "Firebase already initialized")
            }
            
            // Verify initialization was successful
            val app = FirebaseApp.getInstance()
            Log.d(TAG, "Firebase initialized successfully: ${app.name}")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize Firebase", e)
        }
    }
} 