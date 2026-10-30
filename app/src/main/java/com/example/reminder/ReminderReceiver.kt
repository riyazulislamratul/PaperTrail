package com.example.reminder

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.PaperTrailApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

class ReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val app = context.applicationContext as? PaperTrailApplication ?: return
        val docId = intent.getLongExtra(ReminderManager.EXTRA_DOC_ID, -1L)
        if (docId == -1L) return

        when (intent.action) {
            ReminderManager.ACTION_TRIGGER_REMINDER -> {
                val title = intent.getStringExtra(ReminderManager.EXTRA_DOC_TITLE) ?: "Document Deadline"
                val amount = intent.getStringExtra(ReminderManager.EXTRA_DOC_AMOUNT)
                val dueDate = intent.getLongExtra(ReminderManager.EXTRA_DUE_DATE, 0L)
                app.reminderManager.showNotification(docId, title, amount, dueDate)
            }

            ReminderManager.ACTION_MARK_DONE -> {
                // Dismiss notification
                val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                notificationManager.cancel(docId.toInt())

                CoroutineScope(Dispatchers.IO).launch {
                    val doc = app.database.documentDao().getDocumentByIdDirect(docId)
                    if (doc != null) {
                        app.database.documentDao().updateDocument(
                            doc.copy(
                                isActionCompleted = true,
                                hasReminder = false,
                                reminderTime = null,
                                updatedAt = System.currentTimeMillis()
                            )
                        )
                    }
                }
            }

            ReminderManager.ACTION_SNOOZE_DAY -> {
                // Dismiss current notification
                val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                notificationManager.cancel(docId.toInt())

                val title = intent.getStringExtra(ReminderManager.EXTRA_DOC_TITLE) ?: "Document Deadline"
                val amount = intent.getStringExtra(ReminderManager.EXTRA_DOC_AMOUNT)
                val newTriggerTime = System.currentTimeMillis() + TimeUnit.DAYS.toMillis(1)

                app.reminderManager.scheduleReminder(docId, title, amount, newTriggerTime)

                CoroutineScope(Dispatchers.IO).launch {
                    val doc = app.database.documentDao().getDocumentByIdDirect(docId)
                    if (doc != null) {
                        app.database.documentDao().updateDocument(
                            doc.copy(
                                hasReminder = true,
                                reminderTime = newTriggerTime,
                                updatedAt = System.currentTimeMillis()
                            )
                        )
                    }
                }
            }
        }
    }
}
