package com.lhzkml.jasmine.di

import com.google.firebase.Firebase
import com.google.firebase.analytics.analytics
import com.google.firebase.crashlytics.crashlytics
import com.google.firebase.remoteconfig.remoteConfig
import kotlinx.serialization.json.Json
import com.lhzkmlhighlight.Highlighter
import com.lhzkml.jasmine.AppScope
import com.lhzkml.jasmine.data.ai.AILoggingManager
import com.lhzkml.jasmine.data.ai.tools.LocalTools
import com.lhzkml.jasmine.service.ChatService
import com.lhzkml.jasmine.utils.JsonInstant
import com.lhzkml.jasmine.utils.UpdateChecker
import org.koin.dsl.module

val appModule = module {
    single<Json> { JsonInstant }

    single {
        Highlighter(get())
    }

    single {
        LocalTools(get())
    }

    single {
        UpdateChecker(get())
    }

    single {
        AppScope()
    }


    

    single {
        Firebase.crashlytics
    }

    single {
        Firebase.remoteConfig
    }

    single {
        Firebase.analytics
    }

    single {
        AILoggingManager()
    }

    single {
        ChatService(
            context = get(),
            appScope = get(),
            settingsStore = get(),
            conversationRepo = get(),
            memoryRepository = get(),
            generationHandler = get(),
            templateTransformer = get(),
            providerManager = get(),
            localTools = get(),
            mcpManager = get()
        )
    }
}
