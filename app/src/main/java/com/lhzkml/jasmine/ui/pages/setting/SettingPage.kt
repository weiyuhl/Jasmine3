package com.lhzkml.jasmine.ui.pages.setting

import android.content.ActivityNotFoundException
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.TheaterComedy
import androidx.compose.material.icons.rounded.TravelExplore
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Warning
 
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.Icons
import com.lhzkml.jasmine.R
import com.lhzkml.jasmine.Screen
import com.lhzkml.jasmine.data.datastore.isNotConfigured
import com.lhzkml.jasmine.ui.components.nav.BackButton
import com.lhzkml.jasmine.ui.components.ui.Select
import com.lhzkml.jasmine.ui.context.LocalNavController
import com.lhzkml.jasmine.ui.hooks.rememberColorMode
import com.lhzkml.jasmine.ui.theme.ColorMode
import com.lhzkml.jasmine.utils.openUrl
import com.lhzkml.jasmine.utils.plus
import org.koin.androidx.compose.koinViewModel

@Composable
fun SettingPage(vm: SettingVM = koinViewModel()) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val navController = LocalNavController.current
    val settings by vm.settings.collectAsStateWithLifecycle()
    Scaffold(
        topBar = {
            LargeTopAppBar(
                title = {
                    Text(text = stringResource(R.string.settings))
                },
                navigationIcon = {
                    BackButton()
                },
                scrollBehavior = scrollBehavior,
                actions = {
                    if(settings.developerMode) {
                        IconButton(
                            onClick = {
                                navController.navigate(Screen.Developer)
                            }
                        ) {
                            Icon(Icons.Filled.Build, "Developer")
                        }
                    }
                }
            )
        },
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection)
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = innerPadding + PaddingValues(8.dp),
        ) {
            if (settings.isNotConfigured()) {
                item {
                    ProviderConfigWarningCard(navController)
                }
            }

            stickyHeader {
                Text(
                    text = stringResource(R.string.setting_page_general_settings),
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }


            item {
                SettingItem(
                    navController = navController,
                    title = { Text(stringResource(R.string.setting_page_display_setting)) },
                    description = { Text(stringResource(R.string.setting_page_display_setting_desc)) },
                    icon = { Icon(Icons.Filled.Tune, "Display Setting") },
                    link = Screen.SettingDisplay
                )
            }

            item {
                SettingItem(
                    navController = navController,
                    title = { Text(stringResource(R.string.setting_page_assistant)) },
                    description = { Text(stringResource(R.string.setting_page_assistant_desc)) },
                    icon = { Icon(Icons.Filled.TheaterComedy, "Assistant") },
                    link = Screen.Assistant
                )
            }

            stickyHeader {
                Text(
                    text = stringResource(R.string.setting_page_model_and_services),
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            item {
                SettingItem(
                    navController = navController,
                    title = { Text(stringResource(R.string.setting_page_default_model)) },
                    description = { Text(stringResource(R.string.setting_page_default_model_desc)) },
                    icon = { Icon(Icons.Filled.Build, stringResource(R.string.setting_page_default_model)) },
                    link = Screen.SettingModels
                )
            }

            item {
                SettingItem(
                    navController = navController,
                    title = { Text(stringResource(R.string.setting_page_providers)) },
                    description = { Text(stringResource(R.string.setting_page_providers_desc)) },
                    icon = { Icon(Icons.Filled.Category, "Models") },
                    link = Screen.SettingProvider
                )
            }

            item {
                SettingItem(
                    navController = navController,
                    title = { Text(stringResource(R.string.setting_page_search_service)) },
                    description = { Text(stringResource(R.string.setting_page_search_service_desc)) },
                    icon = { Icon(Icons.Rounded.TravelExplore, "Search") },
                    link = Screen.SettingSearch
                )
            }

            

            item {
                SettingItem(
                    navController = navController,
                    title = { Text(stringResource(R.string.setting_page_mcp)) },
                    description = { Text(stringResource(R.string.setting_page_mcp_desc)) },
                    icon = { Icon(Icons.Filled.Storage, "MCP") },
                    link = Screen.SettingMcp
                )
            }

            stickyHeader {
                Text(
                    text = stringResource(R.string.setting_page_data_settings),
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            item {
                SettingItem(
                    navController = navController,
                    title = { Text(stringResource(R.string.setting_page_data_backup)) },
                    description = { Text(stringResource(R.string.setting_page_data_backup_desc)) },
                    icon = { Icon(Icons.Filled.Backup, "Backup") },
                    link = Screen.Backup
                )
            }

            

            stickyHeader {
                Text(
                    text = stringResource(R.string.setting_page_about),
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            item {
                SettingItem(
                    navController = navController,
                    title = { Text(stringResource(R.string.setting_page_about)) },
                    description = { Text(stringResource(R.string.setting_page_about_desc)) },
                    icon = { Icon(Icons.Filled.Info, "About") },
                    link = Screen.SettingAbout
                )
            }

            
        }
    }
}

@Composable
private fun ProviderConfigWarningCard(navController: NavHostController) {
    Card(
        modifier = Modifier.padding(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            horizontalAlignment = Alignment.End
        ) {
            ListItem(
                headlineContent = {
                    Text(stringResource(R.string.setting_page_config_api_title))
                },
                supportingContent = {
                    Text(stringResource(R.string.setting_page_config_api_desc))
                },
                leadingContent = {
                    Icon(Icons.Filled.Warning, null)
                },
                colors = ListItemDefaults.colors(
                    containerColor = Color.Transparent
                )
            )

            TextButton(
                onClick = {
                    navController.navigate(Screen.SettingProvider)
                }
            ) {
                Text(stringResource(R.string.setting_page_config))
            }
        }
    }
}

@Composable
fun SettingItem(
    navController: NavHostController,
    title: @Composable () -> Unit,
    description: @Composable () -> Unit,
    icon: @Composable () -> Unit,
    link: Screen? = null,
    onClick: () -> Unit = {}
) {
    Surface(
        onClick = {
            if (link != null) navController.navigate(link)
            onClick()
        }
    ) {
        ListItem(
            headlineContent = {
                title()
            },
            supportingContent = {
                description()
            },
            leadingContent = {
                icon()
            }
        )
    }
}
