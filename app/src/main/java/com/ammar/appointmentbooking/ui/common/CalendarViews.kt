package com.ammar.appointmentbooking.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ammar.appointmentbooking.data.Appointment
import com.ammar.appointmentbooking.ui.theme.color
import com.ammar.appointmentbooking.util.TimeFmt
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.TextStyle
import java.util.Locale

data class CalendarColumn(val title: String, val appointments: List<Appointment>)

/** Bar with ‹ label › and a Today button. */
@Composable
fun DateNavBar(label: String, onPrev: () -> Unit, onNext: () -> Unit, onToday: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onPrev) { Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, "Previous") }
        Text(label, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold,
            modifier = Modifier.weight(1f))
        TextButton(onClick = onToday) { Text("Today") }
        IconButton(onClick = onNext) { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, "Next") }
    }
}

/**
 * Day view with one column per staff member (resource view) and 15-minute precision.
 * Appointments are drawn as coloured blocks by status; tap one to open it.
 */
@Composable
fun DayTimeGrid(
    columns: List<CalendarColumn>,
    zone: ZoneId,
    fromMin: Int,
    toMin: Int,
    onClick: (Appointment) -> Unit,
    modifier: Modifier = Modifier,
) {
    val hourHeight = 72.dp
    val minuteHeight = hourHeight / 60
    val totalHeight = minuteHeight * (toMin - fromMin)
    val labelWidth = 56.dp
    val gridLine = MaterialTheme.colorScheme.outlineVariant

    BoxWithConstraints(modifier.fillMaxSize()) {
        val count = columns.size.coerceAtLeast(1)
        val fitWidth = (maxWidth - labelWidth) / count
        val colWidth = if (fitWidth < 140.dp) 140.dp else fitWidth
        val vScroll = rememberScrollState()

        Column(Modifier.horizontalScroll(rememberScrollState())) {
            Row {
                Spacer(Modifier.width(labelWidth))
                columns.forEach { col ->
                    Text(
                        col.title, modifier = Modifier.width(colWidth).padding(8.dp),
                        style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold,
                        maxLines = 1, overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            HorizontalDivider()
            Row(Modifier.weight(1f).verticalScroll(vScroll)) {
                // Hour labels
                Box(Modifier.width(labelWidth).height(totalHeight)) {
                    var h = (fromMin + 59) / 60 * 60
                    while (h < toMin) {
                        Text(
                            TimeFmt.minuteLabel(h),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.offset(y = minuteHeight * (h - fromMin)).padding(start = 4.dp)
                        )
                        h += 60
                    }
                }
                columns.forEach { col ->
                    Box(
                        Modifier.width(colWidth).height(totalHeight)
                            .border(0.5.dp, gridLine)
                    ) {
                        var h = (fromMin + 59) / 60 * 60
                        while (h < toMin) {
                            HorizontalDivider(
                                Modifier.offset(y = minuteHeight * (h - fromMin)),
                                color = gridLine
                            )
                            h += 60
                        }
                        col.appointments.forEach { a ->
                            val start = TimeFmt.minuteOfDay(a.startMillis, zone).coerceIn(fromMin, toMin)
                            val end = TimeFmt.minuteOfDay(a.endMillis, zone).coerceIn(start + 15, toMin + 15)
                            val c = a.status.color()
                            Column(
                                Modifier
                                    .offset(y = minuteHeight * (start - fromMin))
                                    .height(minuteHeight * (end - start))
                                    .fillMaxWidth()
                                    .padding(2.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(c.copy(alpha = 0.18f))
                                    .border(1.dp, c, RoundedCornerShape(6.dp))
                                    .clickable { onClick(a) }
                                    .padding(4.dp)
                            ) {
                                Text(
                                    "${TimeFmt.time(a.startMillis, zone)} ${a.serviceName}",
                                    style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold,
                                    maxLines = 1, overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    a.customerName, style = MaterialTheme.typography.labelSmall,
                                    maxLines = 1, overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/** Month grid with the number of bookings per day. Tap a day to open it. */
@Composable
fun MonthGrid(
    month: LocalDate,
    countsByDate: Map<LocalDate, Int>,
    today: LocalDate,
    onDay: (LocalDate) -> Unit,
) {
    val first = month.withDayOfMonth(1)
    val leading = first.dayOfWeek.value - 1 // Monday first
    val days = first.lengthOfMonth()
    val cells = leading + days
    val rows = (cells + 6) / 7
    Column(Modifier.fillMaxWidth().padding(8.dp)) {
        Row {
            DayOfWeek.values().forEach {
                Text(
                    it.getDisplayName(TextStyle.SHORT, Locale.getDefault()),
                    modifier = Modifier.weight(1f).padding(4.dp),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        for (r in 0 until rows) {
            Row {
                for (c in 0 until 7) {
                    val index = r * 7 + c - leading
                    if (index < 0 || index >= days) {
                        Spacer(Modifier.weight(1f).aspectRatio(1f))
                    } else {
                        val date = first.plusDays(index.toLong())
                        val count = countsByDate[date] ?: 0
                        val isToday = date == today
                        Column(
                            Modifier.weight(1f).aspectRatio(1f).padding(2.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    if (isToday) MaterialTheme.colorScheme.primaryContainer
                                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                )
                                .clickable { onDay(date) }
                                .padding(4.dp),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("${date.dayOfMonth}", style = MaterialTheme.typography.labelLarge)
                            if (count > 0) {
                                Text(
                                    "$count",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.clip(RoundedCornerShape(50))
                                        .background(MaterialTheme.colorScheme.primary)
                                        .padding(horizontal = 6.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
