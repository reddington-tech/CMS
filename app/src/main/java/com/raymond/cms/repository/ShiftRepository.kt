package com.raymond.cms.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.raymond.cms.model.Shift
import com.raymond.cms.model.ShiftStatus
import com.raymond.cms.util.FirestoreCollections
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.*

class ShiftRepository {
    private val firestore = FirebaseFirestore.getInstance()
    private val shiftCollection = firestore.collection(FirestoreCollections.SHIFTS)

    fun getActiveShift(staffId: String): Flow<Shift?> = callbackFlow {
        val subscription = shiftCollection
            .whereEqualTo("staffId", staffId)
            .whereEqualTo("status", ShiftStatus.ACTIVE.name)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(null)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    trySend(snapshot.toObjects(Shift::class.java).firstOrNull())
                }
            }
        awaitClose { subscription.remove() }
    }

    suspend fun getActiveShiftSync(staffId: String): Shift? {
        val snapshot = shiftCollection
            .whereEqualTo("staffId", staffId)
            .whereEqualTo("status", ShiftStatus.ACTIVE.name)
            .get()
            .await()
        return snapshot.toObjects(Shift::class.java).firstOrNull()
    }

    suspend fun startShift(shift: Shift): String {
        val doc = shiftCollection.add(shift).await()
        return doc.id
    }

    suspend fun updateShift(shift: Shift) {
        shiftCollection.document(shift.id).set(shift).await()
    }

    suspend fun getLastClosingBalance(): Shift? {
        val shiftSnapshot = shiftCollection
            .whereEqualTo("status", ShiftStatus.CLOSED.name)
            .orderBy("clockOutTime", Query.Direction.DESCENDING)
            .limit(1)
            .get()
            .await()
        
        val lastShift = shiftSnapshot.toObjects(Shift::class.java).firstOrNull()
        if (lastShift != null) return lastShift
        
        // Fallback to legacy transactions if no shifts exist
        val legacySnapshot = firestore.collection("transactions")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(1)
            .get()
            .await()
        
        val legacy = legacySnapshot.toObjects(com.raymond.cms.model.DailyTransaction::class.java).firstOrNull()
        return if (legacy != null) {
            Shift(
                closingCash = legacy.closingCash,
                closingMpesa = legacy.closingMpesa,
                closingTill = legacy.closingTill,
                clockOutTime = legacy.timestamp
            )
        } else null
    }
    
    fun getAllShifts(): Flow<List<Shift>> = callbackFlow {
        val subscription = shiftCollection
            .orderBy("clockInTime", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    trySend(snapshot.toObjects(Shift::class.java))
                }
            }
        awaitClose { subscription.remove() }
    }

    fun getActiveShifts(): Flow<List<Shift>> = callbackFlow {
        val subscription = shiftCollection
            .whereEqualTo("status", ShiftStatus.ACTIVE.name)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    trySend(snapshot.toObjects(Shift::class.java))
                }
            }
        awaitClose { subscription.remove() }
    }
}
