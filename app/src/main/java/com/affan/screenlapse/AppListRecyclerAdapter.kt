package com.affan.screenlapse

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class AppListRecyclerAdapter(
    private val context: Context,
    private val apps: List<AppInfo>,
    private val initiallySelectedApps: Set<String>
) : RecyclerView.Adapter<AppListRecyclerAdapter.ViewHolder>() {

    private val selectedApps = mutableSetOf<String>().apply { addAll(initiallySelectedApps) }

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val appNameTextView: TextView = view.findViewById(R.id.app_name)
        val appIconImageView: ImageView = view.findViewById(R.id.app_icon)
        val appCheckBox: CheckBox = view.findViewById(R.id.app_checkbox)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.app_list_item, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int = apps.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val app = apps[position]

        holder.appNameTextView.text = app.appName
        try {
            holder.appIconImageView.setImageDrawable(
                context.packageManager.getApplicationIcon(app.packageName)
            )
        } catch (e: Exception) {
            holder.appIconImageView.setImageResource(android.R.drawable.sym_def_app_icon)
        }

        holder.appCheckBox.setOnCheckedChangeListener(null)
        holder.appCheckBox.isChecked = selectedApps.contains(app.packageName)

        holder.appCheckBox.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) selectedApps.add(app.packageName)
            else selectedApps.remove(app.packageName)
        }

        holder.itemView.setOnClickListener {
            holder.appCheckBox.performClick()
        }
    }

    fun getSelectedApps(): Set<String> = selectedApps
}
