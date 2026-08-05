package com.kimik3znttey.insaneblade;

import com.kimik3znttey.insaneblade.client.IBClientConfig;
import com.kimik3znttey.insaneblade.config.IBConfig;
import com.kimik3znttey.insaneblade.network.IBNetwork;
import com.mojang.logging.LogUtils;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import org.slf4j.Logger;

/**
 * Insane Blade·八刀 主类。
 * 主手之外，快捷栏其余八把刀以概率参与伤害计算。
 * 伤害逻辑在服务端，图标/音效反馈通过数据包发给客户端。
 */
@Mod(InsaneBlade.MODID)
public final class InsaneBlade {

    public static final String MODID = "insaneblade";
    public static final Logger LOGGER = LogUtils.getLogger();

    public InsaneBlade() {
        // 伤害规则属于服务端权威数据，用 SERVER 配置（随世界保存、进服自动同步给客户端）
        ModLoadingContext.get().registerConfig(ModConfig.Type.SERVER, IBConfig.SPEC);
        // 图标/音效是个人观感，用 CLIENT 配置（纯本地，联机各看各的）。
        // IBClientConfig 只含配置定义、不触碰客户端专属类，服务端加载它也安全
        ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, IBClientConfig.SPEC);
        IBSounds.register(net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext.get().getModEventBus());
        IBNetwork.init();
    }
}
