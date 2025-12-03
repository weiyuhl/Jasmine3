package com.lhzkml.jasmine.data.datastore.migration

import androidx.datastore.core.DataMigration
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.stringPreferencesKey
import com.lhzkml.jasmine.data.datastore.SettingsStore
import com.lhzkml.jasmine.utils.JsonInstant

class PreferenceStoreV2Migration : DataMigration<Preferences> {
    private val TTS_PROVIDERS = stringPreferencesKey("tts_providers")
    private val SELECTED_TTS_PROVIDER = stringPreferencesKey("selected_tts_provider")

    override suspend fun shouldMigrate(currentData: Preferences): Boolean {
        val version = currentData[SettingsStore.VERSION]
        return version == null || version < 2
    }

    override suspend fun migrate(currentData: Preferences): Preferences {
        val prefs = currentData.toMutablePreferences()

        // 移除历史遗留的 TTS 相关键
        prefs.remove(TTS_PROVIDERS)
        prefs.remove(SELECTED_TTS_PROVIDER)

        // 升级版本号
        prefs[SettingsStore.VERSION] = 2

        return prefs.toPreferences()
    }

    override suspend fun cleanUp() {}
}

