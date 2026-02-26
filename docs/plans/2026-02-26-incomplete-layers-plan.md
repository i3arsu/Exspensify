# Incomplete Layers Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Implement the Budget feature (ViewModel + Screen + navigation) and CSV import — the two missing layers identified in the architecture scan.

**Architecture:** Budget `spent` is computed via a SQL LEFT JOIN in BudgetDao (no in-memory aggregation, compliant with CLAUDE.md rule 4). All entity IDs are `Long`; domain models represent them as `String` via `.toString()` / `.toLongOrNull()` — consistent with existing Transaction/Category mappers. Category name and icon are pulled from the same JOIN query to avoid a second repository call in the ViewModel.

**Tech Stack:** Kotlin · Jetpack Compose · Room 2.6.1 · Hilt · Coroutines + Flow · Navigation Compose · `collectAsStateWithLifecycle()`

---

## File Path Reference

All source files live under:
`app/src/main/java/com/example/exspensify/`

Abbreviated below as `…/`.

Key existing files you will read before modifying:
- `…/data/local/dao/BudgetDao.kt`
- `…/domain/model/Budget.kt`
- `…/di/RepositoryModule.kt`
- `…/core/navigation/Routes.kt`
- `…/core/navigation/NavGraph.kt`
- `…/data/repository/DataManagementRepositoryImpl.kt`

Verify the build after every 2–3 tasks:
```bash
cd C:/Users/lukka/AndroidStudioProjects/Exspensify
./gradlew assembleDebug
```
Expected: `BUILD SUCCESSFUL`

---

## Task 1: Extend BudgetDao

**Files:**
- Modify: `…/data/local/dao/BudgetDao.kt`

**What to do:**

Add `deleteById`, a `BudgetWithSpent` data class, and a `getAllBudgetsWithSpent()` JOIN query.

The JOIN fetches each budget plus: the total EXPENSE amount from transactions that fall within the budget's date window, and the category's name and icon — all in a single SQL statement.

`t.date` is stored as `Long` (epoch ms) via `Converters.kt`. `b.startDate` / `b.endDate` are already `Long` epoch ms. The `>=` / `<=` comparison between them is correct.

**Step 1: Read the file**

Read `…/data/local/dao/BudgetDao.kt` completely before editing.

**Step 2: Add to BudgetDao.kt**

Append the following *inside* the file, after the existing `deleteAll()` query and *before* the closing `}` of the interface, then add the data class after the interface closing brace:

```kotlin
    @Query("DELETE FROM budgets WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("""
        SELECT b.id, b.categoryId, b.amount, b.period, b.startDate, b.endDate, b.createdAt,
               COALESCE(SUM(CASE WHEN t.type = 'EXPENSE' THEN t.amount ELSE 0 END), 0.0) AS spentAmount,
               COALESCE(c.name, 'Unknown') AS categoryName,
               COALESCE(c.icon, '📦') AS categoryIcon
        FROM budgets b
        LEFT JOIN transactions t
          ON t.categoryId = b.categoryId
         AND t.date >= b.startDate
         AND t.date <= b.endDate
        LEFT JOIN categories c ON c.id = b.categoryId
        GROUP BY b.id
        ORDER BY b.startDate DESC
    """)
    fun getAllBudgetsWithSpent(): Flow<List<BudgetWithSpent>>
```

Add the data class **outside** the interface (at the bottom of the file):

```kotlin
data class BudgetWithSpent(
    val id: Long,
    val categoryId: Long,
    val amount: Double,
    val period: BudgetPeriod,
    val startDate: Long,
    val endDate: Long,
    val createdAt: Long,
    val spentAmount: Double,
    val categoryName: String,
    val categoryIcon: String
)
```

**Step 3: Verify build**

```bash
./gradlew assembleDebug
```
Expected: `BUILD SUCCESSFUL`

**Step 4: Commit**

```bash
git add app/src/main/java/com/example/exspensify/data/local/dao/BudgetDao.kt
git commit -m "feat(data): add BudgetWithSpent JOIN query and deleteById to BudgetDao"
```

---

## Task 2: Extend Budget Domain Model

**Files:**
- Modify: `…/domain/model/Budget.kt`

**What to do:**

Add `categoryName` and `categoryIcon` display fields with defaults. These come from the DAO JOIN query and must flow to the UI without a second repository call.

**Step 1: Read the file**

Read `…/domain/model/Budget.kt`.

**Step 2: Replace the data class**

The current `Budget` data class is:
```kotlin
data class Budget(
    val id: String,
    val category: String,
    val limit: Double,
    val spent: Double,
    val period: BudgetPeriod
)
```

Replace it with:
```kotlin
data class Budget(
    val id: String,
    val category: String,       // categoryId as String
    val limit: Double,
    val spent: Double,
    val period: BudgetPeriod,
    val categoryName: String = "",
    val categoryIcon: String = ""
)
```

Do not change anything else in the file.

**Step 3: Verify build**

```bash
./gradlew assembleDebug
```
Expected: `BUILD SUCCESSFUL`

**Step 4: Commit**

```bash
git add app/src/main/java/com/example/exspensify/domain/model/Budget.kt
git commit -m "feat(domain): add categoryName and categoryIcon display fields to Budget"
```

---

## Task 3: Create Budget Mapper

**Files:**
- Create: `…/data/local/mapper/toBudget.kt`

**What to do:**

Map `BudgetWithSpent` ↔ `Budget`. Note the two separate `BudgetPeriod` enums: one in `data.local.entity` (entity layer) and one in `domain.model` (domain layer).

**Step 1: Check if the mapper directory exists**

```bash
ls app/src/main/java/com/example/exspensify/data/local/mapper/
```

Expected: you should see `toTransaction.kt` and `toCategory.kt`. If the directory doesn't exist, it will be created when you write the file.

**Step 2: Create the file**

```kotlin
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
```

**Step 3: Verify build**

```bash
./gradlew assembleDebug
```
Expected: `BUILD SUCCESSFUL`

**Step 4: Commit**

```bash
git add app/src/main/java/com/example/exspensify/data/local/mapper/toBudget.kt
git commit -m "feat(data): add toBudget mapper (BudgetWithSpent <-> Budget domain model)"
```

---

## Task 4: Create BudgetRepositoryImpl

**Files:**
- Create: `…/data/repository/BudgetRepositoryImpl.kt`

**What to do:**

Implement `BudgetRepository`. The `spent` field is already computed in the DAO JOIN — no in-memory aggregation needed here.

`currentPeriodRange()` computes `startDate`/`endDate` (Long epoch ms) from the current date and period type. This requires API 26+ (`java.time`). Annotate with `@RequiresApi(Build.VERSION_CODES.O)` consistent with existing code.

**Step 1: Create the file**

```kotlin
package com.example.exspensify.data.repository

import android.os.Build
import androidx.annotation.RequiresApi
import com.example.exspensify.core.util.Resource
import com.example.exspensify.data.local.dao.BudgetDao
import com.example.exspensify.data.local.mapper.toBudget
import com.example.exspensify.data.local.mapper.toBudgetEntity
import com.example.exspensify.domain.model.Budget
import com.example.exspensify.domain.model.BudgetPeriod
import com.example.exspensify.domain.repository.BudgetRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
@RequiresApi(Build.VERSION_CODES.O)
class BudgetRepositoryImpl @Inject constructor(
    private val budgetDao: BudgetDao
) : BudgetRepository {

    override fun getAllBudgets(): Flow<Resource<List<Budget>>> =
        budgetDao.getAllBudgetsWithSpent().map { list ->
            try {
                Resource.Success(list.map { it.toBudget() })
            } catch (e: Exception) {
                Resource.Error(e.localizedMessage ?: "Failed to load budgets")
            }
        }

    override fun getBudgetById(id: String): Flow<Resource<Budget>> =
        budgetDao.getAllBudgetsWithSpent().map { list ->
            try {
                val found = list.find { it.id == id.toLongOrNull() }
                    ?: return@map Resource.Error("Budget not found")
                Resource.Success(found.toBudget())
            } catch (e: Exception) {
                Resource.Error(e.localizedMessage ?: "Failed to load budget")
            }
        }

    override suspend fun insertBudget(budget: Budget): Resource<Unit> = try {
        val (start, end) = budget.period.currentPeriodRange()
        budgetDao.insert(budget.toBudgetEntity(start, end))
        Resource.Success(Unit)
    } catch (e: Exception) {
        Resource.Error(e.localizedMessage ?: "Failed to save budget")
    }

    override suspend fun updateBudget(budget: Budget): Resource<Unit> = try {
        val (start, end) = budget.period.currentPeriodRange()
        budgetDao.update(budget.toBudgetEntity(start, end))
        Resource.Success(Unit)
    } catch (e: Exception) {
        Resource.Error(e.localizedMessage ?: "Failed to update budget")
    }

    override suspend fun deleteBudget(id: String): Resource<Unit> = try {
        val longId = id.toLongOrNull()
            ?: return Resource.Error("Invalid budget id")
        budgetDao.deleteById(longId)
        Resource.Success(Unit)
    } catch (e: Exception) {
        Resource.Error(e.localizedMessage ?: "Failed to delete budget")
    }
}

@RequiresApi(Build.VERSION_CODES.O)
private fun BudgetPeriod.currentPeriodRange(): Pair<Long, Long> {
    val now = LocalDate.now()
    val zone = ZoneId.systemDefault()
    val (start, end) = when (this) {
        BudgetPeriod.WEEKLY -> {
            val monday = now.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
            monday to monday.plusDays(6)
        }
        BudgetPeriod.MONTHLY -> {
            val first = now.withDayOfMonth(1)
            first to now.with(TemporalAdjusters.lastDayOfMonth())
        }
        BudgetPeriod.YEARLY -> {
            val first = now.withDayOfYear(1)
            first to now.with(TemporalAdjusters.lastDayOfYear())
        }
    }
    return start.atStartOfDay(zone).toInstant().toEpochMilli() to
            end.atTime(23, 59, 59).atZone(zone).toInstant().toEpochMilli()
}
```

**Step 2: Verify build**

```bash
./gradlew assembleDebug
```
Expected: `BUILD SUCCESSFUL`

**Step 3: Commit**

```bash
git add app/src/main/java/com/example/exspensify/data/repository/BudgetRepositoryImpl.kt
git commit -m "feat(data): implement BudgetRepositoryImpl with period date range computation"
```

---

## Task 5: Wire BudgetRepository in DI

**Files:**
- Modify: `…/di/RepositoryModule.kt`

**What to do:**

Add the `@Binds` entry for `BudgetRepositoryImpl → BudgetRepository`. Also add the missing import for `BudgetRepository` and `BudgetRepositoryImpl`.

**Step 1: Read the file**

Read `…/di/RepositoryModule.kt`.

**Step 2: Add binding**

Inside the abstract class body, after the last `abstract fun bind…` entry, add:

```kotlin
    @Binds
    @Singleton
    abstract fun bindBudgetRepository(
        budgetRepositoryImpl: BudgetRepositoryImpl
    ): BudgetRepository
```

Add these imports at the top of the file:
```kotlin
import com.example.exspensify.data.repository.BudgetRepositoryImpl
import com.example.exspensify.domain.repository.BudgetRepository
```

**Step 3: Verify build**

```bash
./gradlew assembleDebug
```
Expected: `BUILD SUCCESSFUL`

**Step 4: Commit**

```bash
git add app/src/main/java/com/example/exspensify/di/RepositoryModule.kt
git commit -m "feat(di): bind BudgetRepositoryImpl to BudgetRepository"
```

---

## Task 6: Create Budget UI State and Events

**Files:**
- Create: `…/ui/budget/BudgetUiState.kt`
- Create: `…/ui/budget/AddEditBudgetUiState.kt`

**What to do:**

Define the state and event sealed classes for both the list screen and the add/edit screen.

**Step 1: Create BudgetUiState.kt**

```kotlin
package com.example.exspensify.ui.budget

import com.example.exspensify.domain.model.Budget

data class BudgetUiState(
    val budgets: List<Budget> = emptyList(),
    val totalBudgeted: Double = 0.0,
    val totalSpent: Double = 0.0,
    val isLoading: Boolean = false,
    val error: String? = null
)

sealed class BudgetEvent {
    data class DeleteBudget(val id: String) : BudgetEvent()
    object Refresh : BudgetEvent()
}
```

**Step 2: Create AddEditBudgetUiState.kt**

```kotlin
package com.example.exspensify.ui.budget

import com.example.exspensify.domain.model.BudgetPeriod
import com.example.exspensify.domain.model.Category

data class AddEditBudgetUiState(
    val id: String? = null,
    val selectedCategory: Category? = null,
    val limitInput: String = "",
    val period: BudgetPeriod = BudgetPeriod.MONTHLY,
    val categories: List<Category> = emptyList(),
    val isLoading: Boolean = false,
    val limitError: String? = null,
    val categoryError: String? = null,
    val error: String? = null
) {
    val isValid: Boolean
        get() = selectedCategory != null &&
                limitInput.toDoubleOrNull()?.let { it > 0 } == true
}

sealed class AddEditBudgetEvent {
    data class CategorySelected(val category: Category) : AddEditBudgetEvent()
    data class LimitChanged(val value: String) : AddEditBudgetEvent()
    data class PeriodChanged(val period: BudgetPeriod) : AddEditBudgetEvent()
    object SaveBudget : AddEditBudgetEvent()
}
```

**Step 3: Verify build**

```bash
./gradlew assembleDebug
```
Expected: `BUILD SUCCESSFUL`

**Step 4: Commit**

```bash
git add app/src/main/java/com/example/exspensify/ui/budget/BudgetUiState.kt
git add app/src/main/java/com/example/exspensify/ui/budget/AddEditBudgetUiState.kt
git commit -m "feat(ui): add BudgetUiState, BudgetEvent, AddEditBudgetUiState, AddEditBudgetEvent"
```

---

## Task 7: Create BudgetViewModel

**Files:**
- Create: `…/ui/budget/BudgetViewModel.kt`

**What to do:**

Loads all budgets from `BudgetRepository`, exposes totals, handles delete. Extends `BaseViewModel` (which provides the `uiEvent` channel and `sendUiEvent()`).

**Step 1: Read BaseViewModel for the correct base class pattern**

Read `…/ui/base/BaseViewModel.kt` to confirm the exact class name and methods.

**Step 2: Create the file**

```kotlin
package com.example.exspensify.ui.budget

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.lifecycle.viewModelScope
import com.example.exspensify.core.util.Resource
import com.example.exspensify.core.util.UiEvent
import com.example.exspensify.domain.repository.BudgetRepository
import com.example.exspensify.ui.base.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@RequiresApi(Build.VERSION_CODES.O)
@HiltViewModel
class BudgetViewModel @Inject constructor(
    private val budgetRepository: BudgetRepository
) : BaseViewModel() {

    private val _uiState = MutableStateFlow(BudgetUiState())
    val uiState: StateFlow<BudgetUiState> = _uiState.asStateFlow()

    init {
        loadBudgets()
    }

    fun onEvent(event: BudgetEvent) {
        when (event) {
            is BudgetEvent.DeleteBudget -> deleteBudget(event.id)
            is BudgetEvent.Refresh -> loadBudgets()
        }
    }

    private fun loadBudgets() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            budgetRepository.getAllBudgets().collect { result ->
                when (result) {
                    is Resource.Success -> {
                        val budgets = result.data ?: emptyList()
                        _uiState.update {
                            it.copy(
                                budgets = budgets,
                                totalBudgeted = budgets.sumOf { b -> b.limit },
                                totalSpent = budgets.sumOf { b -> b.spent },
                                isLoading = false,
                                error = null
                            )
                        }
                    }
                    is Resource.Error -> {
                        _uiState.update { it.copy(isLoading = false, error = result.message) }
                        sendUiEvent(UiEvent.ShowSnackbar(result.message ?: "Failed to load budgets"))
                    }
                    is Resource.Loading -> _uiState.update { it.copy(isLoading = true) }
                }
            }
        }
    }

    private fun deleteBudget(id: String) {
        viewModelScope.launch {
            when (val result = budgetRepository.deleteBudget(id)) {
                is Resource.Success -> sendUiEvent(UiEvent.ShowSnackbar("Budget deleted"))
                is Resource.Error -> sendUiEvent(
                    UiEvent.ShowSnackbar(result.message ?: "Failed to delete budget")
                )
                is Resource.Loading -> {}
            }
        }
    }
}
```

**Step 3: Verify build**

```bash
./gradlew assembleDebug
```
Expected: `BUILD SUCCESSFUL`

**Step 4: Commit**

```bash
git add app/src/main/java/com/example/exspensify/ui/budget/BudgetViewModel.kt
git commit -m "feat(ui): add BudgetViewModel with load and delete"
```

---

## Task 8: Create AddEditBudgetViewModel

**Files:**
- Create: `…/ui/budget/AddEditBudgetViewModel.kt`

**What to do:**

Handles both "add" (budgetId == null) and "edit" (budgetId is a valid id string). Loads categories for the picker. Validates before save.

`SavedStateHandle["budgetId"]` is `"new"` for a new budget (same pattern as `AddEditTransactionViewModel`).

**Step 1: Read AddEditTransactionViewModel for the SavedStateHandle pattern**

Read `…/ui/transactions/AddEditTransactionViewModel.kt` to confirm how `SavedStateHandle` is read and how `"new"` vs real id is handled.

**Step 2: Create the file**

```kotlin
package com.example.exspensify.ui.budget

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.example.exspensify.core.util.Resource
import com.example.exspensify.core.util.UiEvent
import com.example.exspensify.domain.model.Budget
import com.example.exspensify.domain.repository.BudgetRepository
import com.example.exspensify.domain.repository.CategoryRepository
import com.example.exspensify.ui.base.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@RequiresApi(Build.VERSION_CODES.O)
@HiltViewModel
class AddEditBudgetViewModel @Inject constructor(
    private val budgetRepository: BudgetRepository,
    private val categoryRepository: CategoryRepository,
    savedStateHandle: SavedStateHandle
) : BaseViewModel() {

    private val budgetId: String? = savedStateHandle.get<String>("budgetId")
        ?.let { if (it == "new") null else it }

    private val _uiState = MutableStateFlow(AddEditBudgetUiState())
    val uiState: StateFlow<AddEditBudgetUiState> = _uiState.asStateFlow()

    init {
        loadCategories()
        budgetId?.let { loadBudget(it) }
    }

    fun onEvent(event: AddEditBudgetEvent) {
        when (event) {
            is AddEditBudgetEvent.CategorySelected ->
                _uiState.update { it.copy(selectedCategory = event.category, categoryError = null) }
            is AddEditBudgetEvent.LimitChanged ->
                _uiState.update { it.copy(limitInput = event.value, limitError = null) }
            is AddEditBudgetEvent.PeriodChanged ->
                _uiState.update { it.copy(period = event.period) }
            is AddEditBudgetEvent.SaveBudget -> saveBudget()
        }
    }

    private fun loadCategories() {
        viewModelScope.launch {
            categoryRepository.getAllCategories().collect { result ->
                if (result is Resource.Success) {
                    _uiState.update { it.copy(categories = result.data ?: emptyList()) }
                }
            }
        }
    }

    private fun loadBudget(id: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            budgetRepository.getBudgetById(id).collect { result ->
                when (result) {
                    is Resource.Success -> {
                        val budget = result.data ?: return@collect
                        // Resolve full Category object for the picker
                        val categoriesResult = categoryRepository.getAllCategories().first()
                        val category = (categoriesResult as? Resource.Success)?.data
                            ?.find { it.id == budget.category }
                        _uiState.update {
                            it.copy(
                                id = budget.id,
                                selectedCategory = category,
                                limitInput = budget.limit.toString(),
                                period = budget.period,
                                isLoading = false
                            )
                        }
                    }
                    is Resource.Error ->
                        _uiState.update { it.copy(isLoading = false, error = result.message) }
                    is Resource.Loading ->
                        _uiState.update { it.copy(isLoading = true) }
                }
            }
        }
    }

    private fun saveBudget() {
        val state = _uiState.value
        var valid = true

        if (state.selectedCategory == null) {
            _uiState.update { it.copy(categoryError = "Select a category") }
            valid = false
        }
        val limit = state.limitInput.toDoubleOrNull()
        if (limit == null || limit <= 0) {
            _uiState.update { it.copy(limitError = "Enter an amount greater than 0") }
            valid = false
        }
        if (!valid) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val budget = Budget(
                id = state.id ?: "",
                category = state.selectedCategory!!.id,
                limit = limit!!,
                spent = 0.0,
                period = state.period
            )
            val result = if (state.id == null) {
                budgetRepository.insertBudget(budget)
            } else {
                budgetRepository.updateBudget(budget)
            }
            when (result) {
                is Resource.Success -> {
                    sendUiEvent(
                        UiEvent.ShowSnackbar(
                            if (state.id == null) "Budget created" else "Budget updated"
                        )
                    )
                    sendUiEvent(UiEvent.NavigateBack)
                }
                is Resource.Error -> {
                    _uiState.update { it.copy(isLoading = false, error = result.message) }
                    sendUiEvent(UiEvent.ShowSnackbar(result.message ?: "Failed to save budget"))
                }
                is Resource.Loading -> {}
            }
        }
    }
}
```

**Step 3: Verify build**

```bash
./gradlew assembleDebug
```
Expected: `BUILD SUCCESSFUL`

**Step 4: Commit**

```bash
git add app/src/main/java/com/example/exspensify/ui/budget/AddEditBudgetViewModel.kt
git commit -m "feat(ui): add AddEditBudgetViewModel with load, validate, and save"
```

---

## Task 9: Create BudgetScreen

**Files:**
- Create: `…/ui/screens/BudgetScreen.kt`

**What to do:**

Dashboard-style layout:
- **Overview card:** total budgeted, total spent, overall progress bar
- **Budget list:** one card per budget showing category icon + name, period badge, spent/limit, progress bar (green < 75%, amber 75–99%, red ≥ 100%), warning label when near/over
- **FAB** navigates to AddEditBudgetScreen
- **Delete confirmation** `AlertDialog`

Uses `collectAsStateWithLifecycle()` (CLAUDE.md rule 1). No business logic in the composable.

**Step 1: Read TransactionScreen.kt for the error/loading/empty pattern**

Read `…/ui/screens/TransactionScreen.kt` to understand how loading, error, and empty states are structured.

**Step 2: Create the file**

```kotlin
package com.example.exspensify.ui.screens

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.exspensify.core.navigation.Routes
import com.example.exspensify.core.util.UiEvent
import com.example.exspensify.domain.model.Budget
import com.example.exspensify.domain.model.BudgetPeriod
import com.example.exspensify.ui.budget.BudgetEvent
import com.example.exspensify.ui.budget.BudgetViewModel

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetScreen(
    viewModel: BudgetViewModel = hiltViewModel(),
    onNavigate: (String) -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var budgetToDelete by remember { mutableStateOf<Budget?>(null) }

    LaunchedEffect(Unit) {
        viewModel.uiEvent.collect { event ->
            when (event) {
                is UiEvent.ShowSnackbar -> snackbarHostState.showSnackbar(event.message)
                is UiEvent.Navigate -> onNavigate(event.route)
                else -> {}
            }
        }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Budgets") }) },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            FloatingActionButton(onClick = { onNavigate(Routes.AddEditBudget.createRoute()) }) {
                Icon(Icons.Default.Add, contentDescription = "Add Budget")
            }
        }
    ) { padding ->
        when {
            uiState.isLoading -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentAlignment = Alignment.Center
                ) { CircularProgressIndicator() }
            }
            uiState.error != null -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Something went wrong", style = MaterialTheme.typography.titleMedium)
                        TextButton(onClick = { viewModel.onEvent(BudgetEvent.Refresh) }) {
                            Text("Retry")
                        }
                    }
                }
            }
            uiState.budgets.isEmpty() -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(32.dp)
                    ) {
                        Text("💰", style = MaterialTheme.typography.displayLarge)
                        Text(
                            "No budgets yet",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "Tap + to set a spending limit for a category",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        BudgetOverviewCard(
                            totalBudgeted = uiState.totalBudgeted,
                            totalSpent = uiState.totalSpent
                        )
                    }
                    items(uiState.budgets, key = { it.id }) { budget ->
                        BudgetItemCard(
                            budget = budget,
                            onEdit = { onNavigate(Routes.AddEditBudget.createRoute(budget.id)) },
                            onDelete = { budgetToDelete = budget }
                        )
                    }
                }
            }
        }
    }

    budgetToDelete?.let { budget ->
        AlertDialog(
            onDismissRequest = { budgetToDelete = null },
            title = { Text("Delete Budget") },
            text = { Text("Delete this budget? This cannot be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.onEvent(BudgetEvent.DeleteBudget(budget.id))
                    budgetToDelete = null
                }) { Text("Delete", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { budgetToDelete = null }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun BudgetOverviewCard(totalBudgeted: Double, totalSpent: Double) {
    val progress = if (totalBudgeted > 0) {
        (totalSpent / totalBudgeted).toFloat().coerceIn(0f, 1f)
    } else 0f

    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                "Overview",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Total Budgeted", color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("${"%.2f".format(totalBudgeted)}", fontWeight = FontWeight.Medium)
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Total Spent", color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("${"%.2f".format(totalSpent)}", fontWeight = FontWeight.Medium)
            }
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = budgetProgressColor(progress),
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
            Text(
                "${(progress * 100).toInt()}% of total budget used",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun BudgetItemCard(
    budget: Budget,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val progress = if (budget.limit > 0) {
        (budget.spent / budget.limit).toFloat().coerceIn(0f, 1f)
    } else 0f
    val progressColor = budgetProgressColor(progress)

    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(budget.categoryIcon, style = MaterialTheme.typography.titleLarge)
                    Column {
                        Text(
                            budget.categoryName,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            budget.period.label,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .background(
                                    MaterialTheme.colorScheme.primaryContainer,
                                    RoundedCornerShape(4.dp)
                                )
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
                Row {
                    IconButton(onClick = onEdit, modifier = Modifier.size(36.dp)) {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = "Edit",
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Delete",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    "${"%.2f".format(budget.spent)} spent",
                    fontWeight = FontWeight.Medium
                )
                Text(
                    "of ${"%.2f".format(budget.limit)}",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = progressColor,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
            if (progress >= 0.75f) {
                Text(
                    if (progress >= 1f) "⛔ Over budget!" else "⚠ Near limit",
                    style = MaterialTheme.typography.bodySmall,
                    color = progressColor
                )
            }
        }
    }
}

@Composable
private fun budgetProgressColor(progress: Float): Color = when {
    progress >= 1f -> MaterialTheme.colorScheme.error
    progress >= 0.75f -> Color(0xFFF59E0B)
    else -> MaterialTheme.colorScheme.primary
}

private val BudgetPeriod.label: String
    get() = when (this) {
        BudgetPeriod.WEEKLY -> "WEEKLY"
        BudgetPeriod.MONTHLY -> "MONTHLY"
        BudgetPeriod.YEARLY -> "YEARLY"
    }
```

**Step 3: Verify build**

```bash
./gradlew assembleDebug
```
Expected: `BUILD SUCCESSFUL`

**Step 4: Commit**

```bash
git add app/src/main/java/com/example/exspensify/ui/screens/BudgetScreen.kt
git commit -m "feat(ui): add BudgetScreen with dashboard overview and per-budget progress"
```

---

## Task 10: Create AddEditBudgetScreen

**Files:**
- Create: `…/ui/screens/AddEditBudgetScreen.kt`

**What to do:**

Form screen with:
- `ExposedDropdownMenuBox` for category picker (shows icon + name)
- `OutlinedTextField` for budget limit (numeric)
- `FilterChip` row for period selection (WEEKLY / MONTHLY / YEARLY)
- "Save" button — disabled when state is invalid
- Back navigation on `UiEvent.NavigateBack`

No business logic in the composable. All validation is in the ViewModel.

**Step 1: Read AddEditTransactionScreen.kt for form field patterns**

Read `…/ui/screens/AddEditTransactionScreen.kt` to see how `OutlinedTextField`, validation errors, and `LaunchedEffect(uiEvent)` are used.

**Step 2: Create the file**

```kotlin
package com.example.exspensify.ui.screens

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.exspensify.core.util.UiEvent
import com.example.exspensify.domain.model.BudgetPeriod
import com.example.exspensify.domain.model.Category
import com.example.exspensify.ui.budget.AddEditBudgetEvent
import com.example.exspensify.ui.budget.AddEditBudgetViewModel

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditBudgetScreen(
    viewModel: AddEditBudgetViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.uiEvent.collect { event ->
            when (event) {
                is UiEvent.NavigateBack -> onNavigateBack()
                is UiEvent.ShowSnackbar -> snackbarHostState.showSnackbar(event.message)
                else -> {}
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (uiState.id == null) "Add Budget" else "Edit Budget") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Category picker
            CategoryDropdown(
                categories = uiState.categories,
                selected = uiState.selectedCategory,
                onSelect = { viewModel.onEvent(AddEditBudgetEvent.CategorySelected(it)) },
                error = uiState.categoryError
            )

            // Budget limit
            OutlinedTextField(
                value = uiState.limitInput,
                onValueChange = { viewModel.onEvent(AddEditBudgetEvent.LimitChanged(it)) },
                label = { Text("Budget Limit") },
                placeholder = { Text("0.00") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                isError = uiState.limitError != null,
                supportingText = uiState.limitError?.let { { Text(it) } },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            // Period selection
            Text("Period", style = MaterialTheme.typography.labelLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                BudgetPeriod.entries.forEach { period ->
                    FilterChip(
                        selected = uiState.period == period,
                        onClick = { viewModel.onEvent(AddEditBudgetEvent.PeriodChanged(period)) },
                        label = { Text(period.name) }
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = { viewModel.onEvent(AddEditBudgetEvent.SaveBudget) },
                enabled = !uiState.isLoading,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text(if (uiState.id == null) "Create Budget" else "Save Changes")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CategoryDropdown(
    categories: List<Category>,
    selected: Category?,
    onSelect: (Category) -> Unit,
    error: String?
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it }
    ) {
        OutlinedTextField(
            value = if (selected != null) "${selected.icon} ${selected.name}" else "",
            onValueChange = {},
            readOnly = true,
            label = { Text("Category") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            isError = error != null,
            supportingText = error?.let { { Text(it) } },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor()
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            categories.forEach { category ->
                DropdownMenuItem(
                    text = { Text("${category.icon} ${category.name}") },
                    onClick = {
                        onSelect(category)
                        expanded = false
                    }
                )
            }
        }
    }
}
```

**Step 3: Verify build**

```bash
./gradlew assembleDebug
```
Expected: `BUILD SUCCESSFUL`

**Step 4: Commit**

```bash
git add app/src/main/java/com/example/exspensify/ui/screens/AddEditBudgetScreen.kt
git commit -m "feat(ui): add AddEditBudgetScreen with category picker, limit field, period chips"
```

---

## Task 11: Wire Navigation

**Files:**
- Modify: `…/core/navigation/Routes.kt`
- Modify: `…/core/navigation/NavGraph.kt`

**What to do:**

Add `AddEditBudget` route to `Routes.kt`. Replace the `// TODO: Add BudgetScreen later` stub in `NavGraph.kt` and add the `add_edit_budget` composable destination.

`BudgetScreen` and `AddEditBudgetScreen` use `@RequiresApi(O)`, so guard their calls with `if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)` — matching the existing pattern for `StatisticsScreen` and `AddEditTransactionScreen`.

**Step 1: Read both files**

Read `…/core/navigation/Routes.kt` and `…/core/navigation/NavGraph.kt` completely.

**Step 2: Add AddEditBudget to Routes.kt**

After the `AddEditCategory` object in `Routes.kt`, add:

```kotlin
    object AddEditBudget : Routes("add_edit_budget/{budgetId}") {
        fun createRoute(budgetId: String? = null): String =
            "add_edit_budget/${budgetId ?: "new"}"
    }
```

**Step 3: Update NavGraph.kt**

Replace the stub:
```kotlin
        composable(Routes.Budgets.route) {
            // TODO: Add BudgetScreen later
        }
```

With:
```kotlin
        composable(Routes.Budgets.route) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                BudgetScreen(
                    onNavigate = { route -> navController.navigate(route) }
                )
            }
        }
```

Then add the `add_edit_budget` destination after the `add_edit_category` composable:

```kotlin
        composable(
            route = "add_edit_budget/{budgetId}",
            arguments = listOf(
                navArgument("budgetId") {
                    type = NavType.StringType
                    defaultValue = "new"
                }
            )
        ) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                AddEditBudgetScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }
        }
```

Add the required imports to `NavGraph.kt`:
```kotlin
import com.example.exspensify.ui.screens.BudgetScreen
import com.example.exspensify.ui.screens.AddEditBudgetScreen
```

**Step 4: Verify build**

```bash
./gradlew assembleDebug
```
Expected: `BUILD SUCCESSFUL`

**Step 5: Commit**

```bash
git add app/src/main/java/com/example/exspensify/core/navigation/Routes.kt
git add app/src/main/java/com/example/exspensify/core/navigation/NavGraph.kt
git commit -m "feat(nav): wire BudgetScreen and AddEditBudgetScreen into navigation graph"
```

---

## Task 12: Implement CSV Import

**Files:**
- Modify: `…/data/repository/DataManagementRepositoryImpl.kt`

**What to do:**

Replace the stub `importTransactionsFromCSV()` with a real implementation. The function:

1. Reads the CSV file from the SAF URI via `ContentResolver`
2. Parses the header row to find column indices (tolerant of column order)
3. Loads all categories once from `CategoryDao`
4. Resolves an "Other" fallback category
5. Parses each data row; skips malformed rows (counted)
6. Inserts all valid `TransactionEntity` rows in a single `withTransaction` block
7. Returns `Resource.Success(importedCount)`

Also adds a private `parseCsvLine()` helper that handles quoted fields containing commas — the same format that `exportTransactionsToCSV` writes.

**Step 1: Read the file**

Read `…/data/repository/DataManagementRepositoryImpl.kt` completely. Confirm the existing imports and class structure.

**Step 2: Replace the stub method**

Locate the existing method:
```kotlin
    override suspend fun importTransactionsFromCSV(uri: Uri): Resource<Int> {
        // TODO: Implement CSV import functionality
        ...
        return Resource.Error("Import feature not yet implemented")
    }
```

Replace it entirely with:

```kotlin
    override suspend fun importTransactionsFromCSV(uri: Uri): Resource<Int> {
        return withContext(Dispatchers.IO) {
            try {
                val allCategories = categoryDao.getAllCategoriesOnce()
                val categoryByName = allCategories.associateBy { it.name.trim().lowercase() }
                val otherCategory = allCategories.find { it.name.equals("other", ignoreCase = true) }
                    ?: allCategories.firstOrNull()
                    ?: return@withContext Resource.Error("No categories available for import")

                val toInsert = mutableListOf<TransactionEntity>()
                var skipped = 0

                context.contentResolver.openInputStream(uri)?.use { stream ->
                    stream.bufferedReader().use { reader ->
                        val headerLine = reader.readLine()
                            ?: return@withContext Resource.Error("File is empty")
                        val headers = parseCsvLine(headerLine).map { it.trim().lowercase() }

                        val dateIdx = headers.indexOf("date")
                        val titleIdx = headers.indexOf("title")
                        val amountIdx = headers.indexOf("amount")
                        val typeIdx = headers.indexOf("type")
                        val categoryIdx = headers.indexOf("category")
                        val descIdx = headers.indexOf("description")

                        if (listOf(dateIdx, titleIdx, amountIdx, typeIdx).any { it == -1 }) {
                            return@withContext Resource.Error(
                                "Invalid CSV: missing required columns (Date, Title, Amount, Type)"
                            )
                        }

                        val dateFormatter = java.time.format.DateTimeFormatter
                            .ofPattern("yyyy-MM-dd HH:mm:ss")

                        reader.forEachLine { line ->
                            if (line.isBlank()) return@forEachLine
                            try {
                                val cols = parseCsvLine(line)
                                val required = maxOf(dateIdx, titleIdx, amountIdx, typeIdx)
                                if (cols.size <= required) { skipped++; return@forEachLine }

                                val date = java.time.LocalDateTime.parse(
                                    cols[dateIdx].trim(), dateFormatter
                                )
                                val title = cols[titleIdx].trim()
                                if (title.isBlank()) { skipped++; return@forEachLine }

                                val amount = cols[amountIdx].trim().toDoubleOrNull()
                                if (amount == null || amount <= 0) { skipped++; return@forEachLine }

                                val type = when (cols[typeIdx].trim().uppercase()) {
                                    "INCOME" -> TransactionType.INCOME
                                    "EXPENSE" -> TransactionType.EXPENSE
                                    else -> { skipped++; return@forEachLine }
                                }

                                val catName = if (categoryIdx != -1 && categoryIdx < cols.size) {
                                    cols[categoryIdx].trim().lowercase()
                                } else ""
                                val resolvedCategory =
                                    categoryByName[catName] ?: otherCategory

                                val description = if (descIdx != -1 && descIdx < cols.size) {
                                    cols[descIdx].trim().ifBlank { null }
                                } else null

                                toInsert.add(
                                    TransactionEntity(
                                        id = 0L,
                                        title = title,
                                        amount = amount,
                                        type = type,
                                        categoryId = resolvedCategory.id,
                                        date = date,
                                        description = description
                                    )
                                )
                            } catch (e: Exception) {
                                skipped++
                            }
                        }
                    }
                } ?: return@withContext Resource.Error("Could not open file")

                if (toInsert.isEmpty()) {
                    return@withContext Resource.Error(
                        "No valid transactions found" +
                                if (skipped > 0) " ($skipped rows skipped)" else ""
                    )
                }

                database.withTransaction {
                    transactionDao.insertAll(toInsert)
                }

                android.util.Log.i(
                    "DataManagement",
                    "Imported ${toInsert.size} transactions, skipped $skipped"
                )
                Resource.Success(toInsert.size)

            } catch (e: java.io.IOException) {
                android.util.Log.e("DataManagement", "CSV import IO error", e)
                Resource.Error("Failed to read file: ${e.localizedMessage}")
            } catch (e: Exception) {
                android.util.Log.e("DataManagement", "CSV import error", e)
                Resource.Error(e.localizedMessage ?: "Failed to import transactions")
            }
        }
    }
```

**Step 3: Add the parseCsvLine helper**

Below `escapeCsvValue()` at the bottom of the class, add:

```kotlin
    private fun parseCsvLine(line: String): List<String> {
        val result = mutableListOf<String>()
        val current = StringBuilder()
        var inQuotes = false
        for (ch in line) {
            when {
                ch == '"' -> inQuotes = !inQuotes
                ch == ',' && !inQuotes -> {
                    result.add(current.toString())
                    current.clear()
                }
                else -> current.append(ch)
            }
        }
        result.add(current.toString())
        return result
    }
```

**Step 4: Add missing imports if not present**

Ensure these are imported at the top of the file:
```kotlin
import com.example.exspensify.data.local.entity.TransactionEntity
import com.example.exspensify.data.local.entity.TransactionType
import androidx.room.withTransaction
```

The `withTransaction` import may already be present (used by `resetDatabase()`). Check before adding.

**Step 5: Verify build**

```bash
./gradlew assembleDebug
```
Expected: `BUILD SUCCESSFUL`

**Step 6: Commit**

```bash
git add app/src/main/java/com/example/exspensify/data/repository/DataManagementRepositoryImpl.kt
git commit -m "feat(data): implement importTransactionsFromCSV with SAF, header-flexible CSV parsing"
```

---

## Task 13: Final Verification

**Step 1: Clean build**

```bash
./gradlew clean assembleDebug
```
Expected: `BUILD SUCCESSFUL`

**Step 2: Run existing tests**

```bash
./gradlew test
```
Expected: all tests pass (existing ExampleUnitTest and TransactionDaoTest).

**Step 3: Verify nav is wired correctly**

Open `NavGraph.kt`. Confirm:
- `Routes.Budgets.route` composable calls `BudgetScreen` (not a TODO)
- `"add_edit_budget/{budgetId}"` composable calls `AddEditBudgetScreen`

Open `RepositoryModule.kt`. Confirm `BudgetRepositoryImpl` is bound.

Open `Routes.kt`. Confirm `AddEditBudget` object exists with `createRoute()`.

**Step 4: Final commit**

```bash
git add -A
git commit -m "chore: final build verification — budget feature + CSV import complete"
```

---

## Summary of Changes

| Task | Files | Type |
|------|-------|------|
| 1 | `BudgetDao.kt` | Modified |
| 2 | `Budget.kt` | Modified |
| 3 | `toBudget.kt` | Created |
| 4 | `BudgetRepositoryImpl.kt` | Created |
| 5 | `RepositoryModule.kt` | Modified |
| 6 | `BudgetUiState.kt`, `AddEditBudgetUiState.kt` | Created |
| 7 | `BudgetViewModel.kt` | Created |
| 8 | `AddEditBudgetViewModel.kt` | Created |
| 9 | `BudgetScreen.kt` | Created |
| 10 | `AddEditBudgetScreen.kt` | Created |
| 11 | `Routes.kt`, `NavGraph.kt` | Modified |
| 12 | `DataManagementRepositoryImpl.kt` | Modified |
| 13 | Verification | — |
