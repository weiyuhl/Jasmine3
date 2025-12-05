# Jasmine 与 Koog 供应商对照表

| 供应商 | Jasmine 支持 | Koog 支持 | 重复 | 说明 |
|---|---|---|---|---|
| OpenAI | 支持（`ProviderSetting.OpenAI`） | 支持（`LLMProvider.OpenAI`） | ✓ | Jasmine 可通过自定义 `baseUrl` 兼容 OpenAI API 的第三方；Koog 也支持自定义 `baseUrl`。
| Google (Gemini/Vertex AI) | 支持（`ProviderSetting.Google`，含 Vertex AI） | 支持（`LLMProvider.Google`） | ✓ | 两侧均支持 Gemini；Jasmine 额外含服务账号（Vertex AI）。
| Anthropic (Claude) | 支持（`ProviderSetting.Claude`） | 支持（`LLMProvider.Anthropic`） | ✓ | 两侧均支持 Claude 与流式输出/工具调用。
| DeepSeek | 通过 OpenAI 兼容接口（自定义 `baseUrl`） | 支持（`LLMProvider.DeepSeek` 专用客户端） | 部分重复 | Jasmine 以 OpenAI 兼容方式支持；Koog 提供专用 DeepSeek 客户端与能力枚举。
| OpenRouter | 通过 OpenAI 兼容接口（自定义 `baseUrl`） | 支持（`LLMProvider.OpenRouter`） | 部分重复 | Jasmine可用 OpenAI 兼容；Koog 提供 OpenRouter 客户端，统一访问多家模型。
| Mistral AI | 通过 OpenAI 兼容接口（自定义 `baseUrl`） | 支持（`LLMProvider.MistralAI`） | 部分重复 | Jasmine可用 OpenAI 兼容；Koog 有专用 Mistral 客户端与模型枚举。
| Alibaba (DashScope/Qwen) | 通过 OpenAI 兼容接口（自定义 `baseUrl`） | 支持（`LLMProvider.Alibaba`） | 部分重复 | Jasmine可用 OpenAI 兼容；Koog 提供阿里专用客户端与能力枚举。
| Meta | 通过 OpenAI 兼容接口（自定义 `baseUrl`） | 支持（`LLMProvider.Meta`） | 部分重复 | Jasmine可用 OpenAI 兼容；Koog 有统一 Provider 枚举与模型能力。
| AWS Bedrock | 未内置 | 支持（`LLMProvider.Bedrock`） | ✗ | Koog 原生支持 Bedrock；Jasmine 当前未内置此 Provider。
| Ollama（本地模型） | 未内置 | 支持（`LLMProvider.Ollama`） | ✗ | Koog 支持本地推理；Jasmine 当前未内置此 Provider。

备注：
- Jasmine 已内置的供应商类型为 OpenAI / Google / Claude；其他第三方常见平台（如 SiliconFlow、DeepSeek、Mistral、Qwen、OpenRouter 等）通常通过 OpenAI 兼容接口以自定义 `baseUrl` 方式接入。
- Koog 提供多个专用客户端与 `LLMProvider` 枚举，除 OpenAI/Google/Anthropic 外，还包含 DeepSeek、OpenRouter、Mistral、Alibaba、Bedrock、Ollama、Meta 等。

