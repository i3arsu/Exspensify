package com.example.exspensify.domain.model

data class AppSettings(
    val currencyCode: String = "EUR",
    val themeMode: ThemeMode = ThemeMode.SYSTEM
)

enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK
}

// Currency helper
data class Currency(
    val code: String,
    val symbol: String,
    val name: String
)

object SupportedCurrencies {
    val currencies = listOf(
        Currency("EUR", "€", "Euro"),
        Currency("USD", "$", "US Dollar"),
        Currency("GBP", "£", "British Pound"),
        Currency("JPY", "¥", "Japanese Yen"),
        Currency("CHF", "CHF", "Swiss Franc"),
        Currency("CAD", "C$", "Canadian Dollar"),
        Currency("AUD", "A$", "Australian Dollar"),
        Currency("CNY", "¥", "Chinese Yuan"),
        Currency("INR", "₹", "Indian Rupee"),
        Currency("RUB", "₽", "Russian Ruble")
    )
    
    fun getCurrency(code: String): Currency {
        return currencies.find { it.code == code } ?: currencies.first()
    }
}