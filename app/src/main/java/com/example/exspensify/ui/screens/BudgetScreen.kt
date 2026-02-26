package com.example.exspensify.ui.screens

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.exspensify.core.util.UiEvent
import com.example.exspensify.domain.model.Budget
import com.example.exspensify.domain.model.BudgetPeriod
import com.example.exspensify.ui.budget.BudgetEvent
import com.example.exspensify.ui.budget.BudgetViewModel

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetScreen(
    viewModel: BudgetViewModel = hiltViewModel(),
    onNavigate: (String) -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var budgetToDelete by remember { mutableStateOf<Budget?>(null) }

    LaunchedEffect(Unit) {
        viewModel.uiEvent.collect { event ->
            when (event) {
                is UiEvent.ShowSnackbar -> snackbarHostState.showSnackbar(event.message)
                is UiEvent.Navigate -> onNavigate(event.route)
                else -> {}
            }
        }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Budgets") }) },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            FloatingActionButton(
                // TODO: replace with Routes.AddEditBudget.createRoute() after Task 11
                onClick = { onNavigate("add_edit_budget/new") }
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Budget")
            }
        }
    ) { padding ->
        when {
            uiState.isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) { CircularProgressIndicator() }
            }
            uiState.error != null -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Something went wrong", style = MaterialTheme.typography.titleMedium)
                        TextButton(onClick = { viewModel.onEvent(BudgetEvent.Refresh) }) {
                            Text("Retry")
                        }
                    }
                }
            }
            uiState.budgets.isEmpty() -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(32.dp)
                    ) {
                        Text("\uD83D\uDCB0", style = MaterialTheme.typography.displayLarge)
                        Text(
                            "No budgets yet",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "Tap + to set a spending limit for a category",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            else -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        BudgetOverviewCard(
                            totalBudgeted = uiState.totalBudgeted,
                            totalSpent = uiState.totalSpent
                        )
                    }
                    items(uiState.budgets, key = { it.id }) { budget ->
                        BudgetItemCard(
                            budget = budget,
                            // TODO: replace with Routes.AddEditBudget.createRoute(budget.id) after Task 11
                            onEdit = { onNavigate("add_edit_budget/${budget.id}") },
                            onDelete = { budgetToDelete = budget }
                        )
                    }
                }
            }
        }
    }

    budgetToDelete?.let { budget ->
        AlertDialog(
            onDismissRequest = { budgetToDelete = null },
            title = { Text("Delete Budget") },
            text = { Text("Delete this budget? This cannot be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.onEvent(BudgetEvent.DeleteBudget(budget.id))
                    budgetToDelete = null
                }) { Text("Delete", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { budgetToDelete = null }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun BudgetOverviewCard(totalBudgeted: Double, totalSpent: Double) {
    val progress = if (totalBudgeted > 0) {
        (totalSpent / totalBudgeted).toFloat().coerceIn(0f, 1f)
    } else 0f

    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                "Overview",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Total Budgeted", color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("${"%.2f".format(totalBudgeted)}", fontWeight = FontWeight.Medium)
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Total Spent", color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("${"%.2f".format(totalSpent)}", fontWeight = FontWeight.Medium)
            }
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = budgetProgressColor(progress),
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
            Text(
                "${(progress * 100).toInt()}% of total budget used",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun BudgetItemCard(
    budget: Budget,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val progress = if (budget.limit > 0) {
        (budget.spent / budget.limit).toFloat().coerceIn(0f, 1f)
    } else 0f
    val progressColor = budgetProgressColor(progress)

    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(budget.categoryIcon, style = MaterialTheme.typography.titleLarge)
                    Column {
                        Text(
                            budget.categoryName,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            budget.period.label,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .background(
                                    MaterialTheme.colorScheme.primaryContainer,
                                    RoundedCornerShape(4.dp)
                                )
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
                Row {
                    IconButton(onClick = onEdit, modifier = Modifier.size(36.dp)) {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = "Edit",
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Delete",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    "${"%.2f".format(budget.spent)} spent",
                    fontWeight = FontWeight.Medium
                )
                Text(
                    "of ${"%.2f".format(budget.limit)}",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = progressColor,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
            if (progress >= 0.75f) {
                Text(
                    if (progress >= 1f) "\u26D4 Over budget!" else "\u26A0 Near limit",
                    style = MaterialTheme.typography.bodySmall,
                    color = progressColor
                )
            }
        }
    }
}

@Composable
private fun budgetProgressColor(progress: Float): Color = when {
    progress >= 1f -> MaterialTheme.colorScheme.error
    progress >= 0.75f -> Color(0xFFF59E0B)
    else -> MaterialTheme.colorScheme.primary
}

private val BudgetPeriod.label: String
    get() = when (this) {
        BudgetPeriod.WEEKLY -> "WEEKLY"
        BudgetPeriod.MONTHLY -> "MONTHLY"
        BudgetPeriod.YEARLY -> "YEARLY"
    }
