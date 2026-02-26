package com.example.exspensify.domain.model

data class Budget(
    val id: String,
    val category: String,
    val limit: Double,
    val spent: Double,
    val period: BudgetPeriod
)

enum class BudgetPeriod {
    WEEKLY,
    MONTHLY,
    YEARLY
}