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

