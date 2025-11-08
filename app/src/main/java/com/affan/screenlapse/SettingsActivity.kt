package com.affan.screenlapse

import android.app.AlertDialog
import android.content.*
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.affan.screenlapse.getProfilePrefs


class SettingsActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var saveButton: Button
    private lateinit var nextButton: Button
    private lateinit var statsButton: Button
    private lateinit var bugButton: Button
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var adapter: AppListRecyclerAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        recyclerView = findViewById(R.id.app_list_view)
        nextButton = findViewById(R.id.nextButton)
        statsButton = findViewById(R.id.stats_button)
        bugButton = findViewById(R.id.bug_report_button)
        sharedPreferences = getProfilePrefs()

        recyclerView.layoutManager = LinearLayoutManager(this)

        val installedApps = getInstalledApps()
        val previouslySelected = sharedPreferences.getStringSet("selectedApps", emptySet()) ?: emptySet()

        adapter = AppListRecyclerAdapter(
            context = this,
            apps = installedApps,
            initiallySelectedApps = previouslySelected
        )

        recyclerView.adapter = adapter


        nextButton.setOnClickListener {
            saveSelectedApps()
            startActivity(Intent(this, TimerActivity::class.java))
        }

        statsButton.setOnClickListener {
            startActivity(Intent(this, StatsActivity::class.java))
        }

        bugButton.setOnClickListener {
            showBugReportDialog()
        }
    }

    private fun getInstalledApps(): List<AppInfo> {
        val packageManager = packageManager
        return packageManager.getInstalledApplications(PackageManager.GET_META_DATA)
            .filter { packageManager.getLaunchIntentForPackage(it.packageName) != null }
            .map {
                val appName = packageManager.getApplicationLabel(it).toString()
                AppInfo(appName, it.packageName)
            }
    }

    private fun saveSelectedApps() {
        val selected = adapter.getSelectedApps()
        sharedPreferences.edit()
            .putStringSet("selectedApps", selected)
            .apply()

        Toast.makeText(this, "Selected apps have been saved", Toast.LENGTH_SHORT).show()
        Log.d("SettingsActivity", "Saved selected apps: $selected")
    }

    private fun showBugReportDialog() {
        val editText = EditText(this).apply {
            hint = "Describe the issue here..."
            setPadding(32, 32, 32, 32)
        }

        AlertDialog.Builder(this)
            .setTitle("Report a Bug")
            .setView(editText)
            .setPositiveButton("Submit") { _, _ ->
                val bugText = editText.text.toString()
                sendBugReportEmail(bugText)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun sendBugReportEmail(bugText: String) {
        val recipient = "affanahmadq10@gmail.com" // <- Replace with YOUR email
        val uri = Uri.parse("mailto:$recipient")
        val emailIntent = Intent(Intent.ACTION_SENDTO, uri).apply {
            putExtra(Intent.EXTRA_SUBJECT, "Bug Report - ScreenTimeController")
            putExtra(Intent.EXTRA_TEXT, bugText)
        }

        try {
            startActivity(Intent.createChooser(emailIntent, "Send Bug Report via..."))
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(this, "No email app found!", Toast.LENGTH_SHORT).show()
        }
    }
}
