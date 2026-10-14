package com.benji.oasiso.common.block.entity;

import com.benji.oasiso.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import software.bernie.geckolib.animatable.GeoBlockEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.*;
import software.bernie.geckolib.util.GeckoLibUtil;

public final class FactoryCoreBlockEntity extends BlockEntity implements GeoBlockEntity {
    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("idle");
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public FactoryCoreBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.FACTORY_CORE_BE.get(), pos, state);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<FactoryCoreBlockEntity>(this, "idle", 0, state -> state.setAndContinue(IDLE)));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    private int projectileHits;
    private long detonateAt = -1;
    private boolean detonated;

    public long getDetonateAt() {
        return detonateAt;
    }

    public static void serverTick(net.minecraft.world.level.Level world, BlockPos pos, BlockState state, FactoryCoreBlockEntity core) {
        if (!(world instanceof net.minecraft.server.level.ServerLevel server) || !core.isArmed() || core.detonated)
            return;
        if (core.detonateAt < 0) {
            core.detonateAt = world.getGameTime() + com.benji.oasiso.common.factory.FactoryCoreBlastRules.CHARGE_TICKS;
            core.setChanged();
            world.sendBlockUpdated(pos, state, state, 3);
        }
        if (world.getGameTime() >= core.detonateAt) {
            core.detonated = true;
            com.benji.oasiso.common.factory.FactoryCoreDetonation.explode(server, pos);
        }
    }

    public int getProjectileHits() {
        return projectileHits;
    }

    public boolean isArmed() {
        return projectileHits >= 3;
    }

    public void acceptProjectile(net.minecraft.world.entity.projectile.Projectile projectile) {
        if (level == null || level.isClientSide || isArmed()) return;
        var data = projectile.getPersistentData();
        if (data.getBoolean("OasisoFactoryCoreHit")) return;
        data.putBoolean("OasisoFactoryCoreHit", true);
        projectileHits++;
        float pitch = switch (projectileHits) {
            case 1 -> 0.5F;
            case 2 -> 1.0F;
            default -> 2.0F;
        };
        level.playSound(null, worldPosition, com.benji.oasiso.ModSounds.ECHO1.get(), net.minecraft.sounds.SoundSource.BLOCKS, 2.0F, pitch);
        if (isArmed()) {
            detonateAt = level.getGameTime()
                    + com.benji.oasiso.common.factory.FactoryCoreBlastRules.CHARGE_TICKS;

            if (level instanceof net.minecraft.server.level.ServerLevel server
                    && projectile.getOwner() instanceof net.minecraft.world.entity.player.Player player) {
                com.benji.oasiso.common.entity.CrusaderScienceEntity.onCoreDetonationStarted(
                        server,
                        com.benji.oasiso.common.factory.FactoryCoreDetonation.center(worldPosition),
                        player
                );
            }
        }
        setChanged();
        level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);

    }

    @Override
    protected void saveAdditional(net.minecraft.nbt.CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putInt("CoreProjectileHits", projectileHits);
        tag.putLong("CoreDetonateAt", detonateAt);
    }

    @Override
    public void load(net.minecraft.nbt.CompoundTag tag) {
        super.load(tag);
        projectileHits = Math.max(0, Math.min(3, tag.getInt("CoreProjectileHits")));
        detonateAt = tag.contains("CoreDetonateAt") ? tag.getLong("CoreDetonateAt") : -1;
        detonated = false;
    }

    @Override
    public net.minecraft.nbt.CompoundTag getUpdateTag() {
        var tag = super.getUpdateTag();
        tag.putInt("CoreProjectileHits", projectileHits);
        tag.putLong("CoreDetonateAt", detonateAt);
        return tag;
    }

    @Override
    public net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket getUpdatePacket() {
        return net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket.create(this);
    }

    public long nextLightning;

    @Override
    public AABB getRenderBoundingBox() {
        return new AABB(worldPosition.getX() - 32, worldPosition.getY() - 30, worldPosition.getZ() - 32, worldPosition.getX() + 33, worldPosition.getY() + 36, worldPosition.getZ() + 33);
    }
}
