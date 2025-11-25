package com.lhzkmltts.provider

import android.content.Context
import kotlinx.coroutines.flow.Flow
import com.lhzkmltts.model.AudioChunk
import com.lhzkmltts.model.TTSRequest

interface TTSProvider<T : TTSProviderSetting> {
    fun generateSpeech(
        context: Context,
        providerSetting: T,
        request: TTSRequest
    ): Flow<AudioChunk>
}
