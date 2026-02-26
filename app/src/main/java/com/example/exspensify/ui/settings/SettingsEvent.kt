package com.example.exspensify.ui.settings

import android.net.Uri
import com.example.exspensify.domain.model.ThemeMode

sealed class SettingsEvent {
    data class CurrencyChanged(val currencyCode: String) : SettingsEvent()
    data class ThemeChanged(val themeMode: ThemeMode) : SettingsEvent()
    object ResetDatabase : SettingsEvent()
    data class ExportCSV(val uri: Uri) : SettingsEvent()
    object Refresh : SettingsEvent()
}