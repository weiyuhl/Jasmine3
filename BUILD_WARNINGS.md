## 构建环境
- Android Gradle Plugin: 8.13.0（`gradle/libs.versions.toml`）
- Kotlin: 2.2.21（`gradle/libs.versions.toml`）
- JDK: 17（项目以 Java 17 编译）
- OS: Windows（PowerShell 环境）

## 当前构建状态
- Debug APK 构建成功（含 `universal` 与各 ABI 变体）：
  - `app/build/outputs/apk/debug/jasmine_1.6.14_universal-debug.apk`
  - `app/build/outputs/apk/debug/jasmine_1.6.14_arm64-v8a-debug.apk`
  - `app/build/outputs/apk/debug/jasmine_1.6.14_x86_64-debug.apk`
