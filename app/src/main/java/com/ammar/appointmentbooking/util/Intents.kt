package com.ammar.appointmentbooking.util

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.CalendarContract
import android.widget.Toast

/**
 * Everything here opens another app with an intent, so the app needs no phone, SMS or
 * calendar permission.
 */
object Intents {

    private fun Context.launch(intent: Intent, fallback: String) {
        try {
            startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(this, fallback, Toast.LENGTH_SHORT).show()
        }
    }

    fun dial(context: Context, phone: String) =
        context.launch(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phone")), "No phone app found")

    fun email(context: Context, to: String, subject: String) =
        context.launch(
            Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:$to")).putExtra(Intent.EXTRA_SUBJECT, subject),
            "No email app found"
        )

    fun map(context: Context, address: String) =
        context.launch(Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q=${Uri.encode(address)}")), "No maps app found")

    fun addToCalendar(context: Context, title: String, startMillis: Long, endMillis: Long, location: String) =
        context.launch(
            Intent(Intent.ACTION_INSERT)
                .setData(CalendarContract.Events.CONTENT_URI)
                .putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, startMillis)
                .putExtra(CalendarContract.EXTRA_EVENT_END_TIME, endMillis)
                .putExtra(CalendarContract.Events.TITLE, title)
                .putExtra(CalendarContract.Events.EVENT_LOCATION, location),
            "No calendar app found"
        )
}
