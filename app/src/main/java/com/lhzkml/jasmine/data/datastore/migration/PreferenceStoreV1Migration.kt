package com.lhzkml.jasmine.data.datastore.migration

import androidx.datastore.core.DataMigration
import androidx.datastore.preferences.core.Preferences
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import com.lhzkml.jasmine.data.datastore.SettingsStore
import com.lhzkml.jasmine.utils.JsonInstant

class PreferenceStoreV1Migration : DataMigration<Preferences> {
    override suspend fun shouldMigrate(currentData: Preferences): Boolean {
        val version = currentData[SettingsStore.VERSION]
        return version == null || version < 1
    }

    override suspend fun migrate(currentData: Preferences): Preferences {
        val prefs = currentData.toMutablePreferences()

        // 清理老的没有设置@SerialName的字段
        prefs[SettingsStore.MCP_SERVERS] = prefs[SettingsStore.MCP_SERVERS]?.let { json ->
            val element = JsonInstant.parseToJsonElement(json).jsonArray.map { element ->
                val jsonObj = element.jsonObject.toMutableMap()
                val type = jsonObj["type"]?.jsonPrimitive?.content ?: ""
                when(type) {
                    "com.lhzkml.jasmine.data.mcp.McpServerConfig.SseTransportServer" -> {
                        jsonObj["type"] = JsonPrimitive("sse")
                    }
                    "com.lhzkml.jasmine.data.mcp.McpServerConfig.StreamableHTTPServer" -> {
                        jsonObj["type"] = JsonPrimitive("streamable_http")
                    }
                }
                JsonObject(jsonObj)
            }
            JsonInstant.encodeToString(element)
        } ?: "[]"

        // 更新版本
        prefs[SettingsStore.VERSION] = 1

        return prefs.toPreferences()
    }

    override suspend fun cleanUp() {}
}
