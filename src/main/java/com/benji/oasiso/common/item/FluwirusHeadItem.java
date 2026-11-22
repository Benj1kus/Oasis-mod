package com.benji.oasiso.common.item;

import com.benji.oasiso.common.fluwirussort.FluwirusSort;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

public final class FluwirusHeadItem extends Item {
    public FluwirusHeadItem(Properties properties) { super(properties); }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        if (player instanceof ServerPlayer server) FluwirusSort.personal(server, this);
        return InteractionResultHolder.sidedSuccess(player.getItemInHand(hand), level.isClientSide);
    }

    @Override
    public InteractionResult onItemUseFirst(ItemStack stack, UseOnContext ctx) {
        Player player = ctx.getPlayer();
        if (player == null) return InteractionResult.PASS;
        if (player.isShiftKeyDown() && FluwirusSort.storage(ctx.getLevel(), ctx.getClickedPos())) {
            if (player instanceof ServerPlayer server) FluwirusSort.awaitStorage(server, ctx.getClickedPos(), this);
            return InteractionResult.PASS;
        }
        if (player instanceof ServerPlayer server) FluwirusSort.personal(server, this);
        return InteractionResult.sidedSuccess(ctx.getLevel().isClientSide);
    }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        // A locked or obstructed chest must not fall through to the air-use action.
        return InteractionResult.sidedSuccess(ctx.getLevel().isClientSide);
    }
}
