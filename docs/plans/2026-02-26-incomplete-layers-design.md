# Design: Implement Incomplete Layers
**Date:** 2026-02-26
**Scope:** Budget Feature (ViewModel + Screen + nav wiring) + CSV Import

---

## Context

The data layer for budgets is fully implemented (BudgetEntity, BudgetDao, BudgetRepository interface). No ViewModel, screen, or nav wiring exists. CSV import is stubbed with `Resource.Error("Not implemented")`.

All entity IDs are `Long` (autoGenerate). Domain models represent IDs as `String` via `.toString()` conversion in mappers — consistent with Transaction and Category patterns.

---

## Budget Feature

### `spent` Computation — SQL JOIN (CLAUDE.md rule 4 compliant)

Add `getAllBudgetsWithSpent()` to `BudgetDao`. All aggregation is in SQL; no in-memory summation.

```sql
SELECT b.id, b.categoryId, b.amount, b.period, b.startDate, b.endDate, b.createdAt,
       COALESCE(SUM(CASE WHEN t.type = 'EXPENSE' THEN t.amount ELSE 0 END), 0.0) AS spentAmount
FROM budgets b
LEFT JOIN transactions t
  ON t.categoryId = b.categoryId
 AND t.date >= b.startDate
 AND t.date <= b.endDate
GROUP BY b.id
ORDER BY b.startDate DESC
```

`Budget.spent` is populated from `spentAmount` — no Flow combining, no in-memory aggregation.

### Budget Period Date Computation

Computed at save time, stored in `BudgetEntity.startDate`/`endDate` (Long epoch ms):

| Period  | startDate            | endDate                    |
|---------|----------------------|----------------------------|
| WEEKLY  | Monday of this week  | Sunday of this week        |
| MONTHLY | 1st of this month    | Last day of this month     |
| YEARLY  | Jan 1 of this year   | Dec 31 of this year        |

Each budget covers one fixed window. No recurring logic.

### New Files (7)

| File | Purpose |
|------|---------|
| `data/local/mapper/toBudget.kt` | Maps `BudgetWithSpent` ↔ `Budget` domain model |
| `data/repository/BudgetRepositoryImpl.kt` | Implements `BudgetRepository` |
| `ui/budget/BudgetViewModel.kt` | StateFlow + events for budget list dashboard |
| `ui/budget/BudgetUiState.kt` | `BudgetUiState`, `BudgetEvent` |
| `ui/budget/AddEditBudgetViewModel.kt` | StateFlow + events for add/edit form |
| `ui/budget/AddEditBudgetUiState.kt` | `AddEditBudgetUiState`, `AddEditBudgetEvent` |
| `ui/screens/BudgetScreen.kt` | Dashboard: summary card + list with progress bars |
| `ui/screens/AddEditBudgetScreen.kt` | Form: category picker, limit field, period chips |

### Modified Files (4)

| File | Change |
|------|--------|
| `data/local/dao/BudgetDao.kt` | Add `getAllBudgetsWithSpent()` + `BudgetWithSpent` data class |
| `di/RepositoryModule.kt` | `@Binds` for `BudgetRepositoryImpl → BudgetRepository` |
| `core/navigation/Routes.kt` | Add `AddEditBudget` route with `createRoute(id?)` |
| `core/navigation/NavGraph.kt` | Wire `BudgetScreen` and `AddEditBudgetScreen` |

### BudgetScreen Layout (Dashboard-style)

```
┌──────────────────────────────────────────┐
│  Budgets                          [+ FAB] │
├──────────────────────────────────────────┤
│  OVERVIEW CARD                           │
│  Total Budgeted: $1,200                  │
│  Total Spent:    $843                    │
│  [████████░░░░] 70%                      │
├──────────────────────────────────────────┤
│  BUDGET LIST                             │
│  ┌────────────────────────────────────┐  │
│  │ 🍔 Food & Dining  MONTHLY          │  │
│  │ $420 spent of $500                 │  │
│  │ [████████░░] 84%  ⚠ Near limit    │  │
│  └────────────────────────────────────┘  │
│  ┌────────────────────────────────────┐  │
│  │ 🚗 Transportation  MONTHLY         │  │
│  │ $180 spent of $300                 │  │
│  │ [██████░░░░] 60%                   │  │
│  └────────────────────────────────────┘  │
└──────────────────────────────────────────┘
```

Progress bar color: green < 75%, amber 75–99%, red ≥ 100%.

---

## CSV Import

### Strategy

- **Unknown categories** → fallback to "Other" built-in category (no data loss)
- **Malformed rows** → skip and count; report in success message
- **All inserts** in a single `withTransaction` block

### Algorithm

```
1. Open URI via ContentResolver
2. Read header row → build column-index map
   (columns: Date, Title, Amount, Type, Category, Description)
3. Load all categories once → name→id map (case-insensitive)
4. Resolve "Other" fallback categoryId from the map
5. For each data row:
   a. Parse LocalDateTime ("yyyy-MM-dd HH:mm:ss")
   b. Parse amount as Double
   c. Parse TransactionType ("INCOME" / "EXPENSE")
   d. Lookup categoryId by name; fallback to "Other" id
   e. Build TransactionEntity (id=0 → autoGenerate)
6. Insert all valid rows in a single DB withTransaction
7. Return Resource.Success(importedCount)
   (message includes skipped count if > 0)
```

### Modified File

| File | Change |
|------|--------|
| `data/repository/DataManagementRepositoryImpl.kt` | Implement `importTransactionsFromCSV()` |

---

## Constraints

- Follow all CLAUDE.md rules (no SQL in ViewModel, no validation in Composables, IO dispatcher for all DB ops)
- No refactoring of unrelated files
- Collect StateFlow via `collectAsStateWithLifecycle()`
- All new ViewModels extend `BaseViewModel` and use `@HiltViewModel`
