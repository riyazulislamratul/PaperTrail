package com.example.util

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

enum class DeadlineUrgency {
    COMPLETED,
    OVERDUE,
    TODAY,
    TOMORROW,
    THIS_WEEK,
    UPCOMING,
    NO_DEADLINE
}

object Formatters {

    private val standardDateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.US)
    private val shortDateFormat = SimpleDateFormat("MMM dd", Locale.US)
    private val timeFormat = SimpleDateFormat("h:mm a", Locale.US)

    fun formatDate(epochMillis: Long?): String {
        if (epochMillis == null || epochMillis <= 0) return "No date"
        return standardDateFormat.format(Date(epochMillis))
    }

    fun formatShortDate(epochMillis: Long?): String {
        if (epochMillis == null || epochMillis <= 0) return "None"
        return shortDateFormat.format(Date(epochMillis))
    }

    fun formatDateTime(epochMillis: Long?): String {
        if (epochMillis == null || epochMillis <= 0) return "None"
        return "${standardDateFormat.format(Date(epochMillis))} at ${timeFormat.format(Date(epochMillis))}"
    }

    fun formatCurrency(amount: Double?, currency: String? = "$"): String {
        if (amount == null) return "No amount"
        return "${currency ?: "$"}${String.format(Locale.US, "%,.2f", amount)}"
    }

    fun getDeadlineUrgency(dueDate: Long?, isCompleted: Boolean): DeadlineUrgency {
        if (isCompleted) return DeadlineUrgency.COMPLETED
        if (dueDate == null || dueDate <= 0) return DeadlineUrgency.NO_DEADLINE

        val now = System.currentTimeMillis()
        val calNow = Calendar.getInstance().apply { timeInMillis = now }
        val calDue = Calendar.getInstance().apply { timeInMillis = dueDate }

        val diffMillis = dueDate - now
        val diffDays = TimeUnit.MILLISECONDS.toDays(diffMillis)

        val isSameDay = calNow.get(Calendar.YEAR) == calDue.get(Calendar.YEAR) &&
                calNow.get(Calendar.DAY_OF_YEAR) == calDue.get(Calendar.DAY_OF_YEAR)

        if (isSameDay) return DeadlineUrgency.TODAY
        if (dueDate < now) return DeadlineUrgency.OVERDUE

        val tomorrowCal = Calendar.getInstance().apply {
            timeInMillis = now
            add(Calendar.DAY_OF_YEAR, 1)
        }
        val isTomorrow = tomorrowCal.get(Calendar.YEAR) == calDue.get(Calendar.YEAR) &&
                tomorrowCal.get(Calendar.DAY_OF_YEAR) == calDue.get(Calendar.DAY_OF_YEAR)
        if (isTomorrow) return DeadlineUrgency.TOMORROW

        return if (diffDays in 2..7) DeadlineUrgency.THIS_WEEK else DeadlineUrgency.UPCOMING
    }

    fun getRelativeDeadlineText(dueDate: Long?, isCompleted: Boolean): String {
        if (isCompleted) return "Completed"
        if (dueDate == null || dueDate <= 0) return "No deadline"

        val urgency = getDeadlineUrgency(dueDate, isCompleted)
        val diffMillis = dueDate - System.currentTimeMillis()
        val diffDays = TimeUnit.MILLISECONDS.toDays(Math.abs(diffMillis))

        return when (urgency) {
            DeadlineUrgency.COMPLETED -> "Done"
            DeadlineUrgency.OVERDUE -> if (diffDays <= 0) "Overdue" else "$diffDays d overdue"
            DeadlineUrgency.TODAY -> "Due Today"
            DeadlineUrgency.TOMORROW -> "Due Tomorrow"
            DeadlineUrgency.THIS_WEEK -> "Due in $diffDays days"
            DeadlineUrgency.UPCOMING -> "Due ${shortDateFormat.format(Date(dueDate))}"
            DeadlineUrgency.NO_DEADLINE -> "No deadline"
        }
    }
}
