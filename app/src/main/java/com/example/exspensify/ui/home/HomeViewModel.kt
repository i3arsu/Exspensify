package com.example.exspensify.ui.home

import androidx.lifecycle.viewModelScope
import com.example.exspensify.core.util.Resource
import com.example.exspensify.core.util.UiEvent
import com.example.exspensify.domain.model.TransactionType
import com.example.exspensify.domain.repository.CategoryRepository
import com.example.exspensify.domain.repository.TransactionRepository
import com.example.exspensify.ui.base.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val categoryRepository: CategoryRepository
) : BaseViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        loadHomeData()
    }

    fun onEvent(event: HomeEvent) {
        when (event) {
            is HomeEvent.Refresh -> loadHomeData()
            is HomeEvent.TransactionClick -> {
                sendUiEvent(UiEvent.Navigate("add_edit_transaction/${event.transactionId}"))
            }
        }
    }

    private fun loadHomeData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            // Combine transactions and categories flows
            combine(
                transactionRepository.getAllTransactions(),
                categoryRepository.getAllCategories()
            ) { transactionsResult, categoriesResult ->
                Pair(transactionsResult, categoriesResult)
            }.collect { (transactionsResult, categoriesResult) ->
                when (transactionsResult) {
                    is Resource.Success -> {
                        val transactions = transactionsResult.data ?: emptyList()
                        val categories = when (categoriesResult) {
                            is Resource.Success -> categoriesResult.data ?: emptyList()
                            else -> emptyList()
                        }

                        // Calculate current month metrics
                        val now = LocalDateTime.now()
                        val startOfMonth = now.withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0)

                        val currentMonthTransactions = transactions.filter {
                            it.date.isAfter(startOfMonth) || it.date.isEqual(startOfMonth)
                        }

                        val currentMonthIncome = currentMonthTransactions
                            .filter { it.type == TransactionType.INCOME }
                            .sumOf { it.amount }

                        val currentMonthExpenses = currentMonthTransactions
                            .filter { it.type == TransactionType.EXPENSE }
                            .sumOf { it.amount }

                        // Calculate all-time totals
                        val totalIncome = transactions
                            .filter { it.type == TransactionType.INCOME }
                            .sumOf { it.amount }

                        val totalExpenses = transactions
                            .filter { it.type == TransactionType.EXPENSE }
                            .sumOf { it.amount }

                        // Get recent transactions
                        val recentTransactions = transactions
                            .sortedByDescending { it.date }
                            .take(5)

                        // Calculate top categories
                        val expenseTransactions = currentMonthTransactions.filter {
                            it.type == TransactionType.EXPENSE
                        }

                        val categoryTotals = expenseTransactions
                            .groupBy { it.category }
                            .mapValues { (_, txns) -> txns.sumOf { it.amount } }

                        val topCategories = categoryTotals
                            .entries
                            .sortedByDescending { it.value }
                            .take(3)
                            .map { (categoryId, amount) ->
                                val category = categories.find { it.id == categoryId }
                                CategorySpending(
                                    categoryId = categoryId,
                                    categoryName = category?.name ?: "Unknown",
                                    categoryIcon = category?.icon ?: "📦",
                                    categoryColor = category?.color ?: "#999999",
                                    amount = amount,
                                    percentage = if (currentMonthExpenses > 0) {
                                        (amount / currentMonthExpenses * 100).toFloat()
                                    } else 0f
                                )
                            }

                        _uiState.update {
                            it.copy(
                                currentMonthIncome = currentMonthIncome,
                                currentMonthExpenses = currentMonthExpenses,
                                netBalance = currentMonthIncome - currentMonthExpenses,
                                totalIncome = totalIncome,
                                totalExpenses = totalExpenses,
                                totalBalance = totalIncome - totalExpenses,
                                recentTransactions = recentTransactions,
                                topCategories = topCategories,
                                isLoading = false,
                                error = null
                            )
                        }
                    }
                    is Resource.Error -> {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                error = transactionsResult.message
                            )
                        }
                        sendUiEvent(UiEvent.ShowSnackbar(transactionsResult.message ?: "Unknown error"))
                    }
                    is Resource.Loading -> {
                        _uiState.update { it.copy(isLoading = true) }
                    }
                }
            }
        }
    }
}