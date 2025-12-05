# Jasmine 与 Koog 供应商对照表

| 供应商 | Jasmine 支持 | Koog 支持 | 重复 | 说明 |
|---|---|---|---|---|
| OpenAI | 支持（`ProviderSetting.OpenAI`，内置） | 支持（`LLMProvider.OpenAI`） | ✓ | 两侧原生支持；Koog/OpenAI 客户端支持自定义 `baseUrl`。
| Google (Gemini) | 支持（`ProviderSetting.Google`，内置） | 支持（`LLMProvider.Google`） | ✓ | 两侧支持 Gemini；Jasmine 另含 Vertex/服务账号能力。
| Anthropic (Claude) | 支持（`ProviderSetting.Claude`，类型存在） | 支持（`LLMProvider.Anthropic`） | ✓ | Jasmine 存在 Claude 类型（可导入）；Koog 有专用客户端。
| 硅基流动 (SiliconFlow) | 支持（内置，`ProviderSetting.OpenAI` + `baseUrl`） | 支持（用 `OpenAI` 客户端自定义 `baseUrl`） | ✓ | OpenAI 兼容；余额查询已适配于 Jasmine。
| DeepSeek | 支持（内置，`ProviderSetting.OpenAI` + `baseUrl`） | 支持（`LLMProvider.DeepSeek` 专用客户端） | ✓ | Jasmine 走 OpenAI 兼容；Koog 有专用 DeepSeek 客户端。
| OpenRouter | 支持（内置，`ProviderSetting.OpenAI` + `baseUrl`） | 支持（`LLMProvider.OpenRouter` 专用客户端） | ✓ | 两侧均支持；Koog 客户端统一访问多家模型。
| 阿里云百炼 (DashScope) | 支持（内置，`ProviderSetting.OpenAI` + `baseUrl`） | 支持（`LLMProvider.Alibaba` 专用客户端） | ✓ | Jasmine 走兼容模式；Koog 提供 DashScope 客户端。
| 月之暗面 (Moonshot) | 支持（内置，`ProviderSetting.OpenAI` + `baseUrl`） | 支持（用 `OpenAI` 客户端自定义 `baseUrl`） | ✓ | OpenAI 兼容接入。
| 智谱AI开放平台 (GLM) | 支持（内置，`ProviderSetting.OpenAI` + `baseUrl`） | 支持（用 `OpenAI` 客户端自定义 `baseUrl`） | ✓ | OpenAI 兼容接入。
| 腾讯 Hunyuan | 支持（内置，`ProviderSetting.OpenAI` + `baseUrl`） | 支持（用 `OpenAI` 客户端自定义 `baseUrl`） | ✓ | OpenAI 兼容接入。
| xAI | 支持（内置，`ProviderSetting.OpenAI` + `baseUrl`） | 支持（用 `OpenAI` 客户端自定义 `baseUrl`） | ✓ | OpenAI 兼容接入。
| Mistral AI | 未内置 | 支持（`LLMProvider.MistralAI` 专用客户端） | ✗ | Jasmine 默认无此供应商；Koog 有专用客户端与模型枚举。
| AWS Bedrock | 未内置 | 支持（`LLMProvider.Bedrock`） | ✗ | Koog 原生支持 Bedrock（多模型家族）；Jasmine 未内置。
| Ollama（本地模型） | 未内置 | 支持（`LLMProvider.Ollama`） | ✗ | Koog 支持本地推理；Jasmine 未内置。
| Meta | 未内置 | 枚举存在（`LLMProvider.Meta`），未见独立客户端 | ✗ | Koog 枚举包含 Meta；当前未检索到独立客户端模块。

备注：
- Jasmine 供应商类型仅为 `OpenAI` / `Google` / `Claude`（类型存在）；其内置第三方供应商均通过 OpenAI 兼容接口以自定义 `baseUrl` 接入。
- Koog 的 `LLMProvider` 枚举包含 `OpenAI`、`Google`、`Anthropic`、`DeepSeek`、`OpenRouter`、`MistralAI`、`Alibaba`、`Bedrock`、`Ollama`、`Meta`；其中多数有对应专用客户端模块。

## Jasmine 内置供应商清单

| 名称 | 类型 | `baseUrl` | 默认启用 | 余额查询 |
|---|---|---|---|---|
| OpenAI | `ProviderSetting.OpenAI` | `https://api.openai.com/v1` | 是 | 否 |
| Gemini | `ProviderSetting.Google` | `https://generativelanguage.googleapis.com/v1beta` | 是 | 否 |
| 硅基流动 | `ProviderSetting.OpenAI` | `https://api.siliconflow.cn/v1` | 是 | 是 |
| DeepSeek | `ProviderSetting.OpenAI` | `https://api.deepseek.com/v1` | 是 | 是 |
| OpenRouter | `ProviderSetting.OpenAI` | `https://openrouter.ai/api/v1` | 是 | 是 |
| 阿里云百炼 | `ProviderSetting.OpenAI` | `https://dashscope.aliyuncs.com/compatible-mode/v1` | 否 | 否 |
| 月之暗面 | `ProviderSetting.OpenAI` | `https://api.moonshot.cn/v1` | 否 | 是 |
| 智谱AI开放平台 | `ProviderSetting.OpenAI` | `https://open.bigmodel.cn/api/paas/v4` | 否 | 否 |
| 腾讯 Hunyuan | `ProviderSetting.OpenAI` | `https://api.hunyuan.cloud.tencent.com/v1` | 否 | 否 |

| xAI | `ProviderSetting.OpenAI` | `https://api.x.ai/v1` | 否 | 否 |

参考来源：`app/src/main/java/com/lhzkml/jasmine/data/datastore/DefaultProviders.kt`

补充：Jasmine 存在 `ProviderSetting.Claude` 类型（默认不内置），可导入配置，默认 `baseUrl` 为 `https://api.anthropic.com/v1`。

## Koog 供应商清单

| 枚举 | 客户端模块 | 说明 |
|---|---|---|
| `OpenAI` | `prompt-executor-openai-client` | OpenAI 及兼容 `baseUrl`。
| `Google` | `prompt-executor-google-client` | Gemini(Generate/Stream)。
| `Anthropic` | `prompt-executor-anthropic-client` | Claude(消息/流式/工具)。
| `DeepSeek` | `prompt-executor-deepseek-client` | DeepSeek 专用。
| `OpenRouter` | `prompt-executor-openrouter-client` | OpenRouter 专用。
| `MistralAI` | `prompt-executor-mistralai-client` | Mistral 专用。
| `Alibaba` | `prompt-executor-dashscope-client` | 阿里 DashScope（OpenAI 兼容端点）。
| `Bedrock` | `prompt-executor-bedrock-client` | AWS Bedrock（JVM）。
| `Ollama` | `prompt-executor-ollama-client` | 本地模型。
| `Meta` | 暂无独立客户端 | 枚举存在，未检索到专用客户端。

参考来源：`prompt/prompt-llm/src/commonMain/kotlin/ai/koog/prompt/llm/LLMProvider.kt` 与各 `prompt-executor-*-client` 模块。

## UI 图标覆盖（参考）

Jasmine 资源目录包含以下供应商图标（用于 UI 展示，非等同于内置供应商）：`app/src/main/assets/icons`：

`alibabacloud-color.svg`, `anthropic.svg`, `bing.png`, `brave.svg`, `cerebras-color.svg`, `claude-color.svg`, `cloudflare-color.svg`, `cohere-color.svg`, `deepseek-color.svg`, `exa.png`, `firecrawl.svg`, `gemini-color.svg`, `gemma-color.svg`, `github.svg`, `google-color.svg`, `grok.svg`, `groq.svg`, `hunyuan-color.svg`, `internlm-color.svg`, `jina.svg`, `kimi-color.svg`, `ling.png`, `linkup.png`, `meta-color.svg`, `metaso.svg`, `minimax-color.svg`, `mistral-color.svg`, `moonshot.svg`, `nvidia-color.svg`, `ollama.svg`, `openai.svg`, `openrouter.svg`, `perplexity-color.svg`, `ppio-color.svg`, `qwen-color.svg`, `siliconflow.svg`, `tavern.png`, `tavily.png`, `vercel.svg`, `xai.svg`, `zhipu-color.svg`。

## Jasmine 模型连接依赖清单（记录）

- 网络层（HTTP 客户端）
  - `com.squareup.okhttp3:okhttp`
  - `com.squareup.okhttp3:okhttp-sse`
  - `com.squareup.okhttp3:logging-interceptor`
- JSON 序列化
  - `org.jetbrains.kotlinx:kotlinx-serialization-json`
- 并发与时间
  - `org.jetbrains.kotlinx:kotlinx-coroutines-core`
  - `org.jetbrains.kotlinx:kotlinx-datetime`
- 日志
  - Android 平台日志：`android.util.Log`
  - 可选网络日志：`com.squareup.okhttp3:logging-interceptor`
- 统一封装
  - 内部封装：`com.lhzkmlcommon:http`（对 OkHttp/SSE 的扩展）
- 其他网络栈（非模型主链路）
  - `com.squareup.retrofit2:retrofit`
  - `com.squareup.retrofit2:converter-kotlinx-serialization`
  - `io.ktor:ktor-client-core`
  - `io.ktor:ktor-client-okhttp`
  - `io.ktor:ktor-client-content-negotiation`
  - `io.ktor:ktor-serialization-kotlinx-json`

## Koog 模型连接依赖清单（记录）

- 网络层（HTTP 客户端）
  - `io.ktor:ktor-client-core`
  - `io.ktor:ktor-client-content-negotiation`
  - `io.ktor:ktor-serialization-kotlinx-json`
  - `io.ktor:ktor-client-sse`
- JSON 序列化
  - `org.jetbrains.kotlinx:kotlinx-serialization-json`
- 并发与时间
  - `org.jetbrains.kotlinx:kotlinx-coroutines-core`
  - `org.jetbrains.kotlinx:kotlinx-datetime`
- 日志
  - `io.github.oshai:kotlin-logging`
  - `ch.qos.logback:logback-classic` 或 `org.slf4j:slf4j-simple`
- AWS Bedrock（仅 JVM）
  - `aws.sdk.kotlin:bedrockruntime`
  - 按场景：`aws.sdk.kotlin:bedrock`、`aws.sdk.kotlin:sts`
- 传输实现（可选）
  - 默认：`http-client-ktor`
  - 可选：`http-client-okhttp`（搭配 `com.squareup.okhttp3:okhttp`、`com.squareup.okhttp3:okhttp-sse`）
- 引擎/平台支持（按目标）
  - `io.ktor:ktor-client-cio`（JVM）
  - `io.ktor:ktor-client-darwin`（Apple）
  - `io.ktor:ktor-client-js`（JS）
- 客户端实现中的插件使用（示例）
  - 安装 `SSE` 与 `ContentNegotiation(json)`（用于流式与序列化）
- 统一封装
  - 客户端普遍通过 `KoogHttpClient` 封装 Ktor 客户端，用于设置 `baseUrl`、`SSE`、超时、序列化等。

## 迁移方案：Jasmine 模型连接依赖切换为 Koog 同款（记录）

- 目标与原则
  - 引入 Koog 同款 Ktor 客户端栈（`ktor-client-core`、`content-negotiation`、`serialization-kotlinx-json`、`client-sse`），与现有 OkHttp 并存。
  - 保持现有业务边界不变：继续复用 `ProviderSetting`、`API Key`、`baseUrl`、`自定义 Header/Body` 等配置结构。
  - 避免一次性大手术：在既有 Provider 实现内新增 Ktor 并存实现，不改 UI/数据结构。

- 依赖调整
  - 不需要不同连接方式的开关（无需切换开关）。
  - 不进行全量切换：不把全部模型供应商一次性改为新连接方式；DeepSeek 的单独接入方案在文末另述。
  - 并存引入：`io.ktor:ktor-client-core`、`io.ktor:ktor-client-content-negotiation`、`io.ktor:ktor-serialization-kotlinx-json`、`io.ktor:ktor-client-sse`。
  - Android 引擎可用：`io.ktor:ktor-client-okhttp`。
  - 保留通用依赖：`kotlinx-serialization-json`、`kotlinx-coroutines-core`、`kotlinx-datetime`。
  - 保留现有：`okhttp`、`okhttp-sse`（与 Ktor 并存）。

- 客户端封装
  - 新建统一的 Ktor 客户端封装（安装 `SSE` 与 `ContentNegotiation(json)`，设置超时、代理、`baseUrl`），与现有 OkHttp 封装并存。
  - 提供方法：`buildRequest(baseUrl, path, headers)`、`postJson(body)`、`streamSse(url)` 等，供 Provider 复用。

- 接口改造（按 Provider 类型）
  - OpenAI 兼容类（含 SiliconFlow、DeepSeek、OpenRouter、DashScope、GLM、Hunyuan、xAI）：
    - 新增基于 Ktor 的并存实现（`ChatCompletions`/`Responses` 与流式 `SSE`），与现有 OkHttp 实现同时存在。
    - 继续支持 `useResponseApi`、`customHeaders`、`mergeCustomBody` 等现有参数逻辑。
  - Google（Gemini/Vertex）：
    - 新增 Ktor 并存实现（含 `alt=sse` 流式），服务账号令牌沿用现有获取逻辑或接入到 Ktor 请求。
  - Claude（Anthropic）：
    - 新增 Ktor 并存实现（请求与 `SSE` 读取），保持现有消息/工具字段映射与分段解析。

- 配置复用
  - 直接读取 `ProviderSetting` 中的 `baseUrl`、`apiKey`、`proxy`、`自定义 Header/Body` 等，不改 UI 与数据结构。
  - `KeyRoulette` 等密钥轮换策略保留，改为在 Ktor 请求组装阶段插入 `Authorization`。

- 错误处理与重试
  - Ktor 并存实现的异常与状态码处理对齐现有语义；`SSE` 流解析保持与当前 `MessageChunk` 一致。



## 渐进迁移试点：DeepSeek（仅方案）

- 范围
  - 仅对 DeepSeek 供应商的“模型连接依赖”进行替换尝试，其他供应商保持现状不动。
  - 现状：DeepSeek 走 `ProviderSetting.OpenAI + baseUrl=https://api.deepseek.com/v1`，实现复用 OpenAI ChatCompletions/Responses 的 OkHttp 路径。

- 目标
 

- 依赖策略
  - 两种接入方式同时存在：OkHttp（含 `okhttp-sse`）与 Ktor（含 `client-sse`）。
  - 保留通用依赖：`kotlinx-serialization-json`、`kotlinx-coroutines-core`、`kotlinx-datetime`。
  - Android 可使用 `ktor-client-okhttp` 引擎。

- 客户端封装
  - 新建 Ktor 客户端构建器（仿 Koog）：安装 `SSE` 与 `ContentNegotiation(json)`，设置超时、代理与 `baseUrl`。
  - 提供方法：`postJson(url, headers, body)`、`streamSse(url, headers, body)`，产出 `Flow<MessageChunk>`，对齐现有流式语义与结束帧（`[DONE]`）。

- 接口改造（仅 DeepSeek）
  - 非流式：使用 Ktor `POST`，与现有请求体字段一致（模型、消息、温度、topP、maxTokens、tools 等）。
  - 流式：使用 Ktor `client-sse` 读取增量，解析 delta/text/tool 调用，映射到 `MessageChunk`，沿用现有 UI 消息拼接逻辑。
  - 认证与自定义：从 `ProviderSetting` 读取 `apiKey`、`baseUrl`、`proxy`、`customHeaders/customBody`，并在 Ktor 请求阶段统一注入。

  你需要建立的todo（任务清单）：
  1、严格按照KOOG_MIGRATION_PLAN.md方案引入Ktor 依赖并保留OkHttp并存，
  2、约束：两种方式不做开关切换与全部供应商不全量切换
  3、DeepSeek流式改为Ktor（替换）
  4、DeepSeek 非流式改为Ktor（替换）
  5、严格全面重新检查一下是否有没有方案进行修改

 

 



