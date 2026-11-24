package com.ammar.appointmentbooking.ui

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ammar.appointmentbooking.ui.common.BrandSplash
import com.ammar.appointmentbooking.ui.customer.HomeScreen
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.ammar.appointmentbooking.AppContainer
import com.ammar.appointmentbooking.data.Appointment
import com.ammar.appointmentbooking.data.BusinessSettings
import com.ammar.appointmentbooking.data.ProfileState
import com.ammar.appointmentbooking.data.Role
import com.ammar.appointmentbooking.data.Service
import com.ammar.appointmentbooking.data.Staff
import com.ammar.appointmentbooking.data.UserProfile
import com.ammar.appointmentbooking.ui.admin.AdminCalendarScreen
import com.ammar.appointmentbooking.ui.admin.ManageHubScreen
import com.ammar.appointmentbooking.ui.admin.ManageServicesScreen
import com.ammar.appointmentbooking.ui.admin.ManageStaffScreen
import com.ammar.appointmentbooking.ui.admin.SettingsScreen
import com.ammar.appointmentbooking.ui.auth.AuthScreen
import com.ammar.appointmentbooking.ui.common.LoadingBox
import com.ammar.appointmentbooking.ui.customer.BookingScreen
import com.ammar.appointmentbooking.ui.customer.MyBookingsScreen
import com.ammar.appointmentbooking.ui.customer.ServicesScreen
import com.ammar.appointmentbooking.ui.profile.ProfileScreen
import com.ammar.appointmentbooking.ui.staff.StaffScheduleScreen
import com.ammar.appointmentbooking.ui.staff.TimeOffScreen

/** Everything most screens need, loaded once. */
data class AppData(
    val profile: UserProfile,
    val settings: BusinessSettings,
    val services: List<Service>,
    val staff: List<Staff>,
)

private data class Tab(val route: String, val label: String, val icon: ImageVector)

@Composable
fun AppRoot(container: AppContainer) {
    val uid by container.auth.uidFlow.collectAsStateWithLifecycle(initialValue = container.auth.currentUid)
    val currentUid = uid
    if (currentUid == null) {
        AuthScreen(container.auth)
        return
    }
    val profileFlow = remember(currentUid) { container.auth.profileFlow(currentUid) }
    val profileState by profileFlow.collectAsStateWithLifecycle(initialValue = ProfileState.Loading)
    when (val state = profileState) {
        is ProfileState.Ready -> SignedInApp(container, state.profile)
        else -> BrandSplash()
    }
}

@Composable
private fun SignedInApp(container: AppContainer, profile: UserProfile) {
    val repo = container.bookings
    val settings by remember { repo.settingsFlow() }.collectAsStateWithLifecycle(initialValue = BusinessSettings())
    val services by remember { repo.servicesFlow() }.collectAsStateWithLifecycle(initialValue = emptyList())
    val staff by remember { repo.staffFlow() }.collectAsStateWithLifecycle(initialValue = emptyList())
    val data = AppData(profile, settings, services, staff)

    LaunchedEffect(profile.uid) { runCatching { container.auth.saveFcmToken() } }

    val nav = rememberNavController()
    var rescheduleTarget by remember { mutableStateOf<Appointment?>(null) }

    val tabs = when (profile.role) {
        Role.CUSTOMER -> listOf(
            Tab("home", "Home", Icons.Filled.Home),
            Tab("mybookings", "Bookings", Icons.Filled.DateRange),
            Tab("profile", "Profile", Icons.Filled.AccountCircle),
        )
        Role.STAFF -> listOf(
            Tab("schedule", "My schedule", Icons.Filled.DateRange),
            Tab("services", "Walk-in", Icons.Filled.Add),
            Tab("profile", "Profile", Icons.Filled.AccountCircle),
        )
        Role.ADMIN -> listOf(
            Tab("calendar", "Calendar", Icons.Filled.DateRange),
            Tab("services", "Walk-in", Icons.Filled.Add),
            Tab("manage", "Manage", Icons.Filled.Settings),
            Tab("profile", "Profile", Icons.Filled.AccountCircle),
        )
    }
    val start = tabs.first().route

    val backStack by nav.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route

    val bookFor: (Service, Appointment?) -> Unit = { service, appt ->
        rescheduleTarget = appt
        nav.navigate("book/${service.id}")
    }
    val onReschedule: (Appointment) -> Unit = { appt ->
        val service = services.firstOrNull { it.id == appt.serviceId }
        if (service != null) bookFor(service, appt)
    }

    Scaffold(
        bottomBar = {
            if (tabs.any { it.route == currentRoute }) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 0.dp,
                    modifier = Modifier.shadow(12.dp),
                ) {
                    tabs.forEach { tab ->
                        NavigationBarItem(
                            selected = currentRoute == tab.route,
                            onClick = { nav.switchTab(tab.route) },
                            icon = { Icon(tab.icon, contentDescription = null) },
                            label = { Text(tab.label, fontWeight = if (currentRoute == tab.route) FontWeight.SemiBold else FontWeight.Normal) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                            ),
                        )
                    }
                }
            }
        }
    ) { padding ->
        // Only reserve space for the bottom bar: each screen handles the status bar itself,
        // so gradient headers can run edge to edge.
        NavHost(
            nav,
            startDestination = start,
            modifier = Modifier
                .padding(bottom = padding.calculateBottomPadding())
                .consumeWindowInsets(PaddingValues(bottom = padding.calculateBottomPadding())),
        ) {
            composable("home") {
                HomeScreen(
                    data = data,
                    repo = repo,
                    onPick = { bookFor(it, null) },
                    onMyBookings = { nav.switchTab("mybookings") },
                )
            }
            composable("services") {
                ServicesScreen(data, onPick = { bookFor(it, null) })
            }
            composable(
                "book/{serviceId}",
                arguments = listOf(navArgument("serviceId") { type = NavType.StringType })
            ) { entry ->
                val serviceId = entry.arguments?.getString("serviceId")
                val service = services.firstOrNull { it.id == serviceId }
                if (service == null) LoadingBox()
                else BookingScreen(
                    data = data,
                    service = service,
                    rescheduling = rescheduleTarget,
                    repo = repo,
                    onBack = { nav.popBackStack() },
                    onDone = {
                        rescheduleTarget = null
                        nav.switchTab(if (profile.role == Role.CUSTOMER) "mybookings" else start)
                    },
                )
            }
            composable("mybookings") { MyBookingsScreen(data, repo, onReschedule) }
            composable("schedule") {
                StaffScheduleScreen(data, repo, onReschedule, onTimeOff = { nav.navigate("timeoff") })
            }
            composable("timeoff") { TimeOffScreen(data, repo, onBack = { nav.popBackStack() }) }
            composable("calendar") { AdminCalendarScreen(data, repo, onReschedule) }
            composable("manage") {
                ManageHubScreen(
                    onServices = { nav.navigate("admin/services") },
                    onStaff = { nav.navigate("admin/staff") },
                    onTimeOff = { nav.navigate("timeoff") },
                    onSettings = { nav.navigate("admin/settings") },
                )
            }
            composable("admin/services") { ManageServicesScreen(data, repo, onBack = { nav.popBackStack() }) }
            composable("admin/staff") { ManageStaffScreen(data, repo, onBack = { nav.popBackStack() }) }
            composable("admin/settings") { SettingsScreen(data, repo, onBack = { nav.popBackStack() }) }
            composable("profile") { ProfileScreen(data, container.auth) }
        }
    }
}

private fun NavHostController.switchTab(route: String) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
