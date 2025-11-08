package com.affan.screenlapse

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.affan.screenlapse.ProfileManager
import java.text.SimpleDateFormat
import java.util.*

class ResetReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val profiles = ProfileManager.getAllProfiles(context)
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val today = dateFormat.format(Date())

        for ((kidId, name) in profiles) {
            val prefs = context.getSharedPreferences("AppSettings_$name", Context.MODE_PRIVATE)

            // Get today's tracked usage
            val trackedUsage = prefs.getString("trackedUsage", null)
            if (trackedUsage != null) {
                // Archive under usage_yyyy-MM-dd
                prefs.edit().putString("usage_$today", trackedUsage).apply()
                Log.d("ResetReceiver", "📁 Archived usage for $name on $today")
            }

            // Clear today's usage
            prefs.edit().remove("trackedUsage").apply()
            Log.d("ResetReceiver", "🧹 Reset trackedUsage for $name")
        }

        Log.d("ResetReceiver", "✅ ResetReceiver executed for ${profiles.size} profiles")
    }
}
