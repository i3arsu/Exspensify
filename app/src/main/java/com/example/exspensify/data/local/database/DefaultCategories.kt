package com.example.exspensify.data.local.database

import com.example.exspensify.data.local.entity.CategoryEntity

object DefaultCategories {

    // Predefined colors as ARGB (make sure they have full alpha)
    private const val COLOR_RED = 0xFFEF5350L
    private const val COLOR_PINK = 0xFFEC407AL
    private const val COLOR_PURPLE = 0xFFAB47BCL
    private const val COLOR_BLUE = 0xFF42A5F5L
    private const val COLOR_CYAN = 0xFF26C6DAL
    private const val COLOR_TEAL = 0xFF26A69AL
    private const val COLOR_GREEN = 0xFF66BB6AL
    private const val COLOR_LIME = 0xFF9CCC65L
    private const val COLOR_YELLOW = 0xFFFFEE58L
    private const val COLOR_ORANGE = 0xFFFFCA28L
    private const val COLOR_DEEP_ORANGE = 0xFFFF7043L
    private const val COLOR_BROWN = 0xFF8D6E63L
    private const val COLOR_GREY = 0xFF78909CL

    val defaults = listOf(
        // Expense categories
        CategoryEntity(
            name = "Food & Dining",
            icon = "🍔",
            color = COLOR_RED,
            isDefault = true,
            type = "EXPENSE"
        ),
        CategoryEntity(
            name = "Transportation",
            icon = "🚗",
            color = COLOR_BLUE,
            isDefault = true,
            type = "EXPENSE"
        ),
        CategoryEntity(
            name = "Housing",
            icon = "🏠",
            color = COLOR_BROWN,
            isDefault = true,
            type = "EXPENSE"
        ),
        CategoryEntity(
            name = "Entertainment",
            icon = "🎬",
            color = COLOR_PURPLE,
            isDefault = true,
            type = "EXPENSE"
        ),
        CategoryEntity(
            name = "Shopping",
            icon = "🛍️",
            color = COLOR_PINK,
            isDefault = true,
            type = "EXPENSE"
        ),
        CategoryEntity(
            name = "Healthcare",
            icon = "🏥",
            color = COLOR_TEAL,
            isDefault = true,
            type = "EXPENSE"
        ),
        CategoryEntity(
            name = "Education",
            icon = "📚",
            color = COLOR_CYAN,
            isDefault = true,
            type = "EXPENSE"
        ),
        CategoryEntity(
            name = "Bills & Utilities",
            icon = "💡",
            color = COLOR_YELLOW,
            isDefault = true,
            type = "EXPENSE"
        ),
        CategoryEntity(
            name = "Personal Care",
            icon = "💆",
            color = COLOR_LIME,
            isDefault = true,
            type = "EXPENSE"
        ),
        CategoryEntity(
            name = "Travel",
            icon = "✈️",
            color = COLOR_DEEP_ORANGE,
            isDefault = true,
            type = "EXPENSE"
        ),
        CategoryEntity(
            name = "Fitness",
            icon = "💪",
            color = COLOR_GREEN,
            isDefault = true,
            type = "EXPENSE"
        ),
        CategoryEntity(
            name = "Gifts & Donations",
            icon = "🎁",
            color = COLOR_PINK,
            isDefault = true,
            type = "EXPENSE"
        ),
        // Income categories
        CategoryEntity(
            name = "Salary",
            icon = "💰",
            color = COLOR_GREEN,
            isDefault = true,
            type = "INCOME"
        ),
        CategoryEntity(
            name = "Freelance",
            icon = "💼",
            color = COLOR_BLUE,
            isDefault = true,
            type = "INCOME"
        ),
        CategoryEntity(
            name = "Investment",
            icon = "📈",
            color = COLOR_ORANGE,
            isDefault = true,
            type = "INCOME"
        ),
        CategoryEntity(
            name = "Business",
            icon = "🏢",
            color = COLOR_CYAN,
            isDefault = true,
            type = "INCOME"
        ),
        CategoryEntity(
            name = "Other Income",
            icon = "💵",
            color = COLOR_LIME,
            isDefault = true,
            type = "INCOME"
        ),
        // General
        CategoryEntity(
            name = "Other",
            icon = "📦",
            color = COLOR_GREY,
            isDefault = true,
            type = null
        )
    )
}