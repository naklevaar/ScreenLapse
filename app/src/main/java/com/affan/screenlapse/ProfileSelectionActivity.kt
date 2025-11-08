package com.affan.screenlapse

import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import android.util.Log

class ProfileSelectionActivity : AppCompatActivity() {

    private lateinit var profilesLayout: LinearLayout
    private lateinit var addProfileButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile_selection)

        profilesLayout = findViewById(R.id.profiles_container)
        addProfileButton = findViewById(R.id.add_profile_button)

        // Initialize default profile if none exist
        ProfileManager.initializeDefaultProfile(applicationContext)
        loadProfiles()

        addProfileButton.setOnClickListener {
            showAddProfileDialog()
        }
    }

    private fun loadProfiles() {
        profilesLayout.removeAllViews()
        val profiles = ProfileManager.getAllProfiles(applicationContext)

        for ((kidId, name) in profiles) {
            val button = Button(this).apply {
                text = name
                setOnClickListener {
                    setActiveProfile(kidId, name)
                }
            }
            profilesLayout.addView(button)
        }
    }

    private fun showAddProfileDialog() {
        val profiles = ProfileManager.getAllProfiles(applicationContext)
        if (profiles.size >= 2) {
            Toast.makeText(this, "Only 2 profiles allowed.", Toast.LENGTH_SHORT).show()
            return
        }

        val input = EditText(this)
        AlertDialog.Builder(this)
            .setTitle("Enter Profile Name")
            .setView(input)
            .setPositiveButton("Add") { _, _ ->
                val name = input.text.toString().trim()
                val existingNames = profiles.map { it.second }.toSet()
                if (name.isNotEmpty() && name !in existingNames) {
                    val kidId = if (profiles.isEmpty()) 1 else 2 // Assign kidId 1 or 2
                    ProfileManager.saveKidName(applicationContext, kidId, name)
                    ProfileManager.setActiveKid(applicationContext, kidId)
                    Log.d("ProfileSelection", "Added profile: kidId=$kidId, name=$name")
                    loadProfiles()
                } else {
                    Toast.makeText(this, "Invalid or duplicate name", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun setActiveProfile(kidId: Int, profile: String) {
        ProfileManager.setActiveKid(applicationContext, kidId)
        Log.d("ProfileSelection", "Selected profile: kidId=$kidId, name=$profile")
        ProfileManager.debugSharedPreferences(applicationContext)
        Toast.makeText(this, "$profile selected", Toast.LENGTH_SHORT).show()

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        finish()
    }
}