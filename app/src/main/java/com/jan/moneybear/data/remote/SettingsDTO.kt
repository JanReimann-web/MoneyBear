package com.jan.moneybear.data.remote

data class SettingsDTO(
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
    val updatedAt: Long
)
