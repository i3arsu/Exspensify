package com.example.exspensify.ui.budget

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.lifecycle.viewModelScope
import com.example.exspensify.core.util.Resource
import com.example.exspensify.core.util.UiEvent
import com.example.exspensify.domain.repository.BudgetRepository
import com.example.exspensify.ui.base.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@RequiresApi(Build.VERSION_CODES.O)
@HiltViewModel
class BudgetViewModel @Inject constructor(
    private val budgetRepository: BudgetRepository
) : BaseViewModel() {

    private val _uiState = MutableStateFlow(BudgetUiState())
    val uiState: StateFlow<BudgetUiState> = _uiState.asStateFlow()

    init {
        loadBudgets()
    }

    fun onEvent(event: BudgetEvent) {
        when (event) {
            is BudgetEvent.DeleteBudget -> deleteBudget(event.id)
            is BudgetEvent.Refresh -> loadBudgets()
        }
    }

    private fun loadBudgets() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            budgetRepository.getAllBudgets().collect { result ->
                when (result) {
                    is Resource.Success -> {
                        val budgets = result.data ?: emptyList()
                        _uiState.update {
                            it.copy(
                                budgets = budgets,
                                totalBudgeted = budgets.sumOf { b -> b.limit },
                                totalSpent = budgets.sumOf { b -> b.spent },
                                isLoading = false,
                                error = null
                            )
                        }
                    }
                    is Resource.Error -> {
                        _uiState.update { it.copy(isLoading = false, error = result.message) }
                        sendUiEvent(UiEvent.ShowSnackbar(result.message ?: "Failed to load budgets"))
                    }
                    is Resource.Loading -> _uiState.update { it.copy(isLoading = true) }
                }
            }
        }
    }

    private fun deleteBudget(id: String) {
        viewModelScope.launch {
            when (val result = budgetRepository.deleteBudget(id)) {
                is Resource.Success -> sendUiEvent(UiEvent.ShowSnackbar("Budget deleted"))
                is Resource.Error -> sendUiEvent(
                    UiEvent.ShowSnackbar(result.message ?: "Failed to delete budget")
                )
                is Resource.Loading -> {}
            }
        }
    }
}
