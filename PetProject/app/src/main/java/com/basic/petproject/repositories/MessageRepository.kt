package com.basic.petproject.repositories

import com.basic.petproject.models.Message
import com.basic.petproject.models.MessageStatus
import com.basic.petproject.models.MessageType
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class MessageRepository {
    private val db = FirebaseFirestore.getInstance()
    private val messagesCollection = db.collection("messages")
    private val auth = FirebaseAuth.getInstance()
    
    // Map to store message listeners for different conversations
    private val messageListeners = mutableMapOf<String, ListenerRegistration>()
    
    suspend fun sendMessage(message: Message): Result<String> {
        return try {
            val messageWithId = if (message.id.isEmpty()) {
                val newId = messagesCollection.document().id
                message.copy(id = newId, status = MessageStatus.SENDING)
            } else {
                message
            }
            
            // Save to Firestore
            messagesCollection.document(messageWithId.id).set(messageWithId).await()
            
            // Update status to SENT after successful save
            updateMessageStatus(messageWithId.id, MessageStatus.SENT)
            
            Result.success(messageWithId.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // Send an image message
    suspend fun sendImageMessage(receiverId: String, imageUrl: String, petId: String = ""): Result<String> {
        val message = Message(
            senderId = auth.currentUser?.uid ?: "",
            receiverId = receiverId,
            content = "Image",
            type = MessageType.IMAGE,
            imageUrl = imageUrl,
            petId = petId,
            timestamp = System.currentTimeMillis(),
            status = MessageStatus.SENDING
        )
        
        return sendMessage(message)
    }
    
    suspend fun getMessageById(messageId: String): Result<Message?> {
        return try {
            val document = messagesCollection.document(messageId).get().await()
            val message = document.toObject(Message::class.java)
            Result.success(message)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun getMessages(userId1: String, userId2: String): Result<List<Message>> {
        return getConversation(userId1, userId2)
    }
    
    suspend fun getConversation(userId1: String, userId2: String): Result<List<Message>> {
        return try {
            // Get messages where current user is sender and other user is receiver
            val query1 = messagesCollection
                .whereEqualTo("senderId", userId1)
                .whereEqualTo("receiverId", userId2)
                
            // Get messages where current user is receiver and other user is sender
            val query2 = messagesCollection
                .whereEqualTo("senderId", userId2)
                .whereEqualTo("receiverId", userId1)
                
            val messages1 = query1.get().await().documents.mapNotNull { it.toObject(Message::class.java) }
            val messages2 = query2.get().await().documents.mapNotNull { it.toObject(Message::class.java) }
            
            // Combine and sort by timestamp
            val allMessages = (messages1 + messages2).sortedBy { it.timestamp }
            
            // Mark received messages as read
            withContext(Dispatchers.IO) {
                allMessages
                    .filter { it.receiverId == userId1 && it.status != MessageStatus.READ }
                    .forEach { markMessageAsRead(it.id) }
            }
            
            Result.success(allMessages)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun getConversationsForUser(userId: String): Result<Map<String, List<Message>>> {
        return try {
            // Get messages where user is sender
            val query1 = messagesCollection
                .whereEqualTo("senderId", userId)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .await()
                
            // Get messages where user is receiver
            val query2 = messagesCollection
                .whereEqualTo("receiverId", userId)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .await()
                
            val sentMessages = query1.documents.mapNotNull { it.toObject(Message::class.java) }
            val receivedMessages = query2.documents.mapNotNull { it.toObject(Message::class.java) }
            
            // Combine all messages
            val allMessages = sentMessages + receivedMessages
            
            // Group by conversation partner
            val conversations = allMessages.groupBy { message ->
                if (message.senderId == userId) message.receiverId else message.senderId
            }
            
            Result.success(conversations)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun updateMessageStatus(messageId: String, status: MessageStatus): Result<Unit> {
        return try {
            messagesCollection.document(messageId).update("status", status).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun markMessageAsRead(messageId: String): Result<Unit> {
        return updateMessageStatus(messageId, MessageStatus.READ)
    }
    
    suspend fun markMessageAsDelivered(messageId: String): Result<Unit> {
        return updateMessageStatus(messageId, MessageStatus.DELIVERED)
    }
    
    suspend fun deleteMessage(messageId: String): Result<Unit> {
        return try {
            messagesCollection.document(messageId).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // Real-time updates for messages in a conversation
    fun listenForMessages(userId1: String, userId2: String, onUpdate: (List<Message>) -> Unit): String {
        val chatId = if (userId1 < userId2) "$userId1-$userId2" else "$userId2-$userId1"
        
        // Cancel existing listener if any
        messageListeners[chatId]?.remove()
        
        // Create a new listener
        val query1 = messagesCollection
            .whereEqualTo("senderId", userId1)
            .whereEqualTo("receiverId", userId2)
        
        val query2 = messagesCollection
            .whereEqualTo("senderId", userId2)
            .whereEqualTo("receiverId", userId1)
        
        // We will use a combination approach to get real-time updates
        val listener1 = query1.addSnapshotListener { snapshot, error ->
            if (error != null) {
                return@addSnapshotListener
            }
            
            // We're just triggering a fetch operation when changes occur
            fetchAllMessages(userId1, userId2, onUpdate)
        }
        
        val listener2 = query2.addSnapshotListener { snapshot, error ->
            if (error != null) {
                return@addSnapshotListener
            }
            
            // We're just triggering a fetch operation when changes occur
            fetchAllMessages(userId1, userId2, onUpdate)
        }
        
        // Store only one listener (we'll use the first one as the key)
        messageListeners[chatId] = listener1
        
        // Initial fetch
        fetchAllMessages(userId1, userId2, onUpdate)
        
        return chatId
    }
    
    private fun fetchAllMessages(userId1: String, userId2: String, onUpdate: (List<Message>) -> Unit) {
        val query1 = messagesCollection
            .whereEqualTo("senderId", userId1)
            .whereEqualTo("receiverId", userId2)
        
        val query2 = messagesCollection
            .whereEqualTo("senderId", userId2)
            .whereEqualTo("receiverId", userId1)
        
        query1.get().addOnSuccessListener { snapshot1 ->
            val messages1 = snapshot1.documents.mapNotNull { it.toObject(Message::class.java) }
            
            query2.get().addOnSuccessListener { snapshot2 ->
                val messages2 = snapshot2.documents.mapNotNull { it.toObject(Message::class.java) }
                
                // Combine and sort by timestamp
                val allMessages = (messages1 + messages2).sortedBy { it.timestamp }
                
                // Auto-mark messages as delivered/read if current user is the receiver
                allMessages
                    .filter { it.receiverId == userId1 && it.status != MessageStatus.READ }
                    .forEach { message ->
                        messagesCollection.document(message.id).update("status", MessageStatus.DELIVERED)
                    }
                
                // Send the update
                onUpdate(allMessages)
            }
        }
    }
    
    // Stop listening for updates
    fun stopListeningForMessages(chatId: String) {
        messageListeners[chatId]?.remove()
        messageListeners.remove(chatId)
    }
}