package com.ammar.appointmentbooking.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.ammar.appointmentbooking.data.Appointment
import com.ammar.appointmentbooking.data.BookingRepository
import com.ammar.appointmentbooking.data.BusinessSettings
import com.ammar.appointmentbooking.data.Role
import com.ammar.appointmentbooking.data.Status
import com.ammar.appointmentbooking.ui.theme.color
import com.ammar.appointmentbooking.util.Intents
import com.ammar.appointmentbooking.util.TimeFmt
import kotlinx.coroutines.launch
import java.time.ZoneId

@Composable
fun LoadingBox(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
}

@Composable
fun EmptyState(text: String, modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
        Text(text, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
        modifier = modifier.padding(horizontal = 16.dp, vertical = 8.dp)
    )
}

@Composable
fun StatusChip(status: Status) {
    val c = status.color()
    Text(
        status.label,
        color = c,
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(c.copy(alpha = 0.12f))
            .padding(horizontal = 10.dp, vertical = 4.dp)
    )
}

@Composable
fun AppointmentCard(
    appt: Appointment,
    zone: ZoneId,
    showCustomer: Boolean,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp).clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(10.dp).clip(CircleShape).background(appt.status.color()))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(appt.serviceName, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Text(
                    TimeFmt.dayTime(appt.startMillis, zone) + " – " + TimeFmt.time(appt.endMillis, zone),
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    if (showCustomer) "${appt.customerName} · with ${appt.staffName}" else "with ${appt.staffName}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            StatusChip(appt.status)
        }
    }
}

/**
 * Details of one appointment with the actions the current role may take.
 * Customers can cancel or reschedule before the cut-off; staff and admins manage status and notes.
 */
@Composable
fun AppointmentDialog(
    appt: Appointment,
    role: Role,
    uid: String,
    settings: BusinessSettings,
    repo: BookingRepository,
    onReschedule: (Appointment) -> Unit,
    onDismiss: () -> Unit,
) {
    val zone = ZoneId.of(settings.timezone)
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var busy by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    var notes by remember { mutableStateOf(appt.notes) }
    var confirmCancel by remember { mutableStateOf(false) }

    val isStaff = role != Role.CUSTOMER
    val beforeCutoff = appt.startMillis - System.currentTimeMillis() > settings.cancelCutoffMin * 60_000L
    val canChange = appt.status == Status.PENDING || appt.status == Status.CONFIRMED
    val customerCanChange = canChange && beforeCutoff

    fun act(block: suspend () -> Unit) {
        busy = true
        scope.launch {
            try {
                block(); onDismiss()
            } catch (e: Exception) {
                message = e.message ?: "Something went wrong"
            } finally {
                busy = false
            }
        }
    }

    if (confirmCancel) {
        AlertDialog(
            onDismissRequest = { confirmCancel = false },
            title = { Text("Cancel this booking?") },
            text = { Text("The time slot will be released for others.") },
            confirmButton = {
                TextButton(onClick = { confirmCancel = false; act { repo.cancel(appt, uid) } }) { Text("Yes, cancel") }
            },
            dismissButton = { TextButton(onClick = { confirmCancel = false }) { Text("Keep it") } },
        )
        return
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(appt.serviceName) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                StatusChip(appt.status)
                Text(TimeFmt.dayTime(appt.startMillis, zone) + " – " + TimeFmt.time(appt.endMillis, zone))
                Text("${settings.staffWord}: ${appt.staffName}")
                if (isStaff) {
                    Text("${settings.customerWord}: ${appt.customerName}")
                    if (appt.customerPhone.isNotBlank()) {
                        Text(
                            "Call ${appt.customerPhone}",
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.clickable { Intents.dial(context, appt.customerPhone) }
                        )
                    }
                }
                Text("Price: ${TimeFmt.money(appt.price, settings.currency)}")
                if (isStaff) {
                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text(if (settings.isClinic) "Visit notes" else "Style notes") },
                        modifier = Modifier.fillMaxWidth().height(110.dp),
                    )
                } else if (appt.notes.isNotBlank()) {
                    Text("Notes: ${appt.notes}", style = MaterialTheme.typography.bodySmall)
                }
                if (!isStaff && canChange && !beforeCutoff) {
                    Text(
                        "Changes close ${settings.cancelCutoffMin / 60} h before the appointment. Please call us.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
                message?.let { Text(it, color = MaterialTheme.colorScheme.error) }

                // Action buttons
                Spacer(Modifier.height(4.dp))
                if (isStaff) {
                    if (appt.status == Status.PENDING)
                        ActionButton("Approve", busy) { act { repo.setStatus(appt, Status.CONFIRMED, uid) } }
                    if (appt.status == Status.CONFIRMED)
                        ActionButton("Check in", busy) { act { repo.setStatus(appt, Status.CHECKED_IN, uid) } }
                    if (appt.status == Status.CONFIRMED || appt.status == Status.CHECKED_IN)
                        ActionButton("Mark completed", busy) { act { repo.setStatus(appt, Status.COMPLETED, uid) } }
                    if (appt.status == Status.CONFIRMED)
                        ActionButton("Mark no-show", busy) { act { repo.setStatus(appt, Status.NO_SHOW, uid) } }
                    if (notes != appt.notes)
                        ActionButton("Save notes", busy) { act { repo.saveNotes(appt, notes, uid) } }
                }
                if ((isStaff && canChange) || (!isStaff && customerCanChange)) {
                    ActionButton("Reschedule", busy) { onReschedule(appt) }
                    ActionButton("Cancel booking", busy) { confirmCancel = true }
                }
                if (!isStaff && appt.status.isActive) {
                    ActionButton("Add to my calendar", busy) {
                        Intents.addToCalendar(
                            context, "${appt.serviceName} at ${settings.name}",
                            appt.startMillis, appt.endMillis, settings.address
                        )
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } },
    )
}

@Composable
private fun ActionButton(text: String, busy: Boolean, onClick: () -> Unit) {
    OutlinedButton(onClick = onClick, enabled = !busy, modifier = Modifier.fillMaxWidth()) { Text(text) }
}
