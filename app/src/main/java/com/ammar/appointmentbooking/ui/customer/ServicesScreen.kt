package com.ammar.appointmentbooking.ui.customer

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ammar.appointmentbooking.data.Role
import com.ammar.appointmentbooking.data.Service
import com.ammar.appointmentbooking.ui.AppData
import com.ammar.appointmentbooking.ui.common.EmptyState
import com.ammar.appointmentbooking.util.Intents
import com.ammar.appointmentbooking.util.TimeFmt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServicesScreen(data: AppData, onPick: (Service) -> Unit) {
    val context = LocalContext.current
    val settings = data.settings
    val active = data.services.filter { it.active }
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(settings.name, fontWeight = FontWeight.SemiBold)
                        Text(
                            if (data.profile.role == Role.CUSTOMER) "Choose a service to book"
                            else "Book a walk-in ${settings.customerWord.lowercase()}",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                },
                actions = {
                    if (settings.phone.isNotBlank()) {
                        IconButton(onClick = { Intents.dial(context, settings.phone) }) {
                            Icon(Icons.Filled.Call, contentDescription = "Call us")
                        }
                    }
                }
            )
        }
    ) { padding ->
        if (active.isEmpty()) {
            EmptyState(
                if (data.profile.role == Role.ADMIN) "No services yet. Add them under Manage → Services."
                else "No services are available yet.",
                Modifier.padding(padding)
            )
            return@Scaffold
        }
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 8.dp),
        ) {
            itemsIndexed(active, key = { _, s -> s.id }) { index, service ->
                ServiceRow(service, index, settings.currency) { onPick(service) }
            }
        }
    }
}
