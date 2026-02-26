package com.example.exspensify.ui.categories.addedits

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.exspensify.core.util.Resource
import com.example.exspensify.core.util.UiEvent
import com.example.exspensify.domain.repository.CategoryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AddEditCategoryViewModel @Inject constructor(
    private val categoryRepository: CategoryRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _uiState = MutableStateFlow(AddEditCategoryUiState())
    val uiState: StateFlow<AddEditCategoryUiState> = _uiState.asStateFlow()

    private val _uiEvent = Channel<UiEvent>()
    val uiEvent = _uiEvent.receiveAsFlow()

    private val categoryId: String? = savedStateHandle.get<String>("categoryId")

    init {
        categoryId?.let { id ->
            if (id != "new") {
                loadCategory(id)
            }
        }
    }

    fun onEvent(event: AddEditCategoryEvent) {
        when (event) {
            is AddEditCategoryEvent.NameChanged -> {
                _uiState.update {
                    it.copy(
                        name = event.name,
                        nameError = validateName(event.name)
                    )
                }
            }
            is AddEditCategoryEvent.IconSelected -> {
                _uiState.update { it.copy(selectedIcon = event.icon) }
            }
            is AddEditCategoryEvent.ColorSelected -> {
                _uiState.update { it.copy(selectedColor = event.color) }
            }
            AddEditCategoryEvent.SaveCategory -> saveCategory()
        }
    }

    private fun loadCategory(id: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            when (val result = categoryRepository.getCategoryByIdOnce(id)) {
                is Resource.Success -> {
                    result.data?.let { category ->
                        _uiState.update {
                            it.copy(
                                id = category.id,
                                name = category.name,
                                selectedIcon = category.icon,
                                selectedColor = parseColorToLong(category.color),
                                isDefault = category.isDefault,
                                isLoading = false
                            )
                        }
                    }
                }
                is Resource.Error -> {
                    _uiState.update { it.copy(isLoading = false) }
                    sendUiEvent(UiEvent.ShowSnackbar(result.message ?: "Failed to load category"))
                }
                is Resource.Loading -> {}
            }
        }
    }

    private fun saveCategory() {
        val currentState = _uiState.value

        // Validate
        val nameError = validateName(currentState.name)
        _uiState.update { it.copy(nameError = nameError) }

        if (nameError != null) {
            sendUiEvent(UiEvent.ShowSnackbar("Please fix errors"))
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            val result = if (currentState.id == null) {
                // Insert new category
                categoryRepository.insertCategory(
                    name = currentState.name,
                    icon = currentState.selectedIcon,
                    color = currentState.selectedColor
                )
            } else {
                // Update existing category
                categoryRepository.updateCategory(
                    id = currentState.id,
                    name = currentState.name,
                    icon = currentState.selectedIcon,
                    color = currentState.selectedColor
                )
            }

            when (result) {
                is Resource.Success -> {
                    _uiState.update { it.copy(isLoading = false) }
                    sendUiEvent(UiEvent.ShowSnackbar(
                        if (currentState.id == null) "Category created" else "Category updated"
                    ))
                    sendUiEvent(UiEvent.NavigateBack)
                }
                is Resource.Error -> {
                    _uiState.update { it.copy(isLoading = false) }
                    sendUiEvent(UiEvent.ShowSnackbar(result.message ?: "Failed to save category"))
                }
                is Resource.Loading -> {}
            }
        }
    }

    private fun validateName(name: String): String? {
        return when {
            name.isBlank() -> "Name cannot be empty"
            name.length < 2 -> "Name must be at least 2 characters"
            name.length > 30 -> "Name must be less than 30 characters"
            else -> null
        }
    }

    private fun parseColorToLong(colorHex: String): Long {
        return try {
            colorHex.removePrefix("#").toLong(16)
        } catch (e: Exception) {
            0xFF9E9E9EL
        }
    }

    private fun sendUiEvent(event: UiEvent) {
        viewModelScope.launch {
            _uiEvent.send(event)
        }
    }
}