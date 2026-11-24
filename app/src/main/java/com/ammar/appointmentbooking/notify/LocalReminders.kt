package com.ammar.appointmentbooking.notify

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.ammar.appointmentbooking.data.Appointment
import com.ammar.appointmentbooking.data.Status
import com.ammar.appointmentbooking.util.TimeFmt
import java.time.ZoneId
import java.util.concurrent.TimeUnit

/**
 * Backup reminder on the phone, used only if the server push did not arrive (no network, push
 * turned off, etc.). Uses WorkManager with an inexact delay, so no exact-alarm permission is needed.
 */
object LocalReminders {
    private const val PREFS = "reminders"
    private const val LEAD_MINUTES = 120L

    fun markPushReceived(context: Context, appointmentId: String) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putLong(appointmentId, System.currentTimeMillis()).apply()
    }

    fun pushReceived(context: Context, appointmentId: String): Boolean =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).contains(appointmentId)

    /** Keeps one pending local reminder per upcoming appointment, cancelling ones no longer needed. */
    fun sync(context: Context, appointments: List<Appointment>, zone: ZoneId) {
        val wm = WorkManager.getInstance(context)
        val now = System.currentTimeMillis()
        appointments.forEach { a ->
            val name = "reminder_${a.id}"
            val fireAt = a.startMillis - TimeUnit.MINUTES.toMillis(LEAD_MINUTES)
            val wanted = (a.status == Status.CONFIRMED || a.status == Status.PENDING) && fireAt > now
            if (!wanted) {
                wm.cancelUniqueWork(name)
                return@forEach
            }
            val request = OneTimeWorkRequestBuilder<ReminderWorker>()
                .setInitialDelay(fireAt - now, TimeUnit.MILLISECONDS)
                .setInputData(
                    workDataOf(
                        "id" to a.id,
                        "title" to "Upcoming: ${a.serviceName}",
                        "body" to "${TimeFmt.dayTime(a.startMillis, zone)} with ${a.staffName}",
                    )
                )
                .build()
            wm.enqueueUniqueWork(name, ExistingWorkPolicy.REPLACE, request)
        }
    }
}

class ReminderWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val id = inputData.getString("id") ?: return Result.success()
        if (LocalReminders.pushReceived(applicationContext, id)) return Result.success()
        Notifications.show(
            applicationContext,
            id.hashCode(),
            inputData.getString("title") ?: "Appointment reminder",
            inputData.getString("body").orEmpty(),
        )
        return Result.success()
    }
}
