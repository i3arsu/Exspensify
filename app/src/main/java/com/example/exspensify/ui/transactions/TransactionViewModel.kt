package com.example.exspensify.ui.transactions

import androidx.lifecycle.viewModelScope
import com.example.exspensify.core.util.Resource
import com.example.exspensify.core.util.UiEvent
import com.example.exspensify.domain.model.Transaction
import com.example.exspensify.domain.model.TransactionType
import com.example.exspensify.domain.repository.TransactionRepository
import com.example.exspensify.ui.base.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject

@HiltViewModel
class TransactionViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository
) : BaseViewModel() {

    private val _uiState = MutableStateFlow(TransactionUiState())
    val uiState: StateFlow<TransactionUiState> = _uiState.asStateFlow()

    init {
        loadTransactions()
    }

    fun onEvent(event: TransactionEvent) {
        when (event) {
            is TransactionEvent.FilterByType -> filterByType(event.type)
            is TransactionEvent.DeleteTransaction -> deleteTransaction(event.id)
            TransactionEvent.ClearFilters -> clearFilters()
            TransactionEvent.Refresh -> loadTransactions()
        }
    }

    private fun loadTransactions() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            transactionRepository.getAllTransactions().collect { result ->
                when (result) {
                    is Resource.Success -> {
                        result.data?.let { transactions ->
                            val sortedTransactions = transactions.sortedByDescending { it.date }
                            val grouped = groupTransactionsByDate(sortedTransactions)

                            _uiState.update {
                                it.copy(
                                    allTransactions = sortedTransactions,
                                    groupedTransactions = grouped,
                                    isLoading = false,
                                    error = null
                                )
                            }
                        }
                    }
                    is Resource.Error -> {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                error = result.message
                            )
                        }
                        sendUiEvent(UiEvent.ShowSnackbar(result.message ?: "Failed to load transactions"))
                    }
                    is Resource.Loading -> {
                        _uiState.update { it.copy(isLoading = true) }
                    }
                }
            }
        }
    }

    private fun groupTransactionsByDate(transactions: List<Transaction>): Map<String, List<Transaction>> {
        return transactions.groupBy { transaction ->
            formatDateHeader(transaction.date)
        }
    }

    private fun formatDateHeader(date: LocalDateTime): String {
        val today = LocalDateTime.now()
        val yesterday = today.minusDays(1)

        return when {
            date.toLocalDate() == today.toLocalDate() -> "Today"
            date.toLocalDate() == yesterday.toLocalDate() -> "Yesterday"
            date.year == today.year -> date.format(DateTimeFormatter.ofPattern("MMMM dd"))
            else -> date.format(DateTimeFormatter.ofPattern("MMMM dd, yyyy"))
        }
    }

    private fun filterByType(type: TransactionType?) {
        val filtered = if (type == null) {
            _uiState.value.allTransactions
        } else {
            _uiState.value.allTransactions.filter { it.type == type }
        }

        _uiState.update {
            it.copy(
                selectedType = type,
                groupedTransactions = groupTransactionsByDate(filtered)
            )
        }
    }

    private fun clearFilters() {
        _uiState.update {
            it.copy(
                selectedType = null,
                groupedTransactions = groupTransactionsByDate(it.allTransactions)
            )
        }
    }

    private fun deleteTransaction(id: String) {
        viewModelScope.launch {
            when (val result = transactionRepository.deleteTransaction(id)) {
                is Resource.Success -> {
                    sendUiEvent(UiEvent.ShowSnackbar("Transaction deleted"))
                    loadTransactions()
                }
                is Resource.Error -> {
                    sendUiEvent(UiEvent.ShowSnackbar(result.message ?: "Failed to delete"))
                }
                is Resource.Loading -> {}
            }
        }
    }
}