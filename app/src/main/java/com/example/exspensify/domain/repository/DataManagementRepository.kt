package com.example.exspensify.domain.repository

import android.net.Uri
import com.example.exspensify.core.util.Resource

interface DataManagementRepository {
    
    /**
     * Reset database - delete all transactions and custom categories
     * Re-seed default categories
     * Atomic operation
     */
    suspend fun resetDatabase(): Resource<Unit>
    
    /**
     * Export all transactions to CSV
     * @param uri The URI where the CSV file should be written
     * @return Success with number of exported rows or Error
     */
    suspend fun exportTransactionsToCSV(uri: Uri): Resource<Int>
    
    /**
     * Get count of transactions (to check if export should be enabled)
     */
    suspend fun getTransactionCount(): Int
    
    /**
     * Import transactions from CSV (placeholder for future implementation)
     * TODO: Implement CSV import functionality
     */
    suspend fun importTransactionsFromCSV(uri: Uri): Resource<Int>
}