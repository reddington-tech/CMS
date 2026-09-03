package com.raymond.cms.domain

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.raymond.cms.model.*
import com.raymond.cms.repository.FinancialRepository
import com.raymond.cms.repository.ShiftRepository
import com.raymond.cms.util.NotificationHelper
import com.raymond.cms.util.DateTimeUtils
import kotlinx.coroutines.flow.first
import java.util.*

class ShiftWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val type = inputData.getString("type") ?: return Result.failure()
        val shiftRepo = ShiftRepository()
        val financialRepo = FinancialRepository()
        val shiftUseCase = ShiftManagementUseCase()

        return when (type) {
            "AUTO_CLOCK_OUT" -> {
                performAutoClockOut(shiftRepo, financialRepo, shiftUseCase)
            }
            "REMINDER" -> {
                sendReminders(shiftRepo)
            }
            else -> Result.failure()
        }
    }

    private suspend fun performAutoClockOut(shiftRepo: ShiftRepository, financialRepo: FinancialRepository, shiftUseCase: ShiftManagementUseCase): Result {
        val activeShifts = shiftRepo.getActiveShifts().first()
        
        activeShifts.forEach { shift ->
            // Calculate current totals using existing business logic
            val transactions = financialRepo.getTransactionsForShift(shift.id).first()
            val expenses = financialRepo.getExpensesForShift(shift.id).first()
            
            val totalRevenue = transactions.sumOf { it.totalAmount }
            val totalExpenses = expenses.sumOf { it.amount }
            
            // Expected closing based on recorded items
            val expectedClosingTotal = shift.totalOpening + totalRevenue - totalExpenses
            
            // For auto clock out, we estimate the breakdown based on expected total
            // Since it's automatic, we might not know the exact cash/mpesa split
            // So we mark it clearly as system generated
            
            val calendar = DateTimeUtils.getCalendar()
            try {
                val shiftDate = DateTimeUtils.getFormat("yyyy-MM-dd").parse(shift.date)
                if (shiftDate != null) {
                    calendar.time = shiftDate
                    calendar.set(Calendar.HOUR_OF_DAY, 23)
                    calendar.set(Calendar.MINUTE, 59)
                    calendar.set(Calendar.SECOND, 59)
                }
            } catch (e: Exception) {}

            val result = shiftUseCase.closeShift(
                currentShift = shift,
                closingCash = expectedClosingTotal, 
                closingMpesa = 0.0,
                closingTill = 0.0,
                reason = "SYSTEM/AUTOMATIC CLOCK-OUT at 11:59 PM",
                clockOutTime = calendar.timeInMillis
            )

            if (result is ShiftManagementUseCase.ShiftResult.Success) {
                NotificationHelper.sendNotification(
                    applicationContext,
                    "Automatic Clock-Out",
                    "Automatic Clock-Out: ${shift.staffName} was automatically clocked out at 11:59 PM. System-calculated Closing: KSh $expectedClosingTotal."
                )
            }
        }
        return Result.success()
    }

    private suspend fun sendReminders(shiftRepo: ShiftRepository): Result {
        val activeShifts = shiftRepo.getActiveShifts().first()
        activeShifts.forEach { _ ->
            // In a real multi-user app, we'd target specific users. 
            // For this implementation, we send a local notification if any shift is active on this device.
            NotificationHelper.sendNotification(
                applicationContext,
                "Clock-Out Reminder",
                "Clock-Out Reminder: You are still clocked in. Please clock out when you have completed your work."
            )
        }
        return Result.success()
    }
}
