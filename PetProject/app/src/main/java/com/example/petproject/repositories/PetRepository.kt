package com.example.petproject.repositories

import com.example.petproject.models.Pet
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await

class PetRepository {
    private val db = FirebaseFirestore.getInstance()
    private val petsCollection = db.collection("pets")
    
    suspend fun addPet(pet: Pet): Result<String> {
        return try {
            val petWithId = if (pet.id.isEmpty()) {
                val newId = petsCollection.document().id
                pet.copy(id = newId)
            } else {
                pet
            }
            
            petsCollection.document(petWithId.id).set(petWithId).await()
            Result.success(petWithId.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun getPetById(petId: String): Result<Pet?> {
        return try {
            val document = petsCollection.document(petId).get().await()
            val pet = document.toObject(Pet::class.java)
            Result.success(pet)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun getAllPets(): Result<List<Pet>> {
        return try {
            val querySnapshot = petsCollection
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .await()
                
            val pets = querySnapshot.documents.mapNotNull { it.toObject(Pet::class.java) }
            Result.success(pets)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun getPetsByOwnerId(ownerId: String): Result<List<Pet>> {
        return try {
            val querySnapshot = petsCollection
                .whereEqualTo("ownerId", ownerId)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .await()
                
            val pets = querySnapshot.documents.mapNotNull { it.toObject(Pet::class.java) }
            Result.success(pets)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun updatePet(pet: Pet): Result<Unit> {
        return try {
            petsCollection.document(pet.id).set(pet).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun deletePet(petId: String): Result<Unit> {
        return try {
            petsCollection.document(petId).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun searchPets(query: String): Result<List<Pet>> {
        return try {
            // Firestore doesn't support full-text search natively
            // This is a simple implementation that searches by name, type, or breed
            val querySnapshot = petsCollection
                .orderBy("name")
                .startAt(query)
                .endAt(query + "\uf8ff")
                .get()
                .await()
                
            val petsByName = querySnapshot.documents.mapNotNull { it.toObject(Pet::class.java) }
            
            val querySnapshot2 = petsCollection
                .orderBy("type")
                .startAt(query)
                .endAt(query + "\uf8ff")
                .get()
                .await()
                
            val petsByType = querySnapshot2.documents.mapNotNull { it.toObject(Pet::class.java) }
            
            val querySnapshot3 = petsCollection
                .orderBy("breed")
                .startAt(query)
                .endAt(query + "\uf8ff")
                .get()
                .await()
                
            val petsByBreed = querySnapshot3.documents.mapNotNull { it.toObject(Pet::class.java) }
            
            // Combine results and remove duplicates
            val allPets = (petsByName + petsByType + petsByBreed).distinctBy { it.id }
            Result.success(allPets)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}