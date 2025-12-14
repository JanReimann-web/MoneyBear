package com.jan.moneybear.data.remote

import com.jan.moneybear.data.local.TxType

data class TransactionDTO(
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
    val type: TxType = TxType.EXPENSE,
    val updatedAtServer: Long? = null,
    val savingsGoalId: String? = null,
    val savingsImpact: Double = 0.0
)

