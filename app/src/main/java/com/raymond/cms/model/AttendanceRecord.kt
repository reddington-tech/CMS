package com.raymond.cms.model

import com.google.firebase.firestore.DocumentId

data class AttendanceRecord(
    @DocumentId
    val id: String = "", // UID_YYYY-MM-DD
    val userId: String = "",
    val userEmail: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val clockIn: Long? = null,
    val clockOut: Long? = null
)
