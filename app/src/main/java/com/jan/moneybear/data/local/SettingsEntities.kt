package com.jan.moneybear.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "budgets")
data class BudgetEntity(
    @PrimaryKey val id: Int = 0,
    val amount: Double?,
    val updatedAt: Long
)

@Entity(tableName = "expense_categories")
data class ExpenseCategoryEntity(
    @PrimaryKey val name: String,
    val position: Int,
    val updatedAt: Long,
    val deleted: Boolean = false
)

@Entity(tableName = "savings_goals")
data class SavingsGoalEntity(
    @PrimaryKey val id: String,
    val name: String,
    val target: Double,
    val deadlineMillis: Long? = null, // Tähtaeg kuupäevana (millisekundites)
    val updatedAt: Long,
    val deleted: Boolean,
    val pendingOp: String?
)
