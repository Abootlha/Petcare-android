package com.basic.petproject.admin

import android.app.Dialog
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.basic.petproject.AddPetActivity
import com.basic.petproject.R
import com.basic.petproject.models.AdoptionApplication
import com.basic.petproject.repositories.AdoptionRepository
import com.basic.petproject.repositories.PetRepository
import com.google.android.material.chip.Chip
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.tabs.TabLayout
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AdminDashboardActivity : AppCompatActivity() {
    private lateinit var tabLayout: TabLayout
    private lateinit var applicationsRecyclerView: RecyclerView
    private lateinit var progressBar: View
    private lateinit var emptyView: TextView
    private lateinit var fabAddPet: FloatingActionButton
    
    private val adoptionRepository = AdoptionRepository()
    private val petRepository = PetRepository()
    private val applicationAdapter = ApplicationAdapter()
    
    private var currentFilter = "Pending" // Default filter
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_dashboard)
        
        // Initialize views
        initializeViews()
        
        // Setup RecyclerView
        setupRecyclerView()
        
        // Setup tab layout
        setupTabLayout()
        
        // Load initial data
        loadApplications()
        
        // FAB click listener
        fabAddPet.setOnClickListener {
            val intent = Intent(this, AddPetActivity::class.java)
            startActivity(intent)
        }
    }
    
    private fun initializeViews() {
        tabLayout = findViewById(R.id.tabLayout)
        applicationsRecyclerView = findViewById(R.id.applicationsRecyclerView)
        progressBar = findViewById(R.id.progressBar)
        emptyView = findViewById(R.id.emptyView)
        fabAddPet = findViewById(R.id.fabAddPet)
    }
    
    private fun setupRecyclerView() {
        applicationsRecyclerView.layoutManager = LinearLayoutManager(this)
        applicationsRecyclerView.adapter = applicationAdapter
        
        applicationAdapter.onViewDetailsClickListener = { application ->
            showApplicationDetails(application)
        }
        
        applicationAdapter.onApproveClickListener = { application ->
            showApprovalConfirmation(application)
        }
        
        applicationAdapter.onRejectClickListener = { application ->
            showRejectionDialog(application)
        }
    }
    
    private fun setupTabLayout() {
        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                when (tab?.position) {
                    0 -> currentFilter = "Pending"
                    1 -> currentFilter = "Approved"
                    2 -> currentFilter = "Rejected"
                }
                loadApplications()
            }
            
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })
    }
    
    private fun loadApplications() {
        showLoading(true)
        
        lifecycleScope.launch {
            try {
                // Get all applications
                val result = adoptionRepository.getAllApplications()
                
                if (result.isSuccess) {
                    val applications = result.getOrThrow()
                    
                    // Filter applications based on the selected tab
                    val filteredApplications = applications.filter { 
                        it.status.equals(currentFilter, ignoreCase = true) 
                    }
                    
                    withContext(Dispatchers.Main) {
                        applicationAdapter.submitList(filteredApplications)
                        showLoading(false)
                        
                        // Show empty view if no applications
                        if (filteredApplications.isEmpty()) {
                            emptyView.visibility = View.VISIBLE
                            emptyView.text = "No $currentFilter applications found"
                        } else {
                            emptyView.visibility = View.GONE
                        }
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        showError("Failed to load applications: ${result.exceptionOrNull()?.message}")
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    showError("Error: ${e.message}")
                }
            }
        }
    }
    
    private fun showLoading(isLoading: Boolean) {
        runOnUiThread {
            progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
            applicationsRecyclerView.visibility = if (isLoading) View.GONE else View.VISIBLE
            emptyView.visibility = View.GONE
        }
    }
    
    private fun showError(message: String) {
        runOnUiThread {
            showLoading(false)
            Toast.makeText(this, message, Toast.LENGTH_LONG).show()
        }
    }
    
    private fun showApplicationDetails(application: AdoptionApplication) {
        val dialog = Dialog(this, R.style.Theme_MaterialComponents_Dialog_FullWidth)
        dialog.setContentView(R.layout.dialog_application_details)
        
        // Set data to dialog views
        dialog.findViewById<TextView>(R.id.petNameText).text = "Application for: ${application.petName}"
        
        val statusChip = dialog.findViewById<Chip>(R.id.statusChip)
        statusChip.text = application.status
        when (application.status.lowercase()) {
            "pending" -> statusChip.setChipBackgroundColorResource(android.R.color.holo_blue_light)
            "approved" -> statusChip.setChipBackgroundColorResource(android.R.color.holo_green_light)
            "rejected" -> statusChip.setChipBackgroundColorResource(android.R.color.holo_red_light)
        }
        
        // Applicant Information
        dialog.findViewById<TextView>(R.id.applicantNameText).text = "Name: ${application.applicantName}"
        dialog.findViewById<TextView>(R.id.applicantEmailText).text = "Email: ${application.applicantEmail}"
        dialog.findViewById<TextView>(R.id.applicantPhoneText).text = "Phone: ${application.applicantPhone.ifEmpty { "Not provided" }}"
        
        // Living Situation
        dialog.findViewById<TextView>(R.id.homeTypeText).text = "Home Type: ${application.homeType}"
        dialog.findViewById<TextView>(R.id.yardInfoText).text = "Has yard: ${if (application.hasYard) "Yes" else "No"}, Fenced: ${if (application.yardFenced) "Yes" else "No"}"
        dialog.findViewById<TextView>(R.id.childrenInfoText).text = if (application.hasChildren) "Children: Yes, Ages: ${application.childrenAges}" else "Children: No"
        dialog.findViewById<TextView>(R.id.otherPetsText).text = if (application.hasOtherPets) "Other Pets: Yes, ${application.otherPetsDescription}" else "Other Pets: No"
        
        // Experience and Plans
        dialog.findViewById<TextView>(R.id.petExperienceText).text = "Previous Experience: ${application.petExperience}"
        dialog.findViewById<TextView>(R.id.hoursAloneText).text = "Hours Alone: ${application.hoursAlone}"
        dialog.findViewById<TextView>(R.id.exercisePlanText).text = "Exercise Plan: ${application.exercisePlan}"
        dialog.findViewById<TextView>(R.id.trainingPlanText).text = "Training Plan: ${application.trainingPlan}"
        dialog.findViewById<TextView>(R.id.adoptionReasonText).text = "Reason for Adoption: ${application.reasonForAdoption}"
        
        // References
        dialog.findViewById<TextView>(R.id.vetReferenceText).text = "Vet Reference: ${application.veterinarianReference.ifEmpty { "Not provided" }}"
        dialog.findViewById<TextView>(R.id.personalReferenceText).text = "Personal Reference: ${application.personalReference.ifEmpty { "Not provided" }}"
        
        // Admin Notes
        val adminNotesInput = dialog.findViewById<TextInputEditText>(R.id.adminNotesInput)
        adminNotesInput.setText(application.adminNotes)
        
        // Buttons
        val closeButton = dialog.findViewById<Button>(R.id.closeButton)
        val approveButton = dialog.findViewById<Button>(R.id.approveButton)
        val rejectButton = dialog.findViewById<Button>(R.id.rejectButton)
        
        // Hide approve/reject buttons for already processed applications
        if (application.status.equals("Approved", ignoreCase = true) || 
            application.status.equals("Rejected", ignoreCase = true)) {
            approveButton.visibility = View.GONE
            rejectButton.visibility = View.GONE
        }
        
        closeButton.setOnClickListener {
            dialog.dismiss()
        }
        
        approveButton.setOnClickListener {
            val notes = adminNotesInput.text.toString()
            updateApplicationStatus(application, "Approved", notes)
            dialog.dismiss()
        }
        
        rejectButton.setOnClickListener {
            val notes = adminNotesInput.text.toString()
            if (notes.isEmpty()) {
                Toast.makeText(this, "Please provide a reason for rejection", Toast.LENGTH_SHORT).show()
            } else {
                updateApplicationStatus(application, "Rejected", notes)
                dialog.dismiss()
            }
        }
        
        dialog.show()
    }
    
    private fun showApprovalConfirmation(application: AdoptionApplication) {
        AlertDialog.Builder(this)
            .setTitle("Approve Application")
            .setMessage("Are you sure you want to approve this application for ${application.petName}? This will mark the pet as adopted.")
            .setPositiveButton("Approve") { _, _ ->
                updateApplicationStatus(application, "Approved")
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    private fun showRejectionDialog(application: AdoptionApplication) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_rejection_reason, null)
        val reasonInput = dialogView.findViewById<TextInputEditText>(R.id.rejectionReasonInput)
        
        AlertDialog.Builder(this)
            .setTitle("Reject Application")
            .setView(dialogView)
            .setPositiveButton("Reject") { _, _ ->
                val reason = reasonInput.text.toString()
                if (reason.isEmpty()) {
                    Toast.makeText(this, "Please provide a reason for rejection", Toast.LENGTH_SHORT).show()
                } else {
                    updateApplicationStatus(application, "Rejected", reason)
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    private fun updateApplicationStatus(application: AdoptionApplication, newStatus: String, notes: String = "") {
        lifecycleScope.launch {
            try {
                val result = adoptionRepository.updateApplicationStatus(application.id, newStatus, notes)
                
                if (result.isSuccess) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(
                            this@AdminDashboardActivity,
                            "Application ${newStatus.lowercase()}",
                            Toast.LENGTH_SHORT
                        ).show()
                        
                        // Reload applications
                        loadApplications()
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        showError("Failed to update status: ${result.exceptionOrNull()?.message}")
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    showError("Error: ${e.message}")
                }
            }
        }
    }
    
    override fun onResume() {
        super.onResume()
        loadApplications()
    }
    
    // Application adapter
    private inner class ApplicationAdapter : RecyclerView.Adapter<ApplicationAdapter.ViewHolder>() {
        private var applications = listOf<AdoptionApplication>()
        
        var onViewDetailsClickListener: ((AdoptionApplication) -> Unit)? = null
        var onApproveClickListener: ((AdoptionApplication) -> Unit)? = null
        var onRejectClickListener: ((AdoptionApplication) -> Unit)? = null
        
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_application, parent, false)
            return ViewHolder(view)
        }
        
        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val application = applications[position]
            holder.bind(application)
        }
        
        override fun getItemCount() = applications.size
        
        fun submitList(newList: List<AdoptionApplication>) {
            applications = newList
            notifyDataSetChanged()
        }
        
        inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            private val petNameText: TextView = itemView.findViewById(R.id.petNameText)
            private val applicantNameText: TextView = itemView.findViewById(R.id.applicantNameText)
            private val applicationDateText: TextView = itemView.findViewById(R.id.applicationDateText)
            private val applicationStatusChip: Chip = itemView.findViewById(R.id.applicationStatusChip)
            private val viewDetailsButton: Button = itemView.findViewById(R.id.viewDetailsButton)
            private val approveButton: Button = itemView.findViewById(R.id.approveButton)
            private val rejectButton: Button = itemView.findViewById(R.id.rejectButton)
            
            fun bind(application: AdoptionApplication) {
                petNameText.text = application.petName
                applicantNameText.text = "Applicant: ${application.applicantName}"
                
                // Format date
                val date = java.text.SimpleDateFormat("MMM dd, yyyy", java.util.Locale.getDefault())
                    .format(java.util.Date(application.applicationDate))
                applicationDateText.text = "Submitted on: $date"
                
                // Set status chip
                applicationStatusChip.text = application.status
                when (application.status.lowercase()) {
                    "pending" -> applicationStatusChip.setChipBackgroundColorResource(android.R.color.holo_blue_light)
                    "approved" -> applicationStatusChip.setChipBackgroundColorResource(android.R.color.holo_green_light)
                    "rejected" -> applicationStatusChip.setChipBackgroundColorResource(android.R.color.holo_red_light)
                }
                
                // Hide approve/reject buttons for already processed applications
                if (application.status.equals("Approved", ignoreCase = true) || 
                    application.status.equals("Rejected", ignoreCase = true)) {
                    approveButton.visibility = View.GONE
                    rejectButton.visibility = View.GONE
                } else {
                    approveButton.visibility = View.VISIBLE
                    rejectButton.visibility = View.VISIBLE
                }
                
                // Set click listeners
                viewDetailsButton.setOnClickListener {
                    onViewDetailsClickListener?.invoke(application)
                }
                
                approveButton.setOnClickListener {
                    onApproveClickListener?.invoke(application)
                }
                
                rejectButton.setOnClickListener {
                    onRejectClickListener?.invoke(application)
                }
            }
        }
    }
} 