package com.benji.oasiso.common.item;

import com.benji.oasiso.common.shrine.WarCoreRitual;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;

public class WarCoreItem extends Item {
    public static final int CHARGE_TICKS = 100;

    public WarCoreItem(Properties properties) {
        super(properties);
    }

    @Override
    public int getUseDuration(ItemStack stack) {
        return CHARGE_TICKS;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.NONE;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.isSpectator() || player instanceof ServerPlayer server && !WarCoreRitual.canUse(server, stack))
            return InteractionResultHolder.fail(stack);
        if (player.isShiftKeyDown()) {
            if (player instanceof ServerPlayer server) WarCoreRitual.complete(server, stack, true);
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
        }
        player.startUsingItem(hand);
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity user) {
        if (user instanceof ServerPlayer server) WarCoreRitual.complete(server, stack, false);
        return stack;
    }
}
