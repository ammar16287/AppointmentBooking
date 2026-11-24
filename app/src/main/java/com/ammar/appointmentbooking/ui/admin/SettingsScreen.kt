package com.ammar.appointmentbooking.ui.admin

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.ammar.appointmentbooking.data.BookingRepository
import com.ammar.appointmentbooking.ui.AppData
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(data: AppData, repo: BookingRepository, onBack: () -> Unit) {
    val s = data.settings
    val scope = rememberCoroutineScope()
    var name by remember(s) { mutableStateOf(s.name) }
    var type by remember(s) { mutableStateOf(s.type) }
    var phone by remember(s) { mutableStateOf(s.phone) }
    var email by remember(s) { mutableStateOf(s.email) }
    var address by remember(s) { mutableStateOf(s.address) }
    var timezone by remember(s) { mutableStateOf(s.timezone) }
    var currency by remember(s) { mutableStateOf(s.currency) }
    var minNotice by remember(s) { mutableStateOf((s.minNoticeMin / 60.0).toString().removeSuffix(".0")) }
    var maxDays by remember(s) { mutableStateOf(s.maxDaysAhead.toString()) }
    var cutoff by remember(s) { mutableStateOf((s.cancelCutoffMin / 60.0).toString().removeSuffix(".0")) }
    var autoConfirm by remember(s) { mutableStateOf(s.autoConfirm) }
    var step by remember(s) { mutableStateOf(s.slotStepMin.toString()) }
    var reminders by remember(s) { mutableStateOf(s.reminderOffsetsMin.joinToString(", ") { (it / 60.0).toString().removeSuffix(".0") }) }
    var closed by remember(s) { mutableStateOf(s.closedDates.joinToString(", ")) }
    var adminEmail by remember { mutableStateOf("") }
    var message by remember { mutableStateOf<String?>(null) }

    fun save() {
        val zoneOk = runCatching { ZoneId.of(timezone.trim()) }.isSuccess
        val dates = closed.split(",").map { it.trim() }.filter { it.isNotEmpty() }
        val datesOk = dates.all { runCatching { LocalDate.parse(it) }.isSuccess }
        val offsets = reminders.split(",").mapNotNull { it.trim().toDoubleOrNull() }.map { (it * 60).toInt() }
            .filter { it > 0 }.distinct().sortedDescending()
        val stepMin = step.toIntOrNull()
        if (name.isBlank() || !zoneOk || !datesOk || stepMin == null || stepMin !in 5..60) {
            message = "Check the name, time zone (e.g. Asia/Karachi), slot step (5–60) and holiday dates (yyyy-mm-dd)."
            return
        }
        val updated = s.copy(
            name = name.trim(), type = type, phone = phone.trim(), email = email.trim(), address = address.trim(),
            timezone = timezone.trim(), currency = currency.trim().ifBlank { "PKR" },
            minNoticeMin = ((minNotice.toDoubleOrNull() ?: 2.0) * 60).toInt(),
            maxDaysAhead = maxDays.toIntOrNull()?.coerceIn(1, 365) ?: 30,
            cancelCutoffMin = ((cutoff.toDoubleOrNull() ?: 4.0) * 60).toInt(),
            autoConfirm = autoConfirm, slotStepMin = stepMin,
            reminderOffsetsMin = offsets, closedDates = dates,
        )
        scope.launch {
            message = try { repo.saveSettings(updated); "Settings saved." } catch (e: Exception) { e.localizedMessage }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Business settings") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } }
            )
        }
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text("Business", fontWeight = FontWeight.SemiBold)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = type == "salon", onClick = { type = "salon" }, label = { Text("Salon") })
                FilterChip(selected = type == "clinic", onClick = { type = "clinic" }, label = { Text("Clinic") })
            }
            Field("Business name", name) { name = it }
            Field("Phone", phone, KeyboardType.Phone) { phone = it }
            Field("Email", email, KeyboardType.Email) { email = it }
            Field("Address", address) { address = it }
            Field("Time zone", timezone) { timezone = it }
            Field("Currency", currency) { currency = it }

            HorizontalDivider()
            Text("Booking rules", fontWeight = FontWeight.SemiBold)
            Field("Minimum notice (hours)", minNotice, KeyboardType.Decimal) { minNotice = it }
            Field("Book up to (days ahead)", maxDays, KeyboardType.Number) { maxDays = it }
            Field("Customers can cancel up to (hours before)", cutoff, KeyboardType.Decimal) { cutoff = it }
            Field("Slot step (minutes)", step, KeyboardType.Number) { step = it }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Auto-confirm bookings")
                    Text("Off = staff approve each request", style = MaterialTheme.typography.bodySmall)
                }
                Switch(checked = autoConfirm, onCheckedChange = { autoConfirm = it })
            }

            HorizontalDivider()
            Text("Reminders and holidays", fontWeight = FontWeight.SemiBold)
            Field("Send reminders (hours before, comma-separated)", reminders) { reminders = it }
            Field("Closed dates (yyyy-mm-dd, comma-separated)", closed) { closed = it }

            message?.let { Text(it, color = MaterialTheme.colorScheme.primary) }
            Button(onClick = { save() }, modifier = Modifier.fillMaxWidth()) { Text("Save settings") }

            HorizontalDivider()
            Text("Admins", fontWeight = FontWeight.SemiBold)
            Field("Email of a registered user", adminEmail, KeyboardType.Email) { adminEmail = it }
            OutlinedButton(
                onClick = { scope.launch { message = runCatching { repo.makeAdmin(adminEmail) }.getOrElse { it.localizedMessage } } },
                enabled = adminEmail.isNotBlank(),
                modifier = Modifier.fillMaxWidth()
            ) { Text("Make admin") }
        }
    }
}

@Composable
private fun Field(label: String, value: String, keyboard: KeyboardType = KeyboardType.Text, onChange: (String) -> Unit) {
    OutlinedTextField(
        value = value, onValueChange = onChange, label = { Text(label) }, singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = keyboard),
        modifier = Modifier.fillMaxWidth()
    )
}
