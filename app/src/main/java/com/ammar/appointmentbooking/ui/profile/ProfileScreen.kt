package com.ammar.appointmentbooking.ui.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.ammar.appointmentbooking.data.AuthRepository
import com.ammar.appointmentbooking.ui.AppData
import com.ammar.appointmentbooking.util.Intents
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(data: AppData, auth: AuthRepository) {
    val p = data.profile
    val s = data.settings
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var name by remember(p) { mutableStateOf(p.name) }
    var phone by remember(p) { mutableStateOf(p.phone) }
    var push by remember(p) { mutableStateOf(p.notifyPush) }
    var emailOn by remember(p) { mutableStateOf(p.notifyEmail) }
    var sms by remember(p) { mutableStateOf(p.notifySms) }
    var message by remember { mutableStateOf<String?>(null) }
    var confirmDelete by remember { mutableStateOf(false) }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Delete your account?") },
            text = { Text("Your profile and login will be removed permanently. This can't be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    scope.launch {
                        val ok = runCatching { auth.deleteAccount() }.getOrDefault(false)
                        if (!ok) message = "For security, please sign out, sign in again, then delete."
                    }
                }) { Text("Delete", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Cancel") } },
        )
    }

    Scaffold(topBar = { TopAppBar(title = { Text("Profile") }) }) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(p.email, style = MaterialTheme.typography.bodyMedium)
            Text("Role: ${p.role.name.lowercase().replaceFirstChar { it.uppercase() }}",
                style = MaterialTheme.typography.bodySmall)
            OutlinedTextField(name, { name = it }, label = { Text("Name") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(
                phone, { phone = it }, label = { Text("Mobile number") }, singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone), modifier = Modifier.fillMaxWidth()
            )
            HorizontalDivider()
            Text("Reminders", fontWeight = FontWeight.SemiBold)
            Toggle("Push notifications", push) { push = it }
            Toggle("Email", emailOn) { emailOn = it }
            Toggle("SMS (needs a mobile number)", sms) { sms = it }
            Text(
                "SMS is sent by our server. Reply STOP to any message to opt out.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            message?.let { Text(it, color = MaterialTheme.colorScheme.primary) }
            Button(onClick = {
                scope.launch {
                    message = try {
                        auth.updateProfile(p.copy(name = name.trim(), phone = phone.trim(),
                            notifyPush = push, notifyEmail = emailOn, notifySms = sms && phone.isNotBlank()))
                        "Saved."
                    } catch (e: Exception) { e.localizedMessage }
                }
            }, modifier = Modifier.fillMaxWidth()) { Text("Save") }

            HorizontalDivider()
            Text(s.name, fontWeight = FontWeight.SemiBold)
            if (s.address.isNotBlank()) {
                OutlinedButton(onClick = { Intents.map(context, s.address) }, modifier = Modifier.fillMaxWidth()) {
                    Text("Directions")
                }
            }
            if (s.phone.isNotBlank()) {
                OutlinedButton(onClick = { Intents.dial(context, s.phone) }, modifier = Modifier.fillMaxWidth()) {
                    Text("Call ${s.phone}")
                }
            }
            if (s.email.isNotBlank()) {
                OutlinedButton(onClick = { Intents.email(context, s.email, "Question about my booking") },
                    modifier = Modifier.fillMaxWidth()) { Text("Email us") }
            }

            HorizontalDivider()
            OutlinedButton(onClick = { auth.signOut() }, modifier = Modifier.fillMaxWidth()) { Text("Sign out") }
            TextButton(onClick = { confirmDelete = true }, modifier = Modifier.fillMaxWidth()) {
                Text("Delete my account", color = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@Composable
private fun Toggle(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onChange)
    }
}
