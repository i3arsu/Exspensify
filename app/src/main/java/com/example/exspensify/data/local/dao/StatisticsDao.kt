package com.example.exspensify.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import com.example.exspensify.data.local.entity.TransactionType
import kotlinx.coroutines.flow.Flow

@Dao
interface StatisticsDao {
    
    /**
     * Get total amount grouped by category for a specific month and year
     * @param year e.g., 2024
     * @param month 1-12
     * @param type EXPENSE or INCOME
     */
    @Query("""
        SELECT 
            categoryId,
            SUM(amount) as totalAmount
        FROM transactions
        WHERE strftime('%Y', datetime(date/1000, 'unixepoch', 'localtime')) = :year
        AND strftime('%m', datetime(date/1000, 'unixepoch', 'localtime')) = :month
        AND type = :type
        GROUP BY categoryId
        ORDER BY totalAmount DESC
    """)
    fun getCategoryStatistics(
        year: String,
        month: String,
        type: TransactionType
    ): Flow<List<CategoryStatisticEntity>>
    
    /**
     * Get total amount grouped by day for a specific month and year
     * @param year e.g., "2024"
     * @param month e.g., "01", "02", ..., "12"
     * @param type EXPENSE or INCOME
     */
    @Query("""
        SELECT 
            CAST(strftime('%d', datetime(date/1000, 'unixepoch', 'localtime')) AS INTEGER) as dayOfMonth,
            SUM(amount) as totalAmount
        FROM transactions
        WHERE strftime('%Y', datetime(date/1000, 'unixepoch', 'localtime')) = :year
        AND strftime('%m', datetime(date/1000, 'unixepoch', 'localtime')) = :month
        AND type = :type
        GROUP BY dayOfMonth
        ORDER BY dayOfMonth ASC
    """)
    fun getDailyStatistics(
        year: String,
        month: String,
        type: TransactionType
    ): Flow<List<DailyStatisticEntity>>
    
    /**
     * Get total amount grouped by month for a specific year
     * @param year e.g., "2024"
     * @param type EXPENSE or INCOME
     */
    @Query("""
        SELECT 
            CAST(strftime('%m', datetime(date/1000, 'unixepoch', 'localtime')) AS INTEGER) as month,
            SUM(amount) as totalAmount
        FROM transactions
        WHERE strftime('%Y', datetime(date/1000, 'unixepoch', 'localtime')) = :year
        AND type = :type
        GROUP BY month
        ORDER BY month ASC
    """)
    fun getMonthlyStatistics(
        year: String,
        type: TransactionType
    ): Flow<List<MonthlyStatisticEntity>>
}

// Data classes for query results
data class CategoryStatisticEntity(
    val categoryId: Long,
    val totalAmount: Double
)

data class DailyStatisticEntity(
    val dayOfMonth: Int,
    val totalAmount: Double
)

data class MonthlyStatisticEntity(
    val month: Int,
    val totalAmount: Double
)