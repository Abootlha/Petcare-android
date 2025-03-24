package com.example.petproject.repositories

import com.example.petproject.models.Message
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await

class MessageRepository {
    private val db = FirebaseFirestore.getInstance()
    private val messagesCollection = db.collection("messages")
    private val auth = FirebaseAuth.getInstance()
    
    suspend fun sendMessage(message: Message): Result<String> {
        return try {
            val messageWithId = if (message.id.isEmpty()) {
                val newId = messagesCollection.document().id
                message.copy(id = newId)
            } else {
                message
            }
            
            messagesCollection.document(messageWithId.id).set(messageWithId).await()
            Result.success(messageWithId.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
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
    
    suspend fun markMessageAsRead(messageId: String): Result<Unit> {
        return try {
            messagesCollection.document(messageId).update("isRead", true).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun deleteMessage(messageId: String): Result<Unit> {
        return try {
            messagesCollection.document(messageId).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}