package com.jan.moneybear.data.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface SettingsDao {

    @Query("SELECT * FROM budgets LIMIT 1")
    fun observeBudget(): Flow<BudgetEntity?>

    @Upsert
    suspend fun upsertBudget(budget: BudgetEntity)

    @Query("DELETE FROM budgets")
    suspend fun clearBudget()

    @Query("SELECT * FROM expense_categories WHERE deleted = 0 ORDER BY position ASC")
    fun observeExpenseCategories(): Flow<List<ExpenseCategoryEntity>>

    @Upsert
    suspend fun upsertExpenseCategories(categories: List<ExpenseCategoryEntity>)

    @Query("DELETE FROM expense_categories WHERE name NOT IN (:names)")
    suspend fun deleteMissingExpenseCategories(names: List<String>)

    @Query("DELETE FROM expense_categories")
    suspend fun clearExpenseCategories()

    @Transaction
    suspend fun replaceExpenseCategories(categories: List<ExpenseCategoryEntity>) {
        if (categories.isEmpty()) {
            clearExpenseCategories()
        } else {
            deleteMissingExpenseCategories(categories.map { it.name })
            upsertExpenseCategories(categories)
        }
    }

    @Query("SELECT * FROM savings_goals")
    fun observeSavingsGoals(): Flow<List<SavingsGoalEntity>>

    @Upsert
    suspend fun upsertSavingsGoals(goals: List<SavingsGoalEntity>)

    @Query("DELETE FROM savings_goals WHERE id NOT IN (:ids)")
    suspend fun deleteMissingSavingsGoals(ids: List<String>)

    @Query("DELETE FROM savings_goals WHERE id = :id")
    suspend fun deleteSavingsGoal(id: String)

    @Transaction
    suspend fun replaceSavingsGoals(goals: List<SavingsGoalEntity>) {
        if (goals.isEmpty()) {
            clearSavingsGoals()
        } else {
            deleteMissingSavingsGoals(goals.map { it.id })
            upsertSavingsGoals(goals)
        }
    }

    @Query("DELETE FROM savings_goals")
    suspend fun clearSavingsGoals()
}
