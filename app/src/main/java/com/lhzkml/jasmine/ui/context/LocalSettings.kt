package com.lhzkml.jasmine.ui.context

import androidx.compose.runtime.staticCompositionLocalOf
import com.lhzkml.jasmine.data.datastore.Settings

val LocalSettings = staticCompositionLocalOf<Settings> {
    error("No SettingsStore provided")
}
