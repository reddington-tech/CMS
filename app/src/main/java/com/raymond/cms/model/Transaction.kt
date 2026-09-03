package com.raymond.cms.model

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

enum class PaymentMethod {
    CASH,
    MPESA,
    TILL,
    OTHER
}

@IgnoreExtraProperties
data class TransactionItem(
    val type: String = "SERVICE", // SERVICE or PRODUCT
    val id: String = "",
    val name: String = "",
    val quantity: Int = 1,
    val unitPrice: Double = 0.0,
    val costPrice: Double = 0.0,
    val totalAmount: Double = 0.0,
    val profit: Double = 0.0
)

@IgnoreExtraProperties
data class Transaction(
    @DocumentId
    val id: String = "",
    val staffId: String = "",
    val staffName: String = "",
    val shiftId: String = "",
    val items: List<TransactionItem> = emptyList(),
    val totalAmount: Double = 0.0,
    val totalProfit: Double = 0.0,
    val paymentMethod: PaymentMethod = PaymentMethod.CASH,
    val timestamp: Long = System.currentTimeMillis(),
    val date: String = "", // YYYY-MM-DD
    val status: String = "COMPLETED",
    val notes: String = "",
    val updatedBy: String = "",
    val updatedByName: String = "",
    val approvedBy: String = "",
    val approvedByName: String = "",
    @ServerTimestamp
    val createdAt: Date? = null,
    @ServerTimestamp
    val updatedAt: Date? = null
)
