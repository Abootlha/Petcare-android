package com.basic.petproject.models

import java.io.Serializable

data class AdoptionApplication(
    val id: String = "",
    val petId: String = "",
    val petName: String = "",
    val applicantId: String = "",
    val applicantName: String = "",
    val applicantEmail: String = "",
    val applicantPhone: String = "",
    
    // Application details
    val applicationDate: Long = System.currentTimeMillis(),
    val status: String = "Pending", // Pending, Under Review, Approved, Rejected
    val lastUpdated: Long = System.currentTimeMillis(),
    
    // Living situation
    val homeType: String = "", // Apartment, House, etc.
    val hasYard: Boolean = false,
    val yardFenced: Boolean = false,
    val hasChildren: Boolean = false,
    val childrenAges: String = "",
    val hasOtherPets: Boolean = false,
    val otherPetsDescription: String = "",
    
    // Experience and plans
    val petExperience: String = "",
    val hoursAlone: Int = 0,
    val exercisePlan: String = "",
    val trainingPlan: String = "",
    val reasonForAdoption: String = "",
    
    // References
    val veterinarianReference: String = "",
    val personalReference: String = "",
    
    // Admin notes
    val adminNotes: String = "",
    val rejectionReason: String = ""
) : Serializable 