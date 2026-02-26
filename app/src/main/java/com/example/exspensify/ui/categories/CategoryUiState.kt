package com.example.exspensify.ui.categories

import com.example.exspensify.domain.model.Category
import com.example.exspensify.domain.model.TransactionType

data class CategoryUiState(
    val allCategories: List<Category> = emptyList(),
    val filteredCategories: List<Category> = emptyList(),
    val selectedType: TransactionType? = null,
    val isLoading: Boolean = false,
    val error: String? = null
)