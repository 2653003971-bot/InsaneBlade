package com.kimik3znttey.insaneblade.network;

import com.kimik3znttey.insaneblade.InsaneBlade;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.List;

/**
 * 网络通道：服务端 -> 客户端，通知"哪几格刀摇中了"。
 * 发送前用 isRemotePresent 检查对端是否注册了通道，
 * 没装本 mod 的原版客户端不会收到包（也就不会出问题），只是看不到图标反馈。
 */
public final class IBNetwork {

    private static final String VERSION = "1";
    private static SimpleChannel CHANNEL;

    public static void init() {
        CHANNEL = NetworkRegistry.ChannelBuilder
                .named(new ResourceLocation(InsaneBlade.MODID, "proc"))
                .networkProtocolVersion(() -> VERSION)
                .clientAcceptedVersions(VERSION::equals)
                .serverAcceptedVersions(VERSION::equals)
                .simpleChannel();

        CHANNEL.registerMessage(0, ProcBurstPacket.class,
                ProcBurstPacket::encode, ProcBurstPacket::decode, ProcBurstPacket::handle);
    }

    public static void sendProcBurst(Player player, List<Integer> slots) {
        if (player instanceof ServerPlayer sp && CHANNEL != null
                && CHANNEL.isRemotePresent(sp.connection.connection)) {
            CHANNEL.send(PacketDistributor.PLAYER.with(() -> sp), new ProcBurstPacket(slots));
        }
    }

    private IBNetwork() {
    }
}
