package com.jan.moneybear.domain

import java.time.Instant
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

fun monthKey(millis: Long = System.currentTimeMillis()): String {
    val formatter = monthFormatter
    val yearMonth = YearMonth.from(
        Instant.ofEpochMilli(millis)
            .atZone(ZoneId.systemDefault())
            .toLocalDate()
    )
    return formatter.format(yearMonth)
}

fun addMonths(monthKey: String, offset: Int): String {
    val formatter = monthFormatter
    val base = YearMonth.parse(monthKey, formatter)
    return formatter.format(base.plusMonths(offset.toLong()))
}

fun monthSequence(startMonthKey: String, count: Int, step: Int = 1): List<String> {
    require(count >= 0) { "count must be >= 0" }
    return List(count) { index -> addMonths(startMonthKey, index * step) }
}

fun monthWindow(centerMonthKey: String, past: Int, future: Int): List<String> {
    require(past >= 0 && future >= 0)
    val formatter = monthFormatter
    val center = YearMonth.parse(centerMonthKey, formatter)
    return List(past + future + 1) { index ->
        val offset = index - past
        formatter.format(center.plusMonths(offset.toLong()))
    }
}

fun formatMonthLabel(monthKey: String): String {
    val yearMonth = YearMonth.parse(monthKey, monthFormatter)
    return yearMonth.month.getDisplayName(TextStyle.SHORT, Locale.getDefault())
        .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
}

private val monthFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("yyyy-MM", Locale.US)
