package com.lhzkml.jasmine.data.datastore.migration

import androidx.datastore.core.DataMigration
import androidx.datastore.preferences.core.Preferences
import com.lhzkml.jasmine.data.datastore.SettingsStore

class PreferenceStoreV2Migration : DataMigration<Preferences> {

    override suspend fun shouldMigrate(currentData: Preferences): Boolean {
        val version = currentData[SettingsStore.VERSION]
        return version == null || version < 2
    }

    override suspend fun migrate(currentData: Preferences): Preferences {
        val prefs = currentData.toMutablePreferences()

        // 升级版本号
        prefs[SettingsStore.VERSION] = 2

        return prefs.toPreferences()
    }

    override suspend fun cleanUp() {}
}
