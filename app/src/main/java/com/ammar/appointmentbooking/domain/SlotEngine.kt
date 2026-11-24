package com.ammar.appointmentbooking.domain

import com.ammar.appointmentbooking.data.BusinessSettings
import com.ammar.appointmentbooking.data.Service
import com.ammar.appointmentbooking.data.Staff
import com.ammar.appointmentbooking.data.TimeOff
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

data class Slot(val date: LocalDate, val startMin: Int, val staff: Staff)

/**
 * Works out free start times. A booking occupies (duration + buffer) minutes, split into
 * blocks of [BusinessSettings.slotStepMin]. Each taken block has a lock document in Firestore,
 * so [busyBlocks] is the set of block start minutes already locked for that staff member.
 */
object SlotEngine {

    val dateKey: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE

    fun blocksFor(startMin: Int, service: Service, step: Int): List<Int> {
        val total = service.durationMin + service.bufferMin
        val count = (total + step - 1) / step
        return (0 until count).map { startMin + it * step }
    }

    fun lockId(staffId: String, date: LocalDate, blockStartMin: Int): String =
        "${staffId}_${date.format(dateKey)}_$blockStartMin"

    fun isBookableDay(date: LocalDate, settings: BusinessSettings, zone: ZoneId): Boolean {
        val today = LocalDate.now(zone)
        if (date.isBefore(today)) return false
        if (date.isAfter(today.plusDays(settings.maxDaysAhead.toLong()))) return false
        return date.format(dateKey) !in settings.closedDates
    }

    fun freeSlots(
        date: LocalDate,
        service: Service,
        staff: Staff,
        settings: BusinessSettings,
        busyBlocks: Set<Int>,
        timeOff: List<TimeOff>,
        nowMillis: Long = System.currentTimeMillis(),
        ignoreNotice: Boolean = false,
    ): List<Slot> {
        val zone = ZoneId.of(settings.timezone)
        if (!isBookableDay(date, settings, zone)) return emptyList()
        val day = staff.hours[date.dayOfWeek.value] ?: return emptyList()
        val step = settings.slotStepMin
        val need = service.durationMin + service.bufferMin
        val earliest = if (ignoreNotice) nowMillis else nowMillis + settings.minNoticeMin * 60_000L
        val dayStartMillis = date.atStartOfDay(zone).toInstant().toEpochMilli()

        val slots = mutableListOf<Slot>()
        var t = day.startMin
        while (t + need <= day.endMin) {
            val startMillis = dayStartMillis + t * 60_000L
            val endMillis = startMillis + need * 60_000L
            val blocks = blocksFor(t, service, step)
            val free = blocks.none { it in busyBlocks } &&
                startMillis >= earliest &&
                timeOff.none { it.startMillis < endMillis && it.endMillis > startMillis }
            if (free) slots += Slot(date, t, staff)
            t += step
        }
        return slots
    }
}
