package com.manninghouse.notes

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

data class Note(val id: Long, val text: String, val isChecklist: Boolean, val done: Boolean)

/** Stores notes as JSON in the app's private storage on the phone. */
object NoteStore {
    private const val PREFS = "notes"
    private const val KEY = "items"

    fun load(context: Context): MutableList<Note> {
        val raw = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY, null) ?: return mutableListOf()
        return try {
            val arr = JSONArray(raw)
            MutableList(arr.length()) { i ->
                val o = arr.getJSONObject(i)
                Note(
                    o.getLong("id"),
                    o.getString("text"),
                    o.optBoolean("checklist"),
                    o.optBoolean("done")
                )
            }
        } catch (e: Exception) {
            mutableListOf()
        }
    }

    private fun save(context: Context, notes: List<Note>) {
        val arr = JSONArray()
        for (n in notes) {
            val o = JSONObject()
            o.put("id", n.id)
            o.put("text", n.text)
            o.put("checklist", n.isChecklist)
            o.put("done", n.done)
            arr.put(o)
        }
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putString(KEY, arr.toString()).commit()
    }

    /** New notes go to the top, keeping the order they were typed in. */
    fun add(context: Context, texts: List<String>, checklist: Boolean) {
        if (texts.isEmpty()) return
        val notes = load(context)
        var nextId = (notes.maxOfOrNull { it.id } ?: 0L) + 1L
        val newNotes = ArrayList<Note>()
        for (t in texts) {
            newNotes.add(Note(nextId, t, checklist, false))
            nextId++
        }
        notes.addAll(0, newNotes)
        save(context, notes)
    }

    fun toggle(context: Context, id: Long) {
        val notes = load(context)
        val i = notes.indexOfFirst { it.id == id }
        if (i < 0 || !notes[i].isChecklist) return
        notes[i] = notes[i].copy(done = !notes[i].done)
        save(context, notes)
    }

    fun delete(context: Context, id: Long) {
        val notes = load(context)
        if (notes.removeAll { it.id == id }) save(context, notes)
    }
}
