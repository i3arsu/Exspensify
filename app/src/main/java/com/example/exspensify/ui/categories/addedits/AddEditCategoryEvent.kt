package com.example.exspensify.ui.categories.addedits

sealed class AddEditCategoryEvent {
    data class NameChanged(val name: String) : AddEditCategoryEvent()
    data class IconSelected(val icon: String) : AddEditCategoryEvent()
    data class ColorSelected(val color: Long) : AddEditCategoryEvent()
    object SaveCategory : AddEditCategoryEvent()
}