package com.example.exspensify.domain.repository

import com.example.exspensify.domain.model.AppSettings
import com.example.exspensify.domain.model.ThemeMode
import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    
    /**
     * Observe app settings
     */
    fun getSettings(): Flow<AppSettings>
    
    /**
     * Update currency code
     */
    suspend fun updateCurrency(currencyCode: String)
    
    /**
     * Update theme mode
     */
    suspend fun updateThemeMode(themeMode: ThemeMode)
}