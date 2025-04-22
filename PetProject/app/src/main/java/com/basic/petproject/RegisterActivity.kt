package com.basic.petproject

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.basic.petproject.databinding.ActivityRegisterBinding
import com.basic.petproject.repositories.UserRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser

class RegisterActivity : AppCompatActivity() {
    private lateinit var binding: ActivityRegisterBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var userRepository: UserRepository
    private val TAG = "RegisterActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        userRepository = UserRepository()

        binding.buttonRegister.setOnClickListener {
            val username = binding.editTextUsername.text.toString()
            val email = binding.editTextEmailAddress.text.toString()
            val password = binding.editTextPassword.text.toString()

            if (validateInputs(username, email, password)) {
                showLoading(true)
                registerUser(username, email, password)
            }
        }
    }

    private fun validateInputs(username: String, email: String, password: String): Boolean {
        if (username.isEmpty()) {
            binding.editTextUsername.error = "Username is required"
            return false
        }

        if (email.isEmpty()) {
            binding.editTextEmailAddress.error = "Email is required"
            return false
        }

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.editTextEmailAddress.error = "Please enter a valid email address"
            return false
        }

        if (password.isEmpty()) {
            binding.editTextPassword.error = "Password is required"
            return false
        }

        if (password.length < 6) {
            binding.editTextPassword.error = "Password must be at least 6 characters"
            return false
        }

        return true
    }

    private fun showLoading(show: Boolean) {
        binding.progressBar.visibility = if (show) View.VISIBLE else View.GONE
        binding.buttonRegister.isEnabled = !show
    }

    private fun registerUser(username: String, email: String, password: String) {
        Log.d(TAG, "Starting user registration with email: $email")
        
        // Clear any existing users first to prevent conflicts
        if (auth.currentUser != null) {
            auth.signOut()
        }
        
        auth.createUserWithEmailAndPassword(email, password)
            .addOnSuccessListener { result ->
                Log.d(TAG, "Firebase Auth account created successfully")
                val user = result.user
                if (user != null) {
                    createUserProfile(user, username)
                } else {
                    Log.e(TAG, "Firebase Auth account created but user is null")
                    showLoading(false)
                    showToast("Registration failed: User is null")
                }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Firebase Auth account creation failed", e)
                showLoading(false)
                showToast("Registration failed: ${e.message}")
            }
    }

    private fun createUserProfile(user: FirebaseUser, username: String) {
        Log.d(TAG, "Creating user profile for user ID: ${user.uid}")
        val userProfile = mapOf(
            "name" to username,
            "email" to (user.email ?: ""),
            "createdAt" to System.currentTimeMillis(),
            "isAdmin" to false
        )

        val task = userRepository.createUserProfile(user.uid, userProfile)
        
        task.addOnCompleteListener(this) { task ->
            if (task.isSuccessful) {
                Log.d(TAG, "User profile created successfully in Firestore")
                
                // Show toast message first
                Toast.makeText(applicationContext, "Registration successful!", Toast.LENGTH_LONG).show()
                
                // Hide the loading indicator
                showLoading(false)
                
                // Then navigate with a longer delay to ensure toast is visible
                Handler(Looper.getMainLooper()).postDelayed({
                    Log.d(TAG, "Navigating to Login activity after delay")
                    navigateToLogin()
                }, 2500) // Increase delay to 2.5 seconds
            } else {
                Log.e(TAG, "Error creating user profile in Firestore", task.exception)
                showLoading(false)
                showToast("Failed to create profile: ${task.exception?.message}")
            }
        }
    }

    private fun showToast(message: String) {
        Log.d(TAG, "Showing toast: $message")
        
        // Use applicationContext to prevent context leaks
        // and make sure we're on the main thread
        Handler(Looper.getMainLooper()).post {
            Toast.makeText(applicationContext, message, Toast.LENGTH_LONG).show()
        }
    }

    private fun navigateToLogin() {
        Log.d(TAG, "Starting navigation to LoginActivity")
        try {
            val intent = Intent(this@RegisterActivity, LoginActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                    finish()
            Log.d(TAG, "Navigation to LoginActivity completed")
        } catch (e: Exception) {
            Log.e(TAG, "Error navigating to LoginActivity", e)
            // If navigation fails, show a toast and retry with simpler intent
            showToast("Navigation error. Trying alternative method...")
            try {
                startActivity(Intent(this@RegisterActivity, LoginActivity::class.java))
                finish()
            } catch (e2: Exception) {
                Log.e(TAG, "Failed alternative navigation method", e2)
            }
            }
    }

    fun goToLogin(view: View) {
        startActivity(Intent(this, LoginActivity::class.java))
        finish()
    }
}