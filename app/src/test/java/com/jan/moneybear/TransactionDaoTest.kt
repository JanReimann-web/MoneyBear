package com.jan.moneybear

import com.jan.moneybear.data.local.Transaction
import com.jan.moneybear.data.local.TxType
import com.jan.moneybear.domain.monthKey
import org.junit.Test
import org.junit.Assert.*

/**
 * Example unit test for Transaction DAO logic
 * 
 * Note: This is a simple logic test. For actual database tests,
 * use Android Instrumentation tests with an in-memory database.
 */
class TransactionDaoTest {
    
    @Test
    fun `monthKey format is correct`() {
        val calculated = monthKey(1704067200000L) // Jan 1, 2024
        assertEquals("2024-01", calculated)
    }
    
    @Test
    fun `transaction sum calculation logic`() {
        // Simulate what the DAO sum would do
        val transactions = listOf(
            createTestTransaction(amount = 100.0, deleted = false),
            createTestTransaction(amount = 50.0, deleted = false),
            createTestTransaction(amount = 25.0, deleted = true) // Should be excluded
        )
        
        val sum = transactions.filter { !it.deleted }.sumOf { it.amount }
        assertEquals(150.0, sum, 0.001)
    }
    
    @Test
    fun `empty transaction list returns zero sum`() {
        val transactions = emptyList<Transaction>()
        val sum = transactions.filter { !it.deleted }.sumOf { it.amount }
        assertEquals(0.0, sum, 0.001)
    }
    
    private fun createTestTransaction(
        amount: Double,
        deleted: Boolean = false,
        type: TxType = TxType.EXPENSE
    ): Transaction {
        return Transaction(
            id = "tx_test",
            uid = "user123",
            amount = amount,
            currency = "EUR",
            dateUtcMillis = System.currentTimeMillis(),
            monthKey = monthKey(),
            category = "Test",
            note = null,
            planned = false,
            deleted = deleted,
            dirty = false,
            pendingOp = null,
            updatedAtLocal = System.currentTimeMillis(),
            updatedAtServer = null,
            type = type
        )
    }
}



























