package com.example.petproject.repositories

import com.example.petproject.models.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class UserRepository {
    private val db = FirebaseFirestore.getInstance()
    private val usersCollection = db.collection("users")
    private val auth = FirebaseAuth.getInstance()
    
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
}