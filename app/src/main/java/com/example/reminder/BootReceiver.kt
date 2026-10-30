package com.example.reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.PaperTrailApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            val app = context.applicationContext as? PaperTrailApplication ?: return
            CoroutineScope(Dispatchers.IO).launch {
                val now = System.currentTimeMillis()
                val reminders = app.database.documentDao().getAllRemindersDirect()
                for (doc in reminders) {
                    val triggerTime = doc.reminderTime
                    if (triggerTime != null && triggerTime > now) {
                        val amountStr = if (doc.amount != null) "${doc.currency ?: "$"}${doc.amount}" else null
                        app.reminderManager.scheduleReminder(
                            docId = doc.id,
                            title = doc.title,
                            amountText = amountStr,
                            triggerTimeMillis = triggerTime,
                            dueDateMillis = doc.dueDate
                        )
                    }
                }
            }
        }
    }
}
