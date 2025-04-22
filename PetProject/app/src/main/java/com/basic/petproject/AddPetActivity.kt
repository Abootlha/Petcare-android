package com.basic.petproject

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.*
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.basic.petproject.adapters.SelectedImagesAdapter
import com.basic.petproject.models.Pet
import com.basic.petproject.repositories.PetRepository
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.UUID

class AddPetActivity : AppCompatActivity() {
    // Toolbar
    private lateinit var toolbar: Toolbar
    
    // Basic Information
    private lateinit var nameEditText: TextInputEditText
    private lateinit var typeDropdown: AutoCompleteTextView
    private lateinit var breedEditText: TextInputEditText
    private lateinit var ageEditText: TextInputEditText
    private lateinit var genderDropdown: AutoCompleteTextView
    private lateinit var descriptionEditText: TextInputEditText
    
    // Image Selection
    private lateinit var addImageButton: FloatingActionButton
    private lateinit var selectedImagesRecyclerView: RecyclerView
    private lateinit var imageProgressBar: ProgressBar
    
    // Physical Characteristics
    private lateinit var sizeDropdown: AutoCompleteTextView
    private lateinit var colorEditText: TextInputEditText
    private lateinit var weightEditText: TextInputEditText
    private lateinit var sheddingDropdown: AutoCompleteTextView
    
    // Health Information
    private lateinit var vaccinatedCheckbox: CheckBox
    private lateinit var spayedNeuteredCheckbox: CheckBox
    private lateinit var medicalHistoryEditText: TextInputEditText
    private lateinit var specialNeedsEditText: TextInputEditText
    private lateinit var dietaryNeedsEditText: TextInputEditText
    
    // Behavioral Traits
    private lateinit var temperamentEditText: TextInputEditText
    private lateinit var energyLevelDropdown: AutoCompleteTextView
    private lateinit var goodWithChildrenCheckbox: CheckBox
    private lateinit var goodWithDogsCheckbox: CheckBox
    private lateinit var goodWithCatsCheckbox: CheckBox
    private lateinit var trainingLevelDropdown: AutoCompleteTextView
    
    // Adoption Details
    private lateinit var priceEditText: TextInputEditText
    private lateinit var adoptionRequirementsEditText: TextInputEditText
    
    // Button
    private lateinit var addButton: Button
    
    private val petRepository = PetRepository()
    private val auth = FirebaseAuth.getInstance()
    private val storage = FirebaseStorage.getInstance()
    private val storageRef = storage.reference.child("pet_images")
    
    private var selectedImageUris = mutableListOf<Uri>()
    private lateinit var imagesAdapter: SelectedImagesAdapter
    
    private lateinit var pickImageLauncher: ActivityResultLauncher<Intent>
    
    // Permission request code
    private val READ_EXTERNAL_STORAGE_REQUEST = 101
    
    private var isUploading = false
    private var uploadJob: Job? = null
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_pet)
        
        // Initialize views
        initializeViews()
        
        // Setup toolbar
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Add New Pet"
        
        // Setup dropdowns
        setupDropdowns()
        
        // Setup image picker
        setupImagePicker()
        
        // Set up button click listener
        addButton.setOnClickListener {
            if (validateInputs()) {
                uploadImagesAndAddPet()
            }
        }
        
        // Set up add image button
        addImageButton.setOnClickListener {
            checkStoragePermissionAndPickImage()
        }
    }
    
    private fun initializeViews() {
        // Toolbar
        toolbar = findViewById(R.id.toolbar)
        
        // Basic Information
        nameEditText = findViewById(R.id.editTextPetName)
        typeDropdown = findViewById(R.id.editTextPetType)
        breedEditText = findViewById(R.id.editTextPetBreed)
        ageEditText = findViewById(R.id.editTextPetAge)
        genderDropdown = findViewById(R.id.editTextPetGender)
        descriptionEditText = findViewById(R.id.editTextPetDescription)
        
        // Image Selection
        addImageButton = findViewById(R.id.buttonAddImage)
        selectedImagesRecyclerView = findViewById(R.id.recyclerViewSelectedImages)
        imageProgressBar = findViewById(R.id.imageUploadProgressBar)
        
        // Setup RecyclerView
        imagesAdapter = SelectedImagesAdapter(
            selectedImageUris,
            onDeleteClicked = { position ->
                selectedImageUris.removeAt(position)
                imagesAdapter.notifyItemRemoved(position)
            }
        )
        selectedImagesRecyclerView.layoutManager = 
            LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        selectedImagesRecyclerView.adapter = imagesAdapter
        
        // Physical Characteristics
        sizeDropdown = findViewById(R.id.sizeDropdown)
        colorEditText = findViewById(R.id.colorInput)
        weightEditText = findViewById(R.id.weightInput)
        sheddingDropdown = findViewById(R.id.sheddingDropdown)
        
        // Health Information
        vaccinatedCheckbox = findViewById(R.id.vaccinatedCheckbox)
        spayedNeuteredCheckbox = findViewById(R.id.spayedNeuteredCheckbox)
        medicalHistoryEditText = findViewById(R.id.medicalHistoryInput)
        specialNeedsEditText = findViewById(R.id.specialNeedsInput)
        dietaryNeedsEditText = findViewById(R.id.dietaryNeedsInput)
        
        // Behavioral Traits
        temperamentEditText = findViewById(R.id.temperamentInput)
        energyLevelDropdown = findViewById(R.id.energyLevelDropdown)
        goodWithChildrenCheckbox = findViewById(R.id.goodWithChildrenCheckbox)
        goodWithDogsCheckbox = findViewById(R.id.goodWithDogsCheckbox)
        goodWithCatsCheckbox = findViewById(R.id.goodWithCatsCheckbox)
        trainingLevelDropdown = findViewById(R.id.trainingLevelDropdown)
        
        // Adoption Details
        priceEditText = findViewById(R.id.editTextPetPrice)
        adoptionRequirementsEditText = findViewById(R.id.adoptionRequirementsInput)
        
        // Button
        addButton = findViewById(R.id.buttonAddPet)
    }
    
    private fun setupImagePicker() {
        pickImageLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                val data = result.data
                
                // Handle multiple images (if available)
                if (data?.clipData != null) {
                    val clipData = data.clipData!!
                    for (i in 0 until clipData.itemCount) {
                        val uri = clipData.getItemAt(i).uri
                        if (!selectedImageUris.contains(uri)) {
                            selectedImageUris.add(uri)
                        }
                    }
                } 
                // Handle single image
                else if (data?.data != null) {
                    val uri = data.data!!
                    if (!selectedImageUris.contains(uri)) {
                        selectedImageUris.add(uri)
                    }
                }
                
                imagesAdapter.notifyDataSetChanged()
            }
        }
    }
    
    private fun checkStoragePermissionAndPickImage() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // For Android 13+ we need to request READ_MEDIA_IMAGES permission
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.READ_MEDIA_IMAGES
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.READ_MEDIA_IMAGES),
                    READ_EXTERNAL_STORAGE_REQUEST
                )
            } else {
                openImagePicker()
            }
        } else {
            // For older versions, we use READ_EXTERNAL_STORAGE
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.READ_EXTERNAL_STORAGE
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
                    READ_EXTERNAL_STORAGE_REQUEST
                )
            } else {
                openImagePicker()
            }
        }
    }
    
    private fun openImagePicker() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
        intent.type = "image/*"
        pickImageLauncher.launch(intent)
    }
    
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == READ_EXTERNAL_STORAGE_REQUEST) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openImagePicker()
            } else {
                Toast.makeText(
                    this,
                    "Permission denied. Cannot select images.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }
    
    private fun setupDropdowns() {
        // Pet Type Dropdown
        val petTypes = arrayOf("Dog", "Cat", "Bird", "Rabbit", "Fish", "Reptile", "Other")
        val typeAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, petTypes)
        typeDropdown.setAdapter(typeAdapter)
        
        // Gender Dropdown
        val genders = arrayOf("Male", "Female")
        val genderAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, genders)
        genderDropdown.setAdapter(genderAdapter)
        
        // Size Dropdown
        val sizes = arrayOf("Small", "Medium", "Large")
        val sizeAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, sizes)
        sizeDropdown.setAdapter(sizeAdapter)
        
        // Shedding Dropdown
        val sheddingLevels = arrayOf("Low", "Medium", "High")
        val sheddingAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, sheddingLevels)
        sheddingDropdown.setAdapter(sheddingAdapter)
        
        // Energy Level Dropdown
        val energyLevels = arrayOf("Low", "Medium", "High")
        val energyAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, energyLevels)
        energyLevelDropdown.setAdapter(energyAdapter)
        
        // Training Level Dropdown
        val trainingLevels = arrayOf("None", "Basic", "Advanced")
        val trainingAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, trainingLevels)
        trainingLevelDropdown.setAdapter(trainingAdapter)
    }
    
    private fun validateInputs(): Boolean {
        var isValid = true
        
        // Validate required fields
        val name = nameEditText.text.toString().trim()
        val type = typeDropdown.text.toString().trim()
        val breed = breedEditText.text.toString().trim()
        val ageStr = ageEditText.text.toString().trim()
        val gender = genderDropdown.text.toString().trim()
        val description = descriptionEditText.text.toString().trim()
        
        if (name.isEmpty()) {
            nameEditText.error = "Name is required"
            isValid = false
        }
        
        if (type.isEmpty()) {
            Toast.makeText(this, "Please select a pet type", Toast.LENGTH_SHORT).show()
            isValid = false
        }
        
        if (breed.isEmpty()) {
            breedEditText.error = "Breed is required"
            isValid = false
        }
        
        if (ageStr.isEmpty()) {
            ageEditText.error = "Age is required"
            isValid = false
        }
        
        if (gender.isEmpty()) {
            Toast.makeText(this, "Please select a gender", Toast.LENGTH_SHORT).show()
            isValid = false
        }
        
        if (description.isEmpty()) {
            descriptionEditText.error = "Description is required"
            isValid = false
        }
        
        if (selectedImageUris.isEmpty()) {
            Toast.makeText(this, "Please select at least one image", Toast.LENGTH_SHORT).show()
            isValid = false
        }
        
        return isValid
    }
    
    private fun uploadImagesAndAddPet() {
        if (isUploading) {
            return // Prevent multiple submissions
        }
        
        // Show progress and disable add button
        imageProgressBar.visibility = View.VISIBLE
        addButton.isEnabled = false
        isUploading = true
        
        uploadJob = lifecycleScope.launch {
            try {
                val imageUrls = uploadImages()
                val petId = addPetToFirestore(imageUrls)
                
                if (!isActive) {
                    // If the job was cancelled, don't proceed
                    return@launch
                }
                
                runOnUiThread {
                    Toast.makeText(this@AddPetActivity, "Pet added successfully!", Toast.LENGTH_SHORT).show()
                    setResult(RESULT_OK)
                    isUploading = false
                    finish()
                }
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) {
                    runOnUiThread {
                        Toast.makeText(this@AddPetActivity, "Upload cancelled", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    runOnUiThread {
                        Toast.makeText(this@AddPetActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            } finally {
                runOnUiThread {
                    imageProgressBar.visibility = View.GONE
                    addButton.isEnabled = true
                    isUploading = false
                }
            }
        }
    }
    
    private suspend fun uploadImages(): List<String> = withContext(Dispatchers.IO) {
        val uploadedUrls = mutableListOf<String>()
        
        for ((index, uri) in selectedImageUris.withIndex()) {
            val filename = UUID.randomUUID().toString()
            val fileRef = storageRef.child(filename)
            
            try {
                // Update progress on UI thread
                withContext(Dispatchers.Main) {
                    val progressMessage = "Uploading image ${index + 1}/${selectedImageUris.size}"
                    Toast.makeText(this@AddPetActivity, progressMessage, Toast.LENGTH_SHORT).show()
                }
                
                // Upload file to Firebase Storage
                val uploadTask = fileRef.putFile(uri).await()
                
                // Get download URL
                val downloadUrl = fileRef.downloadUrl.await().toString()
                uploadedUrls.add(downloadUrl)
            } catch (e: Exception) {
                // Check for specific Firebase Storage errors
                when {
                    e.message?.contains("permission_denied") == true -> {
                        throw Exception("Permission denied: You don't have access to upload images. Please check your authentication.")
                    }
                    e.message?.contains("network") == true -> {
                        throw Exception("Network error: Please check your internet connection and try again.")
                    }
                    e.message?.contains("canceled") == true -> {
                        throw Exception("Upload canceled")
                    }
                    e.message?.contains("quota") == true -> {
                        throw Exception("Storage quota exceeded: Please contact the app administrator.")
                    }
                    else -> {
                        throw Exception("Failed to upload image: ${e.message}")
                    }
                }
            }
        }
        
        if (uploadedUrls.isEmpty()) {
            throw Exception("No images were uploaded. Please try again.")
        }
        
        return@withContext uploadedUrls
    }
    
    private suspend fun addPetToFirestore(imageUrls: List<String>): String = withContext(Dispatchers.IO) {
        // Get current user info
        val currentUser = auth.currentUser
        val ownerId = currentUser?.uid ?: ""
        val ownerName = currentUser?.displayName ?: ""
        val ownerContact = currentUser?.email ?: ""
        
        // Basic Information
        val name = nameEditText.text.toString().trim()
        val type = typeDropdown.text.toString().trim().lowercase()
        val breed = breedEditText.text.toString().trim()
        val age = ageEditText.text.toString().toIntOrNull() ?: 0
        val gender = genderDropdown.text.toString().trim()
        val description = descriptionEditText.text.toString().trim()
        
        // Physical Characteristics
        val size = sizeDropdown.text.toString().trim()
        val color = colorEditText.text.toString().trim()
        val weight = weightEditText.text.toString().toDoubleOrNull() ?: 0.0
        val shedding = sheddingDropdown.text.toString().trim()
        
        // Health Information
        val vaccinationStatus = vaccinatedCheckbox.isChecked
        val spayedNeutered = spayedNeuteredCheckbox.isChecked
        val medicalHistory = medicalHistoryEditText.text.toString().trim()
        val specialNeeds = specialNeedsEditText.text.toString().trim()
        val dietaryNeeds = dietaryNeedsEditText.text.toString().trim()
        
        // Behavioral Traits
        val temperament = temperamentEditText.text.toString().trim()
        val energyLevel = energyLevelDropdown.text.toString().trim()
        val trainingLevel = trainingLevelDropdown.text.toString().trim()
        
        // Build goodWith list
        val goodWithList = mutableListOf<String>()
        if (goodWithChildrenCheckbox.isChecked) goodWithList.add("children")
        if (goodWithDogsCheckbox.isChecked) goodWithList.add("dogs")
        if (goodWithCatsCheckbox.isChecked) goodWithList.add("cats")
        
        // Adoption Details
        val adoptionFee = priceEditText.text.toString().toDoubleOrNull() ?: 0.0
        val adoptionRequirements = adoptionRequirementsEditText.text.toString().trim()
        
        // Create Pet object
        val pet = Pet(
            name = name,
            type = type,
            breed = breed,
            age = age,
            gender = gender,
            description = description,
            size = size,
            color = color,
            weight = weight,
            shedding = shedding,
            vaccinationStatus = vaccinationStatus,
            spayedNeutered = spayedNeutered,
            goodWith = goodWithList,
            energyLevel = energyLevel,
            temperament = temperament,
            trainingLevel = trainingLevel,
            dietaryNeeds = dietaryNeeds,
            specialNeeds = specialNeeds,
            medicalHistory = medicalHistory,
            adoptionFee = adoptionFee,
            adoptionRequirements = adoptionRequirements,
            ownerId = ownerId,
            ownerName = ownerName,
            ownerContact = ownerContact,
            imageUrl = if (imageUrls.isNotEmpty()) imageUrls[0] else null,
            imageUrls = imageUrls,
            featuredImage = if (imageUrls.isNotEmpty()) imageUrls[0] else null,
            featured = false,
            isAdopted = false,
            createdAt = System.currentTimeMillis(),
        )
        
        // Add to Firestore
        val result = petRepository.addPet(pet)
                
                if (result.isSuccess) {
            return@withContext result.getOrThrow()
                } else {
            throw result.exceptionOrNull() ?: Exception("Unknown error occurred")
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        uploadJob?.cancel()
    }
    
    override fun onBackPressed() {
        if (isUploading) {
            AlertDialog.Builder(this)
                .setTitle("Cancel Upload")
                .setMessage("Are you sure you want to cancel the upload? Your changes will be lost.")
                .setPositiveButton("Yes") { _, _ ->
                    uploadJob?.cancel()
                    super.onBackPressed()
                }
                .setNegativeButton("No", null)
                .show()
        } else {
            super.onBackPressed()
        }
    }
    
    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}