package com.affan.screenlapse

import android.content.*
import android.os.*
import android.util.Log
import android.view.*
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import com.affan.screenlapse.getProfilePrefs

class LockActivity : AppCompatActivity() {

    private lateinit var biometricPrompt: BiometricPrompt
    private lateinit var promptInfo: BiometricPrompt.PromptInfo

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Fullscreen black lock screen with additional flags to prevent bypass
        window.addFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN or
                    WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
                    WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
        )

        // Make the activity a system overlay to prevent Home button bypass
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            window.setType(WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY)
        } else {
            window.setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT)
        }

        setContentView(R.layout.activity_lock)

        findViewById<TextView>(R.id.lockMessage).text =
            "⏰ Time up!\nAsk your parent to unlock."

        setupBiometricPrompt()
    }

    private fun setupBiometricPrompt() {
        val executor = ContextCompat.getMainExecutor(this)

        biometricPrompt = BiometricPrompt(this, executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    Log.d("LockActivity", "✅ Fingerprint authenticated")

                    val prefs = applicationContext.getProfilePrefs()
                    prefs.edit().putStringSet("lockedApps", emptySet()).apply()

                    val stopIntent = Intent(this@LockActivity, LockService::class.java)
                    stopIntent.action = "STOP_SERVICE"
                    startService(stopIntent)

                    finishAndRemoveTask()
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    Log.w("LockActivity", "Biometric error: $errString")
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    Log.d("LockActivity", "⚠️ Fingerprint not recognized")
                }
            })

        promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Parent Unlock")
            .setSubtitle("Use fingerprint to unlock")
            .setNegativeButtonText("Cancel")
            .build()
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        if (event?.action == MotionEvent.ACTION_DOWN) {
            biometricPrompt.authenticate(promptInfo)
        }
        return true
    }

    // Block back, recent, and home buttons
    override fun onBackPressed() {
        // Do nothing to prevent back button bypass
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_HOME) {
            // Log the attempt but don't allow bypass
            Log.d("LockActivity", "Home button press detected, blocking bypass")
            return true
        }
        return true
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent?): Boolean {
        return true
    }

    override fun onStop() {
        super.onStop()
        // Removed re-launch logic to prevent instant black screen reappearance
    }

    override fun onDestroy() {
        super.onDestroy()
        // Removed re-launch logic to prevent instant black screen reappearance
    }
}