package com.benji.oasiso.common.entity.ai;

import com.benji.oasiso.Oasiso;
import com.benji.oasiso.common.entity.PaladinEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BarrelBlockEntity;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

public final class PaladinDeathManager {

    // death ~4.33 s
    public static final int DEATH_DURATION = 87;

    private static final int BODY_PARTICLE_INTERVAL = 2;

    private static final String DATA_TAG = "PaladinDeath";

    private final PaladinEntity boss;

    private boolean active;
    private int deathTicks;
    private UUID killerId;

    public PaladinDeathManager(PaladinEntity boss) {
        this.boss = boss;
    }

    public void begin(ServerLevel level, DamageSource source) {
        if (this.active) {
            return;
        }
        this.active = true;
        this.deathTicks = 0;

        Entity attacker = source.getEntity();
        if (attacker instanceof Player player) {
            this.killerId = player.getUUID();

        } else {
            this.killerId = null;
        }

        boss.setHealth(0.01F);
        boss.setAnimState(PaladinEntity.STATE_DEATH);
        boss.setNoAi(true);
        boss.getNavigation().stop();
        boss.setDeltaMovement(Vec3.ZERO);
        boss.fallDistance = 0.0F;
    }

    public boolean tick(ServerLevel level) {
        if (!this.active) {
            return false;
        }
        this.deathTicks++;
        boss.setDeltaMovement(Vec3.ZERO);
        if (this.deathTicks % BODY_PARTICLE_INTERVAL == 0) {
            spawnBodyParticles(level);
        }
        if (this.deathTicks < DEATH_DURATION) {
            return false;
        }

        spawnDeathExplosion(level);
        spawnRewardBarrel(level);

        this.active = false;
        return true;
    }


    private void spawnBodyParticles(ServerLevel level) {
        double progress = Mth.clamp(this.deathTicks / (double) DEATH_DURATION, 0.0D, 1.0D);
        int count = 7 + Mth.floor(progress * 10.0D);
        double width = Math.max(0.7D, boss.getBbWidth() * 0.65D);
        double height = boss.getBbHeight() * 0.46D;
        level.sendParticles(Oasiso.PURPLE_STARS.get(), boss.getX(), boss.getY() + boss.getBbHeight() * 0.5D, boss.getZ(), count, width, height, width, 0.035D);
    }

    private void spawnDeathExplosion(ServerLevel level) {
        double x = boss.getX();
        double y = boss.getY() + boss.getBbHeight() * 0.48D;
        double z = boss.getZ();

        level.sendParticles(Oasiso.CHAOS_BOMB_CENTER_SMOKE.get(), x, y, z, 10, 0.45D, 0.65D, 0.45D, 0.04D);

        level.sendParticles(Oasiso.CHAOS_BOMB_FIRE_SMOKE.get(), x, y, z, 38, 1.35D, 1.75D, 1.35D, 0.13D);

        level.sendParticles(Oasiso.CHAOS_BOMB_SPARKS.get(), x, y, z, 80, 1.15D, 1.55D, 1.15D, 0.27D);

        level.playSound(null, x, y, z, SoundEvents.GENERIC_EXPLODE, SoundSource.HOSTILE, 2.0F, 0.72F);
    }


    private void spawnRewardBarrel(ServerLevel level) {
        int x = Mth.floor(boss.getX());
        int z = Mth.floor(boss.getZ());
        int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
        BlockPos barrelPos = new BlockPos(x, y, z);

        level.setBlock(barrelPos, Blocks.BARREL.defaultBlockState(), 3);

        if (!(level.getBlockEntity(barrelPos) instanceof BarrelBlockEntity barrel)) {
            return;
        }
        barrel.setItem(0, new ItemStack(Oasiso.ORB_CHAOS.get(), 1));
        barrel.setItem(1, new ItemStack(Oasiso.ORB_DOMINATION.get(), 1));

        barrel.setChanged();
    }

    public ServerPlayer resolveKiller(ServerLevel level) {
        if (this.killerId == null) {
            return null;
        }
        ServerPlayer player = level.getServer().getPlayerList().getPlayer(this.killerId);
        if (player == null || player.serverLevel() != level) {
            return null;
        }

        return player;
    }

    public void save(CompoundTag parent) {
        CompoundTag tag = new CompoundTag();

        tag.putBoolean("Active", this.active);
        tag.putInt("DeathTicks", this.deathTicks);

        if (this.killerId != null) {
            tag.putUUID("Killer", this.killerId);
        }
        parent.put(DATA_TAG, tag);
    }


    public void load(CompoundTag parent) {
        if (!parent.contains(DATA_TAG)) {
            return;
        }


        CompoundTag tag = parent.getCompound(DATA_TAG);

        this.active = tag.getBoolean("Active");
        this.deathTicks = tag.getInt("DeathTicks");
        this.killerId = tag.hasUUID("Killer") ? tag.getUUID("Killer") : null;
    }
}