package com.raymond.cms.model

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

@IgnoreExtraProperties
data class Expense(
    @DocumentId
    val id: String = "",
    val staffId: String = "",
    val staffName: String = "",
    val shiftId: String = "",
    val category: String = "",
    val amount: Double = 0.0,
    val description: String = "",
    val paymentMethod: PaymentMethod = PaymentMethod.CASH,
    val timestamp: Long = System.currentTimeMillis(),
    val date: String = "", // YYYY-MM-DD
    val status: String = "COMPLETED",
    val updatedBy: String = "",
    val updatedByName: String = "",
    val approvedBy: String = "",
    val approvedByName: String = "",
    @ServerTimestamp
    val createdAt: Date? = null,
    @ServerTimestamp
    val updatedAt: Date? = null
) {
    companion object {
        val CATEGORIES = listOf(
            "Reams", "Ink", "Meals", "Rent", "Electricity",
            "Internet", "Transport", "Maintenance", "Stationery", "Other"
        )
    }
}
