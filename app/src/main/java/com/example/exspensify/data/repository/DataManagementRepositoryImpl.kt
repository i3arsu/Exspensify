package com.example.exspensify.data.repository

import android.content.Context
import android.net.Uri
import androidx.room.withTransaction
import com.example.exspensify.core.util.Resource
import com.example.exspensify.data.local.dao.CategoryDao
import com.example.exspensify.data.local.dao.TransactionDao
import com.example.exspensify.data.local.database.DefaultCategories
import com.example.exspensify.data.local.database.ExpensifyDatabase
import com.example.exspensify.domain.repository.DataManagementRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DataManagementRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val database: ExpensifyDatabase,
    private val transactionDao: TransactionDao,
    private val categoryDao: CategoryDao
) : DataManagementRepository {

    override suspend fun resetDatabase(): Resource<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                // Use withTransaction for suspend functions (Room KTX)
                database.withTransaction {
                    // Delete all transactions
                    transactionDao.deleteAll()

                    // Delete all custom categories (keep default ones)
                    categoryDao.deleteAllCustomCategories()

                    // Re-seed default categories if they were somehow deleted
                    val defaultCount = categoryDao.getDefaultCategoryCount()
                    if (defaultCount == 0) {
                        categoryDao.insertAll(DefaultCategories.defaults)
                    }
                }

                Resource.Success(Unit)
            } catch (e: Exception) {
                android.util.Log.e("DataManagement", "Error resetting database", e)
                Resource.Error(e.localizedMessage ?: "Failed to reset database")
            }
        }
    }

    override suspend fun exportTransactionsToCSV(uri: Uri): Resource<Int> {
        return withContext(Dispatchers.IO) {
            try {
                val transactions = transactionDao.getAllTransactionsOnce()
                val categories = categoryDao.getAllCategoriesOnce()

                if (transactions.isEmpty()) {
                    return@withContext Resource.Error("No transactions to export")
                }

                // Create category lookup map
                val categoryMap = categories.associateBy { it.id }

                // Write CSV
                context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                    outputStream.bufferedWriter().use { writer ->
                        // Write header
                        writer.write("Date,Title,Amount,Type,Category,Description\n")

                        // Write data
                        val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
                        transactions.forEach { transaction ->
                            val category = categoryMap[transaction.categoryId]
                            val line = buildString {
                                append(transaction.date.format(dateFormatter))
                                append(",")
                                append(escapeCsvValue(transaction.title))
                                append(",")
                                append(transaction.amount)
                                append(",")
                                append(transaction.type.name)
                                append(",")
                                append(escapeCsvValue(category?.name ?: "Unknown"))
                                append(",")
                                append(escapeCsvValue(transaction.description ?: ""))
                                append("\n")
                            }
                            writer.write(line)
                        }
                    }
                } ?: return@withContext Resource.Error("Failed to open file for writing")

                Resource.Success(transactions.size)
            } catch (e: IOException) {
                android.util.Log.e("DataManagement", "Error exporting CSV", e)
                Resource.Error("Failed to write file: ${e.localizedMessage}")
            } catch (e: Exception) {
                android.util.Log.e("DataManagement", "Error exporting CSV", e)
                Resource.Error(e.localizedMessage ?: "Failed to export transactions")
            }
        }
    }

    override suspend fun getTransactionCount(): Int {
        return withContext(Dispatchers.IO) {
            try {
                transactionDao.getTransactionCount()
            } catch (e: Exception) {
                0
            }
        }
    }

    override suspend fun importTransactionsFromCSV(uri: Uri): Resource<Int> {
        // TODO: Implement CSV import functionality
        // This would involve:
        // 1. Reading CSV file from URI
        // 2. Parsing CSV rows
        // 3. Validating data
        // 4. Matching categories or creating new ones
        // 5. Inserting transactions in a transaction block
        // 6. Handling duplicates
        return Resource.Error("Import feature not yet implemented")
    }

    private fun escapeCsvValue(value: String): String {
        // Escape quotes and wrap in quotes if contains comma or newline
        val escaped = value.replace("\"", "\"\"")
        return if (escaped.contains(",") || escaped.contains("\n") || escaped.contains("\"")) {
            "\"$escaped\""
        } else {
            escaped
        }
    }
}