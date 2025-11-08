package com.affan.screenlapse

import android.content.Context
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import java.text.SimpleDateFormat
import java.util.*

class BarChartActivity : AppCompatActivity() {

    private lateinit var barChart: BarChart

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_bar_chart)

        barChart = findViewById(R.id.bar_chart)

        val profileName = intent.getStringExtra("profile") ?: return
        val weeklyData = getLast7DaysUsage(profileName)

        val entries = weeklyData.mapIndexed { index, usage ->
            BarEntry(index.toFloat(), usage.toFloat())
        }

        val labels = getPast7DaysLabels()
        val dataSet = BarDataSet(entries, "Usage (min)")
        val barData = BarData(dataSet)

        barChart.data = barData
        barChart.description.isEnabled = false
        barChart.xAxis.valueFormatter = IndexAxisValueFormatter(labels)
        barChart.xAxis.position = XAxis.XAxisPosition.BOTTOM
        barChart.axisLeft.axisMinimum = 0f
        barChart.axisRight.isEnabled = false
        barChart.invalidate()
    }

    private fun getLast7DaysUsage(profileName: String): List<Double> {
        val prefs = getSharedPreferences("AppSettings_$profileName", Context.MODE_PRIVATE)
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val calendar = Calendar.getInstance()
        val usageList = mutableListOf<Double>()

        repeat(7) {
            val date = dateFormat.format(calendar.time)
            val usageJson = prefs.getString("usage_$date", null)
            val totalUsage = usageJson?.let { AppUsageUtils.sumUsageFromJson(it) } ?: 0.0
            usageList.add(0, totalUsage)
            calendar.add(Calendar.DATE, -1)
        }

        return usageList
    }

    private fun getPast7DaysLabels(): List<String> {
        val labels = mutableListOf<String>()
        val format = SimpleDateFormat("EEE", Locale.getDefault())
        val calendar = Calendar.getInstance()

        repeat(7) {
            labels.add(0, format.format(calendar.time))
            calendar.add(Calendar.DATE, -1)
        }

        return labels
    }
}
