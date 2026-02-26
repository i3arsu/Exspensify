package com.example.exspensify.ui.transactions.addedittransaction

import com.example.exspensify.domain.model.Category
import com.example.exspensify.domain.model.TransactionType
import java.time.LocalDateTime

sealed class AddEditTransactionEvent {
    data class TitleChanged(val title: String) : AddEditTransactionEvent()
    data class AmountChanged(val amount: String) : AddEditTransactionEvent()
    data class TypeChanged(val type: TransactionType) : AddEditTransactionEvent()
    data class CategorySelected(val category: Category) : AddEditTransactionEvent()
    data class DateChanged(val date: LocalDateTime) : AddEditTransactionEvent()
    data class DescriptionChanged(val description: String) : AddEditTransactionEvent()
    object SaveTransaction : AddEditTransactionEvent()
    object DeleteTransaction : AddEditTransactionEvent()
}