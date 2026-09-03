package com.raymond.cms.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.raymond.cms.model.*
import com.raymond.cms.model.ShiftStatus
import com.raymond.cms.repository.InventoryRepository
import com.raymond.cms.repository.TransactionRepository
import com.raymond.cms.repository.FinancialRepository
import com.raymond.cms.repository.ShiftRepository
import com.raymond.cms.domain.*
import com.raymond.cms.util.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File
import java.util.*

enum class ReportPeriod {
    DAILY, WEEKLY, MONTHLY, YEARLY, CUSTOM
}

class TransactionViewModel : BaseViewModel() {
    private val firestore = com.google.firebase.firestore.FirebaseFirestore.getInstance()
    private val repository = TransactionRepository()
    private val inventoryRepo = InventoryRepository()
    private val financialRepo = FinancialRepository()
    private val shiftRepo = ShiftRepository()
    private val auditRepo = com.raymond.cms.repository.AuditRepository()
    private val approvalRepo = com.raymond.cms.repository.ApprovalRepository()
    
    private val addTransactionUseCase = AddTransactionUseCase(repository, inventoryRepo)
    private val getInsightsUseCase = GetBusinessInsightsUseCase()

    private val _transactions = MutableStateFlow<List<DailyTransaction>>(emptyList())
    val legacyTransactions: StateFlow<List<DailyTransaction>> = _transactions.asStateFlow()
    private val _allTransactions = MutableStateFlow<List<Transaction>>(emptyList())
    private val _allExpenses = MutableStateFlow<List<Expense>>(emptyList())
    private val _allInvestments = MutableStateFlow<List<Investment>>(emptyList())
    private val _allShifts = MutableStateFlow<List<Shift>>(emptyList())
    val allShifts: StateFlow<List<Shift>> = _allShifts.asStateFlow()
    
    private val _selectedPeriod = MutableStateFlow(ReportPeriod.DAILY)
    val selectedPeriod: StateFlow<ReportPeriod> = _selectedPeriod.asStateFlow()

    private val _selectedDate = MutableStateFlow(System.currentTimeMillis())
    val selectedDate: StateFlow<Long> = _selectedDate.asStateFlow()

    private val _dateRange = MutableStateFlow<Pair<Long, Long>?>(null)
    val dateRange: StateFlow<Pair<Long, Long>?> = _dateRange.asStateFlow()

    private val _inventory = MutableStateFlow<List<InventoryItem>>(emptyList())
    val inventory: StateFlow<List<InventoryItem>> = _inventory.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    val filteredTransactions: StateFlow<List<Transaction>> = combine(_allTransactions, _searchQuery) { list, query ->
        if (query.isEmpty()) list
        else list.filter { 
            it.id.contains(query, ignoreCase = true) || 
            it.staffName.contains(query, ignoreCase = true) ||
            it.items.any { item -> item.name.contains(query, ignoreCase = true) } ||
            it.date.contains(query) ||
            it.totalAmount.toString().contains(query) ||
            it.paymentMethod.name.contains(query, ignoreCase = true)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // uiEvent is now in BaseViewModel

    private val _reportState = MutableStateFlow(BusinessReport())
    val reportState: StateFlow<BusinessReport> = _reportState.asStateFlow()

    val allDailyBreakdowns: StateFlow<List<DailyBreakdown>> = _transactions.map { list ->
        list.map { legacy ->
            DailyBreakdown(
                id = legacy.id,
                date = legacy.formattedDate,
                revenue = legacy.grossRevenue,
                expenses = legacy.totalExpenses,
                profit = legacy.profit,
                timestamp = legacy.timestamp
            )
        }.sortedByDescending { it.timestamp }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _insights = MutableStateFlow<List<String>>(emptyList())
    val insights: StateFlow<List<String>> = _insights.asStateFlow()

    private val _auditLogs = MutableStateFlow<List<AuditLog>>(emptyList())
    val auditLogs: StateFlow<List<AuditLog>> = _auditLogs.asStateFlow()

    init {
        viewModelScope.launch(exceptionHandler) {
            try {
                // Default to Monthly for the broad executive overview
                _selectedPeriod.value = ReportPeriod.MONTHLY 
                fetchTransactions()
                fetchInventory()
                fetchAllData()
                fetchAuditLogs()
            } catch (e: Exception) {
                _uiEvent.emit("Startup Load Error: ${e.localizedMessage}")
            }
        }
    }

    private fun fetchAuditLogs() {
        viewModelScope.launch(exceptionHandler) {
            auditRepo.getLogs()
                .catch { e -> _uiEvent.emit("Audit Log Error: ${e.localizedMessage}") }
                .collect { _auditLogs.value = it }
        }
    }

    fun setPeriod(period: ReportPeriod) {
        _selectedPeriod.value = period
        updateReports()
    }

    fun setSelectedDate(timestamp: Long) {
        _selectedDate.value = timestamp
        updateReports()
    }

    fun setCustomRange(start: Long, end: Long) {
        _dateRange.value = Pair(start, end)
        _selectedPeriod.value = ReportPeriod.CUSTOM
        updateReports()
    }

    private fun fetchAllData() {
        viewModelScope.launch(exceptionHandler) {
            financialRepo.getAllTransactions()
                .catch { e -> _uiEvent.emit("Transaction Load Error: Check index links.") }
                .collect { 
                    _allTransactions.value = it
                    updateReports()
                }
        }
        viewModelScope.launch(exceptionHandler) {
            financialRepo.getAllExpenses()
                .catch { e -> _uiEvent.emit("Expense Load Error: Check index links.") }
                .collect { 
                    _allExpenses.value = it
                    updateReports()
                }
        }
        viewModelScope.launch(exceptionHandler) {
            // Fetch all investments
            firestore.collection(FirestoreCollections.INVESTMENTS)
                .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) return@addSnapshotListener
                    _allInvestments.value = snapshot?.toObjects(Investment::class.java) ?: emptyList()
                    updateReports()
                }
        }
        viewModelScope.launch(exceptionHandler) {
            shiftRepo.getAllShifts()
                .catch { e -> _uiEvent.emit("Shift Load Error: Check index links.") }
                .collect {
                    _allShifts.value = it
                    updateReports()
                }
        }
    }

    private fun roundTo10(value: Double): Double {
        return Math.round(value / 10.0) * 10.0
    }

    private fun fetchInventory() {
        viewModelScope.launch {
            try {
                inventoryRepo.getInventory().collect { list ->
                    _inventory.value = list
                    if (list.isEmpty()) {
                        initializeDefaultInventory()
                    }
                }
            } catch (e: Exception) {
                _uiEvent.emit("Inventory Error: ${e.localizedMessage}")
            }
        }
    }

    private fun initializeDefaultInventory() {
        viewModelScope.launch(exceptionHandler) {
            inventoryRepo.addItem(InventoryItem(name = "Paper Reams", currentStock = 10.0, unit = "reams", lowStockThreshold = 3.0))
            inventoryRepo.addItem(InventoryItem(name = "Black Ink", currentStock = 500.0, unit = "ml", lowStockThreshold = 100.0))
            inventoryRepo.addItem(InventoryItem(name = "Color Ink", currentStock = 500.0, unit = "ml", lowStockThreshold = 100.0))
        }
    }

    fun updateStock(id: String, newStock: Double) {
        viewModelScope.launch(exceptionHandler) {
            inventoryRepo.updateStock(id, newStock)
        }
    }

    fun resolveVariance(shiftId: String, status: String, comment: String, admin: com.raymond.cms.model.UserModel) {
        viewModelScope.launch(exceptionHandler) {
            val shift = _allShifts.value.find { it.id == shiftId } ?: return@launch
            val updated = shift.copy(
                varianceStatus = status,
                adminReviewComment = comment,
                varianceReviewedBy = admin.uid,
                varianceReviewedByName = admin.name,
                varianceReviewedAt = Date()
            )
            shiftRepo.updateShift(updated)
            _uiEvent.emit("Variance $status successfully.")
        }
    }

    fun onSearchQueryChange(newQuery: String) {
        _searchQuery.value = newQuery
    }

    fun getLastClosingBalance(): Double {
        return _transactions.value.maxByOrNull { it.timestamp }?.closingAmount ?: 0.0
    }

    private fun fetchTransactions() {
        viewModelScope.launch {
            repository.getTransactions().collect { resource ->
                when (resource) {
                    is Resource.Loading -> {
                        _isLoading.value = true
                    }
                    is Resource.Success -> {
                        _transactions.value = resource.data
                        _isLoading.value = false
                        updateReports()
                    }
                    is Resource.Error -> {
                        _isLoading.value = false
                        _uiEvent.emit(resource.message)
                    }
                }
            }
        }
    }

    private fun updateReports() {
        val legacy = _transactions.value
        val transactions = _allTransactions.value
        val expenses = _allExpenses.value
        val shifts = _allShifts.value
        val period = _selectedPeriod.value
        val timestamp = _selectedDate.value
        val range = _dateRange.value

        viewModelScope.launch(Dispatchers.Default) {
            try {
                val (start, end) = when (period) {
                    ReportPeriod.DAILY -> {
                        val cal = DateTimeUtils.getCalendar(timestamp)
                        cal.set(Calendar.HOUR_OF_DAY, 0)
                        cal.set(Calendar.MINUTE, 0)
                        cal.set(Calendar.SECOND, 0)
                        cal.set(Calendar.MILLISECOND, 0)
                        val s = cal.timeInMillis
                        cal.set(Calendar.HOUR_OF_DAY, 23)
                        cal.set(Calendar.MINUTE, 59)
                        cal.set(Calendar.SECOND, 59)
                        cal.set(Calendar.MILLISECOND, 999)
                        Pair(s, cal.timeInMillis)
                    }
                    ReportPeriod.WEEKLY -> {
                        val cal = DateTimeUtils.getCalendar(timestamp)
                        while (cal.get(Calendar.DAY_OF_WEEK) != Calendar.MONDAY) {
                            cal.add(Calendar.DAY_OF_YEAR, -1)
                        }
                        cal.set(Calendar.HOUR_OF_DAY, 0)
                        cal.set(Calendar.MINUTE, 0)
                        cal.set(Calendar.SECOND, 0)
                        cal.set(Calendar.MILLISECOND, 0)
                        val s = cal.timeInMillis
                        cal.add(Calendar.DAY_OF_YEAR, 6)
                        cal.set(Calendar.HOUR_OF_DAY, 23)
                        cal.set(Calendar.MINUTE, 59)
                        cal.set(Calendar.SECOND, 59)
                        cal.set(Calendar.MILLISECOND, 999)
                        Pair(s, cal.timeInMillis)
                    }
                    ReportPeriod.MONTHLY -> {
                        val cal = DateTimeUtils.getCalendar(timestamp)
                        cal.set(Calendar.DAY_OF_MONTH, 1)
                        cal.set(Calendar.HOUR_OF_DAY, 0)
                        cal.set(Calendar.MINUTE, 0)
                        cal.set(Calendar.SECOND, 0)
                        cal.set(Calendar.MILLISECOND, 0)
                        val s = cal.timeInMillis
                        cal.add(Calendar.MONTH, 1)
                        cal.add(Calendar.DAY_OF_YEAR, -1)
                        cal.set(Calendar.HOUR_OF_DAY, 23)
                        cal.set(Calendar.MINUTE, 59)
                        cal.set(Calendar.SECOND, 59)
                        cal.set(Calendar.MILLISECOND, 999)
                        Pair(s, cal.timeInMillis)
                    }
                    ReportPeriod.YEARLY -> {
                        val cal = DateTimeUtils.getCalendar(timestamp)
                        cal.set(Calendar.DAY_OF_YEAR, 1)
                        cal.set(Calendar.HOUR_OF_DAY, 0)
                        cal.set(Calendar.MINUTE, 0)
                        cal.set(Calendar.SECOND, 0)
                        cal.set(Calendar.MILLISECOND, 0)
                        val s = cal.timeInMillis
                        cal.set(Calendar.MONTH, 11)
                        cal.set(Calendar.DAY_OF_MONTH, 31)
                        cal.set(Calendar.HOUR_OF_DAY, 23)
                        cal.set(Calendar.MINUTE, 59)
                        cal.set(Calendar.SECOND, 59)
                        cal.set(Calendar.MILLISECOND, 999)
                        Pair(s, cal.timeInMillis)
                    }
                    ReportPeriod.CUSTOM -> range ?: Pair(0L, Long.MAX_VALUE)
                }

                val report = getInsightsUseCase.execute(
                    legacy,
                    transactions,
                    expenses,
                    _allInvestments.value,
                    shifts,
                    start,
                    end
                )
                withContext(Dispatchers.Main) {
                    _reportState.value = report
                    _insights.value = report.insights
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    _uiEvent.emit("Report Error: ${e.localizedMessage}")
                }
            }
        }
    }

    fun getTransactionByDate(dateMillis: Long): DailyTransaction? {
        val targetId = DailyTransaction.createId(dateMillis)
        return _transactions.value.find { it.id == targetId }
    }

    fun getAggregatedDataForDate(dateMillis: Long): Map<String, Any> {
        val dateKey = DateTimeUtils.getFormat("yyyy-MM-dd").format(Date(dateMillis))
        val shifts = _allShifts.value.filter { it.date == dateKey }
        val expenses = _allExpenses.value.filter { 
            DateTimeUtils.getFormat("yyyy-MM-dd").format(Date(it.timestamp)) == dateKey && 
            (it.status == "COMPLETED" || it.status == "APPROVED")
        }
        val transactions = _allTransactions.value.filter { it.date == dateKey }
        
        return mapOf(
            "openingCash" to shifts.sumOf { it.openingCash },
            "openingMpesa" to shifts.sumOf { it.openingMpesa },
            "openingTill" to shifts.sumOf { it.openingTill },
            "closingCash" to shifts.sumOf { it.closingCash },
            "closingMpesa" to shifts.sumOf { it.closingMpesa },
            "closingTill" to shifts.sumOf { it.closingTill },
            "expenses" to expenses.map { ExpenseItem(it.category, it.amount) },
            "salesTotal" to transactions.sumOf { it.totalAmount }
        )
    }

    fun addTransaction(
        date: Long,
        openingCash: Double,
        openingMpesa: Double,
        openingTill: Double,
        expenses: List<ExpenseItem>,
        serviceRevenue: Map<String, Double>,
        meals: Double,
        closingCash: Double,
        closingMpesa: Double,
        closingTill: Double,
        onComplete: () -> Unit
    ) {
        viewModelScope.launch(exceptionHandler) {
            val result = addTransactionUseCase.execute(
                date, openingCash, openingMpesa, openingTill, 
                expenses, serviceRevenue, meals, 
                closingCash, closingMpesa, closingTill
            )
            
            when (result) {
                is AddTransactionUseCase.Result.Success -> {
                    _uiEvent.emit("Record Saved Successfully!")
                    onComplete()
                }
                is AddTransactionUseCase.Result.ExcessMoved -> {
                    _uiEvent.emit(result.message)
                    onComplete()
                }
                is AddTransactionUseCase.Result.Error -> {
                    _uiEvent.emit("Failed to Save: ${result.message}")
                }
            }
        }
    }

    fun deleteTransaction(id: String) {
        viewModelScope.launch(exceptionHandler) {
            repository.deleteTransaction(id)
            _uiEvent.emit("Record Deleted")
        }
    }

    fun updateDailySummary(
        recordId: String,
        openingCash: Double,
        openingMpesa: Double,
        openingTill: Double,
        closingCash: Double,
        closingMpesa: Double,
        closingTill: Double,
        expenses: Double,
        meals: Double,
        reason: String,
        user: UserModel
    ) {
        if (reason.isEmpty()) {
            sendEvent("A mandatory reason for edit is required.")
            return
        }

        viewModelScope.launch(exceptionHandler) {
            val isLegacy = recordId.startsWith("LEGACY_")
            val id = if (isLegacy) recordId.substringAfter("LEGACY_") else recordId
            
            val originalData: Map<String, Any?>
            val proposedData = mapOf(
                "openingCash" to openingCash,
                "openingMpesa" to openingMpesa,
                "openingTill" to openingTill,
                "closingCash" to closingCash,
                "closingMpesa" to closingMpesa,
                "closingTill" to closingTill,
                "meals" to meals,
                "expenses" to expenses
            )

            if (isLegacy) {
                val legacy = getTransactionByDate(id.toLongOrNull() ?: 0L) ?: return@launch
                originalData = mapOf(
                    "openingCash" to legacy.openingCash,
                    "openingMpesa" to legacy.openingMpesa,
                    "openingTill" to legacy.openingTill,
                    "closingCash" to legacy.closingCash,
                    "closingMpesa" to legacy.closingMpesa,
                    "closingTill" to legacy.closingTill,
                    "meals" to legacy.meals,
                    "expenses" to legacy.totalExpenses
                )
            } else {
                val shift = _allShifts.value.find { it.id == id } ?: return@launch
                originalData = mapOf(
                    "openingCash" to shift.openingCash,
                    "openingMpesa" to shift.openingMpesa,
                    "openingTill" to shift.openingTill,
                    "closingCash" to shift.closingCash,
                    "closingMpesa" to shift.closingMpesa,
                    "closingTill" to shift.closingTill,
                    "meals" to 0.0, // Shift meals not tracked in top level
                    "expenses" to 0.0
                )
            }

            if (user.role == UserRole.STAFF) {
                // Requirement: Staff edits must go through approval
                val request = ApprovalRequest(
                    recordId = recordId,
                    recordType = RecordType.DAILY_SUMMARY,
                    requestedBy = user.uid,
                    requestedByName = user.name.ifEmpty { user.email },
                    reason = reason,
                    originalData = originalData,
                    proposedData = proposedData
                )
                approvalRepo.createRequest(request)
                _uiEvent.emit("Edit request submitted for Admin approval.")
                return@launch
            }

            // If Admin, proceed with direct update (original logic)
            if (isLegacy) {
                val legacy = getTransactionByDate(id.toLongOrNull() ?: 0L) ?: return@launch
                val updated = legacy.copy(
                    openingCash = openingCash,
                    openingMpesa = openingMpesa,
                    openingTill = openingTill,
                    openingAmount = openingCash + openingMpesa + openingTill,
                    closingCash = closingCash,
                    closingMpesa = closingMpesa,
                    closingTill = closingTill,
                    closingAmount = closingCash + closingMpesa + closingTill,
                    meals = meals,
                    detailedExpenses = listOf(com.raymond.cms.model.ExpenseItem("Audit Correction", expenses))
                )
                repository.addTransaction(updated)
                
                auditRepo.log(AuditLog(
                    action = "LEGACY_RECORD_EDIT",
                    userId = user.uid,
                    userName = user.name,
                    userRole = user.role.name,
                    recordId = recordId,
                    description = "Admin edited legacy summary. Reason: $reason",
                    oldValue = "Old Total: ${legacy.totalClosing}",
                    newValue = "New Total: ${updated.totalClosing}"
                ))
            } else {
                val shift = _allShifts.value.find { it.id == id } ?: return@launch
                val updated = shift.copy(
                    status = if (closingCash + closingMpesa + closingTill > 0) ShiftStatus.CLOSED else shift.status,
                    openingCash = openingCash,
                    openingMpesa = openingMpesa,
                    openingTill = openingTill,
                    closingCash = closingCash,
                    closingMpesa = closingMpesa,
                    closingTill = closingTill,
                    clockOutTime = if (shift.clockOutTime == null && (closingCash + closingMpesa + closingTill > 0)) System.currentTimeMillis() else shift.clockOutTime,
                    openingBalanceDifference = (openingCash + openingMpesa + openingTill) - shift.previousClosingTotal
                )
                shiftRepo.updateShift(updated)
                
                auditRepo.log(AuditLog(
                    action = "SHIFT_RECORD_EDIT",
                    userId = user.uid,
                    userName = user.name,
                    userRole = user.role.name,
                    recordId = id,
                    description = "Admin edited shift summary. Reason: $reason",
                    oldValue = "Old Total: ${shift.totalClosing}",
                    newValue = "New Total: ${updated.totalClosing}"
                ))
            }
            _uiEvent.emit("Summary updated successfully.")
        }
    }

    fun exportToCsv(context: Context) {
        viewModelScope.launch {
            try {
                val report = _reportState.value
                val csvContent = StringBuilder()
                csvContent.append("ID,Date,Time,Service/Category,Amount,Staff,Method\n")
                
                report.filteredTransactions.forEach { tx ->
                    val time = DateTimeUtils.getFormat("hh:mm a").format(Date(tx.timestamp))
                    val serviceName = if (tx.items.isNotEmpty()) tx.items.first().name else "Sale"
                    csvContent.append("${tx.id},${tx.date},$time,$serviceName,${tx.totalAmount},${tx.staffName},${tx.paymentMethod.name}\n")
                }

                report.filteredExpenses.forEach { ex ->
                    val date = DateTimeUtils.getFormat("yyyy-MM-dd").format(Date(ex.timestamp))
                    val time = DateTimeUtils.getFormat("hh:mm a").format(Date(ex.timestamp))
                    csvContent.append("${ex.id},$date,$time,${ex.category},${ex.amount},${ex.staffName},${ex.paymentMethod.name}\n")
                }

                val fileName = "CMS_Business_Report.csv"
                val file = File(context.cacheDir, fileName)
                file.writeText(csvContent.toString())

                val uri: Uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/csv"
                    putExtra(Intent.EXTRA_SUBJECT, "Business Report Export")
                    putExtra(Intent.EXTRA_STREAM, uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                context.startActivity(Intent.createChooser(intent, "Share Business Report"))
            } catch (e: Exception) {
                _uiEvent.emit("Export Failed: ${e.localizedMessage}")
            }
        }
    }

    private val _isMigrating = MutableStateFlow(false)
    val isMigrating: StateFlow<Boolean> = _isMigrating.asStateFlow()
}
