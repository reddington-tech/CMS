package com.raymond.cms.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.raymond.cms.model.*
import com.raymond.cms.ui.components.*
import com.raymond.cms.domain.BusinessReport
import com.raymond.cms.util.DateTimeUtils
import kotlinx.coroutines.flow.collectLatest
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: TransactionViewModel,
    authViewModel: AuthViewModel,
    isReadOnly: Boolean = false,
    onClockInClick: () -> Unit = {},
    onClockOutClick: () -> Unit = {},
    onAddTransaction: () -> Unit = {},
    onNewSale: () -> Unit = {},
    onAddExpense: () -> Unit = {},
    onAddInvestment: () -> Unit = {},
    onMenuClick: () -> Unit,
    onShiftClick: (String) -> Unit = {},
    onEditShiftClick: (String) -> Unit = {},
    onDiscrepancyClick: () -> Unit = {}
) {
    val transactionsList by viewModel.filteredTransactions.collectAsState()
    val shiftTransactions by authViewModel.shiftTransactions.collectAsState()
    val reportState by viewModel.reportState.collectAsState()
    val allShifts by viewModel.allShifts.collectAsState()
    val user by authViewModel.currentUser.collectAsState()
    val attendance by authViewModel.attendance.collectAsState()
    val activeShift by authViewModel.activeShift.collectAsState()
    val insights by viewModel.insights.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(user) {
        if (user?.role == UserRole.STAFF) {
            viewModel.setPeriod(ReportPeriod.WEEKLY)
        } else if (user?.role == UserRole.ADMIN) {
            viewModel.setPeriod(ReportPeriod.MONTHLY)
        }
    }

    LaunchedEffect(Unit) {
        viewModel.uiEvent.collectLatest {
            snackbarHostState.showSnackbar(it)
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("CMS Executive", fontWeight = FontWeight.ExtraBold) },
                navigationIcon = {
                    IconButton(onClick = onMenuClick) { Icon(Icons.Default.Menu, contentDescription = null) }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = Color(0xFF0F1318)
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            if (isLoading) {
                Box(modifier = Modifier.fillMaxWidth().height(100.dp).shimmerEffect().clip(RoundedCornerShape(24.dp)))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Box(modifier = Modifier.weight(1f).height(120.dp).shimmerEffect().clip(RoundedCornerShape(20.dp)))
                    Box(modifier = Modifier.weight(1f).height(120.dp).shimmerEffect().clip(RoundedCornerShape(20.dp)))
                }
            } else {
                Column {
                    val calendar = Calendar.getInstance()
                    val greeting = when (calendar.get(Calendar.HOUR_OF_DAY)) {
                        in 0..11 -> "Good Morning,"
                        in 12..16 -> "Good Afternoon,"
                        else -> "Good Evening,"
                    }
                    Text(
                        text = greeting,
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.White.copy(alpha = 0.6f)
                    )
                    Text(
                        text = user?.name?.ifEmpty { user?.email?.substringBefore("@") } ?: "Executive",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Black,
                        color = Color.White,
                        letterSpacing = (-0.5).sp
                    )
                }

                if (user?.role == UserRole.ADMIN) {
                    val todayDate = DateTimeUtils.getFormat("yyyy-MM-dd").format(Date())
                    val todayTransactions = transactionsList.filter { it.date == todayDate }
                    if (todayTransactions.isNotEmpty()) {
                        LiveActivityPulse(todayTransactions.take(3))
                    }
                }

                if (user?.role == UserRole.STAFF) {
                    ExecutiveCard(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
                    ) {
                        Row(modifier = Modifier.padding(20.dp), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Shift Operations", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                                Text(
                                    text = if (activeShift != null) "Active Duty in Progress" else "System Ready for Shift",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                            Button(
                                onClick = { 
                                    if (activeShift == null) {
                                        if (attendance?.clockIn == null) authViewModel.clockIn()
                                        onClockInClick()
                                    } else {
                                        onClockOutClick()
                                    }
                                }, 
                                enabled = attendance?.clockOut == null, 
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (activeShift == null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                                )
                            ) {
                                Text(if (activeShift == null) "START SHIFT" else "END SHIFT", fontWeight = FontWeight.Black, fontSize = 12.sp)
                            }
                        }
                    }
                }
                
                DashboardSectionHeader(if (user?.role == UserRole.ADMIN) "Business Performance (Month)" else "My Performance (Week)")
                
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    val periodLabel = if (user?.role == UserRole.ADMIN) " (Month)" else " (Week)"
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        StatCard("Revenue$periodLabel", reportState.summary.revenue, Icons.AutoMirrored.Filled.TrendingUp, Color(0xFF2196F3), Modifier.weight(1f))
                        StatCard("Net Profit$periodLabel", reportState.summary.profit, Icons.Default.AccountBalanceWallet, Color(0xFF4CAF50), Modifier.weight(1f))
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        StatCard("Expenses$periodLabel", reportState.summary.expenses, Icons.AutoMirrored.Filled.TrendingDown, Color(0xFFF44336), Modifier.weight(1f))
                        StatCard("Investments$periodLabel", reportState.summary.investmentTotal, Icons.Default.Savings, Color(0xFFFF9800), Modifier.weight(1f))
                    }
                }

                if (user?.role == UserRole.ADMIN) {
                    val discrepancies = reportState.filteredShifts.count { 
                        (it.openingBalanceDifference != 0.0 || it.flaggedForReview) && it.varianceStatus == "PENDING"
                    }
                    if (discrepancies > 0) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            onClick = onDiscrepancyClick,
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFB71C1C).copy(alpha = 0.1f))
                        ) {
                            Row(modifier = Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Warning, null, tint = Color(0xFFE57373))
                                Spacer(Modifier.width(16.dp))
                                Column {
                                    Text("Balance Discrepancies Found", fontWeight = FontWeight.Bold, color = Color.White)
                                    Text("$discrepancies shifts have opening balance mismatches.", style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.7f))
                                }
                            }
                        }
                    }
                }

                if (insights.isNotEmpty()) {
                    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Lightbulb, contentDescription = null, tint = MaterialTheme.colorScheme.onTertiaryContainer)
                                Spacer(Modifier.width(8.dp))
                                Text("Insights", fontWeight = FontWeight.Bold)
                            }
                            insights.forEach { insight -> Text(insight, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 8.dp)) }
                        }
                    }
                }

                // Professional Quick-Action Section for Sale/Records
                val isAdmin = user?.role == UserRole.ADMIN
                if (isAdmin || (!isReadOnly && activeShift != null)) {
                    DashboardSectionHeader("Quick Operations")
                    
                    ExecutiveCard(
                        onClick = onNewSale,
                        containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
                    ) {
                        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.size(40.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.AddShoppingCart, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                                }
                            }
                            Spacer(Modifier.width(16.dp))
                            Column {
                                Text("New Customer Sale", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = Color.White)
                                Text("Record transaction for current customer", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.5f))
                            }
                            Spacer(Modifier.weight(1f))
                            Icon(Icons.Default.ChevronRight, null, tint = Color.White.copy(alpha = 0.3f))
                        }
                    }

                    if (isAdmin) {
                        ExecutiveCard(onClick = onAddTransaction) {
                            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.PostAdd, null, tint = Color.White.copy(alpha = 0.7f), modifier = Modifier.size(20.dp))
                                Spacer(Modifier.width(16.dp))
                                Text("Consolidated Daily Records", style = MaterialTheme.typography.bodyMedium, color = Color.White)
                                Spacer(Modifier.weight(1f))
                                Icon(Icons.Default.ChevronRight, null, tint = Color.White.copy(alpha = 0.3f))
                            }
                        }
                    }

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        ExecutiveCard(
                            modifier = Modifier.weight(1f),
                            onClick = onAddExpense,
                            containerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.05f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.1f))
                        ) {
                            Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.RemoveCircleOutline, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("EXPENSE", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Black, color = Color.White.copy(alpha = 0.8f))
                            }
                        }
                        if (isAdmin) {
                            ExecutiveCard(
                                modifier = Modifier.weight(1f),
                                onClick = onAddInvestment,
                                containerColor = Color(0xFF4CAF50).copy(alpha = 0.05f),
                                border = BorderStroke(1.dp, Color(0xFF4CAF50).copy(alpha = 0.1f))
                            ) {
                                Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.AccountBalance, null, tint = Color(0xFF4CAF50), modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text("TRANSFER", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Black, color = Color.White.copy(alpha = 0.8f))
                                }
                            }
                        }
                    }
                }


                if (user?.role == UserRole.ADMIN) {
                    DashboardSectionHeader("Audit Trail: Recent Summaries")
                    
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        val todayDate = DateTimeUtils.getFormat("yyyy-MM-dd", Locale.US).format(Date())
                        allShifts.take(10).forEach { shift ->
                            val summary = reportState.dailyBreakdown.find { it.date == shift.date || it.id == shift.id }
                            val isTrulyActive = shift.status == ShiftStatus.ACTIVE && shift.date == todayDate
                            val amount = if (isTrulyActive) {
                                summary?.revenue ?: 0.0
                            } else {
                                summary?.revenue ?: (shift.totalClosing - shift.totalOpening)
                            }
                            ShiftSummaryItem(
                                name = shift.staffName,
                                date = shift.date,
                                amount = amount,
                                timestamp = shift.clockInTime,
                                onClick = { onShiftClick(shift.id) },
                                onEditClick = { onEditShiftClick(shift.id) }
                            )
                        }
                    }
                } else if (activeShift != null) {
                    Text("My Active Shift Status", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Color.White)
                    
                    ExecutiveCard {
                        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                                Column {
                                    Text("Personal Collection", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                                    val durationMillis = System.currentTimeMillis() - activeShift!!.clockInTime
                                    val hours = durationMillis / (1000 * 60 * 60)
                                    val minutes = (durationMillis / (1000 * 60)) % 60
                                    Text("On Duty: ${hours}h ${minutes}m", style = MaterialTheme.typography.bodySmall, color = Color(0xFF4CAF50), fontWeight = FontWeight.Bold)
                                }
                                Surface(color = Color(0xFF4CAF50).copy(alpha = 0.1f), shape = RoundedCornerShape(8.dp)) {
                                    Text(
                                        text = "${shiftTransactions.size} CUSTOMERS", 
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        style = MaterialTheme.typography.labelSmall, 
                                        color = Color(0xFF4CAF50),
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                }
                            }
                            
                            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                                SummaryField("My Cash", activeShift!!.openingCash + shiftTransactions.filter { it.paymentMethod == PaymentMethod.CASH }.sumOf { it.totalAmount })
                                SummaryField("My M-Pesa", activeShift!!.openingMpesa + shiftTransactions.filter { it.paymentMethod == PaymentMethod.MPESA }.sumOf { it.totalAmount })
                            }
                            
                            HorizontalDivider(color = Color.Gray.copy(alpha = 0.1f))
                            
                            Text("My Recent Actions", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                            if (shiftTransactions.isEmpty()) {
                                Text("No transactions recorded yet.", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                            } else {
                                shiftTransactions.take(3).forEach { tx ->
                                    Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                                        val desc = if (tx.items.isNotEmpty()) tx.items.first().name else "Sale"
                                        Text(desc, style = MaterialTheme.typography.bodySmall, color = Color.White)
                                        Text("KSH ${tx.totalAmount.toInt()}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = Color.White)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun LiveActivityPulse(latestTransactions: List<com.raymond.cms.model.Transaction>) {
    ExecutiveCard(
        containerColor = Color(0xFF1E252D).copy(alpha = 0.5f),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(6.dp).background(Color(0xFF4CAF50), RoundedCornerShape(3.dp)))
                Spacer(Modifier.width(8.dp))
                Text("LIVE ACTIVITY", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Black, color = Color(0xFF4CAF50), letterSpacing = 1.sp)
            }
            Spacer(Modifier.height(12.dp))
            latestTransactions.forEach { tx ->
                Row(Modifier.padding(vertical = 4.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    val desc = if (tx.items.isNotEmpty()) tx.items.first().name else "Sale"
                    Text("${tx.staffName}: $desc", style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.7f))
                    Text("KSH ${tx.totalAmount.toInt()}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }
    }
}

@Composable
fun StatCard(label: String, value: Double, icon: ImageVector, color: Color, modifier: Modifier = Modifier) {
    ExecutiveCard(
        modifier = modifier,
        containerColor = color.copy(alpha = 0.05f),
        border = BorderStroke(1.dp, color.copy(alpha = 0.15f))
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Surface(
                modifier = Modifier.size(36.dp),
                shape = RoundedCornerShape(10.dp),
                color = color.copy(alpha = 0.1f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp), tint = color)
                }
            }
            SummaryField(label = label, value = value, isBold = true, color = Color.White)
        }
    }
}

@Composable
fun ShiftSummaryItem(
    name: String,
    date: String,
    amount: Double,
    timestamp: Long,
    onClick: () -> Unit,
    onEditClick: () -> Unit
) {
    ExecutiveCard(onClick = onClick) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.Top) {
                Column {
                    Text(
                        text = name.ifEmpty { "Shift Summary" },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = DateTimeUtils.getFormat("dd MMM, HH:mm").format(Date(timestamp)),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.6f)
                    )
                }
                Text(
                    text = "+ KSH ${String.format(Locale.US, "%,.0f", amount)}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF4CAF50)
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFF2C353E),
                    border = CardDefaults.outlinedCardBorder().copy(width = 0.5.dp)
                ) {
                    Text(
                        text = "Shift Total ($date)",
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Medium,
                        color = Color.White.copy(alpha = 0.7f)
                    )
                }
                
                OutlinedButton(
                    onClick = onEditClick,
                    modifier = Modifier.height(36.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary),
                    border = CardDefaults.outlinedCardBorder().copy(width = 0.5.dp)
                ) {
                    Icon(Icons.Default.Edit, null, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("EDIT", fontSize = 11.sp, fontWeight = FontWeight.ExtraBold)
                }

                Button(
                    onClick = onClick,
                    modifier = Modifier.height(36.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("SUMMARY", fontSize = 11.sp, fontWeight = FontWeight.ExtraBold)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionListScreen(
    viewModel: TransactionViewModel,
    authViewModel: AuthViewModel,
    isReadOnly: Boolean = false,
    onAddClick: () -> Unit,
    onMenuClick: () -> Unit,
    onShiftClick: (String) -> Unit,
    onEditShiftClick: (String) -> Unit,
    onItemClick: (String) -> Unit
) {
    val allDailyBreakdowns by viewModel.allDailyBreakdowns.collectAsState()
    val legacyTransactions by viewModel.legacyTransactions.collectAsState()
    val allShifts by viewModel.allShifts.collectAsState()
    val transactions by viewModel.filteredTransactions.collectAsState()
    val reportState by viewModel.reportState.collectAsState()
    val user by authViewModel.currentUser.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    var selectedTab by remember { mutableIntStateOf(0) } // 0 = Shifts, 1 = Individual Transactions

    val consolidatedDailyRecords = remember(allShifts, allDailyBreakdowns, searchQuery, user, reportState) {
        val todayDate = DateTimeUtils.getFormat("yyyy-MM-dd", Locale.US).format(Date())
        val combined = allShifts.map { shift ->
            val summary = reportState.dailyBreakdown.find { it.date == shift.date || it.id == shift.id }
            val isTrulyActive = shift.status == ShiftStatus.ACTIVE && shift.date == todayDate
            val amount = if (isTrulyActive) {
                summary?.profit ?: 0.0
            } else {
                summary?.profit ?: (shift.totalClosing - shift.totalOpening)
            }
            ShiftData(shift.date, shift.staffName, amount, shift.id, shift.clockInTime) 
        } + allDailyBreakdowns.map { 
            val cal = Calendar.getInstance().apply { timeInMillis = it.timestamp }
            val isoDate = String.format(Locale.US, "%04d-%02d-%02d", cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1, cal.get(Calendar.DAY_OF_MONTH))
            ShiftData(isoDate, "Daily Summary", it.profit, "LEGACY_${it.id}", it.timestamp) 
        }
        
        val now = System.currentTimeMillis()
        val sevenDaysMillis = 7 * 24 * 60 * 60 * 1000L
        
        val roleFiltered = if (user?.role == UserRole.STAFF) {
            combined.filter { now - it.timestamp <= sevenDaysMillis }
        } else combined

        val filtered = if (searchQuery.isEmpty()) roleFiltered else roleFiltered.filter { 
            it.staffName.contains(searchQuery, ignoreCase = true) || it.date.contains(searchQuery)
        }

        // Group by date to ensure only one record per day for the summaries list
        filtered.groupBy { it.date }
            .map { (date, items) ->
                val realShifts = items.filter { !it.id.startsWith("LEGACY_") }
                val legacySummaries = items.filter { it.id.startsWith("LEGACY_") }
                val hasRealShifts = realShifts.isNotEmpty()
                val displayItems = if (hasRealShifts) realShifts else legacySummaries
                ShiftData(
                    date = date,
                    staffName = if (displayItems.size > 1) "${displayItems.size} Shifts" 
                                else if (!hasRealShifts) "Executive Summary"
                                else displayItems.first().staffName,
                    amount = displayItems.sumOf { it.amount },
                    id = displayItems.first().id,
                    timestamp = displayItems.maxOf { it.timestamp }
                )
            }.sortedByDescending { it.timestamp }
    }

    val displayTransactions = remember(transactions, user) {
        if (user?.role == UserRole.STAFF) {
            val now = System.currentTimeMillis()
            val sevenDaysMillis = 7 * 24 * 60 * 60 * 1000L
            transactions.filter { now - it.timestamp <= sevenDaysMillis }
        } else transactions
    }

    LaunchedEffect(Unit) { viewModel.uiEvent.collectLatest { snackbarHostState.showSnackbar(it) } }

    Scaffold(
        topBar = {
            Column(modifier = Modifier.background(Color(0xFF0F1318))) {
                TopAppBar(
                    title = { Text("Executive Business Ledger", fontWeight = FontWeight.Black, letterSpacing = (-0.5).sp) },
                    navigationIcon = { IconButton(onClick = onMenuClick) { Icon(Icons.Default.Menu, contentDescription = null) } },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
                )
                
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = Color.Transparent,
                    contentColor = MaterialTheme.colorScheme.primary,
                    indicator = { tabPositions ->
                        TabRowDefaults.SecondaryIndicator(
                            Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                            color = MaterialTheme.colorScheme.primary,
                            height = 3.dp
                        )
                    },
                    divider = {}
                ) {
                    Tab(
                        selected = selectedTab == 0, 
                        onClick = { selectedTab = 0 }, 
                        text = { Text("SHIFT SUMMARIES", style = MaterialTheme.typography.labelMedium, fontWeight = if (selectedTab == 0) FontWeight.Black else FontWeight.Medium) }
                    )
                    Tab(
                        selected = selectedTab == 1, 
                        onClick = { selectedTab = 1 }, 
                        text = { Text("TRANSACTION LOG", style = MaterialTheme.typography.labelMedium, fontWeight = if (selectedTab == 1) FontWeight.Black else FontWeight.Medium) }
                    )
                }

                Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                    StandardTextField(
                        value = searchQuery,
                        onValueChange = { viewModel.onSearchQueryChange(it) },
                        label = if (selectedTab == 0) "Search by staff or date..." else "Search ID, service, personnel...",
                        leadingIcon = { Icon(Icons.Default.Search, null, tint = Color.White.copy(alpha = 0.3f)) }
                    )
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = Color(0xFF0F1318)
    ) { padding ->
        if (isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                if (selectedTab == 0) {
                    items(consolidatedDailyRecords.size) { index ->
                        val item = consolidatedDailyRecords[index]
                        StaggeredEntrance(index = index) {
                            ShiftSummaryItem(
                                name = item.staffName,
                                date = item.date,
                                amount = item.amount,
                                timestamp = item.timestamp,
                                onClick = { onShiftClick(item.id) },
                                onEditClick = { onEditShiftClick(item.id) }
                            )
                        }
                    }
                } else {
                    items(displayTransactions.size) { index ->
                        StaggeredEntrance(index = index) {
                            IndividualTransactionItem(transaction = displayTransactions[index], onClick = { onItemClick(displayTransactions[index].id) })
                        }
                    }
                }
            }
        }
    }
}

data class ShiftData(
    val date: String,
    val staffName: String,
    val amount: Double,
    val id: String,
    val timestamp: Long
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IndividualTransactionItem(transaction: com.raymond.cms.model.Transaction, onClick: () -> Unit) {
    ExecutiveCard(
        onClick = onClick,
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                Column(Modifier.weight(1f)) {
                    val title = if (transaction.items.isNotEmpty()) {
                        if (transaction.items.size > 1) "${transaction.items.first().name} (+${transaction.items.size - 1})"
                        else transaction.items.first().name
                    } else "General Sale"
                    
                    Text(
                        text = title, 
                        style = MaterialTheme.typography.titleMedium, 
                        fontWeight = FontWeight.Bold, 
                        color = Color.White
                    )
                    Text(
                        text = "Ref: ${transaction.id.takeLast(8).uppercase()}",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.4f),
                        letterSpacing = 1.sp
                    )
                }
                
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "KSH ${String.format(Locale.US, "%,.0f", transaction.totalAmount)}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFF4CAF50)
                    )
                    Surface(
                        color = Color.White.copy(alpha = 0.05f),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.padding(top = 4.dp)
                    ) {
                        Text(
                            text = transaction.paymentMethod.name,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.White.copy(alpha = 0.6f)
                        )
                    }
                }
            }

            HorizontalDivider(color = Color.White.copy(alpha = 0.05f))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.size(24.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Person, null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = transaction.staffName,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        color = Color.White.copy(alpha = 0.7f)
                    )
                }
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.AccessTime, 
                        null, 
                        modifier = Modifier.size(14.dp), 
                        tint = Color.White.copy(alpha = 0.4f)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = DateTimeUtils.getFormat("hh:mm a").format(Date(transaction.timestamp)), 
                        style = MaterialTheme.typography.labelSmall, 
                        color = Color.White.copy(alpha = 0.4f)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTransactionScreen(
    viewModel: TransactionViewModel,
    authViewModel: AuthViewModel,
    onBack: () -> Unit
) {
    var selectedDate by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var showDatePicker by remember { mutableStateOf(false) }
    
    val transactions by viewModel.filteredTransactions.collectAsState()
    val user by authViewModel.currentUser.collectAsState()
    
    var openingCash by remember { mutableStateOf("") }
    var openingMpesa by remember { mutableStateOf("") }
    var openingTill by remember { mutableStateOf("") }
    
    var meals by remember { mutableStateOf("") }
    
    var closingCash by remember { mutableStateOf("") }
    var closingMpesa by remember { mutableStateOf("") }
    var closingTill by remember { mutableStateOf("") }
    
    val expenseItems = remember { mutableStateListOf(ExpenseItem()) }

    LaunchedEffect(selectedDate) {
        val existing = viewModel.getTransactionByDate(selectedDate)
        if (existing != null) {
            openingCash = if (existing.openingCash == 0.0) "" else existing.openingCash.toString()
            openingMpesa = if (existing.openingMpesa == 0.0) "" else existing.openingMpesa.toString()
            openingTill = if (existing.openingTill == 0.0) "" else existing.openingTill.toString()
            closingCash = if (existing.closingCash == 0.0) "" else existing.closingCash.toString()
            closingMpesa = if (existing.closingMpesa == 0.0) "" else existing.closingMpesa.toString()
            closingTill = if (existing.closingTill == 0.0) "" else existing.closingTill.toString()
            meals = if (existing.meals == 0.0) "" else existing.meals.toString()
            expenseItems.clear()
            if (existing.detailedExpenses.isNotEmpty()) {
                expenseItems.addAll(existing.detailedExpenses)
            } else {
                expenseItems.add(ExpenseItem())
            }
        } else {
            val aggregated = viewModel.getAggregatedDataForDate(selectedDate)
            val oc = aggregated["openingCash"] as Double
            val om = aggregated["openingMpesa"] as Double
            val ot = aggregated["openingTill"] as Double
            val cc = aggregated["closingCash"] as Double
            val cm = aggregated["closingMpesa"] as Double
            val ct = aggregated["closingTill"] as Double

            openingCash = if (oc == 0.0) "" else oc.toString()
            openingMpesa = if (om == 0.0) "" else om.toString()
            openingTill = if (ot == 0.0) "" else ot.toString()
            closingCash = if (cc == 0.0) "" else cc.toString()
            closingMpesa = if (cm == 0.0) "" else cm.toString()
            closingTill = if (ct == 0.0) "" else ct.toString()
            
            val expenses = aggregated["expenses"] as? List<ExpenseItem>
            expenseItems.clear()
            if (!expenses.isNullOrEmpty()) {
                expenseItems.addAll(expenses)
            } else {
                expenseItems.add(ExpenseItem())
            }
            meals = ""
        }
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = selectedDate)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = { 
                TextButton(onClick = { 
                    selectedDate = datePickerState.selectedDateMillis ?: selectedDate
                    showDatePicker = false 
                }) { Text("SELECT", fontWeight = FontWeight.Bold) } 
            }
        ) { DatePicker(state = datePickerState) }
    }

    Scaffold(
        topBar = { 
            TopAppBar(
                title = { Text("Consolidated Daily Record", fontWeight = FontWeight.Black, letterSpacing = (-0.5).sp) }, 
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) } },
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
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Period Selection Card
            ExecutiveCard(
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
                containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.05f)
            ) {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CalendarMonth, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(12.dp))
                        Text("ACCOUNTING PERIOD", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary, letterSpacing = 1.sp)
                    }
                    
                    if (user?.role == UserRole.ADMIN) {
                        Surface(
                            onClick = { showDatePicker = true },
                            color = Color.White.copy(alpha = 0.05f),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = DateTimeUtils.getFormat("EEEE, dd MMM yyyy").format(Date(selectedDate)),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Icon(Icons.Default.EditCalendar, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                            }
                        }
                    } else {
                        LaunchedEffect(Unit) { selectedDate = System.currentTimeMillis() }
                        Text(
                            text = DateTimeUtils.getFormat("EEEE, dd MMM yyyy").format(Date(selectedDate)),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White.copy(alpha = 0.7f),
                            modifier = Modifier.padding(start = 4.dp)
                        )
                    }
                }
            }

            DashboardSectionHeader("Financial Reconciliation")

            // Opening Balances
            ExecutiveCard {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
                    Text("OPENING BALANCES", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Black, color = Color.White.copy(alpha = 0.4f))
                    StandardTextField(value = openingCash, onValueChange = { openingCash = it }, label = "Opening Cash (Vault)", keyboardType = KeyboardType.Number, leadingIcon = { Icon(Icons.Default.Payments, null, tint = Color.White.copy(alpha = 0.2f)) })
                    StandardTextField(value = openingMpesa, onValueChange = { openingMpesa = it }, label = "Opening M-Pesa", keyboardType = KeyboardType.Number, leadingIcon = { Icon(Icons.Default.Smartphone, null, tint = Color.White.copy(alpha = 0.2f)) })
                    StandardTextField(value = openingTill, onValueChange = { openingTill = it }, label = "Opening Till / Buy Goods", keyboardType = KeyboardType.Number, leadingIcon = { Icon(Icons.Default.Storefront, null, tint = Color.White.copy(alpha = 0.2f)) })
                }
            }

            // Expenses section
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                    Text("OPERATING EXPENSES", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Black, color = Color.White.copy(alpha = 0.4f))
                    Surface(
                        onClick = { expenseItems.add(ExpenseItem()) },
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(Modifier.padding(horizontal = 12.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Add, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("ADD LINE", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
                
                expenseItems.forEachIndexed { index, item ->
                    ExecutiveCard(
                        border = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.05f)),
                        containerColor = Color.White.copy(alpha = 0.02f)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                StandardTextField(value = item.description, onValueChange = { expenseItems[index] = item.copy(description = it) }, label = "Line Description")
                                StandardTextField(value = if (item.price == 0.0) "" else item.price.toString(), onValueChange = { expenseItems[index] = item.copy(price = it.toDoubleOrNull() ?: 0.0) }, label = "Amount (KSh)", keyboardType = KeyboardType.Number)
                            }
                            if (expenseItems.size > 1) {
                                IconButton(onClick = { expenseItems.removeAt(index) }) {
                                    Icon(Icons.Default.RemoveCircleOutline, null, tint = Color(0xFFE57373).copy(alpha = 0.6f))
                                }
                            }
                        }
                    }
                }
            }

            // Closing section
            ExecutiveCard {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
                    Text("CLOSING RECONCILIATION", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Black, color = Color.White.copy(alpha = 0.4f))
                    StandardTextField(value = meals, onValueChange = { meals = it }, label = "Meals / Staff Refreshments", keyboardType = KeyboardType.Number, leadingIcon = { Icon(Icons.Default.Restaurant, null, tint = Color.White.copy(alpha = 0.2f)) })
                    
                    HorizontalDivider(color = Color.White.copy(alpha = 0.05f))
                    
                    StandardTextField(value = closingCash, onValueChange = { closingCash = it }, label = "Final Cash (Vault)", keyboardType = KeyboardType.Number, leadingIcon = { Icon(Icons.Default.Payments, null, tint = Color.White.copy(alpha = 0.2f)) })
                    StandardTextField(value = closingMpesa, onValueChange = { closingMpesa = it }, label = "Final M-Pesa", keyboardType = KeyboardType.Number, leadingIcon = { Icon(Icons.Default.Smartphone, null, tint = Color.White.copy(alpha = 0.2f)) })
                    StandardTextField(value = closingTill, onValueChange = { closingTill = it }, label = "Final Till Balance", keyboardType = KeyboardType.Number, leadingIcon = { Icon(Icons.Default.Storefront, null, tint = Color.White.copy(alpha = 0.2f)) })
                }
            }

            val isExisting = viewModel.getTransactionByDate(selectedDate) != null

            if (isExisting) {
                Surface(
                    color = Color(0xFFB71C1C).copy(alpha = 0.1f),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, Color(0xFFB71C1C).copy(alpha = 0.2f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Warning, null, tint = Color(0xFFE57373), modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(12.dp))
                        Text(
                            "An executive summary already exists for this date. Please use the Audit/Edit feature.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFE57373)
                        )
                    }
                }
            }

            Button(
                onClick = { 
                    viewModel.addTransaction(
                        date = selectedDate, 
                        openingCash = openingCash.toDoubleOrNull() ?: 0.0,
                        openingMpesa = openingMpesa.toDoubleOrNull() ?: 0.0,
                        openingTill = openingTill.toDoubleOrNull() ?: 0.0,
                        expenses = expenseItems.toList(), 
                        serviceRevenue = emptyMap(), 
                        meals = meals.toDoubleOrNull() ?: 0.0, 
                        closingCash = closingCash.toDoubleOrNull() ?: 0.0,
                        closingMpesa = closingMpesa.toDoubleOrNull() ?: 0.0,
                        closingTill = closingTill.toDoubleOrNull() ?: 0.0,
                        onComplete = onBack
                    )
                }, 
                enabled = !isExisting,
                modifier = Modifier.fillMaxWidth().height(60.dp), 
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) { 
                Text("SAVE EXECUTIVE SUMMARY", fontWeight = FontWeight.Black, letterSpacing = 1.sp) 
            }
            
            Spacer(Modifier.height(32.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionDetailScreen(
    transactionId: String, 
    viewModel: TransactionViewModel, 
    authViewModel: AuthViewModel,
    financialViewModel: FinancialViewModel,
    onBack: () -> Unit
) {
    val transactions by viewModel.filteredTransactions.collectAsState()
    val user by authViewModel.currentUser.collectAsState()
    val tx = transactions.find { it.id == transactionId }
    
    var showEditDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Transaction Details") }, navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null) } }) }
    ) { padding ->
        if (tx == null) Box(Modifier.fillMaxSize(), Alignment.Center) { Text("Not found") }
        else Column(Modifier.fillMaxSize().padding(padding).padding(16.dp).verticalScroll(rememberScrollState()), Arrangement.spacedBy(16.dp)) {
            val title = if (tx.items.isNotEmpty()) tx.items.first().name else "Sale"
            Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = Color.White)
            
            ExecutiveCard {
                Column(Modifier.padding(20.dp), Arrangement.spacedBy(12.dp)) {
                    DetailRowItem("Transaction ID", tx.id)
                    DetailRowItem("Date", tx.date)
                    DetailRowItem("Time", DateTimeUtils.getFormat("hh:mm:ss a").format(Date(tx.timestamp)))
                    DetailRowItem("Staff", tx.staffName)
                    DetailRowItem("Payment", tx.paymentMethod.name)
                    
                    HorizontalDivider(color = Color.Gray.copy(alpha = 0.2f))
                    
                    Text("Items Sold", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                    tx.items.forEach { item ->
                        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                            Text("${item.quantity}x ${item.name}", style = MaterialTheme.typography.bodySmall, color = Color.White)
                            Text("KSh ${item.totalAmount}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                    
                    HorizontalDivider(color = Color.Gray.copy(alpha = 0.2f))
                    DetailRowItem("Total Amount", "KSh ${String.format(Locale.US, "%,.0f", tx.totalAmount)}", color = Color(0xFF4CAF50))
                }
            }
            
            if (user?.role == UserRole.STAFF) {
                Button(
                    onClick = { showEditDialog = true },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                ) {
                    Icon(Icons.Default.Edit, null)
                    Spacer(Modifier.width(8.dp))
                    Text("REQUEST EDIT")
                }
            }
            
            Text("Audit Information", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.White)
            ExecutiveCard {
                Column(Modifier.padding(16.dp)) {
                    Text("Created At: ${tx.createdAt ?: "N/A"}", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                    Text("Status: ${tx.status}", style = MaterialTheme.typography.bodySmall, color = Color(0xFF4CAF50))
                    if (tx.approvedByName.isNotEmpty()) {
                        Text("Approved By: ${tx.approvedByName}", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                    }
                }
            }
        }
    }

    if (showEditDialog && tx != null) {
        TransactionEditRequestDialog(
            transaction = tx,
            onDismiss = { showEditDialog = false },
            onSubmit = { proposedData, reason ->
                financialViewModel.requestTransactionEdit(user!!, tx, proposedData, reason)
                showEditDialog = false
            }
        )
    }
}

@Composable
fun TransactionEditRequestDialog(
    transaction: Transaction,
    onDismiss: () -> Unit,
    onSubmit: (Map<String, Any?>, String) -> Unit
) {
    // For now, let's allow editing the total amount and payment method
    var totalAmount by remember { mutableStateOf(transaction.totalAmount.toString()) }
    var reason by remember { mutableStateOf("") }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Request Transaction Edit") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Original Total: KSh ${transaction.totalAmount}", style = MaterialTheme.typography.bodySmall)
                StandardTextField(value = totalAmount, onValueChange = { totalAmount = it }, label = "Proposed Total Amount", keyboardType = KeyboardType.Number)
                StandardTextField(value = reason, onValueChange = { reason = it }, label = "Reason for Change (Mandatory)")
            }
        },
        confirmButton = {
            Button(
                onClick = { 
                    onSubmit(mapOf("totalAmount" to (totalAmount.toDoubleOrNull() ?: transaction.totalAmount)), reason) 
                },
                enabled = reason.isNotEmpty()
            ) {
                Text("Submit Request")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun DetailRow(l: String, v: Double, b: Boolean = false, c: Color = Color.Unspecified, labelOnly: Boolean = false, textValue: String = "") {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), Arrangement.SpaceBetween) {
        Text(l, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.secondary)
        Text(
            text = if (labelOnly) textValue else if (l.contains("ID") || l.contains("Date") || l.contains("Staff")) textValue else String.format(Locale.US, "%.0f", v),
            fontWeight = if (b) FontWeight.Bold else FontWeight.Normal,
            color = c
        )
    }
}

@Composable
fun ExpenseRow(item: ExpenseItem, onDescriptionChange: (String) -> Unit, onPriceChange: (String) -> Unit, onRemove: (() -> Unit)?) {
    Row(Modifier.fillMaxWidth(), Arrangement.spacedBy(8.dp), Alignment.CenterVertically) {
        OutlinedTextField(value = item.description, onValueChange = onDescriptionChange, label = { Text("Item") }, modifier = Modifier.weight(2f), shape = RoundedCornerShape(12.dp))
        OutlinedTextField(value = if (item.price == 0.0) "" else item.price.toString(), onValueChange = onPriceChange, label = { Text("Price") }, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), shape = RoundedCornerShape(12.dp))
        if (onRemove != null) IconButton(onClick = onRemove) { Icon(Icons.Default.Remove, null, tint = Color.Red) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpenseDetailScreen(
    expenseId: String, 
    viewModel: TransactionViewModel, 
    authViewModel: AuthViewModel,
    financialViewModel: FinancialViewModel,
    onBack: () -> Unit
) {
    val reportState by viewModel.reportState.collectAsState()
    val user by authViewModel.currentUser.collectAsState()
    val ex = reportState.filteredExpenses.find { it.id == expenseId }
    
    var showEditDialog by remember { mutableStateOf(false) }

    Scaffold(topBar = { TopAppBar(title = { Text("Expense Details") }, navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) } }) }) { padding ->
        if (ex == null) Box(Modifier.fillMaxSize(), Alignment.Center) { Text("Not found") }
        else Column(Modifier.fillMaxSize().padding(padding).padding(16.dp).verticalScroll(rememberScrollState()), Arrangement.spacedBy(16.dp)) {
            Text(ex.category, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = Color(0xFFE57373))
            
            ExecutiveCard {
                Column(Modifier.padding(20.dp), Arrangement.spacedBy(12.dp)) {
                    DetailRowItem("Expense ID", ex.id)
                    DetailRowItem("Date", DateTimeUtils.getFormat("yyyy-MM-dd").format(Date(ex.timestamp)))
                    DetailRowItem("Time", DateTimeUtils.getFormat("hh:mm:ss a").format(Date(ex.timestamp)))
                    DetailRowItem("Staff", ex.staffName)
                    DetailRowItem("Amount", "KSh ${String.format(Locale.US, "%,.0f", ex.amount)}", color = Color(0xFFE57373))
                    DetailRowItem("Payment", ex.paymentMethod.name)
                    
                    HorizontalDivider(color = Color.Gray.copy(alpha = 0.2f))
                    
                    Text("Description", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                    Text(ex.description, style = MaterialTheme.typography.bodyMedium, color = Color.White)
                }
            }
            
            if (user?.role == UserRole.STAFF) {
                Button(
                    onClick = { showEditDialog = true },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                ) {
                    Icon(Icons.Default.Edit, null)
                    Spacer(Modifier.width(8.dp))
                    Text("REQUEST EDIT")
                }
            }
            
            Text("Status Information", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.White)
            ExecutiveCard {
                Column(Modifier.padding(16.dp)) {
                    Text("Current Status: ${ex.status}", fontWeight = FontWeight.Bold, color = if (ex.status == "APPROVED" || ex.status == "COMPLETED") Color(0xFF4CAF50) else Color.Gray)
                    if (ex.approvedByName.isNotEmpty()) {
                        Text("Approved By: ${ex.approvedByName}", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                    }
                }
            }
        }
    }

    if (showEditDialog && ex != null) {
        ExpenseEditRequestDialog(
            expense = ex,
            onDismiss = { showEditDialog = false },
            onSubmit = { proposedData, reason ->
                // financialViewModel.requestExpenseEdit(user!!, ex, proposedData, reason)
                financialViewModel.sendEvent("Expense edit requests coming in next update.")
                showEditDialog = false
            }
        )
    }
}

@Composable
fun ExpenseEditRequestDialog(
    expense: Expense,
    onDismiss: () -> Unit,
    onSubmit: (Map<String, Any?>, String) -> Unit
) {
    var amount by remember { mutableStateOf(expense.amount.toString()) }
    var reason by remember { mutableStateOf("") }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Request Expense Edit") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Original Amount: KSh ${expense.amount}", style = MaterialTheme.typography.bodySmall)
                StandardTextField(value = amount, onValueChange = { amount = it }, label = "Proposed Amount", keyboardType = KeyboardType.Number)
                StandardTextField(value = reason, onValueChange = { reason = it }, label = "Reason for Change (Mandatory)")
            }
        },
        confirmButton = {
            Button(
                onClick = { 
                    onSubmit(mapOf("amount" to (amount.toDoubleOrNull() ?: expense.amount)), reason) 
                },
                enabled = reason.isNotEmpty()
            ) {
                Text("Submit Request")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun TransactionTextField(label: String, value: String, onValueChange: (String) -> Unit) {
    OutlinedTextField(value = value, onValueChange = onValueChange, label = { Text(label) }, modifier = Modifier.fillMaxWidth(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), shape = RoundedCornerShape(12.dp), singleLine = true)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditDailySummaryScreen(
    recordId: String,
    viewModel: TransactionViewModel,
    authViewModel: AuthViewModel,
    onBack: () -> Unit
) {
    val allDailyBreakdowns by viewModel.allDailyBreakdowns.collectAsState()
    val legacyTransactions by viewModel.legacyTransactions.collectAsState()
    val allShifts by viewModel.allShifts.collectAsState()
    val auditLogs by viewModel.auditLogs.collectAsState()
    val user by authViewModel.currentUser.collectAsState()
    
    val isLegacy = recordId.startsWith("LEGACY_")
    val id = if (isLegacy) recordId.substringAfter("LEGACY_") else recordId

    val recordLogs = remember(auditLogs, recordId, id) {
        auditLogs.filter { it.recordId == recordId || it.recordId == id }
    }

    val shift = remember(allShifts, id) { allShifts.find { it.id == id } }
    val legacy = remember(legacyTransactions, id) { 
        legacyTransactions.find { it.id == id } ?: run {
            val ts = id.toLongOrNull() ?: 0L
            if (ts > 0) {
                val targetId = com.raymond.cms.model.DailyTransaction.createId(ts)
                legacyTransactions.find { it.id == targetId }
            } else null
        }
    }
    
    val recordDate = remember(shift, legacy) {
        shift?.date ?: legacy?.formattedDate?.substringAfter(", ") ?: "Unknown Date"
    }
    
    var openCash by remember { mutableStateOf("") }
    var openMpesa by remember { mutableStateOf("") }
    var openTill by remember { mutableStateOf("") }
    
    var closeCash by remember { mutableStateOf("") }
    var closeMpesa by remember { mutableStateOf("") }
    var closeTill by remember { mutableStateOf("") }

    var expenses by remember { mutableStateOf("") }
    var meals by remember { mutableStateOf("") }
    var editReason by remember { mutableStateOf("") }

    LaunchedEffect(shift, legacy) {
        if (shift != null) {
            openCash = shift.openingCash.toString()
            openMpesa = shift.openingMpesa.toString()
            openTill = shift.openingTill.toString()
            closeCash = shift.closingCash.toString()
            closeMpesa = shift.closingMpesa.toString()
            closeTill = shift.closingTill.toString()
            expenses = "0"; meals = "0"
        } else if (legacy != null) {
            val hasNewOpening = (legacy.openingCash + legacy.openingMpesa + legacy.openingTill) > 0
            val hasNewClosing = (legacy.closingCash + legacy.closingMpesa + legacy.closingTill) > 0
            openCash = if (hasNewOpening) legacy.openingCash.toString() else legacy.openingAmount.toString()
            openMpesa = if (hasNewOpening) legacy.openingMpesa.toString() else "0"
            openTill = if (hasNewOpening) legacy.openingTill.toString() else "0"
            closeCash = if (hasNewClosing) legacy.closingCash.toString() else legacy.closingAmount.toString()
            closeMpesa = if (hasNewClosing) legacy.closingMpesa.toString() else "0"
            closeTill = if (hasNewClosing) legacy.closingTill.toString() else "0"
            expenses = legacy.totalExpenses.toString()
            meals = legacy.meals.toString()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Column {
                        Text("Audit Correction", fontWeight = FontWeight.ExtraBold)
                        Text(recordDate, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
                    }
                },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        containerColor = Color(0xFF0F1318)
    ) { padding ->
        Column(
            modifier = Modifier.padding(padding).fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Header Info
            ExecutiveCard(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.1f)) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Info, null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(16.dp))
                    Text(
                        "You are modifying the financial balances for $recordDate. All changes will be logged in the audit trail.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                }
            }

            // Opening Section
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                SectionHeader("OPENING BALANCES", Icons.Default.Login, Color(0xFF2196F3))
                ExecutiveCard {
                    Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        BalanceEditField("Physical Cash", openCash, { openCash = it }, Icons.Default.Payments)
                        BalanceEditField("M-Pesa Balance", openMpesa, { openMpesa = it }, Icons.Default.PhoneIphone)
                        BalanceEditField("Till Balance", openTill, { openTill = it }, Icons.Default.Store)
                    }
                }
            }

            // Closing Section
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                SectionHeader("CLOSING BALANCES", Icons.Default.Logout, Color(0xFF4CAF50))
                ExecutiveCard {
                    Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        BalanceEditField("Physical Cash", closeCash, { closeCash = it }, Icons.Default.Payments)
                        BalanceEditField("M-Pesa Balance", closeMpesa, { closeMpesa = it }, Icons.Default.PhoneIphone)
                        BalanceEditField("Till Balance", closeTill, { closeTill = it }, Icons.Default.Store)
                    }
                }
            }

            if (isLegacy) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    SectionHeader("RECONCILIATION", Icons.Default.Build, Color(0xFFFF9800))
                    ExecutiveCard {
                        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            BalanceEditField("Total Expenses", expenses, { expenses = it }, Icons.Default.RemoveCircle)
                            BalanceEditField("Meals", meals, { meals = it }, Icons.Default.Restaurant)
                        }
                    }
                }
            }

            // Reason Section
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                SectionHeader("REASON FOR MODIFICATION", Icons.Default.Notes, Color.White)
                StandardTextField(
                    value = editReason, 
                    onValueChange = { editReason = it }, 
                    label = "Explain why you are changing these records",
                    leadingIcon = { Icon(Icons.Default.EditNote, null, tint = Color.Gray) }
                )
            }

            // Recalculation Preview Section
            val totalOp = (openCash.toDoubleOrNull() ?: 0.0) + (openMpesa.toDoubleOrNull() ?: 0.0) + (openTill.toDoubleOrNull() ?: 0.0)
            val totalCl = (closeCash.toDoubleOrNull() ?: 0.0) + (closeMpesa.toDoubleOrNull() ?: 0.0) + (closeTill.toDoubleOrNull() ?: 0.0)
            val totalEx = expenses.toDoubleOrNull() ?: 0.0
            val totalMe = meals.toDoubleOrNull() ?: 0.0
            
            val projectedProfit = (totalCl - totalOp) 
            val projectedRevenue = projectedProfit + totalEx + totalMe

            DashboardSectionHeader("Audit Recalculation Preview")
            ExecutiveCard(
                containerColor = Color.White.copy(alpha = 0.02f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
            ) {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                        Column {
                            Text("PROJECTED REVENUE", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.4f), fontWeight = FontWeight.Black)
                            Text("KSH ${String.format(Locale.US, "%,.0f", projectedRevenue)}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("PROJECTED PROFIT", style = MaterialTheme.typography.labelSmall, color = Color(0xFF4CAF50).copy(alpha = 0.6f), fontWeight = FontWeight.Black)
                            Text("KSH ${String.format(Locale.US, "%,.0f", projectedProfit)}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black, color = Color(0xFF4CAF50))
                        }
                    }
                    
                    LinearProgressIndicator(
                        progress = 1f,
                        modifier = Modifier.fillMaxWidth().height(2.dp),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                        trackColor = Color.Transparent
                    )
                    
                    Text(
                        "Values are calculated in real-time based on your manual adjustments. The profit reflects the net growth between opening and closing balances.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.4f),
                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                    )
                }
            }

            Button(
                onClick = {
                    val admin = user ?: return@Button
                    viewModel.updateDailySummary(
                        recordId = recordId,
                        openingCash = openCash.toDoubleOrNull() ?: 0.0,
                        openingMpesa = openMpesa.toDoubleOrNull() ?: 0.0,
                        openingTill = openTill.toDoubleOrNull() ?: 0.0,
                        closingCash = closeCash.toDoubleOrNull() ?: 0.0,
                        closingMpesa = closeMpesa.toDoubleOrNull() ?: 0.0,
                        closingTill = closeTill.toDoubleOrNull() ?: 0.0,
                        expenses = expenses.toDoubleOrNull() ?: 0.0,
                        meals = meals.toDoubleOrNull() ?: 0.0,
                        reason = editReason,
                        user = admin
                    )
                    onBack()
                },
                modifier = Modifier.fillMaxWidth().height(64.dp),
                shape = RoundedCornerShape(16.dp),
                enabled = editReason.isNotEmpty()
            ) {
                Icon(Icons.Default.Save, null)
                Spacer(Modifier.width(12.dp))
                Text(if (user?.role == UserRole.ADMIN) "SAVE & LOG CHANGES" else "SUBMIT FOR APPROVAL", fontWeight = FontWeight.Bold)
            }

            // Audit Trail Section
            if (recordLogs.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    SectionHeader("CHANGE HISTORY", Icons.Default.History, Color.Gray)
                    recordLogs.forEach { log ->
                        AuditTrailItem(log)
                    }
                }
            }
            
            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
fun SectionHeader(title: String, icon: ImageVector, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = color.copy(alpha = 0.7f), modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text(title, style = MaterialTheme.typography.labelLarge, color = Color.Gray, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun BalanceEditField(label: String, value: String, onValueChange: (String) -> Unit, icon: ImageVector) {
    StandardTextField(
        value = value,
        onValueChange = onValueChange,
        label = label,
        keyboardType = KeyboardType.Number,
        leadingIcon = { Icon(icon, null, tint = Color.Gray, modifier = Modifier.size(20.dp)) }
    )
}

@Composable
fun AuditTrailItem(log: AuditLog) {
    ExecutiveCard(containerColor = Color(0xFF1E252D)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        log.action, 
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        fontSize = 10.sp, 
                        fontWeight = FontWeight.ExtraBold, 
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Text(
                    log.timestamp?.let { DateTimeUtils.getFormat("dd MMM, HH:mm").format(it) } ?: "",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.Gray
                )
            }
            Text(log.description, style = MaterialTheme.typography.bodyMedium, color = Color.White)
            if (log.oldValue.isNotEmpty() || log.newValue.isNotEmpty()) {
                Surface(
                    color = Color.Black.copy(alpha = 0.3f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(Modifier.padding(12.dp), Arrangement.spacedBy(16.dp)) {
                        Column(Modifier.weight(1f)) {
                            Text("WAS", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                            Text(log.oldValue, style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.6f))
                        }
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, null, tint = Color.Gray, modifier = Modifier.align(Alignment.CenterVertically))
                        Column(Modifier.weight(1f)) {
                            Text("NOW", style = MaterialTheme.typography.labelSmall, color = Color(0xFF4CAF50))
                            Text(log.newValue, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = Color(0xFF4CAF50))
                        }
                    }
                }
            }
            Text("Modified by: ${log.userName} (${log.userRole})", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BalanceDiscrepanciesScreen(
    viewModel: TransactionViewModel,
    authViewModel: AuthViewModel,
    onBack: () -> Unit
) {
    val allShifts by viewModel.allShifts.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val user by authViewModel.currentUser.collectAsState()
    
    // Filter shifts that have a difference or were flagged (including resolved ones for audit)
    val discrepancies = allShifts.filter { it.openingBalanceDifference != 0.0 || it.flaggedForReview }
    
    var showResolveDialog by remember { mutableStateOf<com.raymond.cms.model.Shift?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Balance Discrepancies", fontWeight = FontWeight.Black, letterSpacing = (-0.5).sp) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        containerColor = Color(0xFF0F1318)
    ) { padding ->
        if (isLoading) {
            Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator(color = MaterialTheme.colorScheme.primary) }
        } else if (discrepancies.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.CheckCircle, null, modifier = Modifier.size(64.dp), tint = Color(0xFF4CAF50).copy(alpha = 0.2f))
                    Spacer(Modifier.height(16.dp))
                    Text("No system discrepancies found.", color = Color.White.copy(alpha = 0.4f), style = MaterialTheme.typography.bodyLarge)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    DashboardSectionHeader("Audit Discrepancy Registry")
                }

                items(discrepancies.size) { index ->
                    val shift = discrepancies[index]
                    DiscrepancyItem(
                        shift = shift, 
                        onClick = { 
                            if (shift.varianceStatus == "PENDING") {
                                showResolveDialog = shift 
                            }
                        }
                    )
                }
            }
        }
    }

    if (showResolveDialog != null) {
        var comment by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showResolveDialog = null },
            title = { Text("Operational Audit Resolution", fontWeight = FontWeight.ExtraBold, letterSpacing = (-0.5).sp) },
            containerColor = Color(0xFF1E252D),
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text(
                        "Determine the cause of the KSH ${String.format(Locale.US, "%,.0f", showResolveDialog!!.openingBalanceDifference)} variance. Your decision will be archived.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.7f)
                    )
                    StandardTextField(
                        value = comment,
                        onValueChange = { comment = it },
                        label = "Administrative Findings",
                        leadingIcon = { Icon(Icons.Default.RateReview, null, tint = Color.White.copy(alpha = 0.3f)) }
                    )
                }
            },
            confirmButton = {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(
                        onClick = {
                            viewModel.resolveVariance(showResolveDialog!!.id, "REVIEWED", comment, user!!)
                            showResolveDialog = null
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.2f))
                    ) { Text("MARK REVIEWED", fontSize = 10.sp, fontWeight = FontWeight.Bold) }
                    
                    Button(
                        onClick = {
                            viewModel.resolveVariance(showResolveDialog!!.id, "RESOLVED", comment, user!!)
                            showResolveDialog = null
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) { Text("FINALIZE AUDIT", fontSize = 10.sp, fontWeight = FontWeight.Black) }
                }
            },
            dismissButton = {
                TextButton(onClick = { showResolveDialog = null }) { 
                    Text("CANCEL", color = Color.White.copy(alpha = 0.5f)) 
                }
            }
        )
    }
}

@Composable
fun DiscrepancyItem(shift: com.raymond.cms.model.Shift, onClick: () -> Unit) {
    ExecutiveCard(onClick = onClick) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text(text = shift.staffName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.White)
                    Text(text = shift.date, style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.6f))
                }
                val isPending = shift.varianceStatus == "PENDING"
                Surface(
                    color = when(shift.varianceStatus) {
                        "RESOLVED" -> Color(0xFF4CAF50).copy(alpha = 0.1f)
                        "REVIEWED" -> Color(0xFFFF9800).copy(alpha = 0.1f)
                        else -> Color(0xFFB71C1C).copy(alpha = 0.2f)
                    },
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, when(shift.varianceStatus) {
                        "RESOLVED" -> Color(0xFF4CAF50).copy(alpha = 0.2f)
                        "REVIEWED" -> Color(0xFFFF9800).copy(alpha = 0.2f)
                        else -> Color(0xFFB71C1C).copy(alpha = 0.2f)
                    })
                ) {
                    Text(
                        text = if (isPending) "DISCREPANCY DETECTED" else "AUDIT ${shift.varianceStatus}",
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = when(shift.varianceStatus) {
                            "RESOLVED" -> Color(0xFF4CAF50)
                            "REVIEWED" -> Color(0xFFFF9800)
                            else -> Color(0xFFE57373)
                        },
                        fontWeight = FontWeight.Black,
                        letterSpacing = 0.5.sp
                    )
                }
            }
            
            if (shift.adminReviewComment.isNotEmpty()) {
                Surface(
                    color = Color.White.copy(alpha = 0.03f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Admin Note: ${shift.adminReviewComment}",
                        modifier = Modifier.padding(12.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.5f),
                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                    )
                }
            }
            
            HorizontalDivider(color = Color.White.copy(alpha = 0.05f))

            Row(Modifier.fillMaxWidth()) {
                Column(Modifier.weight(1f)) {
                    Text("EXPECTED", style = MaterialTheme.typography.labelSmall, color = Color.Gray, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    DetailRowItem("Cash", "KSh ${shift.expectedOpeningCash}")
                    DetailRowItem("M-Pesa", "KSh ${shift.expectedOpeningMpesa}")
                    DetailRowItem("Till", "KSh ${shift.expectedOpeningTill}")
                    DetailRowItem("Total", "KSh ${shift.expectedOpeningTotal}", color = Color.White)
                }
                
                Spacer(Modifier.width(24.dp))

                Column(Modifier.weight(1f)) {
                    Text("ACTUAL", style = MaterialTheme.typography.labelSmall, color = Color(0xFF2196F3), fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    DetailRowItem("Cash", "KSh ${shift.openingCash}")
                    DetailRowItem("M-Pesa", "KSh ${shift.openingMpesa}")
                    DetailRowItem("Till", "KSh ${shift.openingTill}")
                    DetailRowItem("Total", "KSh ${shift.totalOpening}", color = Color.White)
                }
            }
            
            Surface(
                color = Color(0xFFB71C1C).copy(alpha = 0.1f),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp).fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("VARIANCE", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = Color.White)
                    Text(
                        text = "KSH ${String.format(Locale.US, "%,.0f", shift.openingBalanceDifference)}",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (shift.openingBalanceDifference >= 0) Color(0xFF4CAF50) else Color(0xFFF44336)
                    )
                }
            }
        }
    }
}

@Composable
fun DiscrepancyColumn(label: String, value: Double) {
    Column {
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.5f))
        Text(text = "KSH ${String.format(Locale.US, "%,.0f", value)}", style = MaterialTheme.typography.bodyLarge, color = Color.White)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SuppliesScreen(viewModel: TransactionViewModel, onMenuClick: () -> Unit) {
    val inventory by viewModel.inventory.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    Scaffold(
        topBar = { 
            TopAppBar(
                title = { Text("Enterprise Inventory Control", fontWeight = FontWeight.Black, letterSpacing = (-0.5).sp) }, 
                navigationIcon = { IconButton(onClick = onMenuClick) { Icon(Icons.Default.Menu, null) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            ) 
        },
        containerColor = Color(0xFF0F1318)
    ) { padding ->
        if (isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding), 
                contentPadding = PaddingValues(16.dp), 
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    val lowStockCount = inventory.count { it.currentStock <= it.lowStockThreshold }
                    if (lowStockCount > 0) {
                        Surface(
                            color = Color(0xFFB71C1C).copy(alpha = 0.1f),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, Color(0xFFB71C1C).copy(alpha = 0.2f)),
                            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                        ) {
                            Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Warning, null, tint = Color(0xFFE57373), modifier = Modifier.size(20.dp))
                                Spacer(Modifier.width(12.dp))
                                Text(
                                    text = "ALERT: $lowStockCount ITEMS BELOW THRESHOLD",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Black,
                                    color = Color(0xFFE57373),
                                    letterSpacing = 1.sp
                                )
                            }
                        }
                    }
                }

                item {
                    DashboardSectionHeader("Resources & Stock")
                }

                items(inventory) { item ->
                    val isCritical = item.currentStock <= item.lowStockThreshold
                    ExecutiveCard(
                        border = BorderStroke(1.dp, if (isCritical) Color(0xFFF44336).copy(alpha = 0.2f) else Color.White.copy(alpha = 0.05f))
                    ) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.Top) {
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        text = item.name, 
                                        fontWeight = FontWeight.Bold, 
                                        style = MaterialTheme.typography.titleMedium,
                                        color = Color.White
                                    )
                                    Text(
                                        text = "Units: ${item.unit.uppercase()}", 
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.White.copy(alpha = 0.4f),
                                        letterSpacing = 1.sp
                                    )
                                }

                                Surface(
                                    color = if (isCritical) Color(0xFFF44336).copy(alpha = 0.1f) else Color(0xFF4CAF50).copy(alpha = 0.1f),
                                    shape = RoundedCornerShape(8.dp),
                                    border = BorderStroke(1.dp, if (isCritical) Color(0xFFF44336).copy(alpha = 0.2f) else Color(0xFF4CAF50).copy(alpha = 0.2f))
                                ) {
                                    Text(
                                        text = if (isCritical) "CRITICAL" else "OPTIMAL",
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Black,
                                        color = if (isCritical) Color(0xFFF44336) else Color(0xFF4CAF50)
                                    )
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(), 
                                horizontalArrangement = Arrangement.SpaceBetween, 
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "${item.currentStock.toInt()}", 
                                        style = MaterialTheme.typography.headlineMedium, 
                                        fontWeight = FontWeight.Black,
                                        color = Color.White
                                    )
                                    Text(
                                        text = "CURRENT LEVEL", 
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.White.copy(alpha = 0.3f)
                                    )
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Surface(
                                        color = Color.White.copy(alpha = 0.05f),
                                        shape = RoundedCornerShape(10.dp),
                                        onClick = { viewModel.updateStock(item.id, item.currentStock - 1) }
                                    ) {
                                        Icon(Icons.Default.Remove, null, modifier = Modifier.padding(8.dp).size(20.dp), tint = Color.White.copy(alpha = 0.7f))
                                    }

                                    Surface(
                                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                                        shape = RoundedCornerShape(10.dp),
                                        onClick = { viewModel.updateStock(item.id, item.currentStock + 1) }
                                    ) {
                                        Icon(Icons.Default.Add, null, modifier = Modifier.padding(8.dp).size(20.dp), tint = MaterialTheme.colorScheme.primary)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

