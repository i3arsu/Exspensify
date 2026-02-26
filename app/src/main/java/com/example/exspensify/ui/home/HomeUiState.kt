package com.example.exspensify.ui.home

import com.example.exspensify.domain.model.Transaction

data class HomeUiState(
    // Balance summary
    val currentMonthExpenses: Double = 0.0,
    val currentMonthIncome: Double = 0.0,
    val netBalance: Double = 0.0,

    // All-time totals
    val totalIncome: Double = 0.0,
    val totalExpenses: Double = 0.0,
    val totalBalance: Double = 0.0,

    // Transactions
    val recentTransactions: List<Transaction> = emptyList(),

    // Category breakdown
    val topCategories: List<CategorySpending> = emptyList(),

    // Loading states
    val isLoading: Boolean = false,
    val error: String? = null
)

data class CategorySpending(
    val categoryId: String,
    val categoryName: String,
    val categoryIcon: String,
    val categoryColor: String,
    val amount: Double,
    val percentage: Float
)