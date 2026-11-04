package com.manninghouse.notes

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.os.Bundle
import android.text.InputType
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast

/** Small popup for adding a note to a widget, or renaming a widget's list. */
class AddNoteActivity : Activity() {

    companion object {
        const val ACTION_ADD = "com.manninghouse.notes.ADD"
        const val ACTION_RENAME = "com.manninghouse.notes.RENAME"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val widgetId = resolveWidgetId()
        if (widgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            Toast.makeText(this, R.string.no_widget, Toast.LENGTH_LONG).show()
            finish()
            return
        }

        setContentView(R.layout.activity_add_note)
        setFinishOnTouchOutside(true)

        val input = findViewById<EditText>(R.id.note_input)
        findViewById<Button>(R.id.cancel_button).setOnClickListener { finish() }

        if (intent.action == ACTION_RENAME) {
            setupRename(widgetId, input)
        } else {
            setupAdd(widgetId, input)
        }
        input.requestFocus()
    }

    private fun setupAdd(widgetId: Int, input: EditText) {
        val typeGroup = findViewById<RadioGroup>(R.id.type_group)
        val prefs = getSharedPreferences("settings", MODE_PRIVATE)
        typeGroup.check(
            if (prefs.getBoolean("last_checklist", true)) R.id.type_checklist else R.id.type_note
        )

        findViewById<Button>(R.id.save_button).setOnClickListener {
            val text = input.text.toString().trim()
            val checklist = typeGroup.checkedRadioButtonId == R.id.type_checklist
            prefs.edit().putBoolean("last_checklist", checklist).apply()

            if (text.isNotEmpty()) {
                if (checklist) {
                    // Each line becomes its own checklist item
                    val lines = text.lines().map { it.trim() }.filter { it.isNotEmpty() }
                    NoteStore.add(this, widgetId, lines, true)
                } else {
                    NoteStore.add(this, widgetId, listOf(text), false)
                }
                NotesWidget.refresh(this, widgetId)
            }
            finish()
        }
    }

    private fun setupRename(widgetId: Int, input: EditText) {
        findViewById<TextView>(R.id.dialog_title).setText(R.string.rename_list)
        findViewById<View>(R.id.type_group).visibility = View.GONE
        findViewById<View>(R.id.tip_text).visibility = View.GONE

        input.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_WORDS
        input.isSingleLine = true
        input.minLines = 1
        input.setHint(R.string.title_hint)
        val current = NoteStore.getTitle(this, widgetId) ?: ""
        input.setText(current)
        input.setSelection(current.length)

        findViewById<Button>(R.id.save_button).setOnClickListener {
            NoteStore.setTitle(this, widgetId, input.text.toString().trim())
            NotesWidget.refresh(this, widgetId)
            finish()
        }
    }

    /** Widget taps pass their id. From the app icon, only works if there's exactly one widget. */
    private fun resolveWidgetId(): Int {
        val fromIntent = intent.getIntExtra(
            AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID
        )
        if (fromIntent != AppWidgetManager.INVALID_APPWIDGET_ID) return fromIntent
        val all = NotesWidget.allWidgetIds(this)
        return if (all.size == 1) all[0] else AppWidgetManager.INVALID_APPWIDGET_ID
    }
}
