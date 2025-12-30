package com.jan.moneybear.domain

import java.util.Locale

object LanguageConfig {
    val supported: Set<String> = setOf("en", "et", "fi", "sv", "ru", "lv", "lt", "no", "da", "pl", "de")
    const val FALLBACK: String = "en"

    fun sanitize(raw: String?): String {
        if (raw.isNullOrBlank()) return FALLBACK
        val normalized = raw.trim().lowercase(Locale.ROOT)
        if (supported.contains(normalized)) return normalized
        val base = normalized.substring(0, normalized.indexOf('-').takeIf { it > 0 } ?: normalized.length)
        val candidate = base.takeIf { it.isNotBlank() } ?: normalized
        return if (supported.contains(candidate)) candidate else FALLBACK
    }

    fun deviceDefault(): String = sanitize(Locale.getDefault().language)
}
