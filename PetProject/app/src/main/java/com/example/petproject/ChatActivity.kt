package com.example.petproject

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.petproject.adapters.MessageAdapter
import com.example.petproject.models.Message
import com.example.petproject.repositories.MessageRepository
import kotlinx.coroutines.launch

class ChatActivity : AppCompatActivity() {
    private lateinit var recyclerViewMessages: RecyclerView
    private lateinit var editTextMessage: EditText
    private lateinit var buttonSend: Button
    private lateinit var textViewChatHeader: TextView
    
    private lateinit var messageAdapter: MessageAdapter
    private val messageRepository = MessageRepository()
    
    private lateinit var ownerId: String
    private lateinit var ownerName: String
    private val currentUserId = "current_user_id" // This should be the actual logged-in user ID
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat)
        
        // Get owner info from intent
        ownerId = intent.getStringExtra("ownerId") ?: ""
        ownerName = intent.getStringExtra("ownerName") ?: "Pet Owner"
        
        if (ownerId.isEmpty()) {
            Toast.makeText(this, "Error: Owner information not found", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        
        // Initialize views
        recyclerViewMessages = findViewById(R.id.recyclerViewMessages)
        editTextMessage = findViewById(R.id.editTextMessage)
        buttonSend = findViewById(R.id.buttonSend)
        textViewChatHeader = findViewById(R.id.textViewChatHeader)
        
        // Set chat header
        textViewChatHeader.text = "Chat with $ownerName"
        
        // Setup RecyclerView
        recyclerViewMessages.layoutManager = LinearLayoutManager(this).apply {
            stackFromEnd = true // Messages appear from bottom
        }
        messageAdapter = MessageAdapter(emptyList(), currentUserId)
        recyclerViewMessages.adapter = messageAdapter
        
        // Setup send button
        buttonSend.setOnClickListener {
            sendMessage()
        }
        
        // Load messages
        loadMessages()
    }
    
    private fun loadMessages() {
        lifecycleScope.launch {
            try {
                val result = messageRepository.getMessages(currentUserId, ownerId)
                if (result.isSuccess) {
                    val messages = result.getOrNull() ?: emptyList()
                    messageAdapter.updateMessages(messages)
                    if (messages.isNotEmpty()) {
                        recyclerViewMessages.scrollToPosition(messages.size - 1)
                    }
                } else {
                    Toast.makeText(this@ChatActivity, "Error loading messages", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@ChatActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    private fun sendMessage() {
        val messageText = editTextMessage.text.toString().trim()
        if (messageText.isEmpty()) return
        
        val message = Message(
            senderId = currentUserId,
            receiverId = ownerId,
            content = messageText,
            timestamp = System.currentTimeMillis()
        )
        
        lifecycleScope.launch {
            try {
                val result = messageRepository.sendMessage(message)
                if (result.isSuccess) {
                    editTextMessage.text.clear()
                    loadMessages() // Reload messages to show the new one
                } else {
                    Toast.makeText(this@ChatActivity, "Failed to send message", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@ChatActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
}