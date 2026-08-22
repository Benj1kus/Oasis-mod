package com.benji.oasiso.common.item;

import com.benji.oasiso.Oasiso;
import com.benji.oasiso.common.entity.BombulEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;

public class BombulBottleItem extends Item {

    public BombulBottleItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();

        if (player == null) {
            return InteractionResult.PASS;
        }


        BlockPos spawnPos = context.getClickedPos().relative(context.getClickedFace());

        if (!(context.getLevel() instanceof ServerLevel serverLevel)) {
            return InteractionResult.SUCCESS;
        }

        BombulEntity bombul = Oasiso.BOMBUL.get().spawn(serverLevel, spawnPos, MobSpawnType.MOB_SUMMONED);


        if (bombul == null) {
            return InteractionResult.FAIL;
        }

        float rotation = context.getRotation();

        bombul.setYRot(rotation);
        bombul.setYHeadRot(rotation);


        bombul.setPersistenceRequired();

        bombul.spawnGoldenBurst(45);

        player.setItemInHand(context.getHand(), new ItemStack(Oasiso.BOMBUL_BOTTLE_EMPTY.get()));

        return InteractionResult.CONSUME;
    }
}