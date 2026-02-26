package com.example.exspensify.ui.settings

import com.example.exspensify.domain.model.AppSettings
import com.example.exspensify.domain.model.Currency

data class SettingsUiState(
    val settings: AppSettings = AppSettings(),
    val currentCurrency: Currency = Currency("EUR", "€", "Euro"),
    val transactionCount: Int = 0,
    val isLoading: Boolean = false,
    val isExporting: Boolean = false,
    val error: String? = null
)