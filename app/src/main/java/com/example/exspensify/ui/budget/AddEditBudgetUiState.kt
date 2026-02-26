package com.example.exspensify.ui.budget

import com.example.exspensify.domain.model.BudgetPeriod
import com.example.exspensify.domain.model.Category

data class AddEditBudgetUiState(
    val id: String? = null,
    val selectedCategory: Category? = null,
    val limitInput: String = "",
    val period: BudgetPeriod = BudgetPeriod.MONTHLY,
    val categories: List<Category> = emptyList(),
    val isLoading: Boolean = false,
    val limitError: String? = null,
    val categoryError: String? = null,
    val error: String? = null
) {
    val isValid: Boolean
        get() = selectedCategory != null &&
                limitInput.toDoubleOrNull()?.let { it > 0 } == true
}

sealed class AddEditBudgetEvent {
    data class CategorySelected(val category: Category) : AddEditBudgetEvent()
    data class LimitChanged(val value: String) : AddEditBudgetEvent()
    data class PeriodChanged(val period: BudgetPeriod) : AddEditBudgetEvent()
    object SaveBudget : AddEditBudgetEvent()
}
