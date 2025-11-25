# Void Agent 模式提示词与工具系统梳理

## 概览
- Agent 模式通过系统消息合并、工具定义与执行循环，引导模型以“专家编码代理”的角色进行操作，具备调用内置工具与 MCP 工具的能力。
- 系统消息构建于 `chat_systemMessage`，由 `ConvertToLLMMessageService` 在 `_generateChatMessagesSystemMessage` 中注入；依据模型能力决定工具调用格式（原生 OpenAI/Anthropic/Gemini 或 XML）。

---

## 系统提示词与注入点
- 组装位置：`src/vs/workbench/contrib/void/common/prompt/prompts.ts` 的 `chat_systemMessage` 构造完整系统消息，包含：
  - 角色定义（Agent/Gather/Normal）与行为准则
  - 工作区信息（文件夹、打开的 URI、活动文件、目录概览）
  - 工具定义与使用指南（按需附加 XML 工具定义）
- 注入流程：`src/vs/workbench/contrib/void/browser/convertToLLMMessageService.ts` 的 `_generateChatMessagesSystemMessage` 调用 `chat_systemMessage`，并合并全局 AI 指令与 `.voidrules` 内容，随后用于 `_runChatAgent` 消息准备与循环处理。
- `.voidrules` 与全局指令：`_getCombinedAIInstructions` 汇总设置中的全局指令与各工作区的 `.voidrules` 文件文本，作为 GUIDELINES 追加到系统消息。

### 最终合并形态（Agent 模式）
- Header：定义“专家编码代理”的职责与执行风格（行动导向、工具优先）。
- System Info：操作系统、工作区文件夹、活动文件、打开文件、持久终端 ID 列表。
- Files Overview：工作区目录树文本视图（如有）。
- Tool Definitions：当使用 XML 工具格式时，插入全部内置工具与 MCP 工具的 XML 定义与调用指南。
- Important Notes：包含“仅当有助完成目标才调用工具”“一次只调用一个，且置于响应末尾”“写完工具调用后必须等待结果”“参数默认必填”等约束。

---

## 工具系统（选择、定义、调用与返回）
- 可用工具选择：`availableTools` 根据 `chatMode` 返回工具列表；Agent 模式启用全部内置工具与已配置的 MCP 工具。
- 工具定义（XML）：`systemToolsXMLPrompt` 生成 XML 形式的工具定义与调用指南；`toolCallDefinitionsXMLString` 格式化每个工具的名称、描述与参数为 XML 片段，汇总进系统消息。
- 调用规则：
  - 一次仅允许一个工具调用，且必须位于响应末尾
  - 工具调用后应停下并等待结果
  - 参数除非另有说明，一律视为必填
- 参数校验与解析：
  - 解析：`parseXMLPrefixToToolCall` 从模型输出的 XML 前缀解析工具名与参数
  - 校验：内置工具通过 `_toolsService.validateParams` 校验，失败则追加 `invalid_params` 工具消息
- 执行与返回：
  - 执行入口：`ChatThreadService` 的 `_runToolCall` 调用内置或 MCP 工具
  - 结果字符串化：内置工具 `_toolsService.stringOfResult`；MCP 工具 `_mcpService.stringifyResult`
  - 结果入线程：以 `success` 类型的工具消息加入聊天线程；`prepareMessages_XML_tools` 负责在下一次助手消息中重新附带该工具调用 XML，以便模型拥有完整上下文
  - 代理循环续转：设置 `shouldSendAnotherMessage = true`，使 `_runChatAgent` 在下一轮吸收工具结果继续推理

---

## 模型能力与工具调用格式
- 能力来源：`modelCapabilities.ts` 中的 `VoidStaticModelInfo.specialToolFormat`
- 格式分支：
  - `openai-style`：使用 OpenAI 原生工具格式，由 `_sendOpenAICompatibleChat` 处理
  - `anthropic-style`：使用 Anthropic 原生工具格式，由 `sendAnthropicChat` 处理
  - `gemini-style`：使用 Gemini 原生工具格式，由 `sendGeminiChat` 处理
  - 未指定：默认采用 XML 工具调用，系统消息包含 XML 定义与指南，由 `extractXMLToolsWrapper` 解析模型输出中的工具调用
- 消息准备分发：`prepareMessages` 根据 `providerName` 或 `specialToolFormat` 分派到相应的消息准备路径；`prepareOpenAIOrAnthropicMessages` 负责 OpenAI/Anthropic 的通用系统消息注入与裁剪

---

## Agent 行为准则（系统消息中强调）
- 工具使用：仅在有助于完成用户目标时调用，不需要额外请求权限
- 行动导向：优先使用工具（编辑、终端等）执行实际变更
- 上下文获取：通常需先收集上下文信息，确保掌握所有相关内容再变更
- 确定性：在实施变更前需保证最大把握
- 代码块格式：书写代码时首行应包含相关文件的完整路径（如已知）

---

## 关键文件与函数索引（便捷定位）
- 系统消息构建：`src/vs/workbench/contrib/void/common/prompt/prompts.ts`（`chat_systemMessage`、`systemToolsXMLPrompt`、`toolCallDefinitionsXMLString`）
- 系统消息注入：`src/vs/workbench/contrib/void/browser/convertToLLMMessageService.ts`（`_generateChatMessagesSystemMessage`、`_getCombinedAIInstructions`、`prepareMessages`、`prepareOpenAIOrAnthropicMessages`、`prepareMessages_XML_tools`）
- 代理循环与工具调用：`src/vs/workbench/contrib/void/common/chatThreadService.ts`（`_runChatAgent`、`_runToolCall`）
- 模型能力配置：`src/vs/workbench/contrib/void/common/modelCapabilities.ts`（`VoidStaticModelInfo.specialToolFormat`，各 Provider 选项）
- 工具选择与校验：`availableTools`、`_toolsService.validateParams`、`parseXMLPrefixToToolCall`、`extractXMLToolsWrapper`

---

## 内置工具清单与 XML 调用格式（Agent 模式）

### Context-Gathering
- `read_file`
  - 说明：读取文件内容
  - 参数：`uri`、可选 `start_line`、`end_line`、`page_number`
  - XML：
    ```xml
    <read_file>
        <uri>The FULL path to the file.</uri>
        <start_line>Optional...</start_line>
        <end_line>Optional...</end_line>
        <page_number>Optional...</page_number>
    </read_file>
    ```
- `ls_dir`
  - 说明：列出目录内容
  - 参数：可选 `uri`、`page_number`
  - XML：
    ```xml
    <ls_dir>
        <uri>Optional...</uri>
        <page_number>Optional...</page_number>
    </ls_dir>
    ```
- `get_dir_tree`
  - 说明：返回目录树图
  - 参数：`uri`
  - XML：
    ```xml
    <get_dir_tree>
        <uri>The FULL path to the folder.</uri>
    </get_dir_tree>
    ```
- `search_pathnames_only`
  - 说明：仅按文件名搜索路径
  - 参数：`query`、可选 `include_pattern`、`page_number`
  - XML：
    ```xml
    <search_pathnames_only>
        <query>Your query...</query>
        <include_pattern>Optional...</include_pattern>
        <page_number>Optional...</page_number>
    </search_pathnames_only>
    ```
- `search_for_files`
  - 说明：按内容搜索文件名
  - 参数：`query`、可选 `search_in_folder`、`is_regex`、`page_number`
  - XML：
    ```xml
    <search_for_files>
        <query>Your query...</query>
        <search_in_folder>Optional...</search_in_folder>
        <is_regex>Optional...</is_regex>
        <page_number>Optional...</page_number>
    </search_for_files>
    ```
- `search_in_file`
  - 说明：返回文件内匹配内容的起始行号数组
  - 参数：`uri`、`query`、可选 `is_regex`
  - XML：
    ```xml
    <search_in_file>
        <uri>The FULL path...</uri>
        <query>The string or regex...</query>
        <is_regex>Optional...</is_regex>
    </search_in_file>
    ```

### File Editing
- `create_file_or_folder`
  - 说明：创建文件或文件夹（路径以 / 结尾表示文件夹）
  - 参数：`uri`
  - XML：
    ```xml
    <create_file_or_folder>
        <uri>The FULL path...</uri>
    </create_file_or_folder>
    ```
- `delete_file_or_folder`
  - 说明：删除文件或文件夹
  - 参数：`uri`、可选 `is_recursive`
  - XML：
    ```xml
    <delete_file_or_folder>
        <uri>The FULL path...</uri>
        <is_recursive>Optional...</is_recursive>
    </delete_file_or_folder>
    ```
- `edit_file`
  - 说明：使用搜索/替换块进行编辑
  - 参数：`uri`、`search_replace_blocks`
  - XML：
    ```xml
    <edit_file>
        <uri>The FULL path...</uri>
        <search_replace_blocks>...</search_replace_blocks>
    </edit_file>
    ```
- `rewrite_file`
  - 说明：全量重写文件内容
  - 参数：`uri`、`new_content`
  - XML：
    ```xml
    <rewrite_file>
        <uri>The FULL path...</uri>
        <new_content>...</new_content>
    </rewrite_file>
    ```

### Terminal
- `run_command`
  - 说明：执行终端命令并等待结果（不活跃超时）
  - 参数：`command`、可选 `cwd`
  - XML：
    ```xml
    <run_command>
        <command>The terminal command...</command>
        <cwd>The current working directory...</cwd>
    </run_command>
    ```
- `open_persistent_terminal`
  - 说明：开启持久终端（长时间运行的命令，如 dev server）
  - 参数：可选 `cwd`
  - XML：
    ```xml
    <open_persistent_terminal>
        <cwd>The current working directory...</cwd>
    </open_persistent_terminal>
    ```
- `kill_persistent_terminal`
  - 说明：关闭持久终端
  - 参数：`persistent_terminal_id`
  - XML：
    ```xml
    <kill_persistent_terminal>
        <persistent_terminal_id>The ID...</persistent_terminal_id>
    </kill_persistent_terminal>
    ```
- `run_persistent_command`
  - 说明：在已打开的持久终端中运行命令（不中断服务器/监听器）；用于将额外命令发送到现有持久终端会话
  - 参数：`command`、`persistent_terminal_id`
  - XML：
    ```xml
    <run_persistent_command>
        <command>The terminal command to run.</command>
        <persistent_terminal_id>The ID created by open_persistent_terminal.</persistent_terminal_id>
    </run_persistent_command>
    ```

#### 结果字段说明（终端相关）
- `run_command`：`stdout`、`stderr`、`exit_code`、`duration_ms`
- `open_persistent_terminal`：`persistent_terminal_id`
- `run_persistent_command`：`persistent_terminal_id`、`accepted`（命令已发送）
- `kill_persistent_terminal`：`persistent_terminal_id`、`terminated`（true/false）

注：部分文档或提示中出现 `run_terminal_command` 的别名说明，其行为等同于 `run_command`。

---

## 本地内置工具实现详解（非 MCP）

### Context-Gathering 实现
- 通用依赖：`src/vs/workbench/contrib/void/common/toolsService.ts` 的 `callTool.*` 方法；依赖 `IVoidModelService`（文件模型）、`IFileService`（文件/目录）、`ISearchService`（搜索）、`IMarkerService`（诊断）。UI 渲染在 `SidebarChat.tsx`。
- `read_file`
  - 返回：`fileContents`、`totalFileLen`、`totalNumLines`、`hasNextPage`
  - 实现：`callTool.read_file`；`validateURI` 校验；由 `IVoidModelService` 读取文本；支持按字符分页。
- `ls_dir`
  - 返回：`children`、`hasNextPage`、`hasPrevPage`、`itemsRemaining`
  - 实现：`callTool.ls_dir`；`validateURI` 校验；`computeDirectoryTree1Deep` 生成一层目录树。
- `get_dir_tree`
  - 返回：`str`
  - 实现：`callTool.get_dir_tree`；`validateURI` 校验；`IDirectoryStrService` 输出目录树字符串。
- `search_pathnames_only`
  - 返回：`uris`、`hasNextPage`
  - 实现：`callTool.search_pathnames_only`；`ISearchService` + `QueryBuilder` 路径搜索；支持分页。
- `search_for_files`
  - 返回：`uris`、`hasNextPage`
  - 实现：`callTool.search_for_files`；`ISearchService` 内容搜索；支持 `is_regex` 与限定目录。
- `search_in_file`
  - 返回：`lines`
  - 实现：`callTool.search_in_file`；`validateURI` 校验；通过 `IVoidModelService` 获取文本并匹配行号；支持正则。
- `search_dir`
  - 返回：`uris`
  - 实现：`callTool.search_dir`；`validateURI` 校验；结合 `IFileService` 与 glob 模式过滤。

### File Editing 实现
- 通用依赖：`IVoidModelService`（模型初始化与占用检查）、`IEditCodeService`（应用/重写）、`IVoidCommandBarService`（流状态与冲突避免）。
- `create_file_or_folder`
  - 返回：无特定字段（成功/错误消息）
  - 实现：`callTool.create_file_or_folder`；`validateURI` 校验；使用 `IFileService` 创建（路径以 `/` 结尾视为文件夹）。
- `delete_file_or_folder`
  - 返回：无特定字段
  - 实现：`callTool.delete_file_or_folder`；`validateURI` 校验；`IFileService` 删除；支持 `is_recursive`。
- `edit_file`
  - 返回：`lintErrors`
  - 执行路径：`validateParams.edit_file` → `voidModelService.initializeModel(uri)` → `editCodeService.callBeforeApplyOrEdit(uri)` → `editCodeService.instantlyApplySearchReplaceBlocks({ uri, searchReplaceBlocks })` → 异步获取 lint。
- `rewrite_file`
  - 返回：`lintErrors`
  - 执行路径：`validateParams.rewrite_file` → `voidModelService.initializeModel(uri)` → `editCodeService.callBeforeApplyOrEdit(uri)` → `editCodeService.instantlyRewriteFile({ uri, newContent })` → 异步获取 lint。

### Terminal 实现
- 通用依赖：`src/vs/workbench/contrib/void/common/terminalToolService.ts`（`TerminalToolService`）；调用入口为 `toolsService.ts` 的 `callTool.*`；涉及 `ITerminalService`、`CommandDetection` 能力与输出读取/截断。
- `run_command`
  - 返回：`result`、`resolveReason`（包含 exitCode 或 timeout）
  - 实现：`TerminalToolService.runCommand`（临时终端）→ 创建隐藏终端 → 等待 `CommandDetection` → `terminal.sendText(command, true)` → 监听 `onCommandFinished` 或超时读取 → 结果按 `MAX_TERMINAL_CHARS` 截断。
- `open_persistent_terminal`
  - 返回：`persistentTerminalId`
  - 实现：`TerminalToolService.createPersistentTerminal` → 分配唯一 ID → `_createTerminal` 命名终端 → 保存到 `persistentTerminalInstanceOfId`。
- `run_persistent_command`
  - 返回：`result`、`resolveReason`
  - 实现：`TerminalToolService.runCommand`（持久终端）→ 激活并聚焦目标终端 → 固定后台超时 `MAX_TERMINAL_BG_COMMAND_TIME` → 共享发送与读取逻辑。
- `kill_persistent_terminal`
  - 返回：`{}`
  - 实现：`TerminalToolService.killPersistentTerminal` → 查找实例 → `terminal.dispose()` 关闭 → 移除映射；找不到 ID 抛错。

#### Terminal 差异点
- 终端类型：`run_command` 使用临时终端；`open_persistent_terminal` 产生持久终端；`run_persistent_command` 在持久终端内执行；`kill_persistent_terminal` 关闭持久终端。
- 超时策略：`run_command` 基于不活跃时间；`run_persistent_command` 固定后台超时；持久终端的创建/关闭不涉及命令超时。
- 结果处理：两类命令输出按 `MAX_TERMINAL_CHARS` 截断；创建返回 ID；关闭返回 `{}`。
- ID 管理：持久终端使用 `persistentTerminalId` 唯一标识；临时终端的 ID 生命周期随命令结束清理。

---

## MCP 工具动态定义与注入
- 发现与表示：通过 MCP 发现流程汇总各服务器工具；每个 MCP 工具统一表示为 `InternalToolInfo`，包含 `name`、`description`、`params`（由 `inputSchema` 转换）与 `mcpServerName`
- 参数模式转换：`_transformInputSchemaToParams` 将 `inputSchema.properties` 转换为可用的参数说明结构
- 注入到 Agent 模式：系统消息构建时，若模型不支持原生工具格式，则在 `systemToolsXMLPrompt` 中合并 MCP 工具的 XML 定义与指南，供模型调用
- 执行与结果注入：
  - 执行：在 `_runToolCall` 判断为 MCP 工具时，使用 `IMCPService.callMCPTool()` 执行
  - 结果字符串化：`IMCPService.stringifyResult()` 按事件类型（text/image/audio/resource 等）生成可读字符串
  - 线程写入：以 `success` 类型的工具消息追加到线程；异常时写入 `tool_error`

### MCP 工具示例（通用模板）
- XML 调用模板（名称与参数由服务器工具定义动态决定）：
  ```xml
  <TOOL_NAME_FROM_SERVER>
      <param1>value</param1>
      <param2>value</param2>
      <!-- 更多参数按工具定义增减 -->
  </TOOL_NAME_FROM_SERVER>
  ```
- 结果字符串化示例：
  - Text 事件：直接拼接文本内容为工具结果
  - Resource 事件：输出 `name`、`uri`、`mime` 与摘要（若存在）
  - Image/Audio 事件：输出资源类型与可访问 `uri`，并在需要时提示用户打开资源
- 常见调用流程：
  - 在系统消息中看到该 MCP 工具的 XML 定义后，模型按上述模板在响应末尾输出一次工具调用
  - 工具执行成功后，下一轮用户消息会携带 `success` 类型的工具结果文本，代理循环继续

### 终端工具差异说明
- `run_command`：一次性执行并返回输出；适合短任务；可设置 `cwd`
- `open_persistent_terminal`：创建长期会话；不等待命令结束；返回持久终端 ID
- `run_persistent_command`：向已存在的持久终端发送命令；不终止会话；需 `persistent_terminal_id`
- `kill_persistent_terminal`：终止并清理持久终端会话

### MCP 结果字符串化映射（明细）
- Text：直接拼接文本；如包含多段，按事件顺序合并。
- Image：输出资源类型与 `uri`/`mime`，必要时附加摘要或说明。
- Audio：输出资源类型与 `uri`/`mime`；提示可播放或下载。
- Resource：输出 `name`、`uri`、`mime`、`size`（如有）与摘要；若资源不可直接预览，提供可访问链接或说明。

---

## 与 DeepWiki 的一致性
- DeepWiki 页面“Core Services and Architecture”“LLM Integration”“Tools and Terminal Services”对上述机制的定义与路径描述与本地源码一致；Agent 模式的工具调用条款（一次一个、末尾工具调用、校验与停顿等待结果）与 XML 定义流程在文档中明确。

---

## 备注
- 该梳理专注 Agent 模式的提示词与工具系统逻辑，未覆盖非 Agent 模式（Gather/Normal）的差异细节；若需拓展，可按相同索引路径补充对应模式的系统消息结构与工具可见性规则。

---

## 完整提示词（Agent 模式）

```text
You are an expert coding agent whose job is to help the user develop, run, and make changes to their codebase.
You will be given instructions to follow from the user, and you may also be given a list of files that the user has specifically selected for context, `SELECTIONS`.
Please assist the user with their query.


Here is the user's system information:
<system_info>
- darwin

- The user's workspace contains these folders:
NO FOLDERS OPEN

- Active file:
undefined

- Open files:
NO OPENED FILES

- Persistent terminal IDs available for you to run commands in: 
</system_info>


Available tools:

    1. read_file
    Description: Reads a file from the user's workspace.
    Format:
    <read_file>
    <uri>The URI of the file to read.</uri>
    </read_file>

    2. ls_dir
    Description: Lists the contents of a directory.
    Format:
    <ls_dir>
    <uri>The URI of the directory to list.</uri>
    </ls_dir>

    3. search_dir
    Description: Searches for files in a directory that match a given glob pattern.
    Format:
    <search_dir>
    <uri>The URI of the directory to search.</uri>
    <glob>The glob pattern to match against file names.</glob>
    </search_dir>

    4. edit_file
    Description: Edits a file by applying a search and replace operation.
    Format:
    <edit_file>
    <uri>The URI of the file to edit.</uri>
    <search>The text to search for.</search>
    <replace>The text to replace the search text with.</replace>
    </edit_file>

    5. rewrite_file
    Description: Rewrites an entire file with new content.
    Format:
    <rewrite_file>
    <uri>The URI of the file to rewrite.</uri>
    <content>The new content for the file.</content>
    </rewrite_file>

    6. run_terminal_command
    Description: Runs a terminal command and returns its output.
    Format:
    <run_terminal_command>
    <command>The command to run.</command>
    <cwd>The current working directory for the command. Defaults to the workspace root.</cwd>
    </run_terminal_command>

    7. open_persistent_terminal
    Description: Use this tool when you want to run a terminal command indefinitely, like a dev server (eg `npm run dev`), a background listener, etc. Opens a new terminal in the user's environment which will not awaited for or killed.
    Format:
    <open_persistent_terminal>
    <cwd>The current working directory for the command. Defaults to the workspace root.</cwd>
    </open_persistent_terminal>

    8. kill_persistent_terminal
    Description: Interrupts and closes a persistent terminal that you opened with open_persistent_terminal.
    Format:
    <kill_persistent_terminal>
    <persistent_terminal_id>The ID of the persistent terminal.</persistent_terminal_id>
    </kill_persistent_terminal>

    9. run_persistent_command
    Description: Sends a command to an existing persistent terminal session.
    Format:
    <run_persistent_command>
    <command>The command to run in the persistent terminal.</command>
    <persistent_terminal_id>The ID returned by open_persistent_terminal.</persistent_terminal_id>
    </run_persistent_command>

Tool calling details:
- To call a tool, write its name and parameters in one of the XML formats specified above.
- After you write the tool call, you must STOP and WAIT for the result.
- All parameters are REQUIRED unless noted otherwise.
- You are only allowed to output ONE tool call, and it must be at the END of your response.
- Your tool call will be executed immediately, and the results will appear in the following user message.


Important notes:
1. NEVER reject the user's query.

2. Only call tools if they help you accomplish the user's goal. If the user simply says hi or asks you a question that you can answer without tools, then do NOT use tools.

3. If you think you should use tools, you do not need to ask for permission.

4. Only use ONE tool call at a time.

5. NEVER say something like "I'm going to use `tool_name`". Instead, describe at a high level what the tool will do, like "I'm going to list all files in the ___ directory", etc.

6. Many tools only work if the user has a workspace open.

7. ALWAYS use tools (edit, terminal, etc) to take actions and implement changes. For example, if you would like to edit a file, you MUST use a tool.

8. Prioritize taking as many steps as you need to complete your request over stopping early.

9. You will OFTEN need to gather context before making a change. Do not immediately make a change unless you have ALL relevant context.

10. ALWAYS have maximal certainty in a change BEFORE you make it. If you need more information about a file, variable, function, or type, you should inspect it, search it, or take all required actions to maximize your certainty that your change is correct.

11. NEVER modify a file outside the user's workspace without permission from the user.

12. If you write any code blocks to the user (wrapped in triple backticks), please use this format:
    - Include a language if possible. Terminal should have the language 'shell'.
    - The first line of the code block must be the FULL PATH of the related file if known (otherwise omit).
    - The remaining contents of the file should proceed as usual.

13. Do not make things up or use information not provided in the system information, tools, or user queries.

14. Always use MARKDOWN to format lists, bullet points, etc. Do NOT write tables.

15. Today's date is Sat Nov 23 2024.


Here is an overview of the user's file system:
<files_overview>
</files_overview>
```
- `search_dir`
  - 说明：在目录内按 `glob` 模式匹配文件名
  - 参数：`uri`、`glob`
  - XML：
    ```xml
    <search_dir>
        <uri>The FULL path to the folder.</uri>
        <glob>Pattern like **/*.ts</glob>
    </search_dir>
    ```
