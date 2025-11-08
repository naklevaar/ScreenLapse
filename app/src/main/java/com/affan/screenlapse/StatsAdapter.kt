package com.affan.screenlapse

import android.content.pm.PackageManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class StatsAdapter(
    private val usageList: List<AppUsageInfo>,
    private val packageManager: PackageManager
) : RecyclerView.Adapter<StatsAdapter.StatsViewHolder>() {

    class StatsViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val appName: TextView = view.findViewById(R.id.app_name)
        val usageTime: TextView = view.findViewById(R.id.usage_time)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StatsViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.stats_item, parent, false)
        return StatsViewHolder(view)
    }

    override fun onBindViewHolder(holder: StatsViewHolder, position: Int) {
        val appInfo = usageList[position]
        val label = try {
            val appInfoObj = packageManager.getApplicationInfo(appInfo.packageName, 0)
            packageManager.getApplicationLabel(appInfoObj).toString()
        } catch (e: PackageManager.NameNotFoundException) {
            appInfo.packageName
        }

        holder.appName.text = label
        holder.usageTime.text = String.format("%.2f min", appInfo.usageTimeMillis / 1000.0 / 60.0)
    }

    override fun getItemCount(): Int = usageList.size
}
