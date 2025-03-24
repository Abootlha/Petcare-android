package com.example.petproject.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.petproject.R
import com.example.petproject.models.Pet
import com.squareup.picasso.Picasso

class PetAdapter(private var pets: List<Pet>, private val onPetClick: (Pet) -> Unit) : 
    RecyclerView.Adapter<PetAdapter.PetViewHolder>() {
    
    class PetViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val petImage: ImageView = itemView.findViewById(R.id.petImage)
        val petName: TextView = itemView.findViewById(R.id.petName)
        val petBreed: TextView = itemView.findViewById(R.id.petBreed)
        val petPrice: TextView = itemView.findViewById(R.id.petPrice)
    }
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PetViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_pet, parent, false)
        return PetViewHolder(view)
    }
    
    override fun onBindViewHolder(holder: PetViewHolder, position: Int) {
        val pet = pets[position]
        
        holder.petName.text = pet.name
        holder.petBreed.text = "${pet.type} - ${pet.breed}"
        holder.petPrice.text = "$${pet.price}"
        
        // Load image with Picasso
        if (pet.imageUrl.isNotEmpty()) {
            Picasso.get()
                .load(pet.imageUrl)
                .placeholder(R.drawable.pet_placeholder)
                .error(R.drawable.pet_placeholder)
                .into(holder.petImage)
        } else {
            holder.petImage.setImageResource(R.drawable.pet_placeholder)
        }
        
        // Set click listener
        holder.itemView.setOnClickListener {
            onPetClick(pet)
        }
    }
    
    override fun getItemCount(): Int = pets.size
    
    fun updatePets(newPets: List<Pet>) {
        this.pets = newPets
        notifyDataSetChanged()
    }
}