package com.jan.moneybear.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [
        Transaction::class,
        BudgetEntity::class,
        ExpenseCategoryEntity::class,
        SavingsGoalEntity::class
    ],
    version = 4,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDb : RoomDatabase() {
    abstract fun tx(): TransactionDao
    abstract fun settings(): SettingsDao
}
