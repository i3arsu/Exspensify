package com.example.exspensify.data.local.mapper

import com.example.exspensify.data.local.dao.BudgetWithSpent
import com.example.exspensify.data.local.entity.BudgetEntity
import com.example.exspensify.data.local.entity.BudgetPeriod as EntityBudgetPeriod
import com.example.exspensify.domain.model.Budget
import com.example.exspensify.domain.model.BudgetPeriod as DomainBudgetPeriod

fun BudgetWithSpent.toBudget(): Budget = Budget(
    id = id.toString(),
    category = categoryId.toString(),
    limit = amount,
    spent = spentAmount,
    period = period.toDomainPeriod(),
    categoryName = categoryName,
    categoryIcon = categoryIcon
)

fun Budget.toBudgetEntity(startDate: Long, endDate: Long): BudgetEntity = BudgetEntity(
    id = id.toLongOrNull() ?: 0L,
    categoryId = category.toLongOrNull() ?: 0L,
    amount = limit,
    period = period.toEntityPeriod(),
    startDate = startDate,
    endDate = endDate
)

private fun EntityBudgetPeriod.toDomainPeriod(): DomainBudgetPeriod = when (this) {
    EntityBudgetPeriod.WEEKLY -> DomainBudgetPeriod.WEEKLY
    EntityBudgetPeriod.MONTHLY -> DomainBudgetPeriod.MONTHLY
    EntityBudgetPeriod.YEARLY -> DomainBudgetPeriod.YEARLY
}

private fun DomainBudgetPeriod.toEntityPeriod(): EntityBudgetPeriod = when (this) {
    DomainBudgetPeriod.WEEKLY -> EntityBudgetPeriod.WEEKLY
    DomainBudgetPeriod.MONTHLY -> EntityBudgetPeriod.MONTHLY
    DomainBudgetPeriod.YEARLY -> EntityBudgetPeriod.YEARLY
}
