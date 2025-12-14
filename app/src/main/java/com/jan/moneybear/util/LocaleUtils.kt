package com.jan.moneybear.util

import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import com.jan.moneybear.domain.LanguageConfig

object LocaleUtils {
    private var appliedLanguage: String? = null

    fun applyAppLanguage(code: String) {
        val sanitized = LanguageConfig.sanitize(code)
        if (appliedLanguage == sanitized) return
        appliedLanguage = sanitized
        val locales = LocaleListCompat.forLanguageTags(sanitized)
        AppCompatDelegate.setApplicationLocales(locales)
    }
}
