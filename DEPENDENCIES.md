# 依赖库清单（jasmine 项目）

以下清单按“功能分组”进行整理，并标注被使用的模块与主要用途。

## UI 与界面
- `androidx.core:core-ktx`（app、ai、document、common）— Kotlin 扩展；简化集合与平台 API。
- `androidx.activity:activity-compose`（app）— Activity 与 Compose 集成。
- `androidx.compose:compose-bom`（app、ai、search、tts、highlight）— Compose 版本对齐。
- `androidx.compose.ui:ui`、`ui-graphics`、`ui-tooling-preview`（app、highlight）— UI 核心、图形与预览。
- `androidx.compose.material:material-icons-extended`（app）— Material 图标扩展集。
- `androidx.compose.material3:material3`（app、ai、search、tts、highlight）— Material 3 组件。
- `androidx.compose.material3.adaptive:adaptive`、`adaptive-layout`（app）— 自适应布局。
- `androidx.appcompat:appcompat`、`com.google.android.material:material`（document、common）— View 系 Material 与兼容支持。
- `io.github.brdominguez:compose-sonner`（app）— Compose Toast/通知。
- `com.jvziyaoyao.scale:image-viewer`（app）— 图片查看与缩放。
- `io.github.petterpx:floatingx`、`floatingx-compose`（common）— 悬浮窗/浮层组件。

## 导航、生命周期与后台
- `androidx.navigation:navigation-compose`（app）— 应用内导航。
- `androidx.lifecycle:lifecycle-runtime-ktx`、`lifecycle-process`（app）— 生命周期与进程级回调。
- `androidx.work:work-runtime-ktx`（app）— 后台任务调度。
- `androidx.profileinstaller:profileinstaller`（app）— 安装 Baseline Profile。

## 网络与序列化
- `com.squareup.okhttp3:okhttp`、`okhttp-sse`、`logging-interceptor`（app、ai、search、tts、common）— HTTP 客户端、SSE 与日志。
- `com.squareup.retrofit2:retrofit`、`converter-kotlinx-serialization`（app）— REST 封装与 JSON 转换。
- `io.ktor:ktor-bom`、`ktor-client-core`、`ktor-client-okhttp`、`ktor-client-content-negotiation`、`ktor-serialization-kotlinx-json`（app）— Ktor 客户端与内容协商。
- `org.jetbrains.kotlinx:kotlinx-serialization-json`（app、ai、search、highlight、tts、common）— JSON 序列化。

## 数据存储、数据库与分页
- `androidx.datastore:datastore-preferences`（app）— 轻量键值存储。
- `androidx.room:room-runtime`、`room-ktx`、`room-paging`（app）— Room 数据库与分页。
- `androidx.paging:paging-runtime`、`paging-compose`（app）— 列表分页加载。

## 图片与媒体
- `io.coil-kt.coil3:coil-compose`、`coil-network-okhttp`、`coil-svg`（app）— 图片加载与 SVG 支持。
- `androidx.media3:media3-exoplayer`、`media3-ui`、`media3-common`（tts）— 媒体播放与 UI。
- `com.drewnoakes:metadata-extractor`（app）— 图片/文件元数据读取。
- `com.github.yalantis:ucrop`（app）— 图片裁剪。

## 扫码与相机
- `com.google.zxing:core`（app）— 条码/二维码解析。
- `io.github.g00fy2.quickie:quickie-bundled`（app）— 扫码集成。
- `com.google.mlkit:barcode-scanning`（app）— ML Kit 条码识别。
- `androidx.camera:camera-core`（app）— CameraX 基础能力。

## Firebase 与监测
- `com.google.firebase:firebase-bom`、`firebase-analytics`、`firebase-crashlytics`、`firebase-config`（app）— 分析、崩溃与远程配置。

## 文本、解析与脚本
- `org.apache.commons:commons-text`（app、common）— 文本处理。
- `org.jsoup:jsoup`（search）— HTML 解析。
- `org.jetbrains:markdown`（app）— Markdown 解析与渲染。
- `wang.harlon.quickjs:wrapper-android`（highlight）— QuickJS 脚本引擎。

## 文件、同步与模板
- `androidx.documentfile:documentfile:1.0.1`（app）— 文档/SAF 文件操作。
- `com.github.bitfireAT:dav4jvm`（app）— WebDAV 客户端（同步/远程目录）。
- `io.pebbletemplates:pebble`（app）— 模板引擎。

## 数学渲染与 AI 集成
- `com.github.rikkahub.jlatexmath-android:jlatexmath` 及字体包（app）— LaTeX 公式渲染。
- `io.modelcontextprotocol:kotlin-sdk`（app）— MCP SDK。

## 测试与基准
- `junit:junit`（通用）— 单元测试。
- `androidx.test.ext:junit`、`androidx.test.espresso:espresso-core`（多模块）— 仪器与 UI 测试。
- `androidx.compose.ui:ui-test-junit4`、`ui-test-manifest`（app）— Compose UI 测试。
- `androidx.test.uiautomator:uiautomator`、`androidx.benchmark:benchmark-macro-junit4`（app:baselineprofile）— 设备自动化与性能基准。

## 直接依赖与工具
- `org.jetbrains.kotlin:kotlin-reflect`（app）— Kotlin 反射。

说明：依赖版本与坐标集中定义在 `gradle/libs.versions.toml`；各模块具体引用见相应 `build.gradle.kts`。部分依赖通过 BOM 对齐版本（Compose、Firebase、Ktor、Koin）。
