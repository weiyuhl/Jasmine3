package com.lhzkml.jasmine.data.ai.tools

import android.content.Context
import com.whl.quickjs.wrapper.QuickJSContext
import com.whl.quickjs.wrapper.QuickJSObject
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import com.lhzkmlai.core.InputSchema
import com.lhzkmlai.core.Tool

@Serializable
sealed class LocalToolOption {
    @Serializable
    @SerialName("javascript_engine")
    data object JavascriptEngine : LocalToolOption()
    @Serializable
    @SerialName("markdown_txt")
    data object MarkdownTxt : LocalToolOption()
    @Serializable
    @SerialName("filesystem_tools")
    data object FileSystem : LocalToolOption()
}

class LocalTools(private val context: Context) {
    val javascriptTool by lazy {
        Tool(
            name = "eval_javascript",
            description = "Execute JavaScript code with QuickJS. If use this tool to calculate math, better to add `toFixed` to the code.",
            parameters = {
                InputSchema.Obj(
                    properties = buildJsonObject {
                        put("code", buildJsonObject {
                            put("type", "string")
                            put("description", "The JavaScript code to execute")
                        })
                    },
                )
            },
            execute = {
                val context = QuickJSContext.create()
                val code = it.jsonObject["code"]?.jsonPrimitive?.contentOrNull
                val result = context.evaluate(code)
                buildJsonObject {
                    put(
                        "result", when (result) {
                            is QuickJSObject -> JsonPrimitive(result.stringify())
                            else -> JsonPrimitive(result.toString())
                        }
                    )
                }
            }
        )
    }

    private val markdownTool by lazy { MarkdownFileTool(context).tool }
    private val fileSystemTools by lazy { FileSystemTools(context) }

    fun getTools(options: List<LocalToolOption>): List<Tool> {
        val tools = mutableListOf<Tool>()
        if (options.contains(LocalToolOption.JavascriptEngine)) {
            tools.add(javascriptTool)
        }
        if (options.contains(LocalToolOption.MarkdownTxt)) {
            tools.add(markdownTool)
        }
        if (options.contains(LocalToolOption.FileSystem)) {
            tools.add(fileSystemTools.readLocalFileTool)
            tools.add(fileSystemTools.listDirectoryTool)
            tools.add(fileSystemTools.getDirTreeTool)
            tools.add(fileSystemTools.searchPathnamesOnlyTool)
            tools.add(fileSystemTools.searchForFilesTool)
            tools.add(fileSystemTools.searchInFileTool)
            tools.add(fileSystemTools.createFileOrFolderTool)
            tools.add(fileSystemTools.deleteFileOrFolderTool)
            tools.add(fileSystemTools.editFileTool)
            tools.add(fileSystemTools.rewriteFileTool)
        }
        return tools
    }
}
