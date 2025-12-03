# Header Minimization (ABI-Safe) — Plan and Status

## Goals
- Minimize public headers for LLM and Diffusion engines while keeping ABI/vtable and class layout stable.
- Do not change build/release flows. Keep binaries and build artifacts out of VCS.

## Current Status (Paused)
- Builds verified on main: `:ai` and `:app` assemble pass.
- Virtual APIs and class members retained; only non-virtual, unused declarations and redundant includes removed.
- Plan is paused per request. Remaining work will be merged in phases and branches cleaned up.

## Completed Changes (Merged to main)
- LLM header (`ai/src/main/cpp/mnn/transformers/llm/engine/include/llm/llm.hpp`):
  - Removed heavy includes; use `<iosfwd>` and forward declarations for `Module` and `RuntimeManager`.
  - Removed unused non-virtual methods (protected/private helpers), friend `Pipeline`, and unused enums.
  - Kept `response(ChatMessages)`, `response(vector<int>)`, `generate(int)`, `getContext()`, `set_config`, `dump_config`, and all virtual APIs.
- Diffusion header (`ai/src/main/cpp/mnn/transformers/diffusion/engine/include/diffusion/diffusion.hpp`):
  - Removed `using namespace`; fully qualify `MNN::Express::VARP` and `MNN::MNNForwardType`.
  - Added `<string>/<memory>` to be self-contained.
  - Removed private non-virtual method declarations (`step_plms`, `text_encoder`, `unet`, `vae_decoder`).
- `.gitignore`: forbids binaries, archives, and build artifacts.

## Remaining Work (To be merged in phases)
- Phase 1: Remove any other non-virtual, unused declarations in `llm.hpp`/`diffusion.hpp` that are not referenced by JNI paths.
- Phase 2: Trim redundant `#include`s in headers; prefer forward declarations whenever possible.
- Phase 3: (Optional) Reduce transitive includes in `MNN/expr/*` via safe forward declarations; compile-verify per step.

## Validation per Phase
- Run `./gradlew :ai:assembleDebug -x test --warning-mode all`.
- Run `./gradlew :app:assembleDebug -x test --warning-mode all`.
- Ensure no ABI/vtable changes: do not add/remove/reorder virtual methods or class members.

## Branch Strategy and Cleanup
- Active branch used: `min-headers-abi-safe`.
- After each phase:
  - Merge to `main` with `--no-ff` and descriptive message.
  - Delete merged branch from remote and local to keep repo clean.

## Commands (for reference)
- Merge to main:
  - `git checkout main`
  - `git merge --no-ff min-headers-abi-safe -m "merge: abi-safe header minimization phase N; builds verified"`
  - `git push`
- Cleanup branches after merge:
  - `git branch -d min-headers-abi-safe`
  - `git push origin --delete min-headers-abi-safe`
  - (obsolete) `git push origin --delete min-headers`

## Notes
- Do not modify virtual API signatures, ordering, or class member layout.
- Keep JNI-used overloads and types (`ChatMessages`, vector<int>, context access) intact.

## Progress Log (2025-12-03)
- Harden .gitignore and remove tracked build artifacts
  - Added global ignores for binaries/archives/build outputs; ensured `jniLibs`/`libs` are ignored.
  - Untracked and removed remote `ai/src/main/cpp/build_arm64/*` artifacts.
  - Commits: "repo: remove tracked build artifacts...", "gitignore: ignore global build artifacts and AI native outputs...".
- Initial header pruning (LLM/Diffusion)
  - Removed `#include <MNN/expr/NeuralNetWorkOp.hpp>` and `MathOp.hpp` from engine headers.
  - Deleted unused MNN headers: `ImageProcess.hpp`, `Matrix.h`, `Rect.h` and `expr/ExprCreator.hpp`, `expr/Scope.hpp`, `expr/Optimizer.hpp`.
  - Converted `Module.hpp` dependency to forward declarations; replaced `<iostream>` with `<iosfwd>`; removed heavy std headers.
  - Builds verified; pushed to `main`.
- Namespace-safe diffusion.hpp
  - Removed `using namespace`; fully qualified `MNN::Express::VARP` and `MNN::MNNForwardType`.
  - Added `<string>` and `<memory>` to make header self-contained.
  - Commit pushed (e.g., de11533).
- ABI stability adjustments
  - Restored multimodal virtual declarations in `llm.hpp` to match upstream vtable, while keeping types forward-declared.
  - Removed unused friend `Pipeline`, unused `TimePerformance` forward decl.
- Branch `min-headers-abi-safe`
  - Removed protected non-virtual helpers: `initRuntime`, `setRuntimeHint`, `forwardVec` overloads.
  - Removed private non-virtuals: `setSpeculativeConfig`, `updateContext`.
  - Removed unused enums: `TuneType`, `MatchStrictLevel`, `NgramSelectRule`.
  - Builds verified; merged to `main` (e.g., bb6f9ba, 8f8a599) and pushed.
- Diffusion private helpers removed
  - Removed `step_plms`, `text_encoder`, `unet`, `vae_decoder` declarations from `diffusion.hpp`.
  - Builds verified; pushed to `main` (e.g., 1c72f25).
- Branch cleanup
  - Deleted `min-headers-abi-safe` locally and on remote; deleted legacy `min-headers` on remote.

## Pending (Paused)
- Phase-based merging of remaining non-virtual cleanup and include trimming as described above.
- Build & Test Validation (2025-12-03)
  - Ran unit tests: `./gradlew :app:testDebugUnitTest` — completed; no failing tests.
  - Ran lint: `./gradlew :app:lint` — report at `app/build/reports/lint-results-debug.html`.
  - Built APKs: `./gradlew :app:assembleDebug` — outputs:
    - `app/build/outputs/apk/debug/jasmine_1.0_arm64-v8a-debug.apk`
    - `app/build/outputs/apk/debug/jasmine_1.0_universal-debug.apk`
    - `app/build/outputs/apk/debug/jasmine_1.0_x86_64-debug.apk`
  - Exported test APK:
    - `dist/jasmine-test-20251203-1933-universal.apk`
  - Status: build successful; lint report generated; artifacts verified.
