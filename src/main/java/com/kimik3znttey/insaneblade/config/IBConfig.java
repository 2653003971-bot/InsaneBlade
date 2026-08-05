package com.kimik3znttey.insaneblade.config;

import net.minecraftforge.common.ForgeConfigSpec;

import java.util.List;

/**
 * 配置定义（SERVER 类型：存档级，位于存档 serverconfig/ 下，联机时以服务端为准）。
 * 这里不做缓存，每次直接读 get()，配置热重载后自动生效。
 */
public final class IBConfig {

    public static final ForgeConfigSpec SPEC;

    /** 哪些命名空间算"刀" */
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> BLADE_NAMESPACES;
    /** PRD 常数 C：概率 = C × (连续未中次数+1)，达到 100% 即保底必中 */
    public static final ForgeConfigSpec.DoubleValue PRD_CONSTANT;
    /** 剑势：每层提供的全体概率加成 */
    public static final ForgeConfigSpec.DoubleValue MOMENTUM_BONUS;
    /** 剑势：最大层数 */
    public static final ForgeConfigSpec.IntValue MOMENTUM_MAX_STACKS;
    /** 剑势：窗口持续时长（tick） */
    public static final ForgeConfigSpec.IntValue MOMENTUM_TICKS;
    /** 摇中的刀按自身面板计入伤害的比例（0.0 ~ 1.0） */
    public static final ForgeConfigSpec.DoubleValue DAMAGE_SCALE;
    /** 读不到面板时的兜底攻击值 */
    public static final ForgeConfigSpec.DoubleValue FALLBACK_ATTACK;
    /** 八刀额外伤害的计算模式 */
    public static final ForgeConfigSpec.EnumValue<BonusMode> BONUS_MODE;
    /** 仅 RATIO 模式：每把摇中的刀计入 主手伤害 × ratio ×（该刀面板 ÷ 主刀面板） */
    public static final ForgeConfigSpec.DoubleValue RATIO;
    /** 仅 BLEND 模式：几何混合指数 α */
    public static final ForgeConfigSpec.DoubleValue BLEND_ALPHA;
    /** 保险丝：八刀总加成不超过主手伤害的倍数（0 = 不设限） */
    public static final ForgeConfigSpec.DoubleValue MAX_BONUS_RATIO;

    static {
        ForgeConfigSpec.Builder b = new ForgeConfigSpec.Builder();
        b.comment("Insane Blade·快捷栏八把刀按概率参战",
                  "Eight knives participate in the battle based on probability"
                 )
                .push("insaneblade");

        BLADE_NAMESPACES = b.comment(
                "自定义可参战的装备。自定义哪些物品被视为'剑'，默认覆盖拔刀剑重锋。想兼容其他刀类附属或物品，把它的命名空间加进来即可",
                "Customizable battle-ready item , Which items are considered 'swords', by default overriding the SlashBlade:Resharped",
                ""

                )
                .defineList("bladeNamespaces", List.of("slashblade"), o -> o instanceof String);

        b.comment(
                  "触发参战的概率系统，包含'PRD保底'和'剑势连击'",
                  "The probability system that triggers participation in battle"
                 )
                 .push("probability");

        PRD_CONSTANT = b.comment(
                "伪随机分布常数 C：每把刀独立计数，概率 = C × (连续未中次数+1)，",
                "越不中越容易中，C×n 涨到 100% 时保底必中，长期期望由 C 决定。参考：",
                "  0.033 ≈  15%（保底约 30 刀）",
                "  0.055 ≈  20%（保底约 18 刀）  <- 默认",
                "  0.085 ≈  25%（保底约 12 刀）",
                " Pseudo-random distribution constant C , each sword is counted independently, probability = C × (number of consecutive misses + 1)"
                )
                .defineInRange("prdConstant", 0.100, 0.0, 1.0);

        MOMENTUM_BONUS = b.comment(
                "剑势窗口内，每层为全体刀提供的额外触发概率（制造连续参战）",
                "In the sword stance window, each level provides an extra trigger chance for all blades (to keep them participating consecutively)"
                )
                .defineInRange("momentumBonusPerStack", 0.08, 0.0, 1.0);

        MOMENTUM_MAX_STACKS = b.comment(
                        "剑势最大叠加层数",
                        "Maximum number of sword stance stacks"
                )
                .defineInRange("momentumMaxStacks", 5, 0, 100);

        MOMENTUM_TICKS = b.comment(
                "剑势窗口持续时长（tick，20 tick = 1 秒）",
                "Sword stance window duration (ticks, 20 ticks = 1 second)"
                )
                .defineInRange("momentumTicks", 60, 0, 72000);

        b.pop();

        b.comment("伤害与反馈","damage and feedback").push("gameplay");

        DAMAGE_SCALE = b.comment(
                        "摇中的刀按自身面板计入本次伤害的比例，0.0% ~ 65535.0%",
                        "The proportion of the damage from the shaking knife is calculated based on its own panel."
                )
                .defineInRange("damageScale", 0.4, 0.0, 65535.0);

        FALLBACK_ATTACK = b.comment(
                "读不到刀面板时的基本攻击值，默认为0，推荐使用默认值",
                "When the blade panel cannot be read, the basic attack value defaults to 0. Using the default value is recommended."
                )
                .defineInRange("fallbackAttack", 0.0, 0.0, 65535.0);

        BONUS_MODE = b.comment(
                "八刀额外伤害的计算模式：",
                "  FLAT  - 是最简单的模式，额外的伤害不会计算饰品等带来的额外加成。摇中的刀按自身面板 × damageScale 直接计入伤害（默认）",
                "  Pure Panel: The knife hit by the shake calculates damage directly , based on its own panel × damageScale (default) ,  Additional damage does not take into account extra bonuses from accessories and the like.", "",
                "  RATIO - 根据额外武器面板和主手实际伤害的比值对额外伤害缩放，吃加成但不会与其他模组冲突纯挂钩 , 整合包的饰品/药水/附魔加成全部反应到ratio里影响额外伤害 buff 叠多爆它也同比膨胀 , 每把摇中的刀 = 主手伤害 × ratio ×（该刀面板 ÷ 主刀面板），",
                "  Scale additional damage based on the ratio of the off-hand weapon panel to the main-hand's actual damage , it benefits from bonuses but is purely linked and will not conflict with other modules.", "",
                "  BLEND - 更平衡的计算模式 , '快捷栏的刀剑面板'与'主手造成的最终伤害'共同起作用 , 默认为几何平均模式",
                "  A more balanced computing model , Bonus coefficient = (hotbar sword panel)^(1-a) × (Main hand damage)^a, where a is the number of knives rolled"
                )
                .defineEnum("bonusMode", BonusMode.BLEND);

        RATIO = b.comment(
                "RATIO模式伤害缩放系数：每把摇中的刀计入 主手伤害 × 此系数 ×（该刀面板 ÷ 主刀面板）",
                "RATIO mode damage scaling coefficient",
                "Each knife hit is counted as main-hand damage × this coefficient × (the knife's stats ÷ main knife's stats)"
                )
                .defineInRange("ratio", 1.0, 0.0, 4.0);

        BLEND_ALPHA = b.comment(
                "BLEND模式混合缩放指数。推荐设置为0.5",
                "0.0 = 纯面板（等价 FLAT），1.0 = 纯 Ratio  0.5 = 几何平均模式",
                "0.0 = pure panel input (equivalent to FLAT), 1.0 = pure hook, 0.5 = geometric mean"
                )
                .defineInRange("blendAlpha", 0.5, 0.0, 1.0);

        MAX_BONUS_RATIO = b.comment(
                "伤害平衡器，额外伤害总加成不超过主手伤害 × 此倍率，",
                "防止意外的数值爆炸",
                "防止排在 LOWEST 优先级的乘算模组把加成再次放大失控。0 = 不设限",
                "Extra damage bonus does not exceed main-hand final damage × this multiplier"
                )
                .defineInRange("maxBonusRatio", 2.0, 0.0, 100.0);

        b.pop();
        SPEC = b.build();
    }

    /** 八刀额外伤害的计算模式，详见 bonusMode 配置注释 */
    public enum BonusMode {
        /** 纯面板：摇中的刀按自身面板 × damageScale 计入 */
        FLAT,
        /** 纯挂钩：每把摇中的刀 = 主手伤害 × ratio ×（该刀面板 ÷ 主刀面板） */
        RATIO,
        /** 几何混合：bonus = 面板^(1-α) × 主手伤害^α */
        BLEND
    }

    private IBConfig() {
    }
}
