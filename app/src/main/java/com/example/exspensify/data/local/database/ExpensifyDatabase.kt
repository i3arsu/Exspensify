package com.example.exspensify.data.local.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.exspensify.data.local.converters.Converters
import com.example.exspensify.data.local.dao.*
import com.example.exspensify.data.local.entity.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        TransactionEntity::class,
        CategoryEntity::class,
        BudgetEntity::class
    ],
    version = 4,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class ExpensifyDatabase : RoomDatabase() {

    abstract fun transactionDao(): TransactionDao
    abstract fun categoryDao(): CategoryDao
    abstract fun budgetDao(): BudgetDao
    abstract fun statisticsDao(): StatisticsDao

    companion object {
        @Volatile
        private var INSTANCE: ExpensifyDatabase? = null

        fun getDatabase(
            context: Context,
            scope: CoroutineScope
        ): ExpensifyDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    ExpensifyDatabase::class.java,
                    "exspensify_database"
                )
                    .addCallback(DatabaseCallback(scope))
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private class DatabaseCallback(
            private val scope: CoroutineScope
        ) : RoomDatabase.Callback() {

            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                INSTANCE?.let { database ->
                    scope.launch(Dispatchers.IO) {
                        seedDefaultCategories(database.categoryDao())
                    }
                }
            }

            // Also check on open in case database was migrated
            override fun onOpen(db: SupportSQLiteDatabase) {
                super.onOpen(db)
                INSTANCE?.let { database ->
                    scope.launch(Dispatchers.IO) {
                        seedDefaultCategories(database.categoryDao())
                    }
                }
            }
        }

        /**
         * Seed default categories if they don't exist
         * Uses INSERT IGNORE to prevent duplicates
         */
        private suspend fun seedDefaultCategories(categoryDao: CategoryDao) {
            try {
                val existingCount = categoryDao.getCategoryCount()
                android.util.Log.d("ExpensifyDB", "Existing categories: $existingCount")

                if (existingCount == 0) {
                    categoryDao.insertAll(DefaultCategories.defaults)
                    android.util.Log.d("ExpensifyDB", "Seeded ${DefaultCategories.defaults.size} default categories")
                }
            } catch (e: Exception) {
                android.util.Log.e("ExpensifyDB", "Error seeding categories", e)
            }
        }
    }
}