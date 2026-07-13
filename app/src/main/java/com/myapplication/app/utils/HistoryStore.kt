package com.myapplication.app.utils

import android.content.SharedPreferences
import androidx.core.content.edit
import com.myapplication.app.model.SosHistoryEntry
import org.json.JSONArray
import org.json.JSONObject

object HistoryStore {

    private const val KEY = "sosHistoryJson"
    private const val MAX_ENTRIES = 50 // don't let this grow forever in SharedPreferences

    fun load(prefs: SharedPreferences): MutableList<SosHistoryEntry> {
        val raw = prefs.getString(KEY, null) ?: return mutableListOf()
        val out = mutableListOf<SosHistoryEntry>()
        try {
            val arr = JSONArray(raw)
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                out.add(
                    SosHistoryEntry(
                        obj.optLong("timestamp", 0L),
                        obj.optString("type", "SOS Triggered"),
                        obj.optString("location", "Location unavailable."),
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return out
    }

    fun add(prefs: SharedPreferences, entry: SosHistoryEntry) {
        val current = load(prefs)
        current.add(0, entry) // newest on top
        val trimmed = if (current.size > MAX_ENTRIES) current.subList(0, MAX_ENTRIES) else current

        val arr = JSONArray()
        for (e in trimmed) {
            val obj = JSONObject()
            obj.put("timestamp", e.timestamp)
            obj.put("type", e.type)
            obj.put("location", e.location)
            arr.put(obj)
        }
        prefs.edit { putString(KEY, arr.toString()) }
    }

    fun clear(prefs: SharedPreferences) {
        prefs.edit { remove(KEY) }
    }
}