package com.example.exspensify.ui.screens

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.exspensify.core.util.UiEvent
import com.example.exspensify.domain.model.BudgetPeriod
import com.example.exspensify.domain.model.Category
import com.example.exspensify.ui.budget.AddEditBudgetEvent
import com.example.exspensify.ui.budget.AddEditBudgetViewModel

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditBudgetScreen(
    viewModel: AddEditBudgetViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.uiEvent.collect { event ->
            when (event) {
                is UiEvent.NavigateBack -> onNavigateBack()
                is UiEvent.ShowSnackbar -> snackbarHostState.showSnackbar(event.message)
                else -> {}
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (uiState.id == null) "Add Budget" else "Edit Budget") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
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
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            BudgetCategoryDropdown(
                categories = uiState.categories,
                selected = uiState.selectedCategory,
                onSelect = { viewModel.onEvent(AddEditBudgetEvent.CategorySelected(it)) },
                error = uiState.categoryError
            )

            OutlinedTextField(
                value = uiState.limitInput,
                onValueChange = { viewModel.onEvent(AddEditBudgetEvent.LimitChanged(it)) },
                label = { Text("Budget Limit") },
                placeholder = { Text("0.00") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                isError = uiState.limitError != null,
                supportingText = uiState.limitError?.let { { Text(it) } },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Text("Period", style = MaterialTheme.typography.labelLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                BudgetPeriod.entries.forEach { period ->
                    FilterChip(
                        selected = uiState.period == period,
                        onClick = { viewModel.onEvent(AddEditBudgetEvent.PeriodChanged(period)) },
                        label = { Text(period.name) }
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = { viewModel.onEvent(AddEditBudgetEvent.SaveBudget) },
                enabled = !uiState.isLoading,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text(if (uiState.id == null) "Create Budget" else "Save Changes")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BudgetCategoryDropdown(
    categories: List<Category>,
    selected: Category?,
    onSelect: (Category) -> Unit,
    error: String?
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it }
    ) {
        OutlinedTextField(
            value = if (selected != null) "${selected.icon} ${selected.name}" else "",
            onValueChange = {},
            readOnly = true,
            label = { Text("Category") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            isError = error != null,
            supportingText = error?.let { { Text(it) } },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor()
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            categories.forEach { category ->
                DropdownMenuItem(
                    text = { Text("${category.icon} ${category.name}") },
                    onClick = {
                        onSelect(category)
                        expanded = false
                    }
                )
            }
        }
    }
}
