package com.affan.screenlapse


import com.affan.screenlapse.isAccessibilityServiceEnabled
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
import android.os.*
import android.provider.Settings
import android.widget.Button
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import android.Manifest
import android.content.pm.PackageManager

class TimerActivity : AppCompatActivity() {
    private lateinit var seekBar: SeekBar
    private lateinit var selectedTimeText: TextView
    private lateinit var startButton: Button
    private var selectedTime = 30 // default 30 mins
    private var waitingForAccessibility = false
    private var waitingForPermissions = false
    private var isVivoDevice = false

    companion object {
        private const val REQUEST_POST_NOTIFICATIONS = 1001
        private const val REQUEST_IGNORE_BATTERY_OPTIMIZATIONS = 1002
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_timer)

        seekBar = findViewById(R.id.time_seekbar)
        selectedTimeText = findViewById(R.id.selected_time_text)
        startButton = findViewById(R.id.start_button)

        seekBar.progress = selectedTime
        updateTimeText(selectedTime)

        // Check if the device is a Vivo phone
        isVivoDevice = Build.MANUFACTURER.equals("vivo", ignoreCase = true)
        Log.d("TimerActivity", "Is Vivo device: $isVivoDevice")

        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                selectedTime = if (progress < 1) 1 else progress
                updateTimeText(selectedTime)
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        startButton.setOnClickListener {
            startButton.isEnabled = false
            startButton.animate()
                .scaleX(0.95f)
                .scaleY(0.95f)
                .setDuration(150)
                .withEndAction {
                    startButton.animate().scaleX(1f).scaleY(1f).setDuration(150).start()
                    checkPermissionsAndProceed(selectedTime)
                }
                .start()
        }
    }

    private fun updateTimeText(minutes: Int) {
        selectedTimeText.text = "Selected: $minutes min${if (minutes > 1) "s" else ""}"
    }

    private fun checkPermissionsAndProceed(time: Int) {
        // Check for POST_NOTIFICATIONS permission on Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                Log.d("TimerActivity", "Requesting POST_NOTIFICATIONS permission")
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    REQUEST_POST_NOTIFICATIONS
                )
                waitingForPermissions = true
                return
            }
        }

        // Check for Vivo-specific settings
        if (isVivoDevice) {
            val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
            if (!powerManager.isIgnoringBatteryOptimizations(packageName)) {
                Log.d("TimerActivity", "Requesting to ignore battery optimizations on Vivo device")
                showVivoSettingsDialog()
                return
            }
        }

        // Proceed to save time and start service
        saveTimeAndStartService(time)
    }

    private fun showVivoSettingsDialog() {
        AlertDialog.Builder(this)
            .setTitle("Enable Background Activity on Vivo")
            .setMessage(
                "To ensure ScreenLapse works properly on your Vivo device, please:\n" +
                        "1. Enable 'Auto-Start': Go to Settings > Apps > ScreenLapse > Auto-Start.\n" +
                        "2. Disable Battery Optimization: Allow ScreenLapse to run in the background."
            )
            .setPositiveButton("Go to Settings") { _, _ ->
                try {
                    // Request to ignore battery optimizations
                    val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
                    intent.data = android.net.Uri.parse("package:$packageName")
                    startActivityForResult(intent, REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
                } catch (e: Exception) {
                    Log.e("TimerActivity", "Error opening battery optimization settings: ${e.message}")
                    Toast.makeText(this, "Please manually disable battery optimization for ScreenLapse", Toast.LENGTH_LONG).show()
                    startButton.isEnabled = true
                }
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
                startButton.isEnabled = true
                waitingForPermissions = false
            }
            .setCancelable(false)
            .create()
            .show()
    }

    private fun showAccessibilityDisclosureDialog() {
        val dialogBuilder = AlertDialog.Builder(this)
            .setTitle("Allow Accessibility Permission")
            .setMessage(
                "ScreenLapse uses the Accessibility Service to detect which apps are in use.\n" +
                        "This is required to apply black screen locks after timers expire.\n\n" +
                        "We do not collect, store, or share any personal data. The permission is used only to support core app features."
            )
            .setPositiveButton("Enable") { _, _ ->
                Log.d("TimerActivity", "User agreed to enable Accessibility Service")
                Toast.makeText(this, "Please enable Accessibility for proper tracking", Toast.LENGTH_LONG).show()
                openAccessibilitySettings(this)
                waitingForAccessibility = true
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                Log.d("TimerActivity", "User canceled Accessibility permission")
                dialog.dismiss()
                startButton.isEnabled = true
                waitingForAccessibility = false
            }
            .setCancelable(false)

        val dialog = dialogBuilder.create()
        dialog.show()

        val messageTextView = dialog.findViewById<TextView>(android.R.id.message)
        messageTextView?.setTextIsSelectable(true)
    }

    private fun saveTimeAndStartService(time: Int) {
        Log.d("TimerActivity", "🚀 Starting timer setup for $time mins")

        val sharedPreferences = applicationContext.getProfilePrefs()
        val selectedApps = sharedPreferences.getStringSet("selectedApps", null)

        if (selectedApps.isNullOrEmpty()) {
            Log.e("TimerActivity", "❌ No apps selected")
            Toast.makeText(this, "Please select at least one app first", Toast.LENGTH_SHORT).show()
            startButton.isEnabled = true
            return
        }

        if (!isAccessibilityServiceEnabled(this)) {
            Log.d("TimerActivity", "❌ Accessibility Service not enabled, showing disclosure dialog")
            showAccessibilityDisclosureDialog()
            return
        }

        val usageStatsManager = getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val stats = usageStatsManager.queryUsageStats(
        UsageStatsManager.INTERVAL_DAILY,
        System.currentTimeMillis() - 1000 * 3600,
        System.currentTimeMillis()
        )
        if (stats.isEmpty()) {
            Log.d("TimerActivity", "❌ Usage Access not granted")
            Toast.makeText(this, "Please enable Usage Access in Settings", Toast.LENGTH_LONG).show()
            startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
            startButton.isEnabled = true
            return
        }

        val editor = sharedPreferences.edit()
        editor.putInt("timeLimit", time)
        editor.putLong("startTime", System.currentTimeMillis())
        editor.putBoolean("timeExpired", false)
        editor.apply()

        startTimerFlow()
    }

    private fun startTimerFlow() {
        Handler(Looper.getMainLooper()).postDelayed({
            Log.d("TimerActivity", "✅ Starting LockService for $selectedTime mins")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val vibe = getSystemService(VIBRATOR_SERVICE) as Vibrator
                vibe.vibrate(VibrationEffect.createOneShot(1000, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                (getSystemService(VIBRATOR_SERVICE) as Vibrator).vibrate(1000)
            }

            MediaPlayer.create(this, R.raw.swoosh).start()
            Toast.makeText(this, "Timer set for $selectedTime mins", Toast.LENGTH_SHORT).show()

            try {
                val intent = Intent(this, LockService::class.java)
                startForegroundService(intent)
                finish()
            } catch (e: SecurityException) {
                Log.e("TimerActivity", "Failed to start LockService: ${e.message}")
                Toast.makeText(this, "Failed to start timer service. Please ensure all permissions are granted.", Toast.LENGTH_LONG).show()
                startButton.isEnabled = true
            } catch (e: Exception) {
                Log.e("TimerActivity", "Unexpected error starting LockService: ${e.message}")
                Toast.makeText(this, "An error occurred. Please try again.", Toast.LENGTH_LONG).show()
                startButton.isEnabled = true
            }
        }, 100)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_POST_NOTIFICATIONS) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.d("TimerActivity", "POST_NOTIFICATIONS permission granted")
                checkPermissionsAndProceed(selectedTime)
            } else {
                Log.d("TimerActivity", "POST_NOTIFICATIONS permission denied")
                Toast.makeText(this, "Notification permission is required to show timer status", Toast.LENGTH_LONG).show()
                startButton.isEnabled = true
                waitingForPermissions = false
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_IGNORE_BATTERY_OPTIMIZATIONS) {
            val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
            if (powerManager.isIgnoringBatteryOptimizations(packageName)) {
                Log.d("TimerActivity", "Battery optimization disabled, proceeding")
                checkPermissionsAndProceed(selectedTime)
            } else {
                Log.d("TimerActivity", "Battery optimization still enabled")
                Toast.makeText(this, "Please disable battery optimization and enable Auto-Start to ensure ScreenLapse works properly", Toast.LENGTH_LONG).show()
                startButton.isEnabled = true
                waitingForPermissions = false
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (waitingForAccessibility) {
            if (isAccessibilityServiceEnabled(this)) {
                Log.d("TimerActivity", "✅ Accessibility enabled after return, continuing flow")
                saveTimeAndStartService(selectedTime)
                waitingForAccessibility = false
            } else {
                Log.d("TimerActivity", "❌ Accessibility still not enabled")
                startButton.isEnabled = true
                waitingForAccessibility = false
            }
        }
    }
}