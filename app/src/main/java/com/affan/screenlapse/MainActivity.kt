package com.affan.screenlapse

import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Shader
import android.net.Uri
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {

    private lateinit var pickAppsButton: Button
    private lateinit var setTimerButton: Button
    private lateinit var viewStatsButton: Button
    private lateinit var switchProfileButton: Button
    private lateinit var supportButton: Button
    private lateinit var privacyPolicyButton: Button
    private lateinit var supportText: TextView
    private lateinit var thankYouText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (!hasUsageAccessPermission(this)) {
            requestUsageAccess(this)
            return
        }

        setContentView(R.layout.activity_main)

        pickAppsButton = findViewById(R.id.pick_apps_button)
        setTimerButton = findViewById(R.id.set_timer_button)
        viewStatsButton = findViewById(R.id.view_stats_button)
        switchProfileButton = findViewById(R.id.switch_profile_button)
        supportButton = findViewById(R.id.supportButton)
        privacyPolicyButton = findViewById(R.id.privacy_policy_button)
        supportText = findViewById(R.id.supportText)
        supportText.post {
            val paint = supportText.paint
            val width = paint.measureText(supportText.text.toString())
            val textShader = LinearGradient(
                0f, 0f, width, supportText.textSize,
                intArrayOf(
                    Color.parseColor("#004D40"), // dark teal
                    Color.parseColor("#00897B"), // soft teal
                    Color.parseColor("#80CBC4")  // minty match
                ),
                null,
                Shader.TileMode.CLAMP
            )
            supportText.paint.shader = textShader
            supportText.setShadowLayer(4f, 1f, 1f, Color.parseColor("#22000000"))

            supportText.invalidate()
        }

        thankYouText = findViewById(R.id.thankYouText)

        disableAllButtons()
        authenticateUser()

        pickAppsButton.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }

        setTimerButton.setOnClickListener {
            startActivity(Intent(this, TimerActivity::class.java))
        }

        viewStatsButton.setOnClickListener {
            startActivity(Intent(this, StatsActivity::class.java))
        }

        switchProfileButton.setOnClickListener {
            startActivity(Intent(this, ProfileSelectionActivity::class.java))
            finish()
        }

        val infoButton: ImageButton = findViewById(R.id.info_button)
        infoButton.setOnClickListener {
            showImportantNote()
        }

        val helpButton = findViewById<Button>(R.id.help_button)
        helpButton.setOnClickListener {
            val pdfUrl = "https://drive.google.com/file/d/1x52bXbcO_mhHQ06Juolv7tNeLLWI3u6h/view"
            try {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(pdfUrl))
                startActivity(intent)
            } catch (e: Exception) {
                Log.e("MainActivity", "Failed to open PDF: ${e.message}", e)
                Toast.makeText(this, "Unable to open instructions. Please try again.", Toast.LENGTH_SHORT).show()
            }
        }

        supportButton.setOnClickListener {
            val bmcLink = "https://buymeacoffee.com/screenlapse"
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(bmcLink))
            startActivity(intent)
        }

        privacyPolicyButton.setOnClickListener {
            showPrivacyPolicyDialog()
        }
    }

    private fun showPrivacyPolicyDialog() {
        val dialogBuilder = androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Privacy Policy")
            .setMessage(
                """
                Last updated: May 14, 2025

                Your privacy is important to us. This application ("the App") is designed to help parents monitor and manage their child's app usage in a responsible and secure way.

                1. Information We Collect
                The App does not collect any personal information or data from your device that can identify you or your child.
                However, to function properly, the App:
                - Tracks app usage time locally (stored in your phone’s internal memory via SharedPreferences).
                - Uses Android’s Accessibility Service and Usage Stats permissions to detect which apps are opened and for how long.

                2. How We Use This Information
                The data is:
                - Used only for showing app usage statistics to the parent.
                - Stored locally on the device—it is never shared with third parties or uploaded to any server.
                - Accessible only within the App and deleted when the App is uninstalled.

                3. Accessibility Service Disclosure
                This App uses the Accessibility Service API to:
                - Detect which app is being used and for how long.
                - Apply screen lock overlays when the time limit is reached.
                The Accessibility Service is not used to control the device or collect personal information. This feature is core to the app’s function and is clearly disclosed to users before enabling it.

                4. Permissions
                To function properly, the App may request the following permissions:
                - Usage Stats Access: To detect how long apps are used.
                - Draw Over Other Apps: To display lock screens.
                - Fingerprint (Biometric): To allow parent-only access.
                - Accessibility Service: To monitor which apps are used in real-time.

                5. Data Security
                As the data is stored locally on your device, no data is transmitted or stored externally, reducing the risk of any data leak.

                6. Children’s Privacy
                The App is designed for use by parents, not children. It does not collect or store any personally identifiable information from children.

                7. Contact Us
                If you have any questions about this Privacy Policy, you can contact us at:
                affanahmadq10@gmail.com
                """
            )
            .setPositiveButton("OK") { dialog, _ -> dialog.dismiss() }

        val dialog = dialogBuilder.create()
        dialog.show()

        // Make the message text selectable and scrollable
        val messageTextView = dialog.findViewById<TextView>(android.R.id.message)
        messageTextView?.movementMethod = android.text.method.ScrollingMovementMethod()
        messageTextView?.setTextIsSelectable(true)
    }

    private fun showImportantNote() {
        val builder = androidx.appcompat.app.AlertDialog.Builder(this)
        builder.setTitle("Important Note")
        builder.setMessage(
            "After enabling Accessibility for ScreenLapse, please turn OFF and ON the permission once.\n\n(This ensures the timer lock feature works perfectly.)\n\nThank You!"
        )
        builder.setPositiveButton("OK", null)
        builder.show()
    }

    private fun disableAllButtons() {
        pickAppsButton.visibility = View.GONE
        setTimerButton.visibility = View.GONE
        viewStatsButton.visibility = View.GONE
        switchProfileButton.visibility = View.GONE
        supportButton.visibility = View.GONE
        privacyPolicyButton.visibility = View.GONE
        supportText.visibility = View.GONE
        thankYouText.visibility = View.GONE
    }

    private fun enableAllButtons() {
        pickAppsButton.visibility = View.VISIBLE
        setTimerButton.visibility = View.VISIBLE
        viewStatsButton.visibility = View.VISIBLE
        switchProfileButton.visibility = View.VISIBLE
        supportButton.visibility = View.VISIBLE
        privacyPolicyButton.visibility = View.VISIBLE
        supportText.visibility = View.VISIBLE
        thankYouText.visibility = View.VISIBLE
    }

    private fun authenticateUser() {
        val executor = ContextCompat.getMainExecutor(this)
        val biometricPrompt = BiometricPrompt(this, executor, object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                super.onAuthenticationSucceeded(result)
                Log.d("MainActivity", "Biometric authentication succeeded")
                Toast.makeText(this@MainActivity, "Welcome!", Toast.LENGTH_SHORT).show()
                enableAllButtons()
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                super.onAuthenticationError(errorCode, errString)
                Log.e("MainActivity", "Authentication error: $errString")
                Toast.makeText(this@MainActivity, "Auth failed. Please reopen app.", Toast.LENGTH_LONG).show()
                finish()
            }

            override fun onAuthenticationFailed() {
                super.onAuthenticationFailed()
                Log.d("MainActivity", "Authentication failed")
            }
        })

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Parent Authentication")
            .setSubtitle("Fingerprint Required")
            .setNegativeButtonText("Cancel")
            .build()

        biometricPrompt.authenticate(promptInfo)
    }

    private fun hasUsageAccessPermission(context: Context): Boolean {
        val usm = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val now = System.currentTimeMillis()
        val events = usm.queryEvents(now - 1000 * 10, now)
        return events.hasNextEvent()
    }

    private fun requestUsageAccess(context: Context) {
        val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }

    private fun isIgnoringBatteryOptimizations(context: Context): Boolean {
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        return powerManager.isIgnoringBatteryOptimizations(context.packageName)
    }

    private fun requestIgnoreBatteryOptimizations(context: Context) {
        val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
            data = Uri.parse("package:${context.packageName}")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }
}