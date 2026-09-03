package com.raymond.cms.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.raymond.cms.domain.StaffPerformance
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StaffPerformanceScreen(
    viewModel: TransactionViewModel,
    onBack: () -> Unit
) {
    val reportState by viewModel.reportState.collectAsState()
    val performance = reportState.staffPerformance

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Staff Performance") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        if (performance.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("No data available", color = MaterialTheme.colorScheme.secondary)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(performance) { p ->
                    StaffPerformanceCard(p)
                }
            }
        }
    }
}

@Composable
fun StaffPerformanceCard(p: StaffPerformance) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(p.staffName, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                StatItem("Shifts", p.shiftCount.toString(), Modifier.weight(1f))
                StatItem("Transactions", p.transactionCount.toString(), Modifier.weight(1f))
            }
            
            HorizontalDivider()
            
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                StatItem("Revenue", "KSh ${String.format(Locale.US, "%.0f", p.totalRevenue)}", Modifier.weight(1f))
                StatItem("Expenses", "KSh ${String.format(Locale.US, "%.0f", p.totalExpenses)}", Modifier.weight(1f))
            }
        }
    }
}

@Composable
fun StatItem(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
        Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
    }
}
