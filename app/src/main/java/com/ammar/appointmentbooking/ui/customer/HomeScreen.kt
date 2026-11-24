package com.ammar.appointmentbooking.ui.customer

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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ammar.appointmentbooking.data.Appointment
import com.ammar.appointmentbooking.data.BookingRepository
import com.ammar.appointmentbooking.data.Service
import com.ammar.appointmentbooking.ui.AppData
import com.ammar.appointmentbooking.ui.common.BrandLockup
import com.ammar.appointmentbooking.ui.common.EmptyState
import com.ammar.appointmentbooking.ui.common.GradientHeader
import com.ammar.appointmentbooking.ui.common.InitialAvatar
import com.ammar.appointmentbooking.ui.common.LightStatusBarIcons
import com.ammar.appointmentbooking.ui.common.QuickAction
import com.ammar.appointmentbooking.ui.common.StatusChip
import com.ammar.appointmentbooking.ui.theme.BrandColors
import com.ammar.appointmentbooking.util.Intents
import com.ammar.appointmentbooking.util.TimeFmt
import kotlinx.coroutines.launch
import java.time.LocalTime
import java.time.ZoneId

/** Customer landing page: greeting, next visit, shortcuts, services and contact details. */
@Composable
fun HomeScreen(
    data: AppData,
    repo: BookingRepository,
    onPick: (Service) -> Unit,
    onMyBookings: () -> Unit,
) {
    LightStatusBarIcons()
    val context = LocalContext.current
    val settings = data.settings
    val zone = runCatching { ZoneId.of(settings.timezone) }.getOrDefault(ZoneId.systemDefault())
    val services = data.services.filter { it.active }
    val appointments by remember(data.profile.uid) { repo.customerAppointmentsFlow(data.profile.uid) }
        .collectAsStateWithLifecycle(initialValue = emptyList())
    val now = System.currentTimeMillis()
    val next = appointments.filter { it.endMillis >= now && it.status.isActive }.minByOrNull { it.startMillis }

    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val servicesIndex = 3 // header, next/hero card, quick actions, then the services title
    val firstName = data.profile.name.trim().substringBefore(' ').ifBlank { "there" }

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
    ) {
        // 0. Header
        item {
            GradientHeader {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 72.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        BrandLockup(light = true, logoSize = 36.dp, modifier = Modifier.weight(1f))
                        if (settings.phone.isNotBlank()) {
                            Box(
                                Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.18f))
                                    .clickable { Intents.dial(context, settings.phone) },
                                contentAlignment = Alignment.Center,
                            ) { Icon(Icons.Filled.Call, "Call us", tint = Color.White) }
                        }
                    }
                    Spacer(Modifier.height(28.dp))
                    Text(
                        "${greeting()}, $firstName",
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.White.copy(alpha = 0.85f),
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Welcome to ${settings.name}",
                        style = MaterialTheme.typography.headlineMedium,
                        color = Color.White,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }

        // 1. Next appointment, or a call to book
        item {
            Box(Modifier.padding(horizontal = 16.dp).offset(y = (-52).dp)) {
                if (next != null) NextAppointmentCard(next, zone, onMyBookings)
                else BookNowCard(enabled = services.isNotEmpty()) {
                    scope.launch { listState.animateScrollToItem(servicesIndex) }
                }
            }
        }

        // 2. Shortcuts
        item {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp).offset(y = (-36).dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                QuickAction(Icons.Filled.DateRange, "Book", BrandColors.Indigo, Modifier.weight(1f)) {
                    scope.launch { listState.animateScrollToItem(servicesIndex) }
                }
                QuickAction(Icons.AutoMirrored.Filled.List, "Bookings", BrandColors.Violet, Modifier.weight(1f), onMyBookings)
                QuickAction(Icons.Filled.Call, "Call", Color(0xFF0D9488), Modifier.weight(1f)) {
                    if (settings.phone.isNotBlank()) Intents.dial(context, settings.phone)
                }
                QuickAction(Icons.Filled.LocationOn, "Directions", Color(0xFFDB2777), Modifier.weight(1f)) {
                    if (settings.address.isNotBlank()) Intents.map(context, settings.address)
                }
            }
        }

        // 3. Services
        item {
            Row(
                Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 0.dp, bottom = 8.dp),
                verticalAlignment = Alignment.Bottom,
            ) {
                Column(Modifier.weight(1f)) {
                    Text("Our services", style = MaterialTheme.typography.titleLarge)
                    Text(
                        "Tap a service to pick a time",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (services.isNotEmpty()) {
                    Text(
                        "${services.size} available",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }
        if (services.isEmpty()) {
            item { EmptyState("No services are available yet. Please check back soon.") }
        }
        itemsIndexed(services, key = { _, s -> s.id }) { index, service ->
            ServiceRow(service, index, settings.currency) { onPick(service) }
        }

        // Contact
        if (settings.address.isNotBlank() || settings.phone.isNotBlank() || settings.email.isNotBlank()) {
            item {
                Card(
                    Modifier.fillMaxWidth().padding(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                ) {
                    Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Visit us", style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer)
                        if (settings.address.isNotBlank()) ContactLine(Icons.Filled.LocationOn, settings.address) {
                            Intents.map(context, settings.address)
                        }
                        if (settings.phone.isNotBlank()) ContactLine(Icons.Filled.Call, settings.phone) {
                            Intents.dial(context, settings.phone)
                        }
                        if (settings.email.isNotBlank()) ContactLine(Icons.Filled.Email, settings.email) {
                            Intents.email(context, settings.email, "Question about my appointment")
                        }
                    }
                }
            }
        }
        item { Spacer(Modifier.height(16.dp)) }
    }
}

@Composable
private fun NextAppointmentCard(appt: Appointment, zone: ZoneId, onClick: () -> Unit) {
    Card(
        Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
    ) {
        Column(Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "YOUR NEXT VISIT",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f),
                )
                StatusChip(appt.status)
            }
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                val date = TimeFmt.zdt(appt.startMillis, zone)
                Column(
                    Modifier
                        .clip(MaterialTheme.shapes.medium)
                        .background(BrandColors.gradient)
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(date.dayOfMonth.toString(), style = MaterialTheme.typography.headlineSmall, color = Color.White)
                    Text(
                        date.month.name.take(3),
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White.copy(alpha = 0.85f),
                    )
                }
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(appt.serviceName, style = MaterialTheme.typography.titleMedium, maxLines = 1,
                        overflow = TextOverflow.Ellipsis)
                    Text(
                        "${TimeFmt.dayTime(appt.startMillis, zone)} – ${TimeFmt.time(appt.endMillis, zone)}",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Text(
                        "with ${appt.staffName}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun BookNowCard(enabled: Boolean, onBook: () -> Unit) {
    Card(
        Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
    ) {
        Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Star, null, tint = Color(0xFFF59E0B), modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("No upcoming visits", style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Spacer(Modifier.height(4.dp))
                Text("Ready for your next appointment?", style = MaterialTheme.typography.titleMedium)
            }
            Spacer(Modifier.width(12.dp))
            Button(
                onClick = onBook,
                enabled = enabled,
                shape = MaterialTheme.shapes.medium,
                colors = ButtonDefaults.buttonColors(containerColor = BrandColors.Teal),
            ) { Text("Book now") }
        }
    }
}

@Composable
internal fun ServiceRow(service: Service, index: Int, currency: String, onClick: () -> Unit) {
    Card(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 5.dp)
            .clip(MaterialTheme.shapes.medium)
            .clickable(onClick = onClick),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            InitialAvatar(service.name, BrandColors.tints[index % BrandColors.tints.size])
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(service.name, style = MaterialTheme.typography.titleMedium, maxLines = 1,
                    overflow = TextOverflow.Ellipsis)
                if (service.description.isNotBlank()) {
                    Text(
                        service.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    "⏱ ${service.durationMin} min",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .clip(MaterialTheme.shapes.small)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(horizontal = 8.dp, vertical = 3.dp),
                )
            }
            Spacer(Modifier.width(8.dp))
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    TimeFmt.money(service.price, currency),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    "Book",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White,
                    modifier = Modifier
                        .clip(MaterialTheme.shapes.small)
                        .background(MaterialTheme.colorScheme.primary)
                        .padding(horizontal = 12.dp, vertical = 5.dp),
                )
            }
        }
    }
}

@Composable
private fun ContactLine(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(MaterialTheme.shapes.small).clickable(onClick = onClick).padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(10.dp))
        Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
    }
}

private fun greeting(): String = when (LocalTime.now().hour) {
    in 5..11 -> "Good morning"
    in 12..16 -> "Good afternoon"
    else -> "Good evening"
}
