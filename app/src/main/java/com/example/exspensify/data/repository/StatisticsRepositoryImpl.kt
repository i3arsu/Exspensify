package com.example.exspensify.data.repository

import android.os.Build
import com.example.exspensify.core.util.Resource
import com.example.exspensify.data.local.dao.CategoryDao
import com.example.exspensify.data.local.dao.CategoryStatisticEntity
import com.example.exspensify.data.local.dao.StatisticsDao
import com.example.exspensify.data.local.entity.TransactionType
import com.example.exspensify.domain.model.*
import com.example.exspensify.domain.repository.StatisticsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import java.time.YearMonth
import javax.inject.Inject

class StatisticsRepositoryImpl @Inject constructor(
    private val statisticsDao: StatisticsDao,
    private val categoryDao: CategoryDao
) : StatisticsRepository {

    override fun getCategoryStatistics(
        filter: StatisticsFilter
    ): Flow<Resource<List<CategoryStatistic>>> {
        return try {
            val year = filter.year.toString()
            val month = String.format("%02d", filter.month)
            
            when (filter.type) {
                StatisticsType.EXPENSE -> {
                    getCategoryStatisticsForType(year, month, TransactionType.EXPENSE)
                }
                StatisticsType.INCOME -> {
                    getCategoryStatisticsForType(year, month, TransactionType.INCOME)
                }
                StatisticsType.BOTH -> {
                    combine(
                        statisticsDao.getCategoryStatistics(year, month, TransactionType.EXPENSE),
                        statisticsDao.getCategoryStatistics(year, month, TransactionType.INCOME)
                    ) { expenses, income ->
                        // Combine both types
                        val combined = (expenses + income)
                            .groupBy { it.categoryId }
                            .map { (categoryId, items) ->
                                CategoryStatisticEntity(
                                    categoryId = categoryId,
                                    totalAmount = items.sumOf { it.totalAmount }
                                )
                            }
                        combined
                    }.let { combinedFlow ->
                        mapToCategoryStatistics(combinedFlow)
                    }
                }
            }
        } catch (e: Exception) {
            flow { emit(Resource.Error(e.localizedMessage ?: "Failed to load statistics")) }
        }
    }

    private fun getCategoryStatisticsForType(
        year: String,
        month: String,
        type: TransactionType
    ): Flow<Resource<List<CategoryStatistic>>> {
        return mapToCategoryStatistics(
            statisticsDao.getCategoryStatistics(year, month, type)
        )
    }

    private fun mapToCategoryStatistics(
        entityFlow: Flow<List<com.example.exspensify.data.local.dao.CategoryStatisticEntity>>
    ): Flow<Resource<List<CategoryStatistic>>> {
        return combine(
            entityFlow,
            categoryDao.getAllCategories()
        ) { statistics, categories ->
            val total = statistics.sumOf { it.totalAmount }

            statistics.map { stat ->
                val category = categories.find { it.id == stat.categoryId }

                // Safe color formatting
                val colorString = try {
                    category?.let {
                        val colorValue = 0xFF000000 or (it.color and 0x00FFFFFF)
                        String.format("#%08X", colorValue)
                    }
                } catch (e: Exception) {
                    null
                } ?: "#FF999999"

                CategoryStatistic(
                    categoryId = stat.categoryId.toString(),
                    categoryName = category?.name ?: "Unknown",
                    categoryIcon = category?.icon ?: "📦",
                    categoryColor = colorString,
                    amount = stat.totalAmount,
                    percentage = if (total > 0) ((stat.totalAmount / total) * 100).toFloat() else 0f
                )
            }
        }.map { Resource.Success(it) }
    }

    override fun getDailyStatistics(
        filter: StatisticsFilter
    ): Flow<Resource<List<DailyStatistic>>> {
        return try {
            val year = filter.year.toString()
            val month = String.format("%02d", filter.month)
            val daysInMonth = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                YearMonth.of(filter.year, filter.month).lengthOfMonth()
            } else {
                TODO("VERSION.SDK_INT < O")
            }

            when (filter.type) {
                StatisticsType.EXPENSE -> {
                    statisticsDao.getDailyStatistics(year, month, TransactionType.EXPENSE)
                        .map { entities ->
                            Resource.Success(fillMissingDays(entities, daysInMonth, isExpense = true))
                        }
                }
                StatisticsType.INCOME -> {
                    statisticsDao.getDailyStatistics(year, month, TransactionType.INCOME)
                        .map { entities ->
                            Resource.Success(fillMissingDays(entities, daysInMonth, isExpense = false))
                        }
                }
                StatisticsType.BOTH -> {
                    combine(
                        statisticsDao.getDailyStatistics(year, month, TransactionType.EXPENSE),
                        statisticsDao.getDailyStatistics(year, month, TransactionType.INCOME)
                    ) { expenses, income ->
                        val expenseMap = expenses.associateBy { it.dayOfMonth }
                        val incomeMap = income.associateBy { it.dayOfMonth }
                        
                        (1..daysInMonth).map { day ->
                            DailyStatistic(
                                day = day,
                                expenseAmount = expenseMap[day]?.totalAmount ?: 0.0,
                                incomeAmount = incomeMap[day]?.totalAmount ?: 0.0
                            )
                        }
                    }.map { Resource.Success(it) }
                }
            }
        } catch (e: Exception) {
            flow { emit(Resource.Error(e.localizedMessage ?: "Failed to load daily statistics")) }
        }
    }

    private fun fillMissingDays(
        entities: List<com.example.exspensify.data.local.dao.DailyStatisticEntity>,
        daysInMonth: Int,
        isExpense: Boolean
    ): List<DailyStatistic> {
        val entityMap = entities.associateBy { it.dayOfMonth }
        return (1..daysInMonth).map { day ->
            val amount = entityMap[day]?.totalAmount ?: 0.0
            DailyStatistic(
                day = day,
                expenseAmount = if (isExpense) amount else 0.0,
                incomeAmount = if (!isExpense) amount else 0.0
            )
        }
    }

    override fun getMonthlyStatistics(
        filter: StatisticsFilter
    ): Flow<Resource<List<MonthlyStatistic>>> {
        return try {
            val year = filter.year.toString()
            val monthNames = listOf(
                "Jan", "Feb", "Mar", "Apr", "May", "Jun",
                "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"
            )
            
            when (filter.type) {
                StatisticsType.EXPENSE -> {
                    statisticsDao.getMonthlyStatistics(year, TransactionType.EXPENSE)
                        .map { entities ->
                            Resource.Success(fillMissingMonths(entities, monthNames, isExpense = true))
                        }
                }
                StatisticsType.INCOME -> {
                    statisticsDao.getMonthlyStatistics(year, TransactionType.INCOME)
                        .map { entities ->
                            Resource.Success(fillMissingMonths(entities, monthNames, isExpense = false))
                        }
                }
                StatisticsType.BOTH -> {
                    combine(
                        statisticsDao.getMonthlyStatistics(year, TransactionType.EXPENSE),
                        statisticsDao.getMonthlyStatistics(year, TransactionType.INCOME)
                    ) { expenses, income ->
                        val expenseMap = expenses.associateBy { it.month }
                        val incomeMap = income.associateBy { it.month }
                        
                        (1..12).map { month ->
                            MonthlyStatistic(
                                month = month,
                                monthName = monthNames[month - 1],
                                expenseAmount = expenseMap[month]?.totalAmount ?: 0.0,
                                incomeAmount = incomeMap[month]?.totalAmount ?: 0.0
                            )
                        }
                    }.map { Resource.Success(it) }
                }
            }
        } catch (e: Exception) {
            flow { emit(Resource.Error(e.localizedMessage ?: "Failed to load monthly statistics")) }
        }
    }

    private fun fillMissingMonths(
        entities: List<com.example.exspensify.data.local.dao.MonthlyStatisticEntity>,
        monthNames: List<String>,
        isExpense: Boolean
    ): List<MonthlyStatistic> {
        val entityMap = entities.associateBy { it.month }
        return (1..12).map { month ->
            val amount = entityMap[month]?.totalAmount ?: 0.0
            MonthlyStatistic(
                month = month,
                monthName = monthNames[month - 1],
                expenseAmount = if (isExpense) amount else 0.0,
                incomeAmount = if (!isExpense) amount else 0.0
            )
        }
    }
}