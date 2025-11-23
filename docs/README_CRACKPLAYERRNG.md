# crackPlayerRNG 功能分析与Forge移植指南

[English](#english) | [中文](#chinese)

<a name="chinese"></a>
## 中文说明

### 概述
本文档集提供了对 `crackPlayerRNG` (在代码中实现为 `/ccrackrng` 命令) 功能的全面分析，以及将其从Fabric移植到Forge的详细指南。

### 什么是 crackPlayerRNG？

`crackPlayerRNG` 是Minecraft客户端模组中的一个高级功能，它能够：
1. **破解玩家随机数生成器种子** - 通过分析抛出物品的轨迹
2. **维持同步** - 通过事件跟踪与服务器RNG状态保持同步
3. **操控随机结果** - 允许玩家通过抛出物品来推进RNG到期望的状态
4. **提供便利功能** - 如无限工具（通过避开不利的随机数来防止工具损坏）

### 文档结构

本仓库包含三个主要文档：

#### 1. CRACKPLAYERRNG_ANALYSIS.md - 实现分析
**路径**: `docs/CRACKPLAYERRNG_ANALYSIS.md`

这份文档详细分析了Fabric版本的实现：

- **架构概述**
  - 命令入口点 (`CrackRNGCommand.java`)
  - 核心破解逻辑 (`CCrackRng.java`)
  - RNG维护系统 (`PlayerRandCracker.java`)
  - 种子恢复算法 (`CCrackRngGen.java`)

- **工作原理**
  - 物品抛掷机制（10个物品，垂直向上）
  - 数据收集（捕获速度向量）
  - 使用LattiCG库进行种子恢复
  - 重试逻辑（最多5次尝试）

- **技术细节**
  - RNG实现（Java线性同余生成器）
  - 事件检测系统（20+种事件类型）
  - Mixin系统集成
  - 任务管理

- **功能特性**
  - RNG操控工具
  - 无限工具功能
  - 工具损坏警告
  - 服务器兼容性检测

#### 2. FORGE_IMPLEMENTATION_GUIDE.md - Forge移植指南
**路径**: `docs/FORGE_IMPLEMENTATION_GUIDE.md`

这是一份全面的移植指南，包含：

- **架构差异分析**
  - Fabric vs Forge对比表
  - 事件系统差异
  - Mixin支持对比
  - 配置系统差异

- **分阶段实施计划**
  - 第1阶段：项目设置
  - 第2阶段：核心组件
  - 第3阶段：事件系统迁移
  - 第4阶段：Mixin vs Forge Hooks
  - 第5阶段：核心算法移植
  - 第6阶段：网络包处理
  - 第7阶段：任务管理系统
  - 第8阶段：测试策略
  - 第9阶段：文档编写
  - 第10阶段：发布

- **详细代码示例**
  - Forge主类实现
  - 命令注册（Forge风格）
  - 配置系统（ForgeConfigSpec）
  - 事件处理器
  - Mixin配置（必要时）
  - 网络包处理

- **最佳实践**
  - 何时使用Forge事件
  - 何时使用Access Transformers
  - 何时必须使用Mixin
  - 性能优化策略

- **工作量估算**
  - 43-63小时总时长
  - 按阶段分解的时间估算
  - 复杂度评估

#### 3. CODE_ASSISTANT_PROMPT.md - 代码助手提示词
**路径**: `docs/CODE_ASSISTANT_PROMPT.md`

这是一个结构化的提示词模板，用于指导AI代码助手进行移植工作：

- **上下文设置**
  - 背景信息
  - 技术概览
  - 核心需求

- **任务定义**
  - 要移植的组件列表
  - 每个组件的输入/输出
  - 关键变更点

- **参考资料**
  - 关键文件引用
  - 算法（可直接移植）
  - 事件映射表

- **开发方法**
  - 6个阶段的详细步骤
  - 每个阶段的具体任务
  - 检查点和验证

- **具体问题**
  - 事件检测策略
  - Mixin使用理由
  - 配置管理
  - 性能考虑
  - 兼容性问题

- **交付成果清单**
  - 代码交付物
  - 文档交付物
  - 测试交付物

- **代码风格指南**
  - Forge约定
  - 可读性标准
  - 错误处理

- **测试策略**
  - 单元测试示例
  - 集成测试检查表
  - 手动测试清单

- **常见陷阱**
  - 要避免的做法
  - 推荐的做法
  - 边界情况处理

### 使用方法

#### 对于想要理解实现的开发者：
1. 阅读 `CRACKPLAYERRNG_ANALYSIS.md` 了解Fabric版本如何工作
2. 研究源代码中引用的关键文件
3. 理解RNG破解的数学原理（LattiCG）

#### 对于想要移植到Forge的开发者：
1. 先阅读 `CRACKPLAYERRNG_ANALYSIS.md` 理解功能
2. 详细学习 `FORGE_IMPLEMENTATION_GUIDE.md`
3. 使用指南中的检查表和代码示例
4. 遵循分阶段实施计划
5. 参考事件映射表进行转换

#### 对于使用AI助手的开发者：
1. 将 `CODE_ASSISTANT_PROMPT.md` 的内容提供给AI
2. 指定你想从哪个组件开始
3. 逐步完成每个阶段
4. 使用提示词中的测试清单验证
5. 参考示例交互模式

### 技术要求

#### Fabric版本（原始实现）
- Minecraft: 1.20.1+
- Fabric Loader: 最新
- Fabric API: 必需
- LattiCG: 用于种子破解
- Mixin: 广泛使用

#### Forge版本（移植目标）
- Minecraft: 1.20.1+
- Forge: 47.x+（或NeoForge最新版）
- LattiCG: 需要shadow/重定位
- Mixin: 最小化使用（首选Forge hooks）

### 关键特性

1. **种子破解**
   - 10次物品抛掷
   - 基于格子的密码分析
   - 95%+成功率（重试后）

2. **RNG维护**
   - 20+种事件类型检测
   - 自动状态同步
   - 意外调用时重置

3. **RNG操控**
   - 按条件抛出物品
   - 无限工具（避免损坏）
   - 预测性工具损坏警告

4. **服务器兼容性**
   - 原版服务器支持
   - 模组服务器检测
   - Paper等插件服务器警告

### 限制

1. **服务器要求**
   - 仅适用于类原版服务器
   - Paper和某些插件会破坏RNG确定性

2. **资源需求**
   - 需要可抛出的物品（推荐64+）
   - 约5秒钟抬头时间

3. **网络要求**
   - 高延迟会降低成功率
   - 差的连接可能需要多次尝试

4. **范围**
   - 仅破解玩家RNG（非世界或其他实体RNG）
   - 某些事件仍会导致重置（1.17+中的经验球）

### 性能考虑

- 事件处理器：高效，最小开销
- 任务管理：节流和优先级系统
- 内存使用：及时清理，使用弱引用
- 典型影响：每个事件<5ms

### 安全说明

这是一个客户端功能：
- ✓ 不提供服务器端PvP优势
- ✓ 不破坏服务器安全
- ✓ 不允许作弊超出客户端预测
- ✓ 服务器仍然验证所有操作

本质上是一个复杂的预测工具，类似于使用外部计算器但集成到客户端中。

### 贡献

如果你想改进这些文档：
1. Fork仓库
2. 创建你的功能分支
3. 提交你的更改
4. 推送到分支
5. 创建Pull Request

### 许可证

这些文档按照与clientcommands主项目相同的许可证分发。参见仓库根目录中的LICENSE文件。

---

<a name="english"></a>
## English Documentation

### Overview
This document set provides comprehensive analysis of the `crackPlayerRNG` (implemented as `/ccrackrng` command) functionality and detailed guidance for porting it from Fabric to Forge.

### What is crackPlayerRNG?

`crackPlayerRNG` is an advanced feature in Minecraft client-side mods that:
1. **Cracks the player's random number generator seed** - By analyzing thrown item trajectories
2. **Maintains synchronization** - With server RNG state through event tracking
3. **Enables RNG manipulation** - Allows players to advance RNG to desired states by throwing items
4. **Provides quality-of-life features** - Like infinite tools (preventing breaking by avoiding bad RNG rolls)

### Document Structure

This repository contains three main documents:

#### 1. CRACKPLAYERRNG_ANALYSIS.md - Implementation Analysis
**Path**: `docs/CRACKPLAYERRNG_ANALYSIS.md`

This document provides detailed analysis of the Fabric implementation:

- **Architecture Overview**
  - Command entry point (`CrackRNGCommand.java`)
  - Core cracking logic (`CCrackRng.java`)
  - RNG maintenance system (`PlayerRandCracker.java`)
  - Seed recovery algorithm (`CCrackRngGen.java`)

- **How It Works**
  - Item throwing mechanism (10 items, straight up)
  - Data collection (velocity vector capture)
  - Seed recovery using LattiCG library
  - Retry logic (up to 5 attempts)

- **Technical Details**
  - RNG implementation (Java Linear Congruential Generator)
  - Event detection system (20+ event types)
  - Mixin system integration
  - Task management

- **Features**
  - RNG manipulation utilities
  - Infinite tools feature
  - Tool break warning
  - Server compatibility detection

#### 2. FORGE_IMPLEMENTATION_GUIDE.md - Forge Porting Guide
**Path**: `docs/FORGE_IMPLEMENTATION_GUIDE.md`

This is a comprehensive porting guide containing:

- **Architecture Differences Analysis**
  - Fabric vs Forge comparison table
  - Event system differences
  - Mixin support comparison
  - Configuration system differences

- **Phased Implementation Plan**
  - Phase 1: Project Setup
  - Phase 2: Core Components
  - Phase 3: Event System Migration
  - Phase 4: Mixin vs Forge Hooks
  - Phase 5: Core Algorithm Port
  - Phase 6: Network Packet Handling
  - Phase 7: Task Management System
  - Phase 8: Testing Strategy
  - Phase 9: Documentation
  - Phase 10: Distribution

- **Detailed Code Examples**
  - Forge main class implementation
  - Command registration (Forge style)
  - Configuration system (ForgeConfigSpec)
  - Event handlers
  - Mixin configuration (when necessary)
  - Network packet handling

- **Best Practices**
  - When to use Forge events
  - When to use Access Transformers
  - When Mixins are necessary
  - Performance optimization strategies

- **Effort Estimation**
  - 43-63 hours total
  - Time breakdown by phase
  - Complexity assessment

#### 3. CODE_ASSISTANT_PROMPT.md - Code Assistant Prompt Template
**Path**: `docs/CODE_ASSISTANT_PROMPT.md`

This is a structured prompt template to guide AI code assistants through the porting work:

- **Context Setup**
  - Background information
  - Technical overview
  - Core requirements

- **Task Definition**
  - List of components to port
  - Input/output for each component
  - Key changes needed

- **Reference Materials**
  - Key file references
  - Algorithms (direct port)
  - Event mapping table

- **Development Approach**
  - Detailed steps for 6 phases
  - Specific tasks for each phase
  - Checkpoints and validation

- **Specific Questions**
  - Event detection strategies
  - Mixin usage justification
  - Configuration management
  - Performance considerations
  - Compatibility concerns

- **Deliverables Checklist**
  - Code deliverables
  - Documentation deliverables
  - Testing deliverables

- **Code Style Guidelines**
  - Forge conventions
  - Readability standards
  - Error handling

- **Testing Strategy**
  - Unit test examples
  - Integration test checklist
  - Manual testing checklist

- **Common Pitfalls**
  - What to avoid
  - Recommended approaches
  - Edge case handling

### Usage

#### For Developers Understanding the Implementation:
1. Read `CRACKPLAYERRNG_ANALYSIS.md` to understand how the Fabric version works
2. Study the key files referenced in the source code
3. Understand the mathematical principles of RNG cracking (LattiCG)

#### For Developers Porting to Forge:
1. First read `CRACKPLAYERRNG_ANALYSIS.md` to understand the functionality
2. Study `FORGE_IMPLEMENTATION_GUIDE.md` in detail
3. Use checklists and code examples from the guide
4. Follow the phased implementation plan
5. Reference the event mapping table for conversions

#### For Developers Using AI Assistants:
1. Provide the content of `CODE_ASSISTANT_PROMPT.md` to the AI
2. Specify which component you want to start with
3. Work through each phase step by step
4. Use the testing checklist in the prompt for validation
5. Reference the example interaction patterns

### Technical Requirements

#### Fabric Version (Original Implementation)
- Minecraft: 1.20.1+
- Fabric Loader: Latest
- Fabric API: Required
- LattiCG: For seed cracking
- Mixin: Extensively used

#### Forge Version (Port Target)
- Minecraft: 1.20.1+
- Forge: 47.x+ (or latest NeoForge)
- LattiCG: Requires shadow/relocation
- Mixin: Minimized usage (prefer Forge hooks)

### Key Features

1. **Seed Cracking**
   - 10 item throws
   - Lattice-based cryptanalysis
   - 95%+ success rate (after retries)

2. **RNG Maintenance**
   - 20+ event type detection
   - Automatic state synchronization
   - Reset on unexpected calls

3. **RNG Manipulation**
   - Conditional item throwing
   - Infinite tools (avoid breaking)
   - Predictive tool break warning

4. **Server Compatibility**
   - Vanilla server support
   - Modded server detection
   - Warning for Paper and similar plugins

### Limitations

1. **Server Requirements**
   - Only works on vanilla-like servers
   - Paper and some plugins break RNG determinism

2. **Resource Requirements**
   - Needs items to throw (64+ recommended)
   - ~5 seconds looking up

3. **Network Requirements**
   - High latency reduces success rate
   - Poor connections may need multiple attempts

4. **Scope**
   - Only cracks player RNG (not world or other entity RNG)
   - Some events still cause resets (XP orbs in 1.17+)

### Performance Considerations

- Event handlers: Efficient, minimal overhead
- Task management: Throttling and priority system
- Memory usage: Prompt cleanup, weak references
- Typical impact: <5ms per event

### Security Notes

This is a client-side feature that:
- ✓ Does NOT give server-side PvP advantages
- ✓ Does NOT break server security
- ✓ Does NOT allow cheating beyond client prediction
- ✓ Server still validates all actions

It's essentially a sophisticated prediction tool, similar to using external calculators but built into the client.

### Contributing

If you want to improve these documents:
1. Fork the repository
2. Create your feature branch
3. Commit your changes
4. Push to the branch
5. Create a Pull Request

### License

These documents are distributed under the same license as the main clientcommands project. See the LICENSE file in the repository root.
