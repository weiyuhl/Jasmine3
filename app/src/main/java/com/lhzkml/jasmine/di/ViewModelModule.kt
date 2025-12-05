package com.lhzkml.jasmine.di

import com.lhzkml.jasmine.ui.pages.assistant.AssistantVM
import com.lhzkml.jasmine.ui.pages.assistant.detail.AssistantDetailVM
import com.lhzkml.jasmine.ui.pages.backup.BackupVM
import com.lhzkml.jasmine.ui.pages.chat.ChatVM
import com.lhzkml.jasmine.ui.pages.developer.DeveloperVM
import com.lhzkml.jasmine.ui.pages.history.HistoryVM
import com.lhzkml.jasmine.ui.pages.setting.SettingVM
import org.koin.core.module.dsl.viewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val viewModelModule = module {
    viewModel<ChatVM> { params ->
        ChatVM(
            id = params.get(),
            context = get(),
            settingsStore = get(),
            conversationRepo = get(),
            chatService = get(),
            updateChecker = get(),
            analytics = get()
        )
    }
    viewModelOf(::SettingVM)
    viewModelOf(::HistoryVM)
    viewModelOf(::AssistantVM)
    viewModel<AssistantDetailVM> {
        AssistantDetailVM(
            id = it.get(),
            settingsStore = get(),
            memoryRepository = get(),
            context = get(),
        )
    }
    
    viewModelOf(::BackupVM)
    viewModelOf(::DeveloperVM)
}
