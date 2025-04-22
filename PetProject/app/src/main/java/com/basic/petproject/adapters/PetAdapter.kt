package com.basic.petproject.adapters

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.basic.petproject.R
import com.basic.petproject.models.Pet
import com.google.android.material.floatingactionbutton.FloatingActionButton
import java.text.NumberFormat
import java.util.*

class PetAdapter(private var pets: List<Pet>, private val onPetClick: (Pet) -> Unit) : 
    RecyclerView.Adapter<PetAdapter.PetViewHolder>() {
    
    private val TAG = "PetAdapter"
    
    class PetViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val petImage: ImageView = itemView.findViewById(R.id.petImage)
        val petName: TextView = itemView.findViewById(R.id.petName)
        val petBreed: TextView = itemView.findViewById(R.id.petBreed)
        val petPrice: TextView = itemView.findViewById(R.id.petPrice)
        val petLocation: TextView = itemView.findViewById(R.id.petLocation)
        val ageBadge: TextView = itemView.findViewById(R.id.ageBadge)
        val favoriteButton: FloatingActionButton = itemView.findViewById(R.id.favoriteButton)
        val ownerBadge: TextView = itemView.findViewById(R.id.ownerBadge)
    }
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PetViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_pet, parent, false)
        return PetViewHolder(view)
    }
    
    override fun onBindViewHolder(holder: PetViewHolder, position: Int) {
        val pet = pets[position]
        
        // Set pet name
        holder.petName.text = pet.name
        
        // Set pet breed
        holder.petBreed.text = pet.breed ?: "Unknown breed"
        
        // Set pet price with proper currency formatting
        val price = pet.price
        if (price > 0) {
            val formattedPrice = NumberFormat.getCurrencyInstance(Locale.US).format(price)
            holder.petPrice.text = formattedPrice
        } else {
            holder.petPrice.text = "Free"
        }
        
        // Set pet location
        holder.petLocation.text = pet.location?.takeIf { it.isNotEmpty() } ?: "Unknown location"
        
        // Set age badge
        val age = pet.age ?: 0
        val ageText = if (age == 1) "1 year" else "$age years"
        holder.ageBadge.text = ageText
        
        // Show owner badge if it's the user's pet
        if (pet.isFavorite) {
            holder.ownerBadge.visibility = View.VISIBLE
            holder.ownerBadge.text = "My Pet"
        } else {
            holder.ownerBadge.visibility = View.GONE
        }
        
        // Load pet image - with clean URLs to remove access tokens
        if (!pet.imageUrls.isNullOrEmpty()) {
            val cleanUrl = removeAccessToken(pet.imageUrls[0])
            Log.d(TAG, "Loading image from imageUrls[0] for pet ${pet.name}: $cleanUrl")
            Glide.with(holder.petImage.context)
                .load(cleanUrl)
                .placeholder(R.drawable.placeholder_pet)
                .error(R.drawable.placeholder_pet)
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .centerCrop()
                .into(holder.petImage)
        } else if (!pet.images.isNullOrEmpty()) {
            val cleanUrl = removeAccessToken(pet.images[0])
            Log.d(TAG, "Loading image from images[0] for pet ${pet.name}: $cleanUrl")
            Glide.with(holder.petImage.context)
                .load(cleanUrl)
                .placeholder(R.drawable.placeholder_pet)
                .error(R.drawable.placeholder_pet)
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .centerCrop()
                .into(holder.petImage)
        } else if (pet.imageUrl != null) {
            val cleanUrl = removeAccessToken(pet.imageUrl)
            Log.d(TAG, "Loading image from imageUrl for pet ${pet.name}: $cleanUrl")
            Glide.with(holder.petImage.context)
                .load(cleanUrl)
                .placeholder(R.drawable.placeholder_pet)
                .error(R.drawable.placeholder_pet)
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .centerCrop()
                .into(holder.petImage)
        } else if (pet.featuredImage != null) {
            val cleanUrl = removeAccessToken(pet.featuredImage)
            Log.d(TAG, "Loading image from featuredImage for pet ${pet.name}: $cleanUrl") 
            Glide.with(holder.petImage.context)
                .load(cleanUrl)
                .placeholder(R.drawable.placeholder_pet)
                .error(R.drawable.placeholder_pet)
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .centerCrop()
                .into(holder.petImage)
        } else {
            Log.d(TAG, "No image found for pet ${pet.name}, using placeholder")
            holder.petImage.setImageResource(R.drawable.placeholder_pet)
        }
        
        // Set favorite button state
        holder.favoriteButton.setImageResource(
            if (pet.isFavorite) R.drawable.ic_favorite_filled
            else R.drawable.ic_favorite_outline
        )
        
        // Set click listener for the entire item
        holder.itemView.setOnClickListener {
            onPetClick(pet)
        }
    }
    
    /**
     * Removes Firebase Storage access tokens from URLs
     * This ensures image URLs work for all users regardless of who uploaded them
     */
    private fun removeAccessToken(url: String?): String? {
        if (url.isNullOrEmpty()) return url
        
        return try {
            if (url.contains("firebasestorage.googleapis.com")) {
                // For Firebase Storage URLs, create a direct download URL format
                // Format: https://firebasestorage.googleapis.com/v0/b/BUCKET_NAME/o/OBJECT_PATH?alt=media
                if (url.contains("/v0/b/")) {
                    // Already in the correct format, just make sure it has alt=media
                    if (!url.contains("alt=media")) {
                        if (url.contains("?")) {
                            // Has other parameters, add alt=media
                            url + "&alt=media"
                        } else {
                            // No parameters yet, add alt=media
                            url + "?alt=media"
                        }
                    } else {
                        url // Already has alt=media
                    }
                } else if (url.contains("?")) {
                    // Has a token, extract the path and create a direct URL
                    val path = url.substringBefore("?")
                    path + "?alt=media"
                } else {
                    // No token, just add alt=media
                    url + "?alt=media"
                }
            } else {
                url // Not a Firebase URL, leave as is
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error cleaning URL: $url", e)
            url
        }
    }
    
    override fun getItemCount(): Int = pets.size
    
    fun updatePets(newPets: List<Pet>) {
        // Log pet info for debugging
        Log.d(TAG, "Updating with ${newPets.size} pets")
        
        // Filter out duplicate pets by ID
        val uniquePets = newPets.distinctBy { it.id }
        
        if (uniquePets.size < newPets.size) {
            Log.w(TAG, "Found duplicate pets! Original: ${newPets.size}, Unique: ${uniquePets.size}")
            // Log the duplicate pet IDs for debugging
            val petIds = newPets.map { it.id }
            val duplicates = petIds.groupBy { it }.filter { it.value.size > 1 }.keys
            Log.w(TAG, "Duplicate pet IDs: $duplicates")
        }
        
        // Log pet info for debugging
        uniquePets.forEachIndexed { index, pet ->
            Log.d(TAG, "Unique Pet #$index: ${pet.id} - ${pet.name} (${pet.type}), imageUrls=${pet.imageUrls?.size ?: 0}")
        }
        
        this.pets = uniquePets
        notifyDataSetChanged()
    }
}