package com.basic.petproject.repositories

import android.util.Log
import com.basic.petproject.models.Pet
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.lang.Exception

class PetRepository {
    private val firestore = FirebaseFirestore.getInstance()
    private val petsCollection = firestore.collection("pets")
    private val TAG = "PetRepository"
    private val auth = FirebaseAuth.getInstance()

    // Helper method to check if a pet belongs to current user
    private fun isCurrentUsersPet(pet: Pet): Boolean {
        val currentUserId = auth.currentUser?.uid
        val petOwnerId = pet.ownerId
        
        val isMine = currentUserId != null && petOwnerId == currentUserId
        Log.d(TAG, "Checking pet ownership: petId=${pet.id}, ownerId=$petOwnerId, currentUserId=$currentUserId, isMine=$isMine")
        return isMine
    }

    fun getAllPets(callback: (List<Pet>?) -> Unit) {
        petsCollection.orderBy("createdAt", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { documents ->
                try {
                    val pets = documents.mapNotNull { doc ->
                        doc.toObject(Pet::class.java).apply { id = doc.id }
                    }
                    callback(pets)
                } catch (e: Exception) {
                    Log.e(TAG, "Error parsing pets", e)
                    callback(null)
                }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error getting all pets", e)
                callback(null)
            }
    }

    fun getFeaturedPets(callback: (List<Pet>?) -> Unit) {
        petsCollection.whereEqualTo("featured", true)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { documents ->
                try {
                    val pets = documents.mapNotNull { doc ->
                        doc.toObject(Pet::class.java).apply { id = doc.id }
                    }
                    callback(pets)
                } catch (e: Exception) {
                    Log.e(TAG, "Error parsing featured pets", e)
                    callback(null)
                }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error getting featured pets", e)
                callback(null)
            }
    }

    fun getPetsByCategory(category: String, callback: (List<Pet>?) -> Unit) {
        petsCollection.whereEqualTo("type", category.lowercase())
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { documents ->
                try {
                    val pets = documents.mapNotNull { doc ->
                        doc.toObject(Pet::class.java).apply { id = doc.id }
                    }
                    callback(pets)
                } catch (e: Exception) {
                    Log.e(TAG, "Error parsing pets by category", e)
                    callback(null)
                }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error getting pets by category", e)
                callback(null)
            }
    }

    fun getPetById(petId: String, callback: (Pet?) -> Unit) {
        petsCollection.document(petId)
            .get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    try {
                        val pet = document.toObject(Pet::class.java)?.apply {
                            id = document.id
                        }
                        callback(pet)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error converting document to Pet", e)
                        callback(null)
                    }
            } else {
                    callback(null)
                }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error getting pet by ID", e)
                callback(null)
            }
    }

    fun searchPets(query: String, callback: (List<Pet>?) -> Unit) {
        // Firebase doesn't support full-text search directly
        // Search for matches in name, breed, or description
        // This is a simple implementation and could be improved with Algolia or other search services
        petsCollection
            .orderBy("name")
            .get()
            .addOnSuccessListener { documents ->
                val lowercaseQuery = query.lowercase()
                val petList = documents.mapNotNull { document ->
                    try {
                        val pet = document.toObject(Pet::class.java).apply {
                            id = document.id
                        }
                        
                        // Check if the pet matches the search query
                        if (pet.name.lowercase().contains(lowercaseQuery) ||
                            pet.breed?.lowercase()?.contains(lowercaseQuery) == true ||
                            pet.description?.lowercase()?.contains(lowercaseQuery) == true) {
                            pet
                        } else {
                            null
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error converting document to Pet", e)
                        null
                    }
                }
                callback(petList)
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error searching pets", e)
                callback(null)
            }
    }

    fun addPet(pet: Pet, callback: (Boolean, String?) -> Unit) {
        // Ensure all fields are valid before adding
        val petToAdd = pet.copy(
            id = null,
            createdAt = System.currentTimeMillis()
        )
        
        petsCollection
            .add(petToAdd)
            .addOnSuccessListener { documentReference ->
                callback(true, documentReference.id)
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error adding pet", e)
                callback(false, e.message)
            }
    }

    suspend fun addPet(pet: Pet): Result<String> = withContext(Dispatchers.IO) {
        try {
            val petToAdd = pet.copy(
                id = null,
                createdAt = System.currentTimeMillis()
            )
            
            val documentReference = petsCollection.add(petToAdd).await()
            Result.success(documentReference.id)
        } catch (e: Exception) {
            Log.e(TAG, "Error adding pet", e)
            Result.failure(e)
        }
    }
    
    fun updatePet(pet: Pet, callback: (Boolean, String?) -> Unit) {
        pet.id?.let { petId ->
            petsCollection.document(petId)
                .set(pet)
                .addOnSuccessListener {
                    callback(true, null)
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Error updating pet", e)
                    callback(false, e.message)
                }
        } ?: callback(false, "Pet ID is null")
    }

    fun deletePet(petId: String, callback: (Boolean, String?) -> Unit) {
        petsCollection.document(petId)
            .delete()
            .addOnSuccessListener {
                callback(true, null)
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error deleting pet", e)
                callback(false, e.message)
            }
    }

    fun toggleFavorite(petId: String, isFavorite: Boolean, userId: String, callback: (Boolean) -> Unit) {
        val userFavoritesCollection = firestore.collection("userFavorites")
        
        if (isFavorite) {
            // Add to favorites
            val favorite = hashMapOf(
                "userId" to userId,
                "petId" to petId,
                "createdAt" to System.currentTimeMillis()
            )
            
            userFavoritesCollection.add(favorite)
                .addOnSuccessListener {
                    callback(true)
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Error adding to favorites", e)
                    callback(false)
                }
        } else {
            // Remove from favorites
            userFavoritesCollection
                .whereEqualTo("userId", userId)
                .whereEqualTo("petId", petId)
                .get()
                .addOnSuccessListener { documents ->
                    val batch = firestore.batch()
                    for (document in documents) {
                        batch.delete(document.reference)
                    }
                    batch.commit()
                        .addOnSuccessListener {
                            callback(true)
                        }
                        .addOnFailureListener { e ->
                            Log.e(TAG, "Error removing from favorites", e)
                            callback(false)
                        }
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Error finding favorites to remove", e)
                    callback(false)
                }
        }
    }

    fun getUserFavorites(userId: String, callback: (List<Pet>?) -> Unit) {
        firestore.collection("userFavorites")
            .whereEqualTo("userId", userId)
            .get()
            .addOnSuccessListener { documents ->
                if (documents.isEmpty) {
                    callback(emptyList())
                    return@addOnSuccessListener
                }
                
                val petIds = documents.mapNotNull { it.getString("petId") }
                if (petIds.isEmpty()) {
                    callback(emptyList())
                    return@addOnSuccessListener
                }
                
                // Firestore limits 'in' queries to 10 values, so we might need to chunk
                val chunks = petIds.chunked(10)
                val allPets = mutableListOf<Pet>()
                
                // This is a simplified version, in a real app you might want to use Tasks.whenAllComplete
                var completedChunks = 0
                
                for (chunk in chunks) {
                    petsCollection.whereIn("id", chunk)
                        .get()
                        .addOnSuccessListener { petDocuments ->
                            val pets = petDocuments.mapNotNull { document ->
                                try {
                                    document.toObject(Pet::class.java).apply {
                                        id = document.id
                                        isFavorite = true
                                    }
                                } catch (e: Exception) {
                                    Log.e(TAG, "Error converting document to Pet", e)
                                    null
                                }
                            }
                            allPets.addAll(pets)
                            
                            completedChunks++
                            if (completedChunks == chunks.size) {
                                callback(allPets)
                            }
                        }
                        .addOnFailureListener { e ->
                            Log.e(TAG, "Error getting favorite pets", e)
                            completedChunks++
                            if (completedChunks == chunks.size) {
                                callback(allPets)
                            }
                        }
                }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error getting user favorites", e)
                callback(null)
            }
    }
    
    // For backward compatibility with existing code
    suspend fun getAllPets(): Result<List<Pet>> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Getting all pets from Firestore")
            
            // Get all pets, ensuring we don't filter by any specific user
            val querySnapshot = petsCollection
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .await()
                
            Log.d(TAG, "Firestore query returned ${querySnapshot.documents.size} pet documents")
            
            if (querySnapshot.documents.isEmpty()) {
                Log.w(TAG, "No pets found in database!")
                return@withContext Result.success(emptyList())
            }
            
            val petsMap = mutableMapOf<String, Pet>()
            
            // Process each document explicitly, ensuring no duplicates by ID
            for (doc in querySnapshot.documents) {
                try {
                    val pet = doc.toObject(Pet::class.java)
                    if (pet != null) {
                        // Set the ID from the document
                        val docId = doc.id
                        pet.id = docId
                        
                        // Only add if we don't already have this pet ID (prevents duplicates)
                        if (!petsMap.containsKey(docId)) {
                            // Mark if the pet belongs to current user
                            val currentUserId = auth.currentUser?.uid
                            pet.isFavorite = currentUserId != null && pet.ownerId == currentUserId
                            
                            // Add the pet to our map
                            petsMap[docId] = pet
                            
                            // Log detailed pet data for debugging
                            Log.d(TAG, "Successfully loaded pet: id=${pet.id}, name=${pet.name}, " + 
                                  "type=${pet.type}, ownerId=${pet.ownerId}, " +
                                  "currentUser=${auth.currentUser?.uid}, isFavorite=${pet.isFavorite}, " +
                                  "imageUrls=${pet.imageUrls.size}")
                        } else {
                            Log.w(TAG, "Duplicate pet document found with ID: $docId")
                        }
                    } else {
                        Log.w(TAG, "Document ${doc.id} could not be converted to Pet")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error processing pet document ${doc.id}: ${e.message}", e)
                    // Continue with other documents even if one fails
                }
            }
            
            // Convert map to list
            val pets = petsMap.values.toList()
            
            Log.d(TAG, "Successfully loaded ${pets.size} unique pets out of ${querySnapshot.documents.size} documents")
            Result.success(pets)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting all pets: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    // For backward compatibility with existing code
    suspend fun getPetsByType(type: String): Result<List<Pet>> = withContext(Dispatchers.IO) {
        try {
            // Use lowercase type for consistent querying
            val typeValue = type.lowercase()
            Log.d(TAG, "Getting pets by type: $typeValue")
            
            // Query for all pets with the specified type, regardless of owner
            val querySnapshot = petsCollection
                .whereEqualTo("type", typeValue)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .await()
                
            Log.d(TAG, "Firestore query returned ${querySnapshot.documents.size} pet documents of type: $typeValue")
            
            if (querySnapshot.documents.isEmpty()) {
                Log.w(TAG, "No pets found with type: $typeValue")
                return@withContext Result.success(emptyList())
            }
            
            val petsMap = mutableMapOf<String, Pet>()
            
            // Process each document explicitly, ensuring no duplicates by ID
            for (doc in querySnapshot.documents) {
                try {
                    val pet = doc.toObject(Pet::class.java)
                    if (pet != null) {
                        // Set the ID from the document
                        val docId = doc.id
                        pet.id = docId
                        
                        // Only add if we don't already have this pet ID (prevents duplicates)
                        if (!petsMap.containsKey(docId)) {
                            // Mark if the pet belongs to current user
                            val currentUserId = auth.currentUser?.uid
                            pet.isFavorite = currentUserId != null && pet.ownerId == currentUserId
                            
                            // Add the pet to our map
                            petsMap[docId] = pet
                            
                            // Log detailed pet data for debugging
                            Log.d(TAG, "Successfully loaded pet of type $typeValue: id=${pet.id}, name=${pet.name}, " + 
                                  "ownerId=${pet.ownerId}, currentUser=${auth.currentUser?.uid}, isFavorite=${pet.isFavorite}, " +
                                  "imageUrls=${pet.imageUrls.size}")
                        } else {
                            Log.w(TAG, "Duplicate pet document found with ID: $docId and type: $typeValue")
                        }
                    } else {
                        Log.w(TAG, "Document ${doc.id} could not be converted to Pet")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error processing pet document ${doc.id}: ${e.message}", e)
                    // Continue with other documents even if one fails
                }
            }
            
            // Convert map to list
            val pets = petsMap.values.toList()
            
            Log.d(TAG, "Successfully loaded ${pets.size} unique pets of type $typeValue out of ${querySnapshot.documents.size} documents")
            Result.success(pets)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting pets by type $type: ${e.message}", e)
            Result.failure(e)
        }
    }

    // Add suspend function for getUserApplications
    suspend fun getUserApplications(userId: String): Result<List<Pet>> = withContext(Dispatchers.IO) {
        try {
            val applications = firestore.collection("applications")
                .whereEqualTo("userId", userId)
                .get()
                .await()
                
            if (applications.isEmpty) {
                return@withContext Result.success(emptyList())
            }
            
            val petIds = applications.mapNotNull { it.getString("petId") }
            if (petIds.isEmpty()) {
                return@withContext Result.success(emptyList())
            }
            
            val pets = mutableListOf<Pet>()
            for (petId in petIds) {
                val petDoc = petsCollection.document(petId).get().await()
                if (petDoc.exists()) {
                    petDoc.toObject(Pet::class.java)?.let { pet ->
                        pet.id = petDoc.id
                        pets.add(pet)
                    }
                }
            }
            
            Result.success(pets)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting user applications", e)
            Result.failure(e)
        }
    }
    
    // Add suspend function for getPetById
    suspend fun getPetById(petId: String): Result<Pet> = withContext(Dispatchers.IO) {
        try {
            val document = petsCollection.document(petId).get().await()
            
            if (document != null && document.exists()) {
                val pet = document.toObject(Pet::class.java)?.apply {
                    id = document.id
                }
                
                if (pet != null) {
                    Result.success(pet)
                } else {
                    Result.failure(Exception("Failed to parse pet data"))
                }
            } else {
                Result.failure(Exception("Pet not found"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting pet by ID", e)
            Result.failure(e)
        }
    }
    
    suspend fun getAllPets(
        type: String? = null,
        breed: String? = null,
        minAge: Int? = null,
        maxAge: Int? = null,
        gender: String? = null,
        size: String? = null,
        vaccinationStatus: Boolean? = null,
        spayedNeutered: Boolean? = null,
        goodWith: String? = null,
        energyLevel: String? = null
    ): Result<List<Pet>> = withContext(Dispatchers.IO) {
        try {
            // Start with a base query
            var query = petsCollection.orderBy("createdAt", Query.Direction.DESCENDING)
            
            // Apply filters (Note: Firestore has limitations on composite queries)
            if (!type.isNullOrEmpty()) {
                // Always use lowercase for type querying
                query = query.whereEqualTo("type", type.lowercase())
            }
            
            // Execute the query
            val documents = query.get().await()
            
            // Get all pets and filter client-side for remaining criteria
            val allPets = documents.mapNotNull { document ->
                try {
                    document.toObject(Pet::class.java).apply {
                        id = document.id
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error converting document to Pet", e)
                    null
                }
            }
            
            // Apply remaining filters client-side
            val filteredPets = allPets.filter { pet ->
                var matches = true
                
                if (!breed.isNullOrEmpty() && pet.breed != breed) matches = false
                if (minAge != null && (pet.age ?: 0) < minAge) matches = false
                if (maxAge != null && (pet.age ?: 0) > maxAge) matches = false
                if (!gender.isNullOrEmpty() && pet.gender != gender) matches = false
                if (!size.isNullOrEmpty() && pet.size != size) matches = false
                if (vaccinationStatus != null && pet.vaccinationStatus != vaccinationStatus) matches = false
                if (spayedNeutered != null && pet.spayedNeutered != spayedNeutered) matches = false
                if (!goodWith.isNullOrEmpty() && !pet.goodWith.contains(goodWith)) matches = false
                if (!energyLevel.isNullOrEmpty() && pet.energyLevel != energyLevel) matches = false
                
                matches
            }
            
            Result.success(filteredPets)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting filtered pets", e)
            Result.failure(e)
        }
    }
    
    // Add suspend function for searchPets
    suspend fun searchPets(query: String): Result<List<Pet>> = withContext(Dispatchers.IO) {
        try {
            // Firebase doesn't support full-text search natively
            // Getting all pets and filtering client side (not ideal for large datasets)
            val querySnapshot = petsCollection
                .orderBy("name")
                .get()
                .await()
                
            val lowercaseQuery = query.lowercase()
            val pets = querySnapshot.documents.mapNotNull { doc ->
                val pet = doc.toObject(Pet::class.java)?.apply { id = doc.id }
                
                // Only return pets that match the search query
                if (pet != null && (
                    pet.name.lowercase().contains(lowercaseQuery) ||
                    pet.breed?.lowercase()?.contains(lowercaseQuery) == true ||
                    pet.description?.lowercase()?.contains(lowercaseQuery) == true)
                ) {
                    pet
                } else {
                    null
                }
            }
            
            Result.success(pets)
        } catch (e: Exception) {
            Log.e(TAG, "Error searching pets: $query", e)
            Result.failure(e)
        }
    }

    // Sample data methods
    private fun getSampleFeaturedPets(): List<Pet> {
        return listOf(
            Pet(
                id = "featured1",
                name = "Max",
                type = "dog",
                breed = "Golden Retriever",
                age = 2,
                gender = "Male",
                description = "Friendly golden retriever looking for a loving home.",
                price = 300.0,
                images = listOf("https://images.unsplash.com/photo-1552053831-71594a27632d?ixlib=rb-1.2.1&auto=format&fit=crop&w=500&q=60"),
                featuredImage = "https://images.unsplash.com/photo-1552053831-71594a27632d?ixlib=rb-1.2.1&auto=format&fit=crop&w=500&q=60",
                location = "New York, NY",
                featured = true,
                isFavorite = true
            ),
            Pet(
                id = "featured2",
                name = "Luna",
                type = "cat",
                breed = "Siamese",
                age = 1,
                gender = "Female",
                description = "Beautiful Siamese cat with blue eyes.",
                price = 250.0,
                images = listOf("https://images.unsplash.com/photo-1561948955-570d270324b7?ixlib=rb-1.2.1&auto=format&fit=crop&w=500&q=60"),
                featuredImage = "https://images.unsplash.com/photo-1561948955-570d270324b7?ixlib=rb-1.2.1&auto=format&fit=crop&w=500&q=60",
                location = "Boston, MA",
                featured = true,
                isFavorite = false
            ),
            Pet(
                id = "featured3",
                name = "Charlie",
                type = "dog",
                breed = "Poodle",
                age = 3,
                gender = "Male",
                description = "Intelligent poodle with a playful personality.",
                price = 350.0,
                images = listOf("https://images.unsplash.com/photo-1575859431774-2e57ed632664?ixlib=rb-1.2.1&auto=format&fit=crop&w=500&q=60"),
                featuredImage = "https://images.unsplash.com/photo-1575859431774-2e57ed632664?ixlib=rb-1.2.1&auto=format&fit=crop&w=500&q=60",
                location = "Chicago, IL",
                featured = true,
                isFavorite = false
            )
        )
    }
    
    private fun getSamplePets(): List<Pet> {
        val featuredPets = getSampleFeaturedPets()
        val regularPets = listOf(
            Pet(
                id = "pet1",
                name = "Bella",
                type = "dog",
                breed = "Labrador",
                age = 2,
                gender = "Female",
                description = "Energetic Labrador that loves to play fetch.",
                price = 280.0,
                images = listOf("https://images.unsplash.com/photo-1587402092301-725e37c70fd8?ixlib=rb-1.2.1&auto=format&fit=crop&w=500&q=60"),
                featuredImage = "https://images.unsplash.com/photo-1587402092301-725e37c70fd8?ixlib=rb-1.2.1&auto=format&fit=crop&w=500&q=60",
                location = "Miami, FL",
                featured = false,
                isFavorite = false
            ),
            Pet(
                id = "pet2",
                name = "Oliver",
                type = "cat",
                breed = "Maine Coon",
                age = 3,
                gender = "Male",
                description = "Gentle giant with a luxurious coat.",
                price = 325.0,
                images = listOf("https://images.unsplash.com/photo-1518791841217-8f162f1e1131?ixlib=rb-1.2.1&auto=format&fit=crop&w=500&q=60"),
                featuredImage = "https://images.unsplash.com/photo-1518791841217-8f162f1e1131?ixlib=rb-1.2.1&auto=format&fit=crop&w=500&q=60",
                location = "Seattle, WA",
                featured = false,
                isFavorite = true
            ),
            Pet(
                id = "pet3",
                name = "Buddy",
                type = "dog",
                breed = "Beagle",
                age = 1,
                gender = "Male",
                description = "Playful beagle puppy with lots of energy.",
                price = 270.0,
                images = listOf("https://images.unsplash.com/photo-1586671267731-da2cf3ceeb80?ixlib=rb-1.2.1&auto=format&fit=crop&w=500&q=60"),
                featuredImage = "https://images.unsplash.com/photo-1586671267731-da2cf3ceeb80?ixlib=rb-1.2.1&auto=format&fit=crop&w=500&q=60",
                location = "Austin, TX",
                featured = false,
                isFavorite = false
            ),
            Pet(
                id = "pet4",
                name = "Daisy",
                type = "cat",
                breed = "Ragdoll",
                age = 2,
                gender = "Female",
                description = "Sweet and cuddly ragdoll with stunning blue eyes.",
                price = 290.0,
                images = listOf("https://images.unsplash.com/photo-1541781774459-bb2af2f05b55?ixlib=rb-1.2.1&auto=format&fit=crop&w=500&q=60"),
                featuredImage = "https://images.unsplash.com/photo-1541781774459-bb2af2f05b55?ixlib=rb-1.2.1&auto=format&fit=crop&w=500&q=60",
                location = "Portland, OR",
                featured = false,
                isFavorite = false
            )
        )
        return featuredPets + regularPets
    }

    suspend fun getFeaturedPets(): Result<List<Pet>> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Getting featured pets from Firestore")
            
            // Query for all featured pets, regardless of owner
            val querySnapshot = petsCollection
                .whereEqualTo("featured", true)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .await()
                
            Log.d(TAG, "Firestore query returned ${querySnapshot.documents.size} featured pet documents")
            
            if (querySnapshot.documents.isEmpty()) {
                Log.w(TAG, "No featured pets found in database!")
                return@withContext Result.success(emptyList())
            }
            
            val petsMap = mutableMapOf<String, Pet>()
            
            // Process each document explicitly, ensuring no duplicates by ID
            for (doc in querySnapshot.documents) {
                try {
                    val pet = doc.toObject(Pet::class.java)
                    if (pet != null) {
                        // Set the ID from the document
                        val docId = doc.id
                        pet.id = docId
                        
                        // Only add if we don't already have this pet ID (prevents duplicates)
                        if (!petsMap.containsKey(docId)) {
                            // Mark if the pet belongs to current user
                            val currentUserId = auth.currentUser?.uid
                            pet.isFavorite = currentUserId != null && pet.ownerId == currentUserId
                            
                            // Add the pet to our map
                            petsMap[docId] = pet
                            
                            // Log detailed pet data for debugging
                            Log.d(TAG, "Successfully loaded featured pet: id=${pet.id}, name=${pet.name}, " + 
                                  "type=${pet.type}, ownerId=${pet.ownerId}, " +
                                  "currentUser=${auth.currentUser?.uid}, isFavorite=${pet.isFavorite}, " +
                                  "imageUrls=${pet.imageUrls.size}")
                        } else {
                            Log.w(TAG, "Duplicate featured pet document found with ID: $docId")
                        }
                    } else {
                        Log.w(TAG, "Document ${doc.id} could not be converted to Pet")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error processing pet document ${doc.id}: ${e.message}", e)
                    // Continue with other documents even if one fails
                }
            }
            
            // Convert map to list
            val pets = petsMap.values.toList()
            
            Log.d(TAG, "Successfully loaded ${pets.size} unique featured pets out of ${querySnapshot.documents.size} documents")
            Result.success(pets)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting featured pets: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    suspend fun getPetsByCategory(category: String): Result<List<Pet>> = withContext(Dispatchers.IO) {
        try {
            val lowercaseCategory = category.lowercase()
            Log.d(TAG, "Getting pets by category: $lowercaseCategory")
            
            val querySnapshot = petsCollection
                .whereEqualTo("type", lowercaseCategory)  // Always use lowercase
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .await()
                
            Log.d(TAG, "Query returned ${querySnapshot.documents.size} documents for category: $lowercaseCategory")
            
            val pets = querySnapshot.documents.mapNotNull { doc ->
                doc.toObject(Pet::class.java)?.apply { 
                    id = doc.id
                    // Mark if the pet belongs to current user
                    isFavorite = isCurrentUsersPet(this)
                }
            }
            
            Log.d(TAG, "Successfully loaded ${pets.size} pets for category: $lowercaseCategory")
            Result.success(pets)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting pets by category: $category", e)
            Result.failure(e)
        }
    }
    
    // Add method to get only current user's pets
    suspend fun getCurrentUserPets(): Result<List<Pet>> = withContext(Dispatchers.IO) {
        try {
            val currentUserId = auth.currentUser?.uid ?: throw Exception("User not logged in")
            
            val querySnapshot = petsCollection
                .whereEqualTo("ownerId", currentUserId)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .await()
                
            val pets = querySnapshot.documents.mapNotNull { doc ->
                doc.toObject(Pet::class.java)?.apply { 
                    id = doc.id
                    isFavorite = true // These are user's own pets
                }
            }
            Result.success(pets)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting user's pets", e)
            Result.failure(e)
        }
    }

    // Helper method to get current user ID
    fun getCurrentUserId(): String? {
        return auth.currentUser?.uid
    }
}