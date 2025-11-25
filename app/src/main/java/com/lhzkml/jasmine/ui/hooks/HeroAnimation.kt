package com.lhzkml.jasmine.ui.hooks

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.lhzkml.jasmine.ui.context.LocalAnimatedVisibilityScope
import com.lhzkml.jasmine.ui.context.LocalSharedTransitionScope

@Composable
fun Modifier.heroAnimation(
    key: Any,
): Modifier {
    val sharedTransitionScope = LocalSharedTransitionScope.current
    val animatedVisibilityScope = LocalAnimatedVisibilityScope.current
    return with(sharedTransitionScope) {
        this@heroAnimation.sharedElement(
            sharedContentState = rememberSharedContentState(key),
            animatedVisibilityScope = animatedVisibilityScope
        )
    }
}
