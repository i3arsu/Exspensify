package com.example.exspensify.ui.categories.addedits

data class AddEditCategoryUiState(
    val id: String? = null,
    val name: String = "",
    val selectedIcon: String = "📦",
    val selectedColor: Long = 0xFF9E9E9EL,
    val isLoading: Boolean = false,
    val nameError: String? = null,
    val error: String? = null,
    val isDefault: Boolean = false
)

val AddEditCategoryUiState.isValid: Boolean
    get() = nameError == null && name.isNotBlank()