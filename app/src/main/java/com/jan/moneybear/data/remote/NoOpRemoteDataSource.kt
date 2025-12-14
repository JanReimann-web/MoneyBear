package com.jan.moneybear.data.remote

/**
 * No-op implementation of RemoteDataSource for local-only data storage.
 * All methods return empty results or do nothing.
 */
class NoOpRemoteDataSource : RemoteDataSource {
    override suspend fun ensureUserDocument(): EnsureUserResult? = null
    
    override suspend fun pullDeltas(since: Long): List<TransactionDTO> = emptyList()
    
    override suspend fun pushDirty(items: List<TransactionDTO>): Long = 0L
    
    override suspend fun fetchTransactionsBefore(monthKeyExclusive: String, limit: Int): List<TransactionDTO> = emptyList()
    
    override suspend fun fetchSavingsGoals(): List<SavingsGoalDTO> = emptyList()
    
    override suspend fun pushSavingsGoals(goals: List<SavingsGoalDTO>) {
        // No-op
    }
    
    override suspend fun fetchSettings(): SettingsDTO? = null
    
    override suspend fun pushSettings(settings: SettingsDTO) {
        // No-op
    }
}





