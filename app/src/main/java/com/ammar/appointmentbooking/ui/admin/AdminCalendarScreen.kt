package com.ammar.appointmentbooking.ui.admin

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ammar.appointmentbooking.data.Appointment
import com.ammar.appointmentbooking.data.BookingRepository
import com.ammar.appointmentbooking.data.Status
import com.ammar.appointmentbooking.ui.AppData
import com.ammar.appointmentbooking.ui.common.AppointmentCard
import com.ammar.appointmentbooking.ui.common.AppointmentDialog
import com.ammar.appointmentbooking.ui.common.CalendarColumn
import com.ammar.appointmentbooking.ui.common.DateNavBar
import com.ammar.appointmentbooking.ui.common.DayTimeGrid
import com.ammar.appointmentbooking.ui.common.EmptyState
import com.ammar.appointmentbooking.ui.common.MonthGrid
import com.ammar.appointmentbooking.ui.common.SectionTitle
import com.ammar.appointmentbooking.ui.common.gridRange
import com.ammar.appointmentbooking.util.TimeFmt
import java.time.LocalDate
import java.time.ZoneId

private enum class CalMode(val label: String) { DAY("Day"), WEEK("Week"), MONTH("Month") }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminCalendarScreen(data: AppData, repo: BookingRepository, onReschedule: (Appointment) -> Unit) {
    val zone = ZoneId.of(data.settings.timezone)
    val today = LocalDate.now(zone)
    var mode by remember { mutableStateOf(CalMode.DAY) }
    var date by remember { mutableStateOf(today) }
    var open by remember { mutableStateOf<Appointment?>(null) }

    // Range loaded for the current view.
    val rangeStart = when (mode) {
        CalMode.DAY -> date
        CalMode.WEEK -> date.minusDays((date.dayOfWeek.value - 1).toLong())
        CalMode.MONTH -> date.withDayOfMonth(1)
    }
    val rangeEnd = when (mode) {
        CalMode.DAY -> rangeStart.plusDays(1)
        CalMode.WEEK -> rangeStart.plusDays(7)
        CalMode.MONTH -> rangeStart.plusMonths(1)
    }
    val flow = remember(rangeStart, rangeEnd) {
        repo.appointmentsInRangeFlow(TimeFmt.startOfDay(rangeStart, zone), TimeFmt.startOfDay(rangeEnd, zone))
    }
    val appts by flow.collectAsStateWithLifecycle(initialValue = emptyList())
    val active = appts.filter { it.status != Status.CANCELLED }

    open?.let { appt ->
        AppointmentDialog(
            appt, data.profile.role, data.profile.uid, data.settings, repo,
            onReschedule = { open = null; onReschedule(it) }, onDismiss = { open = null }
        )
    }

    Scaffold(topBar = { TopAppBar(title = { Text("Calendar") }) }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            Row(Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CalMode.entries.forEach { m ->
                    FilterChip(selected = mode == m, onClick = { mode = m }, label = { Text(m.label) })
                }
            }
            val label = when (mode) {
                CalMode.DAY -> TimeFmt.dayLong(date)
                CalMode.WEEK -> "${TimeFmt.day(rangeStart)} – ${TimeFmt.day(rangeEnd.minusDays(1))}"
                CalMode.MONTH -> TimeFmt.month(date)
            }
            DateNavBar(
                label,
                onPrev = {
                    date = when (mode) {
                        CalMode.DAY -> date.minusDays(1)
                        CalMode.WEEK -> date.minusWeeks(1)
                        CalMode.MONTH -> date.minusMonths(1)
                    }
                },
                onNext = {
                    date = when (mode) {
                        CalMode.DAY -> date.plusDays(1)
                        CalMode.WEEK -> date.plusWeeks(1)
                        CalMode.MONTH -> date.plusMonths(1)
                    }
                },
                onToday = { date = today },
            )
            val pending = active.count { it.status == Status.PENDING }
            val revenue = active.filter { it.status == Status.COMPLETED }.sumOf { it.price }
            Text(
                "${active.size} bookings · $pending pending · completed ${TimeFmt.money(revenue, data.settings.currency)}",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )

            when (mode) {
                CalMode.DAY -> {
                    val staffList = data.staff.filter { it.active }
                    if (staffList.isEmpty()) {
                        EmptyState("Add staff under Manage → Staff to see the day view.")
                    } else {
                        val columns = staffList.map { s -> CalendarColumn(s.name, active.filter { it.staffId == s.id }) }
                        val (from, to) = gridRange(staffList, date, active, zone)
                        DayTimeGrid(columns, zone, from, to, onClick = { open = it })
                    }
                }
                CalMode.WEEK -> {
                    val byDay = active.groupBy { TimeFmt.zdt(it.startMillis, zone).toLocalDate() }
                    LazyColumn(Modifier.fillMaxSize()) {
                        (0 until 7).forEach { i ->
                            val d = rangeStart.plusDays(i.toLong())
                            val list = byDay[d].orEmpty().sortedBy { it.startMillis }
                            item(key = "h$d") { SectionTitle("${TimeFmt.day(d)} · ${list.size}") }
                            items(list, key = { it.id }) { a ->
                                AppointmentCard(a, zone, showCustomer = true) { open = a }
                            }
                        }
                    }
                }
                CalMode.MONTH -> {
                    val counts = active.groupingBy { TimeFmt.zdt(it.startMillis, zone).toLocalDate() }.eachCount()
                    MonthGrid(date, counts, today) { d ->
                        date = d
                        mode = CalMode.DAY
                    }
                }
            }
        }
    }
}
