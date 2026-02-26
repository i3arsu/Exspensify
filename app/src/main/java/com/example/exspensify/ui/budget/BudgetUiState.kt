package com.example.exspensify.ui.budget

import com.example.exspensify.domain.model.Budget

data class BudgetUiState(
    val budgets: List<Budget> = emptyList(),
    val totalBudgeted: Double = 0.0,
    val totalSpent: Double = 0.0,
    val isLoading: Boolean = false,
    val error: String? = null
)

sealed class BudgetEvent {
    data class DeleteBudget(val id: String) : BudgetEvent()
    object Refresh : BudgetEvent()
}
