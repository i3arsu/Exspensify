package com.example.exspensify.ui.statistics

import android.os.Build
import androidx.annotation.RequiresApi
import com.example.exspensify.domain.model.*

data class StatisticsUiState @RequiresApi(Build.VERSION_CODES.O) constructor(
    val filter: StatisticsFilter = StatisticsFilter(
        month = java.time.LocalDateTime.now().monthValue,
        year = java.time.LocalDateTime.now().year,
        type = StatisticsType.EXPENSE
    ),
    
    // Chart data
    val categoryStatistics: List<CategoryStatistic> = emptyList(),
    val dailyStatistics: List<DailyStatistic> = emptyList(),
    val monthlyStatistics: List<MonthlyStatistic> = emptyList(),
    
    // Summary
    val totalAmount: Double = 0.0,
    
    // UI states
    val isLoading: Boolean = false,
    val error: String? = null
)