package com.basic.petproject

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.basic.petproject.models.Pet
import com.squareup.picasso.Picasso

class PetDetailActivity : AppCompatActivity() {
    private lateinit var pet: Pet
    private lateinit var imageViewPet: ImageView
    private lateinit var textViewName: TextView
    private lateinit var textViewDescription: TextView
    private lateinit var textViewStatus: TextView
    private lateinit var textViewBreed: TextView
    private lateinit var textViewAge: TextView
    private lateinit var textViewGender: TextView
    private lateinit var textViewSize: TextView
    private lateinit var textViewWeight: TextView
    private lateinit var textViewVaccination: TextView
    private lateinit var textViewSpayedNeutered: TextView
    private lateinit var textViewSpecialNeeds: TextView
    private lateinit var textViewOwner: TextView
    private lateinit var textViewContact: TextView
    private lateinit var buttonMessage: Button
    private lateinit var buttonAdopt: Button
    
    // Detail section text views
    private lateinit var textViewBreedDetail: TextView
    private lateinit var textViewAgeDetail: TextView
    private lateinit var textViewGenderDetail: TextView
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pet_detail)
        
        // Get pet from intent
        pet = intent.getSerializableExtra("pet") as Pet
        
        // Initialize views
        initializeViews()
        
        // Load pet data
        loadPetData()
        
        // Setup button listeners
        setupButtonListeners()
    }
    
    private fun initializeViews() {
        imageViewPet = findViewById(R.id.imageViewPet)
        textViewName = findViewById(R.id.textViewName)
        textViewDescription = findViewById(R.id.textViewDescription)
        textViewStatus = findViewById(R.id.textViewStatus)
        textViewBreed = findViewById(R.id.textViewBreed)
        textViewAge = findViewById(R.id.textViewAge)
        textViewGender = findViewById(R.id.textViewGender)
        textViewSize = findViewById(R.id.textViewSize)
        textViewWeight = findViewById(R.id.textViewWeight)
        textViewVaccination = findViewById(R.id.textViewVaccination)
        textViewSpayedNeutered = findViewById(R.id.textViewSpayedNeutered)
        textViewSpecialNeeds = findViewById(R.id.textViewSpecialNeeds)
        textViewOwner = findViewById(R.id.textViewOwner)
        textViewContact = findViewById(R.id.textViewContact)
        buttonMessage = findViewById(R.id.buttonMessage)
        buttonAdopt = findViewById(R.id.buttonAdopt)
        
        // Initialize detail section views
        textViewBreedDetail = findViewById(R.id.textViewBreedDetail)
        textViewAgeDetail = findViewById(R.id.textViewAgeDetail)
        textViewGenderDetail = findViewById(R.id.textViewGenderDetail)
    }
    
    private fun loadPetData() {
        // Load image
        if (pet.imageUrl?.isNotEmpty() == true) {
            Picasso.get().load(pet.imageUrl).into(imageViewPet)
        }
        
        // Set basic info
        textViewName.text = pet.name
        textViewDescription.text = pet.description
        textViewStatus.text = pet.applicationStatus?.ifEmpty { "Available" } ?: "Available"
        
        // Set status color
        when (pet.applicationStatus) {
            "Adopted" -> textViewStatus.setTextColor(getColor(android.R.color.holo_red_light))
            "Pending" -> textViewStatus.setTextColor(getColor(android.R.color.holo_orange_light))
            else -> textViewStatus.setTextColor(getColor(android.R.color.holo_green_dark))
        }
        
        // Disable adopt button if pet is already adopted or pending
        buttonAdopt.isEnabled = pet.applicationStatus?.isEmpty() ?: true || 
                pet.applicationStatus != "Adopted" && pet.applicationStatus != "Pending"
        
        if (!buttonAdopt.isEnabled) {
            buttonAdopt.text = if (pet.applicationStatus == "Adopted") "Adopted" else "Application Pending"
        }
        
        // Set detailed information
        textViewBreed.text = pet.breed
        textViewAge.text = "${pet.age} years"
        textViewGender.text = pet.gender
        textViewSize.text = pet.size?.ifEmpty { "Not specified" } ?: "Not specified"
        
        // Also set the detail section text views
        textViewBreedDetail.text = pet.breed
        textViewAgeDetail.text = "${pet.age} years"
        textViewGenderDetail.text = pet.gender
        
        // Fix the smart cast issue with weight
        val petWeight = pet.weight
        textViewWeight.text = if (petWeight != null && petWeight > 0) "${petWeight} kg" else "Not specified"
        
        // Set health information
        textViewVaccination.text = if (pet.vaccinationStatus) "Yes" else "No"
        textViewSpayedNeutered.text = if (pet.spayedNeutered) "Yes" else "No"
        textViewSpecialNeeds.text = pet.specialNeeds?.ifEmpty { "None" } ?: "None"
        
        // Set owner information
        textViewOwner.text = pet.ownerName
        textViewContact.text = pet.ownerContact?.ifEmpty { "Contact via app" } ?: "Contact via app"
    }
    
    private fun setupButtonListeners() {
        // Message button
        buttonMessage.setOnClickListener {
            val intent = Intent(this, ChatActivity::class.java)
            intent.putExtra("recipientId", pet.ownerId)
            intent.putExtra("recipientName", pet.ownerName)
            startActivity(intent)
        }
        
        // Adopt button
        buttonAdopt.setOnClickListener {
            if (pet.applicationStatus?.isEmpty() ?: true || 
                    pet.applicationStatus != "Adopted" && pet.applicationStatus != "Pending") {
                // Start adoption application process
                val intent = Intent(this, AdoptionApplicationActivity::class.java)
                intent.putExtra("pet", pet)
                startActivity(intent)
            } else {
                Toast.makeText(this, "This pet is not available for adoption", Toast.LENGTH_SHORT).show()
            }
        }
    }
}