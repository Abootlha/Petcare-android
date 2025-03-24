package com.example.petproject

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.MultiFactorInfo
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import com.google.firebase.auth.PhoneMultiFactorGenerator
import com.google.firebase.auth.PhoneMultiFactorInfo
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.google.firebase.auth.FirebaseAuthMultiFactorException
import com.google.firebase.auth.MultiFactorResolver
import java.util.concurrent.TimeUnit

class LoginActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var googleSignInClient: GoogleSignInClient
    private val RC_SIGN_IN = 9001
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

        // Configure Google Sign In
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(this, gso)

        // Setup UI components
        val editTextEmailAddress: TextInputEditText = findViewById(R.id.loginEmail)
        val editTextPassword: TextInputEditText = findViewById(R.id.loginPassword)
        val buttonLogin: Button = findViewById(R.id.buttonLogin)
        val googleSignInButton: LinearLayout = findViewById(R.id.googleSignInButton)

        buttonLogin.setOnClickListener {
            val email: String = editTextEmailAddress.text.toString().trim()
            val password: String = editTextPassword.text.toString().trim()

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            login(email, password)
        }

        googleSignInButton.setOnClickListener {
            signInWithGoogle()
        }
    }

    private fun login(email: String, password: String) {
        auth.signInWithEmailAndPassword(email, password).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                // Login successful, go to dashboard
                val intent = Intent(this, Dashboard::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finish()
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
                        val intent = Intent(this, Dashboard::class.java)
                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        startActivity(intent)
                        finish()
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

    private fun signInWithGoogle() {
        val signInIntent = googleSignInClient.signInIntent
        startActivityForResult(signInIntent, RC_SIGN_IN)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == RC_SIGN_IN) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                // Google Sign In was successful, authenticate with Firebase
                val account = task.getResult(ApiException::class.java)
                firebaseAuthWithGoogle(account)
            } catch (e: ApiException) {
                // Google Sign In failed
                Log.w(TAG, "Google sign in failed", e) // Log the exception
                Toast.makeText(this, "Google sign in failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun firebaseAuthWithGoogle(account: GoogleSignInAccount?) {  // Making account nullable
        account?.let {  // Using let to safely handle null account
            val credential = GoogleAuthProvider.getCredential(it.idToken, null)
            auth.signInWithCredential(credential)
                .addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        // Sign in success, update UI with the signed-in user's information
                        val intent = Intent(this, Dashboard::class.java)
                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        startActivity(intent)
                        finish()
                    } else {
                        // Check if the exception is due to MFA required
                        val exception = task.exception
                        if (exception is FirebaseAuthMultiFactorException) {
                            // Handle MFA for Google Sign-In similar to email/password login
                            multiFactorResolver = exception.resolver
                            val hints = multiFactorResolver?.hints ?: emptyList()
                            
                            if (hints.isNotEmpty()) {
                                val phoneMultiFactorInfo = hints[0] as? PhoneMultiFactorInfo
                                if (phoneMultiFactorInfo != null) {
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
                            // If sign in fails, display a message to the user.
                            Log.w(TAG, "Firebase authentication failed", task.exception)  // Log the exception
                            Toast.makeText(
                                this, "Authentication Failed: ${task.exception?.message}",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }
        } ?: run {  // Handle case where account is null
            Toast.makeText(this, "Google Sign-in account is null.", Toast.LENGTH_SHORT).show()
            Log.e(TAG, "Google Sign-in account is null.")
        }
    }

    fun goToRegister(view: View) {
        val intent = Intent(this, RegisterActivity::class.java)
        startActivity(intent)
        finish() // Add this line to close the current activity
    }
}