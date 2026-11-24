package com.ammar.appointmentbooking

import android.app.Application
import com.ammar.appointmentbooking.data.AuthRepository
import com.ammar.appointmentbooking.data.BookingRepository
import com.ammar.appointmentbooking.notify.Notifications
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

/** Simple manual dependency container (no DI framework needed for an app this size). */
class AppContainer {
    private val db = FirebaseFirestore.getInstance()
    val auth = AuthRepository(FirebaseAuth.getInstance(), db)
    val bookings = BookingRepository(db)
}

class BookingApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer()
        Notifications.createChannels(this)
    }
}
