package com.ammar.appointmentbooking.ui.staff

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ammar.appointmentbooking.data.Appointment
import com.ammar.appointmentbooking.data.BookingRepository
import com.ammar.appointmentbooking.data.Role
import com.ammar.appointmentbooking.data.Status
import com.ammar.appointmentbooking.ui.AppData
import com.ammar.appointmentbooking.ui.common.AppointmentDialog
import com.ammar.appointmentbooking.ui.common.CalendarColumn
import com.ammar.appointmentbooking.ui.common.DateNavBar
import com.ammar.appointmentbooking.ui.common.DayTimeGrid
import com.ammar.appointmentbooking.ui.common.EmptyState
import com.ammar.appointmentbooking.ui.common.gridRange
import com.ammar.appointmentbooking.util.TimeFmt
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StaffScheduleScreen(
    data: AppData,
    repo: BookingRepository,
    onReschedule: (Appointment) -> Unit,
    onTimeOff: () -> Unit,
) {
    val zone = ZoneId.of(data.settings.timezone)
    val me = data.staff.firstOrNull { it.id == data.profile.staffId }
    var date by remember { mutableStateOf(LocalDate.now(zone)) }
    var open by remember { mutableStateOf<Appointment?>(null) }

    val from = TimeFmt.startOfDay(date, zone)
    val dayFlow = remember(date) { repo.appointmentsInRangeFlow(from, from + 86_400_000L) }
    val dayAppts by dayFlow.collectAsStateWithLifecycle(initialValue = emptyList())
    val mine = dayAppts.filter { it.staffId == me?.id && it.status != Status.CANCELLED }

    open?.let { appt ->
        AppointmentDialog(
            appt, data.profile.role, data.profile.uid, data.settings, repo,
            onReschedule = { open = null; onReschedule(it) }, onDismiss = { open = null }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My schedule") },
                actions = { TextButton(onClick = onTimeOff) { Text("Time off") } }
            )
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            if (me == null) {
                EmptyState("Your account is not linked to a ${data.settings.staffWord.lowercase()} profile yet. Ask the admin to link it under Manage → Staff.")
                return@Column
            }
            DateNavBar(
                TimeFmt.dayLong(date),
                onPrev = { date = date.minusDays(1) },
                onNext = { date = date.plusDays(1) },
                onToday = { date = LocalDate.now(zone) },
            )
            val pending = mine.count { it.status == Status.PENDING }
            Text(
                "${mine.size} appointments" + if (pending > 0) " · $pending waiting for approval" else "",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            val (gFrom, gTo) = gridRange(listOf(me), date, mine, zone)
            DayTimeGrid(
                columns = listOf(CalendarColumn(me.name, mine)),
                zone = zone, fromMin = gFrom, toMin = gTo,
                onClick = { open = it },
            )
        }
    }
}

/** Staff block their own time; admins can block time for anyone. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimeOffScreen(data: AppData, repo: BookingRepository, onBack: () -> Unit) {
    val zone = ZoneId.of(data.settings.timezone)
    val isAdmin = data.profile.role == Role.ADMIN
    val all by remember { repo.timeOffFlow() }.collectAsStateWithLifecycle(initialValue = emptyList())
    val visible = all.filter { isAdmin || it.staffId == data.profile.staffId }
        .filter { it.endMillis > System.currentTimeMillis() }
    var adding by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    if (adding) {
        AddTimeOffDialog(data, zone, isAdmin, onDismiss = { adding = false }) { staffId, start, end, reason ->
            scope.launch { runCatching { repo.addTimeOff(staffId, start, end, reason) } }
            adding = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Time off") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { adding = true }) { Icon(Icons.Filled.Add, "Add time off") }
        }
    ) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding)) {
            item {
                Text(
                    "Blocked time can't be booked. Existing bookings inside it stay — reschedule or cancel them from the calendar.",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(16.dp)
                )
            }
            if (visible.isEmpty()) item { EmptyState("No upcoming time off.") }
            items(visible, key = { it.id }) { t ->
                val who = data.staff.firstOrNull { it.id == t.staffId }?.name ?: "Unknown"
                Card(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp)) {
                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(who, fontWeight = FontWeight.SemiBold)
                            Text("${TimeFmt.dayTime(t.startMillis, zone)} → ${TimeFmt.dayTime(t.endMillis, zone)}")
                            if (t.reason.isNotBlank()) Text(t.reason, style = MaterialTheme.typography.bodySmall)
                        }
                        IconButton(onClick = { scope.launch { runCatching { repo.deleteTimeOff(t.id) } } }) {
                            Icon(Icons.Filled.Delete, "Remove")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AddTimeOffDialog(
    data: AppData,
    zone: ZoneId,
    isAdmin: Boolean,
    onDismiss: () -> Unit,
    onSave: (staffId: String, start: Long, end: Long, reason: String) -> Unit,
) {
    var staffId by remember { mutableStateOf(data.profile.staffId ?: data.staff.firstOrNull()?.id.orEmpty()) }
    var dateText by remember { mutableStateOf(LocalDate.now(zone).toString()) }
    var fromText by remember { mutableStateOf("13:00") }
    var toText by remember { mutableStateOf("14:00") }
    var days by remember { mutableStateOf("1") }
    var reason by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Block time") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (isAdmin) {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(data.staff, key = { it.id }) { s ->
                            FilterChip(selected = staffId == s.id, onClick = { staffId = s.id }, label = { Text(s.name) })
                        }
                    }
                }
                OutlinedTextField(dateText, { dateText = it }, label = { Text("Start date (yyyy-mm-dd)") }, singleLine = true)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(fromText, { fromText = it }, label = { Text("From (HH:mm)") },
                        singleLine = true, modifier = Modifier.weight(1f))
                    OutlinedTextField(toText, { toText = it }, label = { Text("To (HH:mm)") },
                        singleLine = true, modifier = Modifier.weight(1f))
                }
                OutlinedTextField(days, { days = it }, label = { Text("Number of days") }, singleLine = true)
                OutlinedTextField(reason, { reason = it }, label = { Text("Reason (optional)") }, singleLine = true)
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val d = runCatching { LocalDate.parse(dateText.trim()) }.getOrNull()
                val f = TimeFmt.parseHm(fromText)
                val t = TimeFmt.parseHm(toText)
                val n = days.toIntOrNull()?.coerceIn(1, 60)
                if (staffId.isBlank() || d == null || f == null || t == null || n == null) {
                    error = "Check the date and times."
                } else {
                    val start = TimeFmt.startOfDay(d, zone) + f * 60_000L
                    // Multi-day: blocks from the start time on day 1 to the end time on the last day.
                    val end = TimeFmt.startOfDay(d.plusDays((n - 1).toLong()), zone) + t * 60_000L
                    if (end <= start) error = "End must be after start." else onSave(staffId, start, end, reason.trim())
                }
            }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
