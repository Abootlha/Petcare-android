package com.example.petproject

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
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
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.launch

class Dashboard : AppCompatActivity() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var petAdapter: PetAdapter
    private lateinit var progressBar: ProgressBar
    private lateinit var emptyView: TextView
    private lateinit var addPetButton: Button
    private lateinit var bottomNavigation: BottomNavigationView
    private val petRepository = PetRepository()
    private val TAG = "Dashboard"
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dashboard)

        // Initialize views
        recyclerView = findViewById(R.id.petsRecyclerView)
        // TODO: Add progressBar and emptyView to layout
        // progressBar = findViewById(R.id.progressBar)
        // emptyView = findViewById(R.id.emptyView)
        addPetButton = findViewById(R.id.addPetButton)
        bottomNavigation = findViewById(R.id.bottomNavigation)
        
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
        
        // Setup bottom navigation
        bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    // Already on home screen
                    true
                }
                R.id.nav_pets -> {
                    startActivity(Intent(this, PetListingActivity::class.java))
                    false
                }
                R.id.nav_profile -> {
                    // Navigate to profile screen (placeholder for now)
                    Toast.makeText(this, "Profile feature coming soon", Toast.LENGTH_SHORT).show()
                    false
                }
                else -> false
            }
        }
        
        // Set the home item as selected
        bottomNavigation.selectedItemId = R.id.nav_home
        
        // Load pets
        // Temporarily comment out loadPets() until progressBar and emptyView are added
        // loadPets()
    }
    
    override fun onResume() {
        super.onResume()
        // Refresh pet list when returning to this activity
        // Temporarily comment out loadPets() until progressBar and emptyView are added
        // loadPets()
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
                    petAdapter.updatePets(pets)
                    
                    if (pets.isEmpty()) {
                        emptyView.visibility = View.VISIBLE
                    } else {
                        emptyView.visibility = View.GONE
                    }
                } else {
                    val exception = result.exceptionOrNull()
                    Log.e(TAG, "Error loading pets", exception)
                    Toast.makeText(this@Dashboard, "Error loading pets: ${exception?.message}", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Exception in loadPets", e)
                progressBar.visibility = View.GONE
                Toast.makeText(this@Dashboard, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
}