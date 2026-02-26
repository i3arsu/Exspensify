package com.example.exspensify.domain.model

data class CategoryStatistic(
    val categoryId: String,
    val categoryName: String,
    val categoryIcon: String,
    val categoryColor: Any,
    val amount: Double,
    val percentage: Float
)

data class DailyStatistic(
    val day: Int,           // 1-31
    val expenseAmount: Double,
    val incomeAmount: Double
)

data class MonthlyStatistic(
    val month: Int,         // 1-12
    val monthName: String,  // "Jan", "Feb", etc.
    val expenseAmount: Double,
    val incomeAmount: Double
)