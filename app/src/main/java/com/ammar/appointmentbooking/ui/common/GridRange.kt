package com.ammar.appointmentbooking.ui.common

import com.ammar.appointmentbooking.data.Appointment
import com.ammar.appointmentbooking.data.Staff
import com.ammar.appointmentbooking.util.TimeFmt
import java.time.LocalDate
import java.time.ZoneId

/** Hours to show in a day grid: the working hours of these staff, widened to fit any booking. */
fun gridRange(staff: List<Staff>, date: LocalDate, appts: List<Appointment>, zone: ZoneId): Pair<Int, Int> {
    val days = staff.mapNotNull { it.hours[date.dayOfWeek.value] }
    var from = days.minOfOrNull { it.startMin } ?: (8 * 60)
    var to = days.maxOfOrNull { it.endMin } ?: (20 * 60)
    appts.forEach {
        from = minOf(from, TimeFmt.minuteOfDay(it.startMillis, zone))
        to = maxOf(to, TimeFmt.minuteOfDay(it.endMillis, zone))
    }
    from = (from / 60) * 60
    to = ((to + 59) / 60) * 60
    if (to <= from) to = from + 60
    return from to minOf(to, 24 * 60)
}
