# Open Blacksouls

> 一个受 **Black Souls 系列** 启发，基于 **Minecraft Forge 1.20.1** 制作的同人 RPG 改造模组。
> A Minecraft Forge mod inspired by the atmosphere, mechanics, and dark fantasy of Black Souls.

[![License](https://img.shields.io/badge/License-LGPL--3.0-blue.svg)](./LICENSE.txt)
[![Minecraft](https://img.shields.io/badge/Minecraft-1.20.1-green.svg)](https://www.minecraft.net/)
[![Forge](https://img.shields.io/badge/Forge-47.x-orange.svg)](https://files.minecraftforge.net/)
[![Java](https://img.shields.io/badge/Java-17+-red.svg)](https://www.oracle.com/java/)

---

## 写在前面
从 **2026 年 2 月 28 日** 到 **2026 年 7 月 10 日**，这个项目陪我走过了 132 天。  
这个项目最开始只是我想把Black Souls II里那些让我印象很深的机制、技能和角色，搬进Minecraft里。本项目的UI和技能均来源于原作一比一复刻。

从最早的技能、蓝量、篝火，到后来的誓约、Boss（虽然只有一两个，因为实在是太难写了）、武器特效、GUI、Mixin、渲染和附属 API，它慢慢变成了一个比我一开始预想中大得多的东西。
我在里面塞了很多热情、很多尝试，也留下了不少现在回头看会觉得青涩甚至混乱的代码。

由于本人已经毕业，要和生活对线了。所以，与其让它一直躺在我的硬盘里，不如把它开放出来。
如果你只是想看看一个同人Mod是怎么从零散想法堆成一个完整项目的，欢迎翻翻。
如果你想继续改、继续做下去、或者只是从里面拆点有用的实现，也欢迎。

这不是一个完美的项目。
但它确实是我一段时间里的全部认真。


---

## 项目状态

> 当前项目以 **开源归档 / 学习参考 / 二次开发基础** 为主。

- 开始时间：**2026 年 2 月 28 日**
- 开源时间：**2026 年 7 月 10 日**
- 开发周期：**132 天**
- 目标版本：Minecraft **1.20.1**
- 加载器：Minecraft Forge **47.x**
- Java 版本：**Java 17+**
- 维护状态：不保证长期活跃维护
- PR / Issue：可以提交，但不保证即时处理
- 资源版权：请查看下方“版权与素材说明”

---

## 简介

Open Blacksouls 是一个将 Black Souls II 风格玩法引入 Minecraft 的同人 RPG 改造模组。

它包含独立的玩家属性、技能、蓝量、誓约、篝火、Boss、武器、道具、渲染特效与客户端 GUI 等系统，尝试在 Minecraft 中还原一种偏黑暗童话、类魂、RPG 化的游玩体验。

---

## 功能特性

### 玩家系统

* **属性系统**：HP / MP / Stamina 等玩家属性
* **等级成长**：围绕玩家数据进行成长与扩展
* **蓝量机制**：技能施放消耗 MP
* **动态难度**：怪物属性可随玩家成长进行调整

### 技能系统

* **独立技能 GUI**
* **技能学习与解锁**
* **快捷键施放**
* **MP 消耗与冷却**
* **开放技能注册 API**
* **支持附属模组动态扩展技能**

### 世界与交互

* **篝火系统**：存档点、休息、传送
* **誓约系统**：多档誓约等级，影响剧情与战斗
* **自定义道具**：消耗品、戒指、技能书、灵魂
* **自定义武器**：部分武器带有独特机制与渲染特效

### 实体与 Boss

* 自定义实体与 Boss
* 示例实体：

  * Hell Prince
  * Noden

### 客户端与渲染

* 自定义 GUI
* 自定义 Tooltip
* 手持物品描边
* Shader 文字
* 武器 / Boss / 技能视觉特效
* 客户端渲染覆盖

### 底层实现

* Forge Capability 玩家 / 世界数据
* 网络包同步
* Mixin 注入
* 实体 Tick 操控
* 时间停止相关逻辑
* 客户端与服务端分离处理

---

## 安装

### 前置要求

| 依赖              |     版本 | 是否必选 |
| --------------- | -----: | :--: |
| Minecraft       | 1.20.1 |  必选  |
| Minecraft Forge |   47.x |  必选  |
| Java            |    17+ |  必选  |

### 安装步骤

1. 安装 Minecraft **1.20.1**
2. 安装 Forge **47.x**
3. 将编译好的 `blacksouls-*.jar` 放入 `.minecraft/mods/`
4. 启动游戏

### 可选依赖

以下模组主要用于地图专属维度或部分装饰内容，不安装不影响核心玩法：

* Decocraft
* Macaw's Windows
* Macaw's Stairs
* LandK Doors and Windows

---

## 构建

```bash
# 克隆仓库
git clone <仓库地址>
cd Open-Blacksouls

# 构建 Release JAR
./gradlew build
```

构建产物位于：

```text
build/libs/
```

### 开发环境

```bash
# 生成 IDE 运行配置
./gradlew genEclipseRuns
./gradlew genIntellijRuns

# 启动开发客户端
./gradlew runClient
```

建议：

* 使用 JDK 17
* 给 Gradle 分配至少 4GB 内存
* 使用 IntelliJ IDEA 进行开发

---

## 项目结构

```text
src/main/java/com/BlackSouls/BlackSoulsMod/
├── BlackSouls.java              # 主类
├── BSConfig.java                # 配置
├── capability/                  # 玩家 / 世界数据
│   ├── BSPlayerStats
│   ├── BSWorldData
│   └── BonfireEntry
├── client/                      # 客户端代码
│   ├── gui/                     # 技能面板、篝火菜单、誓约、商店等
│   ├── render/                  # 手持描边、Boss 渲染、VFX
│   └── tooltip/                 # 自定义 Tooltip
├── entity/                      # 自定义实体
├── handler/                     # 事件处理器
├── item/                        # 物品
│   ├── accessories/             # 饰品 / 防具
│   ├── consumables/             # 消耗品
│   ├── rings/                   # 戒指
│   ├── weapon/                  # 武器
│   ├── skillbook/               # 技能书
│   └── soul/                    # 灵魂
├── mixin/                       # Mixin 注入
├── network/                     # 网络包
├── potion/                      # 药水效果
├── sound/                       # 音效注册
└── util/                        # 工具类
    └── skill/                   # 技能系统
        ├── AbstractSkill
        └── SkillRegistry
```

---

## 开放技能注册 API

Open Blacksouls 提供动态技能注册接口。
附属模组可以通过继承 `AbstractSkill` 并注册到 `SkillRegistry` 来添加自定义技能，无需 Mixin 或硬编码进本体。

```java
public class MySkill extends AbstractSkill {
    @Override
    public String getSkillId() {
        return "mymod_skill_example";
    }

    @Override
    public String getTranslationKey() {
        return "skill.mymod.example";
    }

    @Override
    public int getManaCost() {
        return 30;
    }

    @Override
    public ResourceLocation getIcon() {
        return new ResourceLocation("mymod", "textures/gui/skills/example.png");
    }

    @Override
    public boolean isUnlockedForGUI(Player player) {
        // 返回玩家是否已学习此技能
        return MyModCapabilities.hasLearned(player, getSkillId());
    }
}
```

注册技能：

```java
SkillRegistry.register(new MySkill());
```

注册后，该技能会自动出现在 Open Blacksouls 的技能 GUI 中，并支持快捷键绑定与施放。

---

## 许可证

本项目基于 [LGPL-3.0](./LICENSE.txt) 许可证开源。

你可以：

* 自由使用、修改、分发本模组
* 在遵守 LGPL-3.0 的前提下进行二次开发
* 将本模组作为依赖开发附属模组

需要注意：

* 对本模组源码本身的修改需要按 LGPL-3.0 开源
* 通过 Java 依赖链接本模组的附属模组不受 LGPL 强制传染
* Minecraft、Minecraft Forge 及其相关内容遵循各自的许可证与使用条款

---

## 版权与素材说明

本项目为同人二创作品，与原作 **Black Souls** 系列及其作者无从属、授权或官方合作关系。

* Black Souls 系列原作为 **寿司勇者** 创作
* 本项目仅作为非官方同人 Mod 与学习项目公开
* 项目中的代码按 LGPL-3.0 开源
* 若仓库中包含图片、音频、字体、模型或其他素材，请以仓库内额外说明为准
* 若相关素材存在版权争议，请联系仓库维护者处理

---

## 致谢

* **寿司勇者** — Black Souls 系列原作者，本项目的灵感来源
* **Minecraft Forge 团队** — 提供模组开发框架
* 所有测试、游玩、反馈与支持过这个项目的人

---

## 相关链接

* [Minecraft Forge 文档](https://mcforge.readthedocs.io/)
* [Minecraft Forge 论坛](https://forums.minecraftforge.net/)
* [Black Souls 系列作者页面](https://www.dlsite.com/maniax/circle/profile/=/maker_id/RG41580.html)

---

> This project is an unofficial fan-made work.
> Open Blacksouls is not affiliated with, endorsed by, or officially connected to the original Black Souls series.
