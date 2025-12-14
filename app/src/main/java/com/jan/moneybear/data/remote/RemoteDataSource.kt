package com.jan.moneybear.data.remote

interface RemoteDataSource {
    suspend fun ensureUserDocument(): EnsureUserResult?
    suspend fun pullDeltas(since: Long): List<TransactionDTO>
    suspend fun pushDirty(items: List<TransactionDTO>): Long
    suspend fun fetchTransactionsBefore(monthKeyExclusive: String, limit: Int = 100): List<TransactionDTO>
    suspend fun fetchSavingsGoals(): List<SavingsGoalDTO>
    suspend fun pushSavingsGoals(goals: List<SavingsGoalDTO>)
    suspend fun fetchSettings(): SettingsDTO?
    suspend fun pushSettings(settings: SettingsDTO)
}
