package com.jan.moneybear.data.store

import android.content.Context
import android.util.Base64
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.jan.moneybear.R
import com.jan.moneybear.data.local.BudgetEntity
import com.jan.moneybear.data.local.ExpenseCategoryEntity
import com.jan.moneybear.data.local.SettingsDao
import com.jan.moneybear.data.local.SavingsGoalEntity
import com.jan.moneybear.domain.defaultExpenseCategories
import com.jan.moneybear.domain.defaultIncomeCategories
import com.jan.moneybear.domain.LanguageConfig
import com.jan.moneybear.domain.SettingsDefaults
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.util.Locale
import java.util.UUID
import kotlin.text.Charsets

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

data class SavingsGoal(
    val id: String,
    val name: String,
    val target: Double,
    val deadlineMillis: Long? = null, // Tähtaeg kuupäevana (millisekundites)
    val updatedAt: Long = 0L,
    val deleted: Boolean = false,
    val pendingOp: String? = null
) {
    val dirty: Boolean get() = pendingOp != null
}

data class BalanceBaseline(
    val amount: Double,
    val dateMillis: Long
)

data class SettingsSnapshot(
    val languageCode: String,
    val currencyCode: String,
    val expenseCategories: List<String>,
    val incomeCategories: List<String>,
    val budgetMonthly: Double?,
    val budgetCycleStartDay: Int,
    val themeMode: String,
    val accentColor: String,
    val balanceBaselineAmount: Double? = null,
    val balanceBaselineDateMillis: Long? = null,
    val updatedAt: Long,
    val dirty: Boolean
)

class SettingsStore(
    private val context: Context,
    private val settingsDao: SettingsDao? = null
) {

    companion object {
        private val LANGUAGE_KEY = stringPreferencesKey("languageCode")
        private val CURRENCY_KEY = stringPreferencesKey("currencyCode")
        private val EXPENSE_CATEGORIES_KEY = stringPreferencesKey("expense_categories")
        private val INCOME_CATEGORIES_KEY = stringPreferencesKey("income_categories")
        private val BUDGET_KEY = doublePreferencesKey("budgetMonthly")
        private val BUDGET_CYCLE_START_DAY_KEY = intPreferencesKey("budgetCycleStartDay")
        private val LEGACY_BUDGET_CYCLE_END_DAY_KEY = intPreferencesKey("budgetCycleEndDay")
        private val LAST_DELTA_KEY = longPreferencesKey("lastDeltaSyncAt")
        private val THEME_MODE_KEY = stringPreferencesKey("themeMode")
        private val ACCENT_COLOR_KEY = stringPreferencesKey("accentColor")
        private val SAVINGS_GOALS_KEY = stringPreferencesKey("savingsGoals")
        private val LEGACY_SAVINGS_GOAL_KEY = doublePreferencesKey("savingsGoal")
        private val LEGACY_SAVINGS_PROGRESS_KEY = doublePreferencesKey("savingsProgress")
        private val BALANCE_BASELINE_AMOUNT_KEY = doublePreferencesKey("balanceBaselineAmount")
        private val BALANCE_BASELINE_DATE_KEY = longPreferencesKey("balanceBaselineDateMillis")
        private val SETTINGS_UPDATED_AT_KEY = longPreferencesKey("settingsUpdatedAt")
        private val SETTINGS_DIRTY_KEY = booleanPreferencesKey("settingsDirty")

        private const val DEFAULT_LANGUAGE = SettingsDefaults.LANGUAGE
        private const val DEFAULT_CURRENCY = SettingsDefaults.CURRENCY
        private const val DEFAULT_THEME_MODE = SettingsDefaults.THEME_MODE
        private const val DEFAULT_ACCENT_COLOR = SettingsDefaults.ACCENT_COLOR
        private const val DEFAULT_BUDGET_CYCLE_START_DAY = SettingsDefaults.BUDGET_CYCLE_START_DAY
        private val LEGACY_EXPENSE_CATEGORIES = listOf("Eluase", "Transport", "Toit", "Meelelahutus", "Muu")
        private val LEGACY_INCOME_CATEGORIES = listOf("Palk", "Bonus", "Muu")

        private const val GOAL_DELIMITER = ";"
        private const val GOAL_FIELD_DELIMITER = "|"
        private const val LEGACY_GOAL_ID = "legacy-default-goal"
        private const val OP_INSERT = "INSERT"
        private const val OP_UPDATE = "UPDATE"
        private const val OP_DELETE = "DELETE"

        private fun MutablePreferences.setCategories(key: Preferences.Key<String>, categories: List<String>) {
            this[key] = categories.joinToString(",")
        }

        private fun Preferences.readCategories(
            key: Preferences.Key<String>,
            fallback: List<String>,
            legacyFallback: List<String>
        ): List<String> {
            val stored = this[key]
            if (stored.isNullOrBlank()) return fallback
            val parsed = stored.split(",").map { it.trim() }.filter { it.isNotEmpty() }
            return if (parsed == legacyFallback) fallback else parsed
        }

        private fun encodeGoals(goals: List<SavingsGoal>): String =
            goals.joinToString(GOAL_DELIMITER) { goal ->
                val encodedName = Base64.encodeToString(goal.name.toByteArray(Charsets.UTF_8), Base64.NO_WRAP)
                listOf(
                    goal.id,
                    encodedName,
                    goal.target.toString(),
                    goal.deadlineMillis?.toString() ?: "",
                    goal.updatedAt.toString(),
                    if (goal.deleted) "1" else "0",
                    goal.pendingOp ?: ""
                ).joinToString(GOAL_FIELD_DELIMITER)
            }

        private fun decodeGoals(raw: String?): List<SavingsGoal> {
            if (raw.isNullOrBlank()) return emptyList()
            return raw.split(GOAL_DELIMITER).mapNotNull { entry ->
                val parts = entry.split(GOAL_FIELD_DELIMITER)
                if (parts.size < 3) return@mapNotNull null
                val id = parts[0]
                val name = runCatching {
                    val bytes = Base64.decode(parts[1], Base64.NO_WRAP)
                    String(bytes, Charsets.UTF_8)
                }.getOrNull() ?: return@mapNotNull null
                val target = parts[2].toDoubleOrNull() ?: return@mapNotNull null
                // Tähtaeg on nüüd 4. väljal (kui on), muidu on see tühi string
                val deadlineMillis = parts.getOrNull(3)?.takeIf { it.isNotBlank() }?.toLongOrNull()
                val updatedAt = parts.getOrNull(4)?.toLongOrNull() ?: 0L
                val deleted = parts.getOrNull(5)?.let(::parseBooleanFlag) ?: false
                val pending = parts.getOrNull(6)?.takeIf { it.isNotBlank() }
                SavingsGoal(id = id, name = name, target = target, deadlineMillis = deadlineMillis, updatedAt = updatedAt, deleted = deleted, pendingOp = pending)
            }
        }

        private fun convertEndDayToStart(endDay: Int): Int = when {
            endDay <= 0 -> 1
            endDay >= 31 -> 1
            else -> (endDay + 1).coerceAtMost(31)
        }

        private fun parseBooleanFlag(raw: String): Boolean =
            raw == "1" || raw.equals("true", ignoreCase = true)

        private fun MutablePreferences.markSettingsDirty(now: Long) {
            this[SETTINGS_UPDATED_AT_KEY] = now
            this[SETTINGS_DIRTY_KEY] = true
        }

        private fun MutablePreferences.markSettingsClean(updatedAt: Long) {
            this[SETTINGS_UPDATED_AT_KEY] = updatedAt
            this[SETTINGS_DIRTY_KEY] = false
        }
    }

    private fun List<SavingsGoal>.removeStaleDeletes(): List<SavingsGoal> =
        filterNot { it.deleted && it.pendingOp == null }

    private fun List<SavingsGoal>.sortedByName(): List<SavingsGoal> =
        sortedBy { it.name.lowercase(Locale.getDefault()) }

    private fun List<SavingsGoal>.displayable(): List<SavingsGoal> =
        removeStaleDeletes()
            .filterNot { it.deleted }
            .sortedByName()

    val languageCode: Flow<String> = context.dataStore.data.map { prefs ->
        val stored = prefs[LANGUAGE_KEY]
        LanguageConfig.sanitize(stored ?: LanguageConfig.deviceDefault())
    }

    val currencyCode: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[CURRENCY_KEY] ?: DEFAULT_CURRENCY
    }

    val expenseCategories: Flow<List<String>> = context.dataStore.data.map { prefs ->
        prefs.readCategories(EXPENSE_CATEGORIES_KEY, defaultExpenseCategories(context), LEGACY_EXPENSE_CATEGORIES)
    }

    val incomeCategories: Flow<List<String>> = context.dataStore.data.map { prefs ->
        prefs.readCategories(INCOME_CATEGORIES_KEY, defaultIncomeCategories(context), LEGACY_INCOME_CATEGORIES)
    }

    val budgetMonthly: Flow<Double?> = context.dataStore.data.map { prefs ->
        prefs[BUDGET_KEY]
    }

    val budgetCycleStartDay: Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[BUDGET_CYCLE_START_DAY_KEY]
            ?: prefs[LEGACY_BUDGET_CYCLE_END_DAY_KEY]?.let { convertEndDayToStart(it) }
            ?: DEFAULT_BUDGET_CYCLE_START_DAY
    }

    val lastDeltaSyncAt: Flow<Long> = context.dataStore.data.map { prefs ->
        prefs[LAST_DELTA_KEY] ?: 0L
    }

    val themeMode: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[THEME_MODE_KEY] ?: DEFAULT_THEME_MODE
    }

    val accentColor: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[ACCENT_COLOR_KEY] ?: DEFAULT_ACCENT_COLOR
    }

    val balanceBaselineAmount: Flow<Double?> = context.dataStore.data.map { prefs ->
        prefs[BALANCE_BASELINE_AMOUNT_KEY]
    }

    val balanceBaselineDateMillis: Flow<Long?> = context.dataStore.data.map { prefs ->
        prefs[BALANCE_BASELINE_DATE_KEY]
    }

    val balanceBaseline: Flow<BalanceBaseline?> = context.dataStore.data.map { prefs ->
        val amount = prefs[BALANCE_BASELINE_AMOUNT_KEY]
        val date = prefs[BALANCE_BASELINE_DATE_KEY]
        if (amount != null && date != null) {
            BalanceBaseline(amount = amount, dateMillis = date)
        } else {
            null
        }
    }

    val savingsGoals: Flow<List<SavingsGoal>> = context.dataStore.data.map { prefs ->
        val stored = decodeGoals(prefs[SAVINGS_GOALS_KEY]).displayable()
        if (stored.isNotEmpty()) {
            stored
        } else {
            val legacyTarget = prefs[LEGACY_SAVINGS_GOAL_KEY]
            if (legacyTarget != null && legacyTarget > 0.0) {
                val fallbackName = context.getString(R.string.default_savings_goal_name)
                listOf(SavingsGoal(id = LEGACY_GOAL_ID, name = fallbackName, target = legacyTarget))
            } else {
                emptyList()
            }
        }
    }

    suspend fun snapshot(): SettingsSnapshot {
        val prefs = context.dataStore.data.first()
        val startDay = prefs[BUDGET_CYCLE_START_DAY_KEY]
            ?: prefs[LEGACY_BUDGET_CYCLE_END_DAY_KEY]?.let { convertEndDayToStart(it) }
            ?: DEFAULT_BUDGET_CYCLE_START_DAY
        val sanitizedLanguage = LanguageConfig.sanitize(prefs[LANGUAGE_KEY] ?: LanguageConfig.deviceDefault())
        return SettingsSnapshot(
            languageCode = sanitizedLanguage,
            currencyCode = prefs[CURRENCY_KEY] ?: DEFAULT_CURRENCY,
            expenseCategories = prefs.readCategories(EXPENSE_CATEGORIES_KEY, defaultExpenseCategories(context), LEGACY_EXPENSE_CATEGORIES),
            incomeCategories = prefs.readCategories(INCOME_CATEGORIES_KEY, defaultIncomeCategories(context), LEGACY_INCOME_CATEGORIES),
            budgetMonthly = prefs[BUDGET_KEY],
            budgetCycleStartDay = startDay,
            themeMode = prefs[THEME_MODE_KEY] ?: DEFAULT_THEME_MODE,
            accentColor = prefs[ACCENT_COLOR_KEY] ?: DEFAULT_ACCENT_COLOR,
            balanceBaselineAmount = prefs[BALANCE_BASELINE_AMOUNT_KEY],
            balanceBaselineDateMillis = prefs[BALANCE_BASELINE_DATE_KEY],
            updatedAt = prefs[SETTINGS_UPDATED_AT_KEY] ?: 0L,
            dirty = prefs[SETTINGS_DIRTY_KEY] ?: false
        )
    }

    suspend fun applyRemoteSettings(snapshot: SettingsSnapshot) {
        val remoteExpense = snapshot.expenseCategories.map { it.trim() }.filter { it.isNotEmpty() }.distinct()
        val remoteIncome = snapshot.incomeCategories.map { it.trim() }.filter { it.isNotEmpty() }.distinct()
        val sanitizedBudget = snapshot.budgetMonthly?.takeIf { it >= 0.0 }
        val sanitizedStartDay = snapshot.budgetCycleStartDay.coerceIn(1, 31)
        val language = LanguageConfig.sanitize(snapshot.languageCode)
        val remoteBaselineAmount = snapshot.balanceBaselineAmount?.takeIf { it >= 0.0 }
        val remoteBaselineDate = snapshot.balanceBaselineDateMillis?.takeIf { it > 0L }
        context.dataStore.edit { prefs ->
            prefs[LANGUAGE_KEY] = language
            prefs[CURRENCY_KEY] = snapshot.currencyCode
            if (remoteExpense.isNotEmpty()) {
                prefs.setCategories(EXPENSE_CATEGORIES_KEY, remoteExpense)
            } else {
                prefs.remove(EXPENSE_CATEGORIES_KEY)
            }
            if (remoteIncome.isNotEmpty()) {
                prefs.setCategories(INCOME_CATEGORIES_KEY, remoteIncome)
            } else {
                prefs.remove(INCOME_CATEGORIES_KEY)
            }
            if (sanitizedBudget != null) {
                prefs[BUDGET_KEY] = sanitizedBudget
            } else {
                prefs.remove(BUDGET_KEY)
            }
            if (sanitizedStartDay == DEFAULT_BUDGET_CYCLE_START_DAY) {
                prefs.remove(BUDGET_CYCLE_START_DAY_KEY)
            } else {
                prefs[BUDGET_CYCLE_START_DAY_KEY] = sanitizedStartDay
            }
            prefs.remove(LEGACY_BUDGET_CYCLE_END_DAY_KEY)
            if (remoteBaselineAmount != null && remoteBaselineDate != null) {
                prefs[BALANCE_BASELINE_AMOUNT_KEY] = remoteBaselineAmount
                prefs[BALANCE_BASELINE_DATE_KEY] = remoteBaselineDate
            } else {
                prefs.remove(BALANCE_BASELINE_AMOUNT_KEY)
                prefs.remove(BALANCE_BASELINE_DATE_KEY)
            }
            prefs[THEME_MODE_KEY] = snapshot.themeMode
            prefs[ACCENT_COLOR_KEY] = snapshot.accentColor
            prefs.markSettingsClean(snapshot.updatedAt)
        }
        persistExpenseCategories(remoteExpense, snapshot.updatedAt)
        persistBudget(sanitizedBudget, snapshot.updatedAt)
    }

    suspend fun markSettingsSynced(updatedAt: Long) {
        context.dataStore.edit { prefs ->
            prefs.markSettingsClean(updatedAt)
        }
    }

    suspend fun setLanguage(code: String) {
        val sanitized = LanguageConfig.sanitize(code)
        val now = System.currentTimeMillis()
        context.dataStore.edit { prefs ->
            val current = LanguageConfig.sanitize(prefs[LANGUAGE_KEY] ?: LanguageConfig.deviceDefault())
            if (current != sanitized) {
                prefs[LANGUAGE_KEY] = sanitized
                prefs.markSettingsDirty(now)
            }
        }
    }

    suspend fun setCurrency(code: String) {
        val sanitized = code.ifBlank { DEFAULT_CURRENCY }
        val now = System.currentTimeMillis()
        context.dataStore.edit { prefs ->
            val current = prefs[CURRENCY_KEY] ?: DEFAULT_CURRENCY
            if (current != sanitized) {
                prefs[CURRENCY_KEY] = sanitized
                prefs.markSettingsDirty(now)
            }
        }
    }

    suspend fun setExpenseCategories(categories: List<String>) {
        val sanitized = categories.map { it.trim() }.filter { it.isNotEmpty() }.distinct()
        val serialized = sanitized.takeIf { it.isNotEmpty() }?.joinToString(",")
        val now = System.currentTimeMillis()
        var changed = false
        context.dataStore.edit { prefs ->
            val currentRaw = prefs[EXPENSE_CATEGORIES_KEY]
            if (currentRaw != serialized) {
                if (serialized != null) {
                    prefs.setCategories(EXPENSE_CATEGORIES_KEY, sanitized)
                } else {
                    prefs.remove(EXPENSE_CATEGORIES_KEY)
                }
                prefs.markSettingsDirty(now)
                changed = true
            }
        }
        if (changed) {
            persistExpenseCategories(sanitized, now)
        }
    }

    suspend fun setIncomeCategories(categories: List<String>) {
        val sanitized = categories.map { it.trim() }.filter { it.isNotEmpty() }.distinct()
        val serialized = sanitized.takeIf { it.isNotEmpty() }?.joinToString(",")
        val now = System.currentTimeMillis()
        context.dataStore.edit { prefs ->
            val currentRaw = prefs[INCOME_CATEGORIES_KEY]
            if (currentRaw != serialized) {
                if (serialized != null) {
                    prefs.setCategories(INCOME_CATEGORIES_KEY, sanitized)
                } else {
                    prefs.remove(INCOME_CATEGORIES_KEY)
                }
                prefs.markSettingsDirty(now)
            }
        }
    }

    suspend fun setBudgetMonthly(budget: Double?) {
        val sanitized = budget?.takeIf { it >= 0.0 }
        val now = System.currentTimeMillis()
        var changed = false
        context.dataStore.edit { prefs ->
            val current = prefs[BUDGET_KEY]
            when {
                sanitized == null && current != null -> {
                    prefs.remove(BUDGET_KEY)
                    prefs.markSettingsDirty(now)
                    changed = true
                }
                sanitized != null && (current == null || current != sanitized) -> {
                    prefs[BUDGET_KEY] = sanitized
                    prefs.markSettingsDirty(now)
                    changed = true
                }
            }
        }
        if (changed) {
            persistBudget(sanitized, now)
        }
    }

    suspend fun setBudgetCycleStartDay(dayOfMonth: Int?) {
        val sanitized = dayOfMonth?.coerceIn(1, 31)
        val now = System.currentTimeMillis()
        context.dataStore.edit { prefs ->
            val current = prefs[BUDGET_CYCLE_START_DAY_KEY] ?: DEFAULT_BUDGET_CYCLE_START_DAY
            val newValue = sanitized ?: DEFAULT_BUDGET_CYCLE_START_DAY
            if (newValue != current) {
                if (sanitized == null || sanitized == DEFAULT_BUDGET_CYCLE_START_DAY) {
                    prefs.remove(BUDGET_CYCLE_START_DAY_KEY)
                } else {
                    prefs[BUDGET_CYCLE_START_DAY_KEY] = sanitized
                }
                prefs.markSettingsDirty(now)
            }
            prefs.remove(LEGACY_BUDGET_CYCLE_END_DAY_KEY)
        }
    }

    suspend fun setLastDeltaSyncAt(millis: Long) {
        context.dataStore.edit { prefs ->
            prefs[LAST_DELTA_KEY] = millis
        }
    }

    suspend fun setThemeMode(mode: String) {
        val sanitized = mode.ifBlank { DEFAULT_THEME_MODE }
        val now = System.currentTimeMillis()
        context.dataStore.edit { prefs ->
            val current = prefs[THEME_MODE_KEY] ?: DEFAULT_THEME_MODE
            if (current != sanitized) {
                prefs[THEME_MODE_KEY] = sanitized
                prefs.markSettingsDirty(now)
            }
        }
    }

    suspend fun setAccentColor(accentId: String) {
        val sanitized = accentId.ifBlank { DEFAULT_ACCENT_COLOR }
        val now = System.currentTimeMillis()
        context.dataStore.edit { prefs ->
            val current = prefs[ACCENT_COLOR_KEY] ?: DEFAULT_ACCENT_COLOR
            if (current != sanitized) {
                prefs[ACCENT_COLOR_KEY] = sanitized
                prefs.markSettingsDirty(now)
            }
        }
    }

    suspend fun setBalanceBaseline(amount: Double, dateMillis: Long) {
        val sanitizedAmount = amount.coerceAtLeast(0.0)
        val sanitizedDate = dateMillis.coerceAtLeast(0L)
        val now = System.currentTimeMillis()
        context.dataStore.edit { prefs ->
            prefs[BALANCE_BASELINE_AMOUNT_KEY] = sanitizedAmount
            prefs[BALANCE_BASELINE_DATE_KEY] = sanitizedDate
            prefs.markSettingsDirty(now)
        }
    }

    suspend fun clearBalanceBaseline() {
        val now = System.currentTimeMillis()
        context.dataStore.edit { prefs ->
            val hadBaseline = prefs.contains(BALANCE_BASELINE_AMOUNT_KEY) || prefs.contains(BALANCE_BASELINE_DATE_KEY)
            prefs.remove(BALANCE_BASELINE_AMOUNT_KEY)
            prefs.remove(BALANCE_BASELINE_DATE_KEY)
            if (hadBaseline) {
                prefs.markSettingsDirty(now)
            }
        }
    }

    suspend fun markAllDirtyForBootstrap(now: Long = System.currentTimeMillis()) {
        var updatedGoals: List<SavingsGoal> = emptyList()
        context.dataStore.edit { prefs ->
            prefs.markSettingsDirty(now)
            val sanitizedLanguage = LanguageConfig.sanitize(prefs[LANGUAGE_KEY] ?: LanguageConfig.deviceDefault())
            prefs[LANGUAGE_KEY] = sanitizedLanguage
            val currentGoals = decodeGoals(prefs[SAVINGS_GOALS_KEY])
            val refreshed = currentGoals.map { goal ->
                val pending = when (goal.pendingOp) {
                    OP_INSERT -> OP_INSERT
                    OP_DELETE -> OP_DELETE
                    else -> if (goal.deleted) OP_DELETE else OP_UPDATE
                }
                goal.copy(
                    pendingOp = pending,
                    updatedAt = now
                )
            }
            prefs.persistSavingsGoals(refreshed)
            updatedGoals = refreshed
        }
        persistSavingsGoals(updatedGoals)
    }

    suspend fun addSavingsGoal(name: String, target: Double, deadlineMillis: Long? = null): SavingsGoal {
        val trimmedName = name.trim().ifEmpty { context.getString(R.string.default_savings_goal_name) }
        val sanitizedTarget = target.coerceAtLeast(0.0)
        val now = System.currentTimeMillis()
        val goal = SavingsGoal(
            id = UUID.randomUUID().toString(),
            name = trimmedName,
            target = sanitizedTarget,
            deadlineMillis = deadlineMillis,
            updatedAt = now,
            pendingOp = OP_INSERT
        )
        var updatedState: List<SavingsGoal> = emptyList()
        context.dataStore.edit { prefs ->
            val current = decodeGoals(prefs[SAVINGS_GOALS_KEY])
                .removeStaleDeletes()
                .filterNot { it.id == LEGACY_GOAL_ID }
            val updated = (current + goal).sortedByName()
            prefs.persistSavingsGoals(updated)
            prefs.remove(LEGACY_SAVINGS_GOAL_KEY)
            prefs.remove(LEGACY_SAVINGS_PROGRESS_KEY)
            updatedState = updated
        }
        persistSavingsGoals(updatedState)
        return goal
    }

    suspend fun updateSavingsGoal(goalId: String, name: String, target: Double, deadlineMillis: Long? = null) {
        val trimmedName = name.trim().ifEmpty { context.getString(R.string.default_savings_goal_name) }
        val sanitizedTarget = target.coerceAtLeast(0.0)
        val now = System.currentTimeMillis()
        var updatedState: List<SavingsGoal> = emptyList()
        context.dataStore.edit { prefs ->
            val current = decodeGoals(prefs[SAVINGS_GOALS_KEY]).removeStaleDeletes()
            val updated = current.mapNotNull { goal ->
                if (goal.id == goalId) {
                    val pending = when (goal.pendingOp) {
                        OP_INSERT -> OP_INSERT
                        OP_DELETE -> OP_UPDATE
                        else -> OP_UPDATE
                    }
                    goal.copy(
                        name = trimmedName,
                        target = sanitizedTarget,
                        deadlineMillis = deadlineMillis,
                        updatedAt = now,
                        deleted = false,
                        pendingOp = pending
                    )
                } else {
                    goal
                }
            }.sortedByName()
            prefs.persistSavingsGoals(updated)
            prefs.remove(LEGACY_SAVINGS_GOAL_KEY)
            prefs.remove(LEGACY_SAVINGS_PROGRESS_KEY)
            updatedState = updated
        }
        persistSavingsGoals(updatedState)
    }

    suspend fun deleteSavingsGoal(goalId: String) {
        val now = System.currentTimeMillis()
        var updatedState: List<SavingsGoal> = emptyList()
        context.dataStore.edit { prefs ->
            val current = decodeGoals(prefs[SAVINGS_GOALS_KEY])
            val updated = current.mapNotNull { goal ->
                if (goal.id != goalId) return@mapNotNull goal
                when (goal.pendingOp) {
                    OP_INSERT -> null
                    else -> goal.copy(
                        deleted = true,
                        pendingOp = OP_DELETE,
                        updatedAt = now
                    )
                }
            }
            prefs.persistSavingsGoals(updated)
            updatedState = updated
        }
        persistSavingsGoals(updatedState)
    }

    suspend fun pendingSavingsGoals(): List<SavingsGoal> =
        rawSavingsGoals(includeLegacy = false).filter { it.pendingOp != null }

    suspend fun rawSavingsGoals(includeLegacy: Boolean = true): List<SavingsGoal> {
        val prefs = context.dataStore.data.first()
        val stored = decodeGoals(prefs[SAVINGS_GOALS_KEY]).removeStaleDeletes()
        return if (includeLegacy) stored else stored.filterNot { it.id == LEGACY_GOAL_ID }
    }

    suspend fun mergeRemoteSavingsGoals(remote: List<SavingsGoal>): Int {
        if (remote.isEmpty()) return 0
        var applied = 0
        var mergedState: List<SavingsGoal> = emptyList()
        context.dataStore.edit { prefs ->
            val local = decodeGoals(prefs[SAVINGS_GOALS_KEY]).removeStaleDeletes()
            val (merged, changes) = mergeSavingsGoals(local, remote)
            applied = changes
            prefs.persistSavingsGoals(merged)
            mergedState = merged
        }
        persistSavingsGoals(mergedState)
        return applied
    }

    suspend fun markSavingsGoalsSynced(goals: List<SavingsGoal>) {
        if (goals.isEmpty()) return
        val updates = goals.associateBy { it.id }
        var updatedState: List<SavingsGoal> = emptyList()
        context.dataStore.edit { prefs ->
            val current = decodeGoals(prefs[SAVINGS_GOALS_KEY])
            val updated = current.mapNotNull { goal ->
                val ack = updates[goal.id] ?: return@mapNotNull goal
                val sanitized = goal.copy(
                    name = ack.name,
                    target = ack.target,
                    updatedAt = ack.updatedAt,
                    pendingOp = null,
                    deleted = ack.deleted
                )
                if (sanitized.deleted) null else sanitized
            }
            val extras = updates.values.filter { extra ->
                current.none { it.id == extra.id } && !extra.deleted
            }
            val normalized = (updated + extras).sortedByName()
            prefs.persistSavingsGoals(normalized)
            updatedState = normalized
        }
        persistSavingsGoals(updatedState)
    }

    private suspend fun persistBudget(amount: Double?, updatedAt: Long) {
        val dao = settingsDao ?: return
        withContext(Dispatchers.IO) {
            dao.upsertBudget(BudgetEntity(amount = amount, updatedAt = updatedAt))
        }
    }

    private suspend fun persistExpenseCategories(categories: List<String>, updatedAt: Long) {
        val dao = settingsDao ?: return
        val entities = categories.mapIndexed { index, name ->
            ExpenseCategoryEntity(
                name = name,
                position = index,
                updatedAt = updatedAt,
                deleted = false
            )
        }
        withContext(Dispatchers.IO) {
            if (entities.isEmpty()) {
                dao.clearExpenseCategories()
            } else {
                dao.replaceExpenseCategories(entities)
            }
        }
    }

    private suspend fun persistSavingsGoals(goals: List<SavingsGoal>) {
        val dao = settingsDao ?: return
        val entities = goals.map { it.toEntity() }
        withContext(Dispatchers.IO) {
            if (entities.isEmpty()) {
                dao.clearSavingsGoals()
            } else {
                dao.replaceSavingsGoals(entities)
            }
        }
    }

    private fun SavingsGoal.toEntity(): SavingsGoalEntity =
        SavingsGoalEntity(
            id = id,
            name = name,
            target = target,
            deadlineMillis = deadlineMillis,
            updatedAt = updatedAt,
            deleted = deleted,
            pendingOp = pendingOp
        )

    private fun mergeSavingsGoals(
        local: List<SavingsGoal>,
        remote: List<SavingsGoal>
    ): Pair<List<SavingsGoal>, Int> {
        if (remote.isEmpty()) return local.removeStaleDeletes().sortedByName() to 0
        val localMap = local.associateBy { it.id }.toMutableMap()
        val result = mutableListOf<SavingsGoal>()
        var applied = 0
        remote.forEach { remoteGoal ->
            val localGoal = localMap.remove(remoteGoal.id)
            val sanitizedRemote = remoteGoal.copy(pendingOp = null)
            when {
                localGoal == null -> {
                    if (!remoteGoal.deleted) {
                        result += sanitizedRemote
                        applied++
                    }
                }
                localGoal.pendingOp != null -> {
                    if (remoteGoal.updatedAt > localGoal.updatedAt) {
                        if (!remoteGoal.deleted) {
                            result += sanitizedRemote
                            applied++
                        } else if (!localGoal.deleted) {
                            applied++
                        }
                    } else {
                        result += localGoal
                    }
                }
                remoteGoal.deleted -> {
                    if (!localGoal.deleted) {
                        applied++
                    }
                }
                remoteGoal.updatedAt > localGoal.updatedAt ||
                    remoteGoal.name != localGoal.name ||
                    remoteGoal.target != localGoal.target ||
                    remoteGoal.deadlineMillis != localGoal.deadlineMillis -> {
                    result += sanitizedRemote
                    applied++
                }
                else -> {
                    result += localGoal
                }
            }
        }
        localMap.values.forEach { remaining ->
            if (!remaining.deleted || remaining.pendingOp != null) {
                result += remaining
            }
        }
        val merged = result.removeStaleDeletes().sortedByName()
        return merged to applied
    }

    private fun MutablePreferences.persistSavingsGoals(goals: List<SavingsGoal>) {
        val normalized = goals.removeStaleDeletes()
        if (normalized.isEmpty()) {
            remove(SAVINGS_GOALS_KEY)
        } else {
            this[SAVINGS_GOALS_KEY] = encodeGoals(normalized.sortedByName())
        }
    }
}
