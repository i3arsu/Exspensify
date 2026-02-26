package com.example.exspensify.ui.categories

sealed class CategoryListEvent {
    data class DeleteCategory(val id: String) : CategoryListEvent()
    object Refresh : CategoryListEvent()
}