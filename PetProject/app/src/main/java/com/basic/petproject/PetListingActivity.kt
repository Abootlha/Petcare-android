package com.basic.petproject

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.basic.petproject.adapters.PetAdapter
import com.basic.petproject.models.Pet
import com.basic.petproject.repositories.PetRepository
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.launch

class PetListingActivity : AppCompatActivity() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var petAdapter: PetAdapter
    private lateinit var progressBar: ProgressBar
    private lateinit var emptyView: LinearLayout
    private lateinit var addPetButton: FloatingActionButton
    private lateinit var searchEditText: EditText
    private lateinit var buttonAll: Button
    private lateinit var buttonDogs: Button
    private lateinit var buttonCats: Button
    private lateinit var buttonBirds: Button
    private lateinit var buttonFish: Button
    
    private val petRepository = PetRepository()
    private val TAG = "PetListingActivity"
    
    private var allPets: List<Pet> = emptyList()
    private var currentFilter: String = "All"
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pet_listing)
        
        // Hide the action bar title since we have our own title in the layout
        supportActionBar?.hide()
        
        // Initialize views
        recyclerView = findViewById(R.id.recyclerViewPets)
        progressBar = findViewById(R.id.progressBar)
        emptyView = findViewById(R.id.emptyView)
        addPetButton = findViewById(R.id.fabAddPet)
        searchEditText = findViewById(R.id.editTextSearch)
        buttonAll = findViewById(R.id.buttonAll)
        buttonDogs = findViewById(R.id.buttonDogs)
        buttonCats = findViewById(R.id.buttonCats)
        buttonBirds = findViewById(R.id.buttonBirds)
        buttonFish = findViewById(R.id.buttonFish)
        
        // Setup RecyclerView with spacing between items
        val layoutManager = GridLayoutManager(this, 2)
        recyclerView.layoutManager = layoutManager
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
        
        // Setup search functionality
        setupSearch()
        
        // Setup category filters
        setupCategoryFilters()
        
        // Load pets
        loadPets()
    }
    
    override fun onResume() {
        super.onResume()
        // Refresh pet list when returning to this activity
        loadPets()
    }
    
    private fun loadPets() {
        progressBar.visibility = View.VISIBLE
        emptyView.visibility = View.GONE
        
        lifecycleScope.launch {
            try {
                val result = petRepository.getAllPets()
                progressBar.visibility = View.GONE
                
                if (result.isSuccess) {
                    allPets = result.getOrNull() ?: emptyList()
                    filterPets() // Apply current filters to the loaded pets
                } else {
                    val exception = result.exceptionOrNull()
                    Log.e(TAG, "Error loading pets: ${exception?.message}")
                    Toast.makeText(this@PetListingActivity, "Error loading pets: ${exception?.message}", Toast.LENGTH_SHORT).show()
                    allPets = emptyList()
                    updateUI(emptyList())
                }
            } catch (e: Exception) {
                progressBar.visibility = View.GONE
                Log.e(TAG, "Exception loading pets: ${e.message}")
                Toast.makeText(this@PetListingActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                allPets = emptyList()
                updateUI(emptyList())
            }
        }
    }
    
    private fun setupSearch() {
        searchEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            
            override fun afterTextChanged(s: Editable?) {
                filterPets()
            }
        })
    }
    
    private fun setupCategoryFilters() {
        val buttons = listOf(buttonAll, buttonDogs, buttonCats, buttonBirds, buttonFish)
        
        buttonAll.setOnClickListener {
            setActiveFilter("All", buttons)
        }
        
        buttonDogs.setOnClickListener {
            setActiveFilter("Dog", buttons)
        }
        
        buttonCats.setOnClickListener {
            setActiveFilter("Cat", buttons)
        }
        
        buttonBirds.setOnClickListener {
            setActiveFilter("Bird", buttons)
        }
        
        buttonFish.setOnClickListener {
            setActiveFilter("Fish", buttons)
        }
    }
    
    private fun setActiveFilter(filter: String, buttons: List<Button>) {
        currentFilter = filter
        
        // Reset all buttons to inactive state
        buttons.forEach { button ->
            button.backgroundTintList = ContextCompat.getColorStateList(this, R.color.medium_gray)
            button.setTextColor(ContextCompat.getColor(this, R.color.dark_gray))
        }
        
        // Set the selected button to active state
        when (filter) {
            "All" -> {
                buttonAll.backgroundTintList = ContextCompat.getColorStateList(this, R.color.primary)
                buttonAll.setTextColor(ContextCompat.getColor(this, R.color.white))
            }
            "Dog" -> {
                buttonDogs.backgroundTintList = ContextCompat.getColorStateList(this, R.color.primary)
                buttonDogs.setTextColor(ContextCompat.getColor(this, R.color.white))
            }
            "Cat" -> {
                buttonCats.backgroundTintList = ContextCompat.getColorStateList(this, R.color.primary)
                buttonCats.setTextColor(ContextCompat.getColor(this, R.color.white))
            }
            "Bird" -> {
                buttonBirds.backgroundTintList = ContextCompat.getColorStateList(this, R.color.primary)
                buttonBirds.setTextColor(ContextCompat.getColor(this, R.color.white))
            }
            "Fish" -> {
                buttonFish.backgroundTintList = ContextCompat.getColorStateList(this, R.color.primary)
                buttonFish.setTextColor(ContextCompat.getColor(this, R.color.white))
            }
        }
        
        filterPets()
    }
    
    private fun filterPets() {
        val searchQuery = searchEditText.text.toString().trim().lowercase()
        
        val filteredPets = allPets.filter { pet ->
            // Apply category filter
            val categoryMatch = currentFilter == "All" || pet.type == currentFilter
            
            // Apply search filter
            val searchMatch = searchQuery.isEmpty() ||
                    pet.name.lowercase().contains(searchQuery) ||
                    pet.breed?.lowercase()?.contains(searchQuery) ?: false ||
                    pet.type.lowercase().contains(searchQuery)
            
            categoryMatch && searchMatch
        }
        
        updateUI(filteredPets)
    }
    
    private fun updateUI(pets: List<Pet>) {
        petAdapter.updatePets(pets)
        
        if (pets.isEmpty()) {
            recyclerView.visibility = View.GONE
            emptyView.visibility = View.VISIBLE
        } else {
            recyclerView.visibility = View.VISIBLE
            emptyView.visibility = View.GONE
        }
    }
}