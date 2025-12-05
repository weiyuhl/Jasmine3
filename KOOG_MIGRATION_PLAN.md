# Jasmine AI 模块迁移到 Koog 框架方案

## 1. 概述

### 1.1 目标
将 Jasmine 现有的自研 AI 模块（`ai/` 目录）替换为 JetBrains Koog 框架，以获得：
- 更强大的 Agent 能力（图工作流、状态管理）
- 更多 LLM 提供商支持（DeepSeek、Ollama、Bedrock 等）
- 企业级特性（OpenTelemetry、持久化、记忆）
- 更好的流式处理和结构化输出支持

### 1.2 当前架构分析

#### Jasmine 现有 AI 模块结构
```
ai/src/main/java/com/lhzkml/ai/
├── core/                    # 核心抽象
│   ├── MessageRole.kt       # 消息角色枚举
│   ├── Reasoning.kt         # 推理相关
│   ├── Tool.kt              # 工具定义
│   └── Usage.kt             # Token 使用统计
├── provider/                # Provider 抽象层
│   ├── Provider.kt          # Provider 接口（无状态设计）
│   ├── ProviderSetting.kt   # Provider 配置（OpenAI/Google/Claude）
│   ├── ProviderManager.kt   # Provider 管理器
│   ├── Model.kt             # 模型定义
│   └── providers/           # 具体实现
│       ├── OpenAIProvider.kt
│       ├── GoogleProvider.kt
│       └── ClaudeProvider.kt
├── ui/
│   └── Message.kt           # UI 消息模型（UIMessage, UIMessagePart）
├── registry/
│   ├── ModelMatcher.kt
│   └── ModelRegistry.kt
└── util/                    # 工具类
    ├── SSE.kt               # SSE 流处理
    ├── Request.kt           # HTTP 请求
    └── ...
```

#### Koog 框架核心结构
```
koog/
├── agents/                  # Agent 核心
│   ├── agents-core/         # AIAgent, Strategy, Environment
│   ├── agents-tools/        # Tool, ToolRegistry
│   ├── agents-mcp/          # MCP 集成
│   └── agents-features-*/   # 特性扩展
├── prompt/                  # LLM 交互层
│   ├── prompt-model/        # Prompt, Message 模型
│   ├── prompt-executor/     # PromptExecutor 抽象
│   └── prompt-executor-clients/  # LLM 客户端实现
│       ├── prompt-executor-openai-client/
│       ├── prompt-executor-anthropic-client/
│       ├── prompt-executor-google-client/
│       └── ...
└── http-client/             # HTTP 客户端抽象
```


## 2. 架构映射分析

### 2.1 核心概念映射

| Jasmine 现有概念 | Koog 对应概念 | 说明 |
|-----------------|--------------|------|
| `Provider<T>` | `LLMClient` | LLM 提供商抽象 |
| `ProviderSetting` | `LLMClient` 配置 | 提供商配置（API Key、Base URL 等） |
| `ProviderManager` | `PromptExecutor` | 执行器管理，Koog 支持多 LLM 编排 |
| `Model` | `LLModel` | 模型定义 |
| `UIMessage` | `Message` | 消息模型 |
| `UIMessagePart` | `Message.Content` | 消息内容部分 |
| `MessageRole` | `Message.Role` | 消息角色 |
| `Tool` | `Tool<TArgs, TResult>` | 工具定义 |
| `TextGenerationParams` | `PromptExecutor.execute()` 参数 | 生成参数 |
| `MessageChunk` | `StreamFrame` | 流式响应帧 |
| `TokenUsage` | `ResponseMetaInfo.usage` | Token 使用统计 |

### 2.1.1 Jasmine 核心接口详细分析

#### Provider 接口
```kotlin
// 无状态设计，每次调用都需要传入 ProviderSetting
interface Provider<T : ProviderSetting> {
    suspend fun listModels(providerSetting: T): List<Model>
    suspend fun getBalance(providerSetting: T): String  // 默认返回 "TODO"
    suspend fun generateText(providerSetting: T, messages: List<UIMessage>, params: TextGenerationParams): MessageChunk
    suspend fun streamText(providerSetting: T, messages: List<UIMessage>, params: TextGenerationParams): Flow<MessageChunk>
}
```

#### UIMessage 数据模型
```kotlin
data class UIMessage(
    val id: Uuid,
    val role: MessageRole,           // SYSTEM, USER, ASSISTANT, TOOL
    val parts: List<UIMessagePart>,  // 支持多种内容类型
    val annotations: List<UIMessageAnnotation>,  // URL 引用等
    val createdAt: LocalDateTime,
    val modelId: Uuid?,
    val usage: TokenUsage?
)

// 支持的消息部分类型
sealed class UIMessagePart {
    data class Text(val text: String, var metadata: JsonObject?)
    data class Image(val url: String, var metadata: JsonObject?)
    data class Video(val url: String, var metadata: JsonObject?)
    data class Document(val url: String, val fileName: String, val mime: String, var metadata: JsonObject?)
    data class Reasoning(val reasoning: String, val createdAt: Instant, val finishedAt: Instant?, var metadata: JsonObject?)
    data class ToolCall(val toolCallId: String, val toolName: String, val arguments: String, var metadata: JsonObject?)
    data class ToolResult(val toolCallId: String, val toolName: String, val content: JsonElement, val arguments: JsonElement, var metadata: JsonObject?)
}
```

#### Tool 定义
```kotlin
data class Tool(
    val name: String,
    val description: String,
    val parameters: () -> InputSchema?,  // 延迟计算参数 schema
    val systemPrompt: (model: Model, messages: List<UIMessage>) -> String,  // 动态系统提示
    val execute: suspend (JsonElement) -> JsonElement  // 执行函数
)
```

### 2.1.2 Koog 核心接口详细分析

#### LLMClient 接口
```kotlin
interface LLMClient {
    suspend fun execute(prompt: Prompt, model: LLModel, params: LLMParams? = null): String
    suspend fun executeStreaming(prompt: Prompt, model: LLModel, params: LLMParams? = null): Flow<StreamFrame>
    suspend fun executeWithStructuredOutput<T>(prompt: Prompt, model: LLModel, config: StructuredOutputConfig<T>): T
    suspend fun moderate(content: String): ModerationResult  // 部分客户端支持
    suspend fun embed(texts: List<String>): List<List<Float>>  // 部分客户端支持
}
```

#### Prompt 和 Message 模型
```kotlin
// Prompt DSL 构建
val prompt = prompt {
    system("You are a helpful assistant")
    user("Hello")
    assistant("Hi there!")
    user {
        text("What's in this image?")
        image("/path/to/image.jpg")  // 多模态支持
    }
}

// Message 类型
sealed class Message {
    data class System(val content: String)
    data class User(val content: List<Content>)  // 支持多模态
    data class Assistant(val content: String, val toolCalls: List<ToolCall>?)
    data class Tool(val toolCallId: String, val content: String)
}
```

#### StreamFrame 类型
```kotlin
sealed class StreamFrame {
    data class Append(val text: String) : StreamFrame()  // 文本增量
    data class ToolCall(val id: String, val name: String, val args: String) : StreamFrame()  // 工具调用
    data class End(val finishReason: String?, val metaInfo: ResponseMetaInfo?) : StreamFrame()  // 结束帧
}
```

#### Tool 系统
```kotlin
// 方式1: 注解式
class MyToolSet : ToolSet {
    @Tool
    @LLMDescription("Adds two numbers")
    fun add(@LLMDescription("First number") a: Int, @LLMDescription("Second number") b: Int): Int = a + b
}

// 方式2: Lambda 式
val tool = simpleTool(
    name = "search",
    description = "Search the web"
) { args: SearchArgs -> searchService.search(args.query) }

// 方式3: 类式
class SearchTool : Tool<SearchArgs, SearchResult> {
    override val name = "search"
    override val description = "Search the web"
    override suspend fun execute(args: SearchArgs, enabler: DirectToolCallsEnabler): SearchResult { ... }
}

### 2.2 Provider 类型映射

| Jasmine Provider | Koog Client | 模块 | 多模态支持 |
|-----------------|-------------|------|-----------|
| `ProviderSetting.OpenAI` | `OpenAILLMClient` | `prompt-executor-openai-client` | 图片✅ 音频✅ 文档✅ |
| `ProviderSetting.Google` | `GoogleLLMClient` | `prompt-executor-google-client` | 图片✅ 音频✅ 视频✅ 文档✅ |
| `ProviderSetting.Claude` | `AnthropicLLMClient` | `prompt-executor-anthropic-client` | 图片✅ 文档✅ |
| - | `DeepSeekLLMClient` | `prompt-executor-deepseek-client` | 继承 OpenAI 基类 |
| - | `OllamaLLMClient` | `prompt-executor-ollama-client` | 图片✅ |
| - | `OpenRouterLLMClient` | `prompt-executor-openrouter-client` | 图片✅ 音频✅ 文档✅ |
| - | `BedrockLLMClient` | `prompt-executor-bedrock-client` | 取决于底层模型 |
| - | `MistralAILLMClient` | `prompt-executor-mistralai-client` | 图片✅ 文档✅ |
| - | `DashscopeLLMClient` | `prompt-executor-dashscope-client` | 阿里云通义 |

### 2.2.1 Jasmine Provider 实现特点分析

#### OpenAIProvider
- 支持 Chat Completions API 和 Responses API（通过 `useResponseApi` 开关）
- 使用 `KeyRoulette` 实现多 API Key 轮询
- 支持自定义 `chatCompletionsPath`
- 支持余额查询（`getBalance`）

#### GoogleProvider
- 同时支持 Google AI Studio 和 Vertex AI
- Vertex AI 使用 Service Account 认证（`ServiceAccountTokenProvider`）
- 支持内置工具：`google_search`、`url_context`
- 支持 Thinking/Reasoning 模式（Gemini 2.5 Pro）
- 支持图片生成输出（`responseModalities: ["TEXT", "IMAGE"]`）

#### ClaudeProvider
- 使用 `anthropic-version: 2023-06-01` 头
- 支持 Thinking 模式（`thinking.type: enabled/disabled`）
- 支持 `thinking_delta`、`signature_delta` 流式事件
- Tool Result 作为 `user` 角色发送

### 2.2.2 Koog LLMClient 实现特点

#### OpenAILLMClient
- 支持 Chat Completions 和 Responses API
- 内置 `RetryingLLMClient` 装饰器支持重试
- 支持 `OpenAIChatParams` 和 `OpenAIResponsesParams` 参数
- 支持 `reasoningEffort` 参数
- 支持 `webSearchOptions` 内置搜索

#### GoogleLLMClient
- 仅支持 Google AI Studio（不支持 Vertex AI）
- 支持完整多模态（图片、音频、视频、文档）
- 支持 `thinkingConfig` 参数

#### AnthropicLLMClient
- 支持 PDF 文档处理
- 支持 Thinking 模式

### 2.3 消息模型映射

#### 2.3.1 消息部分类型对比

| Jasmine UIMessagePart | Koog 对应 | 转换说明 |
|----------------------|-----------|---------|
| `Text(text)` | `Message.Content.Text(text)` | 直接映射 |
| `Image(url)` | `Message.Content.Image(path/url)` | 需处理 base64 和 URL |
| `Video(url)` | `Message.Content.Video(path)` | 仅 Google 支持 |
| `Document(url, fileName, mime)` | `Message.Content.Document(path)` | 需处理文件路径 |
| `Reasoning(reasoning, ...)` | 无直接对应 | 需特殊处理，可能作为 metadata |
| `ToolCall(id, name, args)` | `Message.ToolCall(id, name, args)` | 结构相似 |
| `ToolResult(id, name, content, args)` | `Message.Tool(id, content)` | 需简化 |

#### 2.3.2 关键差异

1. **Reasoning 处理**：Jasmine 将 Reasoning 作为独立的 `UIMessagePart`，Koog 没有直接对应，需要：
   - 在适配层保留 Reasoning 信息
   - 或将其作为特殊的 Text 内容处理

2. **ToolResult 结构**：
   - Jasmine: 包含 `toolCallId`, `toolName`, `content`, `arguments`
   - Koog: 仅包含 `toolCallId`, `content`
   - 需要在适配层保留额外信息

3. **Metadata 支持**：Jasmine 的每个 `UIMessagePart` 都有 `metadata: JsonObject?`，用于存储额外信息（如 `thoughtSignature`），Koog 没有直接对应

4. **Annotations**：Jasmine 支持 `UIMessageAnnotation.UrlCitation`（用于搜索结果引用），Koog 没有直接对应

#### 2.3.3 流式响应映射

| Jasmine MessageChunk | Koog StreamFrame | 说明 |
|---------------------|------------------|------|
| `choices[0].delta.parts[Text]` | `StreamFrame.Append(text)` | 文本增量 |
| `choices[0].delta.parts[ToolCall]` | `StreamFrame.ToolCall(id, name, args)` | 工具调用 |
| `choices[0].finishReason` | `StreamFrame.End(finishReason, metaInfo)` | 结束信号 |
| `usage: TokenUsage` | `StreamFrame.End.metaInfo.usage` | Token 统计 |

```kotlin
// Jasmine MessageChunk 结构
data class MessageChunk(
    val id: String,
    val model: String,
    val choices: List<UIMessageChoice>,
    val usage: TokenUsage?
)

data class UIMessageChoice(
    val index: Int,
    val delta: UIMessage?,      // 流式增量
    val message: UIMessage?,    // 完整消息（非流式）
    val finishReason: String?
)

// Koog StreamFrame 结构
sealed class StreamFrame {
    data class Append(val text: String)
    data class ToolCall(val id: String, val name: String, val args: String)
    data class End(val finishReason: String?, val metaInfo: ResponseMetaInfo?)
}
```


## 3. 迁移策略

### 3.1 迁移方式选择

**推荐方案：渐进式迁移 + 适配层**

由于 Jasmine 的 UI 层和数据层深度依赖现有的 `UIMessage`、`UIMessagePart` 等模型，建议：

1. **保留现有 UI 消息模型**：`UIMessage`、`UIMessagePart` 等继续使用
2. **创建适配层**：在 Koog 和 Jasmine 之间建立转换层
3. **替换底层实现**：用 Koog 的 `PromptExecutor` 替换现有 `Provider` 实现
4. **逐步迁移**：先迁移核心聊天功能，再迁移高级特性

### 3.2 模块依赖变更

```kotlin
// ai/build.gradle.kts 新增依赖
dependencies {
    // Koog 核心
    implementation("ai.koog:koog-agents:0.5.4")
    
    // 或者按需引入特定模块
    implementation("ai.koog:prompt-executor-model:0.5.4")
    implementation("ai.koog:prompt-executor-openai-client:0.5.4")
    implementation("ai.koog:prompt-executor-anthropic-client:0.5.4")
    implementation("ai.koog:prompt-executor-google-client:0.5.4")
    implementation("ai.koog:prompt-executor-ollama-client:0.5.4")
    implementation("ai.koog:prompt-executor-deepseek-client:0.5.4")
    implementation("ai.koog:prompt-executor-openrouter-client:0.5.4")
    
    // MCP 支持（可选）
    implementation("ai.koog:agents-mcp:0.5.4")
    
    // 移除现有 OkHttp SSE 依赖（Koog 使用 Ktor）
    // api(libs.okhttp.sse) // 可能需要保留用于其他功能
}
```

### 3.3 版本兼容性

#### 3.3.1 依赖版本对比

| 依赖 | Jasmine 当前版本 | Koog 要求版本 | 兼容性 |
|-----|-----------------|--------------|--------|
| Kotlin | 2.2.21 | 2.2.10 | ✅ 兼容（Jasmine 更新） |
| kotlinx-coroutines | 1.10.2 | 1.10.2 | ✅ 完全匹配 |
| kotlinx-serialization | 1.9.0 | 1.8.1 | ✅ 兼容（Jasmine 更新） |
| kotlinx-datetime | 0.7.1 | 0.6.2 | ✅ 兼容（Jasmine 更新） |
| Ktor | 3.3.2 | 3.2.2 | ✅ 兼容（Jasmine 更新） |
| OkHttp | 5.1.0 | - | ⚠️ Koog 使用 Ktor，需评估 |
| MCP SDK | 0.7.7 | 0.7.2 | ✅ 兼容（Jasmine 更新） |

#### 3.3.2 HTTP 客户端策略

**当前状态**：
- Jasmine 使用 OkHttp 5.1.0 + OkHttp SSE
- Koog 使用 Ktor 3.2.2（支持多种引擎：CIO、OkHttp、Apache5）

**推荐策略**：混合使用
```kotlin
// Koog 支持 OkHttp 作为 Ktor 引擎
implementation("io.ktor:ktor-client-okhttp:3.2.2")

// 这样可以：
// 1. 复用现有 OkHttp 配置（代理、拦截器等）
// 2. 保持与其他模块的兼容性（Retrofit、Coil 等）
```

#### 3.3.3 潜在冲突

1. **Ktor 版本**：Jasmine 已有 Ktor 3.3.2，Koog 要求 3.2.2
   - 解决：统一使用 Jasmine 的 3.3.2（向后兼容）

2. **kotlinx-serialization**：Jasmine 1.9.0 vs Koog 1.8.1
   - 解决：使用 Jasmine 的 1.9.0（向后兼容）

3. **MCP SDK**：Jasmine 0.7.7 vs Koog 0.7.2
   - 解决：使用 Jasmine 的 0.7.7（向后兼容）

**注意**：Jasmine 目前使用 OkHttp，需要评估是否完全切换到 Ktor 或保持混用。


## 4. 详细迁移步骤

### 4.1 Phase 1: 基础设施准备

#### 4.1.1 添加 Koog 依赖
- 更新 `gradle/libs.versions.toml` 添加 Koog 版本
- 更新 `ai/build.gradle.kts` 添加 Koog 依赖
- 解决版本冲突（kotlinx-coroutines、kotlinx-serialization）

#### 4.1.2 创建适配层目录结构
```
ai/src/main/java/com/lhzkml/ai/
├── koog/                    # 新增：Koog 适配层
│   ├── adapter/             # 消息转换适配器
│   │   ├── MessageAdapter.kt
│   │   └── ToolAdapter.kt
│   ├── executor/            # 执行器封装
│   │   └── KoogExecutorFactory.kt
│   └── client/              # 客户端配置
│       └── KoogClientConfig.kt
├── provider/                # 保留：逐步废弃
└── ...
```

### 4.2 Phase 2: 消息模型适配

#### 4.2.1 创建 MessageAdapter
```kotlin
// ai/src/main/java/com/lhzkml/ai/koog/adapter/MessageAdapter.kt
object MessageAdapter {
    // UIMessage -> Koog Message
    fun toKoogMessage(uiMessage: UIMessage): Message {
        return when (uiMessage.role) {
            MessageRole.USER -> Message.User(
                content = uiMessage.parts.mapNotNull { part ->
                    when (part) {
                        is UIMessagePart.Text -> Message.Content.Text(part.text)
                        is UIMessagePart.Image -> Message.Content.Image(part.url)
                        else -> null
                    }
                }
            )
            MessageRole.ASSISTANT -> Message.Assistant(
                content = uiMessage.toText(),
                toolCalls = uiMessage.getToolCalls().map { tc ->
                    ToolCall(tc.toolCallId, tc.toolName, tc.arguments)
                }.takeIf { it.isNotEmpty() }
            )
            MessageRole.SYSTEM -> Message.System(uiMessage.toText())
            MessageRole.TOOL -> Message.Tool(
                toolCallId = uiMessage.getToolResults().firstOrNull()?.toolCallId ?: "",
                content = uiMessage.getToolResults().firstOrNull()?.content?.toString() ?: ""
            )
        }
    }
    
    // Koog Message -> UIMessage
    fun toUIMessage(message: Message): UIMessage {
        // 反向转换实现
    }
    
    // Koog StreamFrame -> MessageChunk
    fun toMessageChunk(frame: StreamFrame): MessageChunk {
        // 流式帧转换
    }
}
```

#### 4.2.2 创建 ToolAdapter
```kotlin
// ai/src/main/java/com/lhzkml/ai/koog/adapter/ToolAdapter.kt
object ToolAdapter {
    // Jasmine Tool -> Koog Tool
    fun toKoogTool(tool: com.lhzkmlai.core.Tool): ai.koog.agents.tools.Tool<*, *> {
        return simpleTool(
            name = tool.name,
            description = tool.description
        ) { args: JsonObject ->
            tool.execute(args)
        }
    }
}
```


### 4.3 Phase 3: Provider 替换

#### 4.3.1 创建 KoogExecutorFactory
```kotlin
// ai/src/main/java/com/lhzkml/ai/koog/executor/KoogExecutorFactory.kt
class KoogExecutorFactory {
    
    fun createExecutor(providerSetting: ProviderSetting): PromptExecutor {
        val client = createClient(providerSetting)
        return SingleLLMPromptExecutor(client)
    }
    
    private fun createClient(setting: ProviderSetting): LLMClient {
        return when (setting) {
            is ProviderSetting.OpenAI -> OpenAILLMClient(
                apiKey = setting.apiKey,
                baseUrl = setting.baseUrl,
                // 其他配置
            )
            is ProviderSetting.Google -> GoogleLLMClient(
                apiKey = setting.apiKey,
                // 其他配置
            )
            is ProviderSetting.Claude -> AnthropicLLMClient(
                apiKey = setting.apiKey,
                baseUrl = setting.baseUrl,
            )
        }
    }
}
```

#### 4.3.2 创建 KoogProvider（适配现有接口）
```kotlin
// ai/src/main/java/com/lhzkml/ai/koog/KoogProvider.kt
class KoogProvider<T : ProviderSetting>(
    private val executorFactory: KoogExecutorFactory
) : Provider<T> {
    
    override suspend fun listModels(providerSetting: T): List<Model> {
        // 使用 Koog 获取模型列表
    }
    
    override suspend fun generateText(
        providerSetting: T,
        messages: List<UIMessage>,
        params: TextGenerationParams,
    ): MessageChunk {
        val executor = executorFactory.createExecutor(providerSetting)
        val koogMessages = messages.map { MessageAdapter.toKoogMessage(it) }
        
        val prompt = Prompt {
            messages.forEach { +it }
        }
        
        val response = executor.execute(prompt, params.model.toKoogModel())
        return MessageAdapter.toMessageChunk(response)
    }
    
    override suspend fun streamText(
        providerSetting: T,
        messages: List<UIMessage>,
        params: TextGenerationParams,
    ): Flow<MessageChunk> {
        val executor = executorFactory.createExecutor(providerSetting)
        val koogMessages = messages.map { MessageAdapter.toKoogMessage(it) }
        
        return executor.executeStreaming(prompt, model)
            .map { frame -> MessageAdapter.toMessageChunk(frame) }
    }
}
```

#### 4.3.3 更新 ProviderManager
```kotlin
// ai/src/main/java/com/lhzkml/ai/provider/ProviderManager.kt
class ProviderManager(client: OkHttpClient) {
    private val koogExecutorFactory = KoogExecutorFactory()
    private val providers = mutableMapOf<String, Provider<*>>()

    init {
        // 使用 Koog 实现替换原有 Provider
        registerProvider("openai", KoogProvider<ProviderSetting.OpenAI>(koogExecutorFactory))
        registerProvider("google", KoogProvider<ProviderSetting.Google>(koogExecutorFactory))
        registerProvider("claude", KoogProvider<ProviderSetting.Claude>(koogExecutorFactory))
        
        // 新增 Provider（Koog 支持但 Jasmine 原本不支持的）
        registerProvider("deepseek", KoogProvider<ProviderSetting.DeepSeek>(koogExecutorFactory))
        registerProvider("ollama", KoogProvider<ProviderSetting.Ollama>(koogExecutorFactory))
        registerProvider("openrouter", KoogProvider<ProviderSetting.OpenRouter>(koogExecutorFactory))
    }
    // ...
}
```


### 4.4 Phase 4: 新增 Provider 类型

#### 4.4.1 扩展 ProviderSetting
```kotlin
// ai/src/main/java/com/lhzkml/ai/provider/ProviderSetting.kt
@Serializable
sealed class ProviderSetting {
    // 现有类型保持不变
    @Serializable @SerialName("openai")
    data class OpenAI(...) : ProviderSetting()
    
    @Serializable @SerialName("google")
    data class Google(...) : ProviderSetting()
    
    @Serializable @SerialName("claude")
    data class Claude(...) : ProviderSetting()
    
    // 新增类型（利用 Koog 支持）
    @Serializable @SerialName("deepseek")
    data class DeepSeek(
        override var id: Uuid = Uuid.random(),
        override var enabled: Boolean = true,
        override var name: String = "DeepSeek",
        override var models: List<Model> = emptyList(),
        override var proxy: ProviderProxy = ProviderProxy.None,
        override val balanceOption: BalanceOption = BalanceOption(),
        var apiKey: String = "",
        var baseUrl: String = "https://api.deepseek.com/v1",
    ) : ProviderSetting() { ... }
    
    @Serializable @SerialName("ollama")
    data class Ollama(
        override var id: Uuid = Uuid.random(),
        override var enabled: Boolean = true,
        override var name: String = "Ollama",
        override var models: List<Model> = emptyList(),
        var baseUrl: String = "http://localhost:11434",
    ) : ProviderSetting() { ... }
    
    @Serializable @SerialName("openrouter")
    data class OpenRouter(
        override var id: Uuid = Uuid.random(),
        override var enabled: Boolean = true,
        override var name: String = "OpenRouter",
        override var models: List<Model> = emptyList(),
        var apiKey: String = "",
    ) : ProviderSetting() { ... }
    
    companion object {
        val Types by lazy {
            listOf(
                OpenAI::class,
                Google::class,
                Claude::class,
                DeepSeek::class,    // 新增
                Ollama::class,      // 新增
                OpenRouter::class,  // 新增
            )
        }
    }
}
```

### 4.5 Phase 5: 工具系统迁移

#### 4.5.1 MCP 工具适配
```kotlin
// app/src/main/java/com/lhzkml/jasmine/data/ai/mcp/KoogMcpAdapter.kt
class KoogMcpAdapter(
    private val mcpManager: McpManager
) {
    fun getKoogTools(): List<ai.koog.agents.tools.Tool<*, *>> {
        return mcpManager.getAllAvailableTools().map { mcpTool ->
            simpleTool(
                name = mcpTool.name,
                description = mcpTool.description ?: ""
            ) { args: JsonObject ->
                mcpManager.callTool(mcpTool.name, args)
            }
        }
    }
}
```

#### 4.5.2 搜索工具适配
```kotlin
// 现有搜索工具可以直接转换为 Koog Tool
fun createSearchTool(settings: Settings): ai.koog.agents.tools.Tool<*, *> {
    return simpleTool(
        name = "search_web",
        description = "search web for latest information"
    ) { args: SearchArgs ->
        val service = SearchService.getService(settings.searchOptions)
        service.search(args.query, settings.searchCommonOptions)
    }
}
```


### 4.6 Phase 6: 高级特性迁移

#### 4.6.1 流式输出增强
Koog 提供更强大的流式 API：
```kotlin
// 使用 Koog 的 StreamFrame
executor.executeStreaming(prompt, model).collect { frame ->
    when (frame) {
        is StreamFrame.Append -> {
            // 文本分片
            updateUI(frame.text)
        }
        is StreamFrame.ToolCall -> {
            // 工具调用（支持并行）
            handleToolCall(frame.id, frame.name, frame.args)
        }
        is StreamFrame.End -> {
            // 结束，包含 finishReason 和 usage
            finalize(frame.metaInfo)
        }
    }
}
```

#### 4.6.2 结构化输出
Koog 原生支持结构化输出：
```kotlin
// 定义输出结构
@Serializable
data class TitleResponse(val title: String)

// 使用结构化输出
val result = executor.executeWithStructuredOutput<TitleResponse>(
    prompt = prompt,
    model = model,
    config = StructuredOutputConfig()
)
```

#### 4.6.3 历史压缩
Koog 内置历史压缩功能，可优化长对话的 Token 使用：
```kotlin
// 安装 Tokenizer 特性
val agent = AIAgent(executor) {
    install(Tokenizer) {
        // 配置 Token 计数
    }
}
```

### 4.7 Phase 7: 清理与优化

#### 4.7.1 移除旧代码
迁移完成后，可以移除：
- `ai/src/main/java/com/lhzkml/ai/provider/providers/` 目录下的旧 Provider 实现
- `ai/src/main/java/com/lhzkml/ai/util/SSE.kt`（Koog 内置 SSE 处理）
- 其他不再需要的工具类

#### 4.7.2 依赖清理
```kotlin
// ai/build.gradle.kts
dependencies {
    // 移除
    // api(libs.okhttp.sse)  // 如果完全使用 Koog
    
    // 保留（如果其他模块仍需要）
    api(libs.okhttp)
}
```


## 5. 需要修改的文件清单

### 5.1 构建配置
| 文件 | 修改内容 |
|-----|---------|
| `gradle/libs.versions.toml` | 添加 Koog 版本和依赖 |
| `ai/build.gradle.kts` | 添加 Koog 依赖，调整版本 |
| `app/build.gradle.kts` | 可能需要调整依赖传递 |

### 5.2 AI 模块（新增）
| 文件 | 说明 |
|-----|------|
| `ai/.../koog/adapter/MessageAdapter.kt` | 消息模型转换 |
| `ai/.../koog/adapter/ToolAdapter.kt` | 工具模型转换 |
| `ai/.../koog/executor/KoogExecutorFactory.kt` | Koog 执行器工厂 |
| `ai/.../koog/KoogProvider.kt` | 适配现有 Provider 接口 |

### 5.3 AI 模块（修改）
| 文件 | 修改内容 |
|-----|---------|
| `ai/.../provider/ProviderSetting.kt` | 新增 DeepSeek/Ollama/OpenRouter 类型 |
| `ai/.../provider/ProviderManager.kt` | 使用 Koog 实现替换 |
| `ai/.../provider/Model.kt` | 可能需要扩展模型能力 |

### 5.4 App 模块（修改）
| 文件 | 修改内容 |
|-----|---------|
| `app/.../data/ai/GenerationHandler.kt` | 适配 Koog 执行器 |
| `app/.../service/ChatService.kt` | 调整工具创建逻辑 |
| `app/.../ui/pages/setting/SettingProviderPage.kt` | 新增 Provider 类型 UI |
| `app/.../ui/pages/setting/SettingProviderDetailPage.kt` | 新增 Provider 配置 UI |
| `app/.../data/datastore/DefaultProviders.kt` | 新增默认 Provider 配置 |

### 5.5 AI 模块（可删除）
| 文件 | 说明 |
|-----|------|
| `ai/.../provider/providers/OpenAIProvider.kt` | 被 Koog 替代 |
| `ai/.../provider/providers/GoogleProvider.kt` | 被 Koog 替代 |
| `ai/.../provider/providers/ClaudeProvider.kt` | 被 Koog 替代 |
| `ai/.../util/SSE.kt` | Koog 内置 SSE |


## 6. 功能差异与特殊处理

### 6.1 Jasmine 独有功能（需特殊处理）

| 功能 | Jasmine 实现 | Koog 支持 | 处理方案 |
|-----|-------------|----------|---------|
| **Vertex AI** | `GoogleProvider` 支持 Service Account 认证 | ❌ 不支持 | 保留原 GoogleProvider 或扩展 Koog |
| **余额查询** | `Provider.getBalance()` | ❌ 不支持 | 保留原实现，独立于 Koog |
| **自定义 Headers** | `TextGenerationParams.customHeaders` | ⚠️ 部分支持 | 需要扩展 Koog 客户端配置 |
| **自定义 Body** | `TextGenerationParams.customBody` | ⚠️ 部分支持 | 需要扩展 Koog 客户端配置 |
| **Key 轮询** | `KeyRoulette` 多 API Key 轮询 | ❌ 不支持 | 保留原实现，在适配层处理 |
| **Reasoning Metadata** | `UIMessagePart.Reasoning.metadata` 存储 `signature` | ❌ 不支持 | 在适配层保留 |
| **Response API** | OpenAI `useResponseApi` 开关 | ✅ 支持 | 使用 `OpenAIResponsesParams` |
| **内置工具** | Google `google_search`, `url_context` | ⚠️ 部分支持 | 需要验证 Koog 支持情况 |

### 6.2 Koog 新增功能（可利用）

| 功能 | 说明 | 价值 |
|-----|------|------|
| **RetryingLLMClient** | 内置重试逻辑，支持多种策略 | 提高稳定性 |
| **DefaultMultiLLMPromptExecutor** | 多 LLM 编排，自动切换 | 提高可用性 |
| **CachedPromptExecutor** | 响应缓存 | 降低成本 |
| **结构化输出** | `executeWithStructuredOutput<T>()` | 简化解析 |
| **Agent 系统** | 图策略、状态管理 | 复杂任务支持 |
| **OpenTelemetry** | 可观测性集成 | 监控调试 |
| **Memory 特性** | 跨会话记忆 | 增强上下文 |
| **更多 Provider** | DeepSeek、Ollama、OpenRouter、Bedrock、Mistral | 更多选择 |

### 6.3 需要保留的原实现

基于功能差异分析，以下功能建议保留原实现：

```kotlin
// 1. Vertex AI 支持 - 保留 GoogleProvider 中的 Vertex AI 逻辑
class VertexAIProvider(private val client: OkHttpClient) : Provider<ProviderSetting.Google> {
    // 保留 ServiceAccountTokenProvider
    // 保留 Vertex AI URL 构建逻辑
}

// 2. 余额查询 - 独立实现
interface BalanceQueryService {
    suspend fun getBalance(providerSetting: ProviderSetting): String
}

// 3. Key 轮询 - 在适配层使用
class KoogClientFactory {
    private val keyRoulette = KeyRoulette.default()
    
    fun createClient(setting: ProviderSetting.OpenAI): OpenAILLMClient {
        val apiKey = keyRoulette.next(setting.apiKey)  // 轮询选择 key
        return OpenAILLMClient(apiKey = apiKey, ...)
    }
}
```

## 7. 风险与挑战

### 7.1 技术风险

| 风险 | 影响 | 缓解措施 |
|-----|------|---------|
| HTTP 客户端差异 | Koog 使用 Ktor，Jasmine 使用 OkHttp | 使用 `ktor-client-okhttp` 引擎 |
| 消息模型差异 | 转换可能丢失信息 | 仔细设计适配层，保留所有必要字段 |
| 流式处理差异 | Koog StreamFrame 与 MessageChunk 结构不同 | 创建完整的转换逻辑 |
| 版本冲突 | kotlinx 库版本可能冲突 | 统一使用较新版本（向后兼容） |
| Android 兼容性 | Koog 主要面向 JVM | 验证 Android 目标的兼容性 |

### 7.2 功能风险

| 风险 | 影响 | 缓解措施 |
|-----|------|---------|
| 自定义 Header/Body | Jasmine 支持自定义请求头和请求体 | 扩展 Koog 客户端或保留部分原实现 |
| Proxy 支持 | Jasmine 支持 HTTP 代理 | 使用 Ktor OkHttp 引擎配置代理 |
| 余额查询 | Jasmine 有余额查询功能 | 保留原实现，独立于 Koog |
| Vertex AI | Jasmine 支持 Google Vertex AI | 保留原 GoogleProvider 的 Vertex AI 部分 |
| Reasoning Metadata | Jasmine 存储 thinking signature | 在适配层保留，不传递给 Koog |

### 7.3 迁移风险

| 风险 | 影响 | 缓解措施 |
|-----|------|---------|
| 回归问题 | 迁移可能引入 bug | 充分测试，保留回滚能力 |
| 性能变化 | Ktor vs OkHttp 性能差异 | 使用 OkHttp 引擎，性能一致 |
| 用户数据兼容 | Provider 配置序列化格式变化 | 新增类型使用新 SerialName，不影响现有 |


## 7. 迁移收益

### 7.1 新增功能
- **更多 LLM 提供商**：DeepSeek、Ollama、OpenRouter、Bedrock、Mistral、Dashscope
- **Agent 能力**：图工作流、状态管理、复杂任务编排
- **结构化输出**：原生支持 JSON Schema 输出
- **历史压缩**：自动优化长对话 Token 使用
- **多 LLM 编排**：支持在多个提供商间切换和负载均衡
- **可观测性**：OpenTelemetry 集成，支持 Langfuse、W&B Weave

### 7.2 代码质量提升
- **减少维护负担**：不再需要维护多个 Provider 实现
- **统一抽象**：使用 Koog 的统一 API
- **更好的测试**：Koog 提供 Mock 工具
- **类型安全**：Koog 的工具系统提供更好的类型安全

### 7.3 未来扩展
- **A2A 协议**：Agent 间通信能力
- **RAG 支持**：向量存储和检索增强生成
- **持久化**：Agent 状态持久化和恢复
- **记忆系统**：跨会话记忆能力

## 8. 时间估算

| 阶段 | 预计时间 | 说明 |
|-----|---------|------|
| Phase 1: 基础设施准备 | 1-2 天 | 依赖配置、目录结构 |
| Phase 2: 消息模型适配 | 2-3 天 | MessageAdapter、ToolAdapter |
| Phase 3: Provider 替换 | 3-4 天 | KoogProvider、ProviderManager |
| Phase 4: 新增 Provider | 2-3 天 | DeepSeek、Ollama、OpenRouter |
| Phase 5: 工具系统迁移 | 2-3 天 | MCP、搜索工具适配 |
| Phase 6: 高级特性 | 3-4 天 | 流式、结构化输出 |
| Phase 7: 清理优化 | 1-2 天 | 移除旧代码、测试 |
| **总计** | **14-21 天** | |


## 9. 测试策略

### 9.1 单元测试
- 测试 MessageAdapter 的双向转换
- 测试 ToolAdapter 的工具转换
- 测试 KoogExecutorFactory 的客户端创建

### 9.2 集成测试
- 测试各 Provider 的基本聊天功能
- 测试流式输出
- 测试工具调用
- 测试 MCP 工具集成

### 9.3 回归测试
- 确保现有功能不受影响
- 对比迁移前后的响应一致性
- 性能对比测试

## 10. 回滚方案

如果迁移过程中遇到严重问题，可以：

1. **保留原实现**：在迁移期间保留原有 Provider 实现
2. **特性开关**：通过配置切换使用 Koog 或原实现
3. **版本控制**：使用 Git 分支管理，随时可回滚

```kotlin
// 示例：特性开关
class ProviderManager(client: OkHttpClient) {
    private val useKoog = BuildConfig.USE_KOOG_FRAMEWORK
    
    init {
        if (useKoog) {
            // 使用 Koog 实现
            registerProvider("openai", KoogProvider(...))
        } else {
            // 使用原实现
            registerProvider("openai", OpenAIProvider(client))
        }
    }
}
```

## 11. 后续优化方向

迁移完成后，可以进一步利用 Koog 的能力：

1. **Agent 工作流**：使用 Koog 的图策略实现复杂对话流程
2. **记忆系统**：利用 Koog 的 Memory 特性增强记忆功能
3. **可观测性**：集成 OpenTelemetry 进行监控
4. **RAG 集成**：利用 Koog 的向量存储实现知识检索
5. **A2A 协议**：实现多 Agent 协作

## 12. 参考资源

- Koog 官方文档：https://docs.koog.ai
- Koog GitHub：https://github.com/JetBrains/koog
- Koog API 文档：https://api.koog.ai
- 示例代码：`koog-0.5.4/examples/`
- DeepWiki 文档：https://deepwiki.com/JetBrains/koog

## 13. 总结与建议

### 13.1 迁移可行性评估

| 评估维度 | 结论 | 说明 |
|---------|------|------|
| **技术可行性** | ✅ 高 | 依赖版本兼容，架构可适配 |
| **功能覆盖度** | ⚠️ 中高 | 大部分功能可迁移，少数需保留原实现 |
| **迁移复杂度** | ⚠️ 中等 | 需要设计适配层，但不涉及大规模重构 |
| **风险可控性** | ✅ 高 | 可渐进式迁移，支持回滚 |

### 13.2 推荐迁移顺序

1. **Phase 1**：添加 Koog 依赖，验证编译通过
2. **Phase 2**：实现 MessageAdapter，验证消息转换
3. **Phase 3**：实现 KoogProvider for OpenAI，验证基本聊天
4. **Phase 4**：扩展到 Google、Claude
5. **Phase 5**：添加新 Provider（DeepSeek、Ollama、OpenRouter）
6. **Phase 6**：迁移工具系统
7. **Phase 7**：清理旧代码

### 13.3 关键决策点

1. **Vertex AI 支持**：是否保留原 GoogleProvider 的 Vertex AI 实现？
   - 建议：保留，Koog 不支持 Vertex AI

2. **余额查询**：是否保留原实现？
   - 建议：保留，作为独立服务

3. **Key 轮询**：是否在适配层实现？
   - 建议：是，在 KoogClientFactory 中实现

4. **HTTP 客户端**：使用 Ktor 还是 OkHttp？
   - 建议：使用 `ktor-client-okhttp` 引擎，复用现有配置

### 13.4 下一步行动

1. 创建 `ai/src/main/java/com/lhzkml/ai/koog/` 目录结构
2. 更新 `gradle/libs.versions.toml` 添加 Koog 依赖
3. 实现 `MessageAdapter` 并编写单元测试
4. 实现 `KoogProvider` 并进行集成测试

---

**文档版本**：2.0  
**创建日期**：2024-12-05  
**更新日期**：2024-12-05  
**作者**：Kiro AI Assistant

### 变更记录

| 版本 | 日期 | 变更内容 |
|-----|------|---------|
| 1.0 | 2024-12-05 | 初始版本 |
| 2.0 | 2024-12-05 | 添加详细源码分析、依赖兼容性分析、功能差异分析 |
