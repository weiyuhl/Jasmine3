package com.lhzkml.jasmine.ui.context

import androidx.compose.runtime.compositionLocalOf
import com.lhzkml.jasmine.ui.hooks.CustomTtsState

val LocalTTSState = compositionLocalOf<CustomTtsState> { error("Not provided yet") }
