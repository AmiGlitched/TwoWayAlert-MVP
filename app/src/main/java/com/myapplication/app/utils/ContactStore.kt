package com.myapplication.app.utils

import android.content.SharedPreferences
import androidx.core.content.edit
import com.myapplication.app.model.Contact
import org.json.JSONArray
import org.json.JSONObject

// handles saving/loading the contact list to SharedPreferences as a JSON blob
// (didn't want to pull in another dependency like Gson/Moshi just for this)
object ContactStore {

    private const val KEY = "contactsJson"

    fun load(prefs: SharedPreferences): MutableList<Contact> {
        val raw = prefs.getString(KEY, null) ?: return migrateOldContacts(prefs)

        val out = mutableListOf<Contact>()
        try {
            val arr = JSONArray(raw)
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                out.add(Contact(obj.optString("name"), obj.optString("phone")))
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return out
    }

    private fun migrateOldContacts(prefs: SharedPreferences): MutableList<Contact> {
        val out = mutableListOf<Contact>()
        val c1 = prefs.getString("c1Num", "") ?: ""
        val c2 = prefs.getString("c2Num", "") ?: ""
        if (c1.isNotBlank()) out.add(Contact("Primary Contact", c1))
        if (c2.isNotBlank()) out.add(Contact("Secondary Contact", c2))
        save(prefs, out) // write it in the new format so we don't redo this every launch
        return out
    }

    fun save(prefs: SharedPreferences, contacts: List<Contact>) {
        val arr = JSONArray()
        for (c in contacts) {
            val obj = JSONObject()
            obj.put("name", c.name)
            obj.put("phone", c.phone)
            arr.put(obj)
        }
        prefs.edit { putString(KEY, arr.toString()) }
    }

    // for pushing to Firestore alongside the rest of the profile doc
    fun toFirestoreList(contacts: List<Contact>): List<Map<String, String>> {
        return contacts.map { mapOf("name" to it.name, "phone" to it.phone) }
    }

    fun fromFirestoreList(raw: List<Map<String, Any>>?): MutableList<Contact> {
        if (raw == null) return mutableListOf()
        return raw.asSequence().mapNotNull {
            val phone = (it["phone"] as? String) ?: return@mapNotNull null
            Contact((it["name"] as? String) ?: "Contact", phone)
        }.toMutableList()
    }
}