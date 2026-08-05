package com.kimik3znttey.insaneblade.combat;

import com.kimik3znttey.insaneblade.config.IBConfig;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 概率引擎（服务端）：
 * - PRD 保底：每把刀独立记录"连续未中次数"，概率 = C × (未中+1)，到 100% 必中；
 * - 剑势连击：任一刀触发后开启短窗口，全体刀概率临时提升、可叠层，主动制造连爆高峰。
 * 状态存内存即可（重启清零，对玩法无影响）。
 */
public final class BladeProcEngine {

    /** 每个玩家、每个快捷栏槽位的连续未中计数（9 格，主手槽位跳过） */
    private static final Map<UUID, int[]> FAILURES = new HashMap<>();
    /** 每个玩家的剑势状态 */
    private static final Map<UUID, Momentum> MOMENTUM = new HashMap<>();

    private record Momentum(int stacks, long expireTick) {
    }

    /**
     * 对玩家快捷栏除主手外的 8 个槽位逐个判定。
     *
     * @return 摇中的槽位索引列表（可能为空）
     */
    public static List<Integer> roll(Player player, long gameTime) {
        UUID id = player.getUUID();
        int[] fails = FAILURES.computeIfAbsent(id, k -> new int[9]);

        Momentum mom = MOMENTUM.get(id);
        double momBonus = (mom != null && gameTime < mom.expireTick())
                ? mom.stacks() * IBConfig.MOMENTUM_BONUS.get()
                : 0.0;

        List<Integer> procs = new ArrayList<>();
        int selected = player.getInventory().selected;

        for (int slot = 0; slot < 9; slot++) {
            if (slot == selected) continue; // 主手那把我们不算，它是本体
            if (!BladeDamageHandler.isBlade(player.getInventory().getItem(slot))) continue; // 不是刀就跳过

            double chance = Math.min(1.0, IBConfig.PRD_CONSTANT.get() * (fails[slot] + 1) + momBonus);
            if (player.getRandom().nextDouble() < chance) {
                procs.add(slot);
                fails[slot] = 0; // 中了，计数清零
            } else {
                fails[slot]++;     // 没中，下次更容易中
            }
        }

        // 有刀触发 -> 剑势叠加并刷新窗口
        if (!procs.isEmpty()) {
            Momentum cur = (mom != null && gameTime < mom.expireTick()) ? mom : new Momentum(0, 0);
            int stacks = Math.min(IBConfig.MOMENTUM_MAX_STACKS.get(), cur.stacks() + 1);
            MOMENTUM.put(id, new Momentum(stacks, gameTime + IBConfig.MOMENTUM_TICKS.get()));
        }

        return procs;
    }

    /** 玩家下线时清理，防内存泄漏 */
    public static void clear(UUID playerId) {
        FAILURES.remove(playerId);
        MOMENTUM.remove(playerId);
    }

    private BladeProcEngine() {
    }
}
