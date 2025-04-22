package com.basic.petproject

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.basic.petproject.adapters.ApplicationsAdapter
import com.basic.petproject.models.AdoptionApplication
import com.basic.petproject.models.Pet
import com.basic.petproject.repositories.AdoptionRepository
import com.basic.petproject.repositories.PetRepository
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MyApplicationsActivity : AppCompatActivity() {
    private lateinit var toolbar: Toolbar
    private lateinit var recyclerView: RecyclerView
    private lateinit var emptyView: TextView
    private lateinit var progressBar: ProgressBar
    
    private lateinit var adapter: ApplicationsAdapter
    private val adoptionRepository = AdoptionRepository()
    private val petRepository = PetRepository()
    
    private val applications = mutableListOf<AdoptionApplication>()
    private val petMap = mutableMapOf<String, Pet>()
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_my_applications)
        
        // Initialize views
        initializeViews()
        
        // Setup toolbar
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)
        toolbar.setNavigationOnClickListener { onBackPressed() }
        
        // Setup RecyclerView
        setupRecyclerView()
        
        // Load applications
        loadMyApplications()
    }
    
    private fun initializeViews() {
        toolbar = findViewById(R.id.toolbar)
        recyclerView = findViewById(R.id.applicationsRecyclerView)
        emptyView = findViewById(R.id.emptyView)
        progressBar = findViewById(R.id.progressBar)
    }
    
    private fun setupRecyclerView() {
        recyclerView.layoutManager = LinearLayoutManager(this)
        adapter = ApplicationsAdapter(this, applications, petMap) { application ->
            // Navigate to application details or pet details
            val intent = Intent(this, PetDetailActivity::class.java)
            petMap[application.petId]?.let { pet ->
                intent.putExtra("pet", pet)
                startActivity(intent)
            }
        }
        recyclerView.adapter = adapter
    }
    
    private fun loadMyApplications() {
        showLoading(true)
        
        lifecycleScope.launch {
            try {
                val result = withContext(Dispatchers.IO) {
                    adoptionRepository.getUserApplications()
                }
                
                withContext(Dispatchers.Main) {
                    if (result.isSuccess) {
                        val applications = result.getOrNull() ?: emptyList()
                        adapter.updateApplications(applications)
                        showEmptyState(applications.isEmpty())
                    } else {
                        val error = result.exceptionOrNull()
                        Toast.makeText(
                            this@MyApplicationsActivity,
                            "Error: ${error?.message ?: "Unknown error"}",
                            Toast.LENGTH_SHORT
                        ).show()
                        showEmptyState(true)
                    }
                    showLoading(false)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        this@MyApplicationsActivity,
                        "Error: ${e.message ?: "Unknown error"}",
                        Toast.LENGTH_SHORT
                    ).show()
                    showEmptyState(true)
                    showLoading(false)
                }
            }
        }
    }
    
    private fun showLoading(isLoading: Boolean) {
        progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        if (isLoading) {
            recyclerView.visibility = View.GONE
            emptyView.visibility = View.GONE
        }
    }
    
    private fun showEmptyState(isEmpty: Boolean) {
        recyclerView.visibility = if (isEmpty) View.GONE else View.VISIBLE
        emptyView.visibility = if (isEmpty) View.VISIBLE else View.GONE
    }
    
    override fun onResume() {
        super.onResume()
        // Refresh applications when the activity resumes
        loadMyApplications()
    }
} 