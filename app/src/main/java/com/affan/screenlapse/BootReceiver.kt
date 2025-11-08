package com.affan.screenlapse

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            val prefs = context.getSharedPreferences("YourPrefsName", Context.MODE_PRIVATE)
            val lockedApps = prefs.getStringSet("lockedApps", emptySet()) ?: emptySet()

            if (lockedApps.isNotEmpty()) {
                val serviceIntent = Intent(context, LockService::class.java)
                ContextCompat.startForegroundService(context, serviceIntent)
            }
        }
    }
}