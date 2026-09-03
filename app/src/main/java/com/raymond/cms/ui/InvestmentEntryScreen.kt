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
import com.raymond.cms.model.InvestmentType
import com.raymond.cms.ui.components.*
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InvestmentEntryScreen(
    authViewModel: AuthViewModel,
    financialViewModel: FinancialViewModel,
    onBack: () -> Unit
) {
    val user by authViewModel.currentUser.collectAsState()
    val activeShift by authViewModel.activeShift.collectAsState()

    var type by remember { mutableStateOf(InvestmentType.INVESTMENT) }
    var amount by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    
    var expandedType by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(Unit) {
        financialViewModel.uiEvent.collect { snackbarHostState.showSnackbar(it) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Capital & Asset Transfer", fontWeight = FontWeight.Black, letterSpacing = (-0.5).sp) },
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
            DashboardSectionHeader("Transaction Entry")

            ExecutiveCard(
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    ExposedDropdownMenuBox(
                        expanded = expandedType,
                        onExpandedChange = { expandedType = !expandedType }
                    ) {
                        StandardTextField(
                            value = type.name.replace("_", " "),
                            onValueChange = {},
                            readOnly = true,
                            label = "Asset Transfer Type",
                            modifier = Modifier.menuAnchor(),
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedType) }
                        )
                        ExposedDropdownMenu(
                            expanded = expandedType,
                            onDismissRequest = { expandedType = false },
                            modifier = Modifier.background(Color(0xFF1E252D))
                        ) {
                            InvestmentType.values().forEach { t ->
                                DropdownMenuItem(
                                    text = { Text(t.name.replace("_", " "), fontWeight = FontWeight.Bold) },
                                    onClick = {
                                        type = t
                                        expandedType = false
                                    }
                                )
                            }
                        }
                    }

                    StandardTextField(
                        value = amount,
                        onValueChange = { if (it.isEmpty() || it.toDoubleOrNull() != null) amount = it },
                        label = "Transfer Amount (KSh)",
                        keyboardType = KeyboardType.Number,
                        leadingIcon = { Icon(Icons.Default.AccountBalance, null, tint = Color.White.copy(alpha = 0.3f)) }
                    )

                    StandardTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = "Justification / Reference",
                        leadingIcon = { Icon(Icons.Default.Description, null, tint = Color.White.copy(alpha = 0.3f)) }
                    )
                }
            }

            Button(
                onClick = {
                    val u = user ?: return@Button
                    val sId = activeShift?.id ?: return@Button
                    financialViewModel.addInvestment(
                        user = u,
                        shiftId = sId,
                        amount = amount.toDoubleOrNull() ?: 0.0,
                        type = type,
                        description = description,
                        onComplete = onBack
                    )
                },
                modifier = Modifier.fillMaxWidth().height(60.dp),
                enabled = amount.isNotEmpty() && description.isNotEmpty() && activeShift != null,
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text("VALIDATE & CONFIRM TRANSFER", fontWeight = FontWeight.Black, letterSpacing = 1.sp)
            }
        }
    }
}
