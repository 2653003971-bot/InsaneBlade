package com.kimik3znttey.insaneblade;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * 自定义音效注册。
 * 音频文件：assets/insaneblade/sounds/*.ogg（想换音效直接替换同名文件即可）。
 * 事件映射：assets/insaneblade/sounds.json。
 */
public final class IBSounds {

    public static final DeferredRegister<SoundEvent> SOUND_EVENTS =
            DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, InsaneBlade.MODID);

    /** 普通触发：1~2 把刀摇中 */
    public static final RegistryObject<SoundEvent> BLADE_BURST = SOUND_EVENTS.register("blade_burst",
            () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(InsaneBlade.MODID, "blade_burst")));

    /** 重度触发：3 把及以上摇中 */
    public static final RegistryObject<SoundEvent> BLADE_BURST_HEAVY = SOUND_EVENTS.register("blade_burst_heavy",
            () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(InsaneBlade.MODID, "blade_burst_heavy")));

    public static void register(IEventBus modBus) {
        SOUND_EVENTS.register(modBus);
    }

    private IBSounds() {
    }
}
