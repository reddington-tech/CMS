package com.raymond.cms.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.raymond.cms.model.AuditLog
import com.raymond.cms.util.FirestoreCollections
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class AuditRepository {
    private val firestore = FirebaseFirestore.getInstance()
    private val auditCollection = firestore.collection(FirestoreCollections.AUDIT_LOGS)

    fun getLogs(): Flow<List<AuditLog>> = callbackFlow {
        val subscription = auditCollection
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    trySend(snapshot.toObjects(AuditLog::class.java))
                }
            }
        awaitClose { subscription.remove() }
    }

    suspend fun log(auditLog: AuditLog) {
        auditCollection.add(auditLog).await()
    }
}
