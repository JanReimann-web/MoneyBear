package com.jan.moneybear.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class TxType {
    EXPENSE,
    INCOME
}

@Entity(tableName = "transactions")
data class Transaction(
    @PrimaryKey
    val id: String,
    val uid: String,
    val amount: Double,
    val currency: String,
    val dateUtcMillis: Long,
    val monthKey: String,
    val category: String,
    val note: String? = null,
    val planned: Boolean = false,
    val deleted: Boolean = false,
    val dirty: Boolean = true,
    val pendingOp: String? = "INSERT",
    val updatedAtLocal: Long = System.currentTimeMillis(),
    val updatedAtServer: Long? = null,
    val type: TxType = TxType.EXPENSE,
    val savingsGoalId: String? = null,
    val savingsImpact: Double = 0.0
)
