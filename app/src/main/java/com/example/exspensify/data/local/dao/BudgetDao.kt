package com.example.exspensify.data.local.dao

import androidx.room.*
import com.example.exspensify.data.local.entity.BudgetEntity
import com.example.exspensify.data.local.entity.BudgetPeriod
import kotlinx.coroutines.flow.Flow

@Dao
interface BudgetDao {
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(budget: BudgetEntity): Long
    
    @Update
    suspend fun update(budget: BudgetEntity)
    
    @Delete
    suspend fun delete(budget: BudgetEntity)
    
    @Query("SELECT * FROM budgets ORDER BY startDate DESC")
    fun getAllBudgets(): Flow<List<BudgetEntity>>
    
    @Query("SELECT * FROM budgets WHERE id = :id")
    suspend fun getBudgetById(id: Long): BudgetEntity?
    
    @Query("SELECT * FROM budgets WHERE categoryId = :categoryId")
    fun getBudgetsByCategory(categoryId: Long): Flow<List<BudgetEntity>>
    
    @Query("""
        SELECT * FROM budgets 
        WHERE startDate <= :currentTime 
        AND endDate >= :currentTime
    """)
    fun getActiveBudgets(currentTime: Long = System.currentTimeMillis()): Flow<List<BudgetEntity>>
    
    @Query("DELETE FROM budgets")
    suspend fun deleteAll()

    @Query("DELETE FROM budgets WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("""
        SELECT b.id, b.categoryId, b.amount, b.period, b.startDate, b.endDate, b.createdAt,
               COALESCE(SUM(CASE WHEN t.type = 'EXPENSE' THEN t.amount ELSE 0 END), 0.0) AS spentAmount,
               COALESCE(c.name, 'Unknown') AS categoryName,
               COALESCE(c.icon, '📦') AS categoryIcon
        FROM budgets b
        LEFT JOIN transactions t
          ON t.categoryId = b.categoryId
         AND t.date >= b.startDate
         AND t.date <= b.endDate
        LEFT JOIN categories c ON c.id = b.categoryId
        GROUP BY b.id
        ORDER BY b.startDate DESC
    """)
    fun getAllBudgetsWithSpent(): Flow<List<BudgetWithSpent>>
}

data class BudgetWithSpent(
    val id: Long,
    val categoryId: Long,
    val amount: Double,
    val period: BudgetPeriod,
    val startDate: Long,
    val endDate: Long,
    val createdAt: Long,
    val spentAmount: Double,
    val categoryName: String,
    val categoryIcon: String
)