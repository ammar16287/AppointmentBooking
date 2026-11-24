package com.ammar.appointmentbooking.data

import com.google.firebase.firestore.DocumentSnapshot

enum class Role { CUSTOMER, STAFF, ADMIN;
    companion object {
        fun from(value: String?): Role = entries.firstOrNull { it.name == value } ?: CUSTOMER
    }
}

enum class Status(val label: String) {
    PENDING("Pending"),
    CONFIRMED("Confirmed"),
    CHECKED_IN("Checked in"),
    COMPLETED("Completed"),
    CANCELLED("Cancelled"),
    NO_SHOW("No-show");

    val isActive: Boolean get() = this == PENDING || this == CONFIRMED || this == CHECKED_IN

    companion object {
        fun from(value: String?): Status = entries.firstOrNull { it.name == value } ?: PENDING
    }
}

data class UserProfile(
    val uid: String = "",
    val name: String = "",
    val email: String = "",
    val phone: String = "",
    val role: Role = Role.CUSTOMER,
    val staffId: String? = null,
    val notifyPush: Boolean = true,
    val notifyEmail: Boolean = true,
    val notifySms: Boolean = false,
) {
    companion object {
        fun from(doc: DocumentSnapshot): UserProfile? {
            if (!doc.exists()) return null
            return UserProfile(
                uid = doc.id,
                name = doc.getString("name").orEmpty(),
                email = doc.getString("email").orEmpty(),
                phone = doc.getString("phone").orEmpty(),
                role = Role.from(doc.getString("role")),
                staffId = doc.getString("staffId"),
                notifyPush = doc.getBoolean("notifyPush") ?: true,
                notifyEmail = doc.getBoolean("notifyEmail") ?: true,
                notifySms = doc.getBoolean("notifySms") ?: false,
            )
        }
    }
}

data class Service(
    val id: String = "",
    val name: String = "",
    val description: String = "",
    val durationMin: Int = 30,
    val bufferMin: Int = 10,
    val price: Double = 0.0,
    val active: Boolean = true,
) {
    fun toMap(): Map<String, Any> = mapOf(
        "name" to name,
        "description" to description,
        "durationMin" to durationMin,
        "bufferMin" to bufferMin,
        "price" to price,
        "active" to active,
    )

    companion object {
        fun from(doc: DocumentSnapshot): Service? {
            if (!doc.exists()) return null
            return Service(
                id = doc.id,
                name = doc.getString("name").orEmpty(),
                description = doc.getString("description").orEmpty(),
                durationMin = (doc.getLong("durationMin") ?: 30L).toInt(),
                bufferMin = (doc.getLong("bufferMin") ?: 0L).toInt(),
                price = doc.getDouble("price") ?: 0.0,
                active = doc.getBoolean("active") ?: true,
            )
        }
    }
}

/** Working hours for one weekday, as minutes from midnight. */
data class WorkDay(val startMin: Int = 9 * 60, val endMin: Int = 17 * 60)

data class Staff(
    val id: String = "",
    val name: String = "",
    val title: String = "",
    val email: String = "",
    val userId: String? = null,
    val serviceIds: List<String> = emptyList(),
    /** Key = ISO day of week (1 = Monday ... 7 = Sunday). Missing day = day off. */
    val hours: Map<Int, WorkDay> = (1..6).associateWith { WorkDay() },
    val active: Boolean = true,
) {
    fun canDo(serviceId: String) = serviceIds.isEmpty() || serviceId in serviceIds

    fun toMap(): Map<String, Any?> = mapOf(
        "name" to name,
        "title" to title,
        "email" to email,
        "userId" to userId,
        "serviceIds" to serviceIds,
        "hours" to hours.mapKeys { it.key.toString() }
            .mapValues { mapOf("startMin" to it.value.startMin, "endMin" to it.value.endMin) },
        "active" to active,
    )

    companion object {
        @Suppress("UNCHECKED_CAST")
        fun from(doc: DocumentSnapshot): Staff? {
            if (!doc.exists()) return null
            val rawHours = doc.get("hours") as? Map<String, Any?> ?: emptyMap()
            val hours = rawHours.mapNotNull { (k, v) ->
                val day = k.toIntOrNull() ?: return@mapNotNull null
                val m = v as? Map<String, Any?> ?: return@mapNotNull null
                val s = (m["startMin"] as? Number)?.toInt() ?: return@mapNotNull null
                val e = (m["endMin"] as? Number)?.toInt() ?: return@mapNotNull null
                day to WorkDay(s, e)
            }.toMap()
            return Staff(
                id = doc.id,
                name = doc.getString("name").orEmpty(),
                title = doc.getString("title").orEmpty(),
                email = doc.getString("email").orEmpty(),
                userId = doc.getString("userId"),
                serviceIds = (doc.get("serviceIds") as? List<String>) ?: emptyList(),
                hours = hours,
                active = doc.getBoolean("active") ?: true,
            )
        }
    }
}

data class TimeOff(
    val id: String = "",
    val staffId: String = "",
    val startMillis: Long = 0,
    val endMillis: Long = 0,
    val reason: String = "",
) {
    companion object {
        fun from(doc: DocumentSnapshot): TimeOff? {
            if (!doc.exists()) return null
            return TimeOff(
                id = doc.id,
                staffId = doc.getString("staffId").orEmpty(),
                startMillis = doc.getTimestamp("startAt")?.toDate()?.time ?: 0,
                endMillis = doc.getTimestamp("endAt")?.toDate()?.time ?: 0,
                reason = doc.getString("reason").orEmpty(),
            )
        }
    }
}

data class Appointment(
    val id: String = "",
    val customerId: String = "",
    val customerName: String = "",
    val customerPhone: String = "",
    val customerEmail: String = "",
    val staffId: String = "",
    val staffName: String = "",
    val serviceId: String = "",
    val serviceName: String = "",
    val price: Double = 0.0,
    val startMillis: Long = 0,
    val endMillis: Long = 0,
    val status: Status = Status.PENDING,
    val notes: String = "",
    val lockIds: List<String> = emptyList(),
) {
    companion object {
        @Suppress("UNCHECKED_CAST")
        fun from(doc: DocumentSnapshot): Appointment? {
            if (!doc.exists()) return null
            return Appointment(
                id = doc.id,
                customerId = doc.getString("customerId").orEmpty(),
                customerName = doc.getString("customerName").orEmpty(),
                customerPhone = doc.getString("customerPhone").orEmpty(),
                customerEmail = doc.getString("customerEmail").orEmpty(),
                staffId = doc.getString("staffId").orEmpty(),
                staffName = doc.getString("staffName").orEmpty(),
                serviceId = doc.getString("serviceId").orEmpty(),
                serviceName = doc.getString("serviceName").orEmpty(),
                price = doc.getDouble("price") ?: 0.0,
                startMillis = doc.getTimestamp("startAt")?.toDate()?.time ?: 0,
                endMillis = doc.getTimestamp("endAt")?.toDate()?.time ?: 0,
                status = Status.from(doc.getString("status")),
                notes = doc.getString("notes").orEmpty(),
                lockIds = (doc.get("lockIds") as? List<String>) ?: emptyList(),
            )
        }
    }
}

data class BusinessSettings(
    val name: String = "My Salon",
    val type: String = "salon", // "salon" or "clinic"
    val phone: String = "",
    val email: String = "",
    val address: String = "",
    val timezone: String = "Asia/Karachi",
    val currency: String = "PKR",
    val minNoticeMin: Int = 120,
    val maxDaysAhead: Int = 30,
    val cancelCutoffMin: Int = 240,
    val autoConfirm: Boolean = true,
    val slotStepMin: Int = 15,
    val reminderOffsetsMin: List<Int> = listOf(24 * 60, 120),
    /** Dates the business is closed, as yyyy-MM-dd. */
    val closedDates: List<String> = emptyList(),
) {
    val isClinic: Boolean get() = type == "clinic"
    val staffWord: String get() = if (isClinic) "Doctor" else "Stylist"
    val customerWord: String get() = if (isClinic) "Patient" else "Client"

    fun toMap(): Map<String, Any> = mapOf(
        "name" to name, "type" to type, "phone" to phone, "email" to email,
        "address" to address, "timezone" to timezone, "currency" to currency,
        "minNoticeMin" to minNoticeMin, "maxDaysAhead" to maxDaysAhead,
        "cancelCutoffMin" to cancelCutoffMin, "autoConfirm" to autoConfirm,
        "slotStepMin" to slotStepMin, "reminderOffsetsMin" to reminderOffsetsMin,
        "closedDates" to closedDates,
    )

    companion object {
        @Suppress("UNCHECKED_CAST")
        fun from(doc: DocumentSnapshot): BusinessSettings {
            if (!doc.exists()) return BusinessSettings()
            val d = BusinessSettings()
            return BusinessSettings(
                name = doc.getString("name") ?: d.name,
                type = doc.getString("type") ?: d.type,
                phone = doc.getString("phone") ?: d.phone,
                email = doc.getString("email") ?: d.email,
                address = doc.getString("address") ?: d.address,
                timezone = doc.getString("timezone") ?: d.timezone,
                currency = doc.getString("currency") ?: d.currency,
                minNoticeMin = doc.getLong("minNoticeMin")?.toInt() ?: d.minNoticeMin,
                maxDaysAhead = doc.getLong("maxDaysAhead")?.toInt() ?: d.maxDaysAhead,
                cancelCutoffMin = doc.getLong("cancelCutoffMin")?.toInt() ?: d.cancelCutoffMin,
                autoConfirm = doc.getBoolean("autoConfirm") ?: d.autoConfirm,
                slotStepMin = doc.getLong("slotStepMin")?.toInt()?.takeIf { it in 5..60 } ?: d.slotStepMin,
                reminderOffsetsMin = (doc.get("reminderOffsetsMin") as? List<Number>)?.map { it.toInt() }
                    ?: d.reminderOffsetsMin,
                closedDates = (doc.get("closedDates") as? List<String>) ?: d.closedDates,
            )
        }
    }
}
