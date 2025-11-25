package com.lhzkmltts.provider

import android.content.Context
import kotlinx.coroutines.flow.Flow
import com.lhzkmltts.model.AudioChunk
import com.lhzkmltts.model.TTSRequest
import com.lhzkmltts.provider.providers.GeminiTTSProvider
import com.lhzkmltts.provider.providers.MiniMaxTTSProvider
import com.lhzkmltts.provider.providers.OpenAITTSProvider
import com.lhzkmltts.provider.providers.SystemTTSProvider

class TTSManager(private val context: Context) {
    private val openAIProvider = OpenAITTSProvider()
    private val geminiProvider = GeminiTTSProvider()
    private val systemProvider = SystemTTSProvider()
    private val miniMaxProvider = MiniMaxTTSProvider()

    fun generateSpeech(
        providerSetting: TTSProviderSetting,
        request: TTSRequest
    ): Flow<AudioChunk> {
        return when (providerSetting) {
            is TTSProviderSetting.OpenAI -> openAIProvider.generateSpeech(context, providerSetting, request)
            is TTSProviderSetting.Gemini -> geminiProvider.generateSpeech(context, providerSetting, request)
            is TTSProviderSetting.SystemTTS -> systemProvider.generateSpeech(context, providerSetting, request)
            is TTSProviderSetting.MiniMax -> miniMaxProvider.generateSpeech(context, providerSetting, request)
        }
    }
}
