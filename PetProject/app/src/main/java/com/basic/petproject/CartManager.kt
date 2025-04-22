package com.basic.petproject

import com.basic.petproject.models.Pet

/**
 * Singleton class to manage the shopping cart for pets
 */
object CartManager {
    private val cartItems = mutableListOf<Pet>()
    
    /**
     * Add a pet to the cart
     * @param pet The pet to add to the cart
     */
    fun addToCart(pet: Pet) {
        // Check if pet is already in cart
        if (!cartItems.contains(pet)) {
            cartItems.add(pet)
        }
    }
    
    /**
     * Remove a pet from the cart
     * @param pet The pet to remove from the cart
     */
    fun removeFromCart(pet: Pet) {
        cartItems.remove(pet)
    }
    
    /**
     * Get all pets in the cart
     * @return List of pets in the cart
     */
    fun getCartItems(): List<Pet> {
        return cartItems.toList()
    }
    
    /**
     * Clear all items from the cart
     */
    fun clearCart() {
        cartItems.clear()
    }
    
    /**
     * Get the total price of all pets in the cart
     * @return Total price
     */
    fun getCartTotal(): Double {
        return cartItems.sumOf { it.price }
    }
    
    /**
     * Check if the cart is empty
     * @return True if cart is empty, false otherwise
     */
    fun isCartEmpty(): Boolean {
        return cartItems.isEmpty()
    }
    
    /**
     * Get the number of items in the cart
     * @return Number of items
     */
    fun getItemCount(): Int {
        return cartItems.size
    }
}