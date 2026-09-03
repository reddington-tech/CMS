package com.raymond.cms.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.raymond.cms.model.ApprovalRequest
import com.raymond.cms.model.RequestStatus
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class ApprovalRepository {
    private val firestore = FirebaseFirestore.getInstance()
    private val approvalCollection = firestore.collection("approval_requests")

    fun getAllRequests(): Flow<List<ApprovalRequest>> = callbackFlow {
        val subscription = approvalCollection
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    trySend(snapshot.toObjects(ApprovalRequest::class.java))
                }
            }
        awaitClose { subscription.remove() }
    }

    suspend fun getPendingRequestsSync(): List<ApprovalRequest> {
        val snapshot = approvalCollection
            .whereEqualTo("status", RequestStatus.PENDING.name)
            .get()
            .await()
        return snapshot.toObjects(ApprovalRequest::class.java)
    }

    suspend fun createRequest(request: ApprovalRequest) {
        approvalCollection.add(request).await()
    }

    suspend fun updateRequest(request: ApprovalRequest) {
        approvalCollection.document(request.id).set(request).await()
    }
}
