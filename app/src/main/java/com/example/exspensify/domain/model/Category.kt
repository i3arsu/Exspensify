package com.example.exspensify.domain.model

data class Category(
    val id: String,
    val name: String,
    val icon: String,
    val color: String,
    val isDefault: Boolean,
    val type: TransactionType? = null  // Use TransactionType instead of CategoryType
)