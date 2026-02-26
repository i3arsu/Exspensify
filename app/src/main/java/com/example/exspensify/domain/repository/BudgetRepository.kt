package com.example.exspensify.domain.repository

import com.example.exspensify.core.util.Resource
import com.example.exspensify.domain.model.Budget
import kotlinx.coroutines.flow.Flow

interface BudgetRepository {
    fun getAllBudgets(): Flow<Resource<List<Budget>>>
    fun getBudgetById(id: String): Flow<Resource<Budget>>
    suspend fun insertBudget(budget: Budget): Resource<Unit>
    suspend fun updateBudget(budget: Budget): Resource<Unit>
    suspend fun deleteBudget(id: String): Resource<Unit>
}