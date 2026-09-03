package com.raymond.cms.model

import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.PropertyName

enum class UserRole {
    ADMIN, // Owner
    STAFF
}

@IgnoreExtraProperties
data class UserModel(
    val uid: String = "",
    val name: String = "",
    val idNumber: String = "",
    val phoneNumber: String = "",
    val email: String = "",
    val role: UserRole = UserRole.STAFF,
    @get:PropertyName("isActive") @set:PropertyName("isActive")
    var isActive: Boolean = true,
    @get:PropertyName("isRemoved") @set:PropertyName("isRemoved")
    var isRemoved: Boolean = false
)
