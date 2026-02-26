package com.example.exspensify.ui.budgets

import com.example.exspensify.domain.model.Budget

data class BudgetUiState(
    val budgets: List<Budget> = emptyList(),
    val totalBudget: Double = 0.0,
    val totalSpent: Double = 0.0,
    val isLoading: Boolean = false,
    val error: String? = null
)