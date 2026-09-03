package com.raymond.cms.model

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.Exclude
import com.raymond.cms.util.DateTimeUtils
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

data class ExpenseItem(
    val description: String = "",
    val price: Double = 0.0
)

data class DailyTransaction(
    @DocumentId
    val id: String = "", // Format: YYYY-MM-DD
    val timestamp: Long = System.currentTimeMillis(),
    
    val openingCash: Double = 0.0,
    val openingMpesa: Double = 0.0,
    val openingTill: Double = 0.0,
    
    val detailedExpenses: List<ExpenseItem> = emptyList(),
    val serviceRevenue: Map<String, Double> = emptyMap(), // Service Name -> Amount
    val meals: Double = 0.0,
    
    val closingCash: Double = 0.0,
    val closingMpesa: Double = 0.0,
    val closingTill: Double = 0.0,

    // Legacy Support (can be used for totals if components are 0)
    val openingAmount: Double = 0.0,
    val closingAmount: Double = 0.0
) {
    @get:Exclude
    val totalOpening: Double
        get() = if (openingCash + openingMpesa + openingTill > 0) 
                  openingCash + openingMpesa + openingTill 
                else openingAmount

    @get:Exclude
    val totalClosing: Double
        get() = if (closingCash + closingMpesa + closingTill > 0) 
                  closingCash + closingMpesa + closingTill 
                else closingAmount

    @get:Exclude
    val totalExpenses: Double
        get() = detailedExpenses.sumOf { it.price }

    @get:Exclude
    val totalInvestments: Double
        get() = detailedExpenses.filter { it.description == "Other Investment" }.sumOf { it.price }

    @get:Exclude
    val totalEarned: Double
        get() = (totalClosing + totalInvestments) - totalOpening

    @get:Exclude
    val profit: Double
        get() = totalEarned

    @get:Exclude
    val grossRevenue: Double
        get() = totalEarned + (totalExpenses - totalInvestments) + meals

    @get:Exclude
    val formattedDate: String
        get() = DateTimeUtils.getFormat("EEE, dd MMM yyyy").format(Date(timestamp))

    companion object {
        val SERVICES = listOf("Printing", "Gaming", "Typing", "Scanning", "Laminating", "Internet")
        
        fun createId(date: Long): String {
            return DateTimeUtils.getFormat("yyyy-MM-dd", Locale.US).format(Date(date))
        }
    }
}
