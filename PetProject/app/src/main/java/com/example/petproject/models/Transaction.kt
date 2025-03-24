package com.example.petproject.models

import java.io.Serializable

data class Transaction(
    val id: String = "",
    val petId: String = "",
    val buyerId: String = "",
    val sellerId: String = "",
    val amount: Double = 0.0,
    val status: String = "", // "pending", "completed", "cancelled"
    val paymentMethod: String = "",
    val paymentId: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val shippingAddress: String = "",
    val contactPhone: String = ""
) : Serializable