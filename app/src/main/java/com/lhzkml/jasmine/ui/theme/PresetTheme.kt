package com.lhzkml.jasmine.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Composable
import com.lhzkml.jasmine.ui.theme.presets.ModernThemePreset

data class PresetTheme(
    val id: String,
    val name: @Composable () -> Unit,
    val standardLight: ColorScheme,
    val standardDark: ColorScheme,
) {
    fun getColorScheme(dark: Boolean): ColorScheme {
        return if (dark) standardDark else standardLight
    }
}

val PresetThemes by lazy {
    listOf(
        ModernThemePreset,
    )
}

fun findPresetTheme(id: String): PresetTheme {
    return PresetThemes.find { it.id == id } ?: ModernThemePreset
}
