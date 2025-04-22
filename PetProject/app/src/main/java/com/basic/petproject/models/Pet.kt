package com.basic.petproject.models

import java.io.Serializable

data class Pet(
    var id: String? = null,
    val name: String = "",
    val type: String = "",
    val breed: String? = null,
    val age: Int? = null,
    val gender: String? = null,
    val size: String? = null,
    val description: String? = null,
    val price: Double = 0.0,
    val isAdopted: Boolean = false,
    val ownerId: String? = null,
    val ownerName: String? = null,
    val ownerContact: String? = null,
    val images: List<String> = emptyList(),
    val imageUrl: String? = null,
    val imageUrls: List<String> = emptyList(),
    val featuredImage: String? = null,
    val location: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val vaccinationStatus: Boolean = false,
    val spayedNeutered: Boolean = false,
    val goodWith: List<String> = emptyList(),
    val energyLevel: String? = null,
    val healthIssues: String? = null,
    val dietaryNeeds: String? = null,
    val medicalHistory: String? = null,
    val specialNeeds: String? = null,
    val temperament: String? = null,
    val trainingLevel: String? = null,
    val color: String? = null,
    val weight: Double? = null,
    val shedding: String? = null,
    val adoptionFee: Double = 0.0,
    val adoptionRequirements: String? = null,
    val applicationStatus: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val featured: Boolean = false,
    var isFavorite: Boolean = false
) : Serializable {
    // Ensure pets with the same ID are considered equal
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Pet) return false
        
        // If both have IDs, compare by ID
        if (id != null && other.id != null) {
            return id == other.id
        }
        
        // Otherwise, fall back to default equals implementation
        return super.equals(other)
    }
    
    override fun hashCode(): Int {
        return id?.hashCode() ?: super.hashCode()
    }
    
    override fun toString(): String {
        return "Pet(id=$id, name='$name', type='$type', breed=$breed, ownerId=$ownerId)"
    }
}