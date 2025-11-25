package com.lhzkml.jasmine.ui.components.nav

import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.composables.icons.lucide.ArrowLeft
import com.composables.icons.lucide.Lucide
import com.lhzkml.jasmine.R
import com.lhzkml.jasmine.ui.context.LocalNavController

@Composable
fun BackButton(modifier: Modifier = Modifier) {
    val navController = LocalNavController.current
    IconButton(
        onClick = {
            navController.popBackStack()
        },
        modifier = modifier
    ) {
        Icon(
            Lucide.ArrowLeft,
            contentDescription = stringResource(R.string.back)
        )
    }
}
