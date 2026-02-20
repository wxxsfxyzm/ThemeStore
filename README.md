# ThemeStore

**ThemeStoreX** 是 **ThemeStore** 的 fork 版本，致力于优化原项目混乱的 UI 和代码架构，同时也用于测试新的项目架构，个人学习用。

---

## 项目简介

ThemeStoreX 仅支持 Android 16（我只有安卓16的小米设备）。

---

## 主要功能清单

- [x] 安装主题
- [x] 网络安装主题
- [x] 日志统计
- [x] 拦截广播
- [x] 常驻通知栏保活
- [ ] 优化模式（暂时无效）
- [x] 主题混搭
- [x] 支持 Shizuku
- [ ] ~Native Development Kit 定期保活（暂未支持）~
- [ ] ~广播剩余时间计算（暂未支持）~
- [ ] ~官方主题解析（暂未支持）~
- [ ] ~三方主题商店（暂未支持）~

---

## 文件复制方法

ThemeStore 利用 `\u200b`（零宽空格 Unicode 字符）构造文件别名，从而实现无需 Shell 权限即可访问 `Android/data` 目录。

---

## 适用安卓版本

- 支持：Android 16

---

## 所需权限说明

为保证功能完整性及流畅体验，ThemeStore 需申请以下系统权限：

- **所有文件管理权限**（管理存储）  
  用于访问和操作本地存储中的主题文件
- **无障碍服务权限**  
  用于拦截部分广播
- **自启动权限**  
  保障软件在系统启动后能够正常工作
- **后台运行权限**  
  确保程序关键服务在后台持续运行
- **通知权限**  
  用于常驻通知栏提示及相关交互
- **网络权限**  
  用于下载主题文件及作者头像图片

---

## 特别说明

本项目仅供学习与技术交流，请勿用于商业用途。部分功能受系统限制，在不同设备和系统版本下表现可能有所差异，敬请谅解。

---

## 开源协议

本项目采用 [GNU Affero General Public License v3.0](LICENSE)（GNU AGPL v3）开源发布。  
您可以自由地使用、修改和分发本项目，但请遵守 AGPL v3 协议条款并保留原作者信息和许可证说明。

---

## 致谢

### 原项目

[GitHub - MerakXingChen/ThemeStore](https://github.com/MerakXingChen/ThemeStore)

### 原作者

[bilibili - MerakXingChen](https://space.bilibili.com/1064893426)

### 图标

[Microsoft FluentUI Emoji](https://github.com/microsoft/fluentui-emoji)
