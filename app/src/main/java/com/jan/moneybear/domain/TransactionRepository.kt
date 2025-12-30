package com.jan.moneybear.domain

import com.jan.moneybear.data.local.Transaction
import com.jan.moneybear.data.local.TransactionDao
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

data class CategorySeriesEntry(
    val month: String,
    val category: String,
    val amount: Double
)

class TransactionRepository(
    private val dao: TransactionDao
) {

    fun listMonth(month: String): Flow<List<Transaction>> = dao.listMonth(month)

    fun observeTransaction(id: String): Flow<Transaction?> = dao.observeById(id)

    suspend fun getTransaction(id: String): Transaction? = dao.getById(id)

    fun sumMonth(month: String): Flow<Double> = dao.sumMonth(month)

    fun sumMonthExpenses(month: String): Flow<Double> = dao.sumMonthExpenses(month)

    fun sumMonthIncomes(month: String): Flow<Double> = dao.sumMonthIncomes(month)

    fun listRecent(limit: Int = 5): Flow<List<Transaction>> =
        dao.listRecent(limit).map { rows ->
            rows.filterNot { it.amount == 0.0 && it.savingsGoalId != null }
        }

    fun listFuture(fromMillis: Long = System.currentTimeMillis()): Flow<List<Transaction>> =
        dao.listFuture(fromMillis)

    fun transactionsBetween(startMillis: Long, endMillis: Long): Flow<List<Transaction>> =
        dao.transactionsBetween(startMillis, endMillis)

    fun listOlderThan(beforeMonthExclusive: String, limit: Int = 100): Flow<List<Transaction>> =
        dao.listOlderThan(beforeMonthExclusive, limit)

    fun expenseCategorySeries(months: List<String>): Flow<List<CategorySeriesEntry>> =
        dao.expenseCategoriesForMonths(months).map { rows ->
            rows.map { row ->
                CategorySeriesEntry(
                    month = row.monthKey,
                    category = row.category,
                    amount = row.total
                )
            }
        }

    fun incomeCategorySeries(months: List<String>): Flow<List<CategorySeriesEntry>> =
        dao.incomeCategoriesForMonths(months).map { rows ->
            rows.map { row ->
                CategorySeriesEntry(
                    month = row.monthKey,
                    category = row.category,
                    amount = row.total
                )
            }
        }

    fun sumExpensesBetween(startMillis: Long, endMillis: Long): Flow<Double> =
        dao.sumExpensesBetween(startMillis, endMillis)

    fun sumIncomesBetween(startMillis: Long, endMillis: Long): Flow<Double> =
        dao.sumIncomesBetween(startMillis, endMillis)

    fun savingsBalances(): Flow<Map<String, Double>> =
        dao.savingsBalances().map { rows -> rows.associate { it.goalId to it.balance } }

    suspend fun addOrUpdate(tx: Transaction) {
        val normalizedGoalId = tx.savingsGoalId?.takeIf { it.isNotBlank() }
        val normalizedImpact = if (normalizedGoalId == null) 0.0 else tx.savingsImpact
        val operation = when (tx.pendingOp) {
            "DELETE" -> "DELETE"
            "INSERT" -> "INSERT"
            else -> if (tx.updatedAtServer == null) "INSERT" else "UPDATE"
        }
        val updated = tx.copy(
            dirty = true,
            pendingOp = operation,
            updatedAtLocal = System.currentTimeMillis(),
            savingsGoalId = normalizedGoalId,
            savingsImpact = normalizedImpact
        )
        dao.upsert(updated)
    }

    suspend fun delete(id: String) {
        dao.markDeleted(id)
    }

    suspend fun markSynced(ids: List<String>, updatedAt: Long) {
        if (ids.isEmpty()) return
        dao.markSynced(ids, updatedAt)
    }
}
