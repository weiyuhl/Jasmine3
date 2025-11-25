package com.lhzkmlai.provider.providers.openai

import kotlinx.coroutines.flow.Flow
import com.lhzkmlai.provider.ProviderSetting
import com.lhzkmlai.provider.TextGenerationParams
import com.lhzkmlai.ui.MessageChunk
import com.lhzkmlai.ui.UIMessage

interface OpenAIImpl {
    suspend fun generateText(
        providerSetting: ProviderSetting.OpenAI,
        messages: List<UIMessage>,
        params: TextGenerationParams,
    ): MessageChunk

    suspend fun streamText(
        providerSetting: ProviderSetting.OpenAI,
        messages: List<UIMessage>,
        params: TextGenerationParams,
    ): Flow<MessageChunk>
}
