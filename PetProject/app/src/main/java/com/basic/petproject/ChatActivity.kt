package com.basic.petproject

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.basic.petproject.adapters.MessageAdapter
import com.basic.petproject.models.Message
import com.basic.petproject.models.MessageStatus
import com.basic.petproject.models.MessageType
import com.basic.petproject.repositories.MessageRepository
import com.basic.petproject.utils.FirebaseStorageHelper
import kotlinx.coroutines.launch

class ChatActivity : AppCompatActivity() {
    private lateinit var recyclerViewMessages: RecyclerView
    private lateinit var editTextMessage: EditText
    private lateinit var buttonSend: ImageButton
    private lateinit var buttonAttach: ImageButton
    private lateinit var textViewChatHeader: TextView
    private lateinit var loadingIndicator: ProgressBar
    private lateinit var backButton: ImageView
    private lateinit var chatToolbar: Toolbar
    
    private lateinit var messageAdapter: MessageAdapter
    private val messageRepository = MessageRepository()
    private val storageHelper = FirebaseStorageHelper()
    
    private lateinit var ownerId: String
    private lateinit var ownerName: String
    private lateinit var currentUserId: String
    private var petId: String? = null
    private var chatId: String = ""
    
    // Permission request code
    private val REQUEST_IMAGE_PERMISSION = 100
    
    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                uploadAndSendImage(uri)
            }
        }
    }
    
    // Permission result launcher
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions.entries.all { it.value }
        if (granted) {
            // Permissions granted, open image picker
            launchImagePicker()
        } else {
            Toast.makeText(this, "Permission denied. Cannot select images.", Toast.LENGTH_SHORT).show()
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat)
        
        // Get current user ID from shared preferences
        val sharedPreferences = getSharedPreferences("UserPrefs", MODE_PRIVATE)
        currentUserId = sharedPreferences.getString("userId", "") ?: ""
        
        // Get owner info from intent
        ownerId = intent.getStringExtra("ownerId") ?: ""
        ownerName = intent.getStringExtra("ownerName") ?: "Pet Owner"
        petId = intent.getStringExtra("petId")
        
        if (ownerId.isEmpty() || currentUserId.isEmpty()) {
            Toast.makeText(this, "Error: Required information not found", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        
        // Initialize views
        initializeViews()
        
        // Setup toolbar and back button
        setSupportActionBar(chatToolbar)
        supportActionBar?.setDisplayShowTitleEnabled(false)
        backButton.setOnClickListener { finish() }
        
        // Set chat header
        textViewChatHeader.text = "Chat with $ownerName"
        
        // Setup RecyclerView
        setupRecyclerView()
        
        // Setup button listeners
        setupButtonListeners()
        
        // Start listening for messages
        startMessageListener()
        
        // Mark all received messages as read
        markMessagesAsRead()
    }
    
    private fun initializeViews() {
        recyclerViewMessages = findViewById(R.id.recyclerViewMessages)
        editTextMessage = findViewById(R.id.editTextMessage)
        buttonSend = findViewById(R.id.buttonSend)
        buttonAttach = findViewById(R.id.buttonAttach)
        textViewChatHeader = findViewById(R.id.textViewChatHeader)
        loadingIndicator = findViewById(R.id.loadingIndicator)
        backButton = findViewById(R.id.backButton)
        chatToolbar = findViewById(R.id.chatToolbar)
    }
    
    private fun setupRecyclerView() {
        recyclerViewMessages.layoutManager = LinearLayoutManager(this).apply {
            stackFromEnd = true // Messages appear from bottom
        }
        messageAdapter = MessageAdapter(emptyList(), currentUserId)
        recyclerViewMessages.adapter = messageAdapter
    }
    
    private fun setupButtonListeners() {
        buttonSend.setOnClickListener {
            sendTextMessage()
        }
        
        buttonAttach.setOnClickListener {
            openImagePicker()
        }
    }
    
    private fun openImagePicker() {
        // Check for permissions first
        if (hasImagePermissions()) {
            launchImagePicker()
        } else {
            requestImagePermissions()
        }
    }
    
    private fun hasImagePermissions(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.READ_MEDIA_IMAGES
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.READ_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
        }
    }
    
    private fun requestImagePermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestPermissionLauncher.launch(arrayOf(Manifest.permission.READ_MEDIA_IMAGES))
        } else {
            requestPermissionLauncher.launch(arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE))
        }
    }
    
    private fun launchImagePicker() {
        try {
            // Create a chooser intent
            val intent = Intent(Intent.ACTION_PICK).apply {
                type = "image/*"
                putExtra(Intent.EXTRA_MIME_TYPES, arrayOf("image/jpeg", "image/png"))
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            
            // Check if there's an app that can handle this intent
            if (intent.resolveActivity(packageManager) != null) {
                pickImageLauncher.launch(intent)
            } else {
                // Fallback to a basic intent if no specific app is found
                val fallbackIntent = Intent(Intent.ACTION_GET_CONTENT).apply {
                    type = "image/*"
                    addCategory(Intent.CATEGORY_OPENABLE)
                }
                pickImageLauncher.launch(fallbackIntent)
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Error opening image picker: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun uploadAndSendImage(imageUri: Uri) {
        showLoading(true)
        
        lifecycleScope.launch {
            try {
                // Verify we have a valid URI
                if (imageUri.toString().isBlank()) {
                    Toast.makeText(this@ChatActivity, "Invalid image selected", Toast.LENGTH_SHORT).show()
                    return@launch
                }
                
                // Check file permissions
                try {
                    contentResolver.openInputStream(imageUri)?.close()
                } catch (e: SecurityException) {
                    Toast.makeText(this@ChatActivity, "Cannot access the selected image", Toast.LENGTH_SHORT).show()
                    return@launch
                }
                
                // Upload the image
                val imageUrl = storageHelper.uploadImage(imageUri, "chat_images")
                
                if (imageUrl.isNotEmpty()) {
                    val message = Message(
                        senderId = currentUserId,
                        receiverId = ownerId,
                        content = "Image",
                        timestamp = System.currentTimeMillis(),
                        type = MessageType.IMAGE,
                        imageUrl = imageUrl,
                        petId = petId ?: ""
                    )
                    
                    val result = messageRepository.sendMessage(message)
                    if (!result.isSuccess) {
                        Toast.makeText(this@ChatActivity, "Failed to send image", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(this@ChatActivity, "Failed to upload image", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@ChatActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                showLoading(false)
            }
        }
    }
    
    private fun sendTextMessage() {
        val messageText = editTextMessage.text.toString().trim()
        if (messageText.isEmpty()) return
        
        val message = Message(
            senderId = currentUserId,
            receiverId = ownerId,
            content = messageText,
            timestamp = System.currentTimeMillis(),
            type = MessageType.TEXT,
            petId = petId ?: ""
        )
        
        lifecycleScope.launch {
            try {
                messageRepository.sendMessage(message)
                editTextMessage.text.clear()
            } catch (e: Exception) {
                Toast.makeText(this@ChatActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    private fun startMessageListener() {
        chatId = messageRepository.listenForMessages(currentUserId, ownerId) { messages ->
            messageAdapter.updateMessages(messages)
            if (messages.isNotEmpty()) {
                recyclerViewMessages.scrollToPosition(messages.size - 1)
            }
        }
    }
    
    private fun markMessagesAsRead() {
        lifecycleScope.launch {
            try {
                // Get all messages from the conversation
                val result = messageRepository.getMessages(currentUserId, ownerId)
                
                if (result.isSuccess) {
                    val messages = result.getOrNull() ?: emptyList()
                    
                    // Mark messages where current user is the receiver and status isn't READ
                    messages
                        .filter { it.receiverId == currentUserId && it.status != MessageStatus.READ }
                        .forEach { message ->
                            messageRepository.markMessageAsRead(message.id)
                        }
                }
            } catch (e: Exception) {
                // Silently handle error - not critical to user experience
            }
        }
    }
    
    private fun showLoading(isLoading: Boolean) {
        loadingIndicator.visibility = if (isLoading) View.VISIBLE else View.GONE
    }
    
    override fun onDestroy() {
        super.onDestroy()
        // Stop listening for messages when activity is destroyed
        if (chatId.isNotEmpty()) {
            messageRepository.stopListeningForMessages(chatId)
        }
    }
}