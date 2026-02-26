package com.example.exspensify.di

import android.content.Context
import com.example.exspensify.data.local.dao.BudgetDao
import com.example.exspensify.data.local.dao.CategoryDao
import com.example.exspensify.data.local.dao.StatisticsDao
import com.example.exspensify.data.local.dao.TransactionDao
import com.example.exspensify.data.local.database.ExpensifyDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    
    @Provides
    @Singleton
    fun provideApplicationScope(): CoroutineScope {
        return CoroutineScope(SupervisorJob())
    }

    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context,
        scope: CoroutineScope
    ): ExpensifyDatabase {
        return ExpensifyDatabase.getDatabase(context, scope)
    }
    
    @Provides
    fun provideTransactionDao(database: ExpensifyDatabase): TransactionDao {
        return database.transactionDao()
    }
    
    @Provides
    fun provideCategoryDao(database: ExpensifyDatabase): CategoryDao {
        return database.categoryDao()
    }
    
    @Provides
    fun provideBudgetDao(database: ExpensifyDatabase): BudgetDao {
        return database.budgetDao()
    }

    @Provides
    @Singleton
    fun provideStatisticsDao(database: ExpensifyDatabase): StatisticsDao {
        return database.statisticsDao()
    }
}