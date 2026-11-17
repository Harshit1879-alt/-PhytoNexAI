
package com.example.phytonexai

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object ScanHistory {

    private const val PREFS_NAME = "phytonexai_history"
    private const val KEY_HISTORY = "scan_records"

    fun saveResult(
        context: Context,
        disease: String,
        confidence: Float
    ) {
        val prefs = context.getSharedPreferences(
            PREFS_NAME,
            Context.MODE_PRIVATE
        )

        val history = JSONArray(
            prefs.getString(KEY_HISTORY, "[]")
        )

        val record = JSONObject().apply {
            put("disease", disease)
            put("confidence", confidence)
            put(
                "date",
                SimpleDateFormat(
                    "dd MMM yyyy, hh:mm a",
                    Locale.getDefault()
                ).format(Date())
            )
        }

        // Add newest scan at the beginning.
        val updatedHistory = JSONArray()
        updatedHistory.put(record)

        for (i in 0 until history.length()) {
            updatedHistory.put(history.getJSONObject(i))
        }

        prefs.edit()
            .putString(KEY_HISTORY, updatedHistory.toString())
            .apply()
    }

    fun getHistory(context: Context): JSONArray {
        val prefs = context.getSharedPreferences(
            PREFS_NAME,
            Context.MODE_PRIVATE
        )

        return JSONArray(prefs.getString(KEY_HISTORY, "[]"))
    }

    fun clearHistory(context: Context) {
        context.getSharedPreferences(
            PREFS_NAME,
            Context.MODE_PRIVATE
        ).edit()
            .remove(KEY_HISTORY)
            .apply()
    }
}