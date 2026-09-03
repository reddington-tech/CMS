package com.raymond.cms.model

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.PropertyName

data class Service(
    @DocumentId
    val id: String = "",
    val name: String = "",
    val price: Double = 0.0,
    @get:PropertyName("active")
    @set:PropertyName("active")
    var isActive: Boolean = true
) {
    companion object {
        val DEFAULT_SERVICES = listOf(
            "Printing", "Gaming", "Typing", "Scanning", "Laminating",
            "Internet", "Photocopying", "Binding", "Other"
        )
    }
}
