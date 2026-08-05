package com.kimik3znttey.insaneblade.client;

import com.kimik3znttey.insaneblade.IBSounds;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * 客户端收到触发包后的表现层：
 * 按客户端配置把图标分发到三种风格之一，并播放触发音效。
 * 所有开关/旋钮都在 config/insaneblade-client.toml，纯本地，联机各看各的。
 */
public final class ClientBurstHandler {

    public static void onBurst(List<Integer> slots) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        List<ItemStack> icons = new ArrayList<>();
        for (int slot : slots) {
            ItemStack stack = mc.player.getInventory().getItem(slot);
            if (!stack.isEmpty()) {
                icons.add(stack.copy());
            }
        }
        if (icons.isEmpty()) return;
        int n = icons.size();

        if (IBClientConfig.PROC_OVERLAY.get()) {
            switch (IBClientConfig.OVERLAY_STYLE.get()) {
                case BURST -> ProcBurstOverlay.burst(icons);
                case KILLFEED -> ProcBurstOverlay.killfeed(icons);
                case MINIMAL -> ProcBurstOverlay.minimal(n);
                case ULTRABURST -> {
                                      ProcBurstOverlay.burst(icons);
                                      ProcBurstOverlay.killfeed(icons);
                                   }
            }
        }

        if (IBClientConfig.PROC_SOUND.get()) {
            float vol = IBClientConfig.VOLUME.get().floatValue();
            float pitch = 0.9f + 0.15f * (n - 1); // 摇中越多音调越高
            if (n >= 3) {
                // 多把命中用重型刀阵音，爽感递进
                mc.player.playSound(IBSounds.BLADE_BURST_HEAVY.get(), 0.9f * vol, pitch);
            } else {
                mc.player.playSound(IBSounds.BLADE_BURST.get(), 0.8f * vol, pitch);
            }
        }
    }

    private ClientBurstHandler() {
    }
}
