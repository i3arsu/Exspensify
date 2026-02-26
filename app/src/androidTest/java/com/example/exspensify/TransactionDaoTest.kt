package com.example.exspensify

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.example.exspensify.data.local.dao.CategoryDao
import com.example.exspensify.data.local.dao.TransactionDao
import com.example.exspensify.data.local.database.ExpensifyDatabase
import com.example.exspensify.data.local.entity.CategoryEntity
import com.example.exspensify.data.local.entity.TransactionEntity
import com.example.exspensify.data.local.entity.TransactionType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDateTime

@RunWith(AndroidJUnit4::class)
@SmallTest
class TransactionDaoTest {

    private var database: ExpensifyDatabase? = null
    private lateinit var transactionDao: TransactionDao
    private lateinit var categoryDao: CategoryDao

    @Before
    fun setupDatabase() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(
            context,
            ExpensifyDatabase::class.java
        )
            .allowMainThreadQueries()
            .build()

        transactionDao = database!!.transactionDao()
        categoryDao = database!!.categoryDao()
    }

    @After
    fun closeDatabase() {
        database?.close()
    }

    @Test
    fun insertCategory_andRetrieve() {
        runTest {
            // Given
            val category = CategoryEntity(
                name = "Test Category",
                icon = "🧪",
                color = 0xFFFF0000L,
                isDefault = true
            )

            // When
            val id = categoryDao.insert(category)
            val retrieved = categoryDao.getCategoryById(id)

            // Then
            assertNotNull(retrieved)
            assertEquals("Test Category", retrieved?.name)
        }
    }

    @Test
    fun insertTransaction_andRetrieve() {
        runTest {
            // Given - First insert a category
            val category = CategoryEntity(
                name = "Test Category",
                icon = "🧪",
                color = 0xFFFF0000L,
                isDefault = true
            )
            val categoryId = categoryDao.insert(category)

            val transaction = TransactionEntity(
                title = "Test Transaction",
                amount = 100.0,
                type = TransactionType.EXPENSE,
                categoryId = categoryId,  // Use the Long ID directly
                date = LocalDateTime.now()
            )

            // When
            val transactionId = transactionDao.insert(transaction)
            val retrieved = transactionDao.getTransactionById(transactionId)

            // Then
            assertNotNull(retrieved)
            assertEquals("Test Transaction", retrieved?.title)
            retrieved?.amount?.let { assertEquals(100.0, it, 0.01) }
        }
    }

    @Test
    fun insertMultipleTransactions_andGetAll() {
        runTest {
            // Given
            val category = CategoryEntity(
                name = "Test Category",
                icon = "🧪",
                color = 0xFFFF0000L,
                isDefault = true
            )
            val categoryId = categoryDao.insert(category)

            val transactions = listOf(
                TransactionEntity(
                    title = "Transaction 1",
                    amount = 100.0,
                    type = TransactionType.EXPENSE,
                    categoryId = categoryId,
                    date = LocalDateTime.now()
                ),
                TransactionEntity(
                    title = "Transaction 2",
                    amount = 200.0,
                    type = TransactionType.INCOME,
                    categoryId = categoryId,
                    date = LocalDateTime.now()
                )
            )

            // When
            transactionDao.insertAll(transactions)
            val allTransactions = transactionDao.getAllTransactions().first()

            // Then
            assertEquals(2, allTransactions.size)
        }
    }

    @Test
    fun deleteTransaction_removesFromDatabase() {
        runTest {
            // Given
            val category = CategoryEntity(
                name = "Test Category",
                icon = "🧪",
                color = 0xFFFF0000L,
                isDefault = true
            )
            val categoryId = categoryDao.insert(category)

            val transaction = TransactionEntity(
                title = "Delete Me",
                amount = 50.0,
                type = TransactionType.EXPENSE,
                categoryId = categoryId,
                date = LocalDateTime.now()
            )
            val id = transactionDao.insert(transaction)

            // When
            transactionDao.deleteById(id)
            val retrieved = transactionDao.getTransactionById(id)

            // Then
            assertEquals(null, retrieved)
        }
    }

    @Test
    fun updateTransaction() {
        runTest {
            // Given
            val category = CategoryEntity(
                name = "Test Category",
                icon = "🧪",
                color = 0xFFFF0000L,
                isDefault = true
            )
            val categoryId = categoryDao.insert(category)

            val transaction = TransactionEntity(
                title = "Original",
                amount = 100.0,
                type = TransactionType.EXPENSE,
                categoryId = categoryId,
                date = LocalDateTime.now()
            )
            val id = transactionDao.insert(transaction)

            // When
            val updated = transaction.copy(
                id = id,
                title = "Updated",
                amount = 200.0
            )
            transactionDao.update(updated)
            val retrieved = transactionDao.getTransactionById(id)

            // Then
            assertEquals("Updated", retrieved?.title)
            retrieved?.amount?.let { assertEquals(200.0, it, 0.01) }
        }
    }

    @Test
    fun getRecentTransactions() {
        runTest {
            // Given
            val category = CategoryEntity(
                name = "Test Category",
                icon = "🧪",
                color = 0xFFFF0000L,
                isDefault = true
            )
            val categoryId = categoryDao.insert(category)

            val transactions = (1..5).map { i ->
                TransactionEntity(
                    title = "Transaction $i",
                    amount = i * 10.0,
                    type = TransactionType.EXPENSE,
                    categoryId = categoryId,
                    date = LocalDateTime.now().minusDays(i.toLong())
                )
            }
            transactionDao.insertAll(transactions)

            // When
            val recent = transactionDao.getRecentTransactions(3).first()

            // Then
            assertEquals(3, recent.size)
            assertEquals("Transaction 1", recent[0].title) // Most recent first
        }
    }

    @Test
    fun getTransactionsByType() {
        runTest {
            // Given
            val category = CategoryEntity(
                name = "Test Category",
                icon = "🧪",
                color = 0xFFFF0000L,
                isDefault = true
            )
            val categoryId = categoryDao.insert(category)

            val transactions = listOf(
                TransactionEntity(
                    title = "Expense 1",
                    amount = 100.0,
                    type = TransactionType.EXPENSE,
                    categoryId = categoryId,
                    date = LocalDateTime.now()
                ),
                TransactionEntity(
                    title = "Income 1",
                    amount = 200.0,
                    type = TransactionType.INCOME,
                    categoryId = categoryId,
                    date = LocalDateTime.now()
                ),
                TransactionEntity(
                    title = "Expense 2",
                    amount = 150.0,
                    type = TransactionType.EXPENSE,
                    categoryId = categoryId,
                    date = LocalDateTime.now()
                )
            )
            transactionDao.insertAll(transactions)

            // When
            val expenses = transactionDao.getTransactionsByType(TransactionType.EXPENSE).first()
            val income = transactionDao.getTransactionsByType(TransactionType.INCOME).first()

            // Then
            assertEquals(2, expenses.size)
            assertEquals(1, income.size)
        }
    }

    @Test
    fun getTransactionsByCategory() {
        runTest {
            // Given
            val category1 = CategoryEntity(
                name = "Food",
                icon = "🍔",
                color = 0xFFFF0000L,
                isDefault = true
            )
            val category1Id = categoryDao.insert(category1)

            val category2 = CategoryEntity(
                name = "Transport",
                icon = "🚗",
                color = 0xFF00FF00L,
                isDefault = true
            )
            val category2Id = categoryDao.insert(category2)

            val transactions = listOf(
                TransactionEntity(
                    title = "Lunch",
                    amount = 15.0,
                    type = TransactionType.EXPENSE,
                    categoryId = category1Id,
                    date = LocalDateTime.now()
                ),
                TransactionEntity(
                    title = "Dinner",
                    amount = 30.0,
                    type = TransactionType.EXPENSE,
                    categoryId = category1Id,
                    date = LocalDateTime.now()
                ),
                TransactionEntity(
                    title = "Taxi",
                    amount = 20.0,
                    type = TransactionType.EXPENSE,
                    categoryId = category2Id,
                    date = LocalDateTime.now()
                )
            )
            transactionDao.insertAll(transactions)

            // When
            val foodTransactions = transactionDao.getTransactionsByCategory(category1Id).first()
            val transportTransactions = transactionDao.getTransactionsByCategory(category2Id).first()

            // Then
            assertEquals(2, foodTransactions.size)
            assertEquals(1, transportTransactions.size)
        }
    }
}