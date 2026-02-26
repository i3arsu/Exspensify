package com.example.exspensify.data.repository

import com.example.exspensify.core.util.Resource
import com.example.exspensify.data.local.dao.CategoryDao
import com.example.exspensify.data.local.entity.CategoryEntity
import com.example.exspensify.data.mapper.toCategory
import com.example.exspensify.domain.model.Category
import com.example.exspensify.domain.model.TransactionType
import com.example.exspensify.domain.repository.CategoryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class CategoryRepositoryImpl @Inject constructor(
    private val categoryDao: CategoryDao
) : CategoryRepository {

    override fun getAllCategories(): Flow<Resource<List<Category>>> {
        return try {
            categoryDao.getAllCategories().map { entities ->
                Resource.Success(entities.map { it.toDomainModel() })
            }
        } catch (e: Exception) {
            flow { emit(Resource.Error(e.localizedMessage ?: "Failed to load categories")) }
        }
    }

    override fun getCategoryById(id: String): Flow<Resource<Category>> {
        return try {
            categoryDao.getCategoryByIdFlow(id.toLong()).map { entity ->
                entity?.let {
                    Resource.Success(it.toDomainModel())
                } ?: Resource.Error("Category not found")
            }
        } catch (e: Exception) {
            flow { emit(Resource.Error(e.localizedMessage ?: "Failed to load category")) }
        }
    }

    override suspend fun getCategoryByIdOnce(id: String): Resource<Category> {
        return try {
            val entity = categoryDao.getCategoryById(id.toLong())
            if (entity != null) {
                Resource.Success(entity.toDomainModel())
            } else {
                Resource.Error("Category not found")
            }
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Failed to load category")
        }
    }

    override suspend fun insertCategory(
        name: String,
        icon: String,
        color: Long
    ): Resource<Unit> {
        return try {
            // Validate name
            if (name.isBlank()) {
                return Resource.Error("Category name cannot be empty")
            }

            // Check for duplicate name (case insensitive)
            val existing = categoryDao.getCategoryByName(name)
            if (existing != null) {
                return Resource.Error("Category '$name' already exists")
            }

            // Insert new category
            val entity = CategoryEntity(
                name = name.trim(),
                icon = icon,
                color = color,
                isDefault = false
            )

            val id = categoryDao.insert(entity)
            if (id > 0) {
                Resource.Success(Unit)
            } else {
                Resource.Error("Failed to create category")
            }
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Failed to create category")
        }
    }

    override suspend fun updateCategory(
        id: String,
        name: String,
        icon: String,
        color: Long
    ): Resource<Unit> {
        return try {
            val categoryId = id.toLong()

            // Get existing category
            val existing = categoryDao.getCategoryById(categoryId)
                ?: return Resource.Error("Category not found")

            // Prevent editing default categories
            if (existing.isDefault) {
                return Resource.Error("Cannot edit default categories")
            }

            // Validate name
            if (name.isBlank()) {
                return Resource.Error("Category name cannot be empty")
            }

            // Check for duplicate name (excluding current category)
            val duplicate = categoryDao.getCategoryByName(name)
            if (duplicate != null && duplicate.id != categoryId) {
                return Resource.Error("Category '$name' already exists")
            }

            // Update category
            val updated = existing.copy(
                name = name.trim(),
                icon = icon,
                color = color
            )
            categoryDao.update(updated)

            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Failed to update category")
        }
    }

    override suspend fun deleteCategory(id: String): Resource<Unit> {
        return try {
            val categoryId = id.toLong()

            // Get existing category
            val existing = categoryDao.getCategoryById(categoryId)
                ?: return Resource.Error("Category not found")

            // Prevent deleting default categories
            if (existing.isDefault) {
                return Resource.Error("Cannot delete default categories")
            }

            // Check if category is in use
            val transactionCount = categoryDao.getTransactionCountForCategory(categoryId)
            if (transactionCount > 0) {
                return Resource.Error(
                    "Cannot delete category. It is used by $transactionCount transaction(s). " +
                            "Please reassign or delete those transactions first."
                )
            }

            // Delete category
            categoryDao.deleteById(categoryId)
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Failed to delete category")
        }
    }

    override suspend fun isCategoryInUse(id: String): Boolean {
        return try {
            val count = categoryDao.getTransactionCountForCategory(id.toLong())
            count > 0
        } catch (e: Exception) {
            false
        }
    }

    private fun CategoryEntity.toDomainModel(): Category {
        return Category(
            id = this.id.toString(),
            name = this.name,
            icon = this.icon,
            color = String.format("#%08X", this.color),
            isDefault = this.isDefault
        )
    }
    override fun getCategoriesByType(type: TransactionType): Flow<Resource<List<Category>>> {
        return try {
            categoryDao.getAllCategories().map { entities ->
                val filtered = entities
                    .map { it.toCategory() }
                    .filter { it.type == type || it.type == null }
                Resource.Success(filtered)
            }
        } catch (e: Exception) {
            flow { emit(Resource.Error(e.localizedMessage ?: "Failed to load categories")) }
        }
    }
}