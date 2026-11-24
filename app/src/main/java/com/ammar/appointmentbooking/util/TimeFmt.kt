package com.ammar.appointmentbooking.util

import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

object TimeFmt {
    private val time = DateTimeFormatter.ofPattern("h:mm a", Locale.getDefault())
    private val day = DateTimeFormatter.ofPattern("EEE, d MMM", Locale.getDefault())
    private val dayLong = DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy", Locale.getDefault())
    private val month = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault())

    fun zdt(millis: Long, zone: ZoneId): ZonedDateTime = Instant.ofEpochMilli(millis).atZone(zone)

    fun minuteLabel(min: Int): String = LocalTime.of((min / 60) % 24, min % 60).format(time)
    fun time(millis: Long, zone: ZoneId): String = zdt(millis, zone).format(time)
    fun day(date: LocalDate): String = date.format(day)
    fun dayLong(date: LocalDate): String = date.format(dayLong)
    fun month(date: LocalDate): String = date.format(month)
    fun dayTime(millis: Long, zone: ZoneId): String = "${zdt(millis, zone).format(day)}, ${time(millis, zone)}"

    fun minuteOfDay(millis: Long, zone: ZoneId): Int = zdt(millis, zone).let { it.hour * 60 + it.minute }

    fun startOfDay(date: LocalDate, zone: ZoneId): Long = date.atStartOfDay(zone).toInstant().toEpochMilli()

    fun money(amount: Double, currency: String): String =
        if (amount % 1.0 == 0.0) "$currency ${amount.toLong()}" else "$currency ${"%.2f".format(amount)}"

    /** Parses "09:30" into minutes from midnight. */
    fun parseHm(text: String): Int? {
        val parts = text.trim().split(":")
        if (parts.size != 2) return null
        val h = parts[0].toIntOrNull() ?: return null
        val m = parts[1].toIntOrNull() ?: return null
        if (h !in 0..24 || m !in 0..59) return null
        return h * 60 + m
    }

    fun hm(min: Int): String = "%02d:%02d".format(min / 60, min % 60)
}
