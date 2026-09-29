# GTM Advanced Terminal｜独立版高级终端

从 [GTMThings](https://github.com/liansishen/GTMThings) 高级终端功能衍生的独立模组，方便只需要高级终端的玩家使用。

本项目是第三方衍生项目，**不是 GTMThings 官方版本**。

## 版本

| Minecraft | 加载器与目标模组 | 项目版本 | 说明 |
| --- | --- | --- | --- |
| 1.20.1 | Forge / GregTech Modern | 1.0.1 | 从 GTMThings 1.6.0 提取高级终端功能并适配 |
| 1.12.2 | Forge / GTCEu，面向 GT Lite | 1.0.0-forge-1.12.2 | 重新实现相关功能，沿用上游终端纹理 |

两个版本使用不同的 API，不能互相替代。请按 Minecraft 版本和加载器选择对应文件。

## 功能

两个版本的核心功能包括：

- 自动建造多方块结构。
- 选择线圈等级。
- 设置可重复结构的重复次数。
- 无仓室模式。
- 线圈替换。
- 从 AE 网络获取建造材料。

1.12.2 版还提供：

- 附属结构数量设置，支持分阶段补建。
- 镜像模式。
- 搜索 Bubbles/baubles 饰品栏中的 AE 无线终端。

## 使用方式

- 手持高级终端，潜行右键多方块控制器：建造结构。
- 手持高级终端，右键空气：打开设置界面。
- 非潜行右键控制器：保留机器原有交互。

AE 材料供给需要开启相应设置，并使用已绑定网络、具备电量且满足访问范围要求的无线终端。

## 下载与源码

发布后，文件将提供在本仓库的 [Releases](https://github.com/Heartlesx/GTM-Advanced-Terminal/releases) 页面。

分发编译后的 JAR 时，请同时提供对应版本的源码包，或可下载的对应源码链接，并保留许可证和来源说明。源码包包含源码、资源和构建文件，不包含游戏文件或第三方依赖 JAR。

当前仓库正在整理，源码和发布文件将后续上传。

## 测试状态

1.12.2 当前分发包已完成离线构建及归档内容检查，尚未完成全部游戏内回归验证，请按测试版使用。

构建成功不代表所有游戏内功能均已验证。1.20.1 版的运行情况也需要依据对应版本的实测结果判断。

反馈问题时，请提供：

- Minecraft、Forge、GTCEu/GregTech Modern 及本模组版本。
- 复现步骤、终端设置和现场截图。
- 相关日志；1.12.2 版可重点查找 `GTM-AT` 诊断行。

## 许可证与来源

本项目遵循 **GNU LGPL v3.0** 分发。

- 上游项目：[GTMThings](https://github.com/liansishen/GTMThings)
- 上游作者：[liansishen](https://github.com/liansishen)
- 来源版本：GTMThings 1.6.0。
- 开发过程中使用了 Codex 辅助。
- [作者授权确认](https://github.com/liansishen/GTMThings/issues/127#issuecomment-5870569378)：作者确认 LGPLv3.0 涵盖高级终端源码和纹理等资源，并允许按 LGPLv3 要求分发独立版及 1.12.2 移植版，包括分享 JAR 和公开下载。

发布包附带 `LICENSE`（LGPLv3）、`COPYING`（GPLv3）和 `NOTICE`（来源及修改说明）。相关许可证文本和署名应在后续分发时保留。

Minecraft、Forge、GTCEu、AE2 等第三方项目各自遵循其许可证，不包含在本模组的授权范围内。
