package com.basic.petproject.repositories

import android.content.Context
import android.util.Log
import com.basic.petproject.models.User
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await

class UserRepository {
    private val db = FirebaseFirestore.getInstance()
    private val usersCollection = db.collection("users")
    private val auth = FirebaseAuth.getInstance()
    private val TAG = "UserRepository"
    
    suspend fun createUserProfile(user: User): Result<String> {
        return try {
            val userId = user.id.ifEmpty { auth.currentUser?.uid ?: "" }
            if (userId.isEmpty()) {
                return Result.failure(Exception("User not authenticated"))
            }
            
            val userWithId = user.copy(id = userId)
            usersCollection.document(userId).set(userWithId).await()
            Result.success(userId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun getUserById(userId: String): Result<User?> {
        return try {
            val document = usersCollection.document(userId).get().await()
            val user = document.toObject(User::class.java)
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun getCurrentUser(): Result<User?> {
        val currentUser = auth.currentUser ?: return Result.success(null)
        return getUserById(currentUser.uid)
    }
    
    suspend fun updateUserProfile(user: User): Result<Unit> {
        return try {
            usersCollection.document(user.id).set(user).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun searchUsersByName(query: String): Result<List<User>> {
        return try {
            val querySnapshot = usersCollection
                .orderBy("name")
                .startAt(query)
                .endAt(query + "\uf8ff")
                .get()
                .await()
                
            val users = querySnapshot.documents.mapNotNull { it.toObject(User::class.java) }
            Result.success(users)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun createUserProfile(userId: String, userProfile: Map<String, Any>): Task<Void> {
        Log.d(TAG, "Starting to create user profile for user ID: $userId")
        val documentReference = usersCollection.document(userId)
        
        return documentReference
            .set(userProfile, SetOptions.merge())
            .addOnSuccessListener {
                Log.d(TAG, "User profile created successfully in Firestore for user ID: $userId")
                try {
                    // Save user data to SharedPreferences
                    val context = db.app.applicationContext
                    val sharedPreferences = context.getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
                    val editor = sharedPreferences.edit()
                    editor.putString("userId", userId)
                    editor.putString("userName", userProfile["name"] as? String ?: "")
                    editor.putString("userEmail", userProfile["email"] as? String ?: "")
                    editor.putBoolean("isAdmin", userProfile["isAdmin"] as? Boolean ?: false)
                    editor.apply()
                    Log.d(TAG, "User preferences saved successfully for user ID: $userId")
                } catch (e: Exception) {
                    Log.e(TAG, "Error saving user preferences for user ID: $userId", e)
                }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error creating user profile in Firestore for user ID: $userId", e)
            }
    }

    fun getUserProfile(userId: String) = usersCollection
        .document(userId)
        .get()

    fun updateUserProfile(userId: String, updates: Map<String, Any>) = usersCollection
        .document(userId)
        .update(updates)

    fun deleteUserProfile(userId: String) = usersCollection
        .document(userId)
        .delete()
}