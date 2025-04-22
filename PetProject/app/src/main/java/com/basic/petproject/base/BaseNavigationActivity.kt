package com.basic.petproject.base

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.basic.petproject.Dashboard
import com.basic.petproject.FavoriteActivity
import com.basic.petproject.LocationActivity
import com.basic.petproject.ProfileActivity
import com.basic.petproject.R
import com.basic.petproject.SearchActivity
import com.basic.petproject.utils.CustomBottomNavigationView

abstract class BaseNavigationActivity : AppCompatActivity() {
    protected lateinit var bottomNavigation: CustomBottomNavigationView
    private val TAG = "BaseNavigationActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Don't call setupBottomNavigation here - it will be called by child activities after setContentView
    }

    protected fun setupBottomNavigation() {
        try {
            bottomNavigation = findViewById(R.id.bottomNavigation)
            bottomNavigation.setOnItemSelectedListener { item ->
                if (isFinishing) return@setOnItemSelectedListener false
                
                try {
                    when (item.itemId) {
                        R.id.nav_home -> {
                            if (this !is Dashboard) {
                                navigateToActivity(Dashboard::class.java)
                            }
                            true
                        }
                        R.id.nav_search -> {
                            if (this !is SearchActivity) {
                                navigateToActivity(SearchActivity::class.java)
                            }
                            true
                        }
                        R.id.nav_profile -> {
                            if (this !is ProfileActivity) {
                                navigateToActivity(ProfileActivity::class.java)
                            }
                            true
                        }
                        R.id.nav_location -> {
                            if (this !is LocationActivity) {
                                navigateToActivity(LocationActivity::class.java)
                            }
                            true
                        }
                        R.id.nav_favorite -> {
                            if (this !is FavoriteActivity) {
                                navigateToActivity(FavoriteActivity::class.java)
                            }
                            true
                        }
                        else -> false
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error in navigation", e)
                    false
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error setting up bottom navigation", e)
        }
    }

    protected fun navigateToActivity(activityClass: Class<out AppCompatActivity>) {
        try {
            val intent = Intent(this, activityClass)
            intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
            startActivity(intent)
            finish()
        } catch (e: Exception) {
            Log.e(TAG, "Error navigating to activity: ${activityClass.simpleName}", e)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        try {
            if (::bottomNavigation.isInitialized) {
                outState.putInt("selected_nav_item", bottomNavigation.selectedItemId)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error saving navigation state", e)
        }
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        try {
            if (::bottomNavigation.isInitialized) {
                bottomNavigation.selectedItemId = savedInstanceState.getInt("selected_nav_item")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error restoring navigation state", e)
        }
    }
} 