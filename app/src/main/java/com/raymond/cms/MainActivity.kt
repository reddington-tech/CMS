package com.raymond.cms

import android.os.Bundle
import androidx.fragment.app.FragmentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.raymond.cms.model.UserRole
import com.raymond.cms.ui.*
import com.raymond.cms.ui.components.*
import com.raymond.cms.ui.theme.CMSTheme
import com.raymond.cms.util.NotificationHelper
import com.raymond.cms.domain.ShiftScheduler
import kotlinx.coroutines.launch
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.ProcessLifecycleOwner

class MainActivity : FragmentActivity() {
    private var lastBackgroundTime: Long = 0
    private val lockGracePeriod = 60 * 1000 // 60 seconds

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Request Permissions for Android 13+ (Requirement #3, #4)
        askNotificationPermission()

        // Initialize Background Tasks & Notifications (Requirement #1, #3, #4)
        NotificationHelper.createNotificationChannel(this)
        ShiftScheduler.scheduleTasks(this)
        
        enableEdgeToEdge()
        setContent {
            CMSTheme {
                val authViewModel: AuthViewModel = viewModel()
                MainRootWrapper(authViewModel)
            }
        }

        setupLifecycleObserver()
    }

    private fun setupLifecycleObserver() {
        val authViewModel: AuthViewModel by lazy { 
            androidx.lifecycle.ViewModelProvider(this)[AuthViewModel::class.java] 
        }
        
        ProcessLifecycleOwner.get().lifecycle.addObserver(LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_STOP -> {
                    lastBackgroundTime = System.currentTimeMillis()
                }
                Lifecycle.Event.ON_START -> {
                    val currentTime = System.currentTimeMillis()
                    if (lastBackgroundTime != 0L && (currentTime - lastBackgroundTime) > lockGracePeriod) {
                        authViewModel.setLocked(true)
                    }
                }
                else -> {}
            }
        })
    }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            // Permission granted
        } else {
            // Permission denied
        }
    }

    private fun askNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) !=
                PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }
}

@Composable
fun MainRootWrapper(authViewModel: AuthViewModel) {
    val isLocked by authViewModel.isLocked.collectAsState()

    Box(modifier = Modifier.fillMaxSize()) {
        MainRoot(authViewModel)

        if (isLocked) {
            StartupShieldOverlay(authViewModel)
        }
    }
}



@Composable
fun DrawerMenuItem(
    label: String,
    icon: ImageVector,
    badgeCount: Int = 0,
    isErrorBadge: Boolean = false,
    onClick: () -> Unit
) {
    NavigationDrawerItem(
        label = { 
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
            ) {
                Text(label, fontWeight = FontWeight.Medium)
                if (badgeCount > 0) {
                    Surface(
                        color = if (isErrorBadge) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(
                            text = badgeCount.toString(),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        },
        selected = false,
        onClick = onClick,
        icon = { Icon(icon, null) },
        shape = RoundedCornerShape(12.dp),
        colors = NavigationDrawerItemDefaults.colors(
            unselectedContainerColor = Color.Transparent,
            unselectedIconColor = Color.White.copy(alpha = 0.7f),
            unselectedTextColor = Color.White.copy(alpha = 0.9f)
        ),
        modifier = Modifier.padding(horizontal = 4.dp)
    )
}

@Composable
fun MainRoot(authViewModel: AuthViewModel = viewModel()) {
    val currentUser by authViewModel.currentUser.collectAsState()
    val isInitialLoadComplete by authViewModel.isInitialLoadComplete.collectAsState()
    val activeShift by authViewModel.activeShift.collectAsState()
    var skipClockIn by remember { mutableStateOf(false) }
    var setupComplete by remember { mutableStateOf(false) }
    var continuedShift by remember { mutableStateOf(false) }

    if (!isInitialLoadComplete) {
        ExecutiveLoadingScreen()
    } else {
        val user = currentUser
        if (user == null) {
            AuthNavigation(authViewModel)
        } else {
            val isStaff = user.role == UserRole.STAFF
            val isAdmin = user.role == UserRole.ADMIN
            
            // Admins NEVER need to clock in
            val needsClockIn = isStaff && activeShift == null && !skipClockIn

            if (needsClockIn) {
                ClockInWorkflow(
                    viewModel = authViewModel,
                    onSkip = { skipClockIn = true }
                )
            } else if (isStaff && activeShift != null && !continuedShift) {
                ShiftInProgressScreen(
                    shift = activeShift!!,
                    onContinue = { continuedShift = true }
                )
            } else {
                AppNavigation(
                    authViewModel = authViewModel,
                    isReadOnly = isStaff && activeShift == null,
                    onClockInRequest = { skipClockIn = false }
                )
            }
        }
    }
}


@Composable
fun ClockInWorkflow(viewModel: AuthViewModel, onSkip: () -> Unit) {
    var step by remember { mutableStateOf("prompt") } // prompt, balance

    when (step) {
        "prompt" -> {
            ClockInPromptScreen(
                onClockIn = { step = "balance" },
                onSkip = onSkip
            )
        }
        "balance" -> {
            OpeningBalanceConfirmScreen(
                viewModel = viewModel,
                onConfirmed = { /* Handled by Flow */ },
                onBack = { step = "prompt" }
            )
        }
    }
}


@Composable
fun AuthNavigation(viewModel: AuthViewModel) {
    val navController = rememberNavController()
    NavHost(
        navController = navController, 
        startDestination = "login",
        enterTransition = { fadeIn(tween(400)) + slideInHorizontally { it } },
        exitTransition = { fadeOut(tween(400)) + slideOutHorizontally { -it } },
        popEnterTransition = { fadeIn(tween(400)) + slideInHorizontally { -it } },
        popExitTransition = { fadeOut(tween(400)) + slideOutHorizontally { it } }
    ) {
        composable("login") {
            LoginScreen(viewModel)
        }
    }
}


@Composable
fun AppNavigation(
    authViewModel: AuthViewModel,
    isReadOnly: Boolean = false,
    onClockInRequest: () -> Unit = {}
) {
    val navController = rememberNavController()
    val transactionViewModel: TransactionViewModel = viewModel()
    val productViewModel: ProductViewModel = viewModel()
    val financialViewModel: FinancialViewModel = viewModel()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val currentUser by authViewModel.currentUser.collectAsState()
    
    val pendingApprovals by financialViewModel.pendingRequests.collectAsState()
    val allShifts by transactionViewModel.allShifts.collectAsState()
    
    val pendingDiscrepancies = allShifts.count { 
        (it.openingBalanceDifference != 0.0 || it.flaggedForReview) && it.varianceStatus == "PENDING" 
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = Color(0xFF0F1318),
                drawerTonalElevation = 0.dp,
                drawerShape = RoundedCornerShape(topEnd = 24.dp, bottomEnd = 24.dp),
                modifier = Modifier.width(320.dp)
            ) {
                // Drawer Header
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                                    Color.Transparent
                                )
                            )
                        )
                        .statusBarsPadding()
                        .padding(24.dp)
                ) {
                    Column {
                        Surface(
                            color = MaterialTheme.colorScheme.primary,
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.size(48.dp)
                        ) {
                            Box(contentAlignment = androidx.compose.ui.Alignment.Center) {
                                Icon(Icons.Default.DashboardCustomize, null, tint = Color.White)
                            }
                        }
                        Spacer(Modifier.height(16.dp))
                        Text(
                            "Cyber Manager System", 
                            style = MaterialTheme.typography.titleLarge, 
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White
                        )
                        Text(
                            currentUser?.email ?: "", 
                            style = MaterialTheme.typography.labelMedium, 
                            color = Color.Gray
                        )
                        
                        // Role Badge
                        Surface(
                            color = if (currentUser?.role == UserRole.ADMIN) Color(0xFF4CAF50).copy(alpha = 0.1f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.padding(top = 8.dp)
                        ) {
                            Text(
                                text = currentUser?.role?.name ?: "STAFF",
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (currentUser?.role == UserRole.ADMIN) Color(0xFF4CAF50) else MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    DashboardSectionHeader("Home", modifier = Modifier.padding(start = 8.dp))
                    
                    DrawerMenuItem(
                        label = "Business Dashboard",
                        icon = Icons.Default.Dashboard,
                        onClick = {
                            scope.launch { drawerState.close() }
                            navController.navigate("dashboard") { popUpTo("dashboard") { inclusive = true } }
                        }
                    )

                    DrawerMenuItem(
                        label = "Record New Sale",
                        icon = Icons.Default.AddShoppingCart,
                        onClick = {
                            scope.launch { drawerState.close() }
                            navController.navigate("add_transaction")
                        }
                    )

                    Spacer(Modifier.height(8.dp))
                    DashboardSectionHeader("Records", modifier = Modifier.padding(start = 8.dp))

                    DrawerMenuItem(
                        label = "Daily Records",
                        icon = Icons.AutoMirrored.Filled.List,
                        onClick = {
                            scope.launch { drawerState.close() }
                            navController.navigate("list")
                        }
                    )

                    DrawerMenuItem(
                        label = "Supplies & Stock",
                        icon = Icons.Default.Inventory2,
                        onClick = {
                            scope.launch { drawerState.close() }
                            navController.navigate("supplies")
                        }
                    )

                    Spacer(Modifier.height(8.dp))
                    DashboardSectionHeader("Operations", modifier = Modifier.padding(start = 8.dp))

                    DrawerMenuItem(
                        label = "Record Expense",
                        icon = Icons.AutoMirrored.Filled.ReceiptLong,
                        onClick = {
                            scope.launch { drawerState.close() }
                            navController.navigate("add_expense")
                        }
                    )

                    if (currentUser?.role == UserRole.ADMIN) {
                        DrawerMenuItem(
                            label = "Consolidated Daily Records",
                            icon = Icons.Default.PostAdd,
                            onClick = {
                                scope.launch { drawerState.close() }
                                navController.navigate("add")
                            }
                        )

                        DrawerMenuItem(
                            label = "Cash Transfer / Investment",
                            icon = Icons.Default.AccountBalance,
                            onClick = {
                                scope.launch { drawerState.close() }
                                navController.navigate("add_investment")
                            }
                        )

                        Spacer(Modifier.height(8.dp))
                        DashboardSectionHeader("Administration", modifier = Modifier.padding(start = 8.dp))

                        DrawerMenuItem(
                            label = "Pending Approvals",
                            icon = Icons.AutoMirrored.Filled.FactCheck,
                            badgeCount = pendingApprovals.count { it.status == com.raymond.cms.model.RequestStatus.PENDING },
                            onClick = {
                                scope.launch { drawerState.close() }
                                navController.navigate("admin_approvals")
                            }
                        )

                        DrawerMenuItem(
                            label = "Balance Discrepancies",
                            icon = Icons.Default.Warning,
                            badgeCount = pendingDiscrepancies,
                            isErrorBadge = true,
                            onClick = {
                                scope.launch { drawerState.close() }
                                navController.navigate("discrepancies")
                            }
                        )

                        DrawerMenuItem(
                            label = "Business Intelligence",
                            icon = Icons.Default.BarChart,
                            onClick = {
                                scope.launch { drawerState.close() }
                                navController.navigate("reports_hub")
                            }
                        )

                        Spacer(Modifier.height(8.dp))
                        DashboardSectionHeader("Management", modifier = Modifier.padding(start = 8.dp))

                        DrawerMenuItem(
                            label = "Manage Sales Items",
                            icon = Icons.Default.Storefront,
                            onClick = {
                                scope.launch { drawerState.close() }
                                navController.navigate("products")
                            }
                        )

                        DrawerMenuItem(
                            label = "Services & Prices",
                            icon = Icons.Default.PriceChange,
                            onClick = {
                                scope.launch { drawerState.close() }
                                navController.navigate("services")
                            }
                        )

                        DrawerMenuItem(
                            label = "Manage Staff",
                            icon = Icons.Default.People,
                            onClick = {
                                scope.launch { drawerState.close() }
                                navController.navigate("manage_staff")
                            }
                        )

                        DrawerMenuItem(
                            label = "Staff Performance",
                            icon = Icons.Default.Timeline,
                            onClick = {
                                scope.launch { drawerState.close() }
                                navController.navigate("staff_performance")
                            }
                        )
                    }
                }



                // Footer Section
                Column(modifier = Modifier.padding(16.dp)) {
                    HorizontalDivider(modifier = Modifier.padding(bottom = 16.dp), color = Color.White.copy(alpha = 0.05f))
                    NavigationDrawerItem(
                        label = { Text("Logout Account", fontWeight = FontWeight.Black, fontSize = 14.sp) },
                        selected = false,
                        onClick = {
                            scope.launch { drawerState.close() }
                            authViewModel.logout()
                        },
                        icon = { Icon(Icons.AutoMirrored.Filled.ExitToApp, null) },
                        shape = RoundedCornerShape(16.dp),
                        colors = NavigationDrawerItemDefaults.colors(
                            unselectedContainerColor = Color(0xFFE57373).copy(alpha = 0.05f),
                            unselectedIconColor = Color(0xFFE57373),
                            unselectedTextColor = Color(0xFFE57373)
                        )
                    )
                }
            }
        }
    ) {
        NavHost(
            navController = navController, 
            startDestination = "dashboard",
            enterTransition = { slideInVertically(tween(400)) { it } + fadeIn(tween(400)) },
            exitTransition = { slideOutVertically(tween(400)) { -it } + fadeOut(tween(400)) },
            popEnterTransition = { slideInVertically(tween(400)) { -it } + fadeIn(tween(400)) },
            popExitTransition = { slideOutVertically(tween(400)) { it } + fadeOut(tween(400)) }
        ) {
            composable("dashboard") {
                DashboardScreen(
                    viewModel = transactionViewModel,
                    authViewModel = authViewModel,
                    isReadOnly = isReadOnly,
                    onClockInClick = onClockInRequest,
                    onClockOutClick = { navController.navigate("clock_out") },
                    onAddTransaction = { navController.navigate("add") },
                    onNewSale = { navController.navigate("add_transaction") },
                    onAddExpense = { navController.navigate("add_expense") },
                    onAddInvestment = { navController.navigate("add_investment") },
                    onMenuClick = { scope.launch { drawerState.open() } },
                    onShiftClick = { id -> 
                        if (id.startsWith("LEGACY_")) {
                            val dateStr = id.substringAfter("LEGACY_")
                            val summary = transactionViewModel.allDailyBreakdowns.value.find { it.id == dateStr }
                            val ts = summary?.timestamp ?: System.currentTimeMillis()
                            navController.navigate("report_detail/${ReportPeriod.DAILY.name}/$ts")
                        } else if (id.startsWith("CONSOLIDATED_")) {
                            val ts = id.substringAfter("CONSOLIDATED_").toLongOrNull() ?: System.currentTimeMillis()
                            navController.navigate("report_detail/${ReportPeriod.DAILY.name}/$ts")
                        } else {
                            val shift = transactionViewModel.allShifts.value.find { it.id == id }
                            val ts = shift?.clockInTime ?: System.currentTimeMillis()
                            navController.navigate("report_detail/${ReportPeriod.DAILY.name}/$ts") 
                        }
                    },
                    onEditShiftClick = { id ->
                        navController.navigate("edit_summary/$id")
                    },
                    onDiscrepancyClick = { navController.navigate("discrepancies") }
                )
            }
            composable("add_transaction") {
                TransactionEntryScreen(
                    authViewModel = authViewModel,
                    productViewModel = productViewModel,
                    financialViewModel = financialViewModel,
                    onBack = { navController.popBackStack() }
                )
            }
            composable("add_expense") {
                ExpenseEntryScreen(
                    authViewModel = authViewModel,
                    financialViewModel = financialViewModel,
                    onBack = { navController.popBackStack() }
                )
            }
            composable("add_investment") {
                InvestmentEntryScreen(
                    authViewModel = authViewModel,
                    financialViewModel = financialViewModel,
                    onBack = { navController.popBackStack() }
                )
            }
            composable("clock_out") {
                ClockOutScreen(
                    viewModel = authViewModel,
                    onBack = { navController.popBackStack() },
                    onClockOutComplete = { 
                        navController.navigate("dashboard") {
                            popUpTo("dashboard") { inclusive = true }
                        }
                    }
                )
            }
            composable("list") {
                TransactionListScreen(
                    viewModel = transactionViewModel,
                    authViewModel = authViewModel,
                    onAddClick = { navController.navigate("add_transaction") },
                    onMenuClick = { scope.launch { drawerState.open() } },
                    onShiftClick = { id -> 
                        if (id.startsWith("LEGACY_")) {
                            val dateStr = id.substringAfter("LEGACY_")
                            val summary = transactionViewModel.allDailyBreakdowns.value.find { it.id == dateStr }
                            val ts = summary?.timestamp ?: System.currentTimeMillis()
                            navController.navigate("report_detail/${ReportPeriod.DAILY.name}/$ts")
                        } else if (id.startsWith("CONSOLIDATED_")) {
                            val ts = id.substringAfter("CONSOLIDATED_").toLongOrNull() ?: System.currentTimeMillis()
                            navController.navigate("report_detail/${ReportPeriod.DAILY.name}/$ts")
                        } else {
                            val shift = transactionViewModel.allShifts.value.find { it.id == id }
                            val ts = shift?.clockInTime ?: System.currentTimeMillis()
                            navController.navigate("report_detail/${ReportPeriod.DAILY.name}/$ts") 
                        }
                    },
                    onEditShiftClick = { id ->
                        navController.navigate("edit_summary/$id")
                    },
                    onItemClick = { id -> navController.navigate("details/$id") }
                )
            }
            composable("add") {
                AddTransactionScreen(
                    viewModel = transactionViewModel,
                    authViewModel = authViewModel,
                    onBack = { navController.popBackStack() }
                )
            }
            composable("reports_hub") {
                ReportsHubScreen(
                    viewModel = transactionViewModel,
                    onNavigateToPeriod = { period, timestamp ->
                        navController.navigate("report_detail/${period.name}/$timestamp")
                    },
                    onCustomRange = { navController.navigate("custom_report") },
                    onMenuClick = { scope.launch { drawerState.open() } }
                )
            }
            composable(
                route = "report_detail/{period}/{timestamp}",
                arguments = listOf(
                    navArgument("period") { type = NavType.StringType },
                    navArgument("timestamp") { type = NavType.LongType }
                )
            ) { backStackEntry ->
                val periodName = backStackEntry.arguments?.getString("period") ?: "DAILY"
                val timestamp = backStackEntry.arguments?.getLong("timestamp") ?: System.currentTimeMillis()
                val period = ReportPeriod.valueOf(periodName)
                
                PeriodReportScreen(
                    title = "$periodName REPORT",
                    viewModel = transactionViewModel,
                    period = period,
                    initialTimestamp = timestamp,
                    onBack = { navController.popBackStack() },
                    onDrillDown = { drillTs ->
                        if (period == ReportPeriod.DAILY) return@PeriodReportScreen
                        navController.navigate("report_detail/${ReportPeriod.DAILY.name}/$drillTs")
                    },
                    onTransactionClick = { txId ->
                        navController.navigate("details/$txId")
                    },
                    onExpenseClick = { exId ->
                        navController.navigate("expense_details/$exId")
                    },
                    onEditClick = { recordId ->
                        navController.navigate("edit_summary/$recordId")
                    }
                )
            }
            composable("custom_report") {
                CustomRangeReportScreen(
                    viewModel = transactionViewModel,
                    onBack = { navController.popBackStack() },
                    onDrillDown = { drillTs ->
                        navController.navigate("report_detail/${ReportPeriod.DAILY.name}/$drillTs")
                    }
                )
            }
            composable(
                route = "edit_summary/{recordId}",
                arguments = listOf(navArgument("recordId") { type = NavType.StringType })
            ) { backStackEntry ->
                val recordId = backStackEntry.arguments?.getString("recordId") ?: ""
                EditDailySummaryScreen(
                    recordId = recordId,
                    viewModel = transactionViewModel,
                    authViewModel = authViewModel,
                    onBack = { navController.popBackStack() }
                )
            }
            composable("supplies") {
                SuppliesScreen(
                    viewModel = transactionViewModel,
                    onMenuClick = { scope.launch { drawerState.open() } }
                )
            }
            composable("staff_performance") {
                StaffPerformanceScreen(
                    viewModel = transactionViewModel,
                    onBack = { navController.popBackStack() }
                )
            }
            composable("products") {
                ProductManagementScreen(
                    viewModel = productViewModel,
                    authViewModel = authViewModel,
                    onBack = { navController.popBackStack() }
                )
            }
            composable("services") {
                ServicePricesScreen(
                    viewModel = authViewModel,
                    onContinue = { navController.popBackStack() }
                )
            }
            composable("manage_staff") {
                ManageStaffScreen(
                    viewModel = authViewModel,
                    transactionViewModel = transactionViewModel,
                    onBack = { navController.popBackStack() }
                )
            }
            composable("admin_approvals") {
                AdminApprovalScreen(
                    authViewModel = authViewModel,
                    financialViewModel = financialViewModel,
                    onBack = { navController.popBackStack() }
                )
            }
            composable("discrepancies") {
                BalanceDiscrepanciesScreen(
                    viewModel = transactionViewModel,
                    authViewModel = authViewModel,
                    onBack = { navController.popBackStack() }
                )
            }
            composable(
                route = "details/{txId}",
                arguments = listOf(navArgument("txId") { type = NavType.StringType })
            ) { backStackEntry ->
                val txId = backStackEntry.arguments?.getString("txId") ?: ""
                TransactionDetailScreen(
                    transactionId = txId,
                    viewModel = transactionViewModel,
                    authViewModel = authViewModel,
                    financialViewModel = financialViewModel,
                    onBack = { navController.popBackStack() }
                )
            }
            composable(
                route = "expense_details/{exId}",
                arguments = listOf(navArgument("exId") { type = NavType.StringType })
            ) { backStackEntry ->
                val exId = backStackEntry.arguments?.getString("exId") ?: ""
                ExpenseDetailScreen(
                    expenseId = exId,
                    viewModel = transactionViewModel,
                    authViewModel = authViewModel,
                    financialViewModel = financialViewModel,
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}

