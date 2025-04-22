package com.basic.petproject

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.basic.petproject.admin.AdminDashboardActivity
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import com.google.firebase.auth.PhoneMultiFactorGenerator
import com.google.firebase.auth.PhoneMultiFactorInfo
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.auth.FirebaseAuthMultiFactorException
import com.google.firebase.auth.MultiFactorResolver
import java.util.concurrent.TimeUnit

class LoginActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private val TAG = "LoginActivity"
    private var multiFactorResolver: MultiFactorResolver? = null
    private var verificationId: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_login)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        title = "Login"

        // Initialize Firebase Auth
        auth = Firebase.auth

        // Setup UI components
        val editTextEmailAddress: TextInputEditText = findViewById(R.id.loginEmail)
        val editTextPassword: TextInputEditText = findViewById(R.id.loginPassword)
        val buttonLogin: Button = findViewById(R.id.buttonLogin)

        // Hide Google Sign-In button

        // Add debug mode - triple click on title to access debug
        val loginTitle = findViewById<View>(R.id.loginTitle)
        var clickCount = 0
        val resetHandler = Handler(android.os.Looper.getMainLooper())
        
        loginTitle?.setOnClickListener {
            clickCount++
            resetHandler.removeCallbacksAndMessages(null)
            resetHandler.postDelayed({
                if (clickCount >= 3) {
                    // Open debug activity
                    startActivity(Intent(this, DebugActivity::class.java))
                }
                clickCount = 0
            }, 500) // Reset after 500ms
        }

        buttonLogin.setOnClickListener {
            val email: String = editTextEmailAddress.text.toString().trim()
            val password: String = editTextPassword.text.toString().trim()

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            login(email, password)
        }
    }

    private fun login(email: String, password: String) {
        auth.signInWithEmailAndPassword(email, password).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                // Login successful, check if admin
                checkUserRole(auth.currentUser?.uid)
            } else {
                // Check if the exception is due to MFA required
                val exception = task.exception
                if (exception is FirebaseAuthMultiFactorException) {
                    // Get the MultiFactorResolver from the exception
                    multiFactorResolver = exception.resolver
                    // Get the phone number hints from the resolver
                    val hints = multiFactorResolver?.hints ?: emptyList()
                    
                    if (hints.isNotEmpty()) {
                        // Get the first phone hint (assuming only one phone is registered as a second factor)
                        val phoneMultiFactorInfo = hints[0] as? PhoneMultiFactorInfo
                        
                        if (phoneMultiFactorInfo != null) {
                            // Start the phone verification process
                            startPhoneVerification(phoneMultiFactorInfo)
                        } else {
                            Log.e(TAG, "No phone multi-factor info found")
                            Toast.makeText(this, "Authentication error", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        Log.e(TAG, "No multi-factor hints found")
                        Toast.makeText(this, "Authentication error", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    // Handle other login failures
                    Toast.makeText(
                        applicationContext,
                        "Login failed: " + (task.exception?.localizedMessage ?: "Unknown error"),
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    private fun checkUserRole(userId: String?) {
        if (userId == null) {
            navigateToRegularDashboard()
            return
        }

        FirebaseFirestore.getInstance().collection("users").document(userId)
            .get()
            .addOnSuccessListener { documentSnapshot ->
                val isAdmin = documentSnapshot.getBoolean("isAdmin") ?: false
                
                // Save user data to SharedPreferences
                val sharedPreferences = getSharedPreferences("UserPrefs", MODE_PRIVATE)
                sharedPreferences.edit()
                    .putString("userId", userId)
                    .putString("userName", documentSnapshot.getString("name") ?: "")
                    .putString("userEmail", documentSnapshot.getString("email") ?: "")
                    .putBoolean("isAdmin", isAdmin)
                    .apply()
                
                // Navigate to appropriate dashboard
                if (isAdmin) {
                    navigateToAdminDashboard()
                } else {
                    navigateToRegularDashboard()
                }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error checking user role", e)
                // Default to regular user on error
                navigateToRegularDashboard()
            }
    }

    private fun navigateToAdminDashboard() {
        val intent = Intent(this, AdminDashboardActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    private fun navigateToRegularDashboard() {
        val intent = Intent(this, Dashboard::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    private fun startPhoneVerification(phoneMultiFactorInfo: PhoneMultiFactorInfo) {
        // Create a PhoneAuthOptions object for the second factor
        val options = PhoneAuthOptions.newBuilder()
            .setMultiFactorHint(phoneMultiFactorInfo)
            .setTimeout(60L, TimeUnit.SECONDS)
            .setActivity(this)
            .setMultiFactorSession(multiFactorResolver!!.session)
            .setCallbacks(object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                override fun onVerificationCompleted(credential: com.google.firebase.auth.PhoneAuthCredential) {
                    // This callback will be invoked in two situations:
                    // 1) Instant verification. In some cases, the phone number can be instantly
                    //    verified without needing to send or enter a verification code.
                    // 2) Auto-retrieval. On some devices, Google Play services can automatically
                    //    detect the incoming verification SMS and perform verification without
                    //    user action.
                    Log.d(TAG, "onVerificationCompleted: Auto-verification successful")
                    // We don't automatically complete MFA sign-in here as we want user to enter the code
                }

                override fun onVerificationFailed(e: com.google.firebase.FirebaseException) {
                    Log.e(TAG, "onVerificationFailed: ${e.message}", e)
                    Toast.makeText(
                        this@LoginActivity,
                        "Verification failed: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }

                override fun onCodeSent(
                    vId: String,
                    token: PhoneAuthProvider.ForceResendingToken
                ) {
                    super.onCodeSent(vId, token)
                    Log.d(TAG, "onCodeSent: Verification ID = $vId")
                    verificationId = vId
                    
                    // Launch OTP input activity or show OTP input dialog
                    // For simplicity, we'll just show a toast message
                    Toast.makeText(
                        this@LoginActivity,
                        "OTP sent to your phone. Please enter the code.",
                        Toast.LENGTH_SHORT
                    ).show()
                    
                    // In a real app, you would launch an activity to collect the OTP
                    // For now, we'll simulate with a dialog or another activity
                    showOtpInputDialog()
                }
            })
            .build()

        // Start the phone verification process
        PhoneAuthProvider.verifyPhoneNumber(options)
    }

    private fun showOtpInputDialog() {
        // In a real app, you would show a dialog or launch an activity to collect the OTP
        // For this example, we'll just use a simple dialog
        val otpDialog = android.app.AlertDialog.Builder(this)
            .setTitle("Enter OTP")
            .setMessage("Enter the 6-digit code sent to your phone")
            .setView(android.widget.EditText(this).apply {
                hint = "6-digit code"
                inputType = android.text.InputType.TYPE_CLASS_NUMBER
            })
            .setPositiveButton("Verify") { dialog, _ ->
                val editText = (dialog as android.app.AlertDialog).findViewById<android.widget.EditText>(android.R.id.edit)
                val code = editText?.text.toString()
                if (code.length == 6) {
                    verifyOtp(code)
                } else {
                    Toast.makeText(this, "Please enter a valid 6-digit code", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
            .create()
        
        otpDialog.show()
    }

    private fun verifyOtp(code: String) {
        if (verificationId.isNotEmpty() && multiFactorResolver != null) {
            val credential = PhoneAuthProvider.getCredential(verificationId, code)
            val multiFactorAssertion = PhoneMultiFactorGenerator.getAssertion(credential)
            
            multiFactorResolver?.resolveSignIn(multiFactorAssertion)
                ?.addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        // User successfully signed in with the second factor
                        Log.d(TAG, "Multi-factor sign-in successful")
                        // Check if user is admin
                        checkUserRole(auth.currentUser?.uid)
                    } else {
                        // Sign-in failed
                        Log.e(TAG, "Multi-factor sign-in failed: ${task.exception?.message}")
                        Toast.makeText(
                            this,
                            "Authentication failed: ${task.exception?.message}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
        } else {
            Log.e(TAG, "VerificationId or MultiFactorResolver is null")
            Toast.makeText(this, "Authentication error", Toast.LENGTH_SHORT).show()
        }
    }

    fun goToRegister(view: View) {
        val intent = Intent(this, RegisterActivity::class.java)
        startActivity(intent)
        finish() // Add this line to close the current activity
    }
}