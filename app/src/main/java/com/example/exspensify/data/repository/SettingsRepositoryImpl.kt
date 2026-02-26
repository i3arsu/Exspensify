package com.example.exspensify.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.preferencesDataStore
import com.example.exspensify.data.local.datastore.PreferencesKeys
import com.example.exspensify.domain.model.AppSettings
import com.example.exspensify.domain.model.ThemeMode
import com.example.exspensify.domain.repository.SettingsRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

// Extension property creates the DataStore instance
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(
    name = "app_settings"
)

@Singleton
class SettingsRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : SettingsRepository {

    private val dataStore = context.dataStore

    override fun getSettings(): Flow<AppSettings> {
        return dataStore.data
            .catch { exception ->
                if (exception is IOException) {
                    android.util.Log.e("SettingsRepository", "Error reading preferences", exception)
                    emit(emptyPreferences())
                } else {
                    throw exception
                }
            }
            .map { preferences ->
                AppSettings(
                    currencyCode = preferences[PreferencesKeys.CURRENCY_CODE] ?: "EUR",
                    themeMode = try {
                        ThemeMode.valueOf(
                            preferences[PreferencesKeys.THEME_MODE] ?: ThemeMode.SYSTEM.name
                        )
                    } catch (e: IllegalArgumentException) {
                        ThemeMode.SYSTEM
                    }
                )
            }
    }

    override suspend fun updateCurrency(currencyCode: String) {
        try {
            dataStore.edit { preferences ->
                preferences[PreferencesKeys.CURRENCY_CODE] = currencyCode
            }
        } catch (e: IOException) {
            android.util.Log.e("SettingsRepository", "Error updating currency", e)
        }
    }

    override suspend fun updateThemeMode(themeMode: ThemeMode) {
        try {
            dataStore.edit { preferences ->
                preferences[PreferencesKeys.THEME_MODE] = themeMode.name
            }
        } catch (e: IOException) {
            android.util.Log.e("SettingsRepository", "Error updating theme", e)
        }
    }
}