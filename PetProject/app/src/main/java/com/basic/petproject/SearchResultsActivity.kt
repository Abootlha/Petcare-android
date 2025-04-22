package com.basic.petproject

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.basic.petproject.adapters.PetAdapter
import com.basic.petproject.models.Pet

class SearchResultsActivity : AppCompatActivity() {
    private lateinit var toolbar: Toolbar
    private lateinit var resultsCountTextView: TextView
    private lateinit var resultsRecyclerView: RecyclerView
    private lateinit var emptyResultsTextView: TextView
    private lateinit var progressBar: ProgressBar
    
    private lateinit var petAdapter: PetAdapter
    private var searchResults = listOf<Pet>()
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_search_results)
        
        // Initialize views
        initializeViews()
        
        // Setup toolbar
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)
        
        // Setup recycler view
        setupRecyclerView()
        
        // Get search results from intent
        getSearchResults()
    }
    
    private fun initializeViews() {
        toolbar = findViewById(R.id.toolbar)
        resultsCountTextView = findViewById(R.id.resultsCountTextView)
        resultsRecyclerView = findViewById(R.id.resultsRecyclerView)
        emptyResultsTextView = findViewById(R.id.emptyResultsTextView)
        progressBar = findViewById(R.id.progressBar)
    }
    
    private fun setupRecyclerView() {
        resultsRecyclerView.layoutManager = GridLayoutManager(this, 2)
        petAdapter = PetAdapter(emptyList()) { pet ->
            val intent = Intent(this, PetDetailActivity::class.java)
            intent.putExtra("pet", pet)
            startActivity(intent)
        }
        resultsRecyclerView.adapter = petAdapter
    }
    
    private fun getSearchResults() {
        showLoading(true)
        
        @Suppress("UNCHECKED_CAST")
        val results = intent.getSerializableExtra("SEARCH_RESULTS") as? ArrayList<Pet>
        
        if (results != null) {
            searchResults = results
            updateUI()
        } else {
            showEmptyResults()
        }
        
        showLoading(false)
    }
    
    private fun updateUI() {
        if (searchResults.isNotEmpty()) {
            // Update count text
            val countText = "Found ${searchResults.size} " + 
                if (searchResults.size == 1) "pet" else "pets" + 
                " matching your criteria"
            resultsCountTextView.text = countText
            
            // Update recycler view
            petAdapter.updatePets(searchResults)
            resultsRecyclerView.visibility = View.VISIBLE
            emptyResultsTextView.visibility = View.GONE
        } else {
            showEmptyResults()
        }
    }
    
    private fun showEmptyResults() {
        resultsCountTextView.text = "Search Results"
        resultsRecyclerView.visibility = View.GONE
        emptyResultsTextView.visibility = View.VISIBLE
    }
    
    private fun showLoading(isLoading: Boolean) {
        progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        if (isLoading) {
            resultsRecyclerView.visibility = View.GONE
            emptyResultsTextView.visibility = View.GONE
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