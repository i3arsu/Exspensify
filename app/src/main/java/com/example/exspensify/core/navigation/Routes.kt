package com.example.exspensify.core.navigation

sealed class Routes(val route: String) {
    object Home : Routes("home")
    object Transactions : Routes("transactions")
    object Budgets : Routes("budgets")
    object Settings : Routes("settings")
    object AddEditTransaction : Routes("add_edit_transaction/{transactionId}") {
        fun createRoute(transactionId: String? = null): String {
            return if (transactionId != null) {
                "add_edit_transaction/$transactionId"
            } else {
                "add_edit_transaction/new"
            }
        }
    }
    object Statistics : Routes("statistics")
    object Categories : Routes("categories")

    object AddEditCategory : Routes("add_edit_category/{categoryId}") {
        fun createRoute(categoryId: String? = null): String {
            return "add_edit_category/${categoryId ?: "new"}"
        }
    }
}