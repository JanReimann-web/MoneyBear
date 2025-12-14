package com.jan.moneybear.domain

import com.jan.moneybear.data.local.Transaction
import com.jan.moneybear.data.local.TransactionDao
import com.jan.moneybear.data.remote.EnsureUserResult
import com.jan.moneybear.data.remote.RemoteDataSource
import com.jan.moneybear.data.remote.SettingsDTO
import com.jan.moneybear.data.remote.TransactionDTO
import com.jan.moneybear.data.remote.toDomain
import com.jan.moneybear.data.remote.toDto as savingsGoalToDto
import com.jan.moneybear.data.store.SettingsStore
import com.jan.moneybear.data.store.SettingsSnapshot
import com.jan.moneybear.domain.LanguageConfig
import kotlinx.coroutines.flow.first

class SyncRepository(
    private val remote: RemoteDataSource,
    private val dao: TransactionDao,
    private val settings: SettingsStore
) {

    suspend fun pullAndMerge(): Int {
        val ensure = remote.ensureUserDocument() ?: return 0
        val bootstrapped = handleBootstrap(ensure)
        val settingsMerged = if (bootstrapped) 0 else syncSettings()
        val since = settings.lastDeltaSyncAt.first()
        val deltas = remote.pullDeltas(since)
        var merged = 0
        var latestTimestamp = since
        if (deltas.isNotEmpty()) {
            deltas.forEach { dto ->
                val remoteTimestamp = dto.updatedAtServer ?: 0L
                val local = dao.getById(dto.id)
                val localDirty = local?.dirty == true
                val localTimestamp = local?.updatedAtServer ?: 0L

                if (!localDirty && (local == null || remoteTimestamp >= localTimestamp)) {
                    val appliedTimestamp = if (remoteTimestamp > 0L) remoteTimestamp else System.currentTimeMillis()
                    val entity = dto.toEntity().copy(
                        dirty = false,
                        pendingOp = null,
                        updatedAtLocal = System.currentTimeMillis(),
                        updatedAtServer = appliedTimestamp
                    )
                    dao.upsert(entity)
                    merged++
                    if (appliedTimestamp > latestTimestamp) {
                        latestTimestamp = appliedTimestamp
                    }
                }
            }
        }

        if (latestTimestamp > since) {
            settings.setLastDeltaSyncAt(latestTimestamp)
        }

        val savingsMerged = syncSavingsGoalsInternal()
        return settingsMerged + merged + savingsMerged
    }

    suspend fun pushDirtyBatch(limit: Int = 500): Int {
        val ensure = remote.ensureUserDocument() ?: return 0
        val bootstrapped = handleBootstrap(ensure)
        val dirty = dao.dirty(limit)
        val hasSavingsChanges = settings.pendingSavingsGoals().isNotEmpty()
        val settingsSnapshot = settings.snapshot()
        val settingsDirty = settingsSnapshot.dirty
        if (!bootstrapped && dirty.isEmpty() && !hasSavingsChanges && !settingsDirty) return 0

        var pushed = 0
        if (dirty.isNotEmpty()) {
            val timestamp = remote.pushDirty(dirty.map { it.toDto() })
            if (timestamp > 0L) {
                dao.markSynced(dirty.map { it.id }, timestamp)
                settings.setLastDeltaSyncAt(timestamp)
                pushed = dirty.size
            }
        }
        if (settingsDirty) {
            remote.pushSettings(settingsSnapshot.toDto())
            settings.markSettingsSynced(settingsSnapshot.updatedAt)
            pushed += 1
        }
        if (hasSavingsChanges) {
            syncSavingsGoalsInternal(fetchRemote = false)
        }
        return pushed
    }

    private suspend fun syncSettings(): Int {
        val remoteSettings = remote.fetchSettings() ?: return 0
        val localSnapshot = settings.snapshot()
        if (localSnapshot.dirty) return 0
        val remoteSnapshot = remoteSettings.toSnapshot()
        val shouldApply = when {
            remoteSnapshot.updatedAt == 0L && localSnapshot.updatedAt == 0L -> true
            remoteSnapshot.updatedAt > localSnapshot.updatedAt -> true
            else -> false
        }
        return if (shouldApply) {
            settings.applyRemoteSettings(remoteSnapshot)
            1
        } else {
            0
        }
    }

    suspend fun syncSavingsGoals(): Int {
        val ensure = remote.ensureUserDocument() ?: return 0
        handleBootstrap(ensure)
        return syncSavingsGoalsInternal()
    }

    suspend fun fetchOlderTransactions(beforeMonthExclusive: String, limit: Int = 100): List<Transaction> {
        val ensure = remote.ensureUserDocument() ?: return emptyList()
        handleBootstrap(ensure)
        val dtos = remote.fetchTransactionsBefore(beforeMonthExclusive, limit)
        return dtos.map { it.toEntity() }
    }

    private fun SettingsSnapshot.toDto(): SettingsDTO =
        SettingsDTO(
            languageCode = LanguageConfig.sanitize(languageCode),
            currencyCode = currencyCode,
            expenseCategories = expenseCategories.toList(),
            incomeCategories = incomeCategories.toList(),
            budgetMonthly = budgetMonthly,
            budgetCycleStartDay = budgetCycleStartDay,
            themeMode = themeMode,
            accentColor = accentColor,
            balanceBaselineAmount = balanceBaselineAmount,
            balanceBaselineDateMillis = balanceBaselineDateMillis,
            updatedAt = updatedAt
        )

    private fun SettingsDTO.toSnapshot(): SettingsSnapshot =
        SettingsSnapshot(
            languageCode = LanguageConfig.sanitize(languageCode),
            currencyCode = currencyCode,
            expenseCategories = expenseCategories.toList(),
            incomeCategories = incomeCategories.toList(),
            budgetMonthly = budgetMonthly,
            budgetCycleStartDay = budgetCycleStartDay,
            themeMode = themeMode,
            accentColor = accentColor,
            balanceBaselineAmount = balanceBaselineAmount,
            balanceBaselineDateMillis = balanceBaselineDateMillis,
            updatedAt = updatedAt,
            dirty = false
        )

    private fun TransactionDTO.toEntity(): Transaction =
        Transaction(
            id = id,
            uid = uid,
            amount = amount,
            currency = currency,
            dateUtcMillis = dateUtcMillis,
            monthKey = monthKey,
            category = category,
            note = note,
            planned = planned,
            deleted = deleted,
            dirty = false,
            pendingOp = null,
            updatedAtLocal = System.currentTimeMillis(),
            updatedAtServer = updatedAtServer,
            type = type,
            savingsGoalId = savingsGoalId,
            savingsImpact = savingsImpact
        )

    private fun Transaction.toDto(): TransactionDTO =
        TransactionDTO(
            id = id,
            uid = uid,
            amount = amount,
            currency = currency,
            dateUtcMillis = dateUtcMillis,
            monthKey = monthKey,
            category = category,
            note = note,
            planned = planned,
            deleted = deleted,
            type = type,
            updatedAtServer = updatedAtServer,
            savingsGoalId = savingsGoalId,
            savingsImpact = savingsImpact
        )

    private suspend fun syncSavingsGoalsInternal(fetchRemote: Boolean = true): Int {
        val remoteGoals = if (fetchRemote) remote.fetchSavingsGoals() else emptyList()
        var total = if (remoteGoals.isNotEmpty()) {
            settings.mergeRemoteSavingsGoals(remoteGoals.map { it.toDomain() })
        } else {
            0
        }
        val pending = settings.pendingSavingsGoals()
        if (pending.isNotEmpty()) {
            val now = System.currentTimeMillis()
            val normalized = pending.map { goal ->
                val updatedAt = if (goal.updatedAt > 0L) goal.updatedAt else now
                goal.copy(updatedAt = updatedAt)
            }
            remote.pushSavingsGoals(normalized.map { it.savingsGoalToDto(now) })
            val acknowledgements = normalized.map { it.copy(pendingOp = null) }
            settings.markSavingsGoalsSynced(acknowledgements)
            total += normalized.size
        }
        return total
    }

    private suspend fun handleBootstrap(result: EnsureUserResult): Boolean {
        if (!result.bootstrapped) return false
        val now = System.currentTimeMillis()
        dao.markAllDirty(now)
        settings.markAllDirtyForBootstrap(now)
        settings.setLastDeltaSyncAt(0)
        return true
    }
}

