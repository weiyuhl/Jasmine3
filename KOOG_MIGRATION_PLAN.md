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

