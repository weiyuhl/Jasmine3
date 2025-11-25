package com.lhzkml.jasmine.data.ai.tools

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import android.webkit.MimeTypeMap
import androidx.documentfile.provider.DocumentFile
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import com.lhzkmlai.core.InputSchema
import com.lhzkmlai.core.Tool
import com.lhzkml.jasmine.data.datastore.SettingsStore
import org.koin.core.component.KoinComponent
import org.koin.core.component.get
import java.security.MessageDigest

class FileSystemTools(private val context: Context) : KoinComponent {
    val readLocalFileTool = Tool(
        name = "read_local_file",
        description = "Read local file content via SAF (text or binary)",
        systemPrompt = { _, _ ->
            """
            ## tool: read_local_file
            ### when to use
            - 读取本地文件内容时调用
            ### arguments
            - uri: SAF 文件 URI
            - binary: 可选，是否以二进制返回（base64）
            - encoding: 可选，文本编码（默认 UTF-8）
            - offset: 可选，起始字节偏移
            - length: 可选，读取长度
            - maxBytes: 可选，内容大小上限
            """.trimIndent()
        },
        parameters = {
            InputSchema.Obj(
                properties = buildJsonObject {
                    put("uri", buildJsonObject {
                        put("type", "string")
                        put("description", "SAF file URI")
                    })
                    put("binary", buildJsonObject {
                        put("type", "boolean")
                        put("description", "return base64 when true")
                    })
                    put("encoding", buildJsonObject {
                        put("type", "string")
                        put("description", "text encoding, default UTF-8")
                    })
                    put("offset", buildJsonObject {
                        put("type", "integer")
                        put("description", "start offset in bytes")
                    })
                    put("length", buildJsonObject {
                        put("type", "integer")
                        put("description", "read length in bytes")
                    })
                    put("maxBytes", buildJsonObject {
                        put("type", "integer")
                        put("description", "upper size limit")
                    })
                },
                required = listOf("uri")
            )
        },
        execute = { args: JsonElement ->
            val obj = args.jsonObject
            val uriStr = obj["uri"]?.jsonPrimitive?.contentOrNull ?: error("uri is required")
            val binary = obj["binary"]?.jsonPrimitive?.booleanOrNull ?: false
            val encoding = obj["encoding"]?.jsonPrimitive?.contentOrNull ?: "UTF-8"
            val offset = obj["offset"]?.jsonPrimitive?.intOrNull
            val length = obj["length"]?.jsonPrimitive?.intOrNull
            val maxBytes = obj["maxBytes"]?.jsonPrimitive?.intOrNull
            val uri = Uri.parse(uriStr)
            val doc = DocumentFile.fromSingleUri(context, uri) ?: error("E_PERMISSION_DENIED: invalid SAF uri")
            require(!doc.isDirectory) { "E_PATH_OUT_OF_SCOPE: target is a directory" }
            val mime = doc.type ?: context.contentResolver.getType(uri) ?: "application/octet-stream"
            context.contentResolver.openInputStream(uri).use { input ->
                requireNotNull(input) { "E_IO_READ_FAILED: cannot open input stream" }
                val buf = ByteArray(8192)
                val out = java.io.ByteArrayOutputStream()
                var total = 0
                if (offset != null && offset > 0) {
                    var toSkip = offset.toLong()
                    while (toSkip > 0) {
                        val skipped = input.skip(toSkip)
                        if (skipped <= 0) break
                        toSkip -= skipped
                    }
                }
                var remain = length ?: Int.MAX_VALUE
                while (true) {
                    val readLen = kotlin.math.min(buf.size, remain)
                    val n = input.read(buf, 0, readLen)
                    if (n <= 0) break
                    total += n
                    if (maxBytes != null && total > maxBytes) error("E_SIZE_EXCEEDED: file too large")
                    out.write(buf, 0, n)
                    remain -= n
                    if (remain <= 0) break
                }
                val bytes = out.toByteArray()
                val sha256 = sha256Hex(bytes)
                val totalLen = doc.length()
                val off = (offset ?: 0).toLong()
                val len = (length ?: 0).toLong()
                val hasNext = len > 0L && off + len < totalLen
                buildJsonObject {
                    put("uri", JsonPrimitive(uri.toString()))
                    put("filename", JsonPrimitive(doc.name ?: "unknown"))
                    put("size", JsonPrimitive(bytes.size))
                    put("mimeType", JsonPrimitive(mime))
                    put("sha256", JsonPrimitive(sha256))
                    put("totalFileLen", JsonPrimitive(totalLen))
                    put("hasNextPage", JsonPrimitive(hasNext))
                    if (binary) {
                        val b64 = android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP)
                        put("base64Content", JsonPrimitive(b64))
                    } else {
                        val cs = try { java.nio.charset.Charset.forName(encoding) } catch (_: Exception) { Charsets.UTF_8 }
                        val content = bytes.toString(cs).replace("\r\n", "\n").replace("\r", "\n")
                        put("content", JsonPrimitive(content))
                    }
                }
            }
        }
    )

    val listDirectoryTool = Tool(
        name = "list_directory",
        description = "List directory entries via SAF",
        systemPrompt = { _, _ ->
            """
            ## tool: list_directory
            ### when to use
            - 列出目录下的文件时调用
            ### arguments
            - dirUri: 必填，目录 SAF 树 URI
            - recursive: 可选，是否递归
            - maxItems: 可选，返回数量上限
            - maxDepth: 可选，递归最大深度
            - excludeExtensions: 可选，按后缀排除
            """.trimIndent()
        },
        parameters = {
            InputSchema.Obj(
                properties = buildJsonObject {
                    put("dirUri", buildJsonObject {
                        put("type", "string")
                        put("description", "SAF tree URI of directory")
                    })
                    put("recursive", buildJsonObject {
                        put("type", "boolean")
                        put("description", "list recursively")
                    })
                    put("maxItems", buildJsonObject {
                        put("type", "integer")
                        put("description", "limit items")
                    })
                    put("maxDepth", buildJsonObject {
                        put("type", "integer")
                        put("description", "max recursion depth")
                    })
                    put("excludeExtensions", buildJsonObject {
                        put("type", "array")
                        put("items", buildJsonObject { put("type", "string") })
                        put("description", "exclude by extensions")
                    })
                },
                required = listOf("dirUri")
            )
        },
        execute = { args: JsonElement ->
            val obj = args.jsonObject
            val dirUriStr = obj["dirUri"]?.jsonPrimitive?.contentOrNull ?: error("E_PERMISSION_DENIED: missing SAF treeUri")
            val recursive = obj["recursive"]?.jsonPrimitive?.booleanOrNull ?: false
            val maxItems = obj["maxItems"]?.jsonPrimitive?.intOrNull ?: 1000
            val maxDepth = obj["maxDepth"]?.jsonPrimitive?.intOrNull ?: Int.MAX_VALUE
            val excludeExts = obj["excludeExtensions"]?.let { el ->
                el.jsonArray.mapNotNull { it.jsonPrimitive.contentOrNull?.lowercase() }
            } ?: emptyList()
            val dirUri = Uri.parse(dirUriStr)
            val dir = DocumentFile.fromTreeUri(context, dirUri) ?: error("E_PERMISSION_DENIED: invalid SAF treeUri")
            require(dir.isDirectory) { "E_PATH_OUT_OF_SCOPE: target is not a directory" }
            val items = mutableListOf<JsonObject>()
            fun extOf(name: String?): String? {
                if (name == null) return null
                val i = name.lastIndexOf('.')
                return if (i >= 0 && i < name.length - 1) name.substring(i + 1).lowercase() else null
            }
            fun walk(d: DocumentFile, depth: Int) {
                if (items.size >= maxItems) return
                if (depth > maxDepth) return
                d.listFiles().forEach { f ->
                    if (items.size >= maxItems) return@forEach
                    val ext = extOf(f.name)
                    if (ext != null && excludeExts.contains(ext)) return@forEach
                    items += buildJsonObject {
                        put("name", JsonPrimitive(f.name ?: "unknown"))
                        put("size", JsonPrimitive(f.length()))
                        put("mimeType", JsonPrimitive(f.type ?: "application/octet-stream"))
                        put("uri", JsonPrimitive(f.uri.toString()))
                        put("isDirectory", JsonPrimitive(f.isDirectory))
                        put("lastModified", JsonPrimitive(f.lastModified()))
                    }
                    if (recursive && f.isDirectory) walk(f, depth + 1)
                }
            }
            walk(dir, 0)
            buildJsonObject {
                put("dirUri", JsonPrimitive(dirUri.toString()))
                put("count", JsonPrimitive(items.size))
                put("items", JsonArray(items))
                put("hasNextPage", JsonPrimitive(false))
                put("hasPrevPage", JsonPrimitive(false))
                put("itemsRemaining", JsonPrimitive(0))
            }
        }
    )

    private fun sha256Hex(bytes: ByteArray): String {
        val md = MessageDigest.getInstance("SHA-256")
        val digest = md.digest(bytes)
        val sb = StringBuilder(digest.size * 2)
        digest.forEach { b ->
            sb.append(((b.toInt() and 0xFF) + 0x100).toString(16).substring(1))
        }
        return sb.toString()
    }

    val getDirTreeTool = Tool(
        name = "get_dir_tree",
        description = "Return directory tree as text",
        parameters = {
            InputSchema.Obj(
                properties = buildJsonObject {
                    put("uri", buildJsonObject {
                        put("type", "string")
                        put("description", "SAF tree URI of root folder")
                    })
                    put("maxDepth", buildJsonObject {
                        put("type", "integer")
                        put("description", "max depth of tree")
                    })
                    put("excludeExtensions", buildJsonObject {
                        put("type", "array")
                        put("items", buildJsonObject { put("type", "string") })
                        put("description", "exclude file extensions")
                    })
                },
                required = listOf("uri")
            )
        },
        execute = { args: JsonElement ->
            val obj = args.jsonObject
            val uriStr = obj["uri"]?.jsonPrimitive?.contentOrNull ?: error("E_PERMISSION_DENIED: missing SAF treeUri")
            val maxDepth = obj["maxDepth"]?.jsonPrimitive?.intOrNull ?: Int.MAX_VALUE
            val excludeExts = obj["excludeExtensions"]?.let { el ->
                el.jsonArray.mapNotNull { it.jsonPrimitive.contentOrNull?.lowercase() }
            } ?: emptyList()
            val dirUri = Uri.parse(uriStr)
            val dir = DocumentFile.fromTreeUri(context, dirUri) ?: error("E_PERMISSION_DENIED: invalid SAF treeUri")
            require(dir.isDirectory) { "E_PATH_OUT_OF_SCOPE: target is not a directory" }
            fun extOf(name: String?): String? {
                if (name == null) return null
                val i = name.lastIndexOf('.')
                return if (i >= 0 && i < name.length - 1) name.substring(i + 1).lowercase() else null
            }
            val sb = StringBuilder()
            val rootName = dir.name ?: "root"
            sb.append("/").append(rootName).append("\n")
            fun walk(d: DocumentFile, depth: Int) {
                if (depth > maxDepth) return
                d.listFiles().forEach { f ->
                    val ext = extOf(f.name)
                    if (ext != null && excludeExts.contains(ext)) return@forEach
                    sb.append("  ".repeat(depth)).append("- ")
                        .append(f.name ?: "unknown")
                        .append(if (f.isDirectory) "/" else "")
                        .append("\n")
                    if (f.isDirectory) walk(f, depth + 1)
                }
            }
            walk(dir, 1)
            buildJsonObject {
                put("str", JsonPrimitive(sb.toString()))
            }
        }
    )

    val searchPathnamesOnlyTool = Tool(
        name = "search_pathnames_only",
        description = "Search by filename only and return URIs",
        parameters = {
            InputSchema.Obj(
                properties = buildJsonObject {
                    put("query", buildJsonObject {
                        put("type", "string")
                        put("description", "query string")
                    })
                    put("include_pattern", buildJsonObject {
                        put("type", "string")
                        put("description", "optional regex/glob pattern")
                    })
                    put("search_in_folder", buildJsonObject {
                        put("type", "string")
                        put("description", "SAF tree URI to limit search")
                    })
                    put("page_number", buildJsonObject {
                        put("type", "integer")
                        put("description", "optional page number")
                    })
                },
                required = listOf("query")
            )
        },
        execute = { args: JsonElement ->
            val obj = args.jsonObject
            val query = obj["query"]?.jsonPrimitive?.contentOrNull ?: error("query is required")
            val include = obj["include_pattern"]?.jsonPrimitive?.contentOrNull
            val folder = obj["search_in_folder"]?.jsonPrimitive?.contentOrNull
            val settingsStore = get<SettingsStore>()
            val rootUriStr = folder ?: settingsStore.settingsFlow.value.defaultSaveDir ?: error("E_PERMISSION_DENIED: missing SAF treeUri")
            val rootUri = Uri.parse(rootUriStr)
            val dir = DocumentFile.fromTreeUri(context, rootUri) ?: error("E_PERMISSION_DENIED: invalid SAF treeUri")
            require(dir.isDirectory) { "E_PATH_OUT_OF_SCOPE: target is not a directory" }
            val q = query.lowercase()
            val regex = include?.let {
                runCatching { Regex(it) }.getOrNull()
            }
            val results = mutableListOf<String>()
            fun walk(d: DocumentFile) {
                d.listFiles().forEach { f ->
                    val name = f.name ?: ""
                    val match = name.lowercase().contains(q) && (regex?.containsMatchIn(name) ?: true)
                    if (match) results += f.uri.toString()
                    if (f.isDirectory) walk(f)
                }
            }
            walk(dir)
            buildJsonObject {
                put("uris", JsonArray(results.map { JsonPrimitive(it) }))
                put("hasNextPage", JsonPrimitive(false))
            }
        }
    )

    val searchForFilesTool = Tool(
        name = "search_for_files",
        description = "Search files by content and return URIs",
        parameters = {
            InputSchema.Obj(
                properties = buildJsonObject {
                    put("query", buildJsonObject {
                        put("type", "string")
                        put("description", "query string")
                    })
                    put("is_regex", buildJsonObject {
                        put("type", "boolean")
                        put("description", "treat query as regex")
                    })
                    put("search_in_folder", buildJsonObject {
                        put("type", "string")
                        put("description", "SAF tree URI to limit search")
                    })
                    put("page_number", buildJsonObject {
                        put("type", "integer")
                        put("description", "optional page number")
                    })
                    put("maxBytesPerFile", buildJsonObject {
                        put("type", "integer")
                        put("description", "max bytes to read per file")
                    })
                },
                required = listOf("query")
            )
        },
        execute = { args: JsonElement ->
            val obj = args.jsonObject
            val query = obj["query"]?.jsonPrimitive?.contentOrNull ?: error("query is required")
            val isRegex = obj["is_regex"]?.jsonPrimitive?.booleanOrNull ?: false
            val folder = obj["search_in_folder"]?.jsonPrimitive?.contentOrNull
            val maxPer = obj["maxBytesPerFile"]?.jsonPrimitive?.intOrNull ?: (1024 * 1024)
            val settingsStore = get<SettingsStore>()
            val rootUriStr = folder ?: settingsStore.settingsFlow.value.defaultSaveDir ?: error("E_PERMISSION_DENIED: missing SAF treeUri")
            val rootUri = Uri.parse(rootUriStr)
            val dir = DocumentFile.fromTreeUri(context, rootUri) ?: error("E_PERMISSION_DENIED: invalid SAF treeUri")
            require(dir.isDirectory) { "E_PATH_OUT_OF_SCOPE: target is not a directory" }
            val regex = if (isRegex) runCatching { Regex(query) }.getOrNull() else null
            val results = mutableListOf<String>()
            fun shouldSearchFile(f: DocumentFile): Boolean {
                val t = f.type ?: "application/octet-stream"
                val size = f.length()
                return t.startsWith("text/") || size <= maxPer
            }
            fun matchesContent(f: DocumentFile): Boolean {
                if (!shouldSearchFile(f)) return false
                context.contentResolver.openInputStream(f.uri).use { input ->
                    if (input == null) return false
                    val buf = ByteArray(8192)
                    val out = java.io.ByteArrayOutputStream()
                    var total = 0
                    while (true) {
                        val n = input.read(buf)
                        if (n <= 0) break
                        total += n
                        out.write(buf, 0, n)
                        if (total >= maxPer) break
                    }
                    val content = out.toByteArray().toString(Charsets.UTF_8).replace("\r\n", "\n").replace("\r", "\n")
                    return if (regex != null) regex.containsMatchIn(content) else content.contains(query)
                }
            }
            fun walk(d: DocumentFile) {
                d.listFiles().forEach { f ->
                    if (f.isDirectory) {
                        walk(f)
                    } else {
                        if (matchesContent(f)) results += f.uri.toString()
                    }
                }
            }
            walk(dir)
            buildJsonObject {
                put("uris", JsonArray(results.map { JsonPrimitive(it) }))
                put("hasNextPage", JsonPrimitive(false))
            }
        }
    )

    val searchInFileTool = Tool(
        name = "search_in_file",
        description = "Search inside a file and return line numbers",
        parameters = {
            InputSchema.Obj(
                properties = buildJsonObject {
                    put("uri", buildJsonObject {
                        put("type", "string")
                        put("description", "SAF file URI")
                    })
                    put("query", buildJsonObject {
                        put("type", "string")
                        put("description", "query string or regex")
                    })
                    put("is_regex", buildJsonObject {
                        put("type", "boolean")
                        put("description", "treat query as regex")
                    })
                    put("encoding", buildJsonObject {
                        put("type", "string")
                        put("description", "text encoding")
                    })
                },
                required = listOf("uri", "query")
            )
        },
        execute = { args: JsonElement ->
            val obj = args.jsonObject
            val uriStr = obj["uri"]?.jsonPrimitive?.contentOrNull ?: error("uri is required")
            val query = obj["query"]?.jsonPrimitive?.contentOrNull ?: error("query is required")
            val isRegex = obj["is_regex"]?.jsonPrimitive?.booleanOrNull ?: false
            val encoding = obj["encoding"]?.jsonPrimitive?.contentOrNull ?: "UTF-8"
            val uri = Uri.parse(uriStr)
            val doc = DocumentFile.fromSingleUri(context, uri) ?: error("E_PERMISSION_DENIED: invalid SAF uri")
            require(!doc.isDirectory) { "E_PATH_OUT_OF_SCOPE: target is a directory" }
            context.contentResolver.openInputStream(uri).use { input ->
                requireNotNull(input) { "E_IO_READ_FAILED: cannot open input stream" }
                val content = input.readBytes().toString(runCatching { java.nio.charset.Charset.forName(encoding) }.getOrDefault(Charsets.UTF_8))
                    .replace("\r\n", "\n").replace("\r", "\n")
                val linesArr = mutableListOf<JsonPrimitive>()
                val previewArr = mutableListOf<JsonObject>()
                val regex = if (isRegex) runCatching { Regex(query) }.getOrNull() else null
                var lineNo = 0
                for (line in content.split("\n")) {
                    lineNo += 1
                    val matched = if (regex != null) regex.containsMatchIn(line) else line.contains(query)
                    if (matched) {
                        linesArr += JsonPrimitive(lineNo)
                        if (previewArr.size < 100) {
                            previewArr += buildJsonObject {
                                put("line", JsonPrimitive(lineNo))
                                put("text", JsonPrimitive(line))
                            }
                        }
                    }
                }
                buildJsonObject {
                    put("lines", JsonArray(linesArr))
                    if (previewArr.isNotEmpty()) put("preview", JsonArray(previewArr))
                }
            }
        }
    )

    private fun requireAuthorizedTreeUri(): Uri {
        val settingsStore = get<SettingsStore>()
        val treeUriStr = settingsStore.settingsFlow.value.defaultSaveDir ?: error("E_PERMISSION_DENIED: missing SAF treeUri")
        return Uri.parse(treeUriStr)
    }

    private fun ensureWithinAuthorizedTree(treeUri: Uri, targetUri: Uri) {
        val treeId = DocumentsContract.getTreeDocumentId(treeUri)
        val docId = DocumentsContract.getDocumentId(targetUri)
        val ok = docId == treeId || docId.startsWith("$treeId/")
        require(ok) { "E_PATH_OUT_OF_SCOPE: uri out of authorized tree" }
    }

    private fun extMime(name: String): String {
        val i = name.lastIndexOf('.')
        if (i <= 0 || i >= name.length - 1) return "application/octet-stream"
        val ext = name.substring(i + 1).lowercase()
        val mime = MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext)
        return mime ?: "application/octet-stream"
    }

    private fun resolveUnderTree(dir: DocumentFile, segments: List<String>, isDirectory: Boolean): DocumentFile {
        var cur = dir
        val lastIdx = segments.size - 1
        for (idx in 0..lastIdx) {
            val seg = segments[idx]
            val child = cur.listFiles().firstOrNull { it.name == seg }
            if (child != null) {
                cur = child
                continue
            }
            if (idx == lastIdx) {
                cur = if (isDirectory) {
                    cur.createDirectory(seg) ?: error("E_IO_WRITE_FAILED: cannot create directory")
                } else {
                    cur.createFile(extMime(seg), seg) ?: error("E_IO_WRITE_FAILED: cannot create file")
                }
            } else {
                cur = cur.createDirectory(seg) ?: error("E_IO_WRITE_FAILED: cannot create parent directory")
            }
        }
        return cur
    }

    val createFileOrFolderTool = Tool(
        name = "create_file_or_folder",
        description = "Create a file or folder under authorized SAF tree",
        parameters = {
            InputSchema.Obj(
                properties = buildJsonObject {
                    put("uri", buildJsonObject {
                        put("type", "string")
                        put("description", "Target document URI within SAF tree")
                    })
                    put("is_directory", buildJsonObject {
                        put("type", "boolean")
                        put("description", "Create directory when true")
                    })
                },
                required = listOf("uri")
            )
        },
        execute = { args: JsonElement ->
            val obj = args.jsonObject
            val uriStr = obj["uri"]?.jsonPrimitive?.contentOrNull ?: error("uri is required")
            val isDir = obj["is_directory"]?.jsonPrimitive?.booleanOrNull ?: false
            val targetUri = Uri.parse(uriStr)
            val treeUri = requireAuthorizedTreeUri()
            ensureWithinAuthorizedTree(treeUri, targetUri)
            val treeId = DocumentsContract.getTreeDocumentId(treeUri)
            val docId = DocumentsContract.getDocumentId(targetUri)
            val rel = if (docId.startsWith("$treeId/")) docId.removePrefix("$treeId/") else ""
            val segs = rel.split('/').filter { it.isNotEmpty() }
            require(segs.isNotEmpty()) { "E_PATH_OUT_OF_SCOPE: missing target path segments" }
            val dir = DocumentFile.fromTreeUri(context, treeUri) ?: error("E_PERMISSION_DENIED: invalid SAF treeUri")
            val created = resolveUnderTree(dir, segs, isDir)
            val mime = created.type ?: context.contentResolver.getType(created.uri) ?: "application/octet-stream"
            buildJsonObject {
                put("uri", JsonPrimitive(created.uri.toString()))
                put("filename", JsonPrimitive(created.name ?: "unknown"))
                put("mimeType", JsonPrimitive(mime))
                put("created_at", JsonPrimitive(System.currentTimeMillis()))
            }
        }
    )

    val deleteFileOrFolderTool = Tool(
        name = "delete_file_or_folder",
        description = "Delete a file or folder under authorized SAF tree",
        parameters = {
            InputSchema.Obj(
                properties = buildJsonObject {
                    put("uri", buildJsonObject {
                        put("type", "string")
                        put("description", "Target document URI to delete")
                    })
                    put("is_recursive", buildJsonObject {
                        put("type", "boolean")
                        put("description", "Delete directory recursively when true")
                    })
                },
                required = listOf("uri")
            )
        },
        execute = { args: JsonElement ->
            val obj = args.jsonObject
            val uriStr = obj["uri"]?.jsonPrimitive?.contentOrNull ?: error("uri is required")
            val recursive = obj["is_recursive"]?.jsonPrimitive?.booleanOrNull ?: false
            val targetUri = Uri.parse(uriStr)
            val treeUri = requireAuthorizedTreeUri()
            ensureWithinAuthorizedTree(treeUri, targetUri)
            val doc = DocumentFile.fromSingleUri(context, targetUri) ?: error("E_PERMISSION_DENIED: invalid SAF uri")
            fun deleteRec(d: DocumentFile): Boolean {
                if (d.isDirectory) {
                    d.listFiles().forEach { deleteRec(it) }
                }
                return d.delete()
            }
            val ok = if (doc.isDirectory) {
                require(recursive) { "E_PATH_OUT_OF_SCOPE: deleting directory requires is_recursive" }
                deleteRec(doc)
            } else {
                doc.delete()
            }
            buildJsonObject {
                put("deleted", JsonPrimitive(ok))
            }
        }
    )

    val editFileTool = Tool(
        name = "edit_file",
        description = "Search and replace blocks in a file",
        parameters = {
            InputSchema.Obj(
                properties = buildJsonObject {
                    put("uri", buildJsonObject {
                        put("type", "string")
                        put("description", "Target file URI")
                    })
                    put("search_replace_blocks", buildJsonObject {
                        put("type", "array")
                    })
                },
                required = listOf("uri", "search_replace_blocks")
            )
        },
        execute = { args: JsonElement ->
            val obj = args.jsonObject
            val uriStr = obj["uri"]?.jsonPrimitive?.contentOrNull ?: error("uri is required")
            val blocks = obj["search_replace_blocks"]?.jsonArray ?: error("search_replace_blocks is required")
            val targetUri = Uri.parse(uriStr)
            val treeUri = requireAuthorizedTreeUri()
            ensureWithinAuthorizedTree(treeUri, targetUri)
            val doc = DocumentFile.fromSingleUri(context, targetUri) ?: error("E_PERMISSION_DENIED: invalid SAF uri")
            require(!doc.isDirectory) { "E_PATH_OUT_OF_SCOPE: target is a directory" }
            val original = context.contentResolver.openInputStream(targetUri).use { input ->
                requireNotNull(input) { "E_IO_READ_FAILED: cannot open input stream" }
                input.readBytes().toString(Charsets.UTF_8).replace("\r\n", "\n").replace("\r", "\n")
            }
            var content = original
            var applied = 0
            for (el in blocks) {
                val b = el.jsonObject
                val search = b["search"]?.jsonPrimitive?.contentOrNull ?: continue
                val replace = b["replace"]?.jsonPrimitive?.contentOrNull ?: ""
                val isRegex = b["is_regex"]?.jsonPrimitive?.booleanOrNull ?: false
                val matchLimit = b["match_limit"]?.jsonPrimitive?.intOrNull ?: Int.MAX_VALUE
                if (isRegex) {
                    val regex = runCatching { Regex(search, setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.MULTILINE)) }.getOrNull() ?: continue
                    var count = 0
                    content = regex.replace(content) {
                        if (count >= matchLimit) it.value else {
                            count += 1
                            replace
                        }
                    }
                    applied += count
                } else {
                    var count = 0
                    var idx = content.indexOf(search)
                    val sb = StringBuilder()
                    var last = 0
                    while (idx >= 0 && count < matchLimit) {
                        sb.append(content, last, idx)
                        sb.append(replace)
                        last = idx + search.length
                        count += 1
                        idx = content.indexOf(search, last)
                    }
                    sb.append(content.substring(last))
                    if (count > 0) content = sb.toString()
                    applied += count
                }
            }
            val bytes = content.toByteArray(Charsets.UTF_8)
            context.contentResolver.openOutputStream(targetUri, "w").use { out ->
                requireNotNull(out) { "E_IO_WRITE_FAILED: cannot open output stream" }
                out.write(bytes)
                out.flush()
            }
            val sha256 = sha256Hex(bytes)
            buildJsonObject {
                put("uri", JsonPrimitive(targetUri.toString()))
                put("size", JsonPrimitive(bytes.size))
                put("sha256", JsonPrimitive(sha256))
                put("modified_at", JsonPrimitive(System.currentTimeMillis()))
                put("applied", JsonPrimitive(applied))
            }
        }
    )

    val rewriteFileTool = Tool(
        name = "rewrite_file",
        description = "Rewrite whole file content",
        parameters = {
            InputSchema.Obj(
                properties = buildJsonObject {
                    put("uri", buildJsonObject {
                        put("type", "string")
                        put("description", "Target file URI")
                    })
                    put("new_content", buildJsonObject {
                        put("type", "string")
                        put("description", "New content to write")
                    })
                    put("encoding", buildJsonObject {
                        put("type", "string")
                        put("description", "Optional encoding")
                    })
                },
                required = listOf("uri", "new_content")
            )
        },
        execute = { args: JsonElement ->
            val obj = args.jsonObject
            val uriStr = obj["uri"]?.jsonPrimitive?.contentOrNull ?: error("uri is required")
            val content = obj["new_content"]?.jsonPrimitive?.contentOrNull ?: error("new_content is required")
            val encoding = obj["encoding"]?.jsonPrimitive?.contentOrNull ?: "UTF-8"
            val targetUri = Uri.parse(uriStr)
            val treeUri = requireAuthorizedTreeUri()
            ensureWithinAuthorizedTree(treeUri, targetUri)
            val doc = DocumentFile.fromSingleUri(context, targetUri) ?: error("E_PERMISSION_DENIED: invalid SAF uri")
            require(!doc.isDirectory) { "E_PATH_OUT_OF_SCOPE: target is a directory" }
            val cs = runCatching { java.nio.charset.Charset.forName(encoding) }.getOrDefault(Charsets.UTF_8)
            val normalized = content.replace("\r\n", "\n").replace("\r", "\n")
            val bytes = normalized.toByteArray(cs)
            context.contentResolver.openOutputStream(targetUri, "w").use { out ->
                requireNotNull(out) { "E_IO_WRITE_FAILED: cannot open output stream" }
                out.write(bytes)
                out.flush()
            }
            val sha256 = sha256Hex(bytes)
            buildJsonObject {
                put("uri", JsonPrimitive(targetUri.toString()))
                put("size", JsonPrimitive(bytes.size))
                put("sha256", JsonPrimitive(sha256))
                put("modified_at", JsonPrimitive(System.currentTimeMillis()))
            }
        }
    )
}
