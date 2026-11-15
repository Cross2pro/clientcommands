# 项目目标与任务清单

> 仓库：`Cross2pro/clientcommands` 的 `forge` 分支
> 代码状态截至：2026-09-28（提交 `6a56f66b`）；本文档补写于 2026-11-15
> 状态口径：本文只把有构建日志 / 提交记录支撑的事项标为「已完成」；游戏内实测尚无通过结论，不在此虚报。

## 1. 总目标

把 Fabric 版 ClientCommands 移植为 **Minecraft Forge 1.20.1 客户端 MOD**，且**只保留 RNG 相关功能**，
攒出一个可在 Windows 上直接下载 JAR、装进 `mods/` 就能测试的成品。

- 必须用 **Forge，不是 NeoForge**
- 映射使用 Mojang 官方映射（Yarn → Mojang 需逐项翻译）
- 原项目作者 Earthcomputer，许可证 **LGPL-3.0-only**，移植须保留版权与许可证说明

## 2. 范围

### 保留（RNG 功能）

| 命令 | 功能 |
|---|---|
| `/ccrackrng` | 扔物品破解玩家 RNG / 世界种子 |
| `/cenchant` | 附魔台附魔预测 |
| `/cfish` | 钓鱼 RNG 操纵 |
| `/cchorus` | 紫颂果传送操纵 |
| `/cpredictbrushables` | 预测可刷方块（新加；完整预测仅限单人集成服务器，见第 5 节） |

另保留：Player RNG 状态追踪、RNG 事件 Mixin、钓鱼所需的本地延迟逻辑、渲染队列。

### 排除（明确不做）

- `/cping`、waypoints、c2c、配置 GUI 等一切非 RNG 命令 / 功能
- Fabric-only 依赖：`betterconfig`、`clientarguments`、`simplewaypoints`
- 自动重连 `Relogger`

## 3. 技术路线与已定决策

- **构建（当前）**：简化构建，不走 ForgeGradle——直接用官方映射 JAR 作 `compileOnly`，
  Mixin 注解处理器关闭（`-proc:none`），SeedFinding 在 `jar` 任务中 shade 打入。
  这是为绕开本机网络 / ForgeGradle 环境问题定的临时路线，**不是最终生产构建**（见任务 13）。
- **少用 Mixin、优先 Forge 事件**（2026-09-28 定）：能用事件实现的就不写 Mixin。
  已按此把 `MixinMinecraftClient` 删除，其两项职责改由 `RngCommands` 的
  `ClientTickEvent` 承担：每 tick 调 `RenderQueue.tick()`，并检测 `Minecraft.level`
  变化时调 `PlayerRandCracker.onRecreatePlayer()`。
  其余 RNG 追踪 Mixin 注入点精确到具体方法调用 / 字段读取（如 `RandomSource.nextInt` 调用前、
  `Level.isClientSide` 读取处），事件粒度替代不了，暂时保留（当前 mixin 配置共 26 项，
  含 2 个 accessor）。
- **Access Transformer**：用新版 `#` 格式；成员名已逐项对官方映射核实
  （如 `Entity#isInvulnerableTo`，旧稿的 `isInvulnerableToBase` 在 1.20.1 不存在）。
- **发布方式**：代码推 `forge` 分支；GitHub Release `forge-1.0.0` 已建。
  Release 附件上传接口返回 401，故成品 JAR 直接提交在仓库 `dist/rngcommands-1.0.0.jar`，
  Release 说明里链到该文件，Windows 浏览器可直接下载。

## 4. 任务清单

### 已完成

| # | 任务 | 证据 |
|---|---|---|
| 1 | 依赖盘点，确认可移植范围 | `INVENTORY.md`（本地工作区） |
| 2 | 源码移植：command / features / mixin 的 Yarn→Mojang 翻译修复 | 诊断编译通过 |
| 3 | 新增 `/cpredictbrushables` 并注册 | 源码 + 编译通过 |
| 4 | 删除 `/cping` 及未用的非 RNG 代码 | 源码 |
| 5 | 诊断性纯 Java 编译：`compileJava` 0 errors | 构建日志（2026-09-26） |
| 6 | JAR 打包：SeedFinding shade、Mixin 配置、manifest | `dist/rngcommands-1.0.0.jar`（202 KB） |
| 7 | 许可证处理：`LICENSE`（LGPL）、`NOTICE.txt`、作者标注 | 项目根目录 |
| 8 | 修复 `accesstransformer.cfg`：`#` 格式 + `isInvulnerableTo` 改名 | 提交 `05dfcdde` / `e85dfd3b` |
| 9 | 加空 refmap 并在 mixins 配置中引用 | 提交 `a2cbdc9f` |
| 10 | `MixinMinecraftClient` 改 Forge 事件实现并删除该 Mixin | 提交 `6a56f66b` |
| 11 | 发布到 GitHub：`forge` 分支 + Release `forge-1.0.0` + JAR 直达链接 | 分支 head `6a56f66b`，关键文件与本地 blob SHA 逐项一致（2026-11-15 复核） |

### 待办 / 未验证（按优先级）

| # | 任务 | 说明 |
|---|---|---|
| 12 | **游戏内实测** | 装 Forge 47.4.23 + 本 JAR，单人创造世界逐个测 5 条命令；有问题回传 `.minecraft/logs/latest.log`。目前只收到过 AT 语法报错和一次 Mixin 注入报错，均已修；事件版 JAR 尚无实测结论 |
| 13 | 恢复标准 ForgeGradle 构建 | 从干净 1.20.1 MDK 重建 `build.gradle`，恢复 Mixin 注解处理器、refmap 生成与 reobf，做一次标准 `gradle build` |
| 14 | 其余 Mixin 运行时验证 | 逐项核对注入点 descriptor、`isClientSide` FIELD 注入、`triggerItemUseEffects` 合并注入、容器关闭后的掉落追踪偏差等 |
| 15 | `/cpredictbrushables` 补全 | 维度目前写死 `Level.OVERWORLD`（有 TODO）；多人服务器拿不到未揭示的战利品种子，只能显示未知，需在说明里讲清 |
| 16 | 渲染效果复核 | `RenderQueue.NO_DEPTH_LAYER` 为绕开受保护 API 暂简化为 `RenderType.lines()`，原「穿墙」效果可能丢失，需实测或改用正确的 Forge 渲染 API |
| 17 | 资源与许可证收尾 | `en_us.json` 仍含原项目大量非 RNG 文案，需精简；根目录 `LICENSE.txt`（Forge 模板）与 `LICENSE` 并存，需处理；核对最终 JAR 内含 LICENSE / NOTICE |
| 18 | 三个重建文件的逻辑复核 | `FishCommand`、`CEnchantCommand`、`CrackRNGCommand` 曾被误操作损坏后手工重建，目前仅确认可编译，逻辑未复核 |

## 5. 测试方法

1. 环境：Minecraft 1.20.1 + Forge 47.4.23 + Java 17；首次测试只放这一个 MOD，排除冲突。
2. 安装：把 `rngcommands-1.0.0.jar` 放进 `.minecraft/mods/` 后启动。
3. 逐命令测：单人创造世界里依次试 `/ccrackrng`（扔约 20 个物品看能否破种）、
   `/cenchant`（开附魔台看预测）、`/cfish`、`/cchorus`、`/cpredictbrushables`。
4. 排查：看 `.minecraft/logs/latest.log`（崩溃看 `crash-reports/`），搜 `rngcommands` 确认已加载、
   搜 `Mixin` 看注入报错；把报错段落回传即可定位到具体 Mixin 类与方法。

## 6. 已知风险

- 当前 JAR 为**无 Mixin 注解处理器**的简化构建产物，refmap 为空文件；
  Forge 1.20.1 运行时用 Mojang 名、与源码一致，理论上可直接匹配，但注入点未经运行时证明。
- 反射访问服务端字段 / 方法的路径（如可刷方块预测）在 reobf 后的可用性未验证。
- SeedFinding 的完整运行时依赖闭包（Kotlin 等）尚未在游戏内验证。

## 7. 构建环境（复现用）

- Minecraft 1.20.1 / Forge 47.4.23 / ForgeGradle 6.0.24（标准构建目标）
- JDK 17、Gradle 8.8
- SeedFinding `mc_feature:1.171.7`
- 本地映射产物在 `libs/mc/`（官方映射 JAR 与 mappings，体积大不入库，可用 `libs/mc/JarRemapper.java` 再生）
