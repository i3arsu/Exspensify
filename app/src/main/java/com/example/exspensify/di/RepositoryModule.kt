package com.example.exspensify.di

import com.example.exspensify.data.repository.CategoryRepositoryImpl
import com.example.exspensify.data.repository.DataManagementRepositoryImpl
import com.example.exspensify.data.repository.SettingsRepositoryImpl
import com.example.exspensify.data.repository.StatisticsRepositoryImpl
import com.example.exspensify.data.repository.TransactionRepositoryImpl
import com.example.exspensify.domain.repository.CategoryRepository
import com.example.exspensify.domain.repository.DataManagementRepository
import com.example.exspensify.domain.repository.SettingsRepository
import com.example.exspensify.domain.repository.StatisticsRepository
import com.example.exspensify.domain.repository.TransactionRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindTransactionRepository(
        transactionRepositoryImpl: TransactionRepositoryImpl
    ): TransactionRepository

    @Binds
    @Singleton
    abstract fun bindCategoryRepository(
        categoryRepositoryImpl: CategoryRepositoryImpl
    ): CategoryRepository

    @Binds
    @Singleton
    abstract fun bindStatisticsRepository(
        statisticsRepositoryImpl: StatisticsRepositoryImpl
    ): StatisticsRepository

    @Binds
    @Singleton
    abstract fun bindSettingsRepository(
        settingsRepositoryImpl: SettingsRepositoryImpl
    ): SettingsRepository

    @Binds
    @Singleton
    abstract fun bindDataManagementRepository(
        dataManagementRepositoryImpl: DataManagementRepositoryImpl
    ): DataManagementRepository
}