package com.ammar.appointmentbooking.data

import android.util.Log
import com.ammar.appointmentbooking.domain.SlotEngine
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.time.LocalDate
import java.time.ZoneId
import java.util.Date

class SlotTakenException : Exception("Sorry, that time was just taken. Please pick another slot.")

data class BookingRequest(
    val service: Service,
    val staff: Staff,
    val date: LocalDate,
    val startMin: Int,
    val customerId: String,
    val customerName: String,
    val customerPhone: String,
    val customerEmail: String,
    val notes: String = "",
    val bookedBy: String,
)

private const val TAG = "BookingRepository"

/** Turns a Firestore query into a live list. Errors are logged and emitted as an empty list. */
private fun <T> Query.liveList(map: (DocumentSnapshot) -> T?): Flow<List<T>> = callbackFlow {
    val reg = addSnapshotListener { snap, err ->
        if (err != null || snap == null) {
            Log.w(TAG, "Query failed", err)
            trySend(emptyList())
            return@addSnapshotListener
        }
        trySend(snap.documents.mapNotNull(map))
    }
    awaitClose { reg.remove() }
}

class BookingRepository(private val db: FirebaseFirestore) {

    private val users = db.collection("users")
    private val services = db.collection("services")
    private val staff = db.collection("staff")
    private val timeOff = db.collection("timeOff")
    private val appointments = db.collection("appointments")
    private val locks = db.collection("slotLocks")
    private val settingsDoc = db.collection("settings").document("business")

    // ---------- Settings ----------

    fun settingsFlow(): Flow<BusinessSettings> = callbackFlow {
        val reg = settingsDoc.addSnapshotListener { snap, err ->
            if (err != null || snap == null) {
                trySend(BusinessSettings()); return@addSnapshotListener
            }
            trySend(BusinessSettings.from(snap))
        }
        awaitClose { reg.remove() }
    }

    suspend fun saveSettings(settings: BusinessSettings) {
        settingsDoc.set(settings.toMap()).await()
    }

    // ---------- Services ----------

    fun servicesFlow(): Flow<List<Service>> =
        services.orderBy("name").liveList { Service.from(it) }

    suspend fun saveService(service: Service) {
        if (service.id.isBlank()) services.add(service.toMap()).await()
        else services.document(service.id).set(service.toMap()).await()
    }

    suspend fun deleteService(id: String) {
        services.document(id).delete().await()
    }

    // ---------- Staff ----------

    fun staffFlow(): Flow<List<Staff>> = staff.orderBy("name").liveList { Staff.from(it) }

    /** Saves and returns the staff document id (new id when adding). */
    suspend fun saveStaff(member: Staff): String {
        val ref = if (member.id.isBlank()) staff.document() else staff.document(member.id)
        ref.set(member.toMap()).await()
        return ref.id
    }

    suspend fun deleteStaff(id: String) {
        staff.document(id).delete().await()
    }

    /** Links a registered user (found by email) to a staff record and gives them the STAFF role. */
    suspend fun linkStaffAccount(member: Staff, email: String): String {
        val found = users.whereEqualTo("email", email.trim()).limit(1).get().await()
            .documents.firstOrNull() ?: return "No registered user with that email. Ask them to sign up first."
        val current = Role.from(found.getString("role"))
        val newRole = if (current == Role.ADMIN) Role.ADMIN else Role.STAFF
        db.runBatch { b ->
            b.set(users.document(found.id), mapOf("role" to newRole.name, "staffId" to member.id), SetOptions.merge())
            b.set(staff.document(member.id), mapOf("userId" to found.id, "email" to email.trim()), SetOptions.merge())
        }.await()
        return "Linked ${found.getString("name") ?: email} to ${member.name}."
    }

    suspend fun makeAdmin(email: String): String {
        val found = users.whereEqualTo("email", email.trim()).limit(1).get().await()
            .documents.firstOrNull() ?: return "No registered user with that email."
        users.document(found.id).set(mapOf("role" to Role.ADMIN.name), SetOptions.merge()).await()
        return "${found.getString("name") ?: email} is now an admin."
    }

    // ---------- Time off ----------

    fun timeOffFlow(): Flow<List<TimeOff>> =
        timeOff.orderBy("startAt").liveList { TimeOff.from(it) }

    suspend fun addTimeOff(staffId: String, startMillis: Long, endMillis: Long, reason: String) {
        timeOff.add(
            mapOf(
                "staffId" to staffId,
                "startAt" to Timestamp(Date(startMillis)),
                "endAt" to Timestamp(Date(endMillis)),
                "reason" to reason,
            )
        ).await()
    }

    suspend fun deleteTimeOff(id: String) {
        timeOff.document(id).delete().await()
    }

    suspend fun timeOffFor(staffId: String): List<TimeOff> =
        timeOff.whereEqualTo("staffId", staffId).get().await().documents.mapNotNull { TimeOff.from(it) }

    // ---------- Slots ----------

    /** Block start minutes already locked for this staff member on this date. */
    suspend fun busyBlocks(staffId: String, date: LocalDate, ignoreAppointmentId: String? = null): Set<Int> =
        locks.whereEqualTo("staffId", staffId)
            .whereEqualTo("date", date.format(SlotEngine.dateKey))
            .get().await().documents
            .filter { ignoreAppointmentId == null || it.getString("appointmentId") != ignoreAppointmentId }
            .mapNotNull { it.getLong("startMin")?.toInt() }
            .toSet()

    // ---------- Appointments ----------

    fun customerAppointmentsFlow(uid: String): Flow<List<Appointment>> =
        appointments.whereEqualTo("customerId", uid).liveList { Appointment.from(it) }

    /** All appointments starting in [fromMillis, toMillis). Staff and admins only. */
    fun appointmentsInRangeFlow(fromMillis: Long, toMillis: Long): Flow<List<Appointment>> =
        appointments
            .whereGreaterThanOrEqualTo("startAt", Timestamp(Date(fromMillis)))
            .whereLessThan("startAt", Timestamp(Date(toMillis)))
            .orderBy("startAt")
            .liveList { Appointment.from(it) }

    /**
     * Books a slot inside a transaction. Every block the booking needs gets a lock document;
     * if any lock already exists the whole booking fails, so two people can never take the same time.
     */
    suspend fun book(req: BookingRequest, settings: BusinessSettings): String {
        val zone = ZoneId.of(settings.timezone)
        val step = settings.slotStepMin
        val blocks = SlotEngine.blocksFor(req.startMin, req.service, step)
        val lockRefs = blocks.map { locks.document(SlotEngine.lockId(req.staff.id, req.date, it)) }
        val apptRef = appointments.document()
        val start = req.date.atStartOfDay(zone).toInstant().toEpochMilli() + req.startMin * 60_000L
        val end = start + req.service.durationMin * 60_000L
        val status = if (settings.autoConfirm || req.bookedBy != req.customerId) Status.CONFIRMED else Status.PENDING

        db.runTransaction<Unit> { tx ->
            lockRefs.forEach { if (tx.get(it).exists()) throw SlotTakenException() }
            lockRefs.forEachIndexed { i, ref ->
                tx.set(
                    ref, mapOf(
                        "staffId" to req.staff.id,
                        "date" to req.date.format(SlotEngine.dateKey),
                        "startMin" to blocks[i],
                        "appointmentId" to apptRef.id,
                    )
                )
            }
            tx.set(
                apptRef, mapOf(
                    "customerId" to req.customerId,
                    "customerName" to req.customerName,
                    "customerPhone" to req.customerPhone,
                    "customerEmail" to req.customerEmail,
                    "staffId" to req.staff.id,
                    "staffName" to req.staff.name,
                    "staffUserId" to req.staff.userId,
                    "serviceId" to req.service.id,
                    "serviceName" to req.service.name,
                    "price" to req.service.price,
                    "startAt" to Timestamp(Date(start)),
                    "endAt" to Timestamp(Date(end)),
                    "date" to req.date.format(SlotEngine.dateKey),
                    "status" to status.name,
                    "notes" to req.notes,
                    "lockIds" to lockRefs.map { it.id },
                    "remindersSent" to emptyList<Int>(),
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedBy" to req.bookedBy,
                )
            )
            Unit
        }.await()
        return apptRef.id
    }

    /** Moves an existing appointment to a new slot (and possibly a new staff member) atomically. */
    suspend fun reschedule(
        appt: Appointment, service: Service, staff: Staff, date: LocalDate, startMin: Int,
        settings: BusinessSettings, by: String,
    ) {
        val zone = ZoneId.of(settings.timezone)
        val blocks = SlotEngine.blocksFor(startMin, service, settings.slotStepMin)
        val newRefs = blocks.map { locks.document(SlotEngine.lockId(staff.id, date, it)) }
        val newIds = newRefs.map { it.id }.toSet()
        val apptRef = appointments.document(appt.id)
        val start = date.atStartOfDay(zone).toInstant().toEpochMilli() + startMin * 60_000L
        val end = start + service.durationMin * 60_000L
        val status = if (settings.autoConfirm || by != appt.customerId) Status.CONFIRMED else Status.PENDING

        db.runTransaction<Unit> { tx ->
            newRefs.forEach { ref ->
                val snap = tx.get(ref)
                if (snap.exists() && snap.getString("appointmentId") != appt.id) throw SlotTakenException()
            }
            appt.lockIds.filter { it !in newIds }.forEach { tx.delete(locks.document(it)) }
            newRefs.forEachIndexed { i, ref ->
                tx.set(
                    ref, mapOf(
                        "staffId" to staff.id,
                        "date" to date.format(SlotEngine.dateKey),
                        "startMin" to blocks[i],
                        "appointmentId" to appt.id,
                    )
                )
            }
            tx.update(
                apptRef, mapOf(
                    "staffId" to staff.id,
                    "staffName" to staff.name,
                    "staffUserId" to staff.userId,
                    "startAt" to Timestamp(Date(start)),
                    "endAt" to Timestamp(Date(end)),
                    "date" to date.format(SlotEngine.dateKey),
                    "status" to status.name,
                    "lockIds" to newRefs.map { it.id },
                    "remindersSent" to emptyList<Int>(),
                    "updatedBy" to by,
                )
            )
            Unit
        }.await()
    }

    suspend fun setStatus(appt: Appointment, status: Status, by: String) {
        appointments.document(appt.id).update(mapOf("status" to status.name, "updatedBy" to by)).await()
    }

    /** Cancels and frees the time so others can book it. */
    suspend fun cancel(appt: Appointment, by: String) {
        db.runBatch { b ->
            b.update(appointments.document(appt.id), mapOf("status" to Status.CANCELLED.name, "updatedBy" to by))
            appt.lockIds.forEach { b.delete(locks.document(it)) }
        }.await()
    }

    suspend fun saveNotes(appt: Appointment, notes: String, by: String) {
        appointments.document(appt.id).update(mapOf("notes" to notes, "updatedBy" to by)).await()
    }
}
