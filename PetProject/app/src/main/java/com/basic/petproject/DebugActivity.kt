package com.basic.petproject

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.basic.petproject.databinding.ActivityDebugBinding
import java.io.BufferedReader
import java.io.InputStreamReader

class DebugActivity : AppCompatActivity() {
    private lateinit var binding: ActivityDebugBinding
    private val TAG = "DebugActivity"
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDebugBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        binding.clearButton.setOnClickListener {
            binding.logTextView.text = ""
            Log.d(TAG, "Logs cleared")
        }
        
        binding.backButton.setOnClickListener {
            finish()
        }
        
        loadLogs()
    }
    
    private fun loadLogs() {
        val logBuilder = StringBuilder()
        
        try {
            val process = Runtime.getRuntime().exec("logcat -d -v threadtime")
            val bufferedReader = BufferedReader(InputStreamReader(process.inputStream))
            
            var line: String?
            while (bufferedReader.readLine().also { line = it } != null) {
                if (line?.contains("RegisterActivity") == true || 
                    line?.contains("UserRepository") == true || 
                    line?.contains("Firebase") == true) {
                    logBuilder.append(line).append("\n")
                }
            }
        } catch (e: Exception) {
            logBuilder.append("Error reading logs: ${e.message}")
            Log.e(TAG, "Error reading logs", e)
        }
        
        binding.logTextView.text = logBuilder.toString()
    }
    
    override fun onResume() {
        super.onResume()
        loadLogs()
    }
} 