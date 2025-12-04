package com.lhzkml.jasmine.di

import com.lhzkml.jasmine.data.repository.ConversationRepository
import com.lhzkml.jasmine.data.repository.MemoryRepository
import org.koin.dsl.module

val repositoryModule = module {
    single {
        ConversationRepository(get(), get())
    }

    single {
        MemoryRepository(get())
    }

    // Image generation repository removed
}
