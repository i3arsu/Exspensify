package com.example.exspensify.domain.repository

import com.example.exspensify.core.util.Resource
import com.example.exspensify.domain.model.*
import kotlinx.coroutines.flow.Flow

interface StatisticsRepository {
    
    /**
     * Get category statistics for the given filter
     * Automatically handles BOTH type by combining EXPENSE and INCOME
     */
    fun getCategoryStatistics(filter: StatisticsFilter): Flow<Resource<List<CategoryStatistic>>>
    
    /**
     * Get daily statistics for the given filter
     * Returns zero-filled data for all days in the month
     */
    fun getDailyStatistics(filter: StatisticsFilter): Flow<Resource<List<DailyStatistic>>>
    
    /**
     * Get monthly statistics for the given filter
     * Returns zero-filled data for all 12 months
     */
    fun getMonthlyStatistics(filter: StatisticsFilter): Flow<Resource<List<MonthlyStatistic>>>
}