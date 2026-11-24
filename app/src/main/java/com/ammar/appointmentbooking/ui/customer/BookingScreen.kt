package com.ammar.appointmentbooking.ui.customer

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.ammar.appointmentbooking.data.Appointment
import com.ammar.appointmentbooking.data.BookingRepository
import com.ammar.appointmentbooking.data.BookingRequest
import com.ammar.appointmentbooking.data.Role
import com.ammar.appointmentbooking.data.Service
import com.ammar.appointmentbooking.data.SlotTakenException
import com.ammar.appointmentbooking.data.Staff
import com.ammar.appointmentbooking.domain.Slot
import com.ammar.appointmentbooking.domain.SlotEngine
import com.ammar.appointmentbooking.ui.AppData
import com.ammar.appointmentbooking.ui.common.EmptyState
import com.ammar.appointmentbooking.util.Intents
import com.ammar.appointmentbooking.util.TimeFmt
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId

/**
 * Booking flow on one screen: pick staff (or Any) → date → free slot → confirm.
 * Also used for rescheduling, and by staff/admins to book walk-ins.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookingScreen(
    data: AppData,
    service: Service,
    rescheduling: Appointment?,
    repo: BookingRepository,
    onBack: () -> Unit,
    onDone: () -> Unit,
) {
    val settings = data.settings
    val profile = data.profile
    val zone = ZoneId.of(settings.timezone)
    val isStaffUser = profile.role != Role.CUSTOMER
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val eligibleStaff = data.staff.filter { it.active && it.canDo(service.id) }
    var selectedStaff by remember { mutableStateOf<Staff?>(null) } // null = any available
    val dates = remember(settings) {
        (0..settings.maxDaysAhead).map { LocalDate.now(zone).plusDays(it.toLong()) }
            .filter { SlotEngine.isBookableDay(it, settings, zone) }
    }
    var date by remember { mutableStateOf(dates.firstOrNull() ?: LocalDate.now(zone)) }
    var slots by remember { mutableStateOf<List<Slot>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var refresh by remember { mutableIntStateOf(0) }
    var chosen by remember { mutableStateOf<Slot?>(null) }
    var notes by remember { mutableStateOf("") }
    var walkInName by remember { mutableStateOf("") }
    var walkInPhone by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    var success by remember { mutableStateOf<Slot?>(null) }

    LaunchedEffect(date, selectedStaff, refresh, eligibleStaff.map { it.id }, settings) {
        loading = true
        chosen = null
        val candidates = selectedStaff?.let { listOf(it) } ?: eligibleStaff
        slots = try {
            candidates.flatMap { member ->
                val busyBlocks = repo.busyBlocks(member.id, date, ignoreAppointmentId = rescheduling?.id)
                val off = repo.timeOffFor(member.id)
                SlotEngine.freeSlots(date, service, member, settings, busyBlocks, off, ignoreNotice = isStaffUser)
            }
                .groupBy { it.startMin }      // "Any": one slot per time, first free staff member
                .map { it.value.first() }
                .sortedBy { it.startMin }
        } catch (e: Exception) {
            message = e.localizedMessage
            emptyList()
        }
        loading = false
    }

    fun confirm(slot: Slot) {
        if (isStaffUser && rescheduling == null && walkInName.isBlank()) {
            message = "Enter the ${settings.customerWord.lowercase()}'s name."
            return
        }
        busy = true
        message = null
        scope.launch {
            try {
                if (rescheduling != null) {
                    repo.reschedule(rescheduling, service, slot.staff, slot.date, slot.startMin, settings, profile.uid)
                } else {
                    val req = if (isStaffUser) BookingRequest(
                        service, slot.staff, slot.date, slot.startMin,
                        customerId = "walkin", customerName = walkInName.trim(),
                        customerPhone = walkInPhone.trim(), customerEmail = "",
                        notes = notes.trim(), bookedBy = profile.uid,
                    ) else BookingRequest(
                        service, slot.staff, slot.date, slot.startMin,
                        customerId = profile.uid, customerName = profile.name,
                        customerPhone = profile.phone, customerEmail = profile.email,
                        notes = notes.trim(), bookedBy = profile.uid,
                    )
                    repo.book(req, settings)
                }
                success = slot
            } catch (e: Exception) {
                message = if (e is SlotTakenException || e.cause is SlotTakenException)
                    SlotTakenException().message else e.localizedMessage
                refresh++
            } finally {
                busy = false
            }
        }
    }

    success?.let { slot ->
        val startMillis = TimeFmt.startOfDay(slot.date, zone) + slot.startMin * 60_000L
        val needsApproval = !settings.autoConfirm && !isStaffUser
        AlertDialog(
            onDismissRequest = onDone,
            title = { Text(if (needsApproval) "Request sent" else "You're booked!") },
            text = {
                Text(
                    "${service.name} with ${slot.staff.name}\n${TimeFmt.dayLong(slot.date)} at " +
                        TimeFmt.minuteLabel(slot.startMin) +
                        if (needsApproval) "\n\nWe'll notify you once it's confirmed." else ""
                )
            },
            confirmButton = { TextButton(onClick = onDone) { Text("Done") } },
            dismissButton = {
                if (!isStaffUser) TextButton(onClick = {
                    Intents.addToCalendar(
                        context, "${service.name} at ${settings.name}",
                        startMillis, startMillis + service.durationMin * 60_000L, settings.address
                    )
                }) { Text("Add to calendar") }
            },
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (rescheduling != null) "Reschedule ${service.name}" else service.name) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") }
                }
            )
        },
        bottomBar = {
            chosen?.let { slot ->
                Column(Modifier.fillMaxWidth().padding(16.dp)) {
                    message?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                    Button(
                        onClick = { confirm(slot) },
                        enabled = !busy,
                        modifier = Modifier.fillMaxWidth().height(52.dp)
                    ) {
                        Text(
                            if (busy) "Booking…"
                            else "Confirm ${TimeFmt.day(slot.date)}, ${TimeFmt.minuteLabel(slot.startMin)} · ${slot.staff.name}"
                        )
                    }
                }
            }
        }
    ) { padding ->
        LazyVerticalGrid(
            columns = GridCells.Adaptive(100.dp),
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Text(
                    "${service.durationMin} min · ${TimeFmt.money(service.price, settings.currency)}",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            if (isStaffUser && rescheduling == null) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(walkInName, { walkInName = it },
                            label = { Text("${settings.customerWord} name") }, singleLine = true,
                            modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(walkInPhone, { walkInPhone = it },
                            label = { Text("Phone (optional)") }, singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            modifier = Modifier.fillMaxWidth())
                    }
                }
            }
            item(span = { GridItemSpan(maxLineSpan) }) {
                Column {
                    Text("${settings.staffWord}", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        item {
                            FilterChip(selected = selectedStaff == null, onClick = { selectedStaff = null },
                                label = { Text("Any available") })
                        }
                        items(eligibleStaff, key = { it.id }) { member ->
                            FilterChip(selected = selectedStaff?.id == member.id,
                                onClick = { selectedStaff = member }, label = { Text(member.name) })
                        }
                    }
                }
            }
            item(span = { GridItemSpan(maxLineSpan) }) {
                Column {
                    Text("Date", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(dates, key = { it.toString() }) { d ->
                            FilterChip(selected = d == date, onClick = { date = d }, label = { Text(TimeFmt.day(d)) })
                        }
                    }
                }
            }
            item(span = { GridItemSpan(maxLineSpan) }) {
                Text("Available times", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            }
            when {
                loading -> item(span = { GridItemSpan(maxLineSpan) }) {
                    Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                eligibleStaff.isEmpty() -> item(span = { GridItemSpan(maxLineSpan) }) {
                    EmptyState("No ${settings.staffWord.lowercase()} offers this service yet.")
                }
                slots.isEmpty() -> item(span = { GridItemSpan(maxLineSpan) }) {
                    EmptyState("No free times on ${TimeFmt.day(date)}. Try another date.")
                }
                else -> items(slots, key = { "${it.startMin}_${it.staff.id}" }) { slot ->
                    FilterChip(
                        selected = chosen == slot,
                        onClick = { chosen = slot; message = null },
                        label = {
                            Text(TimeFmt.minuteLabel(slot.startMin), textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth())
                        },
                    )
                }
            }
            if (!isStaffUser || rescheduling == null) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    if (rescheduling == null) {
                        OutlinedTextField(notes, { notes = it }, label = { Text("Notes for us (optional)") },
                            modifier = Modifier.fillMaxWidth())
                    }
                }
            }
            item(span = { GridItemSpan(maxLineSpan) }) {
                if (chosen == null) message?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
            item(span = { GridItemSpan(maxLineSpan) }) { Row(Modifier.height(80.dp)) {} }
        }
    }
}
