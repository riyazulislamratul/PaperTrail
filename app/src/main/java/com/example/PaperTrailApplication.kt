package com.example

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import com.example.data.AppDatabase
import com.example.data.DocumentRepository
import com.example.reminder.ReminderManager

class PaperTrailApplication : Application() {

    val database by lazy { AppDatabase.getDatabase(this) }
    val repository by lazy { DocumentRepository(database.documentDao(), this) }
    val reminderManager by lazy { ReminderManager(this) }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                ReminderManager.CHANNEL_ID,
                "Document Reminders & Deadlines",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Alerts for bills, taxes, subscriptions, and document due dates"
                enableVibration(true)
                setShowBadge(true)
            }
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
}
