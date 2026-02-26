package com.example.exspensify.ui.budget

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.example.exspensify.core.util.Resource
import com.example.exspensify.core.util.UiEvent
import com.example.exspensify.domain.model.Budget
import com.example.exspensify.domain.repository.BudgetRepository
import com.example.exspensify.domain.repository.CategoryRepository
import com.example.exspensify.ui.base.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@RequiresApi(Build.VERSION_CODES.O)
@HiltViewModel
class AddEditBudgetViewModel @Inject constructor(
    private val budgetRepository: BudgetRepository,
    private val categoryRepository: CategoryRepository,
    savedStateHandle: SavedStateHandle
) : BaseViewModel() {

    private val budgetId: String? = savedStateHandle.get<String>("budgetId")
        ?.let { if (it == "new") null else it }

    private val _uiState = MutableStateFlow(AddEditBudgetUiState())
    val uiState: StateFlow<AddEditBudgetUiState> = _uiState.asStateFlow()

    init {
        loadCategories()
        budgetId?.let { loadBudget(it) }
    }

    fun onEvent(event: AddEditBudgetEvent) {
        when (event) {
            is AddEditBudgetEvent.CategorySelected ->
                _uiState.update { it.copy(selectedCategory = event.category, categoryError = null) }
            is AddEditBudgetEvent.LimitChanged ->
                _uiState.update { it.copy(limitInput = event.value, limitError = null) }
            is AddEditBudgetEvent.PeriodChanged ->
                _uiState.update { it.copy(period = event.period) }
            is AddEditBudgetEvent.SaveBudget -> saveBudget()
        }
    }

    private fun loadCategories() {
        viewModelScope.launch {
            categoryRepository.getAllCategories().collect { result ->
                if (result is Resource.Success) {
                    _uiState.update { it.copy(categories = result.data ?: emptyList()) }
                }
            }
        }
    }

    private fun loadBudget(id: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            budgetRepository.getBudgetById(id).collect { result ->
                when (result) {
                    is Resource.Success -> {
                        val budget = result.data ?: return@collect
                        val categoriesResult = categoryRepository.getAllCategories().first()
                        val category = (categoriesResult as? Resource.Success)?.data
                            ?.find { it.id == budget.category }
                        _uiState.update {
                            it.copy(
                                id = budget.id,
                                selectedCategory = category,
                                limitInput = budget.limit.toString(),
                                period = budget.period,
                                isLoading = false
                            )
                        }
                    }
                    is Resource.Error ->
                        _uiState.update { it.copy(isLoading = false, error = result.message) }
                    is Resource.Loading ->
                        _uiState.update { it.copy(isLoading = true) }
                }
            }
        }
    }

    private fun saveBudget() {
        val state = _uiState.value
        var valid = true

        if (state.selectedCategory == null) {
            _uiState.update { it.copy(categoryError = "Select a category") }
            valid = false
        }
        val limit = state.limitInput.toDoubleOrNull()
        if (limit == null || limit <= 0) {
            _uiState.update { it.copy(limitError = "Enter an amount greater than 0") }
            valid = false
        }
        if (!valid) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val budget = Budget(
                id = state.id ?: "",
                category = state.selectedCategory!!.id,
                limit = limit!!,
                spent = 0.0,
                period = state.period
            )
            val result = if (state.id == null) {
                budgetRepository.insertBudget(budget)
            } else {
                budgetRepository.updateBudget(budget)
            }
            when (result) {
                is Resource.Success -> {
                    sendUiEvent(
                        UiEvent.ShowSnackbar(
                            if (state.id == null) "Budget created" else "Budget updated"
                        )
                    )
                    sendUiEvent(UiEvent.NavigateBack)
                }
                is Resource.Error -> {
                    _uiState.update { it.copy(isLoading = false, error = result.message) }
                    sendUiEvent(UiEvent.ShowSnackbar(result.message ?: "Failed to save budget"))
                }
                is Resource.Loading -> {}
            }
        }
    }
}
