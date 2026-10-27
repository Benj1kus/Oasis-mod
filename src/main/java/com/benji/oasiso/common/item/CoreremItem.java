package com.benji.oasiso.common.item;

import com.benji.oasiso.common.corerem.CoreremServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

public final class CoreremItem extends Item {
    public CoreremItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide) return InteractionResultHolder.success(stack);
        return player instanceof ServerPlayer server && CoreremServer.start(server) ? InteractionResultHolder.consume(stack) : InteractionResultHolder.fail(stack);
    }

    @Override
    public InteractionResult onItemUseFirst(ItemStack stack, UseOnContext context) {
        return useOn(context);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        return player == null ? InteractionResult.PASS : use(context.getLevel(), player, context.getHand()).getResult();
    }

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity target, InteractionHand hand) {
        return use(player.level(), player, hand).getResult();
    }
}
