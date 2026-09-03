package com.raymond.cms.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Label
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
import com.raymond.cms.model.*
import com.raymond.cms.ui.components.*
import kotlinx.coroutines.flow.collectLatest
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductManagementScreen(
    viewModel: ProductViewModel,
    authViewModel: AuthViewModel,
    onBack: () -> Unit
) {
    val products by viewModel.products.collectAsState()
    val user by authViewModel.currentUser.collectAsState()
    val activeShift by authViewModel.activeShift.collectAsState()
    
    val isLoading by viewModel.isLoading.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }
    var showAddStockDialog by remember { mutableStateOf<Product?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.uiEvent.collectLatest { snackbarHostState.showSnackbar(it) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Enterprise Asset Registry", fontWeight = FontWeight.Black, letterSpacing = (-0.5).sp) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
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
                Icon(Icons.Default.Add, contentDescription = "Add Product")
            }
        }
    ) { padding ->
        if (isLoading) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
        } else if (products.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Inventory2, null, modifier = Modifier.size(64.dp), tint = Color.White.copy(alpha = 0.05f))
                    Spacer(Modifier.height(16.dp))
                    Text("No assets registered yet.", color = Color.White.copy(alpha = 0.2f), style = MaterialTheme.typography.bodyLarge)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    DashboardSectionHeader("Asset Inventory")
                }
                
                items(products) { product ->
                    ProductItem(
                        product = product,
                        onClick = { showAddStockDialog = product }
                    )
                }
            }
        }
    }

    if (showAddDialog) {
        ProductDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { name, desc, bPrice, sPrice, qty ->
                viewModel.addProduct(name, desc, bPrice, sPrice, qty)
                showAddDialog = false
            }
        )
    }

    if (showAddStockDialog != null) {
        AddStockDialog(
            product = showAddStockDialog!!,
            onDismiss = { showAddStockDialog = null },
            onConfirm = { quantity, price, method ->
                val u = user ?: return@AddStockDialog
                val sId = activeShift?.id ?: "ADMIN_SYSTEM_ENTRY"
                viewModel.addStock(showAddStockDialog!!.id, quantity, price, u, sId, method)
                showAddStockDialog = null
            }
        )
    }
}

@Composable
fun ProductItem(
    product: Product,
    onClick: () -> Unit
) {
    ExecutiveCard(
        onClick = onClick,
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.Top) {
                Column(Modifier.weight(1f)) {
                    Text(
                        text = product.name, 
                        style = MaterialTheme.typography.titleMedium, 
                        fontWeight = FontWeight.Bold, 
                        color = Color.White
                    )
                    Text(
                        text = product.description.ifEmpty { "No asset description provided." }, 
                        style = MaterialTheme.typography.bodySmall, 
                        color = Color.White.copy(alpha = 0.5f),
                        maxLines = 1
                    )
                }
                
                Surface(
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
                ) {
                    Text(
                        text = "STOCK: ${product.currentStock}", 
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall, 
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            
            HorizontalDivider(color = Color.White.copy(alpha = 0.05f))
            
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text("VALUATION (BUY)", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.3f), fontWeight = FontWeight.Black, letterSpacing = 1.sp)
                    Text(
                        text = "KSH ${String.format(Locale.US, "%,.0f", product.buyingPrice)}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
                
                Column(horizontalAlignment = Alignment.End) {
                    Text("MARKET PRICE (SELL)", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.3f), fontWeight = FontWeight.Black, letterSpacing = 1.sp)
                    Text(
                        text = "KSH ${String.format(Locale.US, "%,.0f", product.sellingPrice)}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFF4CAF50)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddStockDialog(
    product: Product,
    onDismiss: () -> Unit,
    onConfirm: (Int, Double, PaymentMethod) -> Unit
) {
    var quantity by remember { mutableStateOf("") }
    var buyingPrice by remember { mutableStateOf(product.buyingPrice.toString()) }
    var paymentMethod by remember { mutableStateOf<PaymentMethod>(PaymentMethod.CASH) }
    var expandedPayment by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Inventory Adjustment", fontWeight = FontWeight.ExtraBold, letterSpacing = (-0.5).sp) },
        containerColor = Color(0xFF1E252D),
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Column {
                    Text(text = product.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = Color.White)
                    Text(text = "Current Avg. Valuation: KSH ${product.buyingPrice}", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.4f))
                }
                
                StandardTextField(
                    value = quantity, 
                    onValueChange = { quantity = it }, 
                    label = "Inbound Quantity", 
                    keyboardType = KeyboardType.Number,
                    leadingIcon = { Icon(Icons.Default.AddShoppingCart, null, tint = Color.White.copy(alpha = 0.3f)) }
                )
                
                StandardTextField(
                    value = buyingPrice, 
                    onValueChange = { buyingPrice = it }, 
                    label = "Inbound Unit Cost (KSh)", 
                    keyboardType = KeyboardType.Number,
                    leadingIcon = { Icon(Icons.Default.Payments, null, tint = Color.White.copy(alpha = 0.3f)) }
                )

                ExposedDropdownMenuBox(
                    expanded = expandedPayment,
                    onExpandedChange = { expandedPayment = !expandedPayment }
                ) {
                    StandardTextField(
                        value = paymentMethod.name,
                        onValueChange = {},
                        readOnly = true,
                        label = "Payment Source (Expense)",
                        modifier = Modifier.menuAnchor(),
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedPayment) }
                    )
                    ExposedDropdownMenu(
                        expanded = expandedPayment,
                        onDismissRequest = { expandedPayment = false },
                        modifier = Modifier.background(Color(0xFF1E252D))
                    ) {
                        for (method in PaymentMethod.entries) {
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
                
                if (quantity.isNotEmpty() && buyingPrice.isNotEmpty()) {
                    val qty = quantity.toIntOrNull() ?: 0
                    val price = buyingPrice.toDoubleOrNull() ?: 0.0
                    if (qty > 0) {
                        val totalStock = product.currentStock + qty
                        val newAvg = ((product.currentStock * product.buyingPrice) + (qty * price)) / totalStock
                        Surface(
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.05f),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(Modifier.padding(12.dp)) {
                                Text("PROJECTED VALUATION", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary, letterSpacing = 1.sp)
                                Text(
                                    text = "KSH ${String.format(Locale.US, "%,.2f", newAvg)} / unit",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { 
                    onConfirm(
                        quantity.toIntOrNull() ?: 0,
                        buyingPrice.toDoubleOrNull() ?: product.buyingPrice,
                        paymentMethod
                    ) 
                },
                enabled = quantity.isNotEmpty() && (quantity.toIntOrNull() ?: 0) > 0 && buyingPrice.isNotEmpty(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("VALIDATE & UPDATE STOCK", fontWeight = FontWeight.Black)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("CANCEL", color = Color.White.copy(alpha = 0.5f)) }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductDialog(
    product: Product? = null,
    onDismiss: () -> Unit,
    onConfirm: (String, String, Double, Double, Int) -> Unit
) {
    var name by remember { mutableStateOf(product?.name ?: "") }
    var desc by remember { mutableStateOf(product?.description ?: "") }
    var bPrice by remember { mutableStateOf(product?.buyingPrice?.toString() ?: "") }
    var sPrice by remember { mutableStateOf(product?.sellingPrice?.toString() ?: "") }
    var qty by remember { mutableStateOf(product?.quantityBought?.toString() ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (product == null) "Register New Asset" else "Edit Asset Details", fontWeight = FontWeight.ExtraBold, letterSpacing = (-0.5).sp) },
        containerColor = Color(0xFF1E252D),
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                StandardTextField(value = name, onValueChange = { name = it }, label = "Asset Name", leadingIcon = { Icon(Icons.Default.Label, null, tint = Color.White.copy(alpha = 0.3f)) })
                StandardTextField(value = desc, onValueChange = { desc = it }, label = "Strategic Description", leadingIcon = { Icon(Icons.Default.Description, null, tint = Color.White.copy(alpha = 0.3f)) })
                StandardTextField(value = bPrice, onValueChange = { bPrice = it }, label = "Acquisition Cost (Buying)", keyboardType = KeyboardType.Number, leadingIcon = { Icon(Icons.Default.Payments, null, tint = Color.White.copy(alpha = 0.3f)) })
                StandardTextField(value = sPrice, onValueChange = { sPrice = it }, label = "Market Value (Selling)", keyboardType = KeyboardType.Number, leadingIcon = { Icon(Icons.Default.PriceCheck, null, tint = Color.White.copy(alpha = 0.3f)) })
                StandardTextField(value = qty, onValueChange = { qty = it }, label = "Initial Stock Quantity", keyboardType = KeyboardType.Number, leadingIcon = { Icon(Icons.Default.Inventory, null, tint = Color.White.copy(alpha = 0.3f)) })
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm(
                        name,
                        desc,
                        bPrice.toDoubleOrNull() ?: 0.0,
                        sPrice.toDoubleOrNull() ?: 0.0,
                        qty.toIntOrNull() ?: 0
                    )
                },
                enabled = name.isNotEmpty(),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().height(50.dp)
            ) {
                Text("SAVE ASSET", fontWeight = FontWeight.Black)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, modifier = Modifier.padding(bottom = 8.dp)) { Text("CANCEL", color = Color.White.copy(alpha = 0.5f)) }
        }
    )
}
