package com.ammar.appointmentbooking.ui.admin

import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.ui.draw.clip
import com.ammar.appointmentbooking.ui.theme.BrandColors
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
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
import com.ammar.appointmentbooking.data.Service
import com.ammar.appointmentbooking.data.Staff
import com.ammar.appointmentbooking.data.WorkDay
import com.ammar.appointmentbooking.ui.AppData
import com.ammar.appointmentbooking.ui.common.EmptyState
import com.ammar.appointmentbooking.util.TimeFmt
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.format.TextStyle
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManageHubScreen(onServices: () -> Unit, onStaff: () -> Unit, onTimeOff: () -> Unit, onSettings: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(title = {
                Column {
                    Text("Manage", style = MaterialTheme.typography.headlineSmall)
                    Text("Set up your business", style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            })
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            HubCard(Icons.Filled.Star, BrandColors.tints[0], "Services", "Names, durations, buffers and prices", onServices)
            HubCard(Icons.Filled.Person, BrandColors.tints[1], "Staff", "People, skills, working hours, app login", onStaff)
            HubCard(Icons.Filled.DateRange, BrandColors.tints[3], "Time off", "Leave, breaks and blocked time", onTimeOff)
            HubCard(Icons.Filled.Settings, BrandColors.tints[4], "Business settings", "Booking rules, reminders, holidays, admins", onSettings)
        }
    }
}

@Composable
private fun HubCard(icon: androidx.compose.ui.graphics.vector.ImageVector, tint: androidx.compose.ui.graphics.Color,
                    title: String, subtitle: String, onClick: () -> Unit) {
    Card(
        Modifier.fillMaxWidth().clip(MaterialTheme.shapes.medium).clickable(onClick = onClick),
        shape = MaterialTheme.shapes.medium,
        colors = androidx.compose.material3.CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = androidx.compose.material3.CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(48.dp).clip(MaterialTheme.shapes.medium).background(tint.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center,
            ) { Icon(icon, null, tint = tint) }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BackScaffold(
    title: String,
    onBack: () -> Unit,
    onAdd: (() -> Unit)? = null,
    content: @Composable (Modifier) -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } }
            )
        },
        floatingActionButton = {
            if (onAdd != null) FloatingActionButton(onClick = onAdd) { Icon(Icons.Filled.Add, "Add") }
        }
    ) { padding -> content(Modifier.fillMaxSize().padding(padding)) }
}

// ---------------- Services ----------------

@Composable
fun ManageServicesScreen(data: AppData, repo: BookingRepository, onBack: () -> Unit) {
    var editing by remember { mutableStateOf<Service?>(null) }
    val scope = rememberCoroutineScope()

    editing?.let { s ->
        ServiceDialog(
            initial = s,
            currency = data.settings.currency,
            onDismiss = { editing = null },
            onDelete = if (s.id.isBlank()) null else ({ scope.launch { runCatching { repo.deleteService(s.id) } }; editing = null }),
            onSave = { scope.launch { runCatching { repo.saveService(it) } }; editing = null },
        )
    }

    BackScaffold("Services", onBack, onAdd = { editing = Service() }) { modifier ->
        LazyColumn(modifier) {
            if (data.services.isEmpty()) item { EmptyState("No services yet. Tap + to add your first one.") }
            items(data.services, key = { it.id }) { s ->
                Card(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp).clickable { editing = s }) {
                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(s.name, fontWeight = FontWeight.SemiBold)
                            Text("${s.durationMin} min + ${s.bufferMin} min buffer" + if (!s.active) " · hidden" else "",
                                style = MaterialTheme.typography.bodySmall)
                        }
                        Text(TimeFmt.money(s.price, data.settings.currency))
                    }
                }
            }
        }
    }
}

@Composable
private fun ServiceDialog(
    initial: Service,
    currency: String,
    onDismiss: () -> Unit,
    onDelete: (() -> Unit)?,
    onSave: (Service) -> Unit,
) {
    var name by remember { mutableStateOf(initial.name) }
    var description by remember { mutableStateOf(initial.description) }
    var duration by remember { mutableStateOf(initial.durationMin.toString()) }
    var buffer by remember { mutableStateOf(initial.bufferMin.toString()) }
    var price by remember { mutableStateOf(if (initial.price == 0.0) "" else initial.price.toString()) }
    var active by remember { mutableStateOf(initial.active) }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial.id.isBlank()) "New service" else "Edit service") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(name, { name = it }, label = { Text("Name") }, singleLine = true)
                OutlinedTextField(description, { description = it }, label = { Text("Description") })
                OutlinedTextField(duration, { duration = it }, label = { Text("Duration (minutes)") }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                OutlinedTextField(buffer, { buffer = it }, label = { Text("Buffer after (minutes)") }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                OutlinedTextField(price, { price = it }, label = { Text("Price ($currency)") }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Show to customers", Modifier.weight(1f))
                    Switch(checked = active, onCheckedChange = { active = it })
                }
                if (onDelete != null) TextButton(onClick = onDelete) {
                    Text("Delete service", color = MaterialTheme.colorScheme.error)
                }
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val d = duration.toIntOrNull()
                val b = buffer.toIntOrNull() ?: 0
                val p = price.toDoubleOrNull() ?: 0.0
                if (name.isBlank() || d == null || d !in 5..600 || b < 0) error = "Enter a name and a duration of 5–600 minutes."
                else onSave(initial.copy(name = name.trim(), description = description.trim(),
                    durationMin = d, bufferMin = b, price = p, active = active))
            }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

// ---------------- Staff ----------------

@Composable
fun ManageStaffScreen(data: AppData, repo: BookingRepository, onBack: () -> Unit) {
    var editing by remember { mutableStateOf<Staff?>(null) }
    var info by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    editing?.let { s ->
        StaffDialog(
            initial = s,
            services = data.services,
            staffWord = data.settings.staffWord,
            onDismiss = { editing = null },
            onDelete = if (s.id.isBlank()) null else ({ scope.launch { runCatching { repo.deleteStaff(s.id) } }; editing = null }),
            onSave = { updated, linkEmail ->
                editing = null
                scope.launch {
                    info = try {
                        val id = repo.saveStaff(updated)
                        if (linkEmail.isNotBlank() && (linkEmail != updated.email || updated.userId == null)) {
                            repo.linkStaffAccount(updated.copy(id = id), linkEmail)
                        } else "Saved."
                    } catch (e: Exception) {
                        e.localizedMessage
                    }
                }
            },
        )
    }

    BackScaffold("Staff", onBack, onAdd = { editing = Staff() }) { modifier ->
        LazyColumn(modifier) {
            info?.let { msg -> item { Text(msg, Modifier.padding(16.dp), color = MaterialTheme.colorScheme.primary) } }
            if (data.staff.isEmpty()) item { EmptyState("No staff yet. Tap + to add someone.") }
            items(data.staff, key = { it.id }) { s ->
                Card(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp).clickable { editing = s }) {
                    Column(Modifier.padding(16.dp)) {
                        Text(s.name + if (!s.active) " (inactive)" else "", fontWeight = FontWeight.SemiBold)
                        if (s.title.isNotBlank()) Text(s.title, style = MaterialTheme.typography.bodySmall)
                        Text(
                            if (s.userId != null) "App login: ${s.email}" else "No app login linked",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StaffDialog(
    initial: Staff,
    services: List<Service>,
    staffWord: String,
    onDismiss: () -> Unit,
    onDelete: (() -> Unit)?,
    onSave: (Staff, String) -> Unit,
) {
    var name by remember { mutableStateOf(initial.name) }
    var title by remember { mutableStateOf(initial.title) }
    var email by remember { mutableStateOf(initial.email) }
    var active by remember { mutableStateOf(initial.active) }
    val serviceIds = remember { mutableStateMapOf<String, Boolean>().apply { initial.serviceIds.forEach { put(it, true) } } }
    // Per weekday: on/off + "HH:mm" text
    val dayOn = remember { mutableStateMapOf<Int, Boolean>().apply { (1..7).forEach { put(it, initial.hours.containsKey(it)) } } }
    val dayFrom = remember { mutableStateMapOf<Int, String>().apply { (1..7).forEach { put(it, TimeFmt.hm(initial.hours[it]?.startMin ?: 540)) } } }
    val dayTo = remember { mutableStateMapOf<Int, String>().apply { (1..7).forEach { put(it, TimeFmt.hm(initial.hours[it]?.endMin ?: 1020)) } } }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial.id.isBlank()) "New $staffWord" else "Edit $staffWord") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(name, { name = it }, label = { Text("Name") }, singleLine = true)
                OutlinedTextField(title, { title = it }, label = { Text("Title (e.g. Senior stylist)") }, singleLine = true)
                OutlinedTextField(
                    email, { email = it }, label = { Text("Login email (to link their account)") }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Active", Modifier.weight(1f))
                    Switch(checked = active, onCheckedChange = { active = it })
                }
                HorizontalDivider()
                Text("Services (none ticked = all)", fontWeight = FontWeight.SemiBold)
                services.forEach { s ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = serviceIds[s.id] == true, onCheckedChange = { serviceIds[s.id] = it })
                        Text(s.name)
                    }
                }
                HorizontalDivider()
                Text("Working hours", fontWeight = FontWeight.SemiBold)
                (1..7).forEach { d ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = dayOn[d] == true, onCheckedChange = { dayOn[d] = it })
                        Text(
                            DayOfWeek.of(d).getDisplayName(TextStyle.SHORT, Locale.getDefault()),
                            Modifier.width(40.dp)
                        )
                        if (dayOn[d] == true) {
                            OutlinedTextField(dayFrom[d].orEmpty(), { dayFrom[d] = it }, singleLine = true,
                                modifier = Modifier.weight(1f))
                            Text(" – ")
                            OutlinedTextField(dayTo[d].orEmpty(), { dayTo[d] = it }, singleLine = true,
                                modifier = Modifier.weight(1f))
                        } else {
                            Text("Day off", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
                if (onDelete != null) TextButton(onClick = onDelete) {
                    Text("Delete $staffWord", color = MaterialTheme.colorScheme.error)
                }
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val hours = mutableMapOf<Int, WorkDay>()
                var bad = false
                (1..7).filter { dayOn[it] == true }.forEach { d ->
                    val f = TimeFmt.parseHm(dayFrom[d].orEmpty())
                    val t = TimeFmt.parseHm(dayTo[d].orEmpty())
                    if (f == null || t == null || t <= f) bad = true else hours[d] = WorkDay(f, t)
                }
                when {
                    name.isBlank() -> error = "Enter a name."
                    bad -> error = "Use HH:mm times, with the end after the start."
                    else -> onSave(
                        initial.copy(
                            name = name.trim(), title = title.trim(), active = active,
                            serviceIds = serviceIds.filterValues { it }.keys.toList(),
                            hours = hours,
                        ),
                        email.trim(),
                    )
                }
            }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
