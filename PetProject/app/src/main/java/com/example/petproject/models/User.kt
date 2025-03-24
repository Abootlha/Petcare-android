package com.example.petproject.models

import java.io.Serializable

data class User(
    val id: String = "",
    val name: String = "",
    val email: String = "",
    val phone: String = "",
    val address: String = "",
    val profileImageUrl: String = "",
    val createdAt: Long = System.currentTimeMillis()
) : Serializable