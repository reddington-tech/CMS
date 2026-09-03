package com.raymond.cms.util

object FirestoreCollections {
    const val USERS = "users"
    const val SHIFTS = "shifts"
    const val TRANSACTIONS = "shift_transactions"
    const val EXPENSES = "shift_expenses"
    const val INVESTMENTS = "investments"
    const val SERVICES = "services"
    const val APPROVALS = "approval_requests"
    const val AUDIT_LOGS = "audit_logs"
    const val INVENTORY = "inventory"
    const val PRODUCTS = "products"
    const val ATTENDANCE = "attendance"
}

object RecordStatus {
    const val COMPLETED = "COMPLETED"
    const val APPROVED = "APPROVED"
    const val PENDING = "PENDING"
    const val REJECTED = "REJECTED"
}
