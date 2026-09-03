package com.raymond.cms.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.raymond.cms.model.AttendanceRecord
import com.raymond.cms.util.DateTimeUtils
import com.raymond.cms.util.FirestoreCollections
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*

class AttendanceRepository {
    private val firestore = FirebaseFirestore.getInstance()
    private val attendanceCollection = firestore.collection(FirestoreCollections.ATTENDANCE)

    fun getTodayAttendance(userId: String): Flow<AttendanceRecord?> = callbackFlow {
        val subscription = attendanceCollection
            .whereEqualTo("userId", userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(null)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val active = snapshot.toObjects(AttendanceRecord::class.java)
                        .filter { it.clockOut == null }
                        .maxByOrNull { it.clockIn ?: 0L }
                    trySend(active)
                }
            }
        awaitClose { subscription.remove() }
    }

    suspend fun clockIn(userId: String, email: String) {
        val todayId = createAttendanceId(userId, System.currentTimeMillis())
        val record = AttendanceRecord(
            id = todayId,
            userId = userId,
            userEmail = email,
            clockIn = System.currentTimeMillis()
        )
        attendanceCollection.document(todayId).set(record).await()
    }

    suspend fun clockOut(userId: String) {
        val snapshot = attendanceCollection
            .whereEqualTo("userId", userId)
            .get()
            .await()
            
        val lastOpenRecord = snapshot.toObjects(AttendanceRecord::class.java)
            .filter { it.clockOut == null }
            .maxByOrNull { it.clockIn ?: 0L }
            
        if (lastOpenRecord != null) {
            attendanceCollection.document(lastOpenRecord.id).update("clockOut", System.currentTimeMillis()).await()
        }
    }

    private fun createAttendanceId(userId: String, timestamp: Long): String {
        val date = DateTimeUtils.getFormat("yyyy-MM-dd", Locale.US).format(Date(timestamp))
        return "${userId}_$date"
    }
}
