# 头文件最小化（保持 ABI 安全）— 计划与状态

## 目标
- 在保证 ABI/虚表与类布局稳定的前提下，最小化 LLM 与 Diffusion 引擎的公共头文件。
- 不改变现有构建/发布流程；禁止二进制与构建产物进入版本库。

## 当前状态（已暂缓继续、改为分期合并）
- 主分支构建已验证通过：`:ai` 与 `:app` 均可 assemble。
- 保留所有虚函数与类成员；仅移除非虚且未使用的声明与冗余包含。
- 后续工作将按阶段分期合并，并在每次合并后清理分支。

## 已完成（已合并到 main）
- LLM 头（`ai/src/main/cpp/mnn/transformers/llm/engine/include/llm/llm.hpp`）：
  - 精简重型包含；采用 `<iosfwd>`，并通过前向声明替代 `Module` 与 `RuntimeManager` 的直接包含。
  - 移除未使用的非虚方法（受保护/私有工具）、友元 `Pipeline` 与未用枚举。
  - 保留 `response(ChatMessages)`、`response(vector<int>)`、`generate(int)`、`getContext()`、`set_config`、`dump_config` 及全部虚接口。
- Diffusion 头（`ai/src/main/cpp/mnn/transformers/diffusion/engine/include/diffusion/diffusion.hpp`）：
  - 移除 `using namespace`，全限定使用 `MNN::Express::VARP` 与 `MNN::MNNForwardType`。
  - 补充 `<string>/<memory>` 以保持头文件自包含。
  - 移除私有非虚方法声明（`step_plms`、`text_encoder`、`unet`、`vae_decoder`）。
- `.gitignore`：全面屏蔽二进制、压缩包与构建产物。

## 余下工作（分期合并）
- 阶段 1：继续删除 `llm.hpp`/`diffusion.hpp` 中未被 JNI 路径引用的非虚未用声明。
- 阶段 2：清理冗余 `#include`；尽可能以前向声明替代。
- 阶段 3（可选）：在 `MNN/expr/*` 中减少传递包含；每一步均需编译验证。

## 每阶段验证
- 执行 `./gradlew :ai:assembleDebug -x test --warning-mode all`。
- 执行 `./gradlew :app:assembleDebug -x test --warning-mode all`。
- 确保无 ABI/虚表变化：不增加/删除/重排虚函数或类成员。

## 分支策略与清理
- 已使用的分支：`min-headers-abi-safe`、`min-headers-abi-safe-phase-2`。
- 每一阶段：
  - 使用 `--no-ff` 合并到 `main`，并附加清晰的提交信息。
  - 合并后删除远端与本地分支以保持仓库整洁。

## 常用命令（参考）
- 合并到主分支：
  - `git checkout main`
  - `git merge --no-ff min-headers-abi-safe -m "merge: abi-safe header minimization phase N; builds verified"`
  - `git push`
- 合并后清理分支：
  - `git branch -d min-headers-abi-safe`
  - `git push origin --delete min-headers-abi-safe`
  - （历史）`git push origin --delete min-headers`

## 说明
- 不修改虚接口签名、顺序或类成员布局。
- 保留 JNI 使用的重载与类型（`ChatMessages`、`vector<int>`、上下文访问等）。

## 进度日志（2025-12-03）
- 加固 `.gitignore` 并清理已追踪构建产物：
  - 全局忽略二进制/压缩包/构建输出；确保 `jniLibs`/`libs` 忽略。
  - 取消追踪并删除远端 `ai/src/main/cpp/build_arm64/*` 构建产物。
  - 相关提交：清理构建产物与扩展 `.gitignore`。
- LLM/Diffusion 初次头文件裁剪：
  - 从引擎头中移除 `#include <MNN/expr/NeuralNetWorkOp.hpp>` 与 `MathOp.hpp`。
  - 删除未用 MNN 头：`ImageProcess.hpp`、`Matrix.h`、`Rect.h`、`expr/ExprCreator.hpp`、`expr/Scope.hpp`、`expr/Optimizer.hpp`。
  - 将 `Module.hpp` 改为前向声明；以 `<iosfwd>` 替代 `<iostream>`；移除重型标准库头。
  - 构建通过并推送至 `main`。
- diffusion.hpp 命名空间安全化：
  - 移除 `using namespace`；全限定 `MNN::Express::VARP` 与 `MNN::MNNForwardType`。
  - 增加 `<string>` 与 `<memory>`，保持头自包含。
  - 提交已推送（如 de11533）。
- ABI 稳定性调整：
  - 在 `llm.hpp` 恢复 multimodal 虚接口声明以匹配上游虚表，同时保持类型前向声明。
  - 移除未用友元 `Pipeline` 与未用的 `TimePerformance` 前向声明。
- 分支 `min-headers-abi-safe`：
  - 删除受保护的非虚工具方法：`initRuntime`、`setRuntimeHint`、两版 `forwardVec`。
  - 删除私有非虚方法：`setSpeculativeConfig`、`updateContext`。
  - 删除未用枚举：`TuneType`、`MatchStrictLevel`、`NgramSelectRule`。
  - 构建通过；合并到 `main`（如 bb6f9ba、8f8a599）并推送。
- Diffusion 私有工具方法移除：
  - 从 `diffusion.hpp` 移除 `step_plms`、`text_encoder`、`unet`、`vae_decoder` 声明。
  - 构建通过；推送到 `main`（如 1c72f25）。
- 分支清理：
  - 删除本地与远端 `min-headers-abi-safe`，删除历史分支 `min-headers`（远端）。

## 待办（暂缓、改为分期合并）
- 按上述方案分期合并剩余的非虚清理与包含精简。
- 构建与测试验证（2025-12-03）：
  - 单元测试：`./gradlew :app:testDebugUnitTest` — 完成，无失败用例。
  - Lint：`./gradlew :app:lint` — 报告位于 `app/build/reports/lint-results-debug.html`。
  - 构建 APK：`./gradlew :app:assembleDebug` — 产物：
    - `app/build/outputs/apk/debug/jasmine_1.0_arm64-v8a-debug.apk`
    - `app/build/outputs/apk/debug/jasmine_1.0_universal-debug.apk`
    - `app/build/outputs/apk/debug/jasmine_1.0_x86_64-debug.apk`
  - 测试版 APK 导出：
    - `dist/jasmine-test-20251203-1933-universal.apk`
  - 状态：构建成功；Lint 报告生成；产物已验证。
- Phase 2 (2025-12-03)
  - Removed unnecessary include from diffusion header: `<MNN/Interpreter.hpp>`.
  - Branch: `min-headers-abi-safe-phase-2` — builds verified (`:ai` and `:app`).
  - Pushed branch and prepared PR link: `https://github.com/weiyuhl/jasmine/pull/new/min-headers-abi-safe-phase-2`.
  - Pending: merge to `main` and branch cleanup after review.
  - Make `llm.hpp` self-contained: add `<map>` for `mModulePool`.
  - Builds re-verified (`:ai` and `:app`); branch updated.
  - Add `<MNN/MNNForwardType.h>` in `diffusion.hpp` to ensure `MNNForwardType` is declared without transitive headers.
  - Add `<utility>` in `llm.hpp` to ensure `std::pair` declaration for `ChatMessage`.
  - Builds re-verified (`:ai` and `:app`); branch updated.
- 阶段 2（2025-12-03）
  - 从 diffusion 头移除不必要的包含：`<MNN/Interpreter.hpp>`。
  - 在 `diffusion.hpp` 增加 `<MNN/MNNForwardType.h>`，确保无需传递头也能声明 `MNNForwardType`。
  - 在 `llm.hpp` 增加 `<map>`（用于 `mModulePool`）与 `<utility>`（用于 `std::pair`）。
  - 分支：`min-headers-abi-safe-phase-2` — 构建再次验证通过（`:ai` 与 `:app`）。
  - 已推送分支并准备 PR 链接：`https://github.com/weiyuhl/jasmine/pull/new/min-headers-abi-safe-phase-2`。
  - 已合并到 `main` 并完成分支清理（本地与远端）。
  - 导出测试 APK（主分支）：`dist/jasmine-test-20251203-1957-universal.apk`。
- 阶段 3（2025-12-03）
  - 在 `llm.hpp` 增加 `<cstdint>`，确保 `int64_t`（用于 `LlmContext`）在无传递头时也能声明。
  - 分支：`min-headers-abi-safe-phase-3` — 构建已验证（`:ai` 与 `:app`）。
  - 已推送分支并准备 PR 链接：`https://github.com/weiyuhl/jasmine/pull/new/min-headers-abi-safe-phase-3`。
  - 已合并到 `main` 并完成分支清理（本地与远端）。
- 阶段 4（2025-12-03）
  - 自包含增强（JNI 侧头）：`llm_session.h` 增加 `<functional>/<utility>/<cstdint>`，避免依赖传递头。
  - 分支：`min-headers-abi-safe-phase-4` — 构建已验证（`:ai` 与 `:app`）。
  - 已推送分支、准备 PR 链接：`https://github.com/weiyuhl/jasmine/pull/new/min-headers-abi-safe-phase-4`。
  - 已合并到 `main` 并完成分支清理（本地与远端）。
  - 导出测试 APK（主分支）：`dist/jasmine-test-20251203-2020-universal.apk`。
