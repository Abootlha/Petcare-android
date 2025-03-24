package com.example.petproject

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.petproject.adapters.PetAdapter
import com.example.petproject.models.Pet
import com.example.petproject.repositories.PetRepository
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.launch

class PetListingActivity : AppCompatActivity() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var petAdapter: PetAdapter
    private lateinit var progressBar: ProgressBar
    private lateinit var emptyView: TextView
    private lateinit var addPetButton: FloatingActionButton
    private val petRepository = PetRepository()
    private val TAG = "PetListingActivity"
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pet_listing)
        
        title = "Available Pets"
        
        // Initialize views
        recyclerView = findViewById(R.id.recyclerViewPets)
        progressBar = findViewById(R.id.progressBar)
        emptyView = findViewById(R.id.emptyView)
        addPetButton = findViewById(R.id.fabAddPet)
        
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
                    val pets = result.getOrNull() ?: emptyList()
                    updateUI(pets)
                } else {
                    val exception = result.exceptionOrNull()
                    Log.e(TAG, "Error loading pets: ${exception?.message}")
                    Toast.makeText(this@PetListingActivity, "Error loading pets: ${exception?.message}", Toast.LENGTH_SHORT).show()
                    updateUI(emptyList())
                }
            } catch (e: Exception) {
                progressBar.visibility = View.GONE
                Log.e(TAG, "Exception loading pets: ${e.message}")
                Toast.makeText(this@PetListingActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                updateUI(emptyList())
            }
        }
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