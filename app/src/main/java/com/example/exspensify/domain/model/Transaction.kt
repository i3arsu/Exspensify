package com.example.exspensify.domain.model

import java.time.LocalDateTime

data class Transaction(
    val id: String,
    val title: String,
    val amount: Double,
    val type: TransactionType,
    val category: String,
    val date: LocalDateTime,
    val description: String? = null
)

enum class TransactionType {
    INCOME,
    EXPENSE
}