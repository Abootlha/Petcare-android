package com.basic.petproject

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.lifecycleScope
import com.basic.petproject.repositories.PetRepository
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.launch

class AdvancedSearchActivity : AppCompatActivity() {
    private lateinit var toolbar: Toolbar
    private lateinit var typeDropdown: AutoCompleteTextView
    private lateinit var breedInput: TextInputEditText
    private lateinit var genderDropdown: AutoCompleteTextView
    private lateinit var minAgeInput: TextInputEditText
    private lateinit var maxAgeInput: TextInputEditText
    private lateinit var sizeDropdown: AutoCompleteTextView
    private lateinit var colorInput: TextInputEditText
    private lateinit var vaccinatedCheckbox: CheckBox
    private lateinit var spayedNeuteredCheckbox: CheckBox
    private lateinit var specialNeedsCheckbox: CheckBox
    private lateinit var energyLevelDropdown: AutoCompleteTextView
    private lateinit var goodWithChildrenCheckbox: CheckBox
    private lateinit var goodWithDogsCheckbox: CheckBox
    private lateinit var goodWithCatsCheckbox: CheckBox
    private lateinit var searchButton: Button
    
    private val petRepository = PetRepository()
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_advanced_search)
        
        // Initialize views
        initializeViews()
        
        // Setup toolbar
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)
        supportActionBar?.title = "Advanced Search"
        
        // Setup dropdowns
        setupDropdowns()
        
        // Setup search button
        searchButton.setOnClickListener {
            performAdvancedSearch()
        }
    }
    
    private fun initializeViews() {
        toolbar = findViewById(R.id.toolbar)
        typeDropdown = findViewById(R.id.typeDropdown)
        breedInput = findViewById(R.id.breedInput)
        genderDropdown = findViewById(R.id.genderDropdown)
        minAgeInput = findViewById(R.id.minAgeInput)
        maxAgeInput = findViewById(R.id.maxAgeInput)
        sizeDropdown = findViewById(R.id.sizeDropdown)
        colorInput = findViewById(R.id.colorInput)
        vaccinatedCheckbox = findViewById(R.id.vaccinatedCheckbox)
        spayedNeuteredCheckbox = findViewById(R.id.spayedNeuteredCheckbox)
        specialNeedsCheckbox = findViewById(R.id.specialNeedsCheckbox)
        energyLevelDropdown = findViewById(R.id.energyLevelDropdown)
        goodWithChildrenCheckbox = findViewById(R.id.goodWithChildrenCheckbox)
        goodWithDogsCheckbox = findViewById(R.id.goodWithDogsCheckbox)
        goodWithCatsCheckbox = findViewById(R.id.goodWithCatsCheckbox)
        searchButton = findViewById(R.id.searchButton)
    }
    
    private fun setupDropdowns() {
        // Pet Type Dropdown
        val petTypes = arrayOf("Dog", "Cat", "Bird", "Rabbit", "Fish", "Reptile", "Other")
        val typeAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, petTypes)
        typeDropdown.setAdapter(typeAdapter)
        
        // Gender Dropdown
        val genders = arrayOf("Male", "Female", "Any")
        val genderAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, genders)
        genderDropdown.setAdapter(genderAdapter)
        
        // Size Dropdown
        val sizes = arrayOf("Small", "Medium", "Large", "Any")
        val sizeAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, sizes)
        sizeDropdown.setAdapter(sizeAdapter)
        
        // Energy Level Dropdown
        val energyLevels = arrayOf("Low", "Medium", "High", "Any")
        val energyAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, energyLevels)
        energyLevelDropdown.setAdapter(energyAdapter)
    }
    
    private fun performAdvancedSearch() {
        // Collect filter values
        val type = if (typeDropdown.text.toString().isEmpty() || typeDropdown.text.toString() == "Any") null 
                   else typeDropdown.text.toString()
        
        val breed = if (breedInput.text.toString().isEmpty()) null 
                    else breedInput.text.toString()
        
        val gender = if (genderDropdown.text.toString().isEmpty() || genderDropdown.text.toString() == "Any") null 
                     else genderDropdown.text.toString()
        
        val minAge = try {
            if (minAgeInput.text.toString().isEmpty()) null
            else minAgeInput.text.toString().toInt()
        } catch (e: NumberFormatException) {
            null
        }
        
        val maxAge = try {
            if (maxAgeInput.text.toString().isEmpty()) null
            else maxAgeInput.text.toString().toInt()
        } catch (e: NumberFormatException) {
            null
        }
        
        val size = if (sizeDropdown.text.toString().isEmpty() || sizeDropdown.text.toString() == "Any") null 
                   else sizeDropdown.text.toString()
        
        val color = if (colorInput.text.toString().isEmpty()) null 
                    else colorInput.text.toString()
        
        val vaccinated = if (vaccinatedCheckbox.isChecked) true else null
        val spayedNeutered = if (spayedNeuteredCheckbox.isChecked) true else null
        
        val energyLevel = if (energyLevelDropdown.text.toString().isEmpty() || 
                             energyLevelDropdown.text.toString() == "Any") null 
                          else energyLevelDropdown.text.toString()
        
        // Build goodWith list
        val goodWithList = mutableListOf<String>()
        if (goodWithChildrenCheckbox.isChecked) goodWithList.add("children")
        if (goodWithDogsCheckbox.isChecked) goodWithList.add("dogs")
        if (goodWithCatsCheckbox.isChecked) goodWithList.add("cats")
        
        val goodWith = if (goodWithList.isEmpty()) null else goodWithList.first()
        
        // Perform search
        lifecycleScope.launch {
            try {
                val result = petRepository.getAllPets(
                    type = type,
                    breed = breed,
                    minAge = minAge,
                    maxAge = maxAge,
                    gender = gender,
                    size = size,
                    vaccinationStatus = vaccinated,
                    spayedNeutered = spayedNeutered,
                    goodWith = goodWith,
                    energyLevel = energyLevel
                )
                
                if (result.isSuccess) {
                    val pets = result.getOrNull() ?: emptyList()
                    
                    // Filter client-side for good with multiple values
                    val filteredPets = if (goodWithList.size > 1) {
                        pets.filter { pet ->
                            goodWithList.all { criterion -> pet.goodWith.contains(criterion) }
                        }
                    } else {
                        pets
                    }
                    
                    // Further filter by color if specified (client-side)
                    val colorFilteredPets = if (color != null) {
                        filteredPets.filter { pet ->
                            pet.color?.contains(color, ignoreCase = true) == true
                        }
                    } else {
                        filteredPets
                    }
                    
                    // Further filter by special needs if checked
                    val finalPets = if (specialNeedsCheckbox.isChecked) {
                        colorFilteredPets.filter { pet ->
                            pet.specialNeeds?.isNotEmpty() == true
                        }
                    } else {
                        colorFilteredPets
                    }
                    
                    // Navigate to search results
                    val intent = Intent(this@AdvancedSearchActivity, SearchResultsActivity::class.java)
                    intent.putExtra("SEARCH_RESULTS", ArrayList(finalPets))
                    startActivity(intent)
                } else {
                    Toast.makeText(
                        this@AdvancedSearchActivity,
                        "Error performing search. Please try again.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (e: Exception) {
                Toast.makeText(
                    this@AdvancedSearchActivity,
                    "Error: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }
    
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            onBackPressed()
            return true
        }
        return super.onOptionsItemSelected(item)
    }
} 