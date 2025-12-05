# Jasmine AI 模块迁移到 Koog 框架方案

## 1. 概述

### 1.1 目标
将 Jasmine 替换为 JetBrains Koog 框架，以获得：
- 更强大的 Agent 能力（图工作流、状态管理）
- 更多 LLM 提供商支持（DeepSeek、Ollama、Bedrock 等）
- 企业级特性（OpenTelemetry、持久化、记忆）
- 更好的流式处理和结构化输出支持

### 1.2 当前架构分析

#### Jasmine 现有 AI 模块结构

ai/src/main/java/com/lhzkmlai/    # 注意：包名是 com.lhzkmlai（无点分隔）
├── core/                    # 核心抽象
│   ├── MessageRole.kt       # 消息角色枚举
│   ├── Reasoning.kt         # 推理相关（ReasoningLevel 枚举）
│   ├── Tool.kt              # 工具定义
│   └── Usage.kt             # Token 使用统计
├── provider/                # Provider 抽象层
│   ├── Provider.kt          # Provider 接口（无状态设计）
│   ├── ProviderSetting.kt   # Provider 配置（OpenAI/Google/Claude）
│   ├── ProviderManager.kt   # Provider 管理器
│   ├── Model.kt             # 模型定义（含 ModelType, Modality, ModelAbility, BuiltInTools）
│   └── providers/           # 具体实现
│       ├── OpenAIProvider.kt
│       ├── GoogleProvider.kt
│       ├── ClaudeProvider.kt
│       └── openai/          # OpenAI 子模块
│           ├── ChatCompletionsAPI.kt  # Chat Completions API 实现
│           ├── ResponseAPI.kt         # Response API 实现
│           └── OpenAIImpl.kt
├── ui/
│   └── Message.kt           # UI 消息模型（UIMessage, UIMessagePart, MessageChunk）
├── registry/
│   ├── ModelMatcher.kt
│   └── ModelRegistry.kt     # 模型匹配器（如 GEMINI_3_SERIES）
├── mnn/                     # 本地模型支持（MNN 框架）
│   ├── ChatService.kt
│   ├── ChatSession.kt
│   └── ...
└── util/                    # 工具类
    ├── KeyRoulette.kt       # API Key 轮询
    ├── ProxyUtils.kt        # 代理配置
    ├── Request.kt           # HTTP 请求
    └── ...


> 修订：现状为通过 OkHttp 的 `EventSource` 实现（且存在 `ai/src/main/java/com/lhzkml/ai/util/SSE.kt`）。迁移后统一采用 Koog 客户端（Ktor）进行流式处理，并删除 OkHttp SSE 相关依赖与 `SSE.kt`。

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


### 3.2 模块依赖变更

 - 全项目统一采用 Koog/Ktor，所有模块删除 `okhttp-sse` 依赖，并以 Ktor 作为统一 HTTP 客户端。
 - MCP 集成统一迁移至 Koog `agents-mcp`，不再保留自研 MCP 传输链路。

### 3.3 版本兼容性

#### 3.3.1 依赖版本对比

| 依赖 | Jasmine 当前版本 | Koog 要求版本 | 兼容性 |
|-----|-----------------|--------------|--------|
| Kotlin | 2.2.21 | 2.2.10 | 需对齐（以 Koog 为准，降至 2.2.10） |
| kotlinx-coroutines | 1.10.2 | 1.10.2 | 完全匹配 |
| kotlinx-serialization | 1.9.0 | 1.8.1 | 需对齐（降至 1.8.1） |
| kotlinx-datetime | 0.7.1 | 0.6.2 | 需对齐（降至 0.6.2） |
| Ktor | 3.3.2 | 3.2.2 | 需对齐（降至 3.2.2） |
| OkHttp | 5.1.0 | - | 弃用（统一使用 Koog/Ktor；旧实现完全移除） |
| MCP SDK | 0.7.7 | 0.8.0 | 需对齐（以 Koog 为准） |

依赖对齐补充（Koog 必需且 Jasmine 当前未配置）：
- `io.ktor:ktor-client-cio`（Ktor 客户端引擎，统一采用 Ktor，避免 OkHttp）
- `io.ktor:ktor-client-sse`（SSE 流式支持，替代 OkHttp SSE）
- `org.jetbrains.kotlinx:kotlinx-io-core`（Koog 依赖的 I/O 扩展）
- `io.modelcontextprotocol:kotlin-sdk-client` 与 `io.modelcontextprotocol:kotlin-sdk-server`（替换 `io.modelcontextprotocol:kotlin-sdk` 汇总模块）
- Koog MCP 模块（`agents-mcp`）用于统一工具注册与通信（作为 Koog 内部模块使用）
- `ai.koog:koog-agents`（Koog 框架核心聚合）
- `ai.koog.prompt:prompt-executor-openai-client`
- `ai.koog.prompt:prompt-executor-anthropic-client`
- `ai.koog.prompt:prompt-executor-google-client`
- `ai.koog.prompt:prompt-executor-ollama-client`
- `ai.koog.prompt:prompt-executor-deepseek-client`
- `ai.koog.prompt:prompt-executor-openrouter-client`

对齐操作（MCP）：
- 将 `io.modelcontextprotocol:kotlin-sdk` 替换为 `io.modelcontextprotocol:kotlin-sdk-client` 与 `io.modelcontextprotocol:kotlin-sdk-server`（版本统一为 `0.8.0`，以 Koog 为准）。
- 受影响代码文件：
  - `app/src/main/java/com/lhzkml/jasmine/data/ai/mcp/McpManager.kt`
  - `app/src/main/java/com/lhzkml/jasmine/data/ai/mcp/transport/StreamableHttpClientTransport.kt`
  - `app/src/main/java/com/lhzkml/jasmine/data/ai/mcp/transport/SseClientTransport.kt`
- 版本清单更新位置：`c:\jasmine\gradle\libs.versions.toml`（新增 `kotlin-sdk-client`、`kotlin-sdk-server`，移除 `kotlin-sdk` 汇总模块）
#### 3.3.3 潜在冲突

1. Kotlin：统一为 `2.2.10`（以 Koog 为准）。
2. Ktor：统一为 `3.2.2`（以 Koog 为准）。
3. kotlinx-serialization：统一为 `1.8.1`（以 Koog 为准）。
4. kotlinx-datetime：统一为 `0.6.2`（以 Koog 为准）。
5. MCP SDK：统一为 `0.8.0`（以 Koog 为准），避免工具注册/调用 API 差异。
6. OkHttp 与 SSE：AI 模块全面弃用 OkHttp SSE，HTTP 客户端统一采用 Ktor；旧流式实现全部移除。

注意：全局采用新框架（Koog/Ktor），所有版本与实现以 Koog 为准，不保留旧实现或混用策略。



#### 4.1.1 添加 Koog 依赖
 - 在版本清单中加入 `koog` 版本与所需模块（此处不展示具体依赖代码）。



### 6.1 Jasmine 独有功能（需特殊处理）

| 功能 | Jasmine 实现 | Koog 支持 | 处理方案 |
|-----|-------------|----------|---------|
| **Vertex AI** | `GoogleProvider` 支持 Service Account 认证 | ⚠️ 部分支持 | 统一迁移至 Koog/Ktor，实现服务账号鉴权与流式处理，删除旧实现 |
| **余额查询** | `Provider.getBalance()` | ❌ 未内置 | 新增 Koog 扩展或独立服务，移除旧接口 |
| **自定义 Headers** | `TextGenerationParams.customHeaders` | ⚠️ 部分支持 | 在适配层通过 Ktor `defaultRequest` 统一注入 |
| **自定义 Body** | `TextGenerationParams.customBody` | ⚠️ 部分支持 | 在适配层进行 JSON 合并注入，遵循现有合并语义 |
| **Key 轮询** | `KeyRoulette` 的 API Key 轮询 | ❌ 未内置 | 在适配层实现请求级轮询，移除旧实现 |
| **Reasoning Metadata** | `UIMessagePart.Reasoning.metadata` 存储 `signature` | ⚠️ 部分支持 | 适配层映射到 Koog `thinking`/`signature` 字段 |
| **Response API** | OpenAI `useResponseApi` 开关 | ✅ 支持 | 使用 Koog `OpenAIResponsesParams` 统一实现 |
| **内置工具** | Google `google_search`, `url_context` | ⚠️ 部分支持 | 统一迁移到 Koog Tool/MCP，按需补齐支持 |

补充：Key 轮询策略为按请求级轮询，不在流式连接过程中切换 Key；统一由适配层管理。
