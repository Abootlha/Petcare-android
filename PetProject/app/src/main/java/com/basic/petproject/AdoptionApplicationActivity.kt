package com.basic.petproject

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.basic.petproject.models.AdoptionApplication
import com.basic.petproject.models.Pet
import com.basic.petproject.repositories.AdoptionRepository
import com.google.android.material.slider.Slider
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.launch

class AdoptionApplicationActivity : AppCompatActivity() {
    private lateinit var pet: Pet
    private lateinit var petNameText: TextView
    private lateinit var homeTypeDropdown: AutoCompleteTextView
    private lateinit var hasYardCheckbox: CheckBox
    private lateinit var yardFencedCheckbox: CheckBox
    private lateinit var hasChildrenCheckbox: CheckBox
    private lateinit var childrenAgesLayout: TextInputLayout
    private lateinit var childrenAgesInput: TextInputEditText
    private lateinit var hasOtherPetsCheckbox: CheckBox
    private lateinit var otherPetsLayout: TextInputLayout
    private lateinit var otherPetsInput: TextInputEditText
    private lateinit var petExperienceInput: TextInputEditText
    private lateinit var hoursAloneSlider: Slider
    private lateinit var hoursAloneText: TextView
    private lateinit var exercisePlanInput: TextInputEditText
    private lateinit var trainingPlanInput: TextInputEditText
    private lateinit var reasonForAdoptionInput: TextInputEditText
    private lateinit var vetReferenceInput: TextInputEditText
    private lateinit var personalReferenceInput: TextInputEditText
    private lateinit var agreementCheckbox: CheckBox
    private lateinit var submitButton: Button
    
    private val adoptionRepository = AdoptionRepository()
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_adoption_application)
        
        // Get pet from intent
        pet = intent.getSerializableExtra("pet") as Pet
        
        // Initialize views
        initializeViews()
        
        // Setup UI with pet data
        setupUI()
        
        // Setup listeners
        setupListeners()
    }
    
    private fun initializeViews() {
        petNameText = findViewById(R.id.petNameText)
        homeTypeDropdown = findViewById(R.id.homeTypeDropdown)
        hasYardCheckbox = findViewById(R.id.hasYardCheckbox)
        yardFencedCheckbox = findViewById(R.id.yardFencedCheckbox)
        hasChildrenCheckbox = findViewById(R.id.hasChildrenCheckbox)
        childrenAgesLayout = findViewById(R.id.childrenAgesLayout)
        childrenAgesInput = findViewById(R.id.childrenAgesInput)
        hasOtherPetsCheckbox = findViewById(R.id.hasOtherPetsCheckbox)
        otherPetsLayout = findViewById(R.id.otherPetsLayout)
        otherPetsInput = findViewById(R.id.otherPetsInput)
        petExperienceInput = findViewById(R.id.petExperienceInput)
        hoursAloneSlider = findViewById(R.id.hoursAloneSlider)
        hoursAloneText = findViewById(R.id.hoursAloneText)
        exercisePlanInput = findViewById(R.id.exercisePlanInput)
        trainingPlanInput = findViewById(R.id.trainingPlanInput)
        reasonForAdoptionInput = findViewById(R.id.reasonForAdoptionInput)
        vetReferenceInput = findViewById(R.id.vetReferenceInput)
        personalReferenceInput = findViewById(R.id.personalReferenceInput)
        agreementCheckbox = findViewById(R.id.agreementCheckbox)
        submitButton = findViewById(R.id.submitButton)
    }
    
    private fun setupUI() {
        // Set pet name
        petNameText.text = "Application for: ${pet.name}"
        
        // Setup home type dropdown
        val homeTypes = arrayOf("Apartment", "House", "Condo", "Townhouse", "Farm", "Other")
        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, homeTypes)
        homeTypeDropdown.setAdapter(adapter)
        
        // Initially disable yard fenced checkbox
        yardFencedCheckbox.isEnabled = false
    }
    
    private fun setupListeners() {
        // Yard checkbox
        hasYardCheckbox.setOnCheckedChangeListener { _, isChecked ->
            yardFencedCheckbox.isEnabled = isChecked
            if (!isChecked) {
                yardFencedCheckbox.isChecked = false
            }
        }
        
        // Children checkbox
        hasChildrenCheckbox.setOnCheckedChangeListener { _, isChecked ->
            childrenAgesLayout.visibility = if (isChecked) View.VISIBLE else View.GONE
        }
        
        // Other pets checkbox
        hasOtherPetsCheckbox.setOnCheckedChangeListener { _, isChecked ->
            otherPetsLayout.visibility = if (isChecked) View.VISIBLE else View.GONE
        }
        
        // Hours alone slider
        hoursAloneSlider.addOnChangeListener { _, value, _ ->
            hoursAloneText.text = "Hours pet will be alone: ${value.toInt()}"
        }
        
        // Agreement checkbox
        agreementCheckbox.setOnCheckedChangeListener { _, isChecked ->
            validateForm()
        }
        
        // Setup text change listeners for required fields
        val requiredFields = listOf(
            petExperienceInput,
            exercisePlanInput,
            reasonForAdoptionInput
        )
        
        val textWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                validateForm()
            }
        }
        
        requiredFields.forEach { it.addTextChangedListener(textWatcher) }
        
        // Submit button
        submitButton.setOnClickListener {
            submitApplication()
        }
    }
    
    private fun validateForm(): Boolean {
        // Check if required fields are filled
        val requiredFieldsFilled = petExperienceInput.text?.isNotEmpty() == true &&
                exercisePlanInput.text?.isNotEmpty() == true &&
                reasonForAdoptionInput.text?.isNotEmpty() == true
        
        // Check if user agreed to terms
        val userAgreed = agreementCheckbox.isChecked
        
        // If children checkbox is checked, children ages must be provided
        val childrenInfoValid = !hasChildrenCheckbox.isChecked || 
                (hasChildrenCheckbox.isChecked && childrenAgesInput.text?.isNotEmpty() == true)
        
        // If other pets checkbox is checked, other pets description must be provided
        val otherPetsInfoValid = !hasOtherPetsCheckbox.isChecked || 
                (hasOtherPetsCheckbox.isChecked && otherPetsInput.text?.isNotEmpty() == true)
        
        val formValid = requiredFieldsFilled && userAgreed && childrenInfoValid && otherPetsInfoValid
        
        // Enable/disable submit button
        submitButton.isEnabled = formValid
        
        return formValid
    }
    
    private fun submitApplication() {
        if (!validateForm()) {
            Toast.makeText(this, "Please fill all required fields", Toast.LENGTH_SHORT).show()
            return
        }
        
        // Create application object
        val application = AdoptionApplication(
            petId = pet.id ?: "",
            petName = pet.name,
            homeType = homeTypeDropdown.text.toString(),
            hasYard = hasYardCheckbox.isChecked,
            yardFenced = yardFencedCheckbox.isChecked,
            hasChildren = hasChildrenCheckbox.isChecked,
            childrenAges = if (hasChildrenCheckbox.isChecked) childrenAgesInput.text.toString() else "",
            hasOtherPets = hasOtherPetsCheckbox.isChecked,
            otherPetsDescription = if (hasOtherPetsCheckbox.isChecked) otherPetsInput.text.toString() else "",
            petExperience = petExperienceInput.text.toString(),
            hoursAlone = hoursAloneSlider.value.toInt(),
            exercisePlan = exercisePlanInput.text.toString(),
            trainingPlan = trainingPlanInput.text.toString(),
            reasonForAdoption = reasonForAdoptionInput.text.toString(),
            veterinarianReference = vetReferenceInput.text.toString(),
            personalReference = personalReferenceInput.text.toString()
        )
        
        // Submit application
        submitButton.isEnabled = false
        submitButton.text = "Submitting..."
        
        lifecycleScope.launch {
            try {
                val result = adoptionRepository.submitApplication(application)
                
                if (result.isSuccess) {
                    showApplicationSubmittedDialog()
                } else {
                    val error = result.exceptionOrNull()
                    Toast.makeText(this@AdoptionApplicationActivity, 
                        "Error: ${error?.message ?: "Unknown error"}",
                        Toast.LENGTH_LONG).show()
                    submitButton.isEnabled = true
                    submitButton.text = "Submit Application"
                }
            } catch (e: Exception) {
                Toast.makeText(this@AdoptionApplicationActivity, 
                    "Error: ${e.message ?: "Unknown error"}",
                    Toast.LENGTH_LONG).show()
                submitButton.isEnabled = true
                submitButton.text = "Submit Application"
            }
        }
    }
    
    private fun showApplicationSubmittedDialog() {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Application Submitted")
            .setMessage("Your adoption application for ${pet.name} has been submitted successfully! The shelter will review your application and contact you soon.")
            .setPositiveButton("View My Applications") { _, _ ->
                // Navigate to my applications screen
                val intent = Intent(this, MyApplicationsActivity::class.java)
                startActivity(intent)
                finish()
            }
            .setNegativeButton("Return to Pet") { _, _ ->
                finish()
            }
            .setCancelable(false)
            .show()
    }
} 