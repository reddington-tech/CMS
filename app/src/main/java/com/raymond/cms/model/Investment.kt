package com.raymond.cms.model

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

enum class InvestmentType {
    INVESTMENT,
    CASH_TRANSFER
}

data class Investment(
    @DocumentId
    val id: String = "",
    val staffId: String = "",
    val staffName: String = "",
    val shiftId: String = "",
    val amount: Double = 0.0,
    val type: InvestmentType = InvestmentType.INVESTMENT,
    val description: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    @ServerTimestamp
    val createdAt: Date? = null
)
