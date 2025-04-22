package com.basic.petproject

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.basic.petproject.admin.AdminDashboardActivity
import com.basic.petproject.models.User
import com.basic.petproject.repositories.UserRepository
import com.basic.petproject.utils.CustomBottomNavigationView
import com.basic.petproject.utils.UserPreferences
import androidx.appcompat.widget.SwitchCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import de.hdodenhof.circleimageview.CircleImageView
import kotlinx.coroutines.launch

class ProfileActivity : AppCompatActivity() {
    private lateinit var profileImage: CircleImageView
    private lateinit var userName: TextView
    private lateinit var userEmail: TextView
    private lateinit var userPhone: TextView
    private lateinit var editProfileButton: Button
    private lateinit var myPetsOption: View
    private lateinit var myApplicationsOption: View
    private lateinit var addressOption: View
    private lateinit var notificationsOption: View
    private lateinit var notificationsSwitch: SwitchCompat
    private lateinit var helpOption: View
    private lateinit var adminDashboardOption: View
    private lateinit var logoutButton: Button
    private lateinit var bottomNavigation: CustomBottomNavigationView
    
    private val userRepository = UserRepository()
    private val TAG = "ProfileActivity"
    private lateinit var userPreferences: UserPreferences
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)
        
        // Initialize UserPreferences
        userPreferences = UserPreferences.getInstance(this)
        
        // Initialize views
        profileImage = findViewById(R.id.profileImage)
        userName = findViewById(R.id.userName)
        userEmail = findViewById(R.id.userEmail)
        userPhone = findViewById(R.id.userPhone)
        editProfileButton = findViewById(R.id.editProfileButton)
        myPetsOption = findViewById(R.id.myPetsOption)
        myApplicationsOption = findViewById(R.id.myApplicationsOption)
        addressOption = findViewById(R.id.addressOption)
        notificationsOption = findViewById(R.id.notificationsOption)
        notificationsSwitch = findViewById(R.id.notificationsSwitch)
        helpOption = findViewById(R.id.helpOption)
        adminDashboardOption = findViewById(R.id.adminDashboardOption)
        logoutButton = findViewById(R.id.logoutButton)
        bottomNavigation = findViewById(R.id.bottomNavigation)
        
        // Setup bottom navigation
        bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    val intent = Intent(this, Dashboard::class.java)
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    startActivity(intent)
                    true
                }
                R.id.nav_search -> {
                    val intent = Intent(this, SearchActivity::class.java)
                    intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
                    startActivity(intent)
                    true
                }
                R.id.nav_profile -> {
                    // Already on profile screen
                    true
                }
                R.id.nav_location -> {
                    val intent = Intent(this, LocationActivity::class.java)
                    intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
                    startActivity(intent)
                    true
                }
                R.id.nav_favorite -> {
                    val intent = Intent(this, FavoriteActivity::class.java)
                    intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
                    startActivity(intent)
                    true
                }
                else -> false
            }
        }
        
        // Set the profile item as selected
        bottomNavigation.selectedItemId = R.id.nav_profile
        
        // Load user profile
        loadUserProfile()
        
        // Show/hide admin dashboard based on UserPreferences
        adminDashboardOption.visibility = if (userPreferences.isAdmin()) View.VISIBLE else View.GONE
        
        // Setup click listeners
        setupClickListeners()
    }
    
    private fun loadUserProfile() {
        // First try to get data from UserPreferences
        if (userPreferences.isLoggedIn()) {
            // Display basic user info from UserPreferences
            displayBasicUserInfo()
            
            // Then try to get additional user data from Firestore
            loadUserDataFromFirestore()
        } else {
            // Fallback to Firebase Auth
            val firebaseUser = FirebaseAuth.getInstance().currentUser
            
            if (firebaseUser != null) {
                // Display Firebase Auth data immediately
                displayFirebaseUserData(firebaseUser)
                
                // Then try to get additional user data from Firestore
                loadUserDataFromFirestore()
            } else {
                // User not authenticated, redirect to login
                Toast.makeText(this@ProfileActivity, "User not authenticated", Toast.LENGTH_SHORT).show()
                val intent = Intent(this, LoginActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finish()
            }
        }
    }
    
    private fun displayBasicUserInfo() {
        // Set name from UserPreferences
        userName.text = userPreferences.getUserName()
        
        // Set email from UserPreferences
        userEmail.text = userPreferences.getUserEmail()
    }
    
    private fun displayFirebaseUserData(firebaseUser: FirebaseUser) {
        // Set name from Firebase Auth (if available)
        userName.text = firebaseUser.displayName ?: "No Name"
        
        // Set email from Firebase Auth
        userEmail.text = firebaseUser.email ?: "No Email"
        
        // Set phone from Firebase Auth (if available)
        userPhone.text = firebaseUser.phoneNumber ?: "No Phone"
        
        // Load profile image if available
        val photoUrl = firebaseUser.photoUrl
        if (photoUrl != null) {
            // TODO: Implement image loading with a library like Glide or Picasso
            // For now, we'll use the default image
        }
    }
    
    private fun loadUserDataFromFirestore() {
        lifecycleScope.launch {
            try {
                val result = userRepository.getCurrentUser()
                
                if (result.isSuccess) {
                    val user = result.getOrNull()
                    if (user != null) {
                        updateUI(user)
                    }
                } else {
                    val exception = result.exceptionOrNull()
                    Log.e(TAG, "Error loading additional user profile data", exception)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Exception in loadUserDataFromFirestore", e)
            }
        }
    }
    
    private fun updateUI(user: User) {
        userName.text = user.name
        userEmail.text = user.email
        userPhone.text = user.phone
        
        // Show admin dashboard option if user is admin
        adminDashboardOption.visibility = if (user.isAdmin) View.VISIBLE else View.GONE
        
        // Load profile image if available
        if (user.profileImageUrl.isNotEmpty()) {
            // TODO: Implement image loading with a library like Glide or Picasso
            // For now, we'll use the default image
        }
    }
    
    private fun setupClickListeners() {
        editProfileButton.setOnClickListener {
            // TODO: Implement edit profile functionality
            Toast.makeText(this, "Edit Profile feature coming soon", Toast.LENGTH_SHORT).show()
        }
        
        myPetsOption.setOnClickListener {
            startActivity(Intent(this, PetListingActivity::class.java))
        }
        
        myApplicationsOption.setOnClickListener {
            startActivity(Intent(this, MyApplicationsActivity::class.java))
        }
        
        addressOption.setOnClickListener {
            // TODO: Implement address management functionality
            Toast.makeText(this, "Address management feature coming soon", Toast.LENGTH_SHORT).show()
        }
        
        notificationsOption.setOnClickListener {
            notificationsSwitch.toggle()
        }
        
        helpOption.setOnClickListener {
            // TODO: Implement help & support functionality
            Toast.makeText(this, "Help & Support feature coming soon", Toast.LENGTH_SHORT).show()
        }
        
        adminDashboardOption.setOnClickListener {
            startActivity(Intent(this, AdminDashboardActivity::class.java))
        }
        
        logoutButton.setOnClickListener {
            // Sign out from Firebase Authentication
            FirebaseAuth.getInstance().signOut()
            
            // Clear user preferences
            userPreferences.clearUserData()
            
            // Navigate to login screen
            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }
    }
}