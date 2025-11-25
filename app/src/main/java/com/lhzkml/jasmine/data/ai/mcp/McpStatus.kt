package com.lhzkml.jasmine.data.ai.mcp

sealed class McpStatus {
    object Idle : McpStatus()
    object Connecting : McpStatus()
    object Connected : McpStatus()
    class Error(val message: String) : McpStatus()
}
