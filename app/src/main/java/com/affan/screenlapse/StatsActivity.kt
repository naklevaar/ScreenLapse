package com.affan.screenlapse

import android.app.DatePickerDialog
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.RectF
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ShareCompat
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.formatter.ValueFormatter
import android.util.Log
import java.text.SimpleDateFormat
import java.util.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.animation.core.Easing
import androidx.core.content.ContextCompat
import com.affan.screenlapse.getProfilePrefs
import com.github.mikephil.charting.animation.ChartAnimator
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.interfaces.dataprovider.BarDataProvider
import com.github.mikephil.charting.interfaces.datasets.IBarDataSet
import com.github.mikephil.charting.renderer.BarChartRenderer
import com.github.mikephil.charting.utils.ViewPortHandler
import java.io.OutputStreamWriter

class StatsActivity : AppCompatActivity() {

    private lateinit var dateLabel: TextView
    private lateinit var selectDateButton: Button
    private lateinit var totalTimeLabel: TextView
    private lateinit var weeklyModeSwitch: Switch
    private lateinit var barChart: BarChart
    private lateinit var weeklyChart: BarChart
    private lateinit var refreshButton: Button
    private lateinit var exportButton: Button
    private lateinit var resetButton: Button
    private lateinit var viewWeeklyButton: Button
    private var isWeeklyMode = false
    private var selectedDate: String = ""

    private val createDocumentLauncher =
        registerForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
            uri?.let { documentUri ->
                try {
                    contentResolver.openOutputStream(documentUri)?.use { outputStream ->
                        OutputStreamWriter(outputStream).use { writer ->
                            val csvContent = generateCsvContent()
                            writer.write(csvContent)
                            writer.flush()
                            Log.d("StatsActivity", "CSV file saved to $documentUri")
                            Toast.makeText(this, "Stats exported successfully", Toast.LENGTH_SHORT)
                                .show()
                            shareCsvFile(documentUri)
                        }
                    }
                } catch (e: Exception) {
                    Log.e("StatsActivity", "Failed to export stats: ${e.message}", e)
                    Toast.makeText(this, "Failed to export stats: ${e.message}", Toast.LENGTH_LONG)
                        .show()
                }
            } ?: run {
                Log.w("StatsActivity", "Document creation canceled by user")
                Toast.makeText(this, "Export canceled", Toast.LENGTH_SHORT).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_stats)

        dateLabel = findViewById(R.id.date_label)
        selectDateButton = findViewById(R.id.select_date_button)
        totalTimeLabel = findViewById(R.id.total_time_label)
        weeklyModeSwitch = findViewById(R.id.weekly_mode_switch)
        barChart = findViewById(R.id.bar_chart)
        weeklyChart = findViewById(R.id.weekly_chart)
        refreshButton = findViewById(R.id.refresh_button)
        exportButton = findViewById(R.id.export_button)
        resetButton = findViewById(R.id.reset_button)
        viewWeeklyButton = findViewById(R.id.view_weekly_button)

        selectedDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        Log.d("StatsActivity", "Initialized selectedDate to: $selectedDate")

        updateDateLabel()
        loadStats()

        selectDateButton.setOnClickListener {
            showDatePicker()
        }

        weeklyModeSwitch.setOnCheckedChangeListener { _, isChecked ->
            isWeeklyMode = isChecked
            barChart.visibility = if (isWeeklyMode) BarChart.GONE else BarChart.VISIBLE
            weeklyChart.visibility = if (isWeeklyMode) BarChart.VISIBLE else BarChart.GONE
            loadStats()
        }

        refreshButton.setOnClickListener {
            loadStats()
        }

        exportButton.setOnClickListener {
            if (isWeeklyMode) {
                Toast.makeText(this, "Export not supported in weekly mode", Toast.LENGTH_SHORT)
                    .show()
                return@setOnClickListener
            }
            val fileName = "ScreenTimeStats_${selectedDate}.csv"
            createDocumentLauncher.launch(fileName)
        }

        resetButton.setOnClickListener {
            authenticateAndResetStats()
        }

        viewWeeklyButton.setOnClickListener {
            isWeeklyMode = !isWeeklyMode
            weeklyModeSwitch.isChecked = isWeeklyMode
            barChart.visibility = if (isWeeklyMode) BarChart.GONE else BarChart.VISIBLE
            weeklyChart.visibility = if (isWeeklyMode) BarChart.VISIBLE else BarChart.GONE
            loadStats()
        }
    }

    private fun authenticateAndResetStats() {
        val biometricManager = BiometricManager.from(this)
        when (biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG)) {
            BiometricManager.BIOMETRIC_SUCCESS -> {
                Log.d("StatsActivity", "Biometric authentication is available")
                showBiometricPrompt()
            }

            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> {
                Log.w("StatsActivity", "No biometric hardware available")
                Toast.makeText(
                    this,
                    "Biometric authentication not available on this device",
                    Toast.LENGTH_LONG
                ).show()
            }

            BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE -> {
                Log.w("StatsActivity", "Biometric hardware currently unavailable")
                Toast.makeText(
                    this,
                    "Biometric authentication currently unavailable",
                    Toast.LENGTH_LONG
                ).show()
            }

            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> {
                Log.w("StatsActivity", "No biometric credentials enrolled")
                Toast.makeText(
                    this,
                    "Please enroll a fingerprint in your device settings",
                    Toast.LENGTH_LONG
                ).show()
            }

            else -> {
                Log.w("StatsActivity", "Biometric authentication not supported")
                Toast.makeText(this, "Biometric authentication not supported", Toast.LENGTH_LONG)
                    .show()
            }
        }
    }

    private fun showBiometricPrompt() {
        val executor = ContextCompat.getMainExecutor(this)
        val biometricPrompt =
            BiometricPrompt(this, executor, object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    Log.w("StatsActivity", "Authentication error: $errString (code: $errorCode)")
                    Toast.makeText(
                        this@StatsActivity,
                        "Authentication failed: $errString",
                        Toast.LENGTH_LONG
                    ).show()
                }

                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    Log.d("StatsActivity", "Authentication succeeded")
                    resetStats()
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    Log.w("StatsActivity", "Authentication failed")
                    Toast.makeText(
                        this@StatsActivity,
                        "Authentication failed. Please try again.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            })

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Parent Authentication")
            .setSubtitle("Please authenticate to reset stats")
            .setDescription("Only parents can reset the stats for the current day.")
            .setNegativeButtonText("Cancel")
            .build()

        biometricPrompt.authenticate(promptInfo)
    }

    private fun resetStats() {
        val profile = ProfileManager.getActiveKidId(this).let { kidId ->
            ProfileManager.getKidName(this, kidId) ?: "default"
        }
        val prefs = getProfilePrefs()
        val editor = prefs.edit()
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val key = "trackedUsage_$today"

        // Clear the stats for the current day only
        editor.remove(key)
        editor.apply()
        Log.d("StatsActivity", "Cleared stats for $today")

        loadStats()
        Toast.makeText(this, "Stats for $today have been reset", Toast.LENGTH_SHORT).show()
    }

    private fun generateCsvContent(): String {
        val prefs = getProfilePrefs()
        val key = "trackedUsage_$selectedDate"
        val rawData = prefs.getString(key, "") ?: ""

        val appUsagesMap = mutableMapOf<String, Long>()
        if (rawData.isNotEmpty()) {
            rawData.split(",").forEach { entry ->
                val parts = entry.split(":")
                if (parts.size == 2) {
                    val packageName = parts[0]
                    val usageTimeMillis = parts[1].toLongOrNull() ?: 0L
                    appUsagesMap[packageName] = (appUsagesMap[packageName] ?: 0L) + usageTimeMillis
                }
            }
        }

        val appUsages = appUsagesMap.map { (packageName, usageTimeMillis) ->
            AppUsageInfo(packageName, usageTimeMillis)
        }.sortedByDescending { it.usageTimeMillis }

        val csvBuilder = StringBuilder()
        csvBuilder.append("Date,App Name,Usage Time (mins)\n")
        appUsages.forEach { usage ->
            val appName = usage.packageName.split(".").last()
            val minutes = usage.usageTimeMillis / 1000.0 / 60.0
            csvBuilder.append("$selectedDate,$appName,${String.format("%.2f", minutes)}\n")
        }
        return csvBuilder.toString()
    }

    private fun shareCsvFile(uri: android.net.Uri) {
        val shareIntent = ShareCompat.IntentBuilder(this)
            .setType("text/csv")
            .setStream(uri)
            .setSubject("Screen Time Stats for $selectedDate")
            .setText("Here are the screen time stats for $selectedDate")
            .intent
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)

        startActivity(Intent.createChooser(shareIntent, "Share Screen Time Stats"))
    }

    private fun showDatePicker() {
        val calendar = Calendar.getInstance()
        val currentDate =
            SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(selectedDate) ?: Date()
        calendar.time = currentDate

        DatePickerDialog(
            this,
            { _, year, month, dayOfMonth ->
                calendar.set(year, month, dayOfMonth)
                selectedDate =
                    SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(calendar.time)
                Log.d("StatsActivity", "Date picker selected: $selectedDate")
                updateDateLabel()
                loadStats()
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun updateDateLabel() {
        val displayFormat = SimpleDateFormat("MMMM d", Locale.getDefault())
        val date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(selectedDate) ?: Date()
        dateLabel.text = "Stats for: ${displayFormat.format(date)}"
        Log.d("StatsActivity", "Updated date label to: ${dateLabel.text}")
    }

    private fun loadStats() {
        val profile = ProfileManager.getActiveKidId(this).let { kidId ->
            ProfileManager.getKidName(this, kidId) ?: "default"
        }
        Log.d("StatsActivity", "Loading data for profile: $profile")

        val prefs = getProfilePrefs()
        val allKeys = prefs.all.keys
        Log.d("StatsActivity", "SharedPreferences keys: $allKeys")

        if (isWeeklyMode) {
            loadWeeklyStats()
            return
        }

        val key = "trackedUsage_$selectedDate"
        Log.d("StatsActivity", "🔍 Loading today's stats from key: $key")
        val rawData = prefs.getString(key, "") ?: ""
        Log.d("StatsActivity", "Raw data for $key: '$rawData'")

        val appUsagesMap = mutableMapOf<String, Long>()
        if (rawData.isNotEmpty()) {
            rawData.split(",").filter { it.isNotBlank() }.forEach { entry ->
                Log.d("StatsActivity", "Parsing entry: '$entry'")
                val parts = entry.split(":")
                Log.d("StatsActivity", "Split parts: ${parts.joinToString(", ")}")
                if (parts.size == 2) {
                    val packageName = parts[0].trim()
                    val usageTimeMillis = parts[1].toLongOrNull() ?: 0L
                    if (packageName.isNotEmpty() && usageTimeMillis > 0) {
                        appUsagesMap[packageName] =
                            (appUsagesMap[packageName] ?: 0L) + usageTimeMillis
                        Log.d(
                            "StatsActivity",
                            "Updated app: $packageName with total usage: ${appUsagesMap[packageName]} ms"
                        )
                    } else {
                        Log.w(
                            "StatsActivity",
                            "Skipping invalid data: packageName='$packageName', usageTimeMillis=$usageTimeMillis"
                        )
                    }
                } else {
                    Log.w(
                        "StatsActivity",
                        "Skipping invalid entry: '$entry' (expected 2 parts, got ${parts.size})"
                    )
                }
            }
        }

        val appUsages = appUsagesMap.map { (packageName, usageTimeMillis) ->
            AppUsageInfo(packageName, usageTimeMillis)
        }.sortedByDescending { it.usageTimeMillis }
        Log.d(
            "StatsActivity",
            "📦 Found ${appUsages.size} apps: ${appUsages.map { "${it.packageName}:${it.usageTimeMillis}" }}"
        )

        val totalMinutes = appUsages.sumOf { it.usageTimeMillis } / 1000.0 / 60.0
        totalTimeLabel.text = "Total Screen Time: ${String.format("%.2f", totalMinutes)} mins"
        Log.d("StatsActivity", "Total screen time: $totalMinutes mins")

        val entries = appUsages.mapIndexed { index, usage ->
            val minutes = (usage.usageTimeMillis / 1000.0 / 60.0).toFloat()
            BarEntry(index.toFloat(), minutes)
        }

        val labels = appUsages.map { it.packageName.split(".").last() }.distinct()

        val dataSet = BarDataSet(entries, "Usage (mins)").apply {
            // Gradient pastel shades like your chart
            colors = listOf(
                Color.parseColor("#FF8A80"), // red pastel
                Color.parseColor("#4DD0E1"), // teal pastel
                Color.parseColor("#4FC3F7")  // sky pastel
            )

            valueTextSize = 12f
            valueTextColor = Color.DKGRAY
            valueFormatter = object : ValueFormatter() {
                override fun getFormattedValue(value: Float): String {
                    return String.format("%.0f", value)
                }
            }
            setDrawValues(true)
        }

        val barData = BarData(dataSet).apply {
            barWidth = 0.6f
        }

        barChart.apply {
            data = barData
            description.isEnabled = false
            setDrawGridBackground(false)
            setDrawBarShadow(false)
            setFitBars(true)

            // Rounded bar tip workaround (shadow color + bar shadow false = clean look)
            renderer = RoundedBarChartRenderer(this, animator, viewPortHandler)

            xAxis.apply {
                valueFormatter = IndexAxisValueFormatter(labels)
                position = XAxis.XAxisPosition.BOTTOM
                granularity = 1f
                isGranularityEnabled = true
                setDrawGridLines(false)
                textColor = Color.DKGRAY
                textSize = 12f
                labelRotationAngle = 0f
            }

            axisLeft.apply {
                axisMinimum = 0f
                textColor = Color.DKGRAY
                textSize = 12f
                setDrawGridLines(true)
                gridColor = Color.LTGRAY
            }

            axisRight.isEnabled = false
            legend.isEnabled = false
            animateY(1000)
            invalidate()
        }

        Log.d(
            "StatsActivity",
            "📊 Preparing chart with ${appUsages.size} apps and ${labels.size} labels"
        )
    }

    private fun loadWeeklyStats() {
        val profile = ProfileManager.getActiveKidId(this).let { kidId ->
            ProfileManager.getKidName(this, kidId) ?: "default"
        }
        val prefs = getProfilePrefs()

        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val calendar = Calendar.getInstance()
        calendar.time = dateFormat.parse(selectedDate) ?: Date()

        // Adjust to the start of the week (Monday)
        while (calendar.get(Calendar.DAY_OF_WEEK) != Calendar.MONDAY) {
            calendar.add(Calendar.DAY_OF_YEAR, -1)
        }

        // Log the start of the week
        val weekStartDate = dateFormat.format(calendar.time)
        Log.d("StatsActivity", "Week starts on: $weekStartDate")

        val daysOfWeek = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
        val entries = mutableListOf<BarEntry>()
        var weeklyTotalMinutes = 0f

        // Iterate through the 7 days of the current week (Mon to Sun)
        for (i in 0 until 7) {
            val date = dateFormat.format(calendar.time)
            val rawData = prefs.getString("trackedUsage_$date", "") ?: ""
            Log.d("StatsActivity", "Weekly stats for $date: Raw data: '$rawData'")

            var totalMinutes = 0f
            if (rawData.isNotEmpty()) {
                rawData.split(",").forEach { entry ->
                    val parts = entry.split(":")
                    if (parts.size == 2) {
                        val usageTimeMillis = parts[1].toLongOrNull() ?: 0L
                        totalMinutes += (usageTimeMillis / 1000.0 / 60.0).toFloat()
                    }
                }
            }

            weeklyTotalMinutes += totalMinutes
            entries.add(BarEntry(i.toFloat(), totalMinutes))
            Log.d("StatsActivity", "Weekly stats for $date (${daysOfWeek[i]}): $totalMinutes mins")

            calendar.add(Calendar.DAY_OF_YEAR, 1)
        }

        // Update total screen time to reflect the weekly total
        totalTimeLabel.text = "Total Screen Time: ${String.format("%.2f", weeklyTotalMinutes)} mins"
        Log.d("StatsActivity", "Weekly total screen time: $weeklyTotalMinutes mins")

        val dataSet = BarDataSet(entries, "Usage (mins)")
        dataSet.color = android.graphics.Color.parseColor("#4DD0E1")
        val barData = BarData(dataSet)
        barData.barWidth = 0.9f

        weeklyChart.apply {
            data = barData
            description.isEnabled = false
            setFitBars(true)
            xAxis.valueFormatter = IndexAxisValueFormatter(daysOfWeek)
            xAxis.setDrawGridLines(false)
            xAxis.setAvoidFirstLastClipping(true)
            xAxis.labelCount = daysOfWeek.size
            xAxis.granularity = 1f
            xAxis.isGranularityEnabled = true
            xAxis.axisMinimum = -0.5f
            xAxis.axisMaximum = daysOfWeek.size - 0.5f
            axisLeft.setDrawGridLines(true)
            axisRight.isEnabled = false

            val maxMinutes = if (entries.any { it.y > 0f }) {
                (entries.maxOf { it.y } + 0.5f)
            } else {
                1f
            }
            axisLeft.axisMinimum = 0f
            axisLeft.axisMaximum = maxMinutes
            axisLeft.labelCount = 5

            animateY(1000)
            invalidate()
        }
        Log.d("StatsActivity", "📊 Preparing weekly chart with ${entries.size} days")
    }

    class RoundedBarChartRenderer(
        chart: BarDataProvider,
        animator: ChartAnimator,
        viewPortHandler: ViewPortHandler
    ) : BarChartRenderer(chart, animator, viewPortHandler) {

        override fun drawDataSet(c: Canvas, dataSet: IBarDataSet, index: Int) {
            val buffer = mBarBuffers[index]
            val paint = mRenderPaint

            for (j in buffer.buffer.indices step 4) {
                val left = buffer.buffer[j]
                val top = buffer.buffer[j + 1]
                val right = buffer.buffer[j + 2]
                val bottom = buffer.buffer[j + 3]

                val radius = 20f // bar corner radius
                paint.color = dataSet.getColor(j / 4)
                val rectF = RectF(left, top, right, bottom)
                c.drawRoundRect(rectF, radius, radius, paint)
            }
        }
    }
}