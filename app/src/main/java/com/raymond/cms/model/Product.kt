package com.raymond.cms.model

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

@IgnoreExtraProperties
data class Product(
    @DocumentId
    val id: String = "",
    val name: String = "",
    val description: String = "",
    val buyingPrice: Double = 0.0,
    val sellingPrice: Double = 0.0, // Recommended selling price
    val quantityBought: Int = 0,
    val currentStock: Int = 0,
    @ServerTimestamp
    val createdAt: Date? = null,
    val lastUpdated: Long = System.currentTimeMillis()
)
