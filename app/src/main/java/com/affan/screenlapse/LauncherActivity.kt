package com.affan.screenlapse

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.util.Log
import android.view.WindowManager

class LauncherActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d("LauncherActivity", "onCreate called")

        // Set transparent and lock screen attributes
        window.addFlags(
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
                    WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
        )
        setShowWhenLocked(true)
        setTurnScreenOn(true)

        // Get locked apps from intent
        val lockedApps = intent.getStringArrayListExtra("lockedApps") ?: emptyList()
        if (lockedApps.isEmpty()) {
            Log.e("LauncherActivity", "No apps to lock")
            finish()
            return
        }

        // Launch LockActivity
        val lockIntent = Intent(this, LockActivity::class.java).apply {
            putStringArrayListExtra("lockedApps", ArrayList(lockedApps))
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_ANIMATION)
        }
        startActivity(lockIntent)
        Log.d("LauncherActivity", "Launched LockActivity for apps=$lockedApps")
        finish()
    }
}