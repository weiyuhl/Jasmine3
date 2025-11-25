package com.lhzkml.jasmine.data.ai.tools

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import com.lhzkmlai.core.InputSchema
import com.lhzkmlai.core.Tool
import com.lhzkml.jasmine.data.datastore.SettingsStore
import org.koin.core.component.KoinComponent
import org.koin.core.component.get
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MarkdownFileTool(private val context: Context) : KoinComponent {
    val tool = Tool(
        name = "write_markdown_md",
        description = "Save markdown content as a local .md file and return its URI",
        systemPrompt = { _, _ ->
            """
            ## tool: write_markdown_md

            ### when to use
            - 仅当用户明确要求“保存/导出为本地 .md（内容为 Markdown）”时调用
            - 不要基于推测或你认为内容有用而主动调用；意图不明确时先询问确认
            - 用户未提供文件名或未明确同意保存时，应先确认再调用

            ### arguments
            - filename: 以 .md 结尾；不包含路径；若缺失后缀将自动补齐
            - content: 完整 Markdown 文本；不做渲染或转换

            ### notes
            - 若文件名已存在，将自动追加时间戳或使用 UUID 生成唯一文件名
            - 写入编码为 UTF-8，统一换行 \n，不写 BOM
            - 仅在授权目录内写入；若未授权，将先请求授权后再写入
            - 当助手的本地工具开关未启用该工具时，不应在请求中声明或调用该工具
            """.trimIndent()
        },
        parameters = {
            InputSchema.Obj(
                properties = buildJsonObject {
                    put("filename", buildJsonObject {
                        put("type", "string")
                        put("description", "The final file name with .md suffix")
                    })
                    put("content", buildJsonObject {
                        put("type", "string")
                        put("description", "Markdown content to write")
                    })
                    put("subdir", buildJsonObject {
                        put("type", "string")
                        put("description", "Relative subdirectory under authorized SAF tree")
                    })
                },
                required = listOf("filename", "content")
            )
        },
        execute = { args: JsonElement ->
            val params = args.jsonObject
            val rawName = params["filename"]?.jsonPrimitive?.contentOrNull ?: error("filename is required")
            val content = params["content"]?.jsonPrimitive?.contentOrNull ?: error("content is required")
            val subdir = params["subdir"]?.jsonPrimitive?.contentOrNull

            val name = sanitizeFileName(ensureMdSuffix(rawName))
            val settingsStore = get<SettingsStore>()
            val treeUriStr = settingsStore.settingsFlow.value.defaultSaveDir ?: error("E_PERMISSION_DENIED: missing SAF treeUri")
            val treeUri = Uri.parse(treeUriStr)
            val dir = DocumentFile.fromTreeUri(context, treeUri) ?: error("E_PERMISSION_DENIED: invalid SAF treeUri")
            require(dir.isDirectory) { "E_PATH_OUT_OF_SCOPE: target is not a directory" }
            val targetDir = subdir?.let {
                val segs = it.split('/')
                    .map { s -> s.trim() }
                    .filter { s -> s.isNotEmpty() }
                if (segs.isEmpty()) dir else resolveDir(dir, segs)
            } ?: dir

            val finalName = uniqueName(targetDir, name)
            val file = targetDir.createFile("text/markdown", finalName) ?: error("E_IO_WRITE_FAILED: cannot create file")
            val uri = file.uri

            context.contentResolver.openOutputStream(uri, "w").use { out ->
                requireNotNull(out) { "E_IO_WRITE_FAILED: cannot open output stream" }
                val bytes = normalizeContent(content).toByteArray(Charsets.UTF_8)
                out.write(bytes)
                out.flush()
                val size = bytes.size
                val sha256 = sha256Hex(bytes)
                buildJsonObject {
                    put("uri", JsonPrimitive(uri.toString()))
                    put("path", JsonPrimitive(uri.toString()))
                    put("filename", JsonPrimitive(finalName))
                    put("size", JsonPrimitive(size))
                    put("sha256", JsonPrimitive(sha256))
                    put("created_at", JsonPrimitive(System.currentTimeMillis()))
                }
            }
        }
    )

    private fun ensureMdSuffix(name: String): String {
        return if (name.lowercase(Locale.ROOT).endsWith(".md")) name else "$name.md"
    }

    private fun sanitizeFileName(name: String): String {
        val normalized = java.text.Normalizer.normalize(name, java.text.Normalizer.Form.NFKC)
        val cleaned = normalized.replace(Regex("[\\\\/:*?\\\"<>|\\u0000-\\u001F]"), "_").trim()
        return if (cleaned.length > 120) cleaned.substring(0, 120) else cleaned
    }

    private fun uniqueName(dir: DocumentFile, base: String): String {
        val exists = dir.listFiles().any { it.name == base }
        if (!exists) return base
        val ts = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.ROOT).format(Date())
        val dot = base.lastIndexOf('.')
        return if (dot > 0) {
            val prefix = base.substring(0, dot)
            val suffix = base.substring(dot)
            "${prefix}_$ts$suffix"
        } else "${base}_$ts"
    }

    private fun resolveDir(dir: DocumentFile, segments: List<String>): DocumentFile {
        var cur = dir
        for (seg in segments) {
            val child = cur.listFiles().firstOrNull { it.name == seg && it.isDirectory }
            cur = child ?: (cur.createDirectory(seg) ?: error("E_IO_WRITE_FAILED: cannot create directory"))
        }
        return cur
    }

    private fun normalizeContent(text: String): String {
        return text.replace("\r\n", "\n").replace("\r", "\n")
    }

    private fun sha256Hex(bytes: ByteArray): String {
        val md = MessageDigest.getInstance("SHA-256")
        val digest = md.digest(bytes)
        val sb = StringBuilder(digest.size * 2)
        digest.forEach { b ->
            sb.append(((b.toInt() and 0xFF) + 0x100).toString(16).substring(1))
        }
        return sb.toString()
    }
}
