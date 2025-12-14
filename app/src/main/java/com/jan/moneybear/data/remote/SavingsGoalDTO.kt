package com.jan.moneybear.data.remote

import com.jan.moneybear.data.store.SavingsGoal

data class SavingsGoalDTO(
    val id: String,
    val name: String,
    val target: Double,
    val updatedAt: Long,
    val deleted: Boolean
)

fun SavingsGoalDTO.toDomain(): SavingsGoal =
    SavingsGoal(
        id = id,
        name = name,
        target = target,
        updatedAt = updatedAt,
        deleted = deleted,
        pendingOp = null
    )

fun SavingsGoal.toDto(now: Long = System.currentTimeMillis()): SavingsGoalDTO {
    val effectiveUpdatedAt = updatedAt.takeIf { it > 0L } ?: now
    return SavingsGoalDTO(
        id = id,
        name = name,
        target = target,
        updatedAt = effectiveUpdatedAt,
        deleted = deleted
    )
}

