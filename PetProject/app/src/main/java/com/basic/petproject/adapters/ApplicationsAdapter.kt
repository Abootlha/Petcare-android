package com.basic.petproject.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.basic.petproject.R
import com.basic.petproject.models.AdoptionApplication
import com.basic.petproject.models.Pet
import com.google.android.material.chip.Chip
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

class ApplicationsAdapter(
    private val context: Context,
    private var applications: List<AdoptionApplication>,
    private val petMap: Map<String, Pet>,
    private val onItemClick: (AdoptionApplication) -> Unit
) : RecyclerView.Adapter<ApplicationsAdapter.ApplicationViewHolder>() {

    class ApplicationViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val petNameText: TextView = view.findViewById(R.id.petNameText)
        val applicantNameText: TextView = view.findViewById(R.id.applicantNameText)
        val applicationDateText: TextView = view.findViewById(R.id.applicationDateText)
        val applicationStatusChip: Chip = view.findViewById(R.id.applicationStatusChip)
        val viewDetailsButton: Button = view.findViewById(R.id.viewDetailsButton)
        val approveButton: Button = view.findViewById(R.id.approveButton)
        val rejectButton: Button = view.findViewById(R.id.rejectButton)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ApplicationViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_application, parent, false)
        return ApplicationViewHolder(view)
    }

    override fun onBindViewHolder(holder: ApplicationViewHolder, position: Int) {
        val application = applications[position]
        val pet = petMap[application.petId]
        
        // Set pet name
        holder.petNameText.text = pet?.name ?: application.petName
        
        // Set applicant name
        holder.applicantNameText.text = application.applicantName
        
        // Set application date
        val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
        val applicationDate = Date(application.applicationDate)
        holder.applicationDateText.text = "Submitted on: ${dateFormat.format(applicationDate)}"
        
        // Set status with appropriate color
        holder.applicationStatusChip.text = application.status
        when (application.status.lowercase()) {
            "pending" -> {
                holder.applicationStatusChip.setChipBackgroundColorResource(android.R.color.holo_orange_light)
            }
            "under review" -> {
                holder.applicationStatusChip.setChipBackgroundColorResource(android.R.color.holo_blue_light)
            }
            "approved" -> {
                holder.applicationStatusChip.setChipBackgroundColorResource(android.R.color.holo_green_dark)
            }
            "rejected" -> {
                holder.applicationStatusChip.setChipBackgroundColorResource(android.R.color.holo_red_light)
            }
        }
        
        // Set button visibility based on status and user role
        // For simplicity, always show all buttons for now
        holder.viewDetailsButton.setOnClickListener { onItemClick(application) }
        
        // Set approve button listener (if needed)
        holder.approveButton.setOnClickListener {
            // Call a method to approve the application
            // onApproveClick(application)
            onItemClick(application)
        }
        
        // Set reject button listener (if needed)
        holder.rejectButton.setOnClickListener {
            // Call a method to reject the application
            // onRejectClick(application)
            onItemClick(application)
        }
    }

    override fun getItemCount() = applications.size
    
    fun updateApplications(newApplications: List<AdoptionApplication>) {
        applications = newApplications
        notifyDataSetChanged()
    }
    
    private fun getTimeAgo(timeInMillis: Long): String {
        val now = System.currentTimeMillis()
        val diff = now - timeInMillis
        
        return when {
            diff < TimeUnit.MINUTES.toMillis(1) -> "just now"
            diff < TimeUnit.HOURS.toMillis(1) -> "${TimeUnit.MILLISECONDS.toMinutes(diff)} minutes ago"
            diff < TimeUnit.DAYS.toMillis(1) -> "${TimeUnit.MILLISECONDS.toHours(diff)} hours ago"
            diff < TimeUnit.DAYS.toMillis(7) -> "${TimeUnit.MILLISECONDS.toDays(diff)} days ago"
            else -> {
                val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
                dateFormat.format(Date(timeInMillis))
            }
        }
    }
} 