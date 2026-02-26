package com.example.exspensify.ui.transactions.addedittransaction

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.example.exspensify.core.util.Resource
import com.example.exspensify.core.util.UiEvent
import com.example.exspensify.domain.model.Transaction
import com.example.exspensify.domain.model.TransactionType
import com.example.exspensify.domain.repository.CategoryRepository
import com.example.exspensify.domain.repository.TransactionRepository
import com.example.exspensify.ui.base.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AddEditTransactionViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val categoryRepository: CategoryRepository,
    savedStateHandle: SavedStateHandle
) : BaseViewModel() {

    private val _uiState = MutableStateFlow(AddEditTransactionUiState())
    val uiState: StateFlow<AddEditTransactionUiState> = _uiState.asStateFlow()

    private val transactionId: String? = savedStateHandle.get<String>("transactionId")

    init {
        loadCategories()
        transactionId?.let { id ->
            if (id != "new") {  // Only load if not creating new transaction
                loadTransaction(id)
            }
        }
    }

    fun onEvent(event: AddEditTransactionEvent) {
        when (event) {
            is AddEditTransactionEvent.TitleChanged -> {
                _uiState.update {
                    it.copy(
                        title = event.title,
                        titleError = validateTitle(event.title)
                    )
                }
            }
            is AddEditTransactionEvent.AmountChanged -> {
                _uiState.update {
                    it.copy(
                        amount = event.amount,
                        amountError = validateAmount(event.amount)
                    )
                }
            }
            is AddEditTransactionEvent.TypeChanged -> {
                _uiState.update { it.copy(type = event.type) }
                loadCategories(event.type)
            }
            is AddEditTransactionEvent.CategorySelected -> {
                _uiState.update {
                    it.copy(
                        selectedCategory = event.category,
                        categoryError = null
                    )
                }
            }
            is AddEditTransactionEvent.DateChanged -> {
                _uiState.update { it.copy(date = event.date) }
            }
            is AddEditTransactionEvent.DescriptionChanged -> {
                _uiState.update { it.copy(description = event.description) }
            }
            AddEditTransactionEvent.SaveTransaction -> saveTransaction()
            AddEditTransactionEvent.DeleteTransaction -> deleteTransaction()
        }
    }

    private fun loadCategories(type: TransactionType? = null) {
        viewModelScope.launch {
            categoryRepository.getAllCategories().collect { result ->
                when (result) {
                    is Resource.Success -> {
                        result.data?.let { allCategories ->
                            val filteredCategories = if (type != null) {
                                // Filter by type OR include categories with null type (universal)
                                allCategories.filter { it.type == type || it.type == null }
                            } else {
                                // Filter by current UI state type
                                allCategories.filter {
                                    it.type == _uiState.value.type || it.type == null
                                }
                            }
                            _uiState.update { it.copy(categories = filteredCategories) }
                        }
                    }
                    is Resource.Error -> {
                        sendUiEvent(UiEvent.ShowSnackbar(result.message ?: "Failed to load categories"))
                    }
                    is Resource.Loading -> {}
                }
            }
        }
    }

    private fun loadTransaction(id: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            transactionRepository.getTransactionById(id).collect { result ->
                when (result) {
                    is Resource.Success -> {
                        result.data?.let { transaction ->
                            _uiState.update {
                                it.copy(
                                    id = transaction.id,
                                    title = transaction.title,
                                    amount = transaction.amount.toString(),
                                    type = transaction.type,
                                    date = transaction.date,
                                    description = transaction.description ?: "",
                                    isLoading = false
                                )
                            }
                            // Load category by ID - but check if it's a valid number first
                            if (transaction.category.toLongOrNull() != null) {
                                loadCategoryById(transaction.category)
                            }
                        }
                    }
                    is Resource.Error -> {
                        _uiState.update { it.copy(isLoading = false) }
                        sendUiEvent(UiEvent.ShowSnackbar(result.message ?: "Failed to load transaction"))
                    }
                    is Resource.Loading -> {
                        _uiState.update { it.copy(isLoading = true) }
                    }
                }
            }
        }
    }

    private fun loadCategoryById(categoryId: String) {
        // Validate that categoryId is a valid number before trying to load
        if (categoryId == "new" || categoryId.toLongOrNull() == null) {
            return
        }

        viewModelScope.launch {
            categoryRepository.getCategoryById(categoryId).collect { result ->
                when (result) {
                    is Resource.Success -> {
                        result.data?.let { category ->
                            _uiState.update { it.copy(selectedCategory = category) }
                        }
                    }
                    is Resource.Error -> {}
                    is Resource.Loading -> {}
                }
            }
        }
    }

    private fun saveTransaction() {
        // Validate all fields
        val currentState = _uiState.value
        val titleError = validateTitle(currentState.title)
        val amountError = validateAmount(currentState.amount)
        val categoryError = validateCategory(currentState.selectedCategory)

        _uiState.update {
            it.copy(
                titleError = titleError,
                amountError = amountError,
                categoryError = categoryError
            )
        }

        if (titleError != null || amountError != null || categoryError != null) {
            sendUiEvent(UiEvent.ShowSnackbar("Please fix the errors"))
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            val transaction = Transaction(
                id = currentState.id ?: "",
                title = currentState.title,
                amount = currentState.amount.toDouble(),
                type = currentState.type,
                category = currentState.selectedCategory!!.id,
                date = currentState.date,
                description = currentState.description.ifBlank { null }
            )

            val result = if (currentState.id == null) {
                transactionRepository.insertTransaction(transaction)
            } else {
                transactionRepository.updateTransaction(transaction)
            }

            when (result) {
                is Resource.Success -> {
                    _uiState.update { it.copy(isLoading = false) }
                    val message = if (currentState.id == null) {
                        "Transaction added successfully"
                    } else {
                        "Transaction updated successfully"
                    }
                    sendUiEvent(UiEvent.ShowSnackbar(message))
                    sendUiEvent(UiEvent.NavigateBack)
                }
                is Resource.Error -> {
                    _uiState.update { it.copy(isLoading = false) }
                    sendUiEvent(UiEvent.ShowSnackbar(result.message ?: "Failed to save transaction"))
                }
                is Resource.Loading -> {}
            }
        }
    }

    private fun deleteTransaction() {
        val id = _uiState.value.id ?: return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            when (val result = transactionRepository.deleteTransaction(id)) {
                is Resource.Success -> {
                    _uiState.update { it.copy(isLoading = false) }
                    sendUiEvent(UiEvent.ShowSnackbar("Transaction deleted"))
                    sendUiEvent(UiEvent.NavigateBack)
                }
                is Resource.Error -> {
                    _uiState.update { it.copy(isLoading = false) }
                    sendUiEvent(UiEvent.ShowSnackbar(result.message ?: "Failed to delete"))
                }
                is Resource.Loading -> {}
            }
        }
    }

    private fun validateTitle(title: String): String? {
        return when {
            title.isBlank() -> "Title is required"
            title.length < 3 -> "Title must be at least 3 characters"
            else -> null
        }
    }

    private fun validateAmount(amount: String): String? {
        return when {
            amount.isBlank() -> "Amount is required"
            amount.toDoubleOrNull() == null -> "Invalid amount"
            amount.toDouble() <= 0 -> "Amount must be greater than 0"
            else -> null
        }
    }

    private fun validateCategory(category: com.example.exspensify.domain.model.Category?): String? {
        return if (category == null) "Category is required" else null
    }
}