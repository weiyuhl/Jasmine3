package com.lhzkml.jasmine.ui.pages.setting

import android.os.Build
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lhzkml.jasmine.R
import com.lhzkml.jasmine.data.datastore.DisplaySetting
import com.lhzkml.jasmine.ui.components.nav.BackButton
import com.lhzkml.jasmine.ui.components.ui.permission.PermissionManager
import com.lhzkml.jasmine.ui.components.ui.permission.PermissionNotification
import com.lhzkml.jasmine.ui.components.ui.permission.rememberPermissionState
 
import com.lhzkml.jasmine.ui.hooks.rememberSharedPreferenceBoolean
import com.lhzkml.jasmine.ui.pages.setting.components.PresetThemeButtonGroup
import com.lhzkml.jasmine.utils.plus
import org.koin.androidx.compose.koinViewModel

@Composable
fun SettingDisplayPage(vm: SettingVM = koinViewModel()) {
    val settings by vm.settings.collectAsStateWithLifecycle()
    var displaySetting by remember(settings) { mutableStateOf(settings.displaySetting) }
    

    fun updateDisplaySetting(setting: DisplaySetting) {
        displaySetting = setting
        vm.updateSettings(
            settings.copy(
                displaySetting = setting
            )
        )
    }

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    val permissionState = rememberPermissionState(
        permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) setOf(
            PermissionNotification
        ) else emptySet(),
    )
    PermissionManager(permissionState = permissionState)

    Scaffold(
        topBar = {
            LargeTopAppBar(
                title = {
                    Text(stringResource(R.string.setting_display_page_title))
                },
                navigationIcon = {
                    BackButton()
                },
                scrollBehavior = scrollBehavior
            )
        }
    ) { contentPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .nestedScroll(scrollBehavior.nestedScrollConnection)
                .consumeWindowInsets(contentPadding),
            contentPadding = contentPadding + PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            stickyHeader {
                Text(
                    text = stringResource(R.string.setting_page_theme_setting),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                )
            }

            item {
                ListItem(
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    headlineContent = {
                        Text(stringResource(R.string.setting_page_dynamic_color))
                    },
                    supportingContent = {
                        Text(stringResource(R.string.setting_page_dynamic_color_desc))
                    },
                    trailingContent = {
                        Switch(
                            checked = settings.dynamicColor,
                            onCheckedChange = {
                                vm.updateSettings(settings.copy(dynamicColor = it))
                            },
                        )
                    },
                )
            }

            if (!settings.dynamicColor) {
                item {
                    PresetThemeButtonGroup(
                        themeId = settings.themeId,
                        modifier = Modifier.fillMaxWidth(),
                        onChangeTheme = {
                            vm.updateSettings(settings.copy(themeId = it))
                        }
                    )
                }
            }

            

            stickyHeader {
                Text(
                    text = stringResource(R.string.setting_page_basic_settings),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                )
            }

            item {
                var createNewConversationOnStart by rememberSharedPreferenceBoolean(
                    "create_new_conversation_on_start",
                    true
                )
                ListItem(
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    headlineContent = {
                        Text(stringResource(R.string.setting_display_page_create_new_conversation_on_start_title))
                    },
                    supportingContent = {
                        Text(stringResource(R.string.setting_display_page_create_new_conversation_on_start_desc))
                    },
                    trailingContent = {
                        Switch(
                            checked = createNewConversationOnStart,
                            onCheckedChange = {
                                createNewConversationOnStart = it
                            }
                        )
                    },
                )
            }

            item {
                ListItem(
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    headlineContent = {
                        Text(stringResource(R.string.setting_display_page_show_updates_title))
                    },
                    supportingContent = {
                        Text(stringResource(R.string.setting_display_page_show_updates_desc))
                    },
                    trailingContent = {
                        Switch(
                            checked = displaySetting.showUpdates,
                            onCheckedChange = {
                                updateDisplaySetting(displaySetting.copy(showUpdates = it))
                            }
                        )
                    },
                )
            }

            item {
                ListItem(
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    headlineContent = {
                        Text(stringResource(R.string.setting_display_page_notification_message_generated))
                    },
                    supportingContent = {
                        Text(stringResource(R.string.setting_display_page_notification_message_generated_desc))
                    },
                    trailingContent = {
                        Switch(
                            checked = displaySetting.enableNotificationOnMessageGeneration,
                            onCheckedChange = {
                                if (it && !permissionState.allPermissionsGranted) {
                                    // 请求权限
                                    permissionState.requestPermissions()
                                }
                                updateDisplaySetting(displaySetting.copy(enableNotificationOnMessageGeneration = it))
                            }
                        )
                    },
                )
            }

//            item {
//                ListItem(
//                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
//                    headlineContent = {
//                        Text(stringResource(R.string.setting_display_page_developer_mode))
//                    },
//                    supportingContent = {
//                        Text(stringResource(R.string.setting_display_page_developer_mode_desc))
//                    },
//                    trailingContent = {
//                        Switch(
//                            checked = settings.developerMode,
//                            onCheckedChange = {
//                                vm.updateSettings(settings.copy(developerMode = it))
//                            }
//                        )
//                    },
//                )
//            }

        }
    }
}
