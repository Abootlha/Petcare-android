package com.basic.petproject.models

import java.io.Serializable

enum class MessageStatus {
    SENDING,
    SENT,
    DELIVERED,
    READ
}

enum class MessageType {
    TEXT,
    IMAGE
}

data class Message(
    val id: String = "",
    val senderId: String = "",
    val receiverId: String = "",
    val content: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val status: MessageStatus = MessageStatus.SENDING,
    val type: MessageType = MessageType.TEXT,
    val imageUrl: String = "",
    val petId: String = "" // Optional reference to a pet if the conversation is about a specific pet
) : Serializable