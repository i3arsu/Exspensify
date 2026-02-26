package com.example.exspensify.ui.settings

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.exspensify.core.util.Resource
import com.example.exspensify.core.util.UiEvent
import com.example.exspensify.domain.model.SupportedCurrencies
import com.example.exspensify.domain.model.ThemeMode
import com.example.exspensify.domain.repository.DataManagementRepository
import com.example.exspensify.domain.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val dataManagementRepository: DataManagementRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    private val _uiEvent = Channel<UiEvent>()
    val uiEvent = _uiEvent.receiveAsFlow()

    init {
        loadSettings()
        loadTransactionCount()
    }

    fun onEvent(event: SettingsEvent) {
        when (event) {
            is SettingsEvent.CurrencyChanged -> updateCurrency(event.currencyCode)
            is SettingsEvent.ThemeChanged -> updateTheme(event.themeMode)
            SettingsEvent.ResetDatabase -> resetDatabase()
            is SettingsEvent.ExportCSV -> exportCSV(event.uri)
            SettingsEvent.Refresh -> {
                loadSettings()
                loadTransactionCount()
            }
        }
    }

    private fun loadSettings() {
        viewModelScope.launch {
            settingsRepository.getSettings().collect { settings ->
                val currency = SupportedCurrencies.getCurrency(settings.currencyCode)
                _uiState.update {
                    it.copy(
                        settings = settings,
                        currentCurrency = currency
                    )
                }
            }
        }
    }

    private fun loadTransactionCount() {
        viewModelScope.launch {
            val count = dataManagementRepository.getTransactionCount()
            _uiState.update { it.copy(transactionCount = count) }
        }
    }

    private fun updateCurrency(currencyCode: String) {
        viewModelScope.launch {
            settingsRepository.updateCurrency(currencyCode)
            sendUiEvent(UiEvent.ShowSnackbar("Currency updated to $currencyCode"))
        }
    }

    private fun updateTheme(themeMode: ThemeMode) {
        viewModelScope.launch {
            settingsRepository.updateThemeMode(themeMode)
            sendUiEvent(UiEvent.ShowSnackbar("Theme updated"))
        }
    }

    private fun resetDatabase() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            when (val result = dataManagementRepository.resetDatabase()) {
                is Resource.Success -> {
                    _uiState.update { it.copy(isLoading = false, transactionCount = 0) }
                    sendUiEvent(UiEvent.ShowSnackbar("Database reset successfully"))
                }
                is Resource.Error -> {
                    _uiState.update { it.copy(isLoading = false) }
                    sendUiEvent(UiEvent.ShowSnackbar(result.message ?: "Failed to reset database"))
                }
                is Resource.Loading -> {}
            }
        }
    }

    private fun exportCSV(uri: Uri) {
        viewModelScope.launch {
            _uiState.update { it.copy(isExporting = true) }

            when (val result = dataManagementRepository.exportTransactionsToCSV(uri)) {
                is Resource.Success -> {
                    _uiState.update { it.copy(isExporting = false) }
                    sendUiEvent(UiEvent.ShowSnackbar("Exported ${result.data} transactions"))
                }
                is Resource.Error -> {
                    _uiState.update { it.copy(isExporting = false) }
                    sendUiEvent(UiEvent.ShowSnackbar(result.message ?: "Failed to export"))
                }
                is Resource.Loading -> {}
            }
        }
    }

    private fun sendUiEvent(event: UiEvent) {
        viewModelScope.launch {
            _uiEvent.send(event)
        }
    }
}