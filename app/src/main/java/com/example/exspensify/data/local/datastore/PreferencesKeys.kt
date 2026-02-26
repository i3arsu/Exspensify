package com.example.exspensify.data.local.datastore

import androidx.datastore.preferences.core.stringPreferencesKey

object PreferencesKeys {
    val CURRENCY_CODE = stringPreferencesKey("currency_code")
    val THEME_MODE = stringPreferencesKey("theme_mode")
}