package com.manninghouse.notes

import android.app.Activity
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.RadioGroup

/** Small popup for adding a note. Also opens if you tap the app icon. */
class AddNoteActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_note)
        setFinishOnTouchOutside(true)

        val input = findViewById<EditText>(R.id.note_input)
        val typeGroup = findViewById<RadioGroup>(R.id.type_group)
        val prefs = getSharedPreferences("settings", MODE_PRIVATE)

        typeGroup.check(
            if (prefs.getBoolean("last_checklist", true)) R.id.type_checklist else R.id.type_note
        )

        findViewById<Button>(R.id.cancel_button).setOnClickListener { finish() }

        findViewById<Button>(R.id.save_button).setOnClickListener {
            val text = input.text.toString().trim()
            val checklist = typeGroup.checkedRadioButtonId == R.id.type_checklist
            prefs.edit().putBoolean("last_checklist", checklist).apply()

            if (text.isNotEmpty()) {
                if (checklist) {
                    // Each line becomes its own checklist item
                    val lines = text.lines().map { it.trim() }.filter { it.isNotEmpty() }
                    NoteStore.add(this, lines, true)
                } else {
                    NoteStore.add(this, listOf(text), false)
                }
                NotesWidget.refreshAll(this)
            }
            finish()
        }

        input.requestFocus()
    }
}
