package com.affan.screenlapse

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class BugReportActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_bug_report)

        val nameInput = findViewById<EditText>(R.id.name_input)
        val bugInput = findViewById<EditText>(R.id.bug_input)
        val submitButton = findViewById<Button>(R.id.submit_button)

        submitButton.setOnClickListener {
            val name = nameInput.text.toString().trim()
            val bugDetails = bugInput.text.toString().trim()

            if (bugDetails.isEmpty()) {
                Toast.makeText(this, "Please describe the bug!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val emailIntent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("mailto:")
                putExtra(Intent.EXTRA_EMAIL, arrayOf("devteam@example.com"))
                putExtra(Intent.EXTRA_SUBJECT, "Bug Report from ${name.ifEmpty { "Anonymous" }}")
                putExtra(Intent.EXTRA_TEXT, bugDetails)
            }

            if (emailIntent.resolveActivity(packageManager) != null) {
                startActivity(Intent.createChooser(emailIntent, "Send email..."))
            } else {
                Toast.makeText(this, "No email app found!", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
