package com.affan.screenlapse

import android.content.Context
import androidx.biometric.BiometricPrompt
import androidx.fragment.app.FragmentActivity
import java.util.concurrent.Executor
import java.util.concurrent.Executors

object BiometricUtils {
    fun createPrompt(
        activity: FragmentActivity,
        title: String,
        subtitle: String,
        onSuccess: () -> Unit
    ): BiometricPrompt {
        val executor: Executor = Executors.newSingleThreadExecutor()
        return BiometricPrompt(activity, executor, object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                onSuccess()
            }
        })
    }

    fun defaultPromptInfo(context: Context): BiometricPrompt.PromptInfo {
        return BiometricPrompt.PromptInfo.Builder()
            .setTitle("Parental Authentication")
            .setSubtitle("Fingerprint required")
            .setNegativeButtonText("Cancel")
            .build()
    }
}
