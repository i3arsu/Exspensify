package com.example.exspensify.data.repository

import com.example.exspensify.core.util.Resource
import com.example.exspensify.data.local.dao.TransactionDao
import com.example.exspensify.data.mapper.toTransaction
import com.example.exspensify.data.mapper.toTransactionEntity
import com.example.exspensify.domain.model.Transaction
import com.example.exspensify.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class TransactionRepositoryImpl @Inject constructor(
    private val transactionDao: TransactionDao
) : TransactionRepository {

    override fun getAllTransactions(): Flow<Resource<List<Transaction>>> {
        return try {
            transactionDao.getAllTransactions().map { entities ->
                Resource.Success(entities.map { it.toTransaction() })
            }
        } catch (e: Exception) {
            kotlinx.coroutines.flow.flow {
                emit(Resource.Error(e.localizedMessage ?: "An error occurred"))
            }
        }
    }

    override fun getTransactionById(id: String): Flow<Resource<Transaction>> {
        return try {
            transactionDao.getTransactionByIdFlow(id.toLong()).map { entity ->
                entity?.let {
                    Resource.Success(it.toTransaction())
                } ?: Resource.Error("Transaction not found")
            }
        } catch (e: Exception) {
            kotlinx.coroutines.flow.flow {
                emit(Resource.Error(e.localizedMessage ?: "An error occurred"))
            }
        }
    }

    override suspend fun insertTransaction(transaction: Transaction): Resource<Unit> {
        return try {
            transactionDao.insert(transaction.toTransactionEntity())
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Failed to insert transaction")
        }
    }

    override suspend fun updateTransaction(transaction: Transaction): Resource<Unit> {
        return try {
            transactionDao.update(transaction.toTransactionEntity())
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Failed to update transaction")
        }
    }

    override suspend fun deleteTransaction(id: String): Resource<Unit> {
        return try {
            transactionDao.deleteById(id.toLong())
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Failed to delete transaction")
        }
    }

    override fun getRecentTransactions(limit: Int): Flow<Resource<List<Transaction>>> {
        return try {
            transactionDao.getRecentTransactions(limit).map { entities ->
                Resource.Success(entities.map { it.toTransaction() })
            }
        } catch (e: Exception) {
            kotlinx.coroutines.flow.flow {
                emit(Resource.Error(e.localizedMessage ?: "An error occurred"))
            }
        }
    }
}