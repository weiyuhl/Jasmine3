package com.lhzkml.jasmine.ui.modifier

import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.composed
import androidx.compose.ui.platform.LocalFocusManager

@Composable
fun Modifier.onClick(
    enabled: Boolean = true,
    onClick: () -> Unit
): Modifier = this.then(Modifier.clickable(
    onClick = onClick,
    interactionSource = remember { MutableInteractionSource() },
    indication = LocalIndication.current,
    role = Role.Button,
))

@Composable
fun Modifier.clearFocusOnTap(): Modifier = composed {
    val focusManager = LocalFocusManager.current
    val interactionSource = remember { MutableInteractionSource() }
    clickable(
        interactionSource = interactionSource,
        indication = null,
        onClick = { focusManager.clearFocus() }
    )
}
