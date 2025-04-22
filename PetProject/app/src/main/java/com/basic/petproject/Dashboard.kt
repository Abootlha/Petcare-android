package com.basic.petproject

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.cardview.widget.CardView
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.basic.petproject.adapters.PetAdapter
import com.basic.petproject.base.BaseNavigationActivity
import com.basic.petproject.repositories.PetRepository
import kotlinx.coroutines.launch

class Dashboard : BaseNavigationActivity() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var petAdapter: PetAdapter
    private lateinit var addPetButton: Button
    private lateinit var dogCategory: CardView
    private lateinit var catCategory: CardView
    private lateinit var birdCategory: CardView
    private lateinit var fishCategory: CardView
    private lateinit var userNameText: TextView
    private val petRepository = PetRepository()
    private val TAG = "Dashboard"
    private var currentCategory: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dashboard)
        
        // Setup bottom navigation after setContentView
        setupBottomNavigation()

        // Initialize views
        recyclerView = findViewById(R.id.petsRecyclerView)
        addPetButton = findViewById(R.id.addPetButton)
        userNameText = findViewById(R.id.userNameText)

        // Initialize category cards
        dogCategory = findViewById(R.id.dogCategory)
        catCategory = findViewById(R.id.catCategory)
        birdCategory = findViewById(R.id.birdCategory)
        fishCategory = findViewById(R.id.fishCategory)

        // Set Welcome Text (example - replace "User Name" with actual user name if available)
        userNameText.text = "User Name"

        // Setup RecyclerView
        recyclerView.layoutManager = GridLayoutManager(this, 2)
        petAdapter = PetAdapter(emptyList()) { pet ->
            // Handle pet item click - open pet details
            val intent = Intent(this, PetDetailActivity::class.java)
            intent.putExtra("pet", pet)
            startActivity(intent)
        }
        recyclerView.adapter = petAdapter

        // Setup add pet button
        addPetButton.setOnClickListener {
            val intent = Intent(this, AddPetActivity::class.java)
            startActivity(intent)
        }

        // Set the home item as selected
        bottomNavigation.selectedItemId = R.id.nav_home

        // Setup category click listeners
        setupCategoryListeners()

        // Load all pets initially
        loadPets()
    }

    override fun onResume() {
        super.onResume()
        // Refresh pet list when returning to this activity
        // If a category is selected, load pets for that category, otherwise load all pets
        if (currentCategory != null) {
            loadPetsByCategory(currentCategory!!)
        } else {
            loadPets()
        }
    }

    private fun setupCategoryListeners() {
        // Set click listener for dog category
        dogCategory.setOnClickListener {
            resetCategoryBackgrounds()
            dogCategory.setCardBackgroundColor(getColor(R.color.primary_light))
            currentCategory = "Dog"
            loadPetsByCategory("Dog")
        }

        // Set click listener for cat category
        catCategory.setOnClickListener {
            resetCategoryBackgrounds()
            catCategory.setCardBackgroundColor(getColor(R.color.primary_light))
            currentCategory = "Cat"
            loadPetsByCategory("Cat")
        }

        // Set click listener for bird category
        birdCategory.setOnClickListener {
            resetCategoryBackgrounds()
            birdCategory.setCardBackgroundColor(getColor(R.color.primary_light))
            currentCategory = "Bird"
            loadPetsByCategory("Bird")
        }

        // Set click listener for fish category
        fishCategory.setOnClickListener {
            resetCategoryBackgrounds()
            fishCategory.setCardBackgroundColor(getColor(R.color.primary_light))
            currentCategory = "Fish"
            loadPetsByCategory("Fish")
        }

        // Long press on any category resets to show all pets
        val longClickListener = View.OnLongClickListener {
            resetCategoryBackgrounds()
            currentCategory = null
            loadPets()
            Toast.makeText(this@Dashboard, "Showing all pets", Toast.LENGTH_SHORT).show()
            true
        }

        dogCategory.setOnLongClickListener(longClickListener)
        catCategory.setOnLongClickListener(longClickListener)
        birdCategory.setOnLongClickListener(longClickListener)
        fishCategory.setOnLongClickListener(longClickListener)
    }

    private fun resetCategoryBackgrounds() {
        dogCategory.setCardBackgroundColor(getColor(R.color.dog_category_color))
        catCategory.setCardBackgroundColor(getColor(R.color.cat_category_color))
        birdCategory.setCardBackgroundColor(getColor(R.color.bird_category_color))
        fishCategory.setCardBackgroundColor(getColor(R.color.fish_category_color))
    }

    private fun loadPetsByCategory(category: String) {
        lifecycleScope.launch {
            try {
                val result = petRepository.getPetsByType(category)
                if (result.isSuccess) {
                    val pets = result.getOrNull() ?: emptyList()
                    petAdapter.updatePets(pets)
                } else {
                    val exception = result.exceptionOrNull()
                    Log.e(TAG, "Error loading pets by category", exception)
                    Toast.makeText(
                        this@Dashboard,
                        "Error loading pets: ${exception?.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Exception in loadPetsByCategory", e)
                Toast.makeText(this@Dashboard, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun loadPets() {
        lifecycleScope.launch {
            try {
                val result = petRepository.getAllPets()
                if (result.isSuccess) {
                    val pets = result.getOrNull() ?: emptyList()
                    petAdapter.updatePets(pets)
                } else {
                    val exception = result.exceptionOrNull()
                    Log.e(TAG, "Error loading pets", exception)
                    Toast.makeText(
                        this@Dashboard,
                        "Error loading pets: ${exception?.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Exception in loadPets", e)
                Toast.makeText(this@Dashboard, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
}