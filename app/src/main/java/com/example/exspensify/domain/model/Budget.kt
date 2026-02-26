package com.example.exspensify.domain.model

data class Budget(
    val id: String,
    val category: String,       // categoryId as String
    val limit: Double,
    val spent: Double,
    val period: BudgetPeriod,
    val categoryName: String = "",
    val categoryIcon: String = ""
)

enum class BudgetPeriod {
    WEEKLY,
    MONTHLY,
    YEARLY
}