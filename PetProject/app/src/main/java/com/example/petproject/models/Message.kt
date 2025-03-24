package com.example.petproject.models

import java.io.Serializable

data class Message(
    val id: String = "",
    val senderId: String = "",
    val receiverId: String = "",
    val content: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val isRead: Boolean = false,
    val petId: String = "" // Optional reference to a pet if the conversation is about a specific pet
) : Serializable