package com.kimik3znttey.insaneblade.client;

import com.kimik3znttey.insaneblade.InsaneBlade;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 触发反馈覆盖层，三种风格（客户端配置 overlayStyle 切换）：
 *   BURST    屏幕中下侧图标一字炸开：回弹放大、缓慢上浮、消散（默认）
 *   KILLFEED 屏幕右侧连杀信息流：整组从右滑入、向上堆叠、滑出消散
 *   MINIMAL  极简：只在快捷栏上方闪一个计数文字
 *
 * 计数阶梯（三种风格共用）：1~4 黄字、5~7 红字加大、
 * 8（满刀）不显示数字，改为专属徽章 + 「疯狂！！」。
 * 外观尺寸/停留时长由客户端配置 iconScale / lingerMs 控制。
 */
@Mod.EventBusSubscriber(modid = InsaneBlade.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class ProcBurstOverlay {

    /** 满刀专属徽章，assets/insaneblade/textures/gui/jackpot.png（128×128 透明底） */
    private static final ResourceLocation JACKPOT_TEX =
            new ResourceLocation(InsaneBlade.MODID, "textures/gui/jackpot.png");

    private static final int MAX_ICONS = 12;    // 防高速连打时的图标堆积
    private static final int FEED_MAX_ROWS = 9; // 连杀流最多同屏行数
    private static final int SLIDE_MS = 150;    // 连杀流滑入/滑出时长

    private record IconEntry(ItemStack icon, long birth) {
    }

    private record FeedEntry(List<ItemStack> icons, long birth) {
    }

    private record TextEntry(int count, long birth) {
    }

    private static final List<IconEntry> BURSTS = new CopyOnWriteArrayList<>();
    private static final List<FeedEntry> FEED = new CopyOnWriteArrayList<>();
    private static final List<TextEntry> MINIMALS = new CopyOnWriteArrayList<>();

    /** BURST：摇中的刀图标逐格炸开 */
    public static void burst(List<ItemStack> icons) {
        long now = Util.getMillis();
        for (ItemStack icon : icons) {
            BURSTS.add(new IconEntry(icon, now));
        }
        while (BURSTS.size() > MAX_ICONS) {
            BURSTS.remove(0);
        }
    }

    /** KILLFEED：本次摇中作为一整条记录滑入 */
    public static void killfeed(List<ItemStack> icons) {
        FEED.add(new FeedEntry(List.copyOf(icons), Util.getMillis()));
        while (FEED.size() > FEED_MAX_ROWS) {
            FEED.remove(0);
        }
    }

    /** MINIMAL：只记一个数字 */
    public static void minimal(int count) {
        MINIMALS.add(new TextEntry(count, Util.getMillis()));
        while (MINIMALS.size() > 4) {
            MINIMALS.remove(0);
        }
    }

    @SubscribeEvent
    public static void onRegisterOverlays(RegisterGuiOverlaysEvent event) {
        event.registerAboveAll("insaneblade_proc", (gui, g, partialTick, w, h) -> render(g, w, h));
    }

    private static void render(GuiGraphics g, int width, int height) {
        long now = Util.getMillis();
        long life = IBClientConfig.LINGER_MS.get();
        float iconScale = IBClientConfig.ICON_SCALE.get().floatValue();
        renderBurst(g, width, height, now, life, iconScale);
        renderKillfeed(g, width, height, now, life, iconScale);
        renderMinimal(g, width, height, now, life, iconScale);
    }

    // ---------- BURST ----------

    private static void renderBurst(GuiGraphics g, int width, int height, long now, long life, float iconScale) {
        if (BURSTS.isEmpty()) return;
        BURSTS.removeIf(e -> now - e.birth() > life);
        int count = BURSTS.size();
        if (count == 0) return;

        int spacing = (int) (22 * iconScale);
        // 纵向锚点：距底边 90px = 快捷栏与扩展行(约46px) + 图标半高(约14px) + 呼吸空间(30px)
        // 不用"双倍快捷栏"类模组的话可以改回 64
        int baseY = height - 90;
        int startX = width / 2 - (count - 1) * spacing / 2;

        for (int i = 0; i < count; i++) {
            IconEntry e = BURSTS.get(i);
            float age = (now - e.birth()) / (float) life;
            float popIn = Math.min(1.0f, age / 0.2f);
            float scale = (0.5f + 1.3f * easeOutBack(popIn)) * iconScale; // 回弹式放大
            int rise = (int) (age * 18);                                  // 随生命上浮
            int x = startX + i * spacing;
            int y = baseY - rise;

            g.pose().pushPose();
            g.pose().translate(x, y, 0);
            g.pose().scale(scale, scale, 1.0f);
            g.renderItem(e.icon(), -8, -8);
            g.pose().popPose();
        }

        if (count >= 2) {
            renderCount(g, count, width / 2, baseY - (int) (34 * iconScale), iconScale);
        }
    }

    // ---------- KILLFEED ----------

    private static void renderKillfeed(GuiGraphics g, int width, int height, long now, long life, float iconScale) {
        if (FEED.isEmpty()) return;
        FEED.removeIf(e -> now - e.birth() > life);
        int rows = FEED.size();
        if (rows == 0) return;

        Font font = Minecraft.getInstance().font;
        int rowH = (int) (20 * iconScale);
        int anchorY = height / 2 - (int) (20 * iconScale); // 最新一行在屏幕右侧中部
        int margin = (int) (14 * iconScale);
        float labelScaleCap = Math.min(iconScale, 1.0f);   // 行内文字不随图标放大，保持信息流紧凑

        for (int i = rows - 1; i >= 0; i--) { // 新的在下，旧的向上堆
            FeedEntry e = FEED.get(i);
            long ageMs = now - e.birth();
            int fromBottom = rows - 1 - i;
            int y = anchorY - fromBottom * rowH;

            // 滑入：从右侧进来；生命末期向右滑出
            float slideIn = Math.min(1.0f, ageMs / (float) SLIDE_MS);
            float remain = Math.min(1.0f, (life - ageMs) / (float) SLIDE_MS);
            float t = easeOutCubic(Math.max(0.0f, Math.min(slideIn, remain)));

            int n = e.icons().size();
            boolean jackpot = n >= 8;
            Component label = countLabel(n);
            float labelScale = Math.min(countScale(n), 1.25f) * labelScaleCap;
            int emblemW = jackpot ? (int) (16 * iconScale) + 2 : 0;
            int labelW = emblemW + (int) (font.width(label) * labelScale);
            int iconStep = (int) (18 * iconScale);
            int rowWidth = 8 + labelW + 6 + (n - 1) * iconStep + 16;
            int x = width - margin - (int) (rowWidth * t);

            int curX = x + 4;
            if (jackpot) {
                int es = (int) (16 * iconScale);
                blitJackpot(g, curX, y + rowH / 2 - es / 2, es);
                curX += es + 2;
            }

            // 计数标签（阶梯色）
            g.pose().pushPose();
            g.pose().translate(curX, y + rowH / 2 - 4 * labelScale, 0);
            g.pose().scale(labelScale, labelScale, 1.0f);
            g.drawString(font, label, 0, 0, 0xFFFFFF, true);
            g.pose().popPose();
            curX += labelW + 6;

            // 本组刀图标
            float iscale = 0.9f * iconScale;
            int iy = y + rowH / 2;
            for (int k = 0; k < n; k++) {
                g.pose().pushPose();
                g.pose().translate(curX + k * iconStep, iy, 0);
                g.pose().scale(iscale, iscale, 1.0f);
                g.renderItem(e.icons().get(k), -8, -8);
                g.pose().popPose();
            }
        }
    }

    // ---------- MINIMAL ----------

    private static void renderMinimal(GuiGraphics g, int width, int height, long now, long life, float iconScale) {
        if (MINIMALS.isEmpty()) return;
        MINIMALS.removeIf(e -> now - e.birth() > life);
        if (MINIMALS.isEmpty()) return;

        TextEntry e = MINIMALS.get(MINIMALS.size() - 1); // 只显示最新一条
        float age = (now - e.birth()) / (float) life;
        float popIn = Math.min(1.0f, age / 0.25f);
        float pop = 0.6f + 0.8f * easeOutBack(popIn);
        // 与 BURST 的 baseY 对齐（双倍快捷栏环境），没装可改回 72
        renderCount(g, e.count(), width / 2, height - 92, iconScale * pop);
    }

    // ---------- 计数阶梯（三种风格共用） ----------

    /** 计数内容：1~7 为 "xN"（1~4 黄、5~7 红），满刀 8 为红色粗体「疯狂！！」 */
    private static Component countLabel(int n) {
        if (n >= 8) {
            return Component.literal("疯狂！！").withStyle(ChatFormatting.BOLD, ChatFormatting.RED);
        }
        ChatFormatting color = n >= 5 ? ChatFormatting.RED : ChatFormatting.GOLD;
        return Component.literal("x" + n).withStyle(color, ChatFormatting.BOLD);
    }

    /** 计数放大倍率：5 把以上加大，满刀最大 */
    private static float countScale(int n) {
        if (n >= 8) return 1.6f;
        return n >= 5 ? 1.35f : 1.0f;
    }

    /** BURST / MINIMAL 的中央计数：满刀时「疯狂！！」+ 徽章横向排布（徽章贴文字右侧），否则画阶梯色 xN */
    private static void renderCount(GuiGraphics g, int n, int cx, int cy, float scale) {
        if (n >= 8) {
            Font font = Minecraft.getInstance().font;
            Component label = countLabel(n);
            float ts = 1.4f * scale;
            int textW = (int) (font.width(label) * ts);
            int es = (int) (18 * scale);  // 徽章边长：与文字行高匹配，想更醒目改 22~24
            int gap = (int) (3 * scale);  // 文字与徽章的间距，想更贴改 2
            int totalW = textW + gap + es;

            // 文字 + 徽章作为一个组合整体居中，垂直方向共享同一条中线 cy
            int textX = cx - totalW / 2;
            int textY = cy - (int) (9 * ts) / 2;
            int emblemX = textX + textW + gap;
            int emblemY = cy - es / 2;

            g.pose().pushPose();
            g.pose().translate(textX, textY, 0);
            g.pose().scale(ts, ts, 1.0f);
            g.drawString(font, label, 0, 0, 0xFFFFFF, true);
            g.pose().popPose();
            blitJackpot(g, emblemX, emblemY, es);
        } else {
            drawCenteredScaled(g, countLabel(n), cx, cy, countScale(n) * scale);
        }
    }

    private static void drawCenteredScaled(GuiGraphics g, Component text, int cx, int y, float scale) {
        Font font = Minecraft.getInstance().font;
        g.pose().pushPose();
        g.pose().translate(cx, y, 0);
        g.pose().scale(scale, scale, 1.0f);
        g.drawString(font, text, -font.width(text) / 2, 0, 0xFFFFFF, true);
        g.pose().popPose();
    }

    /**
     * 把 128×128 的徽章整体缩放到 size×size 画出。
     * 注意：blit 的宽高参数同时是贴图 UV 采样区域（1 texel = 1 屏幕像素），
     * 直接传小尺寸只会裁出贴图左上角一小块（徽章四角是透明的，画出来约等于隐身），
     * 必须 UV 采样全图、走 pose 缩放控制屏幕尺寸。
     */
    private static void blitJackpot(GuiGraphics g, int x, int y, int size) {
        float s = size / 128.0f;
        g.pose().pushPose();
        g.pose().translate(x, y, 0);
        g.pose().scale(s, s, 1.0f);
        g.blit(JACKPOT_TEX, 0, 0, 0, 0, 128, 128, 128, 128);
        g.pose().popPose();
    }

    /** 回弹缓动：先冲过头再弹回来，炸开感的来源 */
    private static float easeOutBack(float t) {
        float c1 = 1.70158f;
        float c3 = c1 + 1.0f;
        float u = t - 1.0f;
        return 1.0f + c3 * u * u * u + c1 * u * u;
    }

    /**  cubic 缓出：连杀流滑入滑出用 */
    private static float easeOutCubic(float t) {
        float u = 1.0f - t;
        return 1.0f - u * u * u;
    }

    private ProcBurstOverlay() {
    }
}
