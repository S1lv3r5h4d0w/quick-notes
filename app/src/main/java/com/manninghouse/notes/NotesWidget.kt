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

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        for (id in appWidgetIds) NoteStore.removeWidget(context, id)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action != ACTION_ITEM) return
        val widgetId = intent.getIntExtra(
            AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID
        )
        if (widgetId == AppWidgetManager.INVALID_APPWIDGET_ID) return
        val noteId = intent.getLongExtra(EXTRA_NOTE_ID, -1L)
        when (intent.getStringExtra(EXTRA_COMMAND)) {
            CMD_TOGGLE -> NoteStore.toggle(context, widgetId, noteId)
            CMD_DELETE -> NoteStore.delete(context, widgetId, noteId)
            else -> return
        }
        refresh(context, widgetId)
    }

    companion object {
        private const val ACTION_ITEM = "com.manninghouse.notes.ITEM_CLICK"
        private const val EXTRA_NOTE_ID = "note_id"
        private const val EXTRA_COMMAND = "command"
        private const val CMD_TOGGLE = "toggle"
        private const val CMD_DELETE = "delete"

        fun allWidgetIds(context: Context): IntArray =
            AppWidgetManager.getInstance(context)
                .getAppWidgetIds(ComponentName(context, NotesWidget::class.java))

        fun refresh(context: Context, widgetId: Int) {
            updateWidget(context, AppWidgetManager.getInstance(context), widgetId)
        }

        private fun updateWidget(context: Context, manager: AppWidgetManager, widgetId: Int) {
            val views = RemoteViews(context.packageName, R.layout.widget_notes)

            // Title (tap to rename this list)
            views.setTextViewText(
                R.id.widget_title,
                NoteStore.getTitle(context, widgetId) ?: context.getString(R.string.widget_title)
            )
            views.setOnClickPendingIntent(
                R.id.widget_title,
                activityIntent(context, widgetId, AddNoteActivity.ACTION_RENAME)
            )

            // "+" button opens the add-note popup for THIS widget
            views.setOnClickPendingIntent(
                R.id.add_button,
                activityIntent(context, widgetId, AddNoteActivity.ACTION_ADD)
            )

            // This widget's notes
            val items = RemoteViews.RemoteCollectionItems.Builder().setHasStableIds(true)
            for (note in NoteStore.load(context, widgetId)) items.addItem(note.id, buildRow(context, note))
            views.setRemoteAdapter(R.id.notes_list, items.build())
            views.setEmptyView(R.id.notes_list, R.id.empty_text)

            // Taps inside the list (check / delete) come back here, tagged with this widget's id
            val template = Intent(context, NotesWidget::class.java)
                .setAction(ACTION_ITEM)
                .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
            views.setPendingIntentTemplate(
                R.id.notes_list,
                PendingIntent.getBroadcast(
                    context, widgetId, template,
                    PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                )
            )

            manager.updateAppWidget(widgetId, views)
        }

        private fun activityIntent(context: Context, widgetId: Int, action: String): PendingIntent {
            val intent = Intent(context, AddNoteActivity::class.java)
                .setAction(action)
                .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            return PendingIntent.getActivity(
                context, widgetId, intent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
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
