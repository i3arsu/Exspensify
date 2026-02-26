package com.example.exspensify.core.navigation

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.navigation.compose.composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.navArgument
import com.example.exspensify.ui.home.HomeScreen
import com.example.exspensify.ui.screens.AddEditCategoryScreen
import com.example.exspensify.ui.screens.AddEditTransactionScreen
import com.example.exspensify.ui.screens.AddEditBudgetScreen
import com.example.exspensify.ui.screens.BudgetScreen
import com.example.exspensify.ui.screens.CategoryListScreen
import com.example.exspensify.ui.screens.StatisticsScreen
import com.example.exspensify.ui.screens.TransactionScreen
import com.example.exspensify.ui.screens.SettingsScreen

@Composable
fun NavigationGraph(
    navController: NavHostController,
    paddingValues: PaddingValues
) {
    NavHost(
        navController = navController,
        startDestination = Routes.Home.route,
        modifier = Modifier.padding(paddingValues)
    ) {
        composable(Routes.Home.route) {
            HomeScreen(
                onNavigate = { route ->
                    navController.navigate(route)
                }
            )
        }

        composable(Routes.Transactions.route) {
            // TODO: Add TransactionScreen later
        }

        composable(Routes.Budgets.route) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                BudgetScreen(
                    onNavigate = { route -> navController.navigate(route) }
                )
            }
        }

        composable(Routes.Settings.route) {
            SettingsScreen()
        }

        // Add/Edit Transaction Screen
        composable(
            route = "add_edit_transaction/{transactionId}",
            arguments = listOf(
                navArgument("transactionId") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = "new"
                }
            )
        ) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                AddEditTransactionScreen(
                    onNavigateBack = {
                        navController.popBackStack()
                    }
                )
            }
        }
        composable(Routes.Transactions.route) {
            TransactionScreen(
                onNavigate = { route ->
                    navController.navigate(route)
                }
            )
        }
        composable(Routes.Statistics.route) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                StatisticsScreen()
            }
        }
        composable(Routes.Categories.route) {
            CategoryListScreen(
                onNavigate = { route -> navController.navigate(route) },
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(
            route = "add_edit_category/{categoryId}",
            arguments = listOf(
                navArgument("categoryId") {
                    type = NavType.StringType
                }
            )
        ) {
            AddEditCategoryScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

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
    }
}