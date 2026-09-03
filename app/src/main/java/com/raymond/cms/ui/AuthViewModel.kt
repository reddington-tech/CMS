package com.raymond.cms.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.raymond.cms.model.AttendanceRecord
import com.raymond.cms.model.Shift
import com.raymond.cms.model.ShiftStatus
import com.raymond.cms.model.AuditLog
import com.raymond.cms.model.UserModel
import com.raymond.cms.model.UserRole
import com.raymond.cms.model.Service
import com.raymond.cms.model.Transaction
import com.raymond.cms.model.Expense
import com.raymond.cms.model.ApprovalRequest
import com.raymond.cms.model.RecordType
import com.raymond.cms.repository.*
import com.raymond.cms.domain.ShiftManagementUseCase
import com.raymond.cms.util.DateTimeUtils
import com.raymond.cms.util.FirestoreCollections
import com.google.firebase.auth.EmailAuthProvider
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.Date
import java.util.Locale

class AuthViewModel : BaseViewModel() {
    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()
    private val attendanceRepo = AttendanceRepository()
    private val shiftRepo = ShiftRepository()
    private val auditRepo = AuditRepository()
    private val serviceRepo = ServiceRepository()
    private val approvalRepo = ApprovalRepository()
    private val financialRepo = FinancialRepository()
    
    private val shiftUseCase = ShiftManagementUseCase(shiftRepo, auditRepo)

    private var staffListener: com.google.firebase.firestore.ListenerRegistration? = null
    private val _staffList = MutableStateFlow<List<UserModel>>(emptyList())

    private val _currentUser = MutableStateFlow<UserModel?>(null)
    val currentUser: StateFlow<UserModel?> = _currentUser.asStateFlow()

    private val _isInitialLoadComplete = MutableStateFlow(false)
    val isInitialLoadComplete: StateFlow<Boolean> = _isInitialLoadComplete.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _attendance = MutableStateFlow<AttendanceRecord?>(null)
    val attendance: StateFlow<AttendanceRecord?> = _attendance.asStateFlow()

    private val _activeShift = MutableStateFlow<Shift?>(null)
    val activeShift: StateFlow<Shift?> = _activeShift

    private val _lastShift = MutableStateFlow<Shift?>(null)
    val lastShift: StateFlow<Shift?> = _lastShift.asStateFlow()

    private val _services = MutableStateFlow<List<Service>>(emptyList())
    val services: StateFlow<List<Service>> = _services.asStateFlow()

    private val _shiftTransactions = MutableStateFlow<List<Transaction>>(emptyList())
    val shiftTransactions: StateFlow<List<Transaction>> = _shiftTransactions.asStateFlow()

    private val _shiftExpenses = MutableStateFlow<List<Expense>>(emptyList())
    val shiftExpenses: StateFlow<List<Expense>> = _shiftExpenses.asStateFlow()

    private val _isLocked = MutableStateFlow(false)
    val isLocked: StateFlow<Boolean> = _isLocked.asStateFlow()

    fun setLocked(locked: Boolean) {
        if (_currentUser.value != null) { 
            _isLocked.value = locked
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val allStaffShifts: StateFlow<List<Pair<UserModel, Shift?>>> = _staffList.flatMapLatest { staff ->
        shiftRepo.getActiveShifts().map { shifts ->
            val activeShiftsMap = shifts.associateBy { it.staffId }
            staff.map { it to activeShiftsMap[it.uid] }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // uiEvent is now in BaseViewModel

    init {
        val firebaseUser = auth.currentUser
        if (firebaseUser != null) {
            initializeUserSession(firebaseUser.uid)
        } else {
            _isLoading.value = false
            _isInitialLoadComplete.value = true
        }
    }

    private fun initializeUserSession(uid: String) {
        viewModelScope.launch(exceptionHandler) {
            _isLoading.value = true
            _isInitialLoadComplete.value = false
            
            // Sync all essential data in parallel
            val profileTask = async { fetchUserProfileSync(uid) }
            val shiftTask = async { observeActiveShiftSync(uid) }
            val attendanceTask = async { observeAttendance(uid) }
            
            profileTask.await()
            shiftTask.await()
            // attendanceTask doesn't need to be awaited as it's a Flow observer
            
            _isInitialLoadComplete.value = true
            _isLoading.value = false
        }
    }

    private suspend fun fetchUserProfileSync(uid: String) {
        try {
            val doc = firestore.collection(FirestoreCollections.USERS).document(uid).get().await()
            val user = doc.toObject(UserModel::class.java)
            
            if (user != null && (!user.isActive || user.isRemoved)) {
                auth.signOut()
                _currentUser.value = null
                _error.value = "Your account is suspended or removed. Please contact Admin."
                return
            }

            _currentUser.value = user
            if (user != null) {
                observeServices()
                if (user.role == UserRole.ADMIN) fetchAllStaff()
                
                // Only log if not already logged (optional check)
                auditRepo.log(AuditLog(
                    action = "LOGIN",
                    userId = user.uid,
                    userName = user.name,
                    userRole = user.role.name,
                    description = "User session initialized"
                ))
            }
        } catch (e: Exception) {
            _error.value = "Failed to load profile: ${e.localizedMessage}"
        }
    }

    private suspend fun observeActiveShiftSync(uid: String) {
        // Stop any existing shift observation
        this.shiftDataJob?.cancel()
        
        // Use a one-shot fetch for the initial startup check (Requirement #1 & #21)
        // This is more reliable than .first() on a listener which might return empty cache first
        try {
            val shift = shiftRepo.getActiveShiftSync(uid)
            _activeShift.value = shift
            if (shift != null) {
                startObservingShiftData(shift.id)
            }
        } catch (e: Exception) {
            // Handle query error
        }
        
        // Start the perpetual observer in background to catch remote changes
        observeActiveShift(uid)
    }

    private fun observeServices() {
        viewModelScope.launch(exceptionHandler) {
            serviceRepo.getServices()
                .catch { e -> _uiEvent.emit("Service Load Error: ${e.localizedMessage}") }
                .collect {
                    if (it.isEmpty() && currentUser.value?.role == UserRole.ADMIN) {
                        serviceRepo.initializeDefaultServices()
                    }
                    _services.value = it
                }
        }
    }

    fun requestPriceChange(service: Service, newPrice: Double, reason: String) {
        val user = _currentUser.value ?: return
        viewModelScope.launch(exceptionHandler) {
            val request = ApprovalRequest(
                recordId = service.id,
                recordType = RecordType.SERVICE_PRICE,
                requestedBy = user.uid,
                requestedByName = user.name.ifEmpty { user.email },
                reason = reason,
                originalData = mapOf("price" to service.price),
                proposedData = mapOf("price" to newPrice)
            )
            approvalRepo.createRequest(request)
            _uiEvent.emit("Price change request submitted for Admin approval")
        }
    }

    private var shiftDataJob: Job? = null

    private fun observeActiveShift(uid: String) {
        viewModelScope.launch {
            shiftRepo.getActiveShift(uid).collect { shift ->
                _activeShift.value = shift
                if (shift != null) {
                    startObservingShiftData(shift.id)
                } else {
                    stopObservingShiftData()
                }
            }
        }
    }

    private fun startObservingShiftData(shiftId: String) {
        this.shiftDataJob?.cancel()
        this.shiftDataJob = viewModelScope.launch {
            launch {
                financialRepo.getTransactionsForShift(shiftId).collect {
                    _shiftTransactions.value = it
                }
            }
            launch {
                financialRepo.getExpensesForShift(shiftId).collect {
                    _shiftExpenses.value = it
                }
            }
        }
    }

    private fun stopObservingShiftData() {
        this.shiftDataJob?.cancel()
        this.shiftDataJob = null
        _shiftTransactions.value = emptyList()
        _shiftExpenses.value = emptyList()
    }

    fun fetchLastShift() {
        viewModelScope.launch {
            try {
                _lastShift.value = shiftRepo.getLastClosingBalance()
            } catch (e: Exception) {
                // Silent fail for last shift fetch
            }
        }
    }

    fun startShift(openingCash: Double, openingMpesa: Double, openingTill: Double) {
        val user = _currentUser.value ?: return
        viewModelScope.launch(exceptionHandler) {
            _isLoading.value = true
            val result = shiftUseCase.startShift(user, openingCash, openingMpesa, openingTill)
            
            when (result) {
                is ShiftManagementUseCase.ShiftResult.Success -> {
                    _uiEvent.emit(result.message)
                    result.shift?.let {
                        if (it.openingBalanceDifference != 0.0) {
                            _uiEvent.emit("⚠️ Balance Discrepancy: KSh ${it.openingBalanceDifference} difference recorded.")
                        }
                    }
                    fetchUserProfile(user.uid)
                }
                is ShiftManagementUseCase.ShiftResult.ActiveShiftFound -> {
                    _activeShift.value = result.shift
                    startObservingShiftData(result.shift.id)
                    _uiEvent.emit("Active shift restored")
                }
                is ShiftManagementUseCase.ShiftResult.Error -> {
                    _error.value = result.message
                }
            }
            _isLoading.value = false
        }
    }

    fun closeShift(
        closingCash: Double, 
        closingMpesa: Double, 
        closingTill: Double,
        meals: Double = 0.0,
        additionalExpenses: List<com.raymond.cms.model.ExpenseItem> = emptyList(),
        verificationMethod: String = "Manual"
    ) {
        val currentShift = _activeShift.value ?: return
        viewModelScope.launch(exceptionHandler) {
            val result = shiftUseCase.closeShift(
                currentShift, 
                closingCash, 
                closingMpesa, 
                closingTill,
                meals,
                additionalExpenses
            )
            
            when (result) {
                is ShiftManagementUseCase.ShiftResult.Success -> {
                    auditRepo.log(AuditLog(
                        action = "SHIFT_CLOSED",
                        userId = currentShift.staffId,
                        userName = currentShift.staffName,
                        userRole = "STAFF",
                        recordId = currentShift.id,
                        description = "Shift closed successfully via $verificationMethod verification",
                        newValue = "Closing Total: ${closingCash + closingMpesa + closingTill}"
                    ))
                    _uiEvent.emit(result.message)
                    stopObservingShiftData()
                }
                is ShiftManagementUseCase.ShiftResult.Error -> {
                    _error.value = result.message
                }
                else -> {}
            }
        }
    }

    fun adminCloseShift(staffShift: Shift, closingCash: Double, closingMpesa: Double, closingTill: Double, reason: String) {
        val admin = _currentUser.value ?: return
        if (admin.role != UserRole.ADMIN) return
        
        viewModelScope.launch(exceptionHandler) {
            val result = shiftUseCase.closeShift(
                staffShift, 
                closingCash, 
                closingMpesa, 
                closingTill, 
                0.0, 
                emptyList(), 
                admin, 
                reason
            )
            when (result) {
                is ShiftManagementUseCase.ShiftResult.Success -> {
                    _uiEvent.emit(result.message)
                    // No need to fetch profile for Admin as it's not their shift
                }
                is ShiftManagementUseCase.ShiftResult.Error -> {
                    _uiEvent.emit(result.message)
                }
                else -> {}
            }
        }
    }

    private fun observeAttendance(uid: String) {
        viewModelScope.launch {
            attendanceRepo.getTodayAttendance(uid).collect { record ->
                _attendance.value = record
            }
        }
    }

    fun clockIn() {
        val user = _currentUser.value ?: return
        viewModelScope.launch(exceptionHandler) {
            try {
                attendanceRepo.clockIn(user.uid, user.email)
                
                auditRepo.log(AuditLog(
                    action = "CLOCK_IN",
                    userId = user.uid,
                    userName = user.name.ifEmpty { user.email },
                    userRole = user.role.name,
                    description = "Staff clocked in"
                ))
            } catch (e: Exception) {
                _error.value = "Clock In Failed: ${e.localizedMessage}"
            }
        }
    }

    fun clockOut() {
        val user = _currentUser.value ?: return
        viewModelScope.launch(exceptionHandler) {
            try {
                attendanceRepo.clockOut(user.uid)
                
                auditRepo.log(AuditLog(
                    action = "CLOCK_OUT",
                    userId = user.uid,
                    userName = user.name.ifEmpty { user.email },
                    userRole = user.role.name,
                    description = "Staff clocked out"
                ))
            } catch (e: Exception) {
                _error.value = "Clock Out Failed: ${e.localizedMessage}"
            }
        }
    }

    private fun fetchUserProfile(uid: String) {
        viewModelScope.launch(exceptionHandler) {
            val doc = firestore.collection(FirestoreCollections.USERS).document(uid).get().await()
            val user = doc.toObject(UserModel::class.java)
            _currentUser.value = user
            
            if (user != null) {
                observeServices()
                auditRepo.log(AuditLog(
                    action = "LOGIN",
                    userId = user.uid,
                    userName = user.name,
                    userRole = user.role.name,
                    description = "User logged in successfully"
                ))
            }
            
            if (user?.role == UserRole.ADMIN) {
                fetchAllStaff()
            }
            _isLoading.value = false
        }
    }

    fun fetchAllStaff() {
        if (staffListener != null) return
        
        staffListener = firestore.collection(FirestoreCollections.USERS)
            .whereEqualTo("role", UserRole.STAFF.name)
            .addSnapshotListener { snapshot, error ->
                if (error != null) return@addSnapshotListener
                if (snapshot != null) {
                    _staffList.value = snapshot.toObjects(UserModel::class.java)
                }
            }
    }

    suspend fun verifyPassword(password: String): Boolean {
        val email = _currentUser.value?.email ?: return false
        return try {
            val credential = EmailAuthProvider.getCredential(email, password)
            auth.currentUser?.reauthenticate(credential)?.await()
            true
        } catch (e: Exception) {
            false
        }
    }


    fun updateStaffCredentials(user: UserModel, newPassword: String = "") {
        val admin = _currentUser.value
        if (admin?.role != UserRole.ADMIN) {
            sendEvent("Permission Denied: Only Admins can update staff.")
            return
        }

        viewModelScope.launch(exceptionHandler) {
            // Update Firestore Profile
            firestore.collection(FirestoreCollections.USERS).document(user.uid).set(user).await()
            
            if (newPassword.isNotEmpty()) {
                // NOTE: Standard Firebase Client SDK does NOT allow an Admin to change another user's password.
                // This requires Firebase Admin SDK (Backend) or the user being logged in.
                _uiEvent.emit("Staff data updated. Security Note: Manual password overrides must be done via the Firebase Console.")
            } else {
                _uiEvent.emit("Staff details updated successfully")
            }
        }
    }

    fun login(email: String, pass: String) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _error.value = null
                auth.signInWithEmailAndPassword(email, pass).await()
                val uid = auth.currentUser?.uid ?: ""
                initializeUserSession(uid)
            } catch (e: Exception) {
                _isLoading.value = false
                _error.value = "Wrong Email or password try again"
            }
        }
    }

    fun register(email: String, pass: String, role: UserRole, name: String = "", idNo: String = "", phone: String = "") {
        val admin = _currentUser.value
        
        viewModelScope.launch(exceptionHandler) {
            _isLoading.value = true
            
            if (admin != null && admin.role == UserRole.ADMIN) {
                // Scenario: Admin adding staff. We use a secondary app instance 
                // to prevent the Admin from being signed out by the Firebase Client SDK.
                try {
                    val secondaryApp = try {
                        com.google.firebase.FirebaseApp.getInstance("secondary")
                    } catch (e: Exception) {
                        val options = com.google.firebase.FirebaseApp.getInstance().options
                        com.google.firebase.FirebaseApp.initializeApp(com.google.firebase.FirebaseApp.getInstance().applicationContext, options, "secondary")
                    }
                    
                    val secondaryAuth = com.google.firebase.auth.FirebaseAuth.getInstance(secondaryApp)
                    val result = secondaryAuth.createUserWithEmailAndPassword(email, pass).await()
                    val uid = result.user?.uid ?: ""
                    
                    val newUser = UserModel(
                        uid = uid,
                        email = email,
                        role = role,
                        name = name,
                        idNumber = idNo,
                        phoneNumber = phone
                    )
                    
                    // Save to Firestore using Admin's default app session
                    firestore.collection(FirestoreCollections.USERS).document(uid).set(newUser).await()
                    secondaryAuth.signOut()
                    
                    _uiEvent.emit("Staff account '$name' created successfully.")
                } catch (e: Exception) {
                    _uiEvent.emit("Failed to create staff: ${e.localizedMessage}")
                } finally {
                    _isLoading.value = false
                }
            } else {
                // Scenario: Normal registration (First-time setup or no user logged in)
                val result = auth.createUserWithEmailAndPassword(email, pass).await()
                val uid = result.user?.uid ?: ""
                val user = UserModel(
                    uid = uid,
                    email = email,
                    role = role,
                    name = name,
                    idNumber = idNo,
                    phoneNumber = phone
                )
                firestore.collection(FirestoreCollections.USERS).document(uid).set(user).await()
                initializeUserSession(uid)
            }
        }
    }

    fun logout() {
        val user = _currentUser.value
        viewModelScope.launch {
            if (user != null) {
                auditRepo.log(AuditLog(
                    action = "LOGOUT",
                    userId = user.uid,
                    userName = user.name.ifEmpty { user.email },
                    userRole = user.role.name,
                    description = "User logged out"
                ))
            }
            staffListener?.remove()
            staffListener = null
            auth.signOut()
            _currentUser.value = null
            _activeShift.value = null
            _isLoading.value = false
            _isInitialLoadComplete.value = true // Ensure we return to AuthNavigation instead of hanging on loading
        }
    }

    fun clearError() {
        _error.value = null
    }

    fun toggleStaffStatus(userId: String, isActive: Boolean) {
        val admin = _currentUser.value
        if (admin?.role != UserRole.ADMIN) {
            sendEvent("Permission Denied: Only Admins can modify staff status.")
            return
        }

        viewModelScope.launch(exceptionHandler) {
            firestore.collection(FirestoreCollections.USERS).document(userId).update("isActive", isActive).await()
            _uiEvent.emit(if (isActive) "Staff reactivated" else "Staff disabled")
        }
    }

    fun removeStaff(userId: String, isRemoved: Boolean = true) {
        val admin = _currentUser.value
        if (admin?.role != UserRole.ADMIN) {
            sendEvent("Permission Denied: Only Admins can remove staff.")
            return
        }

        viewModelScope.launch(exceptionHandler) {
            firestore.collection(FirestoreCollections.USERS).document(userId)
                .update("isRemoved", isRemoved).await()
            _uiEvent.emit(if (isRemoved) "Staff member removed (Soft Delete)" else "Staff member restored")
        }
    }
}
