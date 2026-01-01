package com.jan.moneybear.domain

import android.content.Context
import com.jan.moneybear.R

fun defaultExpenseCategories(context: Context): List<String> =
    context.resources.getStringArray(R.array.default_expense_categories).toList()

fun defaultIncomeCategories(context: Context): List<String> =
    context.resources.getStringArray(R.array.default_income_categories).toList()
