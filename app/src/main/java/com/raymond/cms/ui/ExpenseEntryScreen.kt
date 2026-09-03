package com.raymond.cms.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.raymond.cms.model.Expense
import com.raymond.cms.model.PaymentMethod
import com.raymond.cms.ui.components.*
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpenseEntryScreen(
    authViewModel: AuthViewModel,
    financialViewModel: FinancialViewModel,
    onBack: () -> Unit
) {
    val user by authViewModel.currentUser.collectAsState()
    val activeShift by authViewModel.activeShift.collectAsState()

    val isAdmin = user?.role == com.raymond.cms.model.UserRole.ADMIN
    val isShiftActive = activeShift != null && activeShift?.status == com.raymond.cms.model.ShiftStatus.ACTIVE

    if (!isAdmin && !isShiftActive) {
        Box(Modifier.fillMaxSize().background(Color(0xFF0F1318)).padding(24.dp), contentAlignment = Alignment.Center) {
            ExecutiveCard(
                border = BorderStroke(1.dp, Color(0xFFF44336).copy(alpha = 0.2f)),
                containerColor = Color(0xFF1E252D)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(24.dp)) {
                    Surface(
                        modifier = Modifier.size(64.dp),
                        shape = RoundedCornerShape(16.dp),
                        color = Color(0xFFF44336).copy(alpha = 0.1f)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Lock, null, modifier = Modifier.size(32.dp), tint = Color(0xFFF44336))
                        }
                    }
                    Spacer(Modifier.height(24.dp))
                    Text(
                        "SHIFT CONTROL LOCK", 
                        style = MaterialTheme.typography.labelSmall, 
                        fontWeight = FontWeight.Black, 
                        color = Color(0xFFF44336),
                        letterSpacing = 1.sp
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Operational restriction in effect. You cannot record expenses without an active shift.",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        color = Color.White.copy(alpha = 0.7f)
                    )
                    Spacer(Modifier.height(32.dp))
                    Button(
                        onClick = onBack,
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("BACK TO DASHBOARD", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
        return
    }

    var category by remember { mutableStateOf(Expense.CATEGORIES.first()) }
    var amount by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var paymentMethod by remember { mutableStateOf(PaymentMethod.CASH) }
    
    var expandedCategory by remember { mutableStateOf(false) }
    var expandedPayment by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(Unit) {
        financialViewModel.uiEvent.collect { snackbarHostState.showSnackbar(it) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Log Operating Expense", fontWeight = FontWeight.Black, letterSpacing = (-0.5).sp) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = Color(0xFF0F1318)
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            DashboardSectionHeader("Expense Entry")

            ExecutiveCard(
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    ExposedDropdownMenuBox(
                        expanded = expandedCategory,
                        onExpandedChange = { expandedCategory = !expandedCategory }
                    ) {
                        StandardTextField(
                            value = category,
                            onValueChange = {},
                            readOnly = true,
                            label = "Expense Category",
                            modifier = Modifier.menuAnchor(),
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedCategory) }
                        )
                        ExposedDropdownMenu(
                            expanded = expandedCategory,
                            onDismissRequest = { expandedCategory = false },
                            modifier = Modifier.background(Color(0xFF1E252D))
                        ) {
                            Expense.CATEGORIES.forEach { cat ->
                                DropdownMenuItem(
                                    text = { Text(cat, fontWeight = FontWeight.Bold) },
                                    onClick = {
                                        category = cat
                                        expandedCategory = false
                                    }
                                )
                            }
                        }
                    }

                    StandardTextField(
                        value = amount,
                        onValueChange = { if (it.isEmpty() || it.toDoubleOrNull() != null) amount = it },
                        label = "Amount (KSh)",
                        keyboardType = KeyboardType.Number,
                        leadingIcon = { Icon(Icons.Default.Payments, null, tint = Color.White.copy(alpha = 0.3f)) }
                    )

                    StandardTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = "Description / Justification",
                        leadingIcon = { Icon(Icons.Default.Description, null, tint = Color.White.copy(alpha = 0.3f)) }
                    )

                    ExposedDropdownMenuBox(
                        expanded = expandedPayment,
                        onExpandedChange = { expandedPayment = !expandedPayment }
                    ) {
                        StandardTextField(
                            value = paymentMethod.name,
                            onValueChange = {},
                            readOnly = true,
                            label = "Source of Funds (Payment Method)",
                            modifier = Modifier.menuAnchor(),
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedPayment) }
                        )
                        ExposedDropdownMenu(
                            expanded = expandedPayment,
                            onDismissRequest = { expandedPayment = false },
                            modifier = Modifier.background(Color(0xFF1E252D))
                        ) {
                            PaymentMethod.values().forEach { method ->
                                DropdownMenuItem(
                                    text = { Text(method.name, fontWeight = FontWeight.Bold) },
                                    onClick = {
                                        paymentMethod = method
                                        expandedPayment = false
                                    }
                                )
                            }
                        }
                    }
                }
            }

            Button(
                onClick = {
                    val u = user ?: return@Button
                    val sId = activeShift?.id ?: "ADMIN_SYSTEM_ENTRY"
                    financialViewModel.addExpense(
                        user = u,
                        shiftId = sId,
                        category = category,
                        amount = amount.toDoubleOrNull() ?: 0.0,
                        description = description,
                        paymentMethod = paymentMethod,
                        onComplete = onBack
                    )
                },
                modifier = Modifier.fillMaxWidth().height(60.dp),
                enabled = amount.isNotEmpty() && description.isNotEmpty() && (isAdmin || isShiftActive),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE57373))
            ) {
                Text("VALIDATE & RECORD EXPENSE", fontWeight = FontWeight.Black, letterSpacing = 1.sp)
            }
        }
    }
}
