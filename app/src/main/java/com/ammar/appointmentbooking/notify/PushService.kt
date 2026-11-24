package com.ammar.appointmentbooking.notify

import com.ammar.appointmentbooking.BookingApp
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Receives pushes from the Cloud Functions. The server sends data-only messages so this method
 * runs even when the app is in the background, and we can record that a reminder arrived
 * (so the local WorkManager fallback does not show it twice).
 */
class PushService : FirebaseMessagingService() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onNewToken(token: String) {
        val app = application as BookingApp
        scope.launch { runCatching { app.container.auth.saveFcmToken(token) } }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        val data = message.data
        val title = data["title"] ?: message.notification?.title ?: return
        val body = data["body"] ?: message.notification?.body.orEmpty()
        val apptId = data["appointmentId"].orEmpty()
        val kind = data["kind"].orEmpty()
        val channel = if (kind == "reminder") Notifications.CHANNEL_REMINDERS else Notifications.CHANNEL_BOOKINGS
        if (kind == "reminder" && apptId.isNotEmpty()) LocalReminders.markPushReceived(this, apptId)
        Notifications.show(this, (apptId + kind).hashCode(), title, body, channel)
    }
}
