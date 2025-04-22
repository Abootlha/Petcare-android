package com.basic.petproject

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth

class MainActivity : AppCompatActivity() {

    companion object {
        private const val SPLASH_DISPLAY_LENGTH = 2000L // 2 seconds
        private const val TAG = "MainActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Hide the action bar
        supportActionBar?.hide()

        Handler(Looper.getMainLooper()).postDelayed({
            navigateToNextScreen()
        }, SPLASH_DISPLAY_LENGTH)
    }
    
    private fun navigateToNextScreen() {
        try {
            // Ensure Firebase is initialized before using Firebase Auth
            if (FirebaseApp.getApps(this).isEmpty()) {
                Log.d(TAG, "Firebase not initialized, initializing now")
                FirebaseApp.initializeApp(this)
            }
            
            // Check if user is logged in
            val auth = FirebaseAuth.getInstance()
            val currentUser = auth.currentUser
            Log.d(TAG, "User logged in: ${currentUser != null}")
            
            val intent = if (currentUser != null) {
                Intent(this, Dashboard::class.java)
            } else {
                Intent(this, LoginActivity::class.java)
            }
            
            // Add flags to clear the activity stack
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            startActivity(intent)
            finish()
        } catch (e: Exception) {
            Log.e(TAG, "Error during navigation", e)
            // Fallback to login screen if any error occurs
            val intent = Intent(this, LoginActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            startActivity(intent)
            finish()
        }
    }
}