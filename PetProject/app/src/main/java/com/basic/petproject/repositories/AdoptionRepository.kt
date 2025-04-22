package com.basic.petproject.repositories

import com.basic.petproject.models.AdoptionApplication
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await
import java.util.UUID

class AdoptionRepository {
    private val db = FirebaseFirestore.getInstance()
    private val applicationsCollection = db.collection("adoptionApplications")
    private val petsCollection = db.collection("pets")
    private val auth = FirebaseAuth.getInstance()
    
    // Submit a new adoption application
    suspend fun submitApplication(application: AdoptionApplication): Result<AdoptionApplication> {
        return try {
            val applicationId = application.id.ifEmpty { UUID.randomUUID().toString() }
            val newApplication = application.copy(
                id = applicationId,
                applicantId = auth.currentUser?.uid ?: "",
                applicantName = auth.currentUser?.displayName ?: "",
                applicantEmail = auth.currentUser?.email ?: "",
                applicantPhone = auth.currentUser?.phoneNumber ?: "",
                applicationDate = System.currentTimeMillis(),
                lastUpdated = System.currentTimeMillis()
            )
            
            // Save to Firestore
            applicationsCollection.document(applicationId).set(newApplication).await()
            
            // Update pet status to "Pending" if it's the first application
            petsCollection.document(application.petId)
                .update("applicationStatus", "Pending")
                .await()
                
            Result.success(newApplication)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // Get all applications for a specific pet
    suspend fun getApplicationsForPet(petId: String): Result<List<AdoptionApplication>> {
        return try {
            val snapshot = applicationsCollection
                .whereEqualTo("petId", petId)
                .orderBy("applicationDate", Query.Direction.DESCENDING)
                .get()
                .await()
                
            val applications = snapshot.documents.mapNotNull { doc -> 
                doc.toObject(AdoptionApplication::class.java)
            }
            
            Result.success(applications)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // Get all applications submitted by the current user
    suspend fun getUserApplications(): Result<List<AdoptionApplication>> {
        return try {
            val userId = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("User not logged in"))
            
            val snapshot = applicationsCollection
                .whereEqualTo("applicantId", userId)
                .orderBy("applicationDate", Query.Direction.DESCENDING)
                .get()
                .await()
                
            val applications = snapshot.documents.mapNotNull { doc -> 
                doc.toObject(AdoptionApplication::class.java)
            }
            
            Result.success(applications)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // Get all applications (for admin)
    suspend fun getAllApplications(): Result<List<AdoptionApplication>> {
        return try {
            val snapshot = applicationsCollection
                .orderBy("applicationDate", Query.Direction.DESCENDING)
                .get()
                .await()
                
            val applications = snapshot.documents.mapNotNull { doc -> 
                doc.toObject(AdoptionApplication::class.java)
            }
            
            Result.success(applications)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // Update application status
    suspend fun updateApplicationStatus(applicationId: String, newStatus: String, notes: String = ""): Result<Boolean> {
        return try {
            val updates = hashMapOf<String, Any>(
                "status" to newStatus,
                "lastUpdated" to System.currentTimeMillis()
            )
            
            if (notes.isNotEmpty()) {
                updates["adminNotes"] = notes
            }
            
            if (newStatus == "Rejected" && notes.isNotEmpty()) {
                updates["rejectionReason"] = notes
            }
            
            applicationsCollection.document(applicationId)
                .update(updates)
                .await()
                
            // If application is approved, update pet status to Adopted
            if (newStatus == "Approved") {
                val application = applicationsCollection.document(applicationId).get().await()
                    .toObject(AdoptionApplication::class.java)
                    
                application?.let {
                    petsCollection.document(it.petId)
                        .update(
                            "applicationStatus", "Adopted",
                            "isAdopted", true
                        )
                        .await()
                }
            }
            
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
} 