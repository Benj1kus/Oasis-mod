package com.benji.oasiso.common.geyser;

import com.benji.oasiso.Oasiso;
import com.benji.oasiso.common.block.GeyserBigBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = Oasiso.MODID)
public final class BigGeyserContactDamage {
    public static void touch(Entity entity) {
        if (!entity.level().isClientSide && entity instanceof LivingEntity living && !entity.isSteppingCarefully() && !EnchantmentHelper.hasFrostWalker(living))
            entity.hurt(entity.damageSources().hotFloor(), 1.0F);
    }

    @SubscribeEvent
    public static void tick(LivingEvent.LivingTickEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity.level().isClientSide || !entity.isAlive() || !entity.onGround()) return;
        var box = entity.getBoundingBox();
        int y = BlockPos.containing(entity.getX(), box.minY - 26.0 / 16.0 + .03, entity.getZ()).getY();
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        int minX = (int) Math.floor(box.minX), maxX = (int) Math.floor(box.maxX);
        int minZ = (int) Math.floor(box.minZ), maxZ = (int) Math.floor(box.maxZ);
        for (int x = minX; x <= maxX; x++)
            for (int z = minZ; z <= maxZ; z++) {
                pos.set(x, y, z);
                if (!entity.level().hasChunkAt(pos) || !(entity.level().getBlockState(pos).getBlock() instanceof GeyserBigBlock))
                    continue;
                if (Math.abs(box.minY - (y + 26.0 / 16.0)) <= .04 && box.maxX > x + 3.0 / 16.0 && box.minX < x + 13.0 / 16.0 && box.maxZ > z + 3.0 / 16.0 && box.minZ < z + 13.0 / 16.0) {
                    touch(entity);
                    return;
                }
            }
    }

    private BigGeyserContactDamage() {
    }
}
