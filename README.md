# RNG Commands (Forge 1.20.1)

Forge 1.20.1 客户端 MOD，只包含 RNG 相关功能。从 [clientcommands](https://github.com/Earthcomputer/clientcommands) 移植，原作者 Earthcomputer，LGPL-3.0 许可证。

## 功能

| 命令 | 说明 |
|------|------|
| `/ccrackrng` | 通过扔物品破解玩家 RNG 种子 |
| `/cenchant` | 附魔台附魔预测 |
| `/cfish` | 钓鱼 RNG 操纵 |
| `/cchorus` | 紫颂果传送操纵 |
| `/cpredictbrushables` | 预测可刷方块（单人游戏） |

## 环境要求

- Minecraft 1.20.1
- Forge 47.4.23
- Java 17

## 安装

从 [Releases](../../releases) 下载 `rngcommands-1.0.0.jar`，放入 `.minecraft/mods/` 文件夹。

## 从源码构建

本项目使用简化构建（`build.gradle`），不依赖 ForgeGradle，避免网络问题：

```bash
export JAVA_HOME=<你的JDK17路径>
./gradlew jar --offline
```

构建产物在 `build/libs/rngcommands-1.0.0.jar`。

构建需要本地 Minecraft 映射 JAR，放在 `libs/mc/` 下（`client-official.jar`、`server-official.jar` 及映射文件）。这些文件体积较大，不提交到仓库，可用 `libs/mc/JarRemapper.java` 从官方映射重新生成。

## 许可证

LGPL-3.0-only，见 [LICENSE](LICENSE)。
