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

