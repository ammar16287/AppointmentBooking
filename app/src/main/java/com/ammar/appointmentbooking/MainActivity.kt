package com.ammar.appointmentbooking

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import com.ammar.appointmentbooking.notify.Notifications
import com.ammar.appointmentbooking.ui.AppRoot
import com.ammar.appointmentbooking.ui.theme.AppTheme

class MainActivity : ComponentActivity() {

    // The app keeps working if the user says no: email and SMS reminders still come from the server.
    private val notificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val container = (application as BookingApp).container
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !Notifications.canPost(this)) {
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        setContent {
            AppTheme {
                // Solid app background so the brand-coloured launch window never shows through.
                androidx.compose.material3.Surface(
                    color = androidx.compose.material3.MaterialTheme.colorScheme.background,
                ) { AppRoot(container) }
            }
        }
    }
}
