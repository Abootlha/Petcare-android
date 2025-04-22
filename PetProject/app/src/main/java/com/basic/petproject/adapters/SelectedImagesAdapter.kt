package com.basic.petproject.adapters

import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.basic.petproject.R

/**
 * Adapter for displaying selected images in the AddPetActivity
 */
class SelectedImagesAdapter(
    private val imageUris: List<Uri>,
    private val onDeleteClicked: (Int) -> Unit
) : RecyclerView.Adapter<SelectedImagesAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val imageView: ImageView = view.findViewById(R.id.imageViewSelectedImage)
        val deleteButton: ImageView = view.findViewById(R.id.imageViewDeleteImage)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_selected_image, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val uri = imageUris[position]
        
        // Load image with Glide
        Glide.with(holder.imageView.context)
            .load(uri)
            .placeholder(R.drawable.ic_image_placeholder)
            .error(R.drawable.ic_image_error)
            .centerCrop()
            .into(holder.imageView)
            
        // Set delete button click listener
        holder.deleteButton.setOnClickListener {
            onDeleteClicked(position)
        }
    }

    override fun getItemCount() = imageUris.size
} 