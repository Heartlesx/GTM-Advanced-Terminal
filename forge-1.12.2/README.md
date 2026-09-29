# 1.12.2 版构建与安装说明

当前版本：`1.0.0-forge-1.12.2`。本版面向 Minecraft 1.12.2、Forge 和 GTCEu，开发目标整合包为 GT Lite。

这是 GTMThings 高级终端功能的第三方移植版，沿用上游终端纹理。原作者 liansishen 已在[授权回复](https://github.com/liansishen/GTMThings/issues/127#issuecomment-5870569378)中确认代码和纹理遵循 LGPLv3.0，允许独立版及移植版分发。

## 下载与安装

在 [Releases](https://github.com/Heartlesx/GTM-Advanced-Terminal/releases) 中下载以 `forge-1.12.2.jar` 结尾的文件，手动放入对应 1.12.2 实例的 `mods` 目录。

源码 ZIP 用于查看、修改和构建，不要把它放入游戏 `mods` 目录。不要将本版 JAR 用于 Minecraft 1.20.1。

运行需要 GTCEu、ModularUI、AE2 及这些模组自身所需的依赖。下面列出的版本是当前编译基线，不代表所有其他版本均兼容。Bubbles/baubles 为可选集成，仅在使用饰品栏 AE 无线终端时需要。

## 构建环境

- JDK 8。
- Gradle 4.9，使用本目录中的 wrapper。
- 当前验证使用 ForgeGradle 3.0.197。
- Forge 14.23.5.2860，Minecraft 1.12.2，MCP stable 39-1.12。

在本目录下创建 `libs`，从各项目官方发布渠道获得下列编译依赖：

| 文件名 | 来源 |
| --- | --- |
| `gregtech-1.12.2-2901.jar` | [GTCEu](https://github.com/GregTechCEu/GregTech) |
| `modularui-3.0.6.jar` | [ModularUI](https://www.curseforge.com/minecraft/mc-mods/modularui) |
| `ae2-uel-v0.56.7.jar` | [AE2 UEL](https://github.com/AE2-UEL/Applied-Energistics-2) |
| `CodeChickenLib-1.12.2-3.2.3.358-universal.jar` | [CodeChickenLib](https://www.curseforge.com/minecraft/mc-mods/codechicken-lib-1-8) |
| `EnderCore-1.12.2-0.5.78.jar` | [EnderCore](https://www.curseforge.com/minecraft/mc-mods/endercore) |
| `annotations-13.0.jar` | Gradle 4.9 发行包中的 `lib/annotations-13.0.jar` |

第三方依赖 JAR 不随本仓库和源码包分发，也不会嵌入本模组 JAR。

修改 `gradle.properties` 中的 `org.gradle.java.home`，将其设为你自己的 JDK 8 路径，同时设置 `JAVA_HOME` 并将 JDK 8 的 `bin` 加入 `Path`。可以通过 `GRADLE_USER_HOME` 指定自己的 Gradle 缓存。

当前脚本的 ForgeGradle 版本为 `3.+`；如需与本次构建保持一致，请在 `build.gradle` 中改为 `3.0.197`。

在本目录执行：

```powershell
.\gradlew.bat build sourceDistribution --console=plain
```

首次构建需要联网获取 wrapper、Forge 和 Gradle 依赖；缓存完整后可增加 `--offline`。

输出位置：

- `build/libs`：重混淆后的游戏 JAR。
- `mods输出`：同内容 JAR 和对应源码 ZIP。

构建不会自动复制到真实游戏目录。

## 使用与测试状态

潜行右键控制器进行建造，右键空气打开设置界面。

当前功能版本已由用户长期在游戏内使用，主要功能正常。发布包仅补充许可证与来源说明，功能代码与该实测版本一致；同时已完成离线构建、许可证内容检查及源码对应检查。游戏内使用结论针对用户所使用的整合包环境。

反馈问题时请附设置、复现步骤、现场截图和 `latest.log` 中的 `GTM-AT` 日志。

## 再分发

分享 JAR 时应同时提供对应源码 ZIP，或可下载的对应源码链接，并保留 `LICENSE`、`COPYING` 和 `NOTICE`。完整许可证文本保留英文原文；来源及修改详情见 `NOTICE`。
