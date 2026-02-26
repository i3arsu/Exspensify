package com.example.exspensify.ui.transactions.addedittransaction

import com.example.exspensify.domain.model.Category
import com.example.exspensify.domain.model.TransactionType
import java.time.LocalDateTime

data class AddEditTransactionUiState(
    val id: String? = null,
    val title: String = "",
    val amount: String = "",
    val type: TransactionType = TransactionType.EXPENSE,
    val selectedCategory: Category? = null,
    val date: LocalDateTime = LocalDateTime.now(),
    val description: String = "",
    val categories: List<Category> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    
    // Validation errors
    val titleError: String? = null,
    val amountError: String? = null,
    val categoryError: String? = null
)

val AddEditTransactionUiState.isValid: Boolean
    get() = titleError == null && 
            amountError == null && 
            categoryError == null &&
            title.isNotBlank() &&
            amount.isNotBlank() &&
            selectedCategory != null