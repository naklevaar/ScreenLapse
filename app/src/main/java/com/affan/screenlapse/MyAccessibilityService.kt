package com.affan.screenlapse

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Intent
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import com.affan.screenlapse.getProfilePrefs

class MyAccessibilityService : AccessibilityService() {

    // MyAccessibilityService.kt
    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null || event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return

        val packageName = event.packageName?.toString() ?: return
        Log.d("MyAccessibilityService", "🪟 Window Changed: $packageName")

        val prefs = applicationContext.getProfilePrefs()
        val lockedApps = prefs.getStringSet("lockedApps", emptySet())?.toMutableSet() ?: mutableSetOf()

        if (packageName in lockedApps) {
            Log.d("MyAccessibilityService", "🔒 Blocking $packageName")
            val intent = Intent(this, LockActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            }
            startActivity(intent)
        }
    }

    override fun onInterrupt() {
        Log.w("MyAccessibilityService", "⚠️ Service interrupted")
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        Log.d("MyAccessibilityService", "✅ Accessibility Service connected")

        serviceInfo = serviceInfo.apply {
            eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
            packageNames = null // Listen to all apps
            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            flags = AccessibilityServiceInfo.FLAG_INCLUDE_NOT_IMPORTANT_VIEWS
        }
    }
}

