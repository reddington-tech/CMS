package com.raymond.cms.domain

import com.raymond.cms.model.*
import com.raymond.cms.repository.AuditRepository
import com.raymond.cms.repository.ShiftRepository
import com.raymond.cms.util.DateTimeUtils
import kotlinx.coroutines.flow.first
import java.util.*

class ShiftManagementUseCase(
    private val shiftRepo: ShiftRepository = ShiftRepository(),
    private val auditRepo: AuditRepository = AuditRepository(),
    private val transactionRepo: com.raymond.cms.repository.TransactionRepository = com.raymond.cms.repository.TransactionRepository()
) {
    sealed class ShiftResult {
        data class Success(val message: String, val shift: Shift? = null) : ShiftResult()
        data class ActiveShiftFound(val shift: Shift) : ShiftResult()
        data class Error(val message: String) : ShiftResult()
    }

    suspend fun startShift(
        user: UserModel,
        openingCash: Double,
        openingMpesa: Double,
        openingTill: Double
    ): ShiftResult {
        return try {
            // 1. Check for existing active shift
            val activeShift = shiftRepo.getActiveShiftSync(user.uid)
            val todayDate = DateTimeUtils.getFormat("yyyy-MM-dd", Locale.US).format(Date())

            if (activeShift != null) {
                // If it's a stale shift from a previous day, auto-close it (Requirement: Automated Clock-out)
                if (activeShift.date != todayDate) {
                    closeShift(
                        currentShift = activeShift,
                        closingCash = activeShift.totalOpening, // Minimal fallback
                        closingMpesa = 0.0,
                        closingTill = 0.0,
                        reason = "SYSTEM/STALE CLOCK-OUT: New shift started on $todayDate"
                    )
                } else {
                    return ShiftResult.ActiveShiftFound(activeShift)
                }
            }
            
            // 2. Get latest BUSINESS closing balance
            val lastBusinessShift = try {
                shiftRepo.getLastClosingBalance()
            } catch (e: Exception) {
                null
            }
            
            val expectedCash = lastBusinessShift?.closingCash ?: 0.0
            val expectedMpesa = lastBusinessShift?.closingMpesa ?: 0.0
            val expectedTill = lastBusinessShift?.closingTill ?: 0.0
            val expectedTotal = expectedCash + expectedMpesa + expectedTill
            
            val actualTotal = openingCash + openingMpesa + openingTill
            val totalDifference = actualTotal - expectedTotal
            
            val shift = Shift(
                staffId = user.uid,
                staffName = user.name.ifEmpty { user.email },
                date = todayDate,
                clockInDate = todayDate,
                openingCash = openingCash,
                openingMpesa = openingMpesa,
                openingTill = openingTill,
                expectedOpeningCash = expectedCash,
                expectedOpeningMpesa = expectedMpesa,
                expectedOpeningTill = expectedTill,
                previousClosingTotal = expectedTotal,
                openingBalanceDifference = totalDifference,
                flaggedForReview = Math.abs(totalDifference) > 0.1
            )
            
            val shiftId = shiftRepo.startShift(shift)
            val finalShift = shift.copy(id = shiftId)
            
            auditRepo.log(AuditLog(
                action = "SHIFT_START",
                userId = user.uid,
                userName = user.name.ifEmpty { user.email },
                userRole = user.role.name,
                recordId = shiftId,
                description = "Started shift. Opening: $actualTotal, Expected: $expectedTotal, Diff: $totalDifference"
            ))
            
            ShiftResult.Success("Shift started successfully", finalShift)
        } catch (e: Exception) {
            ShiftResult.Error("Failed to start shift: ${e.localizedMessage}")
        }
    }

    suspend fun closeShift(
        currentShift: Shift,
        closingCash: Double,
        closingMpesa: Double,
        closingTill: Double,
        meals: Double = 0.0,
        additionalExpenses: List<com.raymond.cms.model.ExpenseItem> = emptyList(),
        admin: UserModel? = null,
        reason: String? = null,
        clockOutTime: Long? = null
    ): ShiftResult {
        return try {
            val isOverride = admin != null
            val isSystem = reason?.contains("SYSTEM") == true
            
            val updatedShift = currentShift.copy(
                status = ShiftStatus.CLOSED,
                clockOutTime = clockOutTime ?: System.currentTimeMillis(),
                closingCash = closingCash,
                closingMpesa = closingMpesa,
                closingTill = closingTill,
                adminReviewComment = reason ?: currentShift.adminReviewComment
            )
            shiftRepo.updateShift(updatedShift)

            // Requirement: Create/Update a matching DailyTransaction (Summary) for analytics
            // We use the date as the ID. If it already exists, we aggregate.
            val existingSummary = transactionRepo.getTransactionByIdSync(currentShift.date)
            
            val summary = if (existingSummary != null) {
                existingSummary.copy(
                    openingCash = existingSummary.openingCash + currentShift.openingCash,
                    openingMpesa = existingSummary.openingMpesa + currentShift.openingMpesa,
                    openingTill = existingSummary.openingTill + currentShift.openingTill,
                    closingCash = existingSummary.closingCash + closingCash,
                    closingMpesa = existingSummary.closingMpesa + closingMpesa,
                    closingTill = existingSummary.closingTill + closingTill,
                    meals = existingSummary.meals + meals,
                    detailedExpenses = existingSummary.detailedExpenses + additionalExpenses
                )
            } else {
                DailyTransaction(
                    id = currentShift.date,
                    timestamp = currentShift.clockInTime,
                    openingCash = currentShift.openingCash,
                    openingMpesa = currentShift.openingMpesa,
                    openingTill = currentShift.openingTill,
                    detailedExpenses = additionalExpenses,
                    meals = meals,
                    closingCash = closingCash,
                    closingMpesa = closingMpesa,
                    closingTill = closingTill
                )
            }
            transactionRepo.addTransaction(summary)
            
            val auditAction = if (isSystem) "SYSTEM_SHIFT_CLOSE" else if (isOverride) "ADMIN_SHIFT_OVERRIDE" else "SHIFT_CLOSE"
            val auditUserId = admin?.uid ?: currentShift.staffId
            val auditUserName = admin?.name ?: (if (isSystem) "System" else currentShift.staffName)
            val auditDescription = if (isSystem) {
                "Automatically closed shift. $reason"
            } else if (isOverride) {
                "Admin $auditUserName manually closed shift for ${currentShift.staffName}. Reason: $reason"
            } else {
                "Closed shift with summary. Total Closing: ${closingCash + closingMpesa + closingTill}"
            }

            auditRepo.log(AuditLog(
                action = auditAction,
                userId = auditUserId,
                userName = auditUserName,
                userRole = admin?.role?.name ?: (if (isSystem) "SYSTEM" else UserRole.STAFF.name),
                recordId = currentShift.id,
                description = auditDescription
            ))
            
            ShiftResult.Success(if (isSystem) "Shift closed by system" else if (isOverride) "Shift closed by Admin" else "Shift closed successfully")
        } catch (e: Exception) {
            ShiftResult.Error("Failed to close shift: ${e.localizedMessage}")
        }
    }
}
