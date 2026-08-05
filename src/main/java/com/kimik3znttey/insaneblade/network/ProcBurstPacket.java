package com.kimik3znttey.insaneblade.network;

import com.kimik3znttey.insaneblade.client.ClientBurstHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * 触发通知包：把摇中的快捷栏槽位索引发给攻击者客户端。
 */
public record ProcBurstPacket(List<Integer> slots) {

    public static void encode(ProcBurstPacket pkt, FriendlyByteBuf buf) {
        buf.writeByte(pkt.slots.size());
        for (int slot : pkt.slots) {
            buf.writeByte(slot);
        }
    }

    public static ProcBurstPacket decode(FriendlyByteBuf buf) {
        int count = buf.readByte();
        List<Integer> slots = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            slots.add((int) buf.readByte());
        }
        return new ProcBurstPacket(slots);
    }

    public static void handle(ProcBurstPacket pkt, Supplier<NetworkEvent.Context> ctx) {
        // 包体在服务端也会加载，客户端处理类必须 Dist 隔离调用
        ctx.get().enqueueWork(() ->
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientBurstHandler.onBurst(pkt.slots())));
        ctx.get().setPacketHandled(true);
    }
}
