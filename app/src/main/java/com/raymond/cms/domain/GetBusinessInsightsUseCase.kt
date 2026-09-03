package com.raymond.cms.domain

import com.raymond.cms.model.*
import com.raymond.cms.util.DateTimeUtils
import java.util.*

data class BusinessReport(
    val summary: ReportSummary = ReportSummary(),
    val dailyBreakdown: List<DailyBreakdown> = emptyList(),
    val monthlyBreakdown: List<MonthlyBreakdown> = emptyList(),
    val serviceBreakdown: Map<String, Double> = emptyMap(),
    val expensesByCategory: Map<String, Double> = emptyMap(),
    val staffPerformance: List<StaffPerformance> = emptyList(),
    val filteredTransactions: List<Transaction> = emptyList(),
    val filteredExpenses: List<Expense> = emptyList(),
    val filteredInvestments: List<Investment> = emptyList(),
    val filteredShifts: List<Shift> = emptyList(),
    val insights: List<String> = emptyList()
)

data class ReportSummary(
    val revenue: Double = 0.0,
    val expenses: Double = 0.0,
    val profit: Double = 0.0,
    val transactionCount: Int = 0,
    val investmentTotal: Double = 0.0,
    val openingBalance: Double = 0.0,
    val closingBalance: Double = 0.0,
    val cashDifference: Double = 0.0,
    val averageDailyRevenue: Double = 0.0,
    val averageDailyExpenses: Double = 0.0,
    val averageDailyProfit: Double = 0.0
)

data class DailyBreakdown(
    val id: String = "",
    val date: String, // YYYY-MM-DD or readable
    val revenue: Double = 0.0,
    val expenses: Double = 0.0,
    val profit: Double = 0.0,
    val timestamp: Long = 0L
)

data class MonthlyBreakdown(
    val monthName: String,
    val revenue: Double = 0.0,
    val expenses: Double = 0.0,
    val profit: Double = 0.0,
    val monthIndex: Int = 0
)

data class StaffPerformance(
    val staffId: String = "",
    val staffName: String = "",
    val transactionCount: Int = 0,
    val totalRevenue: Double = 0.0,
    val totalExpenses: Double = 0.0,
    val shiftCount: Int = 0
)

class GetBusinessInsightsUseCase {
    fun execute(
        legacyRecords: List<DailyTransaction>,
        transactions: List<Transaction>,
        expenses: List<Expense>,
        investments: List<Investment>,
        shifts: List<Shift>,
        startDate: Long? = null,
        endDate: Long? = null
    ): BusinessReport {
        // 1. Filter all data sources by date range
        val filteredLegacy = if (startDate != null && endDate != null) {
            legacyRecords.filter { it.timestamp in startDate..endDate }
        } else legacyRecords

        val filteredTx = if (startDate != null && endDate != null) {
            transactions.filter { it.timestamp in startDate..endDate }
        } else transactions

        val filteredEx = if (startDate != null && endDate != null) {
            expenses.filter { it.timestamp in startDate..endDate }
        } else expenses

        val filteredInv = if (startDate != null && endDate != null) {
            investments.filter { it.timestamp in startDate..endDate }
        } else investments

        val filteredShifts = if (startDate != null && endDate != null) {
            shifts.filter { it.clockInTime in startDate..endDate }
        } else shifts

        // 2. Calculate Revenue & Expenses (Hybrid)
        val txByDate = filteredTx.groupBy { it.date }
        val legacyByDate = filteredLegacy.associateBy { it.id } // YYYY-MM-DD
        val exByDate = filteredEx.groupBy { DateTimeUtils.getFormat("yyyy-MM-dd").format(Date(it.timestamp)) }
        val shiftsByDate = filteredShifts.groupBy { it.date }

        val allDates = (txByDate.keys + legacyByDate.keys + exByDate.keys + shiftsByDate.keys).distinct()
        
        var totalRevenue = 0.0
        var totalExpenses = 0.0
        val totalInvestments = legacyByDate.values.sumOf { it.totalInvestments } + filteredInv.sumOf { it.amount }

        val dailyMap = mutableMapOf<String, DailyBreakdown>()

        allDates.forEach { dateKey ->
            val dayTxs = txByDate[dateKey] ?: emptyList()
            val dayLegacy = legacyByDate[dateKey]
            val dayExpensesList = filteredEx.filter { 
                DateTimeUtils.getFormat("yyyy-MM-dd").format(Date(it.timestamp)) == dateKey && 
                (it.status == "COMPLETED" || it.status == "APPROVED")
            }
            // Find all closed or reconciled shifts for this day (Source of Truth for cash reconciliation)
            val shiftsForDay = filteredShifts.filter { 
                it.date == dateKey && (it.status == ShiftStatus.CLOSED || it.totalClosing > 0) 
            }
            
            val dayRevenue: Double
            val dayExpenses: Double
            val dayProfit: Double
            
            val salesRev = dayTxs.sumOf { it.totalAmount }
            val mealsRev = dayLegacy?.meals ?: 0.0
            
            if (shiftsForDay.isNotEmpty()) {
                // EXECUTIVE AUDIT LOGIC: 
                // Aggregate all closed shifts for the day.
                dayExpenses = dayExpensesList.sumOf { it.amount }
                
                // Sum profit across all shifts (Closing - Opening)
                dayProfit = shiftsForDay.sumOf { it.totalClosing - it.totalOpening }
                
                // Reconstruct Revenue: Profit + Expenses
                dayRevenue = dayProfit + dayExpenses
            } else if (dayTxs.isNotEmpty() || dayExpensesList.isNotEmpty()) {
                dayRevenue = salesRev + mealsRev
                dayExpenses = dayExpensesList.sumOf { it.amount }
                dayProfit = dayRevenue - dayExpenses
            } else if (dayLegacy != null) {
                dayRevenue = dayLegacy.grossRevenue
                dayExpenses = dayLegacy.detailedExpenses.filter { it.description != "Other Investment" }.sumOf { it.price }
                dayProfit = dayRevenue - dayExpenses
            } else {
                dayRevenue = 0.0
                dayExpenses = 0.0
                dayProfit = 0.0
            }

            totalRevenue += dayRevenue
            totalExpenses += dayExpenses

            val timestamp = dayTxs.maxOfOrNull { it.timestamp } ?: dayLegacy?.timestamp ?: 0L
            val formattedDate = dayLegacy?.formattedDate ?: dayTxs.firstOrNull()?.date ?: dateKey
            
            // Priority for ID: Use the first shift ID if available
            val id = shiftsForDay.firstOrNull()?.id ?: dayLegacy?.id ?: dayTxs.firstOrNull()?.shiftId ?: dateKey
            
            dailyMap[dateKey] = DailyBreakdown(
                id = id,
                date = formattedDate,
                revenue = dayRevenue,
                expenses = dayExpenses,
                profit = dayProfit,
                timestamp = timestamp
            )
        }

        val profit = totalRevenue - totalExpenses
        
        // 3. Merged Breakdowns
        val serviceMap = mutableMapOf<String, Double>()
        filteredTx.forEach { tx ->
            tx.items.forEach { item ->
                serviceMap[item.name] = (serviceMap[item.name] ?: 0.0) + item.totalAmount
            }
        }
        
        filteredLegacy.forEach { legacy ->
            legacy.serviceRevenue.forEach { (name, amount) ->
                serviceMap[name] = (serviceMap[name] ?: 0.0) + amount
            }
        }

        val expenseMap = filteredEx.groupBy { it.category }.mapValues { it.value.sumOf { e -> e.amount } }.toMutableMap()
        filteredLegacy.forEach { legacy ->
            legacy.detailedExpenses.forEach { item ->
                if (item.description != "Other Investment") {
                    expenseMap[item.description] = (expenseMap[item.description] ?: 0.0) + item.price
                }
            }
        }

        // Monthly breakdown
        val monthlyMap = mutableMapOf<Int, MonthlyBreakdown>()
        allDates.forEach { dateKey ->
            val dayBreakdown = dailyMap[dateKey] ?: return@forEach
            val cal = Calendar.getInstance().apply { timeInMillis = dayBreakdown.timestamp }
            if (dayBreakdown.timestamp == 0L) {
                // Parse dateKey if timestamp is missing
                try {
                    val date = DateTimeUtils.getFormat("yyyy-MM-dd").parse(dateKey)
                    if (date != null) cal.time = date
                } catch (e: Exception) {}
            }
            val m = cal.get(Calendar.MONTH)
            val name = DateTimeUtils.getFormat("MMMM").format(cal.time)
            val existing = monthlyMap[m] ?: MonthlyBreakdown(name, monthIndex = m)
            monthlyMap[m] = existing.copy(
                revenue = existing.revenue + dayBreakdown.revenue,
                expenses = existing.expenses + dayBreakdown.expenses,
                profit = existing.profit + dayBreakdown.profit
            )
        }

        val staffMap = mutableMapOf<String, StaffPerformance>()
        filteredTx.forEach { tx ->
            val p = staffMap[tx.staffId] ?: StaffPerformance(tx.staffId, tx.staffName)
            staffMap[tx.staffId] = p.copy(transactionCount = p.transactionCount + 1, totalRevenue = p.totalRevenue + tx.totalAmount)
        }
        filteredEx.forEach { ex ->
            val p = staffMap[ex.staffId] ?: StaffPerformance(ex.staffId, ex.staffName)
            staffMap[ex.staffId] = p.copy(totalExpenses = p.totalExpenses + ex.amount)
        }
        filteredShifts.forEach { shift ->
            val p = staffMap[shift.staffId] ?: StaffPerformance(shift.staffId, shift.staffName)
            staffMap[shift.staffId] = p.copy(shiftCount = p.shiftCount + 1)
        }

        val daysCount = dailyMap.size.coerceAtLeast(1)
        val summary = ReportSummary(
            revenue = totalRevenue,
            expenses = totalExpenses,
            profit = profit,
            transactionCount = filteredTx.size,
            investmentTotal = totalInvestments,
            openingBalance = filteredShifts.filter { it.status == ShiftStatus.CLOSED || it.totalClosing > 0 }.sumOf { it.totalOpening },
            closingBalance = filteredShifts.filter { it.status == ShiftStatus.CLOSED || it.totalClosing > 0 }.sumOf { it.totalClosing },
            cashDifference = filteredShifts.sumOf { it.openingBalanceDifference },
            averageDailyRevenue = totalRevenue / daysCount,
            averageDailyExpenses = totalExpenses / daysCount,
            averageDailyProfit = profit / daysCount
        )

        return BusinessReport(
            summary = summary,
            dailyBreakdown = dailyMap.values.sortedByDescending { it.timestamp },
            monthlyBreakdown = monthlyMap.values.sortedBy { it.monthIndex },
            serviceBreakdown = serviceMap,
            expensesByCategory = expenseMap,
            staffPerformance = staffMap.values.toList(),
            filteredTransactions = filteredTx,
            filteredExpenses = filteredEx,
            filteredInvestments = filteredInv,
            filteredShifts = filteredShifts,
            insights = generateInsights(filteredTx, filteredEx)
        )
    }

    private fun generateInsights(transactions: List<Transaction>, expenses: List<Expense>): List<String> {
        val insightsList = mutableListOf<String>()
        val revenue = transactions.sumOf { it.totalAmount }
        if (revenue > 5000) insightsList.add("Strong revenue today! Keep up the momentum.")
        
        val topService = transactions.flatMap { it.items }.groupBy { it.name }.maxByOrNull { it.value.sumOf { t -> t.totalAmount } }?.key
        if (topService != null) insightsList.add("$topService is performing exceptionally well.")
        
        return insightsList
    }
}
