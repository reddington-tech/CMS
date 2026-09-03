package com.raymond.cms.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.raymond.cms.model.*
import com.raymond.cms.ui.components.*
import com.raymond.cms.util.SecurityHelper
import androidx.fragment.app.FragmentActivity
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun LoginScreen(
    viewModel: AuthViewModel
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()

    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F1318))
    ) {
        AnimatedVisibility(
            visible = visible,
            enter = fadeIn(tween(1000)) + slideInVertically(tween(1000)) { it / 4 }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .systemBarsPadding()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(24.dp, Alignment.CenterVertically)
            ) {
                // Professional Branding Section
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Surface(
                        modifier = Modifier.size(80.dp),
                        shape = RoundedCornerShape(22.dp),
                        color = MaterialTheme.colorScheme.primary,
                        tonalElevation = 12.dp,
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.2f))
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Default.DashboardCustomize, 
                                contentDescription = null, 
                                tint = Color.White, 
                                modifier = Modifier.size(36.dp)
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        text = "CMS EXECUTIVE", 
                        style = MaterialTheme.typography.headlineMedium, 
                        fontWeight = FontWeight.Black,
                        color = Color.White,
                        letterSpacing = (-1).sp
                    )
                    Text(
                        text = "Enterprise Asset Management", 
                        style = MaterialTheme.typography.labelMedium, 
                        color = Color.White.copy(alpha = 0.4f),
                        letterSpacing = 1.sp
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))

                // Input Container
                ExecutiveCard(
                    containerColor = Color(0xFF1E252D).copy(alpha = 0.5f),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = "SECURE ACCESS",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.primary,
                            letterSpacing = 1.5.sp
                        )

                        StandardTextField(
                            value = email,
                            onValueChange = { 
                                email = it
                                viewModel.clearError()
                            },
                            label = "Corporate Identity (Email)",
                            leadingIcon = { Icon(Icons.Default.PersonOutline, null, tint = Color.White.copy(alpha = 0.3f)) }
                        )

                        StandardTextField(
                            value = password,
                            onValueChange = { 
                                password = it
                                viewModel.clearError()
                            },
                            label = "Access Key (Password)",
                            leadingIcon = { Icon(Icons.Default.LockOpen, null, tint = Color.White.copy(alpha = 0.3f)) },
                            visualTransformation = PasswordVisualTransformation()
                        )
                    }
                }

                if (error != null) {
                    Surface(
                        color = Color(0xFFB71C1C).copy(alpha = 0.1f),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, Color(0xFFB71C1C).copy(alpha = 0.2f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.ErrorOutline, null, tint = Color(0xFFE57373), modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(12.dp))
                            Text(
                                text = error!!,
                                color = Color(0xFFE57373),
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }

                Button(
                    onClick = { viewModel.login(email, password) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp),
                    enabled = !isLoading && email.isNotEmpty() && password.isNotEmpty(),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        disabledContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                    )
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White, strokeWidth = 3.dp)
                    } else {
                        Text("ACCESS DASHBOARD", fontWeight = FontWeight.Black, letterSpacing = 1.sp)
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                // Footer
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Authorized Personnel Only", 
                        style = MaterialTheme.typography.labelSmall, 
                        color = Color.White.copy(alpha = 0.2f)
                    )
                    Text(
                        text = "Cyber Manager System v1.0", 
                        style = MaterialTheme.typography.labelSmall, 
                        color = Color.White.copy(alpha = 0.1f),
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun ExecutiveLoadingScreen() {
    var startAnimation by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0.8f,
        animationSpec = tween(durationMillis = 1000, easing = FastOutSlowInEasing)
    )
    val alpha by animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0f,
        animationSpec = tween(durationMillis = 1000)
    )

    LaunchedEffect(Unit) {
        startAnimation = true
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F1318)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.scale(scale).alpha(alpha)
        ) {
            Surface(
                modifier = Modifier.size(90.dp),
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Default.DashboardCustomize,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(40.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            Text(
                text = "ESTABLISHING SESSION",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Black,
                color = Color.White.copy(alpha = 0.4f),
                letterSpacing = 2.sp
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Text(
                text = "Welcome Back",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            
            Spacer(modifier = Modifier.height(48.dp))
            
            CircularProgressIndicator(
                modifier = Modifier.size(28.dp),
                color = MaterialTheme.colorScheme.primary,
                strokeWidth = 3.dp
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManageStaffScreen(
    viewModel: AuthViewModel,
    transactionViewModel: TransactionViewModel,
    onBack: () -> Unit
) {
    val allStaffShifts by viewModel.allStaffShifts.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val isMigrating by transactionViewModel.isMigrating.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    
    var showEditDialog by remember { mutableStateOf<UserModel?>(null) }
    var showAddDialog by remember { mutableStateOf(false) }
    var showCloseShiftDialog by remember { mutableStateOf<Shift?>(null) }

    LaunchedEffect(Unit) {
        viewModel.uiEvent.collectLatest { snackbarHostState.showSnackbar(it) }
        transactionViewModel.uiEvent.collectLatest { snackbarHostState.showSnackbar(it) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Executive Staff Control", fontWeight = FontWeight.ExtraBold, letterSpacing = (-0.5).sp) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "Back") }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = Color(0xFF0F1318),
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White,
                shape = RoundedCornerShape(16.dp),
                elevation = FloatingActionButtonDefaults.elevation(8.dp)
            ) {
                Icon(Icons.Default.PersonAdd, contentDescription = "Add Staff")
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                ActionGuideCard()
            }

            item {
                DashboardSectionHeader("Staff Registry")
            }

            if (allStaffShifts.isEmpty() && !isLoading) {
                item {
                    Box(Modifier.fillMaxWidth().padding(48.dp), contentAlignment = Alignment.Center) {
                        Text("No registered personnel found.", color = Color.White.copy(alpha = 0.4f), style = MaterialTheme.typography.bodyMedium)
                    }
                }
            } else {
                items(allStaffShifts) { (staff, activeShift) ->
                    StaffItem(
                        staff = staff,
                        activeShift = activeShift,
                        onEdit = { showEditDialog = staff },
                        onToggleStatus = { viewModel.toggleStaffStatus(staff.uid, !staff.isActive) },
                        onRemove = { viewModel.removeStaff(staff.uid, !staff.isRemoved) },
                        onForceCloseShift = { showCloseShiftDialog = activeShift }
                    )
                }
            }
        }
    }

    if (showAddDialog) {
        StaffDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { email, pass, name, idNo, phone ->
                viewModel.register(email, pass, UserRole.STAFF, name, idNo, phone)
                showAddDialog = false
            },
            isNew = true
        )
    }

    if (showEditDialog != null) {
        StaffDialog(
            user = showEditDialog!!,
            onDismiss = { showEditDialog = null },
            onConfirm = { email, password, name, idNo, phone ->
                viewModel.updateStaffCredentials(
                    showEditDialog!!.copy(
                        email = email,
                        name = name, 
                        idNumber = idNo, 
                        phoneNumber = phone
                    ),
                    newPassword = password
                )
                showEditDialog = null
            },
            isNew = false
        )
    }

    if (showCloseShiftDialog != null) {
        AdminCloseShiftDialog(
            staffName = showCloseShiftDialog!!.staffName,
            onDismiss = { showCloseShiftDialog = null },
            onConfirm = { cash, mpesa, till, reason ->
                viewModel.adminCloseShift(showCloseShiftDialog!!, cash, mpesa, till, reason)
                showCloseShiftDialog = null
            }
        )
    }
}

@Composable
fun ActionGuideCard() {
    ExecutiveCard(
        containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.1f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.2f))
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Info, null, tint = MaterialTheme.colorScheme.tertiary, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(8.dp))
                Text("EXECUTIVE GUIDE", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.tertiary)
            }
            Text("• Edit: Update personnel credentials and contact info.", style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.6f))
            Text("• Status: Suspend or restore system access.", style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.6f))
            Text("• History: All records are preserved even if staff is removed.", style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.6f))
        }
    }
}

@Composable
fun StaffItem(
    staff: UserModel,
    activeShift: Shift?,
    onEdit: () -> Unit,
    onToggleStatus: () -> Unit,
    onRemove: () -> Unit,
    onForceCloseShift: () -> Unit
) {
    val isOnDuty = activeShift != null && activeShift.status == ShiftStatus.ACTIVE
    
    ExecutiveCard(
        border = BorderStroke(1.dp, when {
            isOnDuty -> Color(0xFF4CAF50).copy(alpha = 0.2f)
            staff.isRemoved -> Color.Gray.copy(alpha = 0.1f)
            else -> Color.White.copy(alpha = 0.05f)
        }),
        containerColor = if (staff.isRemoved) Color(0xFF1E252D).copy(alpha = 0.5f) else Color(0xFF1E252D)
    ) {
        Column(
            modifier = Modifier.padding(16.dp).alpha(if (staff.isRemoved) 0.6f else 1f),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        text = staff.name.ifEmpty { "New Staff Member" },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "ID: ${staff.idNumber.ifEmpty { "N/A" }} • ${staff.phoneNumber.ifEmpty { "No Phone" }}",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.5f)
                    )
                }
                
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (staff.isRemoved) {
                        Surface(
                            color = Color.Gray.copy(alpha = 0.1f),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, Color.Gray.copy(alpha = 0.2f))
                        ) {
                            Text(
                                text = "REMOVED",
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Black,
                                color = Color.Gray
                            )
                        }
                    }
                    
                    Surface(
                        color = if (staff.isActive) Color(0xFF4CAF50).copy(alpha = 0.1f) else Color(0xFFF44336).copy(alpha = 0.1f),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, if (staff.isActive) Color(0xFF4CAF50).copy(alpha = 0.2f) else Color(0xFFF44336).copy(alpha = 0.2f))
                    ) {
                        Text(
                            text = if (staff.isActive) "ACTIVE" else "DISABLED",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Black,
                            color = if (staff.isActive) Color(0xFF4CAF50) else Color(0xFFF44336)
                        )
                    }
                }
            }

            if (isOnDuty) {
                Surface(
                    color = Color(0xFF4CAF50).copy(alpha = 0.05f),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, Color(0xFF4CAF50).copy(alpha = 0.1f))
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(Modifier.size(8.dp).background(Color(0xFF4CAF50), RoundedCornerShape(4.dp)))
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text("CURRENTLY ON DUTY", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Black, color = Color(0xFF4CAF50))
                            Text(
                                "Clocked in at ${SimpleDateFormat("hh:mm a", Locale.US).format(Date(activeShift!!.clockInTime))}",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.6f)
                            )
                        }
                        IconButton(onClick = onForceCloseShift) {
                            Icon(Icons.AutoMirrored.Filled.Logout, "Close Shift", tint = Color(0xFFE57373), modifier = Modifier.size(20.dp))
                        }
                    }
                }
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = onEdit,
                    modifier = Modifier.weight(1f).height(38.dp),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(0.dp),
                    border = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.1f)),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White.copy(alpha = 0.7f))
                ) {
                    Icon(Icons.Default.Edit, null, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("EDIT", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
                
                if (staff.isActive) {
                    OutlinedButton(
                        onClick = onToggleStatus,
                        modifier = Modifier.weight(1f).height(38.dp),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(0.dp),
                        border = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.1f)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White.copy(alpha = 0.7f))
                    ) {
                        Icon(Icons.Default.Block, null, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("SUSPEND", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                } else {
                    Button(
                        onClick = onToggleStatus,
                        modifier = Modifier.weight(1f).height(38.dp),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(0.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
                    ) {
                        Icon(Icons.Default.CheckCircle, null, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("RE-ACTIVATE", fontSize = 10.sp, fontWeight = FontWeight.Black)
                    }
                }

                OutlinedButton(
                    onClick = onRemove,
                    modifier = Modifier.weight(1f).height(38.dp),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(0.dp),
                    border = BorderStroke(0.5.dp, if (staff.isRemoved) Color(0xFF4CAF50).copy(alpha = 0.2f) else Color(0xFFE57373).copy(alpha = 0.2f)),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = if (staff.isRemoved) Color(0xFF4CAF50) else Color(0xFFE57373))
                ) {
                    Icon(if (staff.isRemoved) Icons.Default.RestoreFromTrash else Icons.Default.PersonRemove, null, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(if (staff.isRemoved) "RESTORE" else "REMOVE", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }
            
            if (isOnDuty && activeShift != null) {
                HorizontalDivider(color = Color.White.copy(alpha = 0.05f))
                DetailRowItem("Shift Live Collection", "KSh ${String.format(Locale.US, "%,.0f", activeShift.totalClosing)}", color = Color(0xFF4CAF50))
            }
        }
    }
}

@Composable
fun AdminCloseShiftDialog(
    staffName: String,
    onDismiss: () -> Unit,
    onConfirm: (Double, Double, Double, String) -> Unit
) {
    var cash by remember { mutableStateOf("") }
    var mpesa by remember { mutableStateOf("") }
    var till by remember { mutableStateOf("") }
    var reason by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Emergency Shift Termination", fontWeight = FontWeight.ExtraBold, color = Color(0xFFE57373)) },
        containerColor = Color(0xFF1E252D),
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(
                    "You are closing the shift for $staffName. Provide the final estimated balances for reconciliation.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.7f)
                )
                StandardTextField(value = cash, onValueChange = { cash = it }, label = "Final Cash Balance", keyboardType = KeyboardType.Number)
                StandardTextField(value = mpesa, onValueChange = { mpesa = it }, label = "Final M-Pesa Balance", keyboardType = KeyboardType.Number)
                StandardTextField(value = till, onValueChange = { till = it }, label = "Final Till Balance", keyboardType = KeyboardType.Number)
                StandardTextField(value = reason, onValueChange = { reason = it }, label = "Reason for Manual Closure")
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm(
                        cash.toDoubleOrNull() ?: 0.0,
                        mpesa.toDoubleOrNull() ?: 0.0,
                        till.toDoubleOrNull() ?: 0.0,
                        reason
                    )
                },
                enabled = reason.isNotEmpty(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE57373))
            ) {
                Text("TERMINATE SHIFT", fontWeight = FontWeight.Black)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("CANCEL", color = Color.White.copy(alpha = 0.5f)) }
        }
    )
}

@Composable
fun StaffDialog(
    user: UserModel = UserModel(),
    onDismiss: () -> Unit,
    onConfirm: (String, String, String, String, String) -> Unit,
    isNew: Boolean
) {
    var email by remember { mutableStateOf(user.email) }
    var password by remember { mutableStateOf("") }
    var name by remember { mutableStateOf(user.name) }
    var idNo by remember { mutableStateOf(user.idNumber) }
    var phone by remember { mutableStateOf(user.phoneNumber) }
    
    var showPasswordFields by remember { mutableStateOf(isNew) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isNew) "Register Personnel" else "Update Credentials", fontWeight = FontWeight.ExtraBold, letterSpacing = (-0.5).sp) },
        containerColor = Color(0xFF1E252D),
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                StandardTextField(
                    value = email, 
                    onValueChange = { email = it }, 
                    label = "Corporate Email Address",
                    leadingIcon = { Icon(Icons.Default.Email, null, tint = Color.White.copy(alpha = 0.3f)) }
                )
                
                if (!isNew) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showPasswordFields = !showPasswordFields },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = showPasswordFields,
                            onCheckedChange = { showPasswordFields = it },
                            colors = CheckboxDefaults.colors(checkedColor = MaterialTheme.colorScheme.primary)
                        )
                        Text(
                            text = "Reset Access Key (Password)", 
                            style = MaterialTheme.typography.bodyMedium, 
                            color = Color.White,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                if (showPasswordFields) {
                    StandardTextField(
                        value = password, 
                        onValueChange = { password = it }, 
                        label = if (isNew) "Initial Access Key (Password)" else "New Access Key (Password)", 
                        visualTransformation = PasswordVisualTransformation(),
                        leadingIcon = { Icon(Icons.Default.Lock, null, tint = Color.White.copy(alpha = 0.3f)) }
                    )
                }
                
                StandardTextField(value = name, onValueChange = { name = it }, label = "Legal Full Name")
                StandardTextField(value = idNo, onValueChange = { idNo = it }, label = "National ID Number", keyboardType = KeyboardType.Number)
                StandardTextField(value = phone, onValueChange = { phone = it }, label = "Official Phone Contact", keyboardType = KeyboardType.Phone)
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(email, password, name, idNo, phone) },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().height(50.dp)
            ) {
                Text(if (isNew) "CONFIRM REGISTRATION" else "UPDATE PERSONNEL DATA", fontWeight = FontWeight.Black)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, modifier = Modifier.padding(bottom = 8.dp)) { 
                Text("CANCEL", color = Color.White.copy(alpha = 0.5f)) 
            }
        }
    )
}

@Composable
fun StartupShieldOverlay(
    viewModel: AuthViewModel
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var showPasswordFallback by remember { mutableStateOf(false) }
    var password by remember { mutableStateOf("") }
    var isVerifying by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        if (SecurityHelper.isBiometricAvailable(context)) {
            SecurityHelper.authenticate(
                activity = context as FragmentActivity,
                title = "System Unlock",
                subtitle = "Authenticate to access Cyber Manager System",
                onSuccess = { viewModel.setLocked(false) },
                onError = { showPasswordFallback = true }
            )
        } else {
            showPasswordFallback = true
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F1318)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(32.dp)
        ) {
            Surface(
                modifier = Modifier.size(80.dp),
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Default.Lock,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }

            Spacer(Modifier.height(24.dp))
            Text(
                "SYSTEM SECURE",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Black,
                color = Color.White.copy(alpha = 0.4f),
                letterSpacing = 2.sp
            )
            Text(
                "Cyber Manager System",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            Spacer(Modifier.height(48.dp))

            if (showPasswordFallback) {
                ExecutiveCard(
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
                ) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Identity Verification Required", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.6f))
                        StandardTextField(
                            value = password,
                            onValueChange = { password = it; error = null },
                            label = "Account Password",
                            visualTransformation = PasswordVisualTransformation(),
                            leadingIcon = { Icon(Icons.Default.VpnKey, null, tint = Color.White.copy(alpha = 0.3f)) }
                        )
                        if (error != null) {
                            Text(error!!, color = Color.Red, style = MaterialTheme.typography.labelSmall)
                        }
                        Button(
                            onClick = {
                                isVerifying = true
                                scope.launch {
                                    val success = viewModel.verifyPassword(password)
                                    if (success) {
                                        viewModel.setLocked(false)
                                    } else {
                                        error = "Invalid password. Access denied."
                                        isVerifying = false
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth().height(50.dp),
                            enabled = password.isNotEmpty() && !isVerifying,
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            if (isVerifying) CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                            else Text("UNLOCK SYSTEM", fontWeight = FontWeight.Black)
                        }
                    }
                }
            } else {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.height(16.dp))
                TextButton(onClick = { showPasswordFallback = true }) {
                    Text("USE PASSWORD INSTEAD", color = MaterialTheme.colorScheme.primary)
                }
            }
        }

        // Branding Footer
        Text(
            text = "Cyber Manager System v1.0",
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 32.dp),
            style = MaterialTheme.typography.labelSmall,
            color = Color.White.copy(alpha = 0.1f)
        )
    }
}
