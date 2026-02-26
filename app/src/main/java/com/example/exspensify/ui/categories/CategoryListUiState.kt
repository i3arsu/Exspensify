package com.example.exspensify.ui.categories

import com.example.exspensify.domain.model.Category

data class CategoryListUiState(
    val categories: List<Category> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)