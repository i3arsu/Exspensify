package com.example.exspensify.ui.transactions

import com.example.exspensify.domain.model.TransactionType

sealed class TransactionEvent {
    data class FilterByType(val type: TransactionType?) : TransactionEvent()
    data class DeleteTransaction(val id: String) : TransactionEvent()
    object ClearFilters : TransactionEvent()
    object Refresh : TransactionEvent()
}