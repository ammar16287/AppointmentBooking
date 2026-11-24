package com.ammar.appointmentbooking.data

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthRecentLoginRequiredException
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

sealed interface ProfileState {
    data object Loading : ProfileState
    data object SignedOut : ProfileState
    data class Ready(val profile: UserProfile) : ProfileState
}

class AuthRepository(
    private val auth: FirebaseAuth,
    private val db: FirebaseFirestore,
) {
    val currentUid: String? get() = auth.currentUser?.uid

    /** Emits the signed-in user's id, or null when signed out. */
    val uidFlow: Flow<String?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { trySend(it.currentUser?.uid) }
        auth.addAuthStateListener(listener)
        awaitClose { auth.removeAuthStateListener(listener) }
    }

    /** Live profile of the given user. Creates a customer profile if the document is missing. */
    fun profileFlow(uid: String): Flow<ProfileState> = callbackFlow {
        trySend(ProfileState.Loading)
        val reg = db.collection("users").document(uid).addSnapshotListener { snap, err ->
            if (err != null || snap == null) {
                trySend(ProfileState.Loading)
                return@addSnapshotListener
            }
            val profile = UserProfile.from(snap)
            if (profile == null) {
                val user = auth.currentUser
                db.collection("users").document(uid).set(
                    mapOf(
                        "name" to (user?.displayName ?: user?.email?.substringBefore('@') ?: "Customer"),
                        "email" to (user?.email ?: ""),
                        "phone" to "",
                        "role" to Role.CUSTOMER.name,
                        "notifyPush" to true,
                        "notifyEmail" to true,
                        "notifySms" to false,
                    )
                )
            } else {
                trySend(ProfileState.Ready(profile))
            }
        }
        awaitClose { reg.remove() }
    }

    suspend fun signIn(email: String, password: String) {
        auth.signInWithEmailAndPassword(email.trim(), password).await()
        saveFcmToken()
    }

    suspend fun register(name: String, email: String, phone: String, password: String) {
        val result = auth.createUserWithEmailAndPassword(email.trim(), password).await()
        val uid = result.user?.uid ?: error("Registration failed")
        db.collection("users").document(uid).set(
            mapOf(
                "name" to name.trim(),
                "email" to email.trim(),
                "phone" to phone.trim(),
                "role" to Role.CUSTOMER.name,
                "notifyPush" to true,
                "notifyEmail" to true,
                "notifySms" to phone.isNotBlank(),
            )
        ).await()
        saveFcmToken()
    }

    suspend fun sendPasswordReset(email: String) {
        auth.sendPasswordResetEmail(email.trim()).await()
    }

    fun signOut() {
        auth.signOut()
    }

    suspend fun updateProfile(profile: UserProfile) {
        db.collection("users").document(profile.uid).set(
            mapOf(
                "name" to profile.name,
                "phone" to profile.phone,
                "notifyPush" to profile.notifyPush,
                "notifyEmail" to profile.notifyEmail,
                "notifySms" to profile.notifySms,
            ),
            SetOptions.merge()
        ).await()
    }

    suspend fun saveFcmToken(token: String? = null) {
        val uid = currentUid ?: return
        val t = token ?: runCatching { FirebaseMessaging.getInstance().token.await() }.getOrNull() ?: return
        db.collection("users").document(uid).set(mapOf("fcmToken" to t), SetOptions.merge()).await()
    }

    /**
     * Deletes the user's profile and their login. Booking records stay for the business's
     * history but lose the link to the account. Returns false if Firebase needs a fresh sign-in.
     */
    suspend fun deleteAccount(): Boolean {
        val user = auth.currentUser ?: return true
        return try {
            db.collection("users").document(user.uid).delete().await()
            user.delete().await()
            true
        } catch (e: FirebaseAuthRecentLoginRequiredException) {
            false
        }
    }
}
