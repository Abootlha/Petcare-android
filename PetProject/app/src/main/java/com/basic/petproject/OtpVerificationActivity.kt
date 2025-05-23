package com.basic.petproject
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.ktx.Firebase
import java.util.concurrent.TimeUnit

class OtpVerificationActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var verificationId: String
    private lateinit var phoneNumber: String
    private lateinit var email: String
    private lateinit var password: String
    private val TAG = "OtpVerificationActivity"

    private lateinit var otpDigit1: EditText
    private lateinit var otpDigit2: EditText
    private lateinit var otpDigit3: EditText
    private lateinit var otpDigit4: EditText
    private lateinit var otpDigit5: EditText
    private lateinit var otpDigit6: EditText
    private lateinit var buttonVerifyOtp: Button
    private lateinit var resendOtp: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_otp_verification)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        title = "Phone Verification"
        auth = Firebase.auth

        // Get data from intent
        phoneNumber = intent.getStringExtra("phone") ?: ""
        email = intent.getStringExtra("email") ?: ""
        password = intent.getStringExtra("password") ?: ""

        Log.d(TAG, "Phone number received: $phoneNumber")
        Log.d(TAG, "Email received: $email")

        initViews()
        setupOtpInputs()
        
        // Send verification code to phone first
        sendVerificationCode(phoneNumber)

        buttonVerifyOtp.setOnClickListener {
            verifyCode()
        }

        resendOtp.setOnClickListener {
            sendVerificationCode(phoneNumber)
            Toast.makeText(this, "OTP sent again", Toast.LENGTH_SHORT).show()
        }
    }

    private fun initViews() {
        otpDigit1 = findViewById(R.id.otpDigit1)
        otpDigit2 = findViewById(R.id.otpDigit2)
        otpDigit3 = findViewById(R.id.otpDigit3)
        otpDigit4 = findViewById(R.id.otpDigit4)
        otpDigit5 = findViewById(R.id.otpDigit5)
        otpDigit6 = findViewById(R.id.otpDigit6)
        buttonVerifyOtp = findViewById(R.id.buttonVerifyOtp)
        resendOtp = findViewById(R.id.resendOtp)
    }

    private fun setupOtpInputs() {
        // Auto-focus to next field when a digit is entered
        otpDigit1.addTextChangedListener(createTextWatcher(otpDigit2))
        otpDigit2.addTextChangedListener(createTextWatcher(otpDigit3))
        otpDigit3.addTextChangedListener(createTextWatcher(otpDigit4))
        otpDigit4.addTextChangedListener(createTextWatcher(otpDigit5))
        otpDigit5.addTextChangedListener(createTextWatcher(otpDigit6))
        otpDigit6.addTextChangedListener(createTextWatcher(null))
    }

    private fun createTextWatcher(nextField: EditText?): TextWatcher {
        return object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                if (s?.length == 1 && nextField != null) {
                    nextField.requestFocus()
                }
            }
        }
    }

    private fun sendVerificationCode(phoneNumber: String) {
        try {
            val formattedNumber = formatPhoneNumber(phoneNumber)
            Log.d(TAG, "Sending verification code to: $formattedNumber")

            // Check if we should use test verification
            if (isTestMode(formattedNumber)) {
                // For testing purposes - simulate verification success
                Log.d(TAG, "Using test verification mode")
                Toast.makeText(this, "TEST MODE: Verification code sent to $formattedNumber", Toast.LENGTH_SHORT).show()
                this@OtpVerificationActivity.verificationId = "test-verification-id"
                return
            }

            // Create a PhoneAuthOptions object for direct phone verification
            val options = PhoneAuthOptions.newBuilder(auth)
                .setPhoneNumber(formattedNumber)
                .setTimeout(60L, TimeUnit.SECONDS)
                .setActivity(this)
                .setCallbacks(object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                    override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                        // Auto-verification if the device has the phone number
                        Log.d(TAG, "onVerificationCompleted: Auto-verification successful")
                        // We don't automatically verify as we want user to enter the code
                    }

                    override fun onVerificationFailed(e: FirebaseException) {
                        Log.e(TAG, "onVerificationFailed: ${e.message}", e)
                        
                        // Log more detailed error information
                        when (e) {
                            is com.google.firebase.FirebaseTooManyRequestsException -> {
                                Log.e(TAG, "Too many requests - quota exceeded")
                                Toast.makeText(
                                    this@OtpVerificationActivity,
                                    "Verification failed: Too many requests. Try again later.",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                            is com.google.firebase.auth.FirebaseAuthInvalidCredentialsException -> {
                                Log.e(TAG, "Invalid phone number format")
                                Toast.makeText(
                                    this@OtpVerificationActivity,
                                    "Invalid phone number format. Please check the number and try again.",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                            else -> {
                                // Check if the error message contains billing-related keywords
                                val errorMsg = e.message?.lowercase() ?: ""
                                if (errorMsg.contains("billing") || errorMsg.contains("payment") || 
                                    errorMsg.contains("quota") || errorMsg.contains("enable")) {
                                    Log.e(TAG, "Billing or API enablement issue detected")
                                    Toast.makeText(
                                        this@OtpVerificationActivity,
                                        "Firebase Phone Auth service issue: Please check Firebase console and ensure billing is properly set up",
                                        Toast.LENGTH_LONG
                                    ).show()
                                } else {
                                    Toast.makeText(
                                        this@OtpVerificationActivity,
                                        "Verification failed: ${e.message}",
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
                            }
                        }
                    }

                    override fun onCodeSent(
                        verificationId: String,
                        token: PhoneAuthProvider.ForceResendingToken
                    ) {
                        super.onCodeSent(verificationId, token)
                        Log.d(TAG, "onCodeSent: Verification ID = $verificationId")
                        this@OtpVerificationActivity.verificationId = verificationId
                        Toast.makeText(
                            this@OtpVerificationActivity,
                            "OTP sent to your phone",
                            Toast.LENGTH_SHORT
                        ).show()
                    }

                    override fun onCodeAutoRetrievalTimeOut(verificationId: String) {
                        super.onCodeAutoRetrievalTimeOut(verificationId)
                        Log.d(TAG, "onCodeAutoRetrievalTimeOut: Verification ID = $verificationId")
                    }
                })
                .build()

            PhoneAuthProvider.verifyPhoneNumber(options)
        } catch (e: FirebaseException) {
            Log.e(TAG, "Phone number formatting error: ${e.message}", e)
            Toast.makeText(this, "${e.message}", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error: ${e.message}", e)
            Toast.makeText(this, "An unexpected error occurred: ${e.message}", Toast.LENGTH_LONG)
                .show()
        }
    }

    private fun formatPhoneNumber(phoneNumber: String): String {
        // Remove any non-digit characters
        val digitsOnly = phoneNumber.replace(Regex("\\D"), "")

        // Check if the number already has a country code (starts with +)
        if (phoneNumber.startsWith("+")) {
            // Validate the length of the number with country code
            if (phoneNumber.length < 8) { // Minimum length for a valid international number
                throw FirebaseException("Phone number too short. Please enter a valid phone number.")
            }
            return phoneNumber
        }

        // If the number starts with a leading zero, remove it
        val normalizedNumber = if (digitsOnly.startsWith("0")) {
            digitsOnly.substring(1)
        } else {
            digitsOnly
        }

        // Validate the length of the normalized number
        if (normalizedNumber.length < 10) { // Indian numbers are typically 10 digits
            throw FirebaseException("Phone number too short. Please enter a valid 10-digit phone number.")
        }

        // Add the Indian country code (+91)
        return "+91$normalizedNumber"
    }

    private fun isTestMode(phoneNumber: String): Boolean {
        // Use test mode for these specific test numbers or when running on an emulator
        val testNumbers = listOf("+15555215554", "+15555215556", "+15555215558", "+15555215560")
        return phoneNumber in testNumbers || android.os.Build.PRODUCT.contains("sdk")
    }

    private fun verifyCode() {
        val code = otpDigit1.text.toString() +
                otpDigit2.text.toString() +
                otpDigit3.text.toString() +
                otpDigit4.text.toString() +
                otpDigit5.text.toString() +
                otpDigit6.text.toString()

        if (code.length == 6) {
            Log.d(TAG, "Verifying code: $code with verification ID: $verificationId")
            
            // If in test mode, skip actual verification
            if (verificationId == "test-verification-id") {
                Log.d(TAG, "TEST MODE: Proceeding with account creation")
                // Skip actual verification and proceed with account creation
                if (email.isNotEmpty() && password.isNotEmpty()) {
                    signInWithEmail()
                } else {
                    Log.e(TAG, "Invalid email or password")
                    Toast.makeText(this, "Invalid email or password", Toast.LENGTH_SHORT).show()
                    finish()
                }
                return
            }
            
            val credential = PhoneAuthProvider.getCredential(verificationId, code)
            signInWithPhoneAuthCredential(credential)
        } else {
            Toast.makeText(this, "Please enter a valid OTP", Toast.LENGTH_SHORT).show()
        }
    }

    private fun signInWithPhoneAuthCredential(credential: PhoneAuthCredential) {
        // After phone verification is successful, create the user account
        if (email.isNotEmpty() && password.isNotEmpty()) {
            auth.createUserWithEmailAndPassword(email, password).addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Log.d(TAG, "Email/password account created successfully")
                    
                    // Save user data to Firestore
                    val userId = auth.currentUser?.uid
                    if (userId != null) {
                        val userProfile = hashMapOf(
                            "id" to userId,
                            "email" to email,
                            "phone" to phoneNumber,
                            "name" to "",  // Can be updated later in profile
                            "address" to "",  // Can be updated later in profile
                            "profileImageUrl" to "",  // Can be updated later in profile
                            "isAdmin" to false,
                            "createdAt" to System.currentTimeMillis()
                        )
                        
                        // Save to Firestore
                        val db = FirebaseFirestore.getInstance()
                        db.collection("users").document(userId)
                            .set(userProfile)
                            .addOnSuccessListener {
                                Log.d(TAG, "User profile created successfully")
                                Toast.makeText(this, "Registration successful!", Toast.LENGTH_SHORT).show()
                                
                                // Registration complete, go to dashboard
                                val intent = Intent(this, Dashboard::class.java)
                                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                startActivity(intent)
                                finish()
                            }
                            .addOnFailureListener { e ->
                                Log.e(TAG, "Error creating user profile", e)
                                Toast.makeText(this, "Error creating user profile: ${e.message}", Toast.LENGTH_LONG).show()
                            }
                    } else {
                        Log.e(TAG, "User ID is null after authentication")
                        Toast.makeText(this, "Registration error: User ID is null", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Log.e(TAG, "Registration failed: ${task.exception?.message}", task.exception)
                    Toast.makeText(
                        this,
                        "Registration failed: ${task.exception?.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        } else {
            Log.e(TAG, "Invalid email or password")
            Toast.makeText(this, "Invalid email or password", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun signInWithEmail() {
        auth.createUserWithEmailAndPassword(email, password).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                Log.d(TAG, "Email/password account created successfully")
                
                // Save user data to Firestore
                val userId = auth.currentUser?.uid
                if (userId != null) {
                    val userProfile = hashMapOf(
                        "id" to userId,
                        "email" to email,
                        "phone" to phoneNumber,
                        "name" to "",  // Can be updated later in profile
                        "address" to "",  // Can be updated later in profile
                        "profileImageUrl" to "",  // Can be updated later in profile
                        "isAdmin" to false,
                        "createdAt" to System.currentTimeMillis()
                    )
                    
                    // Save to Firestore
                    val db = FirebaseFirestore.getInstance()
                    db.collection("users").document(userId)
                        .set(userProfile)
                        .addOnSuccessListener {
                            Log.d(TAG, "User profile created successfully")
                            Toast.makeText(this, "Registration successful!", Toast.LENGTH_SHORT).show()
                            
                            // Registration complete, go to dashboard
                            val intent = Intent(this, Dashboard::class.java)
                            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                            startActivity(intent)
                            finish()
                        }
                        .addOnFailureListener { e ->
                            Log.e(TAG, "Error creating user profile", e)
                            Toast.makeText(this, "Error creating user profile: ${e.message}", Toast.LENGTH_LONG).show()
                        }
                } else {
                    Log.e(TAG, "User ID is null after authentication")
                    Toast.makeText(this, "Registration error: User ID is null", Toast.LENGTH_SHORT).show()
                }
            } else {
                Log.e(TAG, "Registration failed: ${task.exception?.message}", task.exception)
                Toast.makeText(
                    this,
                    "Registration failed: ${task.exception?.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }
}