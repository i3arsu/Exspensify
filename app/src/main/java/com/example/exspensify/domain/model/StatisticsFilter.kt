package com.example.exspensify.domain.model

enum class StatisticsType {
    EXPENSE,
    INCOME,
    BOTH
}

data class StatisticsFilter(
    val month: Int,      // 1-12
    val year: Int,       // e.g., 2024
    val type: StatisticsType = StatisticsType.EXPENSE
)