package com.example.reminder

import android.app.AlarmManager
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ReminderManager(private val context: Context) {

    companion object {
        const val CHANNEL_ID = "papertrail_reminders"
        const val EXTRA_DOC_ID = "extra_doc_id"
        const val EXTRA_DOC_TITLE = "extra_doc_title"
        const val EXTRA_DOC_AMOUNT = "extra_doc_amount"
        const val EXTRA_DUE_DATE = "extra_due_date"
        const val ACTION_TRIGGER_REMINDER = "com.example.papertrail.ACTION_TRIGGER_REMINDER"
        const val ACTION_MARK_DONE = "com.example.papertrail.ACTION_MARK_DONE"
        const val ACTION_SNOOZE_DAY = "com.example.papertrail.ACTION_SNOOZE_DAY"
    }

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    fun scheduleReminder(docId: Long, title: String, amountText: String?, triggerTimeMillis: Long, dueDateMillis: Long? = null) {
        if (triggerTimeMillis <= System.currentTimeMillis()) return

        val intent = Intent(context, ReminderReceiver::class.java).apply {
            action = ACTION_TRIGGER_REMINDER
            putExtra(EXTRA_DOC_ID, docId)
            putExtra(EXTRA_DOC_TITLE, title)
            putExtra(EXTRA_DOC_AMOUNT, amountText)
            putExtra(EXTRA_DUE_DATE, dueDateMillis ?: 0L)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            docId.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTimeMillis, pendingIntent)
            } else {
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerTimeMillis, pendingIntent)
            }
        } catch (e: SecurityException) {
            // In case exact alarm permission is restricted on Android 12+, fallback to normal set
            alarmManager.set(AlarmManager.RTC_WAKEUP, triggerTimeMillis, pendingIntent)
        }
    }

    fun cancelReminder(docId: Long) {
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            action = ACTION_TRIGGER_REMINDER
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            docId.toInt(),
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
        }

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(docId.toInt())
    }

    fun showNotification(docId: Long, title: String, amountText: String?, dueDateMillis: Long?) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Intent to open app to document
        val contentIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_DOC_ID, docId)
        }
        val contentPendingIntent = PendingIntent.getActivity(
            context,
            docId.toInt(),
            contentIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Action: Mark as Done
        val doneIntent = Intent(context, ReminderReceiver::class.java).apply {
            action = ACTION_MARK_DONE
            putExtra(EXTRA_DOC_ID, docId)
        }
        val donePendingIntent = PendingIntent.getBroadcast(
            context,
            (docId + 100000).toInt(),
            doneIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Action: Snooze 1 Day
        val snoozeIntent = Intent(context, ReminderReceiver::class.java).apply {
            action = ACTION_SNOOZE_DAY
            putExtra(EXTRA_DOC_ID, docId)
            putExtra(EXTRA_DOC_TITLE, title)
            putExtra(EXTRA_DOC_AMOUNT, amountText)
        }
        val snoozePendingIntent = PendingIntent.getBroadcast(
            context,
            (docId + 200000).toInt(),
            snoozeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val formattedDueDate = if (dueDateMillis != null && dueDateMillis > 0) {
            "Due: " + SimpleDateFormat("EEE, MMM dd", Locale.US).format(Date(dueDateMillis))
        } else "Action Required"

        val bodyText = if (!amountText.isNullOrBlank()) {
            "Amount: $amountText • $formattedDueDate"
        } else {
            formattedDueDate
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Deadline Reminder: $title")
            .setContentText(bodyText)
            .setStyle(NotificationCompat.BigTextStyle().bigText("$title is due soon. Check document details or mark as done."))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(contentPendingIntent)
            .setAutoCancel(true)
            .addAction(0, "Mark Done", donePendingIntent)
            .addAction(0, "Snooze 1 Day", snoozePendingIntent)
            .build()

        notificationManager.notify(docId.toInt(), notification)
    }
}
