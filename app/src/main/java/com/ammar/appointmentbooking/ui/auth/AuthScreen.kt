package com.ammar.appointmentbooking.ui.auth

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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.ammar.appointmentbooking.data.AuthRepository
import com.ammar.appointmentbooking.ui.common.BrandLogo
import com.ammar.appointmentbooking.ui.common.GradientHeader
import com.ammar.appointmentbooking.ui.common.LightStatusBarIcons
import kotlinx.coroutines.launch

@Composable
fun AuthScreen(auth: AuthRepository) {
    var registering by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    var info by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    LightStatusBarIcons()

    fun submit() {
        message = null; info = null
        if (registering && name.isBlank()) { message = "Please enter your name."; return }
        if (email.isBlank() || !email.contains('@')) { message = "Please enter a valid email address."; return }
        if (password.length < 6) { message = "Password must be at least 6 characters."; return }
        busy = true
        scope.launch {
            try {
                if (registering) auth.register(name.trim(), email.trim(), phone.trim(), password)
                else auth.signIn(email.trim(), password)
            } catch (e: Exception) {
                message = friendlyError(e)
            } finally {
                busy = false
            }
        }
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
        ) {
            GradientHeader(bottomRadius = 40.dp) {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(top = 36.dp, bottom = 76.dp, start = 24.dp, end = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    BrandLogo(84.dp, onGradient = true)
                    Spacer(Modifier.height(16.dp))
                    Text("BookEase", style = MaterialTheme.typography.headlineLarge, color = Color.White)
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Book your appointment in seconds",
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.White.copy(alpha = 0.85f),
                        textAlign = TextAlign.Center,
                    )
                }
            }

            Card(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .offset(y = (-48).dp),
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
            ) {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    ModeSwitch(registering) { registering = it; message = null; info = null }

                    Text(
                        if (registering) "Create your account" else "Welcome back",
                        style = MaterialTheme.typography.headlineSmall,
                    )
                    Text(
                        if (registering) "It only takes a minute." else "Sign in to manage your bookings.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )

                    if (registering) {
                        Field(name, { name = it }, "Full name", Icons.Filled.Person)
                        Field(phone, { phone = it }, "Mobile number (optional)", Icons.Filled.Phone, KeyboardType.Phone)
                    }
                    Field(email, { email = it }, "Email", Icons.Filled.Email, KeyboardType.Email)
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Password") },
                        singleLine = true,
                        leadingIcon = { Icon(Icons.Filled.Lock, null) },
                        trailingIcon = {
                            TextButton(onClick = { showPassword = !showPassword }) {
                                Text(if (showPassword) "Hide" else "Show")
                            }
                        },
                        visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { submit() }),
                        shape = MaterialTheme.shapes.medium,
                        colors = fieldColors(),
                        modifier = Modifier.fillMaxWidth(),
                    )

                    if (!registering) {
                        Text(
                            "Forgot password?",
                            color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.labelLarge,
                            modifier = Modifier
                                .align(Alignment.End)
                                .clip(MaterialTheme.shapes.small)
                                .clickable {
                                    message = null; info = null
                                    if (email.isBlank()) message = "Type your email above, then tap Forgot password."
                                    else scope.launch {
                                        try {
                                            auth.sendPasswordReset(email.trim()); info = "Password reset link sent to your email."
                                        } catch (e: Exception) {
                                            message = friendlyError(e)
                                        }
                                    }
                                }
                                .padding(4.dp),
                        )
                    }

                    message?.let { Banner(it, MaterialTheme.colorScheme.error) }
                    info?.let { Banner(it, MaterialTheme.colorScheme.secondary) }

                    Button(
                        onClick = { submit() },
                        enabled = !busy,
                        shape = MaterialTheme.shapes.medium,
                        modifier = Modifier.fillMaxWidth().height(54.dp),
                    ) {
                        if (busy) CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp, color = Color.White)
                        else Text(if (registering) "Create account" else "Sign in")
                    }
                }
            }

            Row(
                Modifier.fillMaxWidth().offset(y = (-32).dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    if (registering) "Already have an account?" else "New to BookEase?",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                TextButton(onClick = { registering = !registering; message = null; info = null }) {
                    Text(if (registering) "Sign in" else "Create account", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun ModeSwitch(registering: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(4.dp)
    ) {
        listOf(false to "Sign in", true to "Register").forEach { (value, label) ->
            val selected = registering == value
            Box(
                Modifier
                    .weight(1f)
                    .clip(MaterialTheme.shapes.small)
                    .background(if (selected) MaterialTheme.colorScheme.surface else Color.Transparent)
                    .clickable { onChange(value) }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    label,
                    style = MaterialTheme.typography.labelLarge,
                    color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun Field(
    value: String,
    onChange: (String) -> Unit,
    label: String,
    icon: ImageVector,
    type: KeyboardType = KeyboardType.Text,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        singleLine = true,
        leadingIcon = { Icon(icon, null) },
        keyboardOptions = KeyboardOptions(keyboardType = type, imeAction = ImeAction.Next),
        shape = MaterialTheme.shapes.medium,
        colors = fieldColors(),
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun fieldColors() = OutlinedTextFieldDefaults.colors(
    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
    unfocusedLeadingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
    focusedLeadingIconColor = MaterialTheme.colorScheme.primary,
)

@Composable
private fun Banner(text: String, color: Color) {
    Text(
        text,
        color = color,
        style = MaterialTheme.typography.bodyMedium,
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.small)
            .background(color.copy(alpha = 0.10f))
            .padding(12.dp),
    )
}

/** Turns Firebase's technical messages into plain language. */
private fun friendlyError(e: Exception): String {
    val raw = e.localizedMessage ?: return "Something went wrong. Please try again."
    return when {
        "password is invalid" in raw || "INVALID_LOGIN_CREDENTIALS" in raw || "credential is incorrect" in raw ->
            "Email or password is incorrect."
        "no user record" in raw -> "No account found with this email."
        "already in use" in raw -> "An account with this email already exists. Try signing in."
        "badly formatted" in raw -> "Please enter a valid email address."
        "network" in raw.lowercase() -> "No internet connection. Check your network and try again."
        "blocked all requests" in raw || "too many" in raw.lowercase() -> "Too many attempts. Please wait a moment and try again."
        else -> raw
    }
}
