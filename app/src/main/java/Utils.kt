package com.affan.screenlapse

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.text.TextUtils
import android.provider.Settings
import android.util.Log
import com.affan.screenlapse.MyAccessibilityService
import com.affan.screenlapse.ProfileManager

fun Context.getProfilePrefs(): SharedPreferences {
    val kidId = ProfileManager.getActiveKidId(this)
    val profileName = ProfileManager.getKidName(this, kidId) ?: run {
        Log.w("Utils", "No kid name found for kidId=$kidId, defaulting to 'default'")
        "default"
    }
    Log.d("Utils", "Using profile: $profileName for SharedPreferences")
    return getSharedPreferences("AppSettings_$profileName", Context.MODE_PRIVATE)
}

fun isAccessibilityServiceEnabled(context: Context): Boolean {
    val expectedComponentName = ComponentName(context, MyAccessibilityService::class.java)
    val enabledServicesSetting = Settings.Secure.getString(
        context.contentResolver,
        Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
    ) ?: return false

    val colonSplitter = TextUtils.SimpleStringSplitter(':')
    colonSplitter.setString(enabledServicesSetting)
    while (colonSplitter.hasNext()) {
        val componentName = ComponentName.unflattenFromString(colonSplitter.next())
        if (componentName != null && componentName == expectedComponentName) {
            return true
        }
    }
    return false
}

fun openAccessibilitySettings(context: Context) {
    val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
    context.startActivity(intent)
}