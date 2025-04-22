package com.basic.petproject.utils

import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment

/**
 * Utility class to handle permissions in the app.
 * This class provides methods to check, request, and handle permissions.
 */
class PermissionHandler {
    companion object {
        // Permission sets by feature
        val STORAGE_PERMISSIONS = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arrayOf(android.Manifest.permission.READ_MEDIA_IMAGES)
        } else {
            arrayOf(android.Manifest.permission.READ_EXTERNAL_STORAGE)
        }

        val SMS_PERMISSIONS = arrayOf(
            android.Manifest.permission.RECEIVE_SMS,
            android.Manifest.permission.READ_SMS
        )

        val LOCATION_PERMISSIONS = arrayOf(
            android.Manifest.permission.ACCESS_FINE_LOCATION,
            android.Manifest.permission.ACCESS_COARSE_LOCATION
        )

        /**
         * Check if all permissions in the given set are granted
         */
        fun hasPermissions(context: Context, permissions: Array<String>): Boolean {
            for (permission in permissions) {
                if (ContextCompat.checkSelfPermission(
                        context,
                        permission
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    return false
                }
            }
            return true
        }

        /**
         * Request permissions if not granted
         */
        fun requestPermissionsIfNeeded(
            activity: Activity,
            permissions: Array<String>,
            requestCode: Int
        ): Boolean {
            if (!hasPermissions(activity, permissions)) {
                ActivityCompat.requestPermissions(activity, permissions, requestCode)
                return false
            }
            return true
        }

        /**
         * Set up permission launcher for an Activity
         */
        fun setupPermissionLauncher(
            activity: AppCompatActivity,
            onPermissionResult: (Boolean) -> Unit
        ): ActivityResultLauncher<Array<String>> {
            return activity.registerForActivityResult(
                ActivityResultContracts.RequestMultiplePermissions()
            ) { permissions ->
                val allGranted = permissions.values.all { it }
                onPermissionResult(allGranted)
            }
        }

        /**
         * Set up permission launcher for a Fragment
         */
        fun setupPermissionLauncher(
            fragment: Fragment,
            onPermissionResult: (Boolean) -> Unit
        ): ActivityResultLauncher<Array<String>> {
            return fragment.registerForActivityResult(
                ActivityResultContracts.RequestMultiplePermissions()
            ) { permissions ->
                val allGranted = permissions.values.all { it }
                onPermissionResult(allGranted)
            }
        }
    }
} 