package com.example.exspensify.domain.repository

import com.example.exspensify.core.util.Resource
import com.example.exspensify.domain.model.Category
import com.example.exspensify.domain.model.TransactionType
import kotlinx.coroutines.flow.Flow

interface CategoryRepository {

    /**
     * Observe all categories sorted alphabetically
     */
    fun getAllCategories(): Flow<Resource<List<Category>>>

    /**
     * Observe single category by ID
     */
    fun getCategoryById(id: String): Flow<Resource<Category>>

    /**
     * Get category by ID (suspend, one-shot)
     */
    suspend fun getCategoryByIdOnce(id: String): Resource<Category>

    /**
     * Insert new category
     * Validates:
     * - Name is not empty
     * - Name is unique (case insensitive)
     */
    suspend fun insertCategory(
        name: String,
        icon: String,
        color: Long
    ): Resource<Unit>

    /**
     * Update existing category
     * Validates:
     * - Name is not empty
     * - Name is unique (case insensitive) excluding current category
     * - Cannot edit default categories
     */
    suspend fun updateCategory(
        id: String,
        name: String,
        icon: String,
        color: Long
    ): Resource<Unit>

    /**
     * Delete category
     * Validates:
     * - Cannot delete default categories
     * - Cannot delete if used by transactions
     */
    suspend fun deleteCategory(id: String): Resource<Unit>

    /**
     * Check if category is used by any transactions
     */
    suspend fun isCategoryInUse(id: String): Boolean

    /**
     * Get categories filtered by transaction type
     */
    fun getCategoriesByType(type: TransactionType): Flow<Resource<List<Category>>>
}