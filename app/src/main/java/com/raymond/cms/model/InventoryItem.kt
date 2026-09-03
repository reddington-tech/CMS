package com.raymond.cms.model

import com.google.firebase.firestore.DocumentId

data class InventoryItem(
    @DocumentId
    val id: String = "",
    val name: String = "",
    val currentStock: Double = 0.0,
    val unit: String = "pcs", // e.g., "reams", "ml"
    val lowStockThreshold: Double = 2.0
)
