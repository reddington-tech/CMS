package com.raymond.cms.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.raymond.cms.model.*
import com.raymond.cms.repository.FinancialRepository
import com.raymond.cms.repository.AuditRepository
import com.raymond.cms.repository.ApprovalRepository
import com.raymond.cms.repository.ServiceRepository
import com.raymond.cms.util.DateTimeUtils
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.*

class FinancialViewModel : BaseViewModel() {
    private val firestore = com.google.firebase.firestore.FirebaseFirestore.getInstance()
    private val financialRepo = FinancialRepository()
    private val auditRepo = AuditRepository()
    private val approvalRepo = ApprovalRepository()
    private val serviceRepo = ServiceRepository()
    private val shiftRepo = com.raymond.cms.repository.ShiftRepository()
    private val productRepo = com.raymond.cms.repository.ProductRepository()

    private val _pendingRequests = MutableStateFlow<List<ApprovalRequest>>(emptyList())
    val pendingRequests: StateFlow<List<ApprovalRequest>> = _pendingRequests.asStateFlow()

    init {
        observePendingRequests()
    }

    private fun observePendingRequests() {
        viewModelScope.launch(exceptionHandler) {
            approvalRepo.getAllRequests()
                .catch { e -> _uiEvent.emit("Approval Load Error: ${e.localizedMessage}") }
                .collect { list ->
                    _pendingRequests.value = list.sortedByDescending { it.requestedAt?.time ?: 0L }
                }
        }
    }

    fun requestTransactionEdit(
        user: UserModel,
        transaction: Transaction,
        proposedData: Map<String, Any?>,
        reason: String
    ) {
        if (reason.isEmpty()) {
            sendEvent("A mandatory reason for edit is required.")
            return
        }

        viewModelScope.launch(exceptionHandler) {
            // Requirement #17: Prevent duplicate edit requests
            val existing = approvalRepo.getPendingRequestsSync().find { 
                it.recordId == transaction.id && it.status == RequestStatus.PENDING 
            }
            if (existing != null) {
                _uiEvent.emit("An edit request for this record is already pending Admin approval.")
                return@launch
            }

            val request = ApprovalRequest(
                recordId = transaction.id,
                recordType = RecordType.TRANSACTION,
                requestedBy = user.uid,
                requestedByName = user.name.ifEmpty { user.email },
                reason = reason,
                originalData = if (transaction.items.isNotEmpty()) {
                    val first = transaction.items.first()
                    mapOf(
                        "name" to first.name,
                        "quantity" to first.quantity,
                        "unitPrice" to first.unitPrice,
                        "totalAmount" to transaction.totalAmount,
                        "paymentMethod" to transaction.paymentMethod.name
                    )
                } else {
                    mapOf(
                        "totalAmount" to transaction.totalAmount,
                        "paymentMethod" to transaction.paymentMethod.name
                    )
                },
                proposedData = proposedData
            )
            approvalRepo.createRequest(request)
            
            auditRepo.log(AuditLog(
                action = "EDIT_REQUESTED",
                userId = user.uid,
                userName = user.name.ifEmpty { user.email },
                userRole = user.role.name,
                description = "Requested edit for transaction ${transaction.id}"
            ))
            
            _uiEvent.emit("Edit request submitted for Admin approval")
        }
    }

    fun approveRequest(admin: UserModel, request: ApprovalRequest) {
        viewModelScope.launch(exceptionHandler) {
            // 1. Update the original record
            when (request.recordType) {
                RecordType.TRANSACTION -> {
                    val original = financialRepo.getTransactionById(request.recordId)
                    if (original != null) {
                        val updated = applyProposedData(original, request.proposedData).copy(
                            updatedBy = admin.uid,
                            updatedByName = admin.name,
                            approvedBy = admin.uid,
                            approvedByName = admin.name
                        )
                        financialRepo.updateTransaction(updated)
                    }
                }
                RecordType.EXPENSE -> {
                    val original = financialRepo.getExpenseById(request.recordId)
                    if (original != null) {
                        val updated = applyProposedDataToExpense(original, request.proposedData).copy(
                            updatedBy = admin.uid,
                            updatedByName = admin.name,
                            approvedBy = admin.uid,
                            approvedByName = admin.name
                        )
                        financialRepo.updateExpense(updated)
                    }
                }
                RecordType.SERVICE_PRICE -> {
                    val original = serviceRepo.getServiceById(request.recordId)
                    if (original != null) {
                        val newPrice = (request.proposedData["price"] as? Number)?.toDouble() ?: original.price
                        serviceRepo.updateService(original.copy(price = newPrice))
                    }
                }
                RecordType.DAILY_SUMMARY -> {
                    val isLegacy = request.recordId.startsWith("LEGACY_")
                    val docId = if (isLegacy) request.recordId.substringAfter("LEGACY_") else request.recordId
                    
                    if (isLegacy) {
                        val doc = firestore.collection("transactions").document(docId).get().await()
                        val legacy = doc.toObject(DailyTransaction::class.java)
                        if (legacy != null) {
                            val updated = legacy.copy(
                                openingCash = (request.proposedData["openingCash"] as? Number)?.toDouble() ?: legacy.openingCash,
                                openingMpesa = (request.proposedData["openingMpesa"] as? Number)?.toDouble() ?: legacy.openingMpesa,
                                openingTill = (request.proposedData["openingTill"] as? Number)?.toDouble() ?: legacy.openingTill,
                                openingAmount = ((request.proposedData["openingCash"] as? Number)?.toDouble() ?: legacy.openingCash) + 
                                                ((request.proposedData["openingMpesa"] as? Number)?.toDouble() ?: legacy.openingMpesa) + 
                                                ((request.proposedData["openingTill"] as? Number)?.toDouble() ?: legacy.openingTill),
                                closingCash = (request.proposedData["closingCash"] as? Number)?.toDouble() ?: legacy.closingCash,
                                closingMpesa = (request.proposedData["closingMpesa"] as? Number)?.toDouble() ?: legacy.closingMpesa,
                                closingTill = (request.proposedData["closingTill"] as? Number)?.toDouble() ?: legacy.closingTill,
                                closingAmount = ((request.proposedData["closingCash"] as? Number)?.toDouble() ?: legacy.closingCash) + 
                                                ((request.proposedData["closingMpesa"] as? Number)?.toDouble() ?: legacy.closingMpesa) + 
                                                ((request.proposedData["closingTill"] as? Number)?.toDouble() ?: legacy.closingTill),
                                meals = (request.proposedData["meals"] as? Number)?.toDouble() ?: legacy.meals,
                                detailedExpenses = listOf(ExpenseItem("Audit Correction", (request.proposedData["expenses"] as? Number)?.toDouble() ?: 0.0))
                            )
                            firestore.collection("transactions").document(docId).set(updated).await()
                        }
                    } else {
                        val doc = firestore.collection("shifts").document(docId).get().await()
                        val shift = doc.toObject(Shift::class.java)
                        if (shift != null) {
                            val updated = shift.copy(
                                openingCash = (request.proposedData["openingCash"] as? Number)?.toDouble() ?: shift.openingCash,
                                openingMpesa = (request.proposedData["openingMpesa"] as? Number)?.toDouble() ?: shift.openingMpesa,
                                openingTill = (request.proposedData["openingTill"] as? Number)?.toDouble() ?: shift.openingTill,
                                closingCash = (request.proposedData["closingCash"] as? Number)?.toDouble() ?: shift.closingCash,
                                closingMpesa = (request.proposedData["closingMpesa"] as? Number)?.toDouble() ?: shift.closingMpesa,
                                closingTill = (request.proposedData["closingTill"] as? Number)?.toDouble() ?: shift.closingTill,
                                openingBalanceDifference = ((request.proposedData["openingCash"] as? Number)?.toDouble() ?: shift.openingCash) + 
                                                            ((request.proposedData["openingMpesa"] as? Number)?.toDouble() ?: shift.openingMpesa) + 
                                                            ((request.proposedData["openingTill"] as? Number)?.toDouble() ?: shift.openingTill) - shift.previousClosingTotal
                            )
                            shiftRepo.updateShift(updated)
                            
                            // SYNC with DailyTransaction summary in legacy collection
                            try {
                                val summaryDoc = firestore.collection("transactions").document(docId).get().await()
                                val summary = summaryDoc.toObject(DailyTransaction::class.java)
                                if (summary != null) {
                                    val updatedSummary = summary.copy(
                                        openingCash = updated.openingCash,
                                        openingMpesa = updated.openingMpesa,
                                        openingTill = updated.openingTill,
                                        closingCash = updated.closingCash,
                                        closingMpesa = updated.closingMpesa,
                                        closingTill = updated.closingTill,
                                        meals = (request.proposedData["meals"] as? Number)?.toDouble() ?: summary.meals,
                                        detailedExpenses = if (request.proposedData.containsKey("expenses")) {
                                            listOf(ExpenseItem("Audit Correction", (request.proposedData["expenses"] as? Number)?.toDouble() ?: 0.0))
                                        } else summary.detailedExpenses
                                    )
                                    firestore.collection("transactions").document(docId).set(updatedSummary).await()
                                }
                            } catch (e: Exception) {
                                // Summary might not exist or other error
                            }
                        }
                    }
                }
                else -> { /* Handle others */ }
            }

            // 2. Update request status
            val updatedRequest = request.copy(
                status = RequestStatus.APPROVED,
                reviewedBy = admin.uid,
                reviewedByName = admin.name.ifEmpty { admin.email },
                reviewedAt = Date()
            )
            approvalRepo.updateRequest(updatedRequest)

            auditRepo.log(AuditLog(
                action = "EDIT_APPROVED",
                userId = admin.uid,
                userName = admin.name.ifEmpty { admin.email },
                userRole = admin.role.name,
                recordId = request.recordId,
                description = "Approved edit request ${request.id}",
                approvedBy = admin.name
            ))

            _uiEvent.emit("Request approved and record updated")
        }
    }

    fun rejectRequest(admin: UserModel, request: ApprovalRequest, comment: String) {
        viewModelScope.launch(exceptionHandler) {
            val updatedRequest = request.copy(
                status = RequestStatus.REJECTED,
                reviewedBy = admin.uid,
                reviewedByName = admin.name.ifEmpty { admin.email },
                reviewedAt = Date(),
                adminComment = comment
            )
            approvalRepo.updateRequest(updatedRequest)

            auditRepo.log(AuditLog(
                action = "EDIT_REJECTED",
                userId = admin.uid,
                userName = admin.name.ifEmpty { admin.email },
                userRole = admin.role.name,
                recordId = request.recordId,
                description = "Rejected edit request ${request.id}. Reason: $comment"
            ))

            _uiEvent.emit("Request rejected")
        }
    }

    private fun applyProposedData(original: Transaction, proposed: Map<String, Any?>): Transaction {
        // Since we now have multi-items, editing individual fields in the summary is complex.
        // For simplicity, we update the total and first item if applicable.
        val firstItem = if (original.items.isNotEmpty()) {
            original.items.first().copy(
                name = (proposed["name"] as? String) ?: original.items.first().name,
                quantity = (proposed["quantity"] as? Number)?.toInt() ?: original.items.first().quantity,
                unitPrice = (proposed["unitPrice"] as? Number)?.toDouble() ?: original.items.first().unitPrice,
                totalAmount = ((proposed["quantity"] as? Number)?.toInt() ?: original.items.first().quantity) * 
                              ((proposed["unitPrice"] as? Number)?.toDouble() ?: original.items.first().unitPrice)
            )
        } else null

        val updatedItems = if (firstItem != null) listOf(firstItem) + original.items.drop(1) else original.items

        return original.copy(
            items = updatedItems,
            totalAmount = (proposed["totalAmount"] as? Number)?.toDouble() ?: updatedItems.sumOf { it.totalAmount },
            paymentMethod = PaymentMethod.valueOf(proposed["paymentMethod"] as? String ?: original.paymentMethod.name)
        )
    }

    private fun applyProposedDataToExpense(original: Expense, proposed: Map<String, Any?>): Expense {
        return original.copy(
            amount = (proposed["amount"] as? Number)?.toDouble() ?: original.amount,
            description = (proposed["description"] as? String) ?: original.description,
            category = (proposed["category"] as? String) ?: original.category,
            paymentMethod = PaymentMethod.valueOf(proposed["paymentMethod"] as? String ?: original.paymentMethod.name)
        )
    }

    fun addMultiItemTransaction(
        user: UserModel,
        shiftId: String,
        items: List<TransactionItem>,
        paymentMethod: PaymentMethod,
        notes: String,
        onComplete: () -> Unit
    ) {
        if (shiftId != "ADMIN_SYSTEM_ENTRY") {
            viewModelScope.launch(exceptionHandler) {
                val shift = shiftRepo.getActiveShiftSync(user.uid)
                if (shift == null || shift.id != shiftId) {
                    _uiEvent.emit("Shift Closed. You cannot record additional transactions.")
                    return@launch
                }
            }
        }

        viewModelScope.launch(exceptionHandler) {
            val timestamp = System.currentTimeMillis()
            val date = DateTimeUtils.getFormat("yyyy-MM-dd", Locale.US).format(Date(timestamp))
            
            // 1. Fetch current product data to calculate profit accurately
            val products = productRepo.getProducts().first()
            val updatedItems = items.map { item ->
                if (item.type == "PRODUCT") {
                    val p = products.find { it.id == item.id }
                    val cost = p?.buyingPrice ?: 0.0
                    val profit = (item.unitPrice - cost) * item.quantity
                    item.copy(costPrice = cost, profit = profit)
                } else {
                    // For services, profit = totalAmount (assuming zero direct cost for now)
                    item.copy(profit = item.totalAmount)
                }
            }

            val total = updatedItems.sumOf { it.totalAmount }
            val totalProfit = updatedItems.sumOf { it.profit }

            val transaction = Transaction(
                staffId = user.uid,
                staffName = user.name.ifEmpty { user.email },
                shiftId = shiftId,
                items = updatedItems,
                totalAmount = total,
                totalProfit = totalProfit,
                paymentMethod = paymentMethod,
                timestamp = timestamp,
                date = date,
                notes = notes
            )
            financialRepo.addTransaction(transaction)
            
            // Log audit
            auditRepo.log(AuditLog(
                action = "TRANSACTION_CREATED",
                userId = user.uid,
                userName = user.name.ifEmpty { user.email },
                userRole = user.role.name,
                description = "Recorded sale: ${items.size} items (KSh $total)"
            ))
            
            // Deduct from products stock if any (Requirement #5 & #POS)
            items.filter { it.type == "PRODUCT" }.forEach { pItem ->
                productRepo.deductStock(pItem.id, pItem.quantity)
            }

            _uiEvent.emit("Sale recorded successfully")
            onComplete()
        }
    }

    fun addExpense(
        user: UserModel,
        shiftId: String,
        category: String,
        amount: Double,
        description: String,
        paymentMethod: PaymentMethod,
        onComplete: () -> Unit
    ) {
        if (shiftId != "ADMIN_SYSTEM_ENTRY") {
            viewModelScope.launch(exceptionHandler) {
                val shift = shiftRepo.getActiveShiftSync(user.uid)
                if (shift == null || shift.id != shiftId) {
                    _uiEvent.emit("Shift Closed. You cannot record additional expenses.")
                    return@launch
                }
            }
        }

        viewModelScope.launch(exceptionHandler) {
            val timestamp = System.currentTimeMillis()
            val date = DateTimeUtils.getFormat("yyyy-MM-dd", Locale.US).format(Date(timestamp))
            
            val expense = Expense(
                staffId = user.uid,
                staffName = user.name.ifEmpty { user.email },
                shiftId = shiftId,
                category = category,
                amount = amount,
                description = description,
                paymentMethod = paymentMethod,
                timestamp = timestamp,
                date = date
            )
            financialRepo.addExpense(expense)
            
            auditRepo.log(AuditLog(
                action = "EXPENSE_CREATED",
                userId = user.uid,
                userName = user.name.ifEmpty { user.email },
                userRole = user.role.name,
                description = "Recorded expense: $category (KSh $amount)"
            ))
            
            _uiEvent.emit("Expense recorded")
            onComplete()
        }
    }

    fun addInvestment(
        user: UserModel,
        shiftId: String,
        amount: Double,
        type: InvestmentType,
        description: String,
        onComplete: () -> Unit
    ) {
        viewModelScope.launch(exceptionHandler) {
            val investment = Investment(
                staffId = user.uid,
                staffName = user.name.ifEmpty { user.email },
                shiftId = shiftId,
                amount = amount,
                type = type,
                description = description
            )
            financialRepo.addInvestment(investment)
            
            auditRepo.log(AuditLog(
                action = "INVESTMENT_RECORDED",
                userId = user.uid,
                userName = user.name.ifEmpty { user.email },
                userRole = user.role.name,
                description = "Recorded ${type.name}: KSh $amount"
            ))
            
            _uiEvent.emit("${type.name} recorded")
            onComplete()
        }
    }
}
