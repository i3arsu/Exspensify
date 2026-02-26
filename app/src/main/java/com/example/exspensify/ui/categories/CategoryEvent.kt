package com.example.exspensify.ui.categories

import com.example.exspensify.domain.model.TransactionType

sealed class CategoryEvent {
    data class FilterByType(val type: TransactionType?) : CategoryEvent()
    object ClearFilter : CategoryEvent()
    object Refresh : CategoryEvent()
}