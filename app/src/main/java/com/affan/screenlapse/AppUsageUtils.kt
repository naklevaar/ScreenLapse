package com.affan.screenlapse

import org.json.JSONObject

object AppUsageUtils {
    fun sumUsageFromJson(json: String): Double {
        val obj = JSONObject(json)
        var totalMillis = 0L
        for (key in obj.keys()) {
            totalMillis += obj.optLong(key, 0L)
        }
        return totalMillis / 1000.0 / 60.0 // return in minutes
    }
}
