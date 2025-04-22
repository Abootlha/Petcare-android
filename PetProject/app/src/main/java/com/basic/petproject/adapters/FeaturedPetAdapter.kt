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

class FeaturedPetAdapter(
    private var pets: List<Pet>,
    private val onPetClicked: (Pet) -> Unit
) : RecyclerView.Adapter<FeaturedPetAdapter.FeaturedPetViewHolder>() {

    private val TAG = "FeaturedPetAdapter"

    class FeaturedPetViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val petImage: ImageView = itemView.findViewById(R.id.petImage)
        val petName: TextView = itemView.findViewById(R.id.petName)
        val petDetails: TextView = itemView.findViewById(R.id.petDetails)
        val favoriteButton: FloatingActionButton = itemView.findViewById(R.id.favoriteButton)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FeaturedPetViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_pet_featured, parent, false)
        return FeaturedPetViewHolder(view)
    }

    override fun onBindViewHolder(holder: FeaturedPetViewHolder, position: Int) {
        val pet = pets[position]
        
        // Set pet name
        holder.petName.text = pet.name
        
        // Set pet details (age and price)
        val age = pet.age ?: 0
        val ageText = if (age == 1) "1 year" else "$age years"
        
        // Format price properly
        val priceText = if (pet.price > 0) {
            NumberFormat.getCurrencyInstance(Locale.US).format(pet.price)
        } else {
            "Free"
        }
        
        holder.petDetails.text = "$ageText • $priceText"
        
        // Load pet image with improved handling of different image fields
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
        
        // Set up favorite button
        holder.favoriteButton.setImageResource(
            if (pet.isFavorite) R.drawable.ic_favorite else R.drawable.ic_favorite_border
        )
        
        holder.favoriteButton.setOnClickListener {
            // Toggle favorite status
            pet.isFavorite = !pet.isFavorite
            holder.favoriteButton.setImageResource(
                if (pet.isFavorite) R.drawable.ic_favorite else R.drawable.ic_favorite_border
            )
            
            // TODO: Update pet favorite status in repository
        }
        
        // Set click listener on the whole item
        holder.itemView.setOnClickListener {
            onPetClicked(pet)
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
        Log.d(TAG, "Updating with ${newPets.size} featured pets")
        newPets.forEachIndexed { index, pet ->
            Log.d(TAG, "Featured Pet #$index: ${pet.id} - ${pet.name}, imageUrls=${pet.imageUrls?.size ?: 0}")
        }
        this.pets = newPets
        notifyDataSetChanged()
    }
} 