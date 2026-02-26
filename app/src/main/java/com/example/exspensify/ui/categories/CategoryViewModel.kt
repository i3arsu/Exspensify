package com.example.exspensify.ui.categories

import androidx.lifecycle.viewModelScope
import com.example.exspensify.core.util.Resource
import com.example.exspensify.core.util.UiEvent
import com.example.exspensify.domain.model.Category
import com.example.exspensify.domain.model.TransactionType
import com.example.exspensify.domain.repository.CategoryRepository
import com.example.exspensify.ui.base.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CategoryViewModel @Inject constructor(
    private val categoryRepository: CategoryRepository
) : BaseViewModel() {

    private val _uiState = MutableStateFlow(CategoryUiState())
    val uiState: StateFlow<CategoryUiState> = _uiState.asStateFlow()

    init {
        loadCategories()
    }

    fun onEvent(event: CategoryEvent) {
        when (event) {
            is CategoryEvent.FilterByType -> filterByType(event.type)
            CategoryEvent.ClearFilter -> clearFilter()
            CategoryEvent.Refresh -> loadCategories()
        }
    }

    private fun loadCategories() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            
            categoryRepository.getAllCategories().collect { result ->
                when (result) {
                    is Resource.Success -> {
                        result.data?.let { categories ->
                            _uiState.update {
                                it.copy(
                                    allCategories = categories,
                                    filteredCategories = categories,
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
                        sendUiEvent(UiEvent.ShowSnackbar(result.message ?: "Failed to load categories"))
                    }
                    is Resource.Loading -> {
                        _uiState.update { it.copy(isLoading = true) }
                    }
                }
            }
        }
    }

    private fun filterByType(type: TransactionType?) {
        _uiState.update { state ->
            val filtered = if (type == null) {
                state.allCategories
            } else {
                state.allCategories.filter { it.type == type }
            }
            state.copy(
                selectedType = type,
                filteredCategories = filtered
            )
        }
    }

    private fun clearFilter() {
        _uiState.update {
            it.copy(
                selectedType = null,
                filteredCategories = it.allCategories
            )
        }
    }
}