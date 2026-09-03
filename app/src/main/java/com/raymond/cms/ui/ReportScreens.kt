package com.raymond.cms.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.ui.graphics.Brush
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.raymond.cms.ui.components.*
import com.raymond.cms.model.Transaction
import com.raymond.cms.model.Expense
import com.raymond.cms.model.Shift
import com.raymond.cms.util.DateTimeUtils
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsHubScreen(
    viewModel: TransactionViewModel,
    onNavigateToPeriod: (ReportPeriod, Long) -> Unit,
    onCustomRange: () -> Unit,
    onMenuClick: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Executive Intelligence", fontWeight = FontWeight.Black, letterSpacing = (-0.5).sp) },
                navigationIcon = {
                    IconButton(onClick = onMenuClick) { Icon(Icons.Default.Menu, contentDescription = null) }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        containerColor = Color(0xFF0F1318)
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            DashboardSectionHeader("Reporting Suite")
            
            ReportPeriodCard("DAILY AUDIT", "Single day performance & records", Icons.Default.Today, Color(0xFF673AB7)) {
                onNavigateToPeriod(ReportPeriod.DAILY, System.currentTimeMillis())
            }
            ReportPeriodCard("WEEKLY ANALYSIS", "Mon-Sun trend tracking", Icons.Default.DateRange, Color(0xFF1976D2)) {
                onNavigateToPeriod(ReportPeriod.WEEKLY, System.currentTimeMillis())
            }
            ReportPeriodCard("MONTHLY STATEMENT", "Comprehensive monthly averages", Icons.Default.CalendarMonth, Color(0xFF388E3C)) {
                onNavigateToPeriod(ReportPeriod.MONTHLY, System.currentTimeMillis())
            }
            ReportPeriodCard("ANNUAL REVIEW", "Strategic yearly growth summary", Icons.Default.History, Color(0xFFF57C00)) {
                onNavigateToPeriod(ReportPeriod.YEARLY, System.currentTimeMillis())
            }
            ReportPeriodCard("CUSTOM AD-HOC", "User-defined date range audit", Icons.Default.Tune, Color(0xFFE91E63)) {
                onCustomRange()
            }
        }
    }
}

@Composable
fun ReportPeriodCard(title: String, subtitle: String, icon: ImageVector, color: Color, onClick: () -> Unit) {
    ExecutiveCard(
        onClick = onClick,
        border = BorderStroke(1.dp, color.copy(alpha = 0.1f)),
        containerColor = color.copy(alpha = 0.05f)
    ) {
        Row(modifier = Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(
                modifier = Modifier.size(48.dp), 
                shape = RoundedCornerShape(12.dp), 
                color = color.copy(alpha = 0.1f)
            ) {
                Box(contentAlignment = Alignment.Center) { 
                    Icon(icon, null, tint = color, modifier = Modifier.size(24.dp)) 
                }
            }
            Spacer(Modifier.width(20.dp))
            Column {
                Text(
                    text = title, 
                    style = MaterialTheme.typography.titleSmall, 
                    fontWeight = FontWeight.Black, 
                    color = color,
                    letterSpacing = 1.sp
                )
                Text(
                    text = subtitle, 
                    style = MaterialTheme.typography.bodySmall, 
                    color = Color.White.copy(alpha = 0.4f)
                )
            }
            Spacer(Modifier.weight(1f))
            Icon(Icons.Default.ChevronRight, null, tint = color.copy(alpha = 0.3f))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PeriodReportScreen(
    title: String,
    viewModel: TransactionViewModel,
    period: ReportPeriod,
    initialTimestamp: Long = System.currentTimeMillis(),
    onBack: () -> Unit,
    onDrillDown: (Long) -> Unit = {},
    onTransactionClick: (String) -> Unit = {},
    onExpenseClick: (String) -> Unit = {},
    onEditClick: (String) -> Unit = {}
) {
    val reportState by viewModel.reportState.collectAsState()
    val selectedDate by viewModel.selectedDate.collectAsState()
    var showDatePicker by remember { mutableStateOf(false) }

    LaunchedEffect(period, initialTimestamp) { 
        viewModel.setPeriod(period) 
        viewModel.setSelectedDate(initialTimestamp)
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = selectedDate)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.setSelectedDate(datePickerState.selectedDateMillis ?: selectedDate)
                    showDatePicker = false
                }) { Text("OK") }
            }
        ) { DatePicker(state = datePickerState) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Column {
                        Text(title.uppercase(Locale.getDefault()), fontWeight = FontWeight.Black, letterSpacing = (-0.5).sp, fontSize = 16.sp)
                        Text(
                            text = DateTimeUtils.getFormat("EEEE, dd MMM yyyy").format(Date(selectedDate)),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )
                    }
                },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) } },
                actions = {
                    val context = LocalContext.current
                    if (period == ReportPeriod.DAILY) {
                        IconButton(onClick = {
                            val isoDate = DateTimeUtils.getFormat("yyyy-MM-dd").format(Date(selectedDate))
                            val shiftId = reportState.filteredShifts.firstOrNull()?.id ?: "LEGACY_$isoDate"
                            onEditClick(shiftId)
                        }) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit Records", tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                    IconButton(onClick = { 
                        ReportGenerator.generateDetailedPDF(
                            context, title, 
                            reportState.filteredTransactions, 
                            reportState.filteredExpenses, 
                            reportState.filteredInvestments
                        )
                    }) { Icon(Icons.Default.PictureAsPdf, null, tint = Color.White.copy(alpha = 0.6f)) }
                    IconButton(onClick = { viewModel.exportToCsv(context) }) { Icon(Icons.Default.Share, null, tint = Color.White.copy(alpha = 0.6f)) }
                    IconButton(onClick = { showDatePicker = true }) { Icon(Icons.Default.CalendarToday, null, tint = MaterialTheme.colorScheme.primary) }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        containerColor = Color(0xFF0F1318)
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            DashboardSectionHeader("Executive Summary")
            SummaryCards(reportState.summary, showAverages = period != ReportPeriod.DAILY)
            
            ReportChartSection(reportState, period)

            if (period == ReportPeriod.DAILY) {
                DailyDetailsSection(reportState, onTransactionClick, onExpenseClick)
            } else {
                val breakdownTitle = if (period == ReportPeriod.YEARLY) "Monthly Performance Breakdown" else "Daily Performance Breakdown"
                DashboardSectionHeader(breakdownTitle)
                
                val breakdownData = when {
                    period == ReportPeriod.YEARLY -> reportState.monthlyBreakdown.map { it.monthName to it.revenue to it.expenses to it.profit to 0L }
                    else -> reportState.dailyBreakdown.map { it.date to it.revenue to it.expenses to it.profit to it.timestamp }
                }
                BreakdownSection(breakdownData, onDrillDown)
            }
        }
    }
}

@Composable
fun ReportChartSection(report: com.raymond.cms.domain.BusinessReport, period: ReportPeriod) {
    val data = when (period) {
        ReportPeriod.YEARLY -> report.monthlyBreakdown.map { it.monthName.take(3) to it.revenue }
        else -> report.dailyBreakdown.map { it.date.substringAfterLast("-") to it.revenue }
    }.take(7).reversed()

    if (data.isEmpty()) return

    Column {
        DashboardSectionHeader("Growth Visualization")
        ExecutiveCard(
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f)),
            containerColor = Color.White.copy(alpha = 0.02f)
        ) {
            Column(Modifier.padding(20.dp)) {
                val max = data.maxOf { it.second }.coerceAtLeast(1.0)
                Row(
                    modifier = Modifier.fillMaxWidth().height(160.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.Bottom
                ) {
                    data.forEach { (label, value) ->
                        val h by animateFloatAsState((value / max).toFloat().coerceIn(0.05f, 1f), tween(1000), label = "")
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(
                                Modifier
                                    .width(16.dp)
                                    .fillMaxHeight(h)
                                    .background(
                                        brush = Brush.verticalGradient(
                                            colors = listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
                                        ),
                                        shape = RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp)
                                    )
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(label, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.White.copy(alpha = 0.4f))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SummaryCards(summary: com.raymond.cms.domain.ReportSummary, showAverages: Boolean = false) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            StatCard("Total Revenue", summary.revenue, Icons.Default.Payments, Color(0xFF2196F3), Modifier.weight(1f))
            StatCard("Operational Costs", summary.expenses, Icons.Default.RemoveCircleOutline, Color(0xFFE57373), Modifier.weight(1f))
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            StatCard("Capital Inflow", summary.investmentTotal, Icons.Default.Savings, Color(0xFFFF9800), Modifier.weight(1f))
            StatCard("Net Performance", summary.profit, Icons.Default.TrendingUp, Color(0xFF4CAF50), Modifier.weight(1f))
        }
        
        if (showAverages) {
            DashboardSectionHeader("Performance Benchmarks")
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                StatCard("Avg Daily Rev", summary.averageDailyRevenue, Icons.Default.ShowChart, Color(0xFF03A9F4), Modifier.weight(1f))
                StatCard("Avg Daily Profit", summary.averageDailyProfit, Icons.Default.AutoGraph, Color(0xFFFFC107), Modifier.weight(1f))
            }
        }
    }
}



@Composable
fun DailyDetailsSection(
    report: com.raymond.cms.domain.BusinessReport,
    onTransactionClick: (String) -> Unit,
    onExpenseClick: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            DashboardSectionHeader("Transaction Ledger")
            if (report.filteredTransactions.isEmpty()) {
                Text("No compliant transactions recorded.", color = Color.White.copy(alpha = 0.2f), style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(start = 8.dp))
            } else {
                report.filteredTransactions.forEach { tx ->
                    IndividualTransactionItem(tx, onClick = { onTransactionClick(tx.id) })
                }
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            DashboardSectionHeader("Operational Expenses")
            if (report.filteredExpenses.isEmpty()) {
                Text("No operational costs recorded.", color = Color.White.copy(alpha = 0.2f), style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(start = 8.dp))
            } else {
                report.filteredExpenses.forEach { ex ->
                    ExpenseListItem(ex, onClick = { onExpenseClick(ex.id) })
                }
            }
        }
        
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            DashboardSectionHeader("Personnel Shift Balances")
            if (report.filteredShifts.isEmpty()) {
                Text("No active personnel sessions found.", color = Color.White.copy(alpha = 0.2f), style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(start = 8.dp))
            } else {
                report.filteredShifts.forEach { shift ->
                    ShiftBalanceCard(shift)
                }
            }
        }
    }
}



@Composable
fun ExpenseListItem(ex: Expense, onClick: () -> Unit) {
    ExecutiveCard(
        onClick = onClick,
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = ex.category, 
                    fontWeight = FontWeight.Bold, 
                    color = Color(0xFFE57373),
                    style = MaterialTheme.typography.titleSmall
                )
                Text(
                    text = ex.description, 
                    style = MaterialTheme.typography.bodySmall, 
                    color = Color.White.copy(alpha = 0.7f),
                    maxLines = 1
                )
                Text(
                    text = "${DateTimeUtils.getFormat("hh:mm a").format(Date(ex.timestamp))} • ${ex.staffName}", 
                    style = MaterialTheme.typography.labelSmall, 
                    color = Color.White.copy(alpha = 0.3f),
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "KSH ${String.format(Locale.US, "%,.0f", ex.amount)}", 
                    fontWeight = FontWeight.Black, 
                    color = Color(0xFFE57373)
                )
                Surface(
                    color = if (ex.status == "APPROVED" || ex.status == "COMPLETED") Color(0xFF4CAF50).copy(alpha = 0.1f) else Color.White.copy(alpha = 0.05f),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    Text(
                        text = ex.status, 
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelSmall, 
                        fontWeight = FontWeight.Black,
                        color = if (ex.status == "APPROVED" || ex.status == "COMPLETED") Color(0xFF4CAF50) else Color.White.copy(alpha = 0.4f)
                    )
                }
            }
        }
    }
}

@Composable
fun ShiftBalanceCard(shift: Shift) {
    ExecutiveCard(
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Person, null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(8.dp))
                    Text(text = shift.staffName, fontWeight = FontWeight.Bold, color = Color.White)
                }
                if (shift.status == com.raymond.cms.model.ShiftStatus.ACTIVE) {
                    Surface(
                        color = Color(0xFF2196F3).copy(alpha = 0.1f),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, Color(0xFF2196F3).copy(alpha = 0.2f))
                    ) {
                        Text(
                            text = "LIVE SESSION",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF2196F3),
                            fontWeight = FontWeight.Black
                        )
                    }
                }
            }
            
            HorizontalDivider(color = Color.White.copy(alpha = 0.05f))

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                DetailRowItem("Opening Balance", "KSH ${String.format(Locale.US, "%,.0f", shift.totalOpening)}")
                DetailRowItem("Final Reconciliation", "KSH ${String.format(Locale.US, "%,.0f", shift.totalClosing)}")
                
                val variance = shift.openingBalanceDifference
                if (variance != 0.0) {
                    DetailRowItem(
                        label = "Audit Discrepancy", 
                        value = "KSH ${String.format(Locale.US, "%,.0f", variance)}", 
                        color = if (variance >= 0) Color(0xFF4CAF50) else Color(0xFFE57373)
                    )
                }
                
                if (shift.status == com.raymond.cms.model.ShiftStatus.CLOSED) {
                    val shiftNet = shift.totalClosing - shift.totalOpening
                    DetailRowItem(
                        label = "Shift Net Change", 
                        value = "KSH ${String.format(Locale.US, "%,.0f", shiftNet)}", 
                        color = if (shiftNet >= 0) Color(0xFF4CAF50) else Color(0xFFE57373)
                    )
                }
            }
        }
    }
}

@Composable
fun BalanceRowItem(label: String, value: Double, isBold: Boolean = false, color: Color = Color.Unspecified) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodySmall)
        Text("KSh ${String.format(Locale.US, "%.0f", value)}", fontWeight = if (isBold) FontWeight.Bold else FontWeight.Normal, color = color)
    }
}

@Composable
fun BreakdownSection(data: List<Pair<Pair<Pair<Pair<String, Double>, Double>, Double>, Long>>, onDrillDown: (Long) -> Unit) {
    ExecutiveCard(
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
    ) {
        Column {
            data.forEach { item ->
                val name = item.first.first.first.first
                val rev = item.first.first.first.second
                val ex = item.first.first.second
                val prof = item.first.second
                val ts = item.second
                
                Row(
                    modifier = Modifier.fillMaxWidth().clickable { onDrillDown(ts) }.padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1.3f)) {
                        Text(name, fontWeight = FontWeight.Bold, color = Color.White, style = MaterialTheme.typography.bodyMedium)
                        if (ts != 0L) {
                            Text("Click to Audit", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f))
                        }
                    }
                    
                    Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                        Text("KSH ${String.format(Locale.US, "%,.0f", rev)}", style = MaterialTheme.typography.bodySmall, color = Color.White)
                        Text("REVENUE", style = MaterialTheme.typography.labelSmall, fontSize = 8.sp, color = Color.White.copy(alpha = 0.3f))
                    }
                    
                    Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                        Text("KSH ${String.format(Locale.US, "%,.0f", ex)}", style = MaterialTheme.typography.bodySmall, color = Color(0xFFE57373))
                        Text("EXPENSE", style = MaterialTheme.typography.labelSmall, fontSize = 8.sp, color = Color.White.copy(alpha = 0.3f))
                    }

                    Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                        Text("KSH ${String.format(Locale.US, "%,.0f", prof)}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Black, color = if (prof >= 0) Color(0xFF4CAF50) else Color(0xFFE57373))
                        Text("NET PROFIT", style = MaterialTheme.typography.labelSmall, fontSize = 8.sp, color = Color.White.copy(alpha = 0.3f))
                    }
                }
                HorizontalDivider(color = Color.White.copy(alpha = 0.05f))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomRangeReportScreen(
    viewModel: TransactionViewModel,
    onBack: () -> Unit,
    onDrillDown: (Long) -> Unit
) {
    val reportState by viewModel.reportState.collectAsState()
    val dateRange by viewModel.dateRange.collectAsState()
    var showRangePicker by remember { mutableStateOf(false) }

    if (showRangePicker) {
        val rangePickerState = rememberDateRangePickerState()
        DatePickerDialog(
            onDismissRequest = { showRangePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    val start = rangePickerState.selectedStartDateMillis
                    val end = rangePickerState.selectedEndDateMillis
                    if (start != null && end != null) {
                        viewModel.setCustomRange(start, end)
                    }
                    showRangePicker = false
                }) { Text("OK") }
            }
        ) { DateRangePicker(state = rangePickerState, modifier = Modifier.height(500.dp)) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Column {
                        Text("AD-HOC CUSTOM AUDIT", fontWeight = FontWeight.Black, letterSpacing = (-0.5).sp, fontSize = 16.sp)
                        dateRange?.let { (start, end) ->
                            Text(
                                text = "${DateTimeUtils.getFormat("dd MMM").format(Date(start))} - ${DateTimeUtils.getFormat("dd MMM yyyy").format(Date(end))}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            )
                        }
                    }
                },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) } },
                actions = {
                    val context = LocalContext.current
                    IconButton(onClick = { 
                        ReportGenerator.generateDetailedPDF(
                            context, "Custom Range Report", 
                            reportState.filteredTransactions, 
                            reportState.filteredExpenses, 
                            reportState.filteredInvestments
                        )
                    }) { Icon(Icons.Default.PictureAsPdf, null, tint = Color.White.copy(alpha = 0.6f)) }
                    IconButton(onClick = { viewModel.exportToCsv(context) }) { Icon(Icons.Default.Share, null, tint = Color.White.copy(alpha = 0.6f)) }
                    IconButton(onClick = { showRangePicker = true }) { Icon(Icons.Default.Tune, null, tint = MaterialTheme.colorScheme.primary) }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        containerColor = Color(0xFF0F1318)
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            DashboardSectionHeader("Executive Summary")
            SummaryCards(reportState.summary, showAverages = true)
            
            ReportChartSection(reportState, ReportPeriod.CUSTOM)

            DashboardSectionHeader("Custom Period Breakdown")
            BreakdownSection(reportState.dailyBreakdown.map { 
                it.date to it.revenue to it.expenses to it.profit to it.timestamp
            }, onDrillDown)
        }
    }
}
