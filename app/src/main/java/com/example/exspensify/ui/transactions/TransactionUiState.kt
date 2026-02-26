package com.example.exspensify.ui.transactions

import com.example.exspensify.domain.model.Transaction
import com.example.exspensify.domain.model.TransactionType

data class TransactionUiState(
    val allTransactions: List<Transaction> = emptyList(),
    val groupedTransactions: Map<String, List<Transaction>> = emptyMap(),
    val selectedType: TransactionType? = null,
    val isLoading: Boolean = false,
    val error: String? = null
)