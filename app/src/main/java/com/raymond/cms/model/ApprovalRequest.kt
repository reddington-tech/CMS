package com.raymond.cms.model

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

enum class RequestStatus {
    PENDING,
    APPROVED,
    REJECTED
}

enum class RecordType {
    TRANSACTION,
    EXPENSE,
    SERVICE_PRICE,
    OPENING_BALANCE,
    CLOSING_BALANCE,
    DAILY_SUMMARY
}

data class ApprovalRequest(
    @DocumentId
    val id: String = "",
    val recordId: String = "",
    val recordType: RecordType = RecordType.TRANSACTION,
    val requestedBy: String = "",
    val requestedByName: String = "",
    val reason: String = "",
    val originalData: Map<String, Any?> = emptyMap(),
    val proposedData: Map<String, Any?> = emptyMap(),
    val status: RequestStatus = RequestStatus.PENDING,
    val reviewedBy: String = "",
    val reviewedByName: String = "",
    val adminComment: String = "",
    @ServerTimestamp
    val requestedAt: Date? = null,
    @ServerTimestamp
    val reviewedAt: Date? = null
)
