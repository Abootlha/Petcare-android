package com.basic.petproject.models

import java.io.Serializable

/**
 * Represents a conversation between two users
 */
data class Conversation(
    val id: String = "",
    val participants: List<String> = emptyList(),
    val lastMessage: String = "",
    val timestamp: Long = 0,
    val unreadCount: Int = 0,
    val otherUserId: String = "",
    val otherUserName: String = "",
    val otherUserProfilePic: String = "",
    val petId: String = "" // Optional - if the conversation is about a specific pet
) : Serializable 