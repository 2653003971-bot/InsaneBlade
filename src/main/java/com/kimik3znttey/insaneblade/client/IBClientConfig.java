package com.kimik3znttey.insaneblade.client;

import net.minecraftforge.common.ForgeConfigSpec;

/**
 * 客户端表现配置（CLIENT 类型：纯本地，位于 config/insaneblade-client.toml）。
 * 图标/音效属于个人观感，联机时各看各的——服主管伤害规则（SERVER 配置），
 * 但管不着你的屏幕怎么爽。
 *
 * 注意：本类只含配置定义，不 import 任何客户端专属类，服务端加载它也安全。
 */
public final class IBClientConfig {

    public static final ForgeConfigSpec SPEC;

    /** 触发音效开关 */
    public static final ForgeConfigSpec.BooleanValue PROC_SOUND;
    /** 屏幕图标开关 */
    public static final ForgeConfigSpec.BooleanValue PROC_OVERLAY;
    /** 图标表现风格 */
    public static final ForgeConfigSpec.EnumValue<OverlayStyle> OVERLAY_STYLE;
    /** 图标整体缩放 */
    public static final ForgeConfigSpec.DoubleValue ICON_SCALE;
    /** 反馈停留时长（毫秒） */
    public static final ForgeConfigSpec.IntValue LINGER_MS;
    /** 音效音量倍率 */
    public static final ForgeConfigSpec.DoubleValue VOLUME;

    static {
        ForgeConfigSpec.Builder b = new ForgeConfigSpec.Builder();
        b.comment("Insane Blade客户端表现").push("feedback");

        PROC_SOUND = b.comment("摇中时播放斩击音效（摇中越多音调越高）")
                .define("procSound", true);

        PROC_OVERLAY = b.comment("摇中时在屏幕上给出图标/文字反馈")
                .define("procOverlay", true);

        OVERLAY_STYLE = b.comment(
                "图标表现风格：",
                "  BURST    - 屏幕中下侧图标一字炸开（命中反馈风）",
                "  KILLFEED - 屏幕右侧的刀剑信息流，整组滑入、向上堆叠",
                "  MINIMAL  - 极简，只在快捷栏上方闪一个计数文字",
                "  ULTRABURST - BURST加KILLFEED的效果",
                "计数随摇中数升级：1~4 黄字，5~7 红字加大，8（满刀）显示专属徽章+「疯狂！！」")
                .defineEnum("overlayStyle", OverlayStyle.KILLFEED);

        ICON_SCALE = b.comment("图标整体缩放，1.0 = 标准")
                .defineInRange("iconScale", 1.0, 0.5, 2.0);

        LINGER_MS = b.comment("反馈停留时长（毫秒）")
                .defineInRange("lingerMs", 900, 200, 3000);

        VOLUME = b.comment("音效音量倍率，1.0 = 标准")
                .defineInRange("volume", 1.0, 0.0, 2.0);

        b.pop();
        SPEC = b.build();
    }

    /** 图标表现风格，详见 overlayStyle 配置注释 */
    public enum OverlayStyle {
        BURST, KILLFEED, MINIMAL, ULTRABURST
    }

    private IBClientConfig() {
    }
}
