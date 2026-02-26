package com.example.exspensify.data.repository

import android.content.Context
import android.net.Uri
import androidx.room.withTransaction
import com.example.exspensify.core.util.Resource
import com.example.exspensify.data.local.dao.CategoryDao
import com.example.exspensify.data.local.dao.TransactionDao
import com.example.exspensify.data.local.database.DefaultCategories
import com.example.exspensify.data.local.database.ExpensifyDatabase
import com.example.exspensify.data.local.entity.TransactionEntity
import com.example.exspensify.data.local.entity.TransactionType
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
        return withContext(Dispatchers.IO) {
            try {
                val allCategories = categoryDao.getAllCategoriesOnce()
                val categoryByName = allCategories.associateBy { it.name.trim().lowercase() }
                val otherCategory = allCategories.find { it.name.equals("other", ignoreCase = true) }
                    ?: allCategories.firstOrNull()
                    ?: return@withContext Resource.Error("No categories available for import")

                val toInsert = mutableListOf<TransactionEntity>()
                var skipped = 0

                context.contentResolver.openInputStream(uri)?.use { stream ->
                    stream.bufferedReader().use { reader ->
                        val headerLine = reader.readLine()
                            ?: return@withContext Resource.Error("File is empty")
                        val headers = parseCsvLine(headerLine).map { it.trim().lowercase() }

                        val dateIdx = headers.indexOf("date")
                        val titleIdx = headers.indexOf("title")
                        val amountIdx = headers.indexOf("amount")
                        val typeIdx = headers.indexOf("type")
                        val categoryIdx = headers.indexOf("category")
                        val descIdx = headers.indexOf("description")

                        if (listOf(dateIdx, titleIdx, amountIdx, typeIdx).any { it == -1 }) {
                            return@withContext Resource.Error(
                                "Invalid CSV: missing required columns (Date, Title, Amount, Type)"
                            )
                        }

                        val dateFormatter = java.time.format.DateTimeFormatter
                            .ofPattern("yyyy-MM-dd HH:mm:ss")

                        reader.forEachLine { line ->
                            if (line.isBlank()) return@forEachLine
                            try {
                                val cols = parseCsvLine(line)
                                val required = maxOf(dateIdx, titleIdx, amountIdx, typeIdx)
                                if (cols.size <= required) { skipped++; return@forEachLine }

                                val date = java.time.LocalDateTime.parse(
                                    cols[dateIdx].trim(), dateFormatter
                                )
                                val title = cols[titleIdx].trim()
                                if (title.isBlank()) { skipped++; return@forEachLine }

                                val amount = cols[amountIdx].trim().toDoubleOrNull()
                                if (amount == null || amount <= 0) { skipped++; return@forEachLine }

                                val type = when (cols[typeIdx].trim().uppercase()) {
                                    "INCOME" -> TransactionType.INCOME
                                    "EXPENSE" -> TransactionType.EXPENSE
                                    else -> { skipped++; return@forEachLine }
                                }

                                val catName = if (categoryIdx != -1 && categoryIdx < cols.size) {
                                    cols[categoryIdx].trim().lowercase()
                                } else ""
                                val resolvedCategory = categoryByName[catName] ?: otherCategory

                                val description = if (descIdx != -1 && descIdx < cols.size) {
                                    cols[descIdx].trim().ifBlank { null }
                                } else null

                                toInsert.add(
                                    TransactionEntity(
                                        id = 0L,
                                        title = title,
                                        amount = amount,
                                        type = type,
                                        categoryId = resolvedCategory!!.id,
                                        date = date,
                                        description = description
                                    )
                                )
                            } catch (e: Exception) {
                                skipped++
                            }
                        }
                    }
                } ?: return@withContext Resource.Error("Could not open file")

                if (toInsert.isEmpty()) {
                    return@withContext Resource.Error(
                        "No valid transactions found" +
                                if (skipped > 0) " ($skipped rows skipped)" else ""
                    )
                }

                database.withTransaction {
                    transactionDao.insertAll(toInsert)
                }

                android.util.Log.i(
                    "DataManagement",
                    "Imported ${toInsert.size} transactions, skipped $skipped"
                )
                Resource.Success(toInsert.size)

            } catch (e: java.io.IOException) {
                android.util.Log.e("DataManagement", "CSV import IO error", e)
                Resource.Error("Failed to read file: ${e.localizedMessage}")
            } catch (e: Exception) {
                android.util.Log.e("DataManagement", "CSV import error", e)
                Resource.Error(e.localizedMessage ?: "Failed to import transactions")
            }
        }
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

    private fun parseCsvLine(line: String): List<String> {
        val result = mutableListOf<String>()
        val current = StringBuilder()
        var inQuotes = false
        for (ch in line) {
            when {
                ch == '"' -> inQuotes = !inQuotes
                ch == ',' && !inQuotes -> {
                    result.add(current.toString())
                    current.clear()
                }
                else -> current.append(ch)
            }
        }
        result.add(current.toString())
        return result
    }
}