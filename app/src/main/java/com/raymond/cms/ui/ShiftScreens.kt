package com.raymond.cms.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.FactCheck
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.raymond.cms.model.Shift
import com.raymond.cms.model.Service
import com.raymond.cms.model.Transaction
import com.raymond.cms.model.Expense
import com.raymond.cms.model.ExpenseItem
import com.raymond.cms.model.PaymentMethod
import com.raymond.cms.ui.components.*
import com.raymond.cms.util.SecurityHelper
import androidx.fragment.app.FragmentActivity
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ShiftInProgressScreen(
    shift: Shift,
    onContinue: () -> Unit
) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            modifier = Modifier
                .systemBarsPadding()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(Icons.Default.HourglassEmpty, null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(24.dp))
            Text("SHIFT IN PROGRESS", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text("You are already clocked in.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.secondary)
            
            Spacer(Modifier.height(32.dp))
            
            ExecutiveCard {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    DetailRowItem("Staff Name", shift.staffName)
                    DetailRowItem("Clock-in Date", shift.clockInDate)
                    DetailRowItem("Clock-in Time", SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(shift.clockInTime)))
                    HorizontalDivider(color = Color.Gray.copy(alpha = 0.2f), modifier = Modifier.padding(vertical = 8.dp))
                    DetailRowItem("Opening Cash", "KSh ${shift.openingCash}")
                    DetailRowItem("Opening M-Pesa", "KSh ${shift.openingMpesa}")
                    DetailRowItem("Opening Till", "KSh ${shift.openingTill}")
                    HorizontalDivider(color = Color.Gray.copy(alpha = 0.2f), modifier = Modifier.padding(vertical = 8.dp))
                    Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                        Text("Status", color = Color.White.copy(alpha = 0.6f))
                        Text("ON DUTY", color = Color(0xFF4CAF50), fontWeight = FontWeight.ExtraBold)
                    }
                }
            }
            
            Spacer(Modifier.height(48.dp))
            Button(
                onClick = onContinue,
                modifier = Modifier.fillMaxWidth().height(56.dp)
            ) {
                Text("CONTINUE SHIFT", fontWeight = FontWeight.Bold)
            }
        }
    }
}


@Composable
fun ClockInPromptScreen(
    onClockIn: () -> Unit,
    onSkip: () -> Unit
) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            modifier = Modifier
                .systemBarsPadding()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(Icons.AutoMirrored.Filled.Login, null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(24.dp))
            Text("Start Your Shift", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Text(
                "Clocking in is required before you can record transactions or expenses.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.secondary,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            Spacer(Modifier.height(48.dp))
            Button(
                onClick = onClockIn,
                modifier = Modifier.fillMaxWidth().height(56.dp)
            ) {
                Text("CLOCK IN", fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(16.dp))
            OutlinedButton(
                onClick = onSkip,
                modifier = Modifier.fillMaxWidth().height(56.dp)
            ) {
                Text("SKIP (Read-Only Mode)")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OpeningBalanceConfirmScreen(
    viewModel: AuthViewModel,
    onConfirmed: () -> Unit,
    onBack: () -> Unit
) {
    val isLoading by viewModel.isLoading.collectAsState()
    val lastShift by viewModel.lastShift.collectAsState()
    val activeShift by viewModel.activeShift.collectAsState()
    
    LaunchedEffect(Unit) {
        viewModel.fetchLastShift()
    }
    
    var cash by remember { mutableStateOf("") }
    var mpesa by remember { mutableStateOf("") }
    var till by remember { mutableStateOf("") }
    
    var showDifferenceAlert by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(Unit) {
        viewModel.uiEvent.collect { snackbarHostState.showSnackbar(it) }
    }
    
    val error by viewModel.error.collectAsState()
    LaunchedEffect(error) {
        error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    Scaffold(
        topBar = { 
            TopAppBar(
                title = { Text("Confirm Opening Balance") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text("Previous BUSINESS Closing Balance", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    Text("Source of truth for today's opening.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
                    Spacer(Modifier.height(12.dp))
                    BalanceRow("Cash", lastShift?.closingCash ?: 0.0)
                    BalanceRow("M-Pesa", lastShift?.closingMpesa ?: 0.0)
                    BalanceRow("Till", lastShift?.closingTill ?: 0.0)
                    HorizontalDivider(Modifier.padding(vertical = 8.dp))
                    BalanceRow("Total Expected", lastShift?.totalClosing ?: 0.0, isBold = true)
                }
            }

            Text("Enter Actual Amounts Found", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

            OutlinedTextField(
                value = cash,
                onValueChange = { cash = it },
                label = { Text("Actual Cash in Drawer") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                enabled = !isLoading && activeShift == null
            )

            OutlinedTextField(
                value = mpesa,
                onValueChange = { mpesa = it },
                label = { Text("Actual M-Pesa Balance") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                enabled = !isLoading && activeShift == null
            )

            OutlinedTextField(
                value = till,
                onValueChange = { till = it },
                label = { Text("Actual Till Balance") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                enabled = !isLoading && activeShift == null
            )

            val total = (cash.toDoubleOrNull() ?: 0.0) + (mpesa.toDoubleOrNull() ?: 0.0) + (till.toDoubleOrNull() ?: 0.0)
            val expectedTotal = lastShift?.totalClosing ?: 0.0
            val difference = total - expectedTotal
            
            if (activeShift == null) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Row(Modifier.padding(16.dp).fillMaxWidth(), Arrangement.SpaceBetween) {
                        Text("Total Opening Balance")
                        Text("KSh ${String.format(Locale.US, "%,.0f", total)}", fontWeight = FontWeight.Bold)
                    }
                }

                Button(
                    onClick = {
                        viewModel.startShift(
                            cash.toDoubleOrNull() ?: 0.0,
                            mpesa.toDoubleOrNull() ?: 0.0,
                            till.toDoubleOrNull() ?: 0.0
                        )
                    },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    enabled = !isLoading && (cash.isNotEmpty() || mpesa.isNotEmpty() || till.isNotEmpty())
                ) {
                    if (isLoading) CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White)
                    else Text("Verify & Start Shift")
                }
            } else {
                // Requirement #6: Difference detected alert
                val hasDifference = Math.abs(activeShift!!.openingBalanceDifference) > 0.1
                
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (hasDifference) MaterialTheme.colorScheme.errorContainer else Color(0xFFE8F5E9)
                    )
                ) {
                    Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                if (hasDifference) Icons.Default.Warning else Icons.Default.CheckCircle, 
                                null, 
                                tint = if (hasDifference) MaterialTheme.colorScheme.error else Color(0xFF2E7D32)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                if (hasDifference) "OPENING DIFFERENCE DETECTED" else "BALANCE VERIFIED",
                                fontWeight = FontWeight.Bold,
                                color = if (hasDifference) MaterialTheme.colorScheme.onErrorContainer else Color(0xFF2E7D32)
                            )
                        }
                        
                        if (hasDifference) {
                            Text(
                                "The opening balance differs by KSh ${String.format(Locale.US, "%,.0f", activeShift!!.openingBalanceDifference)}. This has been recorded and the Admin notified.",
                                style = MaterialTheme.typography.bodySmall
                            )
                        } else {
                            Text("The opening balance matches the previous business closing record.", style = MaterialTheme.typography.bodySmall)
                        }
                        
                        Spacer(Modifier.height(12.dp))
                        Button(
                            onClick = onConfirmed,
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (hasDifference) MaterialTheme.colorScheme.error else Color(0xFF2E7D32)
                            )
                        ) {
                            Text("CONTINUE TO DASHBOARD")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun BalanceRow(label: String, value: Double, isBold: Boolean = false) {
    Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
        Text(label)
        Text("KSh ${String.format(Locale.US, "%,.0f", value)}", fontWeight = if (isBold) FontWeight.Bold else FontWeight.Normal)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServicePricesScreen(
    viewModel: AuthViewModel,
    onContinue: () -> Unit
) {
    val services by viewModel.services.collectAsState()
    var showRequestDialog by remember { mutableStateOf<Service?>(null) }

    Scaffold(
        topBar = { 
            TopAppBar(
                title = { Text("System Asset Valuations", fontWeight = FontWeight.Black, letterSpacing = (-0.5).sp) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            ) 
        },
        containerColor = Color(0xFF0F1318)
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Surface(
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.05f),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
            ) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.VerifiedUser, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(12.dp))
                    Text(
                        text = "Verify market valuations for effective operations. Adjustments require administrative audit.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.7f)
                    )
                }
            }

            DashboardSectionHeader("Current Asset Valuations")

            LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(services) { service ->
                    ExecutiveCard(
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
                    ) {
                        Row(modifier = Modifier.padding(16.dp), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(
                                    text = service.name, 
                                    fontWeight = FontWeight.Bold, 
                                    style = MaterialTheme.typography.titleMedium,
                                    color = Color.White
                                )
                                Text(
                                    text = "Current Value: KSH ${String.format(Locale.US, "%,.0f", service.price)}", 
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Black
                                )
                            }
                            Surface(
                                onClick = { showRequestDialog = service },
                                color = Color.White.copy(alpha = 0.05f),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(
                                    Icons.Default.Edit, 
                                    contentDescription = "Request Change", 
                                    modifier = Modifier.padding(8.dp).size(20.dp),
                                    tint = Color.White.copy(alpha = 0.6f)
                                )
                            }
                        }
                    }
                }
            }

            Button(
                onClick = onContinue, 
                modifier = Modifier.fillMaxWidth().height(60.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text("VALIDATE & PROCEED", fontWeight = FontWeight.Black, letterSpacing = 1.sp)
            }
        }
    }

    if (showRequestDialog != null) {
        PriceChangeRequestDialog(
            service = showRequestDialog!!,
            onDismiss = { showRequestDialog = null },
            onSubmit = { newPrice, reason ->
                viewModel.requestPriceChange(showRequestDialog!!, newPrice, reason)
                showRequestDialog = null
            }
        )
    }
}

@Composable
fun PriceChangeRequestDialog(
    service: Service,
    onDismiss: () -> Unit,
    onSubmit: (Double, String) -> Unit
) {
    var newPrice by remember { mutableStateOf("") }
    var reason by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Price Adjustment Audit", fontWeight = FontWeight.ExtraBold, letterSpacing = (-0.5).sp) },
        containerColor = Color(0xFF1E252D),
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(
                    "You are requesting an adjustment for ${service.name}. All changes are logged for financial compliance.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.7f)
                )
                StandardTextField(
                    value = newPrice,
                    onValueChange = { newPrice = it },
                    label = "Proposed Asset Value (KSh)",
                    keyboardType = KeyboardType.Number,
                    leadingIcon = { Icon(Icons.Default.PriceChange, null, tint = Color.White.copy(alpha = 0.3f)) }
                )
                StandardTextField(
                    value = reason,
                    onValueChange = { reason = it },
                    label = "Justification for Adjustment",
                    leadingIcon = { Icon(Icons.Default.RateReview, null, tint = Color.White.copy(alpha = 0.3f)) }
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSubmit(newPrice.toDoubleOrNull() ?: 0.0, reason) }, 
                enabled = newPrice.isNotEmpty() && reason.isNotEmpty(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("SUBMIT AUDIT REQUEST", fontWeight = FontWeight.Black)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { 
                Text("CANCEL", color = Color.White.copy(alpha = 0.5f)) 
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClockOutScreen(
    viewModel: AuthViewModel,
    onBack: () -> Unit,
    onClockOutComplete: () -> Unit
) {
    val activeShift by viewModel.activeShift.collectAsState()
    val transactions by viewModel.shiftTransactions.collectAsState()
    val expenses by viewModel.shiftExpenses.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    var cash by remember { mutableStateOf("") }
    var mpesa by remember { mutableStateOf("") }
    var till by remember { mutableStateOf("") }
    var meals by remember { mutableStateOf("") }
    val additionalExpenses = remember { mutableStateListOf(ExpenseItem()) }
    
    var step by remember { mutableIntStateOf(0) } // 0 = Balances, 1 = Expenses, 2 = Summary
    var showPasswordAuth by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val totalRevenue = transactions.sumOf { it.totalAmount }
    val totalExpenses = expenses.sumOf { it.amount } + additionalExpenses.sumOf { it.price }
    val openingBalance = activeShift?.totalOpening ?: 0.0
    val expectedBalance = openingBalance + totalRevenue - totalExpenses - (meals.toDoubleOrNull() ?: 0.0)
    val actualClosing = (cash.toDoubleOrNull() ?: 0.0) + (mpesa.toDoubleOrNull() ?: 0.0) + (till.toDoubleOrNull() ?: 0.0)
    val difference = actualClosing - expectedBalance

    Scaffold(
        topBar = { 
            TopAppBar(
                title = { 
                    Column {
                        Text("Shift Conclusion", fontWeight = FontWeight.ExtraBold)
                        Text(activeShift?.date ?: "Daily Record", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
                    }
                }, 
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            ) 
        },
        containerColor = Color(0xFF0F1318)
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState()), 
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Progress Indicator
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                repeat(3) { i ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(if (step >= i) MaterialTheme.colorScheme.primary else Color.Gray.copy(alpha = 0.2f))
                    )
                }
            }

            when (step) {
                0 -> {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        SectionHeader("FINAL BALANCES", Icons.Default.AccountBalanceWallet, Color(0xFF4CAF50))
                        ExecutiveCard {
                            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                BalanceEditField("Final Cash in Hand", cash, { cash = it }, Icons.Default.Payments)
                                BalanceEditField("Final M-Pesa Total", mpesa, { mpesa = it }, Icons.Default.PhoneIphone)
                                BalanceEditField("Final Till Total", till, { till = it }, Icons.Default.Store)
                            }
                        }
                    }
                    
                    Button(
                        onClick = { step = 1 }, 
                        modifier = Modifier.fillMaxWidth().height(64.dp),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text("CONTINUE TO EXPENSES", fontWeight = FontWeight.Bold)
                        Spacer(Modifier.width(8.dp))
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, null)
                    }
                }
                1 -> {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        SectionHeader("DEDUCTIONS & MEALS", Icons.Default.Restaurant, Color(0xFFFF9800))
                        BalanceEditField("Meals Total", meals, { meals = it }, Icons.Default.LocalDining)
                    }

                    if (expenses.isNotEmpty()) {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            SectionHeader("RECORDED EXPENSES", Icons.AutoMirrored.Filled.ReceiptLong, Color(0xFFE57373))
                            ExecutiveCard(containerColor = Color(0xFF1E252D).copy(alpha = 0.5f)) {
                                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    expenses.forEach { ex ->
                                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                            Text(ex.category, style = MaterialTheme.typography.bodySmall, color = Color.White)
                                            Text("KSh ${String.format(Locale.US, "%,.0f", ex.amount)}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = Color(0xFFE57373))
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        SectionHeader("ADDITIONAL EXPENSES", Icons.Default.AddCircle, Color.Cyan)
                        additionalExpenses.forEachIndexed { index, item ->
                            ExecutiveCard {
                                Row(Modifier.padding(16.dp), Arrangement.spacedBy(12.dp), Alignment.CenterVertically) {
                                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        StandardTextField(value = item.description, onValueChange = { additionalExpenses[index] = item.copy(description = it) }, label = "Item Description")
                                        StandardTextField(value = if (item.price == 0.0) "" else item.price.toString(), onValueChange = { additionalExpenses[index] = item.copy(price = it.toDoubleOrNull() ?: 0.0) }, label = "Amount", keyboardType = KeyboardType.Number)
                                    }
                                    IconButton(onClick = { additionalExpenses.removeAt(index) }) {
                                        Icon(Icons.Default.RemoveCircleOutline, null, tint = Color.Red.copy(alpha = 0.7f))
                                    }
                                }
                            }
                        }
                        
                        TextButton(
                            onClick = { additionalExpenses.add(com.raymond.cms.model.ExpenseItem()) },
                            modifier = Modifier.align(Alignment.CenterHorizontally)
                        ) {
                            Icon(Icons.Default.Add, null)
                            Spacer(Modifier.width(8.dp))
                            Text("ADD ANOTHER EXPENSE")
                        }
                    }

                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedButton(
                            onClick = { step = 0 }, 
                            modifier = Modifier.weight(1f).height(56.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) { Text("BACK") }
                        Button(
                            onClick = { step = 2 }, 
                            modifier = Modifier.weight(1.5f).height(56.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) { Text("REVIEW SUMMARY") }
                    }
                }
                2 -> {
                    val productProfit = transactions.sumOf { it.totalProfit }
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        SectionHeader("EXECUTIVE RECONCILIATION", Icons.AutoMirrored.Filled.FactCheck, Color.White)
                        ExecutiveCard {
                            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                SummaryRowItem("Opening Balance", openingBalance)
                                SummaryRowItem("Shift Revenue", totalRevenue)
                                SummaryRowItem("Shift Expenses", totalExpenses)
                                SummaryRowItem("Meals", meals.toDoubleOrNull() ?: 0.0)
                                
                                Surface(
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.05f),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.padding(vertical = 4.dp)
                                ) {
                                    Row(Modifier.padding(12.dp).fillMaxWidth(), Arrangement.SpaceBetween) {
                                        Text("ITEM GROSS PROFIT", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Black)
                                        Text("KSH ${String.format(Locale.US, "%,.0f", productProfit)}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = Color.White)
                                    }
                                }

                                HorizontalDivider(color = Color.Gray.copy(alpha = 0.1f), modifier = Modifier.padding(vertical = 4.dp))
                                SummaryRowItem("Expected Balance", expectedBalance, isBold = true)
                                SummaryRowItem("Actual Closing", actualClosing, isBold = true, color = Color(0xFF2196F3))
                                
                                Surface(
                                    color = (if (difference >= 0) Color(0xFF4CAF50) else Color(0xFFF44336)).copy(alpha = 0.1f),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                                ) {
                                    Row(Modifier.padding(16.dp), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                                        Text("VARIANCE", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                                        Text(
                                            text = "KSH ${String.format(Locale.US, "%,.0f", difference)}", 
                                            style = MaterialTheme.typography.titleLarge,
                                            fontWeight = FontWeight.ExtraBold, 
                                            color = if (difference >= 0) Color(0xFF4CAF50) else Color(0xFFF44336)
                                        )
                                    }
                                }
                            }
                        }
                    }
                    
                    Button(
                        onClick = { 
                            if (SecurityHelper.isBiometricAvailable(context)) {
                                SecurityHelper.authenticate(
                                    activity = context as FragmentActivity,
                                    onSuccess = {
                                        viewModel.closeShift(
                                            cash.toDoubleOrNull() ?: 0.0, 
                                            mpesa.toDoubleOrNull() ?: 0.0, 
                                            till.toDoubleOrNull() ?: 0.0,
                                            meals.toDoubleOrNull() ?: 0.0,
                                            additionalExpenses.toList(),
                                            verificationMethod = "Biometric"
                                        )
                                        onClockOutComplete()
                                    },
                                    onError = { 
                                        showPasswordAuth = true 
                                    }
                                )
                            } else {
                                showPasswordAuth = true
                            }
                        }, 
                        modifier = Modifier.fillMaxWidth().height(64.dp), 
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFB71C1C))
                    ) {
                        Text("FINAL CONFIRM & CLOSE SHIFT", fontWeight = FontWeight.ExtraBold)
                    }
                    TextButton(onClick = { step = 1 }, modifier = Modifier.fillMaxWidth()) { Text("BACK TO EDIT") }
                }
            }
        }
    }

    if (showPasswordAuth) {
        var password by remember { mutableStateOf("") }
        var isVerifying by remember { mutableStateOf(false) }
        var error by remember { mutableStateOf<String?>(null) }

        AlertDialog(
            onDismissRequest = { if (!isVerifying) showPasswordAuth = false },
            title = { Text("Identity Verification", fontWeight = FontWeight.Black) },
            containerColor = Color(0xFF1E252D),
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Please enter your account password to authorize shift closure.", style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.7f))
                    StandardTextField(
                        value = password,
                        onValueChange = { password = it; error = null },
                        label = "Account Password",
                        visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                        leadingIcon = { Icon(Icons.Default.Lock, null, tint = Color.White.copy(alpha = 0.3f)) }
                    )
                    if (error != null) {
                        Text(error!!, color = Color.Red, style = MaterialTheme.typography.labelSmall)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        isVerifying = true
                        scope.launch {
                            val success = viewModel.verifyPassword(password)
                            if (success) {
                                viewModel.closeShift(
                                    cash.toDoubleOrNull() ?: 0.0, 
                                    mpesa.toDoubleOrNull() ?: 0.0, 
                                    till.toDoubleOrNull() ?: 0.0,
                                    meals.toDoubleOrNull() ?: 0.0,
                                    additionalExpenses.toList(),
                                    verificationMethod = "Password Override"
                                )
                                showPasswordAuth = false
                                onClockOutComplete()
                            } else {
                                error = "Invalid password. Access denied."
                                isVerifying = false
                            }
                        }
                    },
                    enabled = password.isNotEmpty() && !isVerifying,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    if (isVerifying) CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                    else Text("VERIFY & CLOSE", fontWeight = FontWeight.Black)
                }
            },
            dismissButton = {
                TextButton(onClick = { showPasswordAuth = false }, enabled = !isVerifying) { 
                    Text("CANCEL", color = Color.White.copy(alpha = 0.5f)) 
                }
            }
        )
    }
}

@Composable
fun SummaryRowItem(label: String, value: Double, isBold: Boolean = false, color: Color = Color.Unspecified) {
    Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.7f))
        Text(
            text = "KSh ${String.format(Locale.US, "%,.0f", value)}", 
            style = if (isBold) MaterialTheme.typography.bodyLarge else MaterialTheme.typography.bodyMedium,
            fontWeight = if (isBold) FontWeight.Bold else FontWeight.Normal, 
            color = if (color == Color.Unspecified) Color.White else color
        )
    }
}
