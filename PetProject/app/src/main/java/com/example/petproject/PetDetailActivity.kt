package com.example.petproject

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.petproject.models.Pet
import com.squareup.picasso.Picasso

class PetDetailActivity : AppCompatActivity() {
    private lateinit var pet: Pet
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pet_detail)
        
        // Get pet from intent
        pet = intent.getSerializableExtra("pet") as Pet
        
        // Set up views
        setupViews()
        
        // Set up buttons
        setupButtons()
    }
    
    private fun setupViews() {
        val imageViewPet = findViewById<ImageView>(R.id.imageViewPet)
        val textViewName = findViewById<TextView>(R.id.textViewName)
        val textViewBreed = findViewById<TextView>(R.id.textViewBreed)
        val textViewAge = findViewById<TextView>(R.id.textViewAge)
        val textViewGender = findViewById<TextView>(R.id.textViewGender)
        val textViewPrice = findViewById<TextView>(R.id.textViewPrice)
        val textViewDescription = findViewById<TextView>(R.id.textViewDescription)
        val textViewOwner = findViewById<TextView>(R.id.textViewOwner)
        
        // Set pet details
        textViewName.text = pet.name
        textViewBreed.text = "${pet.type} - ${pet.breed}"
        textViewAge.text = "Age: ${pet.age} ${if (pet.age == 1) "year" else "years"}"
        textViewGender.text = "Gender: ${pet.gender}"
        textViewPrice.text = "$${pet.price}"
        textViewDescription.text = pet.description
        textViewOwner.text = "Owner: ${pet.ownerName}"
        
        // Load image with Picasso
        if (pet.imageUrl.isNotEmpty()) {
            Picasso.get()
                .load(pet.imageUrl)
                .placeholder(R.drawable.pet_placeholder)
                .error(R.drawable.pet_placeholder)
                .into(imageViewPet)
        } else {
            imageViewPet.setImageResource(R.drawable.pet_placeholder)
        }
    }
    
    private fun setupButtons() {
        val buttonAddToCart = findViewById<Button>(R.id.buttonAddToCart)
        val buttonContactOwner = findViewById<Button>(R.id.buttonContactOwner)
        
        buttonAddToCart.setOnClickListener {
            // Add pet to cart
            CartManager.addToCart(pet)
            Toast.makeText(this, "${pet.name} added to cart", Toast.LENGTH_SHORT).show()
        }
        
        buttonContactOwner.setOnClickListener {
            // Open chat with owner
            val intent = Intent(this, ChatActivity::class.java)
            intent.putExtra("ownerId", pet.ownerId)
            intent.putExtra("ownerName", pet.ownerName)
            startActivity(intent)
        }
    }
}