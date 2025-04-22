package com.basic.petproject

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.basic.petproject.adapters.ConversationAdapter
import com.basic.petproject.models.Conversation
import com.basic.petproject.models.Message
import com.basic.petproject.repositories.MessageRepository
import kotlinx.coroutines.launch

class ConversationsActivity : AppCompatActivity() {
    private lateinit var recyclerViewConversations: RecyclerView
    private lateinit var textViewNoConversations: TextView
    private lateinit var progressBarConversations: ProgressBar
    private lateinit var conversationsToolbar: Toolbar
    
    private lateinit var conversationAdapter: ConversationAdapter
    private val messageRepository = MessageRepository()
    
    private lateinit var currentUserId: String
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_conversations)
        
        // Get current user ID from shared preferences
        val sharedPreferences = getSharedPreferences("UserPrefs", MODE_PRIVATE)
        currentUserId = sharedPreferences.getString("userId", "") ?: ""
        
        if (currentUserId.isEmpty()) {
            Toast.makeText(this, "Error: User not found", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        
        // Initialize views
        initializeViews()
        
        // Setup toolbar
        setSupportActionBar(conversationsToolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowTitleEnabled(false)
        
        // Setup RecyclerView
        setupRecyclerView()
        
        // Start listening for conversations
        loadConversations()
    }
    
    private fun initializeViews() {
        recyclerViewConversations = findViewById(R.id.recyclerViewConversations)
        textViewNoConversations = findViewById(R.id.textViewNoConversations)
        progressBarConversations = findViewById(R.id.progressBarConversations)
        conversationsToolbar = findViewById(R.id.conversationsToolbar)
    }
    
    private fun setupRecyclerView() {
        recyclerViewConversations.layoutManager = LinearLayoutManager(this)
        recyclerViewConversations.addItemDecoration(
            DividerItemDecoration(this, DividerItemDecoration.VERTICAL)
        )
        
        conversationAdapter = ConversationAdapter(emptyList()) { conversation ->
            openChatActivity(conversation)
        }
        recyclerViewConversations.adapter = conversationAdapter
    }
    
    private fun loadConversations() {
        showLoading(true)
        
        lifecycleScope.launch {
            try {
                val result = messageRepository.getConversationsForUser(currentUserId)
                
                showLoading(false)
                
                if (result.isSuccess) {
                    val conversationsMap = result.getOrNull() ?: emptyMap()
                    val conversations = convertToConversations(conversationsMap)
                    conversationAdapter.updateConversations(conversations)
                    showEmptyState(conversations.isEmpty())
                } else {
                    val exception = result.exceptionOrNull()
                    showError("Error: ${exception?.message}")
                    showEmptyState(true)
                }
            } catch (e: Exception) {
                showLoading(false)
                showError("Error: ${e.message}")
                showEmptyState(true)
            }
        }
    }
    
    private fun convertToConversations(conversationsMap: Map<String, List<Message>>): List<Conversation> {
        val conversations = mutableListOf<Conversation>()
        
        // For each conversation partner (the key in the map)
        conversationsMap.forEach { (otherUserId, messages) ->
            if (messages.isNotEmpty()) {
                // Sort messages by timestamp and get the last message
                val lastMessage = messages.maxByOrNull { it.timestamp }!!
                
                // Count unread messages where current user is the receiver
                val unreadCount = messages.count { 
                    it.receiverId == currentUserId && 
                    it.status != com.basic.petproject.models.MessageStatus.READ
                }
                
                // Get pet ID if any
                val petId = messages.firstOrNull { it.petId.isNotEmpty() }?.petId ?: ""
                
                // TODO: Get other user profile details from UserRepository
                // For now, use placeholder data
                val otherUserName = "User $otherUserId" // Replace with actual username
                val otherUserProfilePic = "" // Replace with actual profile pic URL
                
                // Create Conversation object
                val conversation = Conversation(
                    id = if (currentUserId < otherUserId) "$currentUserId-$otherUserId" else "$otherUserId-$currentUserId",
                    participants = listOf(currentUserId, otherUserId),
                    lastMessage = lastMessage.content,
                    timestamp = lastMessage.timestamp,
                    unreadCount = unreadCount,
                    otherUserId = otherUserId,
                    otherUserName = otherUserName,
                    otherUserProfilePic = otherUserProfilePic,
                    petId = petId
                )
                
                conversations.add(conversation)
            }
        }
        
        // Sort conversations by timestamp (most recent first)
        return conversations.sortedByDescending { it.timestamp }
    }
    
    private fun openChatActivity(conversation: Conversation) {
        val intent = Intent(this, ChatActivity::class.java).apply {
            putExtra("ownerId", conversation.otherUserId)
            putExtra("ownerName", conversation.otherUserName)
            putExtra("petId", conversation.petId)
        }
        startActivity(intent)
    }
    
    private fun showLoading(isLoading: Boolean) {
        progressBarConversations.visibility = if (isLoading) View.VISIBLE else View.GONE
    }
    
    private fun showEmptyState(isEmpty: Boolean) {
        textViewNoConversations.visibility = if (isEmpty) View.VISIBLE else View.GONE
        recyclerViewConversations.visibility = if (isEmpty) View.GONE else View.VISIBLE
    }
    
    private fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
    
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }
    
    override fun onResume() {
        super.onResume()
        // Refresh conversations list when returning to this activity
        loadConversations()
    }
} 