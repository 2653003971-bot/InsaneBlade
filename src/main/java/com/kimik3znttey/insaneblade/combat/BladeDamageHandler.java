package com.kimik3znttey.insaneblade.combat;

import com.kimik3znttey.insaneblade.InsaneBlade;
import com.kimik3znttey.insaneblade.config.IBConfig;
import com.kimik3znttey.insaneblade.network.IBNetwork;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;

/**
 * 伤害挂钩（服务端）：
 * 玩家主手持刀、直接近战命中生物时，对快捷栏其余 8 格的刀逐个掷骰，
 * 摇中的刀按 bonusMode 选定的方式折算进这一刀，再把"哪几格摇中"发给客户端做反馈。
 *
 * 面板读取走纯原版 API（属性修饰符 + 附魔加成），不依赖拔刀剑本体，
 * 没装重锋也不会崩——只是永远匹配不到刀而已。
 *
 * 优先级 LOW：让绝大多数加伤模组（NORMAL 优先级）先把倍率叠进 amount，
 * 我们读到的 x0 就是"叠满整合包 buff 的主手伤害"，RATIO / BLEND 模式才能跟上整合包。
 */
@Mod.EventBusSubscriber(modid = InsaneBlade.MODID)
public final class BladeDamageHandler {

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onHurt(LivingHurtEvent event) {
        LivingEntity target = event.getEntity();
        if (target.level().isClientSide) return; // 伤害是服务端权威

        DamageSource src = event.getSource();
        if (!(src.getEntity() instanceof Player player)) return;
        if (src.getDirectEntity() != player) return; // 只要亲手砍的：刀光实体、幻影剑不算

        ItemStack mainHand = player.getMainHandItem();
        if (!isBlade(mainHand)) return;

        List<Integer> procs = BladeProcEngine.roll(player, target.level().getGameTime());
        if (procs.isEmpty()) return;

        // 快照本刀伤害：减防前的瞬时绝对值，
        // 已过属性/附魔/暴击/冷却缩放，以及排在我们前面的模组倍率
        final double x0 = event.getAmount();
        if (x0 <= 0.0) return;

        final IBConfig.BonusMode mode = IBConfig.BONUS_MODE.get();
        final double alpha = IBConfig.BLEND_ALPHA.get();
        final double mainPanel = bladeAttack(mainHand, target);

        double bonus = 0.0;
        for (int slot : procs) {
            double y = bladeAttack(player.getInventory().getItem(slot), target);
            bonus += switch (mode) {
                // 纯面板：各凭本事，整合包 buff 与八刀无关（行为与旧版一致）
                case FLAT -> y * IBConfig.DAMAGE_SCALE.get();
                // 纯挂钩：以主手伤害为基准，再乘该刀与主刀的面板比，保留强弱刀差异
                case RATIO -> x0 * IBConfig.RATIO.get() * (y / Math.max(1.0, mainPanel));
                // 几何混合：面板^(1-α) × 主手伤害^α；9 把相同的刀时 x0=y，与 FLAT 等价
                case BLEND -> Math.pow(y, 1.0 - alpha) * Math.pow(x0, alpha);
            };
        }

        if (mode == IBConfig.BonusMode.FLAT) {
            // 仅纯面板模式需要手动乘冷却比例：
            // RATIO / BLEND 的 x0 本身已被原版冷却缩放乘过，再乘就是双重惩罚
            bonus *= player.getAttackStrengthScale(0.5f);
        }
        if (bonus <= 0.0) return;

        // 保险丝：总加成不超过主手伤害 × maxBonusRatio，
        // 防排在 LOWEST 优先级的乘算模组把加成再次放大失控
        double maxBonusRatio = IBConfig.MAX_BONUS_RATIO.get();
        if (maxBonusRatio > 0.0) {
            bonus = Math.min(bonus, maxBonusRatio * x0);
        }

        // 加在本次伤害上（LivingHurtEvent 在护甲减免之前，八刀加成同样吃护甲，符合直觉）
        event.setAmount((float) (x0 + bonus));

        // 通知客户端：哪几格摇中了，播音效、炸图标
        IBNetwork.sendProcBurst(player, procs);
    }

    /** 是否视为"刀"（按命名空间判定，配置可加） */
    public static boolean isBlade(ItemStack stack) {
        if (stack.isEmpty()) return false;
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        for (String ns : IBConfig.BLADE_NAMESPACES.get()) {
            if (id.getNamespace().equals(ns)) return true;
        }
        return false;
    }

    /**
     * 读一把刀的面板攻击：
     * 主手属性修饰符里的攻击伤害（拔刀剑的面板就写在这里）+ 附魔对目标生物类型的加成。
     */
    private static double bladeAttack(ItemStack stack, LivingEntity target) {
        double attack = 0.0;
        var modifiers = stack.getAttributeModifiers(EquipmentSlot.MAINHAND);
        for (AttributeModifier m : modifiers.get(Attributes.ATTACK_DAMAGE)) {
            attack += m.getAmount();
        }
        attack += EnchantmentHelper.getDamageBonus(stack, target.getMobType());
        return attack > 0.0 ? attack : IBConfig.FALLBACK_ATTACK.get();
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        BladeProcEngine.clear(event.getEntity().getUUID());
    }

    private BladeDamageHandler() {
    }
}
