package com.example.exspensify.data.local.dao

import androidx.room.*
import com.example.exspensify.data.local.entity.TransactionEntity
import com.example.exspensify.data.local.entity.TransactionType
import kotlinx.coroutines.flow.Flow
import java.time.LocalDateTime

@Dao
interface TransactionDao {
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(transaction: TransactionEntity): Long
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(transactions: List<TransactionEntity>)
    
    @Update
    suspend fun update(transaction: TransactionEntity)
    
    @Delete
    suspend fun delete(transaction: TransactionEntity)
    
    @Query("DELETE FROM transactions WHERE id = :id")
    suspend fun deleteById(id: Long)
    
    @Query("SELECT * FROM transactions ORDER BY date DESC")
    fun getAllTransactions(): Flow<List<TransactionEntity>>
    
    @Query("SELECT * FROM transactions WHERE id = :id")
    suspend fun getTransactionById(id: Long): TransactionEntity?
    
    @Query("SELECT * FROM transactions WHERE id = :id")
    fun getTransactionByIdFlow(id: Long): Flow<TransactionEntity?>
    
    @Query("SELECT * FROM transactions ORDER BY date DESC LIMIT :limit")
    fun getRecentTransactions(limit: Int): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions ORDER BY date DESC")
    suspend fun getAllTransactionsOnce(): List<TransactionEntity>
    
    // Query by date range
    @Query("""
        SELECT * FROM transactions 
        WHERE date BETWEEN :startDate AND :endDate 
        ORDER BY date DESC
    """)
    fun getTransactionsByDateRange(
        startDate: LocalDateTime,
        endDate: LocalDateTime
    ): Flow<List<TransactionEntity>>
    
    // Query by type
    @Query("SELECT * FROM transactions WHERE type = :type ORDER BY date DESC")
    fun getTransactionsByType(type: TransactionType): Flow<List<TransactionEntity>>
    
    // Query by category
    @Query("SELECT * FROM transactions WHERE categoryId = :categoryId ORDER BY date DESC")
    fun getTransactionsByCategory(categoryId: Long): Flow<List<TransactionEntity>>
    
    // Sum by month
    @Query("""
        SELECT SUM(amount) FROM transactions 
        WHERE strftime('%Y-%m', date/1000, 'unixepoch', 'localtime') = :yearMonth
        AND type = :type
    """)
    suspend fun getSumByMonth(yearMonth: String, type: TransactionType): Double?
    
    // Sum by month (Flow version)
    @Query("""
        SELECT SUM(amount) FROM transactions 
        WHERE strftime('%Y-%m', date/1000, 'unixepoch', 'localtime') = :yearMonth
        AND type = :type
    """)
    fun getSumByMonthFlow(yearMonth: String, type: TransactionType): Flow<Double?>
    
    // Sum by category
    @Query("""
        SELECT SUM(amount) FROM transactions 
        WHERE categoryId = :categoryId
    """)
    fun getSumByCategory(categoryId: Long): Flow<Double?>
    
    // Sum by category and date range
    @Query("""
        SELECT SUM(amount) FROM transactions 
        WHERE categoryId = :categoryId 
        AND date BETWEEN :startDate AND :endDate
    """)
    fun getSumByCategoryAndDateRange(
        categoryId: Long,
        startDate: LocalDateTime,
        endDate: LocalDateTime
    ): Flow<Double?>
    
    // Sum by day
    @Query("""
        SELECT SUM(amount) FROM transactions 
        WHERE date(date/1000, 'unixepoch', 'localtime') = :date
        AND type = :type
    """)
    fun getSumByDay(date: String, type: TransactionType): Flow<Double?>
    
    // Get daily sums for a month
    @Query("""
        SELECT date(date/1000, 'unixepoch', 'localtime') as day, 
               SUM(CASE WHEN type = 'EXPENSE' THEN amount ELSE 0 END) as expenses,
               SUM(CASE WHEN type = 'INCOME' THEN amount ELSE 0 END) as income
        FROM transactions 
        WHERE strftime('%Y-%m', date/1000, 'unixepoch', 'localtime') = :yearMonth
        GROUP BY day
        ORDER BY day
    """)
    fun getDailySumsByMonth(yearMonth: String): Flow<List<DailySummary>>
    
    // Get category sums
    @Query("""
        SELECT c.id, c.name, c.icon, c.color, SUM(t.amount) as total
        FROM transactions t
        INNER JOIN categories c ON t.categoryId = c.id
        WHERE t.type = :type
        AND t.date BETWEEN :startDate AND :endDate
        GROUP BY c.id, c.name, c.icon, c.color
        ORDER BY total DESC
    """)
    fun getCategorySums(
        type: TransactionType,
        startDate: LocalDateTime,
        endDate: LocalDateTime
    ): Flow<List<CategorySummary>>
    
    @Query("DELETE FROM transactions")
    suspend fun deleteAll()

    @Query("SELECT COUNT(*) FROM transactions")
    suspend fun getTransactionCount(): Int
}

// Data classes for complex queries
data class DailySummary(
    val day: String,
    val expenses: Double,
    val income: Double
)

data class CategorySummary(
    val id: Long,
    val name: String,
    val icon: String,
    val color: String,
    val total: Double
)