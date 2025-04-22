package com.basic.petproject

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.basic.petproject.adapters.PetAdapter
import com.basic.petproject.base.BaseNavigationActivity

class SearchActivity : BaseNavigationActivity() {
    private lateinit var searchEditText: EditText
    private lateinit var searchButton: ImageButton
    private lateinit var advancedSearchButton: Button
    private lateinit var searchResultsRecyclerView: RecyclerView
    private lateinit var petAdapter: PetAdapter
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_search)
        
        // Setup bottom navigation after setContentView
        setupBottomNavigation()
        
        // Initialize views
        searchEditText = findViewById(R.id.searchEditText)
        searchButton = findViewById(R.id.searchButton)
        advancedSearchButton = findViewById(R.id.advancedSearchButton)
        searchResultsRecyclerView = findViewById(R.id.searchResultsRecyclerView)
        
        // Setup RecyclerView
        searchResultsRecyclerView.layoutManager = GridLayoutManager(this, 2)
        petAdapter = PetAdapter(emptyList()) { pet ->
            // Handle pet item click - open pet details
            val intent = Intent(this, PetDetailActivity::class.java)
            intent.putExtra("pet", pet)
            startActivity(intent)
        }
        searchResultsRecyclerView.adapter = petAdapter
        
        // Setup search button
        searchButton.setOnClickListener {
            performSearch()
        }
        
        // Setup advanced search button
        advancedSearchButton.setOnClickListener {
            startActivity(Intent(this, AdvancedSearchActivity::class.java))
        }
        
        // Set the search item as selected
        bottomNavigation.selectedItemId = R.id.nav_search
    }
    
    private fun performSearch() {
        val searchQuery = searchEditText.text.toString().trim()
        if (searchQuery.isNotEmpty()) {
            // TODO: Implement search functionality
            // For now, just navigate to search results with the query
            val intent = Intent(this, SearchResultsActivity::class.java)
            intent.putExtra("query", searchQuery)
            startActivity(intent)
        }
    }
}