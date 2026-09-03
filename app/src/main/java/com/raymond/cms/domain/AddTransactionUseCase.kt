package com.raymond.cms.domain

import com.raymond.cms.model.DailyTransaction
import com.raymond.cms.model.ExpenseItem
import com.raymond.cms.model.InventoryItem
import com.raymond.cms.repository.InventoryRepository
import com.raymond.cms.repository.TransactionRepository
import com.raymond.cms.util.Resource
import kotlinx.coroutines.flow.first

class AddTransactionUseCase(
    private val transactionRepo: TransactionRepository = TransactionRepository(),
    private val inventoryRepo: InventoryRepository = InventoryRepository()
) {
    sealed class Result {
        object Success : Result()
        data class ExcessMoved(val message: String) : Result()
        data class Error(val message: String) : Result()
    }

    suspend fun execute(
        date: Long,
        openingCash: Double,
        openingMpesa: Double,
        openingTill: Double,
        expenses: List<ExpenseItem>,
        serviceRevenue: Map<String, Double>,
        meals: Double,
        closingCash: Double,
        closingMpesa: Double,
        closingTill: Double
    ): Result {
        return try {
            // Requirement #20: Prevent duplicate daily summary records
            val docId = DailyTransaction.createId(date)
            val existing = transactionRepo.getTransactionByIdSync(docId)
            
            if (existing != null) {
                return Result.Error("A record already exists for $docId. Please use the Edit feature instead.")
            }

            val mutableExpenses = expenses.toMutableList()
            var finalClosingCash = closingCash
            var excessMoved = false

            // Enforce KSh 50,000 drawer limit on CASH only
            while (finalClosingCash > 50000.0) {
                mutableExpenses.add(ExpenseItem("Other Investment", 50000.0))
                finalClosingCash -= 50000.0
                excessMoved = true
            }

            val transaction = DailyTransaction(
                id = docId,
                timestamp = date,
                openingCash = openingCash,
                openingMpesa = openingMpesa,
                openingTill = openingTill,
                detailedExpenses = mutableExpenses,
                serviceRevenue = serviceRevenue,
                meals = meals,
                closingCash = finalClosingCash,
                closingMpesa = closingMpesa,
                closingTill = closingTill
            )
            
            transactionRepo.addTransaction(transaction)

            // Inventory Logic: If "Reams" purchased, update stock
            if (expenses.any { it.description.contains("Reams", ignoreCase = true) }) {
                val inventory = inventoryRepo.getInventorySync()
                val reamsItem = inventory.find { it.name.contains("Reams", ignoreCase = true) }
                if (reamsItem != null) {
                    inventoryRepo.updateStock(reamsItem.id, reamsItem.currentStock + 5.0)
                }
            }

            if (excessMoved) {
                Result.ExcessMoved("Drawer limit reached! Excess moved to Other Investment.")
            } else {
                Result.Success
            }
        } catch (e: Exception) {
            Result.Error(e.localizedMessage ?: "Unknown error occurred")
        }
    }
}
