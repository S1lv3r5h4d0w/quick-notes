package com.manninghouse.notes

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Paint
import android.widget.RemoteViews

class NotesWidget : AppWidgetProvider() {

    override fun onUpdate(context: Context, manager: AppWidgetManager, appWidgetIds: IntArray) {
        for (id in appWidgetIds) updateWidget(context, manager, id)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action != ACTION_ITEM) return
        val noteId = intent.getLongExtra(EXTRA_NOTE_ID, -1L)
        when (intent.getStringExtra(EXTRA_COMMAND)) {
            CMD_TOGGLE -> NoteStore.toggle(context, noteId)
            CMD_DELETE -> NoteStore.delete(context, noteId)
            else -> return
        }
        refreshAll(context)
    }

    companion object {
        private const val ACTION_ITEM = "com.manninghouse.notes.ITEM_CLICK"
        private const val EXTRA_NOTE_ID = "note_id"
        private const val EXTRA_COMMAND = "command"
        private const val CMD_TOGGLE = "toggle"
        private const val CMD_DELETE = "delete"

        fun refreshAll(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(ComponentName(context, NotesWidget::class.java))
            for (id in ids) updateWidget(context, manager, id)
        }

        private fun updateWidget(context: Context, manager: AppWidgetManager, widgetId: Int) {
            val views = RemoteViews(context.packageName, R.layout.widget_notes)

            // "+" button opens the add-note popup
            val addIntent = Intent(context, AddNoteActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            views.setOnClickPendingIntent(
                R.id.add_button,
                PendingIntent.getActivity(
                    context, 0, addIntent,
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                )
            )

            // The list of notes
            val items = RemoteViews.RemoteCollectionItems.Builder().setHasStableIds(true)
            for (note in NoteStore.load(context)) items.addItem(note.id, buildRow(context, note))
            views.setRemoteAdapter(R.id.notes_list, items.build())
            views.setEmptyView(R.id.notes_list, R.id.empty_text)

            // Taps inside the list (check / delete) come back here
            val template = Intent(context, NotesWidget::class.java).setAction(ACTION_ITEM)
            views.setPendingIntentTemplate(
                R.id.notes_list,
                PendingIntent.getBroadcast(
                    context, widgetId, template,
                    PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                )
            )

            manager.updateAppWidget(widgetId, views)
        }

        private fun buildRow(context: Context, note: Note): RemoteViews {
            val row = RemoteViews(context.packageName, R.layout.widget_item)
            val done = note.isChecklist && note.done

            row.setTextViewText(R.id.note_text, note.text)
            row.setImageViewResource(
                R.id.note_icon,
                when {
                    !note.isChecklist -> R.drawable.ic_bullet
                    note.done -> R.drawable.ic_checked
                    else -> R.drawable.ic_unchecked
                }
            )
            // Always set these explicitly: list rows get recycled.
            row.setInt(
                R.id.note_text, "setPaintFlags",
                if (done) Paint.ANTI_ALIAS_FLAG or Paint.STRIKE_THRU_TEXT_FLAG else Paint.ANTI_ALIAS_FLAG
            )
            row.setColor(
                R.id.note_text, "setTextColor",
                if (done) R.color.widget_subtext else R.color.widget_text
            )

            row.setOnClickFillInIntent(
                R.id.row_root,
                Intent().putExtra(EXTRA_NOTE_ID, note.id).putExtra(EXTRA_COMMAND, CMD_TOGGLE)
            )
            row.setOnClickFillInIntent(
                R.id.delete_button,
                Intent().putExtra(EXTRA_NOTE_ID, note.id).putExtra(EXTRA_COMMAND, CMD_DELETE)
            )
            return row
        }
    }
}
