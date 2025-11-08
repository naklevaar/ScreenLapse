package com.affan.screenlapse

import android.app.*
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.*
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleService
import com.affan.screenlapse.getProfilePrefs
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean

class LockService : LifecycleService() {
    private lateinit var selectedApps: Set<String>
    private var timeLimit: Int = 0
    private val activeTime = mutableMapOf<String, Long>()
    private var lastForegroundApp: String? = null
    private var lastTimestamp: Long = 0
    private lateinit var usageStatsManager: UsageStatsManager
    private lateinit var sharedPreferences: android.content.SharedPreferences

    private lateinit var handlerThread: HandlerThread
    private lateinit var handler: Handler
    private val isRunning = AtomicBoolean(false)
    private val isLockActive = AtomicBoolean(false)
    private var hasStoppedTracking = false
    private var currentForegroundApp: String? = null

    private val foregroundAppReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val packageName = intent?.getStringExtra("packageName")
            if (packageName != null) {
                Log.d("LockService", "📢 Received foreground app: $packageName")
                currentForegroundApp = packageName
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        Log.d("LockService", "Creating LockService")
        try {
            startForeground(1, createNotification("Tracking screen time…"))
            Log.d("LockService", "Successfully started foreground service")
        } catch (e: SecurityException) {
            Log.e("LockService", "Failed to start foreground service: ${e.message}")
            stopSelf()
            return
        } catch (e: Exception) {
            Log.e("LockService", "Unexpected error starting foreground service: ${e.message}")
            stopSelf()
            return
        }

        usageStatsManager = getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        sharedPreferences = applicationContext.getProfilePrefs()
        handlerThread = HandlerThread("LockTrackingThread", Process.THREAD_PRIORITY_BACKGROUND)
        handlerThread.start()
        handler = Handler(handlerThread.looper)

        // Register the BroadcastReceiver to listen for foreground app changes
        val filter = IntentFilter("com.example.screentimecontroller.FOREGROUND_APP_CHANGED")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            // Android 14 (API 34) and above: Specify RECEIVER_NOT_EXPORTED
            registerReceiver(
                foregroundAppReceiver,
                filter,
                ContextCompat.RECEIVER_NOT_EXPORTED
            )
        } else {
            // Below Android 14: Use the older registerReceiver method
            registerReceiver(foregroundAppReceiver, filter)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        sharedPreferences = applicationContext.getProfilePrefs()
        val profile = ProfileManager.getKidName(
            applicationContext,
            ProfileManager.getActiveKidId(applicationContext)
        ) ?: "default"
        Log.d("LockService", "Saving data for profile: $profile")

        if (intent?.action == "STOP_SERVICE") {
            stopTracking()
            return START_NOT_STICKY
        }

// Clear lockedApps to reset the lock state for a new timer session
        sharedPreferences.edit().putStringSet("lockedApps", emptySet()).apply()
        isLockActive.set(false) // Reset lock state
        Log.d("LockService", "🔓 Reset lock state for new timer session")

        selectedApps = sharedPreferences.getStringSet("selectedApps", emptySet()) ?: emptySet()
        timeLimit = sharedPreferences.getInt("timeLimit", 1)
        Log.d("LockService", "🔍 Selected apps: $selectedApps, Time limit: $timeLimit mins")

        if (selectedApps.isEmpty() || timeLimit <= 0) {
            Log.e("LockService", "❌ Invalid config: selectedApps=$selectedApps, timeLimit=$timeLimit")
            stopSelf()
            return START_NOT_STICKY
        }

        if (isRunning.compareAndSet(false, true)) {
            Log.d("LockService", "✅ Tracking started for $selectedApps ($timeLimit mins)")
            lastTimestamp = System.currentTimeMillis()
            handler.post(trackRunnable)
        }

        // Start lock monitoring if apps are already locked (shouldn't happen after reset)
        val lockedApps = sharedPreferences.getStringSet("lockedApps", emptySet()) ?: emptySet()
        if (lockedApps.isNotEmpty() && !isLockActive.get()) {
            Log.d("LockService", "🔒 Detected locked apps on start, beginning lock monitoring")
            isLockActive.set(true)
            updateNotification("Apps Locked")
            handler.post(lockRunnable)
        }

        return START_STICKY
    }

    private val trackRunnable: Runnable = object : Runnable {
        override fun run() {
            if (!isRunning.get() || isLockActive.get()) return

            val now = System.currentTimeMillis()
            val delta = now - lastTimestamp
            lastTimestamp = now

            // Use the foreground app from the Accessibility Service, fall back to UsageStatsManager
            val foregroundApp = currentForegroundApp ?: getForegroundApp()
            val activeApp = foregroundApp ?: lastForegroundApp
            Log.d("LockService", "🔎 Current foreground app: $foregroundApp, Active app: $activeApp")

            if (activeApp != null && activeApp in selectedApps) {
                activeTime[activeApp] = (activeTime[activeApp] ?: 0L) + delta
                Log.d("LockService", "📊 Tracked $activeApp: +${delta}ms, total=${activeTime[activeApp]}")
                lastForegroundApp = activeApp
            }

            val totalUsed = activeTime.values.sum()
            val totalUsedMin = totalUsed / 1000.0 / 60.0
            Log.d("LockService", "⏳ Total used time: %.2f mins".format(totalUsedMin))

            if (totalUsedMin >= timeLimit) {
                val alreadyLocked = sharedPreferences.getStringSet("lockedApps", emptySet()) ?: emptySet()
                if (alreadyLocked != selectedApps) {
                    Log.d("LockService", "🔒 Time limit hit — saving locked apps")
                    sharedPreferences.edit().putStringSet("lockedApps", selectedApps).apply()
                }
                // Stop tracking time but keep the service running to enforce the lock
                isRunning.set(false)
                isLockActive.set(true)
                updateNotification("Apps Locked")
                handler.removeCallbacks(trackRunnable)
                handler.post(lockRunnable)

                val lockIntent = Intent(applicationContext, LockActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                }
                applicationContext.startActivity(lockIntent)
                return
            }

            handler.postDelayed(this, 500)
        }
    }

    private val lockRunnable: Runnable = object : Runnable {
        override fun run() {
            if (!isLockActive.get()) return

            val lockedApps = sharedPreferences.getStringSet("lockedApps", emptySet()) ?: emptySet()
            if (lockedApps.isEmpty()) {
                // Lock has been cleared, stop the service
                Log.d("LockService", "🔓 Lock cleared, stopping service")
                stopTracking()
                return
            }

            val foregroundApp = currentForegroundApp ?: getForegroundApp()
            if (foregroundApp != null && foregroundApp in lockedApps) {
                Log.d("LockService", "🔒 Detected locked app in foreground: $foregroundApp, re-enforcing lock")
                val lockIntent = Intent(applicationContext, LockActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                }
                applicationContext.startActivity(lockIntent)
            }

            handler.postDelayed(this, 1000) // Check every second
        }
    }

    private fun getForegroundApp(): String? {
        val endTime = System.currentTimeMillis()
        val beginTime = endTime - 10000 // Increased window to 10 seconds
        val events = usageStatsManager.queryEvents(beginTime, endTime)
        val event = UsageEvents.Event()
        var lastApp: String? = null

        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            if (event.eventType == UsageEvents.Event.MOVE_TO_FOREGROUND ||
                event.eventType == UsageEvents.Event.ACTIVITY_RESUMED) {
                lastApp = event.packageName
            }
        }
        Log.d("LockService", "🔎 Fallback getForegroundApp result: $lastApp")
        return lastApp
    }

    private fun stopTracking() {
        if (hasStoppedTracking) return
        hasStoppedTracking = true

        isRunning.set(false)
        isLockActive.set(false)
        handler.removeCallbacksAndMessages(null)

        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val key = "trackedUsage_$today"

        val previous = sharedPreferences.getString(key, "") ?: ""
        val previousMap = previous.split(",")
            .filter { it.contains(":") }
            .associate {
                val (pkg, time) = it.split(":")
                pkg to time.toLong()
            }.toMutableMap()

        for ((pkg, time) in activeTime) {
            previousMap[pkg] = (previousMap[pkg] ?: 0L) + time
        }

        if (previousMap.isNotEmpty()) {
            val usageString = previousMap.entries.joinToString(",") { "${it.key}:${it.value}" }
            Log.d("LockService", "📊 Saving merged usage for $today: $usageString")
            sharedPreferences.edit().putString(key, usageString).apply()
        }

        stopForeground(true)
        stopSelf()
    }

    private fun createNotification(message: String): Notification {
        val channelId = "LockServiceChannel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "Lock Service", NotificationManager.IMPORTANCE_LOW)
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
        return NotificationCompat.Builder(this, channelId)
            .setContentTitle("ScreenLapse")
            .setContentText(message)
            .setSmallIcon(android.R.drawable.ic_lock_lock)
            .build()
    }

    private fun updateNotification(message: String) {
        val notification = createNotification(message)
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(1, notification)
    }

    override fun onDestroy() {
        unregisterReceiver(foregroundAppReceiver)
        stopTracking()
        handlerThread.quitSafely()
        super.onDestroy()
    }
}