package com.raymond.cms.model

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

enum class ShiftStatus {
    ACTIVE,
    CLOSED
}

@IgnoreExtraProperties
data class Shift(
    @DocumentId
    val id: String = "",
    val staffId: String = "",
    val staffName: String = "",
    val clockInTime: Long = System.currentTimeMillis(),
    val clockOutTime: Long? = null,
    val date: String = "", // YYYY-MM-DD
    val clockInDate: String = "",
    val status: ShiftStatus = ShiftStatus.ACTIVE,
    
    val openingCash: Double = 0.0,
    val openingMpesa: Double = 0.0,
    val openingTill: Double = 0.0,
    
    val expectedOpeningCash: Double = 0.0,
    val expectedOpeningMpesa: Double = 0.0,
    val expectedOpeningTill: Double = 0.0,
    
    val closingCash: Double = 0.0,
    val closingMpesa: Double = 0.0,
    val closingTill: Double = 0.0,
    
    val previousClosingTotal: Double = 0.0,
    val openingBalanceDifference: Double = 0.0,
    val flaggedForReview: Boolean = false,
    val adminReviewComment: String = "",
    val varianceStatus: String = "PENDING", // PENDING, REVIEWED, RESOLVED
    val varianceReviewedBy: String = "",
    val varianceReviewedByName: String = "",
    val varianceReviewedAt: Date? = null,
    @ServerTimestamp
    val createdAt: Date? = null,
    @ServerTimestamp
    val updatedAt: Date? = null
) {
    val totalOpening: Double
        get() = openingCash + openingMpesa + openingTill

    val expectedOpeningTotal: Double
        get() = expectedOpeningCash + expectedOpeningMpesa + expectedOpeningTill

    val totalClosing: Double
        get() = closingCash + closingMpesa + closingTill
}
