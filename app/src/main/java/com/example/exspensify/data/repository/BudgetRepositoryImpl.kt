package com.example.exspensify.data.repository

import android.os.Build
import androidx.annotation.RequiresApi
import com.example.exspensify.core.util.Resource
import com.example.exspensify.data.local.dao.BudgetDao
import com.example.exspensify.data.local.mapper.toBudget
import com.example.exspensify.data.local.mapper.toBudgetEntity
import com.example.exspensify.domain.model.Budget
import com.example.exspensify.domain.model.BudgetPeriod
import com.example.exspensify.domain.repository.BudgetRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
@RequiresApi(Build.VERSION_CODES.O)
class BudgetRepositoryImpl @Inject constructor(
    private val budgetDao: BudgetDao
) : BudgetRepository {

    override fun getAllBudgets(): Flow<Resource<List<Budget>>> =
        budgetDao.getAllBudgetsWithSpent().map { list ->
            try {
                Resource.Success(list.map { it.toBudget() })
            } catch (e: Exception) {
                Resource.Error(e.localizedMessage ?: "Failed to load budgets")
            }
        }

    override fun getBudgetById(id: String): Flow<Resource<Budget>> =
        budgetDao.getAllBudgetsWithSpent().map { list ->
            try {
                val found = list.find { it.id == id.toLongOrNull() }
                    ?: return@map Resource.Error("Budget not found")
                Resource.Success(found.toBudget())
            } catch (e: Exception) {
                Resource.Error(e.localizedMessage ?: "Failed to load budget")
            }
        }

    override suspend fun insertBudget(budget: Budget): Resource<Unit> = try {
        val (start, end) = budget.period.currentPeriodRange()
        budgetDao.insert(budget.toBudgetEntity(start, end))
        Resource.Success(Unit)
    } catch (e: Exception) {
        Resource.Error(e.localizedMessage ?: "Failed to save budget")
    }

    override suspend fun updateBudget(budget: Budget): Resource<Unit> = try {
        val (start, end) = budget.period.currentPeriodRange()
        budgetDao.update(budget.toBudgetEntity(start, end))
        Resource.Success(Unit)
    } catch (e: Exception) {
        Resource.Error(e.localizedMessage ?: "Failed to update budget")
    }

    override suspend fun deleteBudget(id: String): Resource<Unit> = try {
        val longId = id.toLongOrNull()
            ?: return Resource.Error("Invalid budget id")
        budgetDao.deleteById(longId)
        Resource.Success(Unit)
    } catch (e: Exception) {
        Resource.Error(e.localizedMessage ?: "Failed to delete budget")
    }
}

@RequiresApi(Build.VERSION_CODES.O)
private fun BudgetPeriod.currentPeriodRange(): Pair<Long, Long> {
    val now = LocalDate.now()
    val zone = ZoneId.systemDefault()
    val (start, end) = when (this) {
        BudgetPeriod.WEEKLY -> {
            val monday = now.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
            monday to monday.plusDays(6)
        }
        BudgetPeriod.MONTHLY -> {
            val first = now.withDayOfMonth(1)
            first to now.with(TemporalAdjusters.lastDayOfMonth())
        }
        BudgetPeriod.YEARLY -> {
            val first = now.withDayOfYear(1)
            first to now.with(TemporalAdjusters.lastDayOfYear())
        }
    }
    return start.atStartOfDay(zone).toInstant().toEpochMilli() to
            end.atTime(23, 59, 59).atZone(zone).toInstant().toEpochMilli()
}
