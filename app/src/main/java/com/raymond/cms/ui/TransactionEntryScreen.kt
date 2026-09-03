package com.raymond.cms.ui

import androidx.compose.animation.*
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.raymond.cms.model.*
import com.raymond.cms.ui.components.ExecutiveCard
import com.raymond.cms.ui.components.StandardTextField
import kotlinx.coroutines.launch
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionEntryScreen(
    authViewModel: AuthViewModel,
    productViewModel: ProductViewModel,
    financialViewModel: FinancialViewModel,
    onBack: () -> Unit
) {
    val services by authViewModel.services.collectAsState()
    val products by productViewModel.products.collectAsState()
    val user by authViewModel.currentUser.collectAsState()
    val activeShift by authViewModel.activeShift.collectAsState()

    val isAdmin = user?.role == UserRole.ADMIN
    val isShiftActive = activeShift != null && activeShift?.status == ShiftStatus.ACTIVE

    if (!isAdmin && !isShiftActive) {
        Box(Modifier.fillMaxSize().background(Color(0xFF0F1318)), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(24.dp)) {
                Icon(Icons.Default.Lock, null, modifier = Modifier.size(64.dp), tint = Color.Red)
                Spacer(Modifier.height(16.dp))
                Text("Shift Closed", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = Color.White)
                Text(
                    "You must be on an active shift to record sales.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
                Spacer(Modifier.height(24.dp))
                Button(onClick = onBack) { Text("BACK TO DASHBOARD") }
            }
        }
        return
    }

    val cartItems = remember { mutableStateListOf<TransactionItem>() }
    var selectedTab by remember { mutableIntStateOf(0) } // 0 = Services, 1 = Products
    var paymentMethod by remember { mutableStateOf(PaymentMethod.CASH) }
    var notes by remember { mutableStateOf("") }
    
    var showSuccess by remember { mutableStateOf(false) }
    val totalAmount = cartItems.sumOf { it.totalAmount }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        financialViewModel.uiEvent.collect { snackbarHostState.showSnackbar(it) }
    }

    if (showSuccess) {
        Box(Modifier.fillMaxSize().background(Color(0xFF0F1318)), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(32.dp)) {
                Surface(
                    color = Color(0xFF4CAF50).copy(alpha = 0.1f),
                    shape = RoundedCornerShape(32.dp),
                    modifier = Modifier.size(120.dp),
                    border = BorderStroke(2.dp, Color(0xFF4CAF50).copy(alpha = 0.2f))
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.CheckCircle, null, modifier = Modifier.size(64.dp), tint = Color(0xFF4CAF50))
                    }
                }
                Spacer(Modifier.height(32.dp))
                Text("TRANSACTION VALIDATED", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black, color = Color.White, letterSpacing = 1.sp)
                Spacer(Modifier.height(8.dp))
                Text("Sale record has been securely archived.", style = MaterialTheme.typography.bodyLarge, color = Color.White.copy(alpha = 0.6f))
                
                Spacer(Modifier.height(64.dp))
                
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    OutlinedButton(
                        onClick = { 
                            cartItems.clear()
                            showSuccess = false 
                        },
                        modifier = Modifier.weight(1f).height(56.dp),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text("NEW SALE", fontWeight = FontWeight.Bold)
                    }
                    
                    Button(
                        onClick = onBack,
                        modifier = Modifier.weight(1.2f).height(56.dp),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text("BACK TO DASHBOARD", fontWeight = FontWeight.Black)
                    }
                }
            }
        }
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Executive POS Terminal", fontWeight = FontWeight.Black, letterSpacing = (-0.5).sp) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        containerColor = Color(0xFF0F1318),
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            // Tab Selection
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
                    text = { Text("SERVICES", style = MaterialTheme.typography.labelMedium, fontWeight = if (selectedTab == 0) FontWeight.Black else FontWeight.Medium) }
                )
                Tab(
                    selected = selectedTab == 1, 
                    onClick = { selectedTab = 1 }, 
                    text = { Text("PRODUCTS", style = MaterialTheme.typography.labelMedium, fontWeight = if (selectedTab == 1) FontWeight.Black else FontWeight.Medium) }
                )
            }

            Row(modifier = Modifier.weight(1f)) {
                // Left Side: Selection List
                Box(modifier = Modifier.weight(1.3f).padding(16.dp)) {
                    if (selectedTab == 0) {
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            items(services) { service ->
                                ServiceItem(service) { qty, price ->
                                    cartItems.add(TransactionItem(
                                        type = "SERVICE",
                                        id = service.id,
                                        name = service.name,
                                        quantity = qty,
                                        unitPrice = price,
                                        totalAmount = qty * price
                                    ))
                                }
                            }
                        }
                    } else {
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            items(products) { product ->
                                ProductSelectionItem(
                                    product = product,
                                    currentInCart = cartItems.filter { it.id == product.id }.sumOf { it.quantity }
                                ) { qty, price ->
                                    val existingInCart = cartItems.filter { it.id == product.id }.sumOf { it.quantity }
                                    if (existingInCart + qty > product.currentStock) {
                                        scope.launch {
                                            snackbarHostState.showSnackbar("Insufficient Inventory: Only ${product.currentStock.toInt()} units available.")
                                        }
                                        return@ProductSelectionItem
                                    }
                                    
                                    cartItems.add(TransactionItem(
                                        type = "PRODUCT",
                                        id = product.id,
                                        name = product.name,
                                        quantity = qty,
                                        unitPrice = price,
                                        totalAmount = qty * price
                                    ))
                                }
                            }
                        }
                    }
                }

                // Right Side: Cart Summary
                VerticalDivider(color = Color.White.copy(alpha = 0.05f))
                Column(modifier = Modifier.weight(1f).padding(16.dp)) {
                    Text("TERMINAL BASKET", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.4f), fontWeight = FontWeight.Black, letterSpacing = 1.sp)
                    Spacer(Modifier.height(16.dp))
                    
                    if (cartItems.isEmpty()) {
                        Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.ShoppingCart, null, tint = Color.White.copy(alpha = 0.05f), modifier = Modifier.size(48.dp))
                                Spacer(Modifier.height(8.dp))
                                Text("Basket is empty", color = Color.White.copy(alpha = 0.2f), fontSize = 12.sp)
                            }
                        }
                    } else {
                        LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            items(cartItems) { item ->
                                CartItemView(item) { cartItems.remove(item) }
                            }
                        }
                    }

                    HorizontalDivider(Modifier.padding(vertical = 16.dp), color = Color.White.copy(alpha = 0.05f))
                    
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.Bottom) {
                            Text("TOTAL", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Black, color = Color.White.copy(alpha = 0.4f))
                            Text(
                                text = "KSH ${String.format(Locale.US, "%,.0f", totalAmount)}", 
                                style = MaterialTheme.typography.headlineMedium, 
                                fontWeight = FontWeight.Black, 
                                color = Color(0xFF4CAF50)
                            )
                        }
                        
                        var expandedPayment by remember { mutableStateOf(false) }
                        ExposedDropdownMenuBox(
                            expanded = expandedPayment,
                            onExpandedChange = { expandedPayment = !expandedPayment }
                        ) {
                            OutlinedTextField(
                                value = paymentMethod.name,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Payment Method", style = MaterialTheme.typography.labelSmall) },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedPayment) },
                                modifier = Modifier.fillMaxWidth().menuAnchor(),
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    unfocusedContainerColor = Color.White.copy(alpha = 0.05f),
                                    focusedContainerColor = Color.White.copy(alpha = 0.05f)
                                )
                            )
                            ExposedDropdownMenu(
                                expanded = expandedPayment, 
                                onDismissRequest = { expandedPayment = false },
                                modifier = Modifier.background(Color(0xFF1E252D))
                            ) {
                                PaymentMethod.values().forEach { method ->
                                    DropdownMenuItem(
                                        text = { Text(method.name, fontWeight = FontWeight.Bold) }, 
                                        onClick = { paymentMethod = method; expandedPayment = false }
                                    )
                                }
                            }
                        }

                        Button(
                            onClick = {
                                val u = user ?: return@Button
                                val sId = activeShift?.id ?: "ADMIN_SYSTEM_ENTRY"
                                financialViewModel.addMultiItemTransaction(
                                    user = u,
                                    shiftId = sId,
                                    items = cartItems.toList(),
                                    paymentMethod = paymentMethod,
                                    notes = notes,
                                    onComplete = { showSuccess = true }
                                )
                            },
                            modifier = Modifier.fillMaxWidth().height(56.dp),
                            enabled = cartItems.isNotEmpty(),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                disabledContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                            )
                        ) {
                            Text("VALIDATE & COMPLETE", fontWeight = FontWeight.Black, letterSpacing = 1.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ServiceItem(service: Service, onAdd: (Int, Double) -> Unit) {
    var qty by remember { mutableStateOf("1") }
    var price by remember { mutableStateOf(service.price.toString()) }
    
    ExecutiveCard(
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.size(32.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Build, null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                    }
                }
                Spacer(Modifier.width(12.dp))
                Text(service.name, fontWeight = FontWeight.Bold, color = Color.White, style = MaterialTheme.typography.bodyMedium)
            }
            
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StandardTextField(value = qty, onValueChange = { qty = it }, label = "Qty", modifier = Modifier.weight(1f), keyboardType = KeyboardType.Number)
                StandardTextField(value = price, onValueChange = { price = it }, label = "Price", modifier = Modifier.weight(1.5f), keyboardType = KeyboardType.Number)
            }
            Button(
                onClick = { onAdd(qty.toIntOrNull() ?: 1, price.toDoubleOrNull() ?: service.price) }, 
                modifier = Modifier.fillMaxWidth().height(40.dp), 
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f), contentColor = MaterialTheme.colorScheme.primary)
            ) {
                Text("ADD TO BASKET", fontSize = 11.sp, fontWeight = FontWeight.Black)
            }
        }
    }
}

@Composable
fun ProductSelectionItem(
    product: Product, 
    currentInCart: Int,
    onAdd: (Int, Double) -> Unit
) {
    var qty by remember { mutableStateOf("1") }
    var price by remember { mutableStateOf(product.sellingPrice.toString()) }
    val effectiveStock = product.currentStock.toInt() - currentInCart
    val isLowStock = effectiveStock <= 5
    val isOutOfStock = effectiveStock <= 0
    
    ExecutiveCard(
        containerColor = if (isOutOfStock) Color.White.copy(alpha = 0.02f) else Color(0xFF1E252D).copy(alpha = 0.6f),
        border = BorderStroke(1.dp, if (isOutOfStock) Color.Red.copy(alpha = 0.2f) else if (isLowStock) Color(0xFFF44336).copy(alpha = 0.2f) else Color.White.copy(alpha = 0.05f))
    ) {
        Column(Modifier.padding(12.dp).alpha(if (isOutOfStock) 0.5f else 1f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(product.name, fontWeight = FontWeight.Bold, color = if (isOutOfStock) Color.Gray else Color(0xFF2196F3), style = MaterialTheme.typography.bodyMedium)
                    if (isOutOfStock) {
                        Text("OUT OF STOCK", style = MaterialTheme.typography.labelSmall, color = Color.Red, fontWeight = FontWeight.Black)
                    }
                }
                Surface(
                    color = if (isOutOfStock) Color.Red.copy(alpha = 0.1f) else if (isLowStock) Color(0xFFF44336).copy(alpha = 0.1f) else Color.White.copy(alpha = 0.05f),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = "AVAIL: $effectiveStock", 
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelSmall, 
                        fontWeight = FontWeight.Black,
                        color = if (isOutOfStock) Color.Red else if (isLowStock) Color(0xFFF44336) else Color.White.copy(alpha = 0.4f)
                    )
                }
            }
            
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StandardTextField(value = qty, onValueChange = { qty = it }, label = "Qty", modifier = Modifier.weight(1f), keyboardType = KeyboardType.Number, enabled = !isOutOfStock)
                StandardTextField(value = price, onValueChange = { price = it }, label = "Price", modifier = Modifier.weight(1.5f), keyboardType = KeyboardType.Number, enabled = !isOutOfStock)
            }
            Button(
                onClick = { onAdd(qty.toIntOrNull() ?: 1, price.toDoubleOrNull() ?: product.sellingPrice) }, 
                modifier = Modifier.fillMaxWidth().height(40.dp), 
                shape = RoundedCornerShape(10.dp), 
                enabled = !isOutOfStock,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isOutOfStock) Color.Gray.copy(alpha = 0.2f) else Color(0xFF2196F3).copy(alpha = 0.2f), 
                    contentColor = if (isOutOfStock) Color.Gray else Color(0xFF2196F3)
                )
            ) {
                Text(if (isOutOfStock) "UNAVAILABLE" else "ADD TO BASKET", fontSize = 11.sp, fontWeight = FontWeight.Black)
            }
        }
    }
}


@Composable
fun CartItemView(item: TransactionItem, onRemove: () -> Unit) {
    Surface(
        color = Color.White.copy(alpha = 0.03f), 
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.05f))
    ) {
        Row(Modifier.padding(10.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(item.name, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = Color.White)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("${item.quantity} units", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.4f))
                    Spacer(Modifier.width(6.dp))
                    Box(Modifier.size(2.dp).background(Color.White.copy(alpha = 0.2f), RoundedCornerShape(1.dp)))
                    Spacer(Modifier.width(6.dp))
                    Text("KSH ${String.format(Locale.US, "%,.0f", item.unitPrice)}", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.4f))
                }
            }
            IconButton(onClick = onRemove, modifier = Modifier.size(28.dp)) {
                Icon(Icons.Default.RemoveCircleOutline, null, tint = Color(0xFFE57373).copy(alpha = 0.6f), modifier = Modifier.size(18.dp))
            }
        }
    }
}
