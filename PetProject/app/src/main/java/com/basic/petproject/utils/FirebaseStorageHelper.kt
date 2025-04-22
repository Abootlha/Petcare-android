package com.basic.petproject.utils

import android.net.Uri
import android.util.Log
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.UUID

/**
 * Helper class for Firebase Storage operations
 */
class FirebaseStorageHelper {
    private val storage = FirebaseStorage.getInstance()
    private val TAG = "FirebaseStorageHelper"
    
    /**
     * Uploads an image to Firebase Storage
     * @param imageUri The URI of the image to upload
     * @param folderPath The folder path within Firebase Storage to store the image
     * @return The download URL of the uploaded image without access tokens
     */
    suspend fun uploadImage(imageUri: Uri, folderPath: String): String = withContext(Dispatchers.IO) {
        try {
            val filename = UUID.randomUUID().toString()
            val reference = storage.reference.child("$folderPath/$filename")
            
            // Verify the URI is valid and accessible
            if (imageUri.toString().isBlank()) {
                Log.e(TAG, "Image URI is blank or invalid")
                throw IllegalArgumentException("Image URI is blank or invalid")
            }
            
            // Attempt the upload with proper error handling
            val uploadTask = reference.putFile(imageUri).await()
            
            // Get the download URL
            val fullUrl = uploadTask.storage.downloadUrl.await().toString()
            
            // Ensure the URL has the alt=media parameter for direct download access
            val cleanUrl = if (fullUrl.contains("?")) {
                if (fullUrl.contains("alt=media")) {
                    fullUrl
                } else {
                    fullUrl + "&alt=media"
                }
            } else {
                fullUrl + "?alt=media"
            }
            
            Log.d(TAG, "Image uploaded successfully. Clean URL: $cleanUrl")
            return@withContext cleanUrl
            
        } catch (e: Exception) {
            // Log the error with more details
            Log.e(TAG, "Error uploading image: ${e.message}", e)
            return@withContext ""
        }
    }
    
    /**
     * Deletes an image from Firebase Storage
     * @param imageUrl The URL of the image to delete
     * @return True if deletion was successful, false otherwise
     */
    suspend fun deleteImage(imageUrl: String): Boolean = withContext(Dispatchers.IO) {
        try {
            if (imageUrl.isNotEmpty()) {
                // Remove any access token from the URL
                val cleanUrl = if (imageUrl.contains("?")) {
                    imageUrl.split("?")[0]
                } else {
                    imageUrl
                }
                
                val reference = storage.getReferenceFromUrl(cleanUrl)
                reference.delete().await()
                Log.d(TAG, "Image deleted successfully: $cleanUrl")
                return@withContext true
            }
            return@withContext false
        } catch (e: Exception) {
            // Log the error
            Log.e(TAG, "Error deleting image: ${e.message}", e)
            return@withContext false
        }
    }
} 