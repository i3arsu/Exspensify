package com.example.exspensify.data.mapper

import com.example.exspensify.data.local.entity.TransactionEntity
import com.example.exspensify.domain.model.Transaction
import com.example.exspensify.domain.model.TransactionType as DomainTransactionType
import com.example.exspensify.data.local.entity.TransactionType as EntityTransactionType

fun TransactionEntity.toTransaction(): Transaction {
    return Transaction(
        id = this.id.toString(),
        title = this.title,
        amount = this.amount,
        type = when (this.type) {
            EntityTransactionType.INCOME -> DomainTransactionType.INCOME
            EntityTransactionType.EXPENSE -> DomainTransactionType.EXPENSE
        },
        category = this.categoryId.toString(), // You might want to join with category
        date = this.date,
        description = this.description
    )
}

fun Transaction.toTransactionEntity(): TransactionEntity {
    return TransactionEntity(
        id = this.id.toLongOrNull() ?: 0L,
        title = this.title,
        amount = this.amount,
        type = when (this.type) {
            DomainTransactionType.INCOME -> EntityTransactionType.INCOME
            DomainTransactionType.EXPENSE -> EntityTransactionType.EXPENSE
        },
        categoryId = this.category.toLongOrNull() ?: 1L,
        date = this.date,
        description = this.description
    )
}