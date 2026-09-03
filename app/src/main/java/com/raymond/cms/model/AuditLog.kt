package com.raymond.cms.model

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

data class AuditLog(
    @DocumentId
    val id: String = "",
    val action: String = "",
    val userId: String = "",
    val userName: String = "",
    val userRole: String = "",
    val recordId: String = "",
    val description: String = "",
    val oldValue: String = "",
    val newValue: String = "",
    val approvedBy: String = "",
    @ServerTimestamp
    val timestamp: Date? = null
)
