package com.affan.screenlapse

import android.content.Context
import android.content.SharedPreferences
import android.util.Log

object ProfileManager {
    private const val PREFS_NAME = "KidProfiles"
    private const val KEY_KID1_NAME = "kid1_name"
    private const val KEY_KID2_NAME = "kid2_name"
    private const val KEY_ACTIVE_KID = "active_kid_id"

    fun getSharedPreferences(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun saveKidName(context: Context, kidId: Int, name: String) {
        if (name.isBlank()) {
            Log.w("ProfileManager", "Attempted to save empty name for kidId=$kidId")
            return
        }
        if (kidId !in 1..2) {
            Log.w("ProfileManager", "Invalid kidId=$kidId")
            return
        }
        val editor = getSharedPreferences(context).edit()
        val key = if (kidId == 1) KEY_KID1_NAME else KEY_KID2_NAME
        editor.putString(key, name)
        Log.d("ProfileManager", "Saving kid${kidId}_name: $name")
        val success = editor.commit()
        Log.d("ProfileManager", "Save kid name (kidId=$kidId, name=$name) successful: $success")
        if (!success) {
            Log.e("ProfileManager", "Failed to save kid name for kidId=$kidId")
        }
        debugSharedPreferences(context)
    }

    fun getKidName(context: Context, kidId: Int): String? {
        if (kidId !in 1..2) {
            Log.w("ProfileManager", "Invalid kidId=$kidId for getKidName")
            return null
        }
        val prefs = getSharedPreferences(context)
        val key = if (kidId == 1) KEY_KID1_NAME else KEY_KID2_NAME
        val name = prefs.getString(key, null)
        Log.d("ProfileManager", "Retrieved kid name for kidId=$kidId: $name")
        return name
    }

    fun setActiveKid(context: Context, kidId: Int) {
        if (kidId !in 1..2) {
            Log.w("ProfileManager", "Invalid kidId=$kidId for setActiveKid")
            return
        }
        val editor = getSharedPreferences(context).edit()
        editor.putInt(KEY_ACTIVE_KID, kidId)
        val success = editor.commit()
        Log.d("ProfileManager", "Set active kidId=$kidId, successful: $success")
        if (!success) {
            Log.e("ProfileManager", "Failed to set active kidId=$kidId")
        }
        debugSharedPreferences(context)
    }

    fun getActiveKidId(context: Context): Int {
        val kidId = getSharedPreferences(context).getInt(KEY_ACTIVE_KID, 1)
        Log.d("ProfileManager", "Retrieved active kidId: $kidId")
        return kidId
    }

    fun getAllProfiles(context: Context): List<Pair<Int, String>> {
        val list = mutableListOf<Pair<Int, String>>()
        getKidName(context, 1)?.let { list.add(1 to it) }
        getKidName(context, 2)?.let { list.add(2 to it) }
        Log.d("ProfileManager", "All profiles: $list")
        return list
    }

    fun initializeDefaultProfile(context: Context) {
        val prefs = getSharedPreferences(context)
        val existingProfiles = getAllProfiles(context)
        if (existingProfiles.isEmpty()) {
            Log.d("ProfileManager", "No profiles found, initializing default profile: KID1")
            saveKidName(context, 1, "KID1")
            setActiveKid(context, 1)
        } else {
            Log.d("ProfileManager", "Profiles already exist: $existingProfiles")
            if (existingProfiles.none { it.first == getActiveKidId(context) }) {
                Log.d("ProfileManager", "Active kidId not found in profiles, setting to first profile")
                setActiveKid(context, existingProfiles.first().first)
            }
        }
        debugSharedPreferences(context)
    }

    fun debugSharedPreferences(context: Context) {
        val prefs = getSharedPreferences(context)
        val allEntries = prefs.all
        Log.d("ProfileManager", "KidProfiles SharedPreferences content: $allEntries")
    }
}