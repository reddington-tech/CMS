package com.raymond.cms.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.raymond.cms.model.ApprovalRequest
import com.raymond.cms.model.RecordType
import com.raymond.cms.ui.components.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminApprovalScreen(
    authViewModel: AuthViewModel,
    financialViewModel: FinancialViewModel,
    onBack: () -> Unit
) {
    val pendingRequests by financialViewModel.pendingRequests.collectAsState()
    val admin by authViewModel.currentUser.collectAsState()
    
    var showRejectDialog by remember { mutableStateOf<ApprovalRequest?>(null) }

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(Unit) {
        financialViewModel.uiEvent.collect { snackbarHostState.showSnackbar(it) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Executive Compliance Hub", fontWeight = FontWeight.Black, letterSpacing = (-0.5).sp) },
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
        if (pendingRequests.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.FactCheck, null, modifier = Modifier.size(64.dp), tint = Color.White.copy(alpha = 0.05f))
                    Spacer(Modifier.height(16.dp))
                    Text("System Fully Compliant", color = Color.White.copy(alpha = 0.2f), style = MaterialTheme.typography.bodyLarge)
                    Text("No pending administrative actions found.", color = Color.White.copy(alpha = 0.1f), style = MaterialTheme.typography.labelSmall)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    DashboardSectionHeader("Review Required")
                }

                items(pendingRequests) { request ->
                    ApprovalRequestItem(
                        request = request,
                        onApprove = { financialViewModel.approveRequest(admin!!, request) },
                        onReject = { showRejectDialog = request }
                    )
                }
            }
        }
    }

    if (showRejectDialog != null) {
        var rejectReason by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showRejectDialog = null },
            title = { Text("Administrative Rejection", fontWeight = FontWeight.ExtraBold, color = Color(0xFFE57373)) },
            containerColor = Color(0xFF1E252D),
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        "Please provide a formal justification for rejecting this request. This action will be audited.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.7f)
                    )
                    StandardTextField(
                        value = rejectReason,
                        onValueChange = { rejectReason = it },
                        label = "Justification for Rejection",
                        leadingIcon = { Icon(Icons.Default.RateReview, null, tint = Color.White.copy(alpha = 0.3f)) }
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        financialViewModel.rejectRequest(admin!!, showRejectDialog!!, rejectReason)
                        showRejectDialog = null
                    },
                    enabled = rejectReason.isNotEmpty(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE57373))
                ) {
                    Text("CONFIRM REJECTION", fontWeight = FontWeight.Black)
                }
            },
            dismissButton = {
                TextButton(onClick = { showRejectDialog = null }) { 
                    Text("CANCEL", color = Color.White.copy(alpha = 0.5f)) 
                }
            }
        )
    }
}

@Composable
fun ApprovalRequestItem(
    request: ApprovalRequest,
    onApprove: () -> Unit,
    onReject: () -> Unit
) {
    ExecutiveCard(
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                Surface(
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
                ) {
                    Text(
                        text = request.recordType.name.replace("_", " "), 
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall, 
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp
                    )
                }
                Text(
                    text = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()).format(request.requestedAt ?: Date()),
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.4f)
                )
            }
            
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        color = Color.White.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.size(24.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Person, null, modifier = Modifier.size(14.dp), tint = Color.White)
                        }
                    }
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "Requested by ${request.requestedByName}", 
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold, 
                        color = Color.White
                    )
                }
                Text(
                    text = "Justification: ${request.reason}", 
                    style = MaterialTheme.typography.bodyMedium, 
                    color = Color.White.copy(alpha = 0.7f),
                    modifier = Modifier.padding(top = 4.dp, start = 32.dp)
                )
            }
            
            HorizontalDivider(color = Color.White.copy(alpha = 0.05f))
            
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Column(Modifier.weight(1f)) {
                    Text("ORIGINAL RECORD", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.3f), fontWeight = FontWeight.Black, letterSpacing = 1.sp)
                    Spacer(Modifier.height(8.dp))
                    request.originalData.forEach { (key, value) ->
                        val isChanged = request.proposedData[key]?.toString() != value?.toString()
                        Text(
                            text = "${key.capitalize()}: $value", 
                            style = MaterialTheme.typography.bodySmall, 
                            color = if (isChanged) Color.White.copy(alpha = 0.4f) else Color.White
                        )
                    }
                }
                
                Box(Modifier.align(Alignment.CenterVertically)) {
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, null, tint = Color.White.copy(alpha = 0.1f), modifier = Modifier.size(20.dp))
                }
                
                Column(Modifier.weight(1f)) {
                    Text("PROPOSED NEW", style = MaterialTheme.typography.labelSmall, color = Color(0xFF4CAF50), fontWeight = FontWeight.Black, letterSpacing = 1.sp)
                    Spacer(Modifier.height(8.dp))
                    request.proposedData.forEach { (key, value) ->
                        val isChanged = request.originalData[key]?.toString() != value?.toString()
                        val isDateChange = key == "date" && isChanged
                        
                        if (isDateChange) {
                            Surface(
                                color = Color(0xFFF44336).copy(alpha = 0.1f),
                                shape = RoundedCornerShape(6.dp),
                                border = BorderStroke(1.dp, Color(0xFFF44336).copy(alpha = 0.2f)),
                                modifier = Modifier.padding(vertical = 2.dp)
                            ) {
                                Text(
                                    text = "DATE CHANGE: $value", 
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    style = MaterialTheme.typography.labelSmall, 
                                    color = Color(0xFFE57373),
                                    fontWeight = FontWeight.Black
                                )
                            }
                        } else {
                            Text(
                                text = "${key.capitalize()}: $value", 
                                style = MaterialTheme.typography.bodySmall, 
                                color = if (isChanged) Color(0xFF4CAF50) else Color.White,
                                fontWeight = if (isChanged) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
            }
            
            if (request.status == com.raymond.cms.model.RequestStatus.PENDING) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(
                        onClick = onReject,
                        modifier = Modifier.weight(1f).height(44.dp),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(0.5.dp, Color(0xFFE57373).copy(alpha = 0.3f)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFE57373))
                    ) {
                        Icon(Icons.Default.Close, null, modifier = Modifier.size(16.dp)); Spacer(Modifier.width(8.dp)); Text("REJECT", fontSize = 11.sp, fontWeight = FontWeight.Black)
                    }

                    Button(
                        onClick = onApprove,
                        modifier = Modifier.weight(1.5f).height(44.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(Icons.Default.Check, null, modifier = Modifier.size(16.dp)); Spacer(Modifier.width(8.dp)); Text("APPROVE COMPLIANCE", fontSize = 11.sp, fontWeight = FontWeight.Black)
                    }
                }
            } else {
                Surface(
                    color = (if (request.status == com.raymond.cms.model.RequestStatus.APPROVED) Color(0xFF4CAF50) else Color(0xFFF44336)).copy(alpha = 0.1f),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, (if (request.status == com.raymond.cms.model.RequestStatus.APPROVED) Color(0xFF4CAF50) else Color(0xFFF44336)).copy(alpha = 0.2f))
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            if (request.status == com.raymond.cms.model.RequestStatus.APPROVED) Icons.Default.CheckCircle else Icons.Default.Cancel,
                            null,
                            tint = if (request.status == com.raymond.cms.model.RequestStatus.APPROVED) Color(0xFF4CAF50) else Color(0xFFF44336),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "REQUEST ${request.status.name}",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Black,
                                color = if (request.status == com.raymond.cms.model.RequestStatus.APPROVED) Color(0xFF4CAF50) else Color(0xFFF44336)
                            )
                            if (request.adminComment.isNotEmpty()) {
                                Text(
                                    text = "Comment: ${request.adminComment}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.White.copy(alpha = 0.6f)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
