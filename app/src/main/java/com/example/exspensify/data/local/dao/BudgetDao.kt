package com.example.exspensify.data.local.dao

import androidx.room.*
import com.example.exspensify.data.local.entity.BudgetEntity
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
}