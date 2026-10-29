package com.benji.oasiso.common.item;

import com.benji.oasiso.common.fault.FaultSystem;
import com.benji.oasiso.common.fault.SoulBottleNetwork;
import com.benji.oasiso.registry.ModItems;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;

public class SoulBotItem extends Item {
    public static final int DRINK_TICKS = 32, BUFF_TICKS = 300;

    public SoulBotItem(Properties properties) {
        super(properties);
    }

    @Override
    public int getUseDuration(ItemStack stack) {
        return DRINK_TICKS;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.DRINK;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        if (player.isSpectator()) return InteractionResultHolder.fail(player.getItemInHand(hand));
        return ItemUtils.startUsingInstantly(level, player, hand);
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity user) {
        if (!(user instanceof ServerPlayer player) || !player.isAlive() || player.isSpectator()) return stack;
        CriteriaTriggers.CONSUME_ITEM.trigger(player, stack);
        player.awardStat(Stats.ITEM_USED.get(this));
        int remaining = FaultSystem.drink(player);
        player.addEffect(new MobEffectInstance(MobEffects.GLOWING, BUFF_TICKS, 0));
        player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, BUFF_TICKS, 1));
        player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, BUFF_TICKS, 1));
        SoulBottleNetwork.send(player, remaining);
        stack.shrink(1);
        ItemStack empty = new ItemStack(ModItems.BOMBUL_BOTTLE_EMPTY.get());
        if (stack.isEmpty()) return empty;
        if (!player.getInventory().add(empty)) player.drop(empty, false);
        return stack;
    }
}
