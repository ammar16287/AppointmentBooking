package com.ammar.appointmentbooking.ui.customer

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ammar.appointmentbooking.data.Appointment
import com.ammar.appointmentbooking.data.BookingRepository
import com.ammar.appointmentbooking.notify.LocalReminders
import com.ammar.appointmentbooking.ui.AppData
import com.ammar.appointmentbooking.ui.common.AppointmentCard
import com.ammar.appointmentbooking.ui.common.AppointmentDialog
import com.ammar.appointmentbooking.ui.common.EmptyState
import com.ammar.appointmentbooking.ui.common.SectionTitle
import java.time.ZoneId

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyBookingsScreen(data: AppData, repo: BookingRepository, onReschedule: (Appointment) -> Unit) {
    val context = LocalContext.current
    val zone = ZoneId.of(data.settings.timezone)
    val all by remember(data.profile.uid) { repo.customerAppointmentsFlow(data.profile.uid) }
        .collectAsStateWithLifecycle(initialValue = emptyList())
    var open by remember { mutableStateOf<Appointment?>(null) }

    LaunchedEffect(all) { LocalReminders.sync(context, all, zone) }

    val now = System.currentTimeMillis()
    val upcoming = all.filter { it.endMillis >= now && it.status.isActive }.sortedBy { it.startMillis }
    val past = all.filter { it !in upcoming }.sortedByDescending { it.startMillis }

    open?.let { appt ->
        AppointmentDialog(
            appt = appt, role = data.profile.role, uid = data.profile.uid, settings = data.settings, repo = repo,
            onReschedule = { open = null; onReschedule(it) },
            onDismiss = { open = null },
        )
    }

    Scaffold(topBar = { TopAppBar(title = { Text("My bookings") }) }) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding)) {
            item { SectionTitle("Upcoming") }
            if (upcoming.isEmpty()) item { EmptyState("No upcoming bookings. Tap Book to make one.") }
            items(upcoming, key = { it.id }) { a ->
                AppointmentCard(a, zone, showCustomer = false) { open = a }
            }
            if (past.isNotEmpty()) {
                item { SectionTitle("Past and cancelled") }
                items(past, key = { it.id }) { a ->
                    AppointmentCard(a, zone, showCustomer = false) { open = a }
                }
            }
        }
    }
}
