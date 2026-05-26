# ThemeStoreX

[![License: AGPL v3](https://img.shields.io/badge/License-AGPLv3-blue.svg)](https://www.gnu.org/licenses/agpl-3.0)
[![Android](https://img.shields.io/badge/Android-16%20only-3DDC84.svg)](https://developer.android.com/)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.3.21-7F52FF.svg)](https://kotlinlang.org/)

> 面向 Android 16 小米 HyperOS 环境的主题安装与辅助工具。

ThemeStoreX 是 [ThemeStore](https://github.com/MerakXingChen/ThemeStore) 的 fork 版本，目标是把主题安装、主题应用、运行保活、日志统计和相关系统权限流程整理成一个更稳定、更易维护的个人学习项目。

本项目不是通用 Android 主题商店，也不承诺适配所有 Android 版本、ROM 或设备。当前代码和测试边界集中在 Android 16 与小米主题管理器行为上。

## 核心能力

- **本地主题安装：** 从设备存储选择 `.mtz` 文件，复制到小米主题管理器可识别的位置并发起应用。
- **外部文件接收：** 支持通过系统文件分享 / 打开方式接收 `.mtz` 文件，并弹出安装选项。
- **在线主题安装：** 输入主题文件下载链接，下载后自动进入安装流程。
- **主题模块选择：** 安装时可选择框架、壁纸、图标、字体、锁屏、状态栏等主题模块，也可一键全选。
- **Shizuku 增强路径：** 在 Shizuku 可用且授权后，通过用户服务写入受限目标文件，并以特权方式启动主题应用流程。
- **无 Shell 文件路径：** 无 Shizuku 时使用零宽空格 `\u200b` 构造 `Android/data` 别名路径，尽量避免依赖 Shell 或 Root。
- **广播监听与日志：** 通过无障碍服务动态注册广播接收器，监听 `miui.intent.action.CHECK_TIME_UP`，记录触发时间和统计数据。
- **常驻通知保活：** 可选开启前台服务通知，展示安装 / 拦截统计，并配合无障碍服务维持关键流程运行。
- **快捷设置磁贴：** 提供 QS Tile 入口，用于快速切换或查看保活相关状态。
- **个性化界面：** 基于 Jetpack Compose、Miuix，支持深浅色、动态取色、自定义主题色、模糊效果和悬浮底栏。

## 支持范围

- **目标系统：** Android 16
- **主要环境：** 小米 / MIUI / HyperOS 风格系统与小米主题管理器
- **应用包名：** `com.merak.x`
- **最低 SDK：** 35
- **编译 / 目标 SDK：** 37.0

其他系统版本、其他 ROM、非小米主题管理器或修改较大的系统环境可能无法正常工作。涉及主题目录、广播行为、隐藏 API、前台服务和无障碍权限的功能都依赖具体 ROM 行为。

## 权限说明

ThemeStoreX 会围绕主题安装和保活流程申请或使用以下权限 / 能力：

- **所有文件管理权限：** 读取本地主题文件，并写入主题管理器相关目录。
- **通知权限：** 显示常驻保活通知和运行统计。
- **无障碍服务：** 动态注册广播接收器，并作为保活链路的一部分。
- **Shizuku 授权：** 执行受限文件写入、权限授予、AppOps 设置和特权启动等高级操作。
- **前台服务权限：** 维持可见的保活服务通知。
- **快捷设置磁贴：** 提供系统 QS Tile 入口。
- **网络权限：** 下载在线主题文件。
- **应用列表 / 安装相关权限：** 与主题管理器、Shizuku 和系统组件交互时使用。

这些权限都和主题安装、状态检测或保活流程相关。请不要在不了解 ROM 行为的情况下扩大权限范围或改成 Root / Shell 常驻方案。

## 工作方式

### 普通模式

普通模式优先使用零宽空格路径别名：

```text
/sdcard/Android/data/com.android.thememanager/files/theme/安装主题.mtz
```

实际写入时会在 `Android/` 后插入 `\u200b` 构造别名路径，从而绕过部分系统对
`Android/data` 的直接访问限制。随后应用会启动小米主题管理器的主题应用 Activity，并携带主题模块 flags。

### Shizuku 模式

当 Shizuku 正在运行且应用已授权时，ThemeStoreX 会切换到 Shizuku 安装路径：

- 通过用户服务获取目标主题文件的可写 `ParcelFileDescriptor`；
- 将本地文件、Content Uri 或在线下载流写入目标路径；
- 使用特权 IPC 启动主题管理器的应用流程；
- 可辅助授予通知、存储、无障碍相关权限和 AppOps。

如果 Shizuku 不可用或未授权，会自动回退到普通模式。

### 广播监听

无障碍服务启动后会动态注册接收器，当前实际监听：

```text
miui.intent.action.CHECK_TIME_UP
```

该逻辑用于记录和统计 MIUI 定时广播触发情况。普通广播无法真正阻断传递；只有有序广播才可能被
`abortBroadcast()` 中止，且是否生效取决于系统发送方式和 ROM 限制。

## 构建项目

ThemeStoreX 是 Android Gradle 项目，当前包含两个 Gradle 模块：

- `:app`
- `:hidden-api`

### 环境要求

- **JDK 25**，并正确配置 `JAVA_HOME`。
- Android SDK / Android Studio，安装 SDK 37 平台和对应构建工具。
- 使用仓库自带 Gradle Wrapper。
- 用于解析 snapshot `miuix` 依赖的 GitHub Packages 凭据。

### GitHub Packages 认证

`miuix` snapshot 依赖从 GitHub Packages 解析，即使公开包也需要认证。请在全局 Gradle 配置中添加 GitHub 用户名和带
`read:packages` 权限的 classic token。

Windows:

```text
%USERPROFILE%\.gradle\gradle.properties
```

Linux / macOS:

```text
~/.gradle/gradle.properties
```

```properties
gpr.user=YOUR_GITHUB_USERNAME
gpr.key=YOUR_PERSONAL_ACCESS_TOKEN
```

也可以使用环境变量：

```text
GITHUB_ACTOR=YOUR_GITHUB_USERNAME
GITHUB_TOKEN=YOUR_TOKEN
```

不要把凭据写进仓库文件或提交到 Git。

### 签名配置

`app/build.gradle.kts` 会读取根目录 `keystore.properties`，并将其用于 debug 和 release 签名配置。该文件属于本地敏感配置，不应公开提交真实密码或私钥材料。

### 构建命令

Windows / PowerShell:

```powershell
.\gradlew.bat assembleDebug
```

Linux / macOS:

```bash
./gradlew assembleDebug
```

更窄的 Kotlin 编译检查：

```powershell
.\gradlew.bat :app:compileDebugKotlin
```

## 常见问题

### 为什么只支持 Android 16？

这是项目当前的真实测试边界。主题管理器路径、广播行为、无障碍策略、隐藏 API 和前台服务限制都强依赖系统版本与 ROM。扩大兼容范围需要针对具体设备验证，而不是简单放宽
`minSdk` 或删除版本假设。

### 没有 Shizuku 能安装主题吗？

可以尝试普通模式。普通模式使用零宽空格路径别名写入小米主题管理器目录，不依赖 Root 或 Shell 常驻。但该行为依赖 ROM 文件访问实现，不保证所有设备都可用。

### Shizuku 有什么作用？

Shizuku 用于增强受限路径写入、权限授予、AppOps 设置和特权启动能力。它不是 Root 替代品的泛化入口，本项目只在明确需要的主题安装与保活流程中使用它。

### 为什么需要无障碍权限？

无障碍服务承担广播监听和保活链路的一部分。服务启动后会动态注册广播接收器，并通过极小的 accessibility overlay 维持服务状态。不开启无障碍时，广播监听和相关状态统计不会完整工作。

### 手动发广播怎么测试？

可用 adb 测试接收器是否触发：

```powershell
adb shell am broadcast --user current -a miui.intent.action.CHECK_TIME_UP
```

这只能验证动态 receiver 是否收到广播。能否真正中止广播取决于系统是否发送有序广播；部分设备的 `am broadcast` 不支持手动发送 ordered broadcast。

### 网络安装会做什么？

应用会从输入 URL 下载主题文件流，写入主题管理器目标路径，然后发起主题应用请求。网络权限只用于这类在线主题安装能力。

## 项目结构

```text
app/          Android 应用主体
hidden-api/   隐藏 API 声明和编译期辅助
gradle/       Gradle Wrapper 与版本目录
keystore/     本地签名材料目录
```

主要 Kotlin 包：

- `core/`：主题安装、Shizuku、反射和平台集成。
- `data/`：DataStore 设置与数据模型。
- `di/`：Koin 依赖注入模块。
- `service/`：无障碍、保活服务和快捷设置磁贴。
- `ui/`：Compose 页面、导航、主题、组件和图标。
- `util/`：日志、崩溃处理和通用工具。

## 风险说明

ThemeStoreX 会调用系统组件、隐藏 API、Shizuku 用户服务、无障碍服务、前台服务和主题管理器私有入口。这些行为可能在系统更新、ROM 差异或权限策略变化后失效。

使用前请确认你了解以下风险：

- 主题应用可能失败、部分应用或状态不一致。
- 错误的主题文件可能导致界面显示异常，需要进入系统主题管理器恢复。
- Shizuku / AppOps / 无障碍相关操作可能被系统安全策略限制。
- 保活行为可能增加后台驻留和通知占用。

本项目以学习和个人设备验证为主，不提供商业使用支持，也不对所有设备兼容性作保证。

## 开源协议

ThemeStoreX 基于 [GNU Affero General Public License v3.0](LICENSE) 开源。

你可以在 AGPL v3 条款下使用、修改和分发本项目。若基于本项目继续开发或发布，请保留许可证和原作者信息，并遵守对应源码版本的开源协议要求。

## 致谢

- 原项目：[MerakXingChen/ThemeStore](https://github.com/MerakXingChen/ThemeStore)
- 原作者：[MerakXingChen - bilibili](https://space.bilibili.com/1064893426)
- 文档结构参考：[InstallerX Revived](https://github.com/wxxsfxyzm/InstallerX)
- 特权能力：[RikkaApps/Shizuku](https://github.com/RikkaApps/Shizuku)
- UI 组件：[compose-miuix-ui/miuix](https://github.com/compose-miuix-ui/miuix)
- 图标素材：[Microsoft FluentUI Emoji](https://github.com/microsoft/fluentui-emoji)
