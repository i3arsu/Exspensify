package com.example.exspensify.domain.repository

import com.example.exspensify.core.util.Resource
import com.example.exspensify.domain.model.Transaction
import kotlinx.coroutines.flow.Flow

interface TransactionRepository {
    fun getAllTransactions(): Flow<Resource<List<Transaction>>>
    fun getTransactionById(id: String): Flow<Resource<Transaction>>
    suspend fun insertTransaction(transaction: Transaction): Resource<Unit>
    suspend fun updateTransaction(transaction: Transaction): Resource<Unit>
    suspend fun deleteTransaction(id: String): Resource<Unit>
    fun getRecentTransactions(limit: Int): Flow<Resource<List<Transaction>>>
}