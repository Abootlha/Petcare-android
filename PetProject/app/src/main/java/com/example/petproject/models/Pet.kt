package com.example.petproject.models

import java.io.Serializable

data class Pet(
    val id: String = "",
    val name: String = "",
    val type: String = "",
    val breed: String = "",
    val age: Int = 0,
    val gender: String = "",
    val description: String = "",
    val imageUrl: String = "",
    val price: Double = 0.0,
    val ownerId: String = "",
    val ownerName: String = "",
    val ownerContact: String = "",
    val isAdopted: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
) : Serializable