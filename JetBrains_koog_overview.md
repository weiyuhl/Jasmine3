# JetBrains/koog 开源框架调研文档

## 概览

Koog 是一个基于 Kotlin 的多平台 AI Agent 框架（支持 JVM、JS、WasmJS、iOS、Android）。它提供：
- Agent 执行引擎（`AIAgent`、图策略与函数式策略）
- LLM 抽象与多提供商编排（`PromptExecutor`、`LLMClient`）
- 工具系统（`ToolRegistry` 与多种工具定义方式）
- 特性系统（事件、跟踪、记忆、持久化、OpenTelemetry 等）
- 企业框架集成（Spring Boot、Ktor）

当前版本：`0.5.2`（发布到 Maven Central，DeepWiki/README/构建脚本均有体现）。

参考：
- GitHub 仓库：https://github.com/JetBrains/koog
- DeepWiki 文档：https://deepwiki.com/JetBrains/koog
- 版本清单（Version Catalog）：`gradle/libs.versions.toml`

## 仓库结构与模块分组

- Agents 核心：`agents-core`、`agents-ext`、`agents-tools`、`agents-test`、`agents-utils`、`agents-mcp`
- Features 扩展：事件处理、记忆、跟踪、快照/持久化、OpenTelemetry、Tokenizer（`agents-features-*`）
- Prompt 层：`prompt-model`、`prompt-llm`、`prompt-executor-model`、`prompt-executor-clients`
- LLM 客户端：OpenAI、Anthropic、Google、AWS Bedrock、Ollama，以及其他兼容实现（OpenRouter、DeepSeek、Mistral、Dashscope）
- Supporting Systems：Embeddings、RAG、A2A（Agent-to-Agent SDK）
- 框架集成：`koog-ktor`、`koog-spring-boot-starter`
- 示例与文档：`examples/*`、`docs/*`

## 版本与依赖（摘自 `gradle/libs.versions.toml`）
[versions]
agp = "8.12.3" # Android Gradle 插件版本（Android 构建工具链）
annotations = "26.0.2" # JetBrains 注解库（org.jetbrains:annotations）
assertj = "3.27.4" # AssertJ 断言库（流式断言 API）
awaitility = "4.3.0" # Awaitility 异步等待测试库（便于测试异步条件）
aws-sdk-kotlin = "1.5.16" # AWS Kotlin SDK（Bedrock/STS 等）
dokka = "2.0.0" # Kotlin 文档生成器
exposed = "0.58.0" # JetBrains Exposed ORM（SQL 映射与查询）
h2 = "2.2.224" # H2 内存数据库驱动
hikaricp = "6.2.1" # HikariCP 数据库连接池
jdkVersion = "17" # JDK 版本（构建/运行环境约束）
jetsign = "45.47" # JetBrains JetSign 插件版本
junit = "5.8.2" # JUnit 5 测试框架
knit = "0.5.0" # Kotlinx Knit 文档示例编织工具
kotest = "6.0.4" # Kotest 测试工具集
kotlin = "2.2.10" # Kotlin 编译器与 BOM 版本
kotlinx-coroutines = "1.10.2" # Kotlin 协程库（核心/测试/Reactive 等）
kotlinx-datetime = "0.6.2" # 跨平台日期时间库
kotlinx-io = "0.7.0" # 跨平台 IO 库
kotlinx-serialization = "1.8.1" # Kotlin 序列化（IDE 校验：check with IJ）
kover = "0.9.2" # 覆盖率分析插件版本
ktlint = "13.1.0" # Kotlin 代码风格检查插件版本
ktor3 = "3.2.2" # Ktor 3.x（HTTP 客户端/服务端）
lettuce = "6.5.5.RELEASE" # Redis 客户端
logback = "1.5.13" # Logback 日志实现
mcp = "0.7.2" # Model Context Protocol（MCP）SDK 版本
mockito = "5.19.0" # Mockito Java Mock 框架
mockk = "1.13.8" # MockK Kotlin Mock 框架
mokksy = "0.5.0-Alpha3" # A2A 协议的模拟工具包
mysql = "8.0.33" # MySQL JDBC 驱动
netty = "4.2.6.Final" # Netty 网络框架版本
okhttp = "5.2.1" # OkHttp 客户端（含 BOM 与 SSE 扩展）
opentelemetry = "1.51.0" # OpenTelemetry BOM（统一遥测依赖）
oshai-logging = "7.0.7" # Kotlin Logging 封装
postgresql = "42.7.4" # PostgreSQL JDBC 驱动
slf4j = "2.0.17" # SLF4J 日志门面
spring-boot = "3.5.7" # Spring Boot 版本（及 BOM）
spring-management = "1.1.7" # Spring 依赖管理 Gradle 插件版本
sqlite = "3.46.1.3" # SQLite JDBC 驱动
testcontainers = "1.19.7" # Testcontainers 测试容器框架版本

[libraries]
jetbrains-annotations = { module = "org.jetbrains:annotations", version.ref = "annotations" } # JetBrains 注解库
mockito-junit-jupiter = { module = "org.mockito:mockito-junit-jupiter", version.ref = "mockito" } # Mockito 与 JUnit5 集成
assertj-core = { module = "org.assertj:assertj-core", version.ref = "assertj" } # AssertJ 断言库
awaitility = { module = "org.awaitility:awaitility-kotlin", version.ref = "awaitility" } # Awaitility（Kotlin 版）异步等待
junit-jupiter-params = { module = "org.junit.jupiter:junit-jupiter-params", version.ref = "junit" } # JUnit 5 参数化测试
junit-platform-launcher = { module = "org.junit.platform:junit-platform-launcher" } # JUnit 平台启动器
kotest-assertions-core = { module = "io.kotest:kotest-assertions-core", version.ref = "kotest" } # Kotest 断言核心
kotest-assertions-json = { module = "io.kotest:kotest-assertions-json", version.ref = "kotest" } # Kotest JSON 断言
kotlin-bom = { module = "org.jetbrains.kotlin:kotlin-bom", version.ref = "kotlin" } # Kotlin BOM
kotlin-gradle-plugin = { module = "org.jetbrains.kotlin:kotlin-gradle-plugin", version.ref = "kotlin" } # Kotlin Gradle 插件
kotlinx-coroutines-core = { module = "org.jetbrains.kotlinx:kotlinx-coroutines-core", version.ref = "kotlinx-coroutines" } # 协程核心
kotlinx-coroutines-jdk8 = { module = "org.jetbrains.kotlinx:kotlinx-coroutines-jdk8", version.ref = "kotlinx-coroutines" } # 协程 JDK8 适配
kotlinx-coroutines-reactive = { module = "org.jetbrains.kotlinx:kotlinx-coroutines-reactive", version.ref = "kotlinx-coroutines" } # 协程 Reactive 适配
kotlinx-coroutines-reactor = { module = "org.jetbrains.kotlinx:kotlinx-coroutines-reactor" } # 协程 Reactor 适配
kotlinx-coroutines-test = { module = "org.jetbrains.kotlinx:kotlinx-coroutines-test", version.ref = "kotlinx-coroutines" } # 协程测试
kotlinx-datetime = { module = "org.jetbrains.kotlinx:kotlinx-datetime", version.ref = "kotlinx-datetime" } # 跨平台日期时间
kotlinx-io-core = { module = "org.jetbrains.kotlinx:kotlinx-io-core", version.ref = "kotlinx-io" } # 跨平台 IO 核心
kotlinx-serialization-core = { module = "org.jetbrains.kotlinx:kotlinx-serialization-core", version.ref = "kotlinx-serialization" } # 序列化核心
kotlinx-serialization-json = { module = "org.jetbrains.kotlinx:kotlinx-serialization-json", version.ref = "kotlinx-serialization" } # Kotlinx JSON 序列化
ktor-client-cio = { module = "io.ktor:ktor-client-cio", version.ref = "ktor3" } # Ktor 客户端 CIO
ktor-client-js = { module = "io.ktor:ktor-client-js", version.ref = "ktor3" } # Ktor 客户端 JS
ktor-client-darwin = { module = "io.ktor:ktor-client-darwin", version.ref = "ktor3" } # Ktor 客户端 iOS（Darwin）
ktor-client-apache5 = { module = "io.ktor:ktor-client-apache5", version.ref = "ktor3" } # Ktor 客户端 Apache5
ktor-client-okhttp = { module = "io.ktor:ktor-client-okhttp", version.ref = "ktor3" } # Ktor 客户端 OkHttp 适配
ktor-client-core = { module = "io.ktor:ktor-client-core", version.ref = "ktor3" } # Ktor 客户端核心
ktor-client-content-negotiation = { module = "io.ktor:ktor-client-content-negotiation", version.ref = "ktor3" } # 客户端内容协商
ktor-client-logging = { module = "io.ktor:ktor-client-logging", version.ref = "ktor3" } # 客户端日志
ktor-client-mock = { module = "io.ktor:ktor-client-mock", version.ref = "ktor3" } # 客户端 Mock
ktor-client-sse = { module = "io.ktor:ktor-client-sse", version.ref = "ktor3" } # 客户端 SSE
ktor-serialization-kotlinx-json = { module = "io.ktor:ktor-serialization-kotlinx-json", version.ref = "ktor3" } # Ktor + Kotlinx JSON
ktor-server-core = { module = "io.ktor:ktor-server-core", version.ref = "ktor3" } # Ktor 服务端核心
ktor-server-cio = { module = "io.ktor:ktor-server-cio", version.ref = "ktor3" } # Ktor 服务端 CIO
ktor-server-netty = { module = "io.ktor:ktor-server-netty-jvm", version.ref = "ktor3" } # Ktor 服务端 Netty（JVM）
ktor-server-sse = { module = "io.ktor:ktor-server-sse", version.ref = "ktor3" } # Ktor 服务端 SSE
ktor-server-content-negotiation = { module = "io.ktor:ktor-server-content-negotiation", version.ref = "ktor3" } # 服务端内容协商
ktor-server-cors = { module = "io.ktor:ktor-server-cors", version.ref = "ktor3" } # 服务端 CORS
ktor-server-test-host = { module = "io.ktor:ktor-server-test-host", version.ref = "ktor3" } # 服务端测试宿主
lettuce-core = { module = "io.lettuce:lettuce-core", version.ref = "lettuce" } # Redis 客户端
logback-classic = { module = "ch.qos.logback:logback-classic", version.ref = "logback" } # Logback Classic
oshai-kotlin-logging = { module = "io.github.oshai:kotlin-logging", version.ref = "oshai-logging" } # Kotlin Logging 封装
mockk = { module = "io.mockk:mockk", version.ref = "mockk" } # MockK（Kotlin Mock 框架）
mokksy-a2a = { module = "me.kpavlov.aimocks:ai-mocks-a2a", version.ref = "mokksy" } # A2A 协议模拟库
dokka-gradle-plugin = { module = "org.jetbrains.dokka:dokka-gradle-plugin", version.ref = "dokka" } # Dokka Gradle 插件
mcp-client = { module = "io.modelcontextprotocol:kotlin-sdk-client", version.ref = "mcp" } # MCP 协议客户端
mcp-server = { module = "io.modelcontextprotocol:kotlin-sdk-server", version.ref = "mcp" } # MCP 协议服务器
slf4j-simple = { module = "org.slf4j:slf4j-simple", version.ref = "slf4j" } # SLF4J 简单实现
jetsign-gradle-plugin = { module = "com.jetbrains:jet-sign", version.ref = "jetsign" } # JetSign Gradle 插件
testcontainers = { module = "org.testcontainers:testcontainers", version.ref = "testcontainers" } # Testcontainers 核心
testcontainers-junit = { module = "org.testcontainers:junit-jupiter", version.ref = "testcontainers" } # Testcontainers JUnit 集成
testcontainers-postgresql = { module = "org.testcontainers:postgresql", version.ref = "testcontainers" } # Testcontainers PostgreSQL 模块
testcontainers-mysql = { module = "org.testcontainers:mysql", version.ref = "testcontainers" } # Testcontainers MySQL 模块
exposed-core = { module = "org.jetbrains.exposed:exposed-core", version.ref = "exposed" } # Exposed 核心
exposed-dao = { module = "org.jetbrains.exposed:exposed-dao", version.ref = "exposed" } # Exposed DAO
exposed-jdbc = { module = "org.jetbrains.exposed:exposed-jdbc", version.ref = "exposed" } # Exposed JDBC
exposed-json = { module = "org.jetbrains.exposed:exposed-json", version.ref = "exposed" } # Exposed JSON 支持
exposed-kotlin-datetime = { module = "org.jetbrains.exposed:exposed-kotlin-datetime", version.ref = "exposed" } # Exposed Kotlin 日期时间扩展
hikaricp = { module = "com.zaxxer:HikariCP", version.ref = "hikaricp" } # HikariCP 数据库连接池
postgresql = { module = "org.postgresql:postgresql", version.ref = "postgresql" } # PostgreSQL JDBC 驱动
mysql = { module = "com.mysql:mysql-connector-j", version.ref = "mysql" } # MySQL JDBC 驱动
h2 = { module = "com.h2database:h2", version.ref = "h2" } # H2 数据库驱动
sqlite = { module = "org.xerial:sqlite-jdbc", version.ref = "sqlite" } # SQLite JDBC 驱动
okhttp-bom = { module = "com.squareup.okhttp3:okhttp-bom", version.ref = "okhttp" } # OkHttp BOM（版本对齐）
okhttp = { module = "com.squareup.okhttp3:okhttp" } # OkHttp 客户端
okhttp-sse = { module = "com.squareup.okhttp3:okhttp-sse" } # OkHttp SSE 扩展
opentelemetry-bom = { module = "io.opentelemetry:opentelemetry-bom", version.ref = "opentelemetry" } # OpenTelemetry BOM
opentelemetry-sdk = { module = "io.opentelemetry:opentelemetry-sdk" } # OpenTelemetry SDK
opentelemetry-exporter-otlp = { module = "io.opentelemetry:opentelemetry-exporter-otlp" } # OTLP 导出器
opentelemetry-exporter-logging = { module = "io.opentelemetry:opentelemetry-exporter-logging" } # Logging 导出器
aws-sdk-kotlin-bedrock = { module = "aws.sdk.kotlin:bedrock", version.ref = "aws-sdk-kotlin" } # AWS Bedrock SDK
aws-sdk-kotlin-bedrockruntime = { module = "aws.sdk.kotlin:bedrockruntime", version.ref = "aws-sdk-kotlin" } # AWS Bedrock Runtime
aws-sdk-kotlin-sts = { module = "aws.sdk.kotlin:sts", version.ref = "aws-sdk-kotlin" } # AWS STS（安全令牌服务）
android-tools-gradle = { module = "com.android.tools.build:gradle", version.ref = "agp" } # Android 构建工具 Gradle 插件

# Spring
spring-boot-bom = { module = "org.springframework.boot:spring-boot-dependencies", version.ref = "spring-boot" } # Spring Boot 依赖管理 BOM（统一 Spring 生态版本）
spring-boot-starter = { module = "org.springframework.boot:spring-boot-starter" } # Spring Boot 基础启动器（常用日志/配置等聚合）
spring-boot-starter-test = { module = "org.springframework.boot:spring-boot-starter-test" } # Spring Boot 测试启动器（JUnit/Mockito 等）
jackson-module-kotlin = { module = "com.fasterxml.jackson.module:jackson-module-kotlin" } # Jackson Kotlin 模块（JSON 序列化/反序列化）
reactor-kotlin-extensions = { module = "io.projectreactor.kotlin:reactor-kotlin-extensions" } # Reactor Kotlin 扩展（响应式支持与协程互操作）

[bundles]
spring-boot-core = [ # Spring Boot 核心依赖包组
    "spring-boot-starter", # 基础启动器（Web/日志等常用依赖聚合）
    "jackson-module-kotlin", # Jackson Kotlin 模块（JSON 序列化/反序列化）
    "kotlinx-coroutines-reactor" # 协程与 Reactor 互操作（响应式编程支持）
]

[plugins]
android-application = { id = "com.android.application", version.ref = "agp" } # Android 应用构建插件
knit = { id = "org.jetbrains.kotlinx.knit", version.ref = "knit" } # 文档示例编织工具（将代码片段编织进文档）
kotlin-serialization = { id = "org.jetbrains.kotlin.plugin.serialization", version.ref = "kotlin" } # Kotlin 序列化插件
kotlin-spring = { id = "org.jetbrains.kotlin.plugin.spring", version.ref = "kotlin" } # Kotlin Spring 插件（简化 Spring 相关开发）
kotlinx-kover = { id = "org.jetbrains.kotlinx.kover", version.ref = "kover" } # 覆盖率统计插件
ktlint = { id = "org.jlleitschuh.gradle.ktlint", version.ref = "ktlint" } # Kotlin 代码风格检查插件
spring-management = { id = "io.spring.dependency-management", version.ref = "spring-management" } # Spring 依赖管理插件（BOM 对齐）


## 核心架构组件

- `AIAgent<I, O>`：Agent 入口类，管理生命周期（未开始/运行中/结束/失败），提供 `run` 与 `runStreaming`，支持安装特性与工具。
- `AIAgentGraphPipeline`：事件总线（拦截器机制），为 Agent、策略、节点、LLM 调用、工具调用等阶段提供 hook。
- 图策略（`AIAgentGraphStrategy`）：将工作流建模为节点与边，负责遍历与条件流转；内置策略涵盖单次执行、ReAct、工具循环等。

## Prompt 执行层

- `PromptExecutor`：抽象执行器，支持标准执行、流式输出、结构化输出。
- 实现：
  - `SingleLLMPromptExecutor`：绑定单一 `LLMClient`
  - `DefaultMultiLLMPromptExecutor`：多提供商编排，可无缝切换并支持历史重写
  - `CachedPromptExecutor`：加入缓存层
- `LLMClient`：面向具体提供商的调用接口（标准/流式/多候选、审核、Embedding 等）。
- `AbstractOpenAILLMClient`：抽象基类，适配 OpenAI 风格协议，扩展到 OpenRouter、DeepSeek 等。

## 流式与结构化输出（Streaming / Structured Output）

- Streaming API（`PromptExecutor.executeStreaming` → `Flow<StreamFrame>`）
  - 帧类型：`Append`（文本分片）、`ToolCall`（工具调用，含 `id/name/args`）、`End`（结束，含 `finishReason/ResponseMetaInfo`）。
  - 工具调用流：`StreamFrameFlowBuilder` 负责组装分片，仅在完成时发出；OpenAI Chat Completions 从 `delta.toolCalls` 累积；Responses API 将 `ResponseOutputItemDone(FunctionToolCall)` 转为 `ToolCall` 帧。
  - 并行工具：`structuredOutputWithToolsStrategy` 支持 `parallelTools = true`，由 `nodeExecuteMultipleTools(parallelTools)` 并发执行工具。
  - 历史重写：`DefaultMultiLLMPromptExecutor` 在切换 LLM 时自动重写会话历史，保证跨提供商一致性。
- Structured Output（`PromptExecutor.executeWithStructuredOutput<T>`）
  - 配置：`StructuredOutputConfig<T>`（含 `default` & `byProvider`、`fixingParser`、`updatePrompt` 手动/原生模式）。
  - JSON 结构：`JsonStructuredData<T>` 定义目标 JSON Schema；类型到 Schema 的映射见 `toJsonSchema()`（Bool/Array/Object 等）。
  - Provider 支持：OpenAI/Google 原生结构化输出；各客户端在初始化时注册各自的 JSON Schema 生成器。


## 工具系统

- `ToolRegistry` DSL：集中注册工具（单个、工具集合、简单函数式）。
- 工具定义三种方式：
  - 注解式（`@Tool` + `ToolSet` 方法，反射发现）
  - 类式（实现 `Tool<TArgs, TResult>`）
  - 简单/函数式（`simpleTool` 构建器）
- `ToolDescriptor`：工具元数据（名称、描述、参数结构），便于 LLM 理解与调用；注解式工具的参数 JSON Schema 可通过反射自动生成。

## 特性系统

- `AIAgentFeature<TConfig>`：通过在 `AIAgentGraphPipeline` 注册拦截器扩展能力。
- 典型特性：
  - 事件处理（生命周期回调）
  - 跟踪（输出到文件/远端/日志）
  - 记忆（概念/事实存储，可加密）
  - 持久化（状态检查点与回滚）
  - OpenTelemetry（OTLP Span 导出）
  - Tokenizer（消息 Token 计数）

## LLM 提供商与编排

- 支持的提供商：OpenAI、Anthropic、Google、AWS Bedrock、Ollama、OpenRouter、DeepSeek、Mistral、Dashscope 等。
- 多提供商编排：在多 `LLMClient` 间切换，保证灵活性与可靠性；可进行历史重写以适配不同上下文格式。

## MCP 集成（Model Context Protocol）

- 模块：`agents-mcp`
- 依赖：`io.modelcontextprotocol:kotlin-sdk-client/server`
- 作用：在工具与上下文层面与 MCP 生态互操作，提升协议化集成能力（工具暴露、上下文访问、跨系统协作）。

## 框架集成

- Spring Boot：`koog-spring-boot-starter`
  - 自动配置 Agent 能力；结合 `spring-boot-bom` 管理依赖；协同 `kotlinx-coroutines-reactor` 适配响应式。
- Ktor：`koog-ktor`
  - 服务端插件；客户端用于 LLM 或外部工具调用；结合 `ktor-serialization-kotlinx-json` 与 `OpenTelemetry`。

## Agent-to-Agent（A2A）协议

- 模块
  - `a2a-server`：将 Agent 暴露为 A2A 服务器，处理请求、执行逻辑、任务生命周期管理与实时流式响应。
  - `a2a-client`：作为 A2A 客户端连接其他 Agent，进行发现、消息交换、任务管理与流式响应。
  - `agents-features-a2a-server`：在 Agent 中安装服务器能力；提供 `RequestContext` 与 `SessionEventProcessor`。
  - `agents-features-a2a-client`：在 Agent 中安装客户端能力；访问注册的 `A2AClient` 实例。
- 消息格式与流程
  - 消息包含 `TextPart` 等分片，类如 `A2AMessage(messageId, role, parts)`；客户端使用 `MessageSendParams`。
  - 典型流程：
    - 暴露为 A2A 服务器：定义 `AgentCard` 能力与元数据，编写 `AgentExecutor` 处理业务；节点中通过 `withA2AAgentServer` 发送任务事件。
    - 作为 A2A 客户端：使用便捷节点 `nodeA2AClientGetAgentCard`、`nodeA2AClientSendMessageStreaming`、`nodeA2AClientSendMessage` 与远程 Agent 交互。
  - 委派与进度：通过 `TaskState.Submitted/Working/Complete` 等状态回传进度。

## RAG 与存储

- Embeddings：向量生成与抽象；`LLMEmbeddingProvider` 等。
- 向量存储：`vector-storage` 与 RAG 相关模块，用于文档检索与排名。
- SQL 与文件系统持久化：提供 `FileSystemProvider` 与多种 SQL 持久化实现。

## 可观测性

- Tracing 与 Debug：文件/远端/日志输出。
- OpenTelemetry：通过 SDK 与 OTLP/Logging Exporter 输出 Span。
- Langfuse、Weave：更深入的运行观测与分析整合。

### OpenTelemetry 采集属性（示例）
- 通用：`gen_ai.operation.name`、`gen_ai.system`、`gen_ai.agent.id`、`gen_ai.conversation.id`、`gen_ai.request.model`、`error.type`
- Koog 特有：`koog.agent.strategy.name`、`koog.node.name`

## 构建、版本与发布

- Gradle Kotlin DSL 构建。
- 版本策略：依据分支与环境变量计算（稳定版/开发版/Nightly/特性分支/本地 SNAPSHOT）。
- 发布：打包并经 Sonatype Central Portal 发布到 Maven Central（自动/手动模式根据分支）。
- `buildSrc`：自定义插件（多平台、Maven 发布、Split Package 检测等）。

## 多平台支持

- 目标平台：JVM、JS、WasmJS、iOS、Android。
- Ktor 客户端：`cio`、`js`、`darwin`、`apache5`；服务端：`netty-jvm`、`cio`。

## 测试与示例

- 测试：单测、集成测试、`MockLLMBuilder` 用于可控模拟。
- 示例：基础 Agent、复杂工作流、银行助手、多模态应用、Compose Multiplatform Demo。

## 快速开始（示意）

> 以下为概念性示例，展示如何在 JVM 项目内定义 Agent、注册工具并调用 LLM。实际依赖请参考 `ai.koog` 组坐标与 Version Catalog。

```kotlin
// 构建依赖（概念示意，不是完整 Gradle 脚本）：
// implementation("ai.koog:agents-core:0.5.2")
// implementation("ai.koog:prompt-executor-model:0.5.2")
// implementation("ai.koog:prompt-executor-openai-client:0.5.2")
// implementation(libs.ktor.client.core)
// implementation(libs.kotlinx.serialization.json)

import ai.koog.agents.core.agent.AIAgent
import ai.koog.agents.tools.ToolRegistry
import ai.koog.prompt.executor.SingleLLMPromptExecutor
import ai.koog.prompt.llm.LLMClient

// 1) 定义工具
val toolRegistry = ToolRegistry {
    simpleTool("calculateSum", "Sums numbers") { args: List<Int> ->
        args.sum()
    }
}

// 2) 配置 LLM 客户端（示意）
val llmClient: LLMClient = /* OpenAI/Anthropic/Bedrock/Ollama 等具体实现 */
val executor = SingleLLMPromptExecutor(llmClient)

// 3) 创建 Agent 并执行
val agent = AIAgent(toolRegistry = toolRegistry) {
    // 安装特性，如 Tracing、EventHandler、Persistence 等
}

// 4) 执行（非流式/流式）
val result = agent.run("hello")
// agent.runStreaming("hello") // 返回 Flow<StreamFrame>
```

## 选型与最佳实践建议

- 统一版本管理：使用 Version Catalog（`libs.versions.toml`）与 BOM（如 Spring/OpenTelemetry）。
- LLM 编排：在多提供商下使用 `DefaultMultiLLMPromptExecutor` + 历史重写，提升稳健性与性价比。
- 可观测性：启用 `Tracing` 与 `OpenTelemetry`，在生产环境接入 OTLP（如 Tempo/Jaeger）。
- 持久化与记忆：选择合适的 SQL Provider 与加密方案，控制数据生命周期与合规。
- MCP：在工具互操作与跨系统上下文需求下使用 MCP Client/Server，规范化接口协议。

## 参考与入口

- DeepWiki 总览与子章节：https://deepwiki.com/JetBrains/koog
- GitHub 仓库：https://github.com/JetBrains/koog
- 版本清单（Version Catalog）：https://github.com/JetBrains/koog/blob/main/gradle/libs.versions.toml
- 文档目录与示例：仓库 `docs/` 与 `examples/`

## 功能清单（DeepWiki 汇总）

- Agents
  - 核心：`AIAgent` 生命周期与执行、`AIAgentGraphPipeline` 事件拦截，支持图策略与函数式策略。
  - 扩展：`agents-ext` 提供 `subgraphWithTask`、`subtask` 等复杂工作流能力。
  - 工具系统：`agents-tools` 支持注解式、类式、Lambda 式工具；统一注册于 `ToolRegistry`。
  - MCP 集成：`agents-mcp` 与 Model Context Protocol 协调模型与工具管理。
- Features（特性系统）
  - EventHandler：生命周期回调（Agent/策略/节点/LLM/工具阶段）。
  - Memory：可插拔记忆（概念/事实存储，支持加密）。
  - Trace：本地/远端/日志的调试与运行轨迹输出。
  - Snapshot/Persistence：状态检查点与回滚（持久化 Agent 状态）。
  - OpenTelemetry：OTLP Span 导出与统一链路追踪。
  - Tokenizer：消息 Token 统计与控制。
- Prompt 层
  - Model：`Prompt`、`Message` 等结构定义。
  - LLM：`LLModel`、`LLMCapability` 抽象能力模型。
  - Executor：`PromptExecutor` 接口；实现含 `SingleLLMPromptExecutor`、`DefaultMultiLLMPromptExecutor`、`CachedPromptExecutor`。
  - Clients：`LLMClient` 接口为各提供商适配（标准/流式/多候选、审核、Embedding）。
- LLM 提供商
  - OpenAI、Anthropic、Google、AWS Bedrock、Ollama、OpenRouter、DeepSeek、Mistral、Dashscope、Azure OpenAI 等。
- A2A（Agent-to-Agent）协议
  - 服务端：`a2a-server` 处理 A2A 请求。
  - 客户端：`a2a-client` 发起 A2A 请求。
  - 集成：`agents-features-a2a-*` 模块与 Agent 流程整合。
- 后端集成
  - Spring Boot Starter：自动配置 LLM/Agent Bean 与依赖，快速落地 JVM 企业应用。
  - Ktor 插件：在 Ktor 应用内注册并运行 Agent，含服务端/客户端支持。
- 存储与 RAG
  - 文件系统：`FileSystemProvider` 抽象与实现。
  - 向量与 Embeddings：`embeddings-*` 提供 RAG 所需向量生成，支持本地与 OpenAI。
  - 向量存储：`rag-base`、`vector-storage` 提供文档向量存储（内存/本地文件）。
  - SQL 持久化：提供 Postgres 等 Provider 保存 Agent 状态与制品。
- 可观测性
  - Tracing/Debugging：全面的调试轨迹输出。
  - OpenTelemetry：原生集成统一链路追踪。
  - Langfuse：Span 适配与图可视化。
  - Weave：W&B Weave 集成（含 OpenTelemetry）。
- 高级特性
  - Streaming API：实时流式响应、并行工具调用。
  - Structured Output：结构化输出 API，原生/内置支持 OpenAI/Google。
  - Tool Calling Patterns：注解/类/函数式多样工具调用模式。
  - History Compression：长对话上下文压缩优化 Token 使用（保留系统消息、加强事实检索相关压缩）。
  - MCP：协议化工具与上下文互操作。
- 示例与测试
  - 示例：Web 搜索、行程规划、Tracing 示例、多模态/Compose 示例等。
  - 测试：`agents-test` 提供 LLM 响应/工具行为/图结构的模拟与测试工具。

## 运行拦截点与内置策略（Pipeline/Strategy）

- 运行拦截点（`AIAgentGraphPipeline`）
  - Agent 级：`interceptAgentStarting`（`AgentStartingContext`，含 `agent/runId`）、`interceptAgentCompleted`（`AgentCompletedContext`，含 `result`）、`interceptAgentExecutionFailed`（含异常）、`interceptAgentClosing`、`interceptAgentEnvironmentTransforming`。
  - Strategy 级：`interceptStrategyStarting`（`StrategyStartingContext`，含 `strategy`）、`interceptStrategyCompleted`（含 `result`）。
  - Node 级：`interceptNodeExecutionStarting`（含 `node/input`）、`interceptNodeExecutionCompleted`（含 `node/input/output`）、`interceptNodeExecutionFailed`。
  - LLM 级：`interceptLLMCallStarting`（含 `prompt/model/tools`）、`interceptLLMCallCompleted`（含 `responses/moderationResponse`）、`interceptLLMStreamingStarting`、`interceptLLMStreamingFrameReceived`、`interceptLLMStreamingCompleted`、`interceptLLMStreamingFailed`。
  - Tool 级：`interceptToolCallStarting`（含 `toolName/toolArgs`）、`interceptToolValidationFailed`、`interceptToolCallFailed`、`interceptToolCallCompleted`（含 `result`）。
- 内置策略
  - `chatAgentStrategy`：面向交互式聊天，循环执行 LLM 调用、工具执行与反馈；当 LLM 直接聊天而非使用工具时提供纠偏反馈。
  - `reActStrategy`：ReAct 模式，推理（LLM）与行动（工具）交替，用于迭代问题求解。
  - `singleRunStrategy`：单次处理输入后返回结果，适合非交互任务。
  - `toolBasedStrategy`：以工具为核心的工作流，依据 LLM 决策执行工具并处理结果。

## 特性模块与配置（Features）

- `agents-features-event-handler`
  - 用途：订阅标准化事件并执行自定义逻辑（日志、指标、外部副作用）。
  - 配置：通过回调注册（如 `onAgentStarting`、`onToolCallStarting`）。
- `agents-features-memory`
  - 用途：可插拔记忆接口，跨会话保存/检索上下文。
  - 配置：`memoryProvider`（如 `LocalFileMemoryProvider`）；可设 `featureName/productName/organizationName` 定义共享范围。
  - 能力：概念/事实（`SingleFact`/`MultipleFacts`），按 `MemorySubject`（User/Project/Environment）与 `MemoryScope`（AGENT/FEATURE/PRODUCT/ORGANIZATION）组织；支持 `Aes256GCMEncryption` 加密；提供节点 `nodeLoadFromMemory`、`nodeLoadAllFactsFromMemory`、`nodeSaveToMemoryAutoDetectFacts`。
- `agents-features-opentelemetry`
  - 用途：OTLP/Logging 导出、统一链路追踪。
  - 配置：`setServiceInfo`、`addSpanExporter`、`addSpanProcessor`、`addResourceAttributes`、`setSampler`、`setVerbose`、`setSdk`。
  - 采集点：`CreateAgentSpan`（实例生命周期）、`InvokeAgentSpan`（一次运行）、`NodeExecuteSpan`、`InferenceSpan`（LLM）、`ExecuteToolSpan`（工具）。
- `agents-features-snapshot`
  - 用途：持久化并恢复快照，重现与时光回溯调试。
  - 配置：`storage`（`InMemoryPersistenceStorageProvider` 等）、`enableAutomaticPersistence`（节点后自动 checkpoint）。
- `agents-features-trace`
  - 用途：端到端跟踪，适合本地开发与生产观测。
  - 配置：如 `messageWriter = TraceFeatureMessageFileWriter("trace.log")`。
- `agents-features-tokenizer`
  - 用途：消息 Token 计数与成本监控。

## LLM 客户端与能力（Clients）

- 抽象基类：`AbstractOpenAILLMClient`
  - 行为：Prompt→OpenAI 消息转换、工具序列化、结构化输出格式处理、流式框架；子类实现具体序列化与响应。
  - 能力：流式（SSE）、结构化输出、工具调用。
  - 接口方法：按提供商支持暴露 `LLMClient.moderate(content): ModerationResult` 与 `LLMClient.embed(texts): List<List<Float>>`（用于审核与向量生成）。
- 具体客户端（均实现 `LLMClient` 接口）
  - `OpenAILLMClient`：流式、结构化输出、工具、审核（moderation）、Embedding。
  - `AnthropicLLMClient`：流式、结构化输出、工具、审核；无 Embedding。
  - `GoogleLLMClient`：流式、结构化输出、工具、审核；无 Embedding。
  - `BedrockLLMClient`：支持流式/结构化/工具/审核；Embedding未明确（Embedding 另有接口）。
  - `OllamaLLMClient`：流式、结构化（JSON）、工具（自定义 ID）、审核、Embedding。
  - `OpenRouterLLMClient`：继承 `AbstractOpenAILLMClient`（流式/结构化/工具），审核；无 Embedding。
  - `DeepSeekLLMClient`：继承 `AbstractOpenAILLMClient`（流式/结构化/工具）；无审核/Embedding。
  - `MistralAILLMClient`：继承 `AbstractOpenAILLMClient`（流式/结构化/工具），审核。
  - `DashscopeLLMClient`：继承或兼容 OpenAI 风格（结构化/工具等）；细节见客户端模块文档。

## 存储与 RAG（Storage & RAG）

- 文件系统 Provider：`agents-utils`（如 `JVMFileSystemProvider.ReadWrite`），供本地记忆与文件持久化使用。
- Embeddings：`embeddings-base`、`embeddings-llm`（将文本转向量，LLM 支持 `embed(texts)`）。
- 向量存储：`rag-base`、`vector-storage`（内存/本地文件实现）。
- SQL 持久化：`agents-features-sql`（如 `ExposedPersistenceStorageProvider` 基于 JetBrains Exposed，支持 PostgreSQL/MySQL/H2/SQLite）。
- Agent 接线：通过特性安装进行接线（示例）
  - `Persistence`：配置 `storage = InMemoryPersistenceStorageProvider()` / 文件 / SQL；可 `enableAutomaticPersistence = true`。
  - `AgentMemory`：配置 `LocalFileMemoryProvider` 等；设定存储根路径与 `FileSystemProvider`。

## 示例（Examples）

- 基础 Agent：单次输入/输出，演示创建与运行 Agent（见 `docs/docs/basic-agents.md`）。
- 复杂工作流：自定义策略/工具/配置与自定义 IO 类型（见 `docs/docs/complex-workflow-agents.md`）。
- 银行业务助手：财务工具集成（如 `GetTransactionsTool`、`CalculateSumTool`）。
- 多模态应用：图片/音频/视频/文件与消息一并发送，示例包含图片描述与社媒文案生成。
- Compose Multiplatform Demo：`demo-compose-app`，支持 Android/桌面/Web/iOS，多平台能力演示。

## Version Catalog 详细清单（gradle/libs.versions.toml）

- 版本变量（[versions]）
  - `agp 8.11.1`
  - `annotations 26.0.2`
  - `assertj 3.27.4`
  - `awaitility 4.3.0`
  - `aws-sdk-kotlin 1.5.16`
  - `dokka 2.0.0`
  - `exposed 0.58.0`
  - `h2 2.2.224`
  - `hikaricp 6.2.1`
  - `jetsign 45.47`
  - `junit 5.8.2`
  - `knit 0.5.0`
  - `kotest 6.0.3`
  - `kotlin 2.2.10`
  - `kotlinx-coroutines 1.10.2`
  - `kotlinx-datetime 0.6.2`
  - `kotlinx-io 0.7.0`
  - `kotlinx-serialization 1.8.1`
  - `kover 0.9.2`
  - `ktlint 13.1.0`
  - `ktor3 3.2.2`
  - `lettuce 6.5.5.RELEASE`
  - `logback 1.5.13`
  - `mcp 0.7.2`
  - `mockito 5.19.0`
  - `mockk 1.13.8`
  - `mysql 8.0.33`
  - `netty 4.2.6.Final`
  - `opentelemetry 1.51.0`
  - `oshai-logging 7.0.7`
  - `postgresql 42.7.4`
  - `slf4j 2.0.17`
  - `spring-boot 3.5.3`
  - `spring-management 1.1.7`
  - `sqlite 3.46.1.3`
  - `testcontainers 1.19.7`
  - `mokksy 0.5.0-Alpha3`

- 库别名（[libraries]）
  - JetBrains/测试/断言：`jetbrains-annotations`、`mockito-junit-jupiter`、`assertj-core`、`awaitility`、`junit-jupiter-params`、`junit-platform-launcher`、`kotest-assertions-core`、`kotest-assertions-json`、`kotest-assertions`
  - Kotlin & 协程：`kotlin-bom`、`kotlin-gradle-plugin`、`kotlinx-coroutines-core/jdk8/reactive/reactor/test`、`kotlinx-datetime`、`kotlinx-io-core`、`kotlinx-serialization-core/json`
  - Ktor 客户端：`ktor-client-cio/js/darwin/apache5/core/content-negotiation/logging/mock`
  - Ktor 服务端：`ktor-server-core/cio/netty-jvm/sse/content-negotiation/cors/test-host`
  - 日志：`logback-classic`、`slf4j-simple`、`oshai-kotlin-logging`
  - Mock：`mockk`、`mokksy-a2a`
  - 文档/插件：`dokka-gradle-plugin`、`jetsign-gradle-plugin`
  - MCP：`mcp-client`、`mcp-server`
  - 测试容器：`testcontainers`、`testcontainers-junit`、`testcontainers-postgresql`、`testcontainers-mysql`
  - 数据/ORM/JDBC：`exposed-core/dao/jdbc/json/kotlin-datetime`、`hikaricp`、`postgresql`、`mysql`、`h2`、`sqlite`
  - OpenTelemetry：`opentelemetry-bom`、`opentelemetry-sdk`、`opentelemetry-exporter-otlp/logging`
  - AWS SDK Kotlin：`aws-sdk-kotlin-bedrock/bedrockruntime/sts`
  - Android 构建：`android-tools-gradle`
  - Spring：`spring-boot-bom`、`spring-boot-starter`、`spring-boot-starter-test`、`jackson-module-kotlin`、`reactor-kotlin-extensions`

- Bundles（依赖包组）
  - `spring-boot-core`：`spring-boot-starter`、`jackson-module-kotlin`、`kotlinx-coroutines-reactor`

- Plugins（Gradle 插件）
  - `com.android.application`（`android-application`）
  - `org.jetbrains.kotlinx.knit`（`knit`）
  - `org.jetbrains.kotlin.plugin.serialization`（`kotlin-serialization`）
  - `org.jetbrains.kotlin.plugin.spring`（`kotlin-spring`）
  - `org.jetbrains.kotlinx.kover`（`kotlinx-kover`）
  - `org.jlleitschuh.gradle.ktlint`（`ktlint`）
  - `io.spring.dependency-management`（`spring-management`）


