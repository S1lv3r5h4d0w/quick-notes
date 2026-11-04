package com.manninghouse.notes

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject

data class Note(val id: Long, val text: String, val isChecklist: Boolean, val done: Boolean)

/** Stores each widget's notes separately, as JSON in the app's private storage. */
object NoteStore {
    private const val PREFS = "notes"
    private const val LEGACY_KEY = "items" // v1.0 single shared list

    private fun itemsKey(widgetId: Int) = "items_$widgetId"
    private fun titleKey(widgetId: Int) = "title_$widgetId"
    private fun prefs(context: Context): SharedPreferences =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun load(context: Context, widgetId: Int): MutableList<Note> {
        val p = prefs(context)
        var raw = p.getString(itemsKey(widgetId), null)
        if (raw == null) {
            // First widget to load after the update keeps your existing notes
            val legacy = p.getString(LEGACY_KEY, null)
            if (legacy != null) {
                p.edit().putString(itemsKey(widgetId), legacy).remove(LEGACY_KEY).commit()
                raw = legacy
            }
        }
        if (raw == null) return mutableListOf()
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

    private fun save(context: Context, widgetId: Int, notes: List<Note>) {
        val arr = JSONArray()
        for (n in notes) {
            val o = JSONObject()
            o.put("id", n.id)
            o.put("text", n.text)
            o.put("checklist", n.isChecklist)
            o.put("done", n.done)
            arr.put(o)
        }
        prefs(context).edit().putString(itemsKey(widgetId), arr.toString()).commit()
    }

    /** New notes go to the top, keeping the order they were typed in. */
    fun add(context: Context, widgetId: Int, texts: List<String>, checklist: Boolean) {
        if (texts.isEmpty()) return
        val notes = load(context, widgetId)
        var nextId = (notes.maxOfOrNull { it.id } ?: 0L) + 1L
        val newNotes = ArrayList<Note>()
        for (t in texts) {
            newNotes.add(Note(nextId, t, checklist, false))
            nextId++
        }
        notes.addAll(0, newNotes)
        save(context, widgetId, notes)
    }

    fun toggle(context: Context, widgetId: Int, id: Long) {
        val notes = load(context, widgetId)
        val i = notes.indexOfFirst { it.id == id }
        if (i < 0 || !notes[i].isChecklist) return
        notes[i] = notes[i].copy(done = !notes[i].done)
        save(context, widgetId, notes)
    }

    fun delete(context: Context, widgetId: Int, id: Long) {
        val notes = load(context, widgetId)
        if (notes.removeAll { it.id == id }) save(context, widgetId, notes)
    }

    fun getTitle(context: Context, widgetId: Int): String? =
        prefs(context).getString(titleKey(widgetId), null)

    fun setTitle(context: Context, widgetId: Int, title: String?) {
        val e = prefs(context).edit()
        if (title.isNullOrBlank()) e.remove(titleKey(widgetId)) else e.putString(titleKey(widgetId), title)
        e.commit()
    }

    /** Called when a widget is removed from the home screen. */
    fun removeWidget(context: Context, widgetId: Int) {
        prefs(context).edit().remove(itemsKey(widgetId)).remove(titleKey(widgetId)).commit()
    }
}
