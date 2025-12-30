package com.jan.moneybear.data.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

data class CategoryMonthlySum(
    val monthKey: String,
    val category: String,
    val total: Double
)

data class SavingsBalanceRow(
    val goalId: String,
    val balance: Double
)

@Dao
interface TransactionDao {

    @Upsert
    suspend fun upsert(vararg tx: Transaction)

    @Query("UPDATE transactions SET deleted = 1, dirty = 1, pendingOp = 'DELETE', updatedAtLocal = :now WHERE id = :id")
    suspend fun markDeleted(id: String, now: Long = System.currentTimeMillis())

    @Query("SELECT * FROM transactions WHERE monthKey = :month AND deleted = 0 ORDER BY dateUtcMillis DESC")
    fun listMonth(month: String): Flow<List<Transaction>>

    @Query("SELECT * FROM transactions WHERE id = :id LIMIT 1")
    fun observeById(id: String): Flow<Transaction?>

    @Query("SELECT * FROM transactions WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): Transaction?

    @Query("SELECT * FROM transactions WHERE deleted = 0 ORDER BY updatedAtLocal DESC, dateUtcMillis DESC LIMIT :limit")
    fun listRecent(limit: Int = 5): Flow<List<Transaction>>

    @Query("SELECT * FROM transactions WHERE deleted = 0 AND dateUtcMillis > :fromMillis ORDER BY dateUtcMillis ASC")
    fun listFuture(fromMillis: Long): Flow<List<Transaction>>

    @Query(
        """
        SELECT * FROM transactions 
        WHERE deleted = 0 AND dateUtcMillis >= :startMillis AND dateUtcMillis <= :endMillis 
        ORDER BY dateUtcMillis ASC
        """
    )
    fun transactionsBetween(startMillis: Long, endMillis: Long): Flow<List<Transaction>>

    @Query("SELECT * FROM transactions WHERE deleted = 0 AND monthKey < :exclusiveUpper ORDER BY dateUtcMillis DESC LIMIT :limit")
    fun listOlderThan(exclusiveUpper: String, limit: Int = 100): Flow<List<Transaction>>

    @Query(
        """
        SELECT savingsGoalId AS goalId, COALESCE(SUM(savingsImpact), 0.0) AS balance
        FROM transactions
        WHERE deleted = 0 AND savingsGoalId IS NOT NULL
        GROUP BY savingsGoalId
        """
    )
    fun savingsBalances(): Flow<List<SavingsBalanceRow>>

    @Query("SELECT COALESCE(SUM(amount), 0.0) FROM transactions WHERE monthKey = :month AND deleted = 0")
    fun sumMonth(month: String): Flow<Double>

    @Query("SELECT COALESCE(SUM(amount), 0.0) FROM transactions WHERE monthKey = :month AND deleted = 0 AND type = 'EXPENSE'")
    fun sumMonthExpenses(month: String): Flow<Double>

    @Query("SELECT COALESCE(SUM(amount), 0.0) FROM transactions WHERE monthKey = :month AND deleted = 0 AND type = 'INCOME'")
    fun sumMonthIncomes(month: String): Flow<Double>

    @Query(
        """
        SELECT 
            monthKey AS monthKey,
            category,
            SUM(ABS(amount)) AS total
        FROM transactions
        WHERE monthKey IN (:months) AND deleted = 0 AND type = 'EXPENSE'
        GROUP BY monthKey, category
        """
    )
    fun expenseCategoriesForMonths(months: List<String>): Flow<List<CategoryMonthlySum>>

    @Query(
        """
        SELECT 
            monthKey AS monthKey,
            category,
            SUM(ABS(amount)) AS total
        FROM transactions
        WHERE monthKey IN (:months) AND deleted = 0 AND type = 'INCOME'
        GROUP BY monthKey, category
        """
    )
    fun incomeCategoriesForMonths(months: List<String>): Flow<List<CategoryMonthlySum>>

    @Query(
        """
        SELECT COALESCE(SUM(amount), 0.0) FROM transactions
        WHERE deleted = 0 AND dateUtcMillis >= :startMillis AND dateUtcMillis < :endMillis AND type = 'EXPENSE'
        """
    )
    fun sumExpensesBetween(startMillis: Long, endMillis: Long): Flow<Double>

    @Query(
        """
        SELECT COALESCE(SUM(amount), 0.0) FROM transactions
        WHERE deleted = 0 AND dateUtcMillis >= :startMillis AND dateUtcMillis < :endMillis AND type = 'INCOME'
        """
    )
    fun sumIncomesBetween(startMillis: Long, endMillis: Long): Flow<Double>

    @Query("SELECT * FROM transactions WHERE dirty = 1 LIMIT :limit")
    suspend fun dirty(limit: Int = 500): List<Transaction>

    @Query(
        """
        UPDATE transactions
        SET 
            dirty = 1,
            pendingOp = CASE
                WHEN pendingOp = 'DELETE' THEN 'DELETE'
                WHEN pendingOp = 'INSERT' THEN 'INSERT'
                WHEN deleted = 1 THEN 'DELETE'
                ELSE 'UPDATE'
            END,
            updatedAtLocal = :now
        """
    )
    suspend fun markAllDirty(now: Long)

    @Query("UPDATE transactions SET dirty = 0, pendingOp = NULL, updatedAtServer = :updatedAt WHERE id IN (:ids)")
    suspend fun markSynced(ids: List<String>, updatedAt: Long)

    @Query("DELETE FROM transactions WHERE deleted = 1 AND dateUtcMillis < :olderThan")
    suspend fun purgeDeleted(olderThan: Long)
}
