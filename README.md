# Insane Blade·九刀流 （Forge 1.20.1）

主手之外，在快捷栏其余八把拔刀剑用「PRD 保底 + 剑势连击」的概率引擎参与伤害计算；
摇中瞬间屏幕中下侧炸开刀身图标、叠加斩击音效

与"拔刀剑：双刃重铸"兼容，数值完全服务端可配置(平衡性)

---
本模组由作者与 Kimi（Moonshot AI） 结对设计并开发：机制讨论、代码实现与调试中均有 AI 参与。
音效（blade_burst / blade_burst_heavy）与满刀徽章贴图（jackpot.png）为 AI 生成资产。
---

## 一、项目结构

```
InsaneBlade/
└── src/main/
    ├── java/com/kimik3znttey/insaneblade/
    │   ├── InsaneBlade.java          主类：注册 SERVER 配置 + 音效 + 网络通道
    │   ├── IBSounds.java             自定义音效注册（blade_burst / blade_burst_heavy）
    │   ├── config/
    │   │   └── IBConfig.java         服务端配置（存档级 serverconfig）
    │   ├── combat/
    │   │   ├── BladeProcEngine.java  概率引擎：PRD 保底 + 剑势连击（服务端）
    │   │   └── BladeDamageHandler.java 伤害挂钩：LivingHurtEvent 加成计算
    │   ├── network/
    │   │   ├── IBNetwork.java        SimpleChannel（isRemotePresent 检查，原版客户端可进服）
    │   │   └── ProcBurstPacket.java  "哪几格摇中"通知包
    │   └── client/
    │       ├── IBClientConfig.java     客户端表现配置（config/ 下，纯本地）
    │       ├── ClientBurstHandler.java 收包表现层：按风格分发图标 + 音效
    │       └── ProcBurstOverlay.java   反馈覆盖层：BURST / KILLFEED / MINIMAL
    └── resources/
        ├── META-INF/mods.toml
        ├── pack.mcmeta
        └── assets/insaneblade/
            ├── sounds.json          音效事件映射
            ├── textures/gui/
            │   └── jackpot.png      满刀专属徽章（8 把全中时替代数字显示）
            └── sounds/              音效文件（.ogg，可替换成自己的录音）
                ├── blade_burst.ogg        普通触发（1~2 把）
                └── blade_burst_heavy.ogg  重度触发（3 把以上）
```

## 二、机制详解

### 概率引擎

每把刀独立维护"连续未中次数"：

```
本次概率 = min(1, C × (连续未中 + 1) + 剑势层数 × 每层加成)
```

- **PRD 保底**（Dota 2 同款伪随机）：越不中越容易中，C×n 涨到 100% 必中。
  长期期望由 C 决定：0.033≈15%、0.055≈20%（默认）、0.085≈25%——正好覆盖你要的 15%~25% 区间。
- **剑势连击**：任一刀触发后开启短窗口（默认 3 秒），全体刀概率 +8%/层、最多 5 层、触发刷新时间。
  "突然连着几刀全爆"的高潮就是它被制造出来的，而不是纯运气。

### 伤害计算（三种模式，`bonusMode` 切换）

- 触发点：服务端 `LivingHurtEvent`（**LOW 优先级**，让绝大多数加伤模组先跑完），
  仅当玩家**主手持刀亲手近战命中**（刀光实体、幻影剑不算）；
- **面板读取走纯原版 API**（主手属性修饰符攻击 + 附魔对目标生物类型加成），
  不需要引入拔刀剑依赖——零耦合，没装重锋也只是静默不生效；
- 加成在护甲减免前加入（吃目标护甲/保护）。设本刀主手伤害快照为 `x0`、摇中刀面板为 `y`：

| 模式 | 公式 | 特点 |
| --- | --- | --- |
| `FLAT`（默认） | y × `damageScale`，再乘攻击冷却比例 | 与旧版行为一致：数值可预测、零意外；但整合包 buff 与八刀无关 |
| `RATIO` | x0 × `ratio` ×（y ÷ 主刀面板） | 完全挂钩主手最终伤害，整合包加成全吃；buff 同比膨胀 |
| `BLEND` | y^(1-α) × x0^α | 挂钩又防爆：9 把相同的刀时与 FLAT 期望一致；buff 越高加成越大，但只按次线性（α=0.5 即平方根级）膨胀 |

- `x0` 是减防前的**瞬时绝对值**（已过属性/附魔/暴击/冷却缩放与排在我们前面的模组倍率），
  每一刀独立快照，无跨刀平均、无状态残留；
- RATIO / BLEND 的 x0 本身已被原版冷却缩放乘过，不再手动乘冷却比例（FLAT 保留手动缩放）；
- `maxBonusRatio` 保险丝：八刀总加成不超过 x0 × 该倍数，防 LOWEST 优先级乘算模组二次放大；
- 设计取舍：三种模式都不吃暴击二次乘算（暴击已含在 x0 里），不引入 mixin——
  想要真正"融进面板"（被后续乘区再次放大）见 V2 路线的 INTEGRATED 设想。

### 反馈表现（客户端配置 config/insaneblade-client.toml，纯本地、联机各看各的）

- **四种图标风格**（`overlayStyle`）：
  - `BURST`（默认）：屏幕中下侧图标一字炸开，回弹放大、上浮消散；
  - `KILLFEED`：屏幕右侧连杀信息流，整组滑入、向上堆叠、滑出消散；
  - `MINIMAL`：极简，只闪一个计数文字。
  - `ULTRABURST`：屏幕右侧连杀信息流，整组滑入、向上堆叠、滑出消散，同时加上屏幕中下侧图标一字炸开，回弹放大、上浮消散.
- **计数阶梯**：摇中 1~4 把显示黄字 xN，5~7 把红字加大，
  **8 把全中（满刀）不显示数字**——弹出专属徽章 + 红色粗体「疯狂！！」；
- **质感旋钮**：`iconScale`（图标缩放 0.5~2.0）、`lingerMs`（停留时长）、`volume`（音量倍率）、
  `procSound` / `procOverlay` 总开关；
- 徽章贴图在 `assets/insaneblade/textures/gui/jackpot.png`，可换成自己的 128×128 透明底 PNG；
- **音效自定义**：内置两段 AI 生成的斩击音（普通触发 / 3 把以上重度触发），音调随摇中数爬升。
  想换成自己的录音：把同名 `.ogg` 替换进 `assets/insaneblade/sounds/` 重新 build 即可
  （建议 44.1kHz，注意响度别爆音）。想加更多变体，注册新 SoundEvent + 在 sounds.json 里映射就行。

### 多人说明

- 伤害逻辑全部在服务端，无作弊空间；
- 发包前用 `isRemotePresent` 检查对端：**没装本 mod 的客户端不会收到包**，可正常进服，只是没有图标/音效反馈；
- 伤害规则是 SERVER 配置，联机以服务端为准并自动同步；
- 图标/音效表现是 CLIENT 配置，每人各调各的，服主与队友互不影响。

### 性能

每次命中 = 8 次物品读取 + 8 次随机数 + 最多一次发包，纳秒级。
不生成刀光实体、不跑连招流程——"性能爆炸"的源头从设计上就不存在。

## 三、配置项（存档 serverconfig/insaneblade-server.toml）

| 键 | 默认 | 说明 |
| --- | --- | --- |
| `bladeNamespaces` | `["slashblade"]` | 视为刀的命名空间 |
| `prdConstant` | `0.055` | PRD 常数 C（15%→0.033，20%→0.055，25%→0.085） |
| `momentumBonusPerStack` | `0.08` | 剑势每层全体概率加成 |
| `momentumMaxStacks` | `5` | 剑势层数上限 |
| `momentumTicks` | `60` | 剑势窗口时长（tick） |
| `damageScale` | `1.0` | 摇中刀计入伤害的比例（0~100%，仅 FLAT 模式） |
| `fallbackAttack` | `8.0` | 读不到面板时的兜底攻击 |
| `bonusMode` | `FLAT` | 八刀加成计算模式：`FLAT` / `RATIO` / `BLEND` |
| `ratio` | `0.4` | 仅 RATIO：每把摇中刀 = 主手伤害 × 此系数 ×（该刀面板 ÷ 主刀面板） |
| `blendAlpha` | `0.5` | 仅 BLEND：几何混合指数（0=纯面板，1=纯挂钩） |
| `maxBonusRatio` | `2.0` | 保险丝：八刀总加成上限（主手伤害倍数，0=不设限） |

### 客户端配置（config/insaneblade-client.toml，纯本地）

| 键 | 默认 | 说明 |
| --- | --- | --- |
| `overlayStyle` | `BURST` | 图标风格：`BURST` / `KILLFEED` / `MINIMAL` |
| `iconScale` | `1.0` | 图标缩放（0.5~2.0） |
| `lingerMs` | `900` | 反馈停留时长（毫秒） |
| `volume` | `1.0` | 音效音量倍率（0~2.0） |
| `procSound` / `procOverlay` | `true` | 音效 / 图标总开关 |

计数阶梯：1~4 黄字，5~7 红字加大，8（满刀）专属徽章 +「疯狂！！」（不显示数字）。

## 四、精修路线

- 音效：更多分层变体（按摇中数渐变），或与重锋原生斩击音混搭；
- 图标：粒子拖尾、自定义高触发语录（可编辑字符串列表，满刀时随机抽一句）；
- 保底曲线可视化调试命令 `/insaneblade stats`（查看各刀未中计数与剑势层数）；
- 剑势层数的屏幕角落小指示条；
- 与 HoldYourCamera 联动：触发八刀瞬间拉一个极短的第三人称运镜；
- ~~多模式伤害~~（已实装：`bonusMode = FLAT / RATIO / BLEND`，默认 FLAT 保兼容）；
  更进一步可做 INTEGRATED 模式：通过 mixin 注入 `Player.attack` 把八刀加成融进面板
  （被暴击与后续乘区再次放大），需要引入 MixinGradle 工具链与 refmap 处理，
  并需重点测试与战斗机制类 mod（如 Better Combat）的兼容。

# Insane Blade · 八刀 — Store Page (English)

Developed in pair with Kimi K3 (Moonshot AI); some art/SFX assets are AI-generated.

## Summary (short, one-liner)

Your whole hotbar joins the fight: the other 8 SlashBlades roll to strike with you — with icon bursts, kill-feed flair, and a full-proc JACKPOT.

## Description (long)

**Insane Blade · 八刀** is a combat addon for SlashBlade-flavored mods (works with *SlashBlade: Resharped* and any mod whose blades live in the `slashblade` namespace) that turns your hotbar into a blade formation. When you land a melee hit with a blade in your main hand, each of the **other 8 blades in your hotbar** independently rolls a chance to join the strike, adding its own damage on top.

### Proc engine — PRD + Momentum

No pure dice here:

- **PRD (pseudo-random distribution, Dota 2 style):** every blade tracks its own miss streak — the longer it misses, the higher its next proc chance, ramping to a guaranteed hit. Long-term proc rate is tunable via the PRD constant (~15–25% out of the box).
- **Momentum:** when any blade procs, a short window opens that boosts *all* blades' proc chance, stacking up to a cap and refreshing on each proc. That's where those "everything fired at once" highlight moments come from — by design, not luck.

### Damage models — pick your flavor (server config)

- **FLAT** (default): each procced blade adds `its damage × damageScale`. Predictable, zero surprises.
- **RATIO**: bonus scales directly with your main hand's final damage — all your modpack buffs apply proportionally.
- **BLEND**: geometric blend of blade damage and main-hand damage (`y^(1-α) · x0^α`). Buff-scaling with a built-in anti-runaway curve; identical blades behave exactly like FLAT.
- **Safety fuse:** `maxBonusRatio` caps the total bonus relative to your main-hand hit, so stacked-buff modpacks can't spiral out of control.

Cooldown scaling is respected, and the bonus is applied before armor reduction.

### Feedback — three overlay styles (client config)

- **BURST** — icons pop and float up above your hotbar.
- **KILLFEED** — a scrolling feed slides in on the right, one row per proc group.
- **MINIMAL** — just a quick counter flash.

Proc counts tier up: 1–4 gold, 5–7 bigger red, and a full 8-blade **JACKPOT** shows a dedicated emblem with 「疯狂！！」 instead of a number. Slash sound effects pitch up with the proc count. Icon scale, linger time, volume, and per-player toggles are all in the client config — on servers, every player gets their own preferred flavor.

### Multiplayer & performance

- All damage logic is server-authoritative (no cheating surface); vanilla clients can join a server running it — they just won't see the feedback.
- Server rules live in `serverconfig`, visuals in per-player `config/insaneblade-client.toml`.
- Cost per hit: 8 item reads + 8 RNG rolls + at most one packet. Nanoseconds. No entities spawned, ever.

### Requirements

- Minecraft 1.20.1, Forge (also runs on NeoForge 1.20.1 47.1.x)
- A SlashBlade mod to supply the blades (e.g. *SlashBlade: Resharped*) — without one, the mod simply stays dormant.

Open source under MIT. Issues and PRs welcome on GitHub.
