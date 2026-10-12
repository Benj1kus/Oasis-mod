package com.benji.oasiso.common.factory;

import com.benji.oasiso.Oasiso;
import com.benji.oasiso.common.block.FactoryCoreBlock;
import com.benji.oasiso.common.block.FactoryCorePartBlock;
import com.benji.oasiso.common.block.entity.FactoryCoreBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.event.entity.ProjectileImpactEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = Oasiso.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class FactoryCoreProjectileHandler {
    private FactoryCoreProjectileHandler() {
    }

    public static boolean hit(Level level, BlockPos pos, Projectile projectile) {
        var state = level.getBlockState(pos);
        BlockPos root;
        if (state.getBlock() instanceof FactoryCorePartBlock) root = FactoryCorePartBlock.root(pos, state);
        else if (state.getBlock() instanceof FactoryCoreBlock) root = pos;
        else return false;
        if (!level.hasChunkAt(root) || !(level.getBlockEntity(root) instanceof FactoryCoreBlockEntity core))
            return false;
        if (!level.isClientSide) core.acceptProjectile(projectile);
        return true;
    }

    @SubscribeEvent
    public static void impact(ProjectileImpactEvent event) {
        if (!(event.getRayTraceResult() instanceof BlockHitResult hit)) return;
        if (event.getImpactResult() != ProjectileImpactEvent.ImpactResult.DEFAULT) return;
        Projectile projectile = event.getProjectile();
        if (!hit(projectile.level(), hit.getBlockPos(), projectile)) return;
        if (!(projectile instanceof AbstractArrow)) {
            event.setImpactResult(ProjectileImpactEvent.ImpactResult.STOP_AT_CURRENT_NO_DAMAGE);
            projectile.discard();
        }
    }
}
