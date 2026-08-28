package com.benji.oasiso.common.entity.ai;

import com.benji.oasiso.Oasiso;
import com.benji.oasiso.common.entity.AzumaalEntity;
import com.benji.oasiso.common.entity.BossPortalEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import com.benji.oasiso.ModSounds;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import com.benji.oasiso.common.dimension.BossArenaEncounter;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BarrelBlockEntity;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

public final class AzumaalDeathManager {

    public static final int DEATH_DURATION = 20 * 14;

    private static final int BODY_PARTICLE_INTERVAL = 2;

    private static final ResourceLocation BARREL_LOOT_TABLE = ResourceLocation.fromNamespaceAndPath(Oasiso.MODID, "chests/azumaal_barrel");

    private static final String DATA_TAG = "AzumaalDeath";

    private final AzumaalEntity boss;

    private boolean active;
    private int deathTicks;

    private UUID killerId;
    private UUID portalId;

    public AzumaalDeathManager(AzumaalEntity boss) {
        this.boss = boss;
    }

    public void setPortal(BossPortalEntity portal) {
        this.portalId = portal.getUUID();
    }

    public void begin(ServerLevel level, DamageSource source) {
        if (this.active) {
            return;
        }
        this.active = true;
        this.deathTicks = 0;
        boss.setDeathVisualTicks(0);

        level.playSound(null, boss.getX(), boss.getY() + boss.getBbHeight() * 0.5D, boss.getZ(), ModSounds.AZUMAAL_DEATH.get(), SoundSource.HOSTILE, 2.0F, 1.0F);

        Entity attacker = source.getEntity();
        if (attacker instanceof Player player) {
            this.killerId = player.getUUID();
        } else {
            this.killerId = null;
        }

        boss.setHealth(0.01F);
        boss.setAnimState(AzumaalEntity.STATE_DEATH);
        boss.setDeltaMovement(Vec3.ZERO);
        boss.fallDistance = 0.0F;
    }

    public boolean tick(ServerLevel level) {
        if (!this.active) {
            return false;
        }

        this.deathTicks++;

        boss.setDeathVisualTicks(this.deathTicks);

        if (this.deathTicks % BODY_PARTICLE_INTERVAL == 0) {

            spawnBodyParticles(level);
        }

        if (this.deathTicks < DEATH_DURATION) {
            return false;
        }

        spawnDeathExplosion(level);
        spawnRewardBarrel(level);
        startPortalDespawning(level);
        spawnChaosReturnPortal(level);

        this.active = false;
        return true;
    }

    private void spawnChaosReturnPortal(ServerLevel level) {
        if (!level.dimension().equals(Oasiso.CHAOS_DIMENSION)) {
            return;
        }

        ServerPlayer player = findArenaPlayer(level);
        if (player == null) {
            return;
        }
        Vec3 portalPosition = findReturnPortalPosition(level, player);
        BossPortalEntity portal = Oasiso.BOSS_PORTAL.get().create(level);

        if (portal == null) {
            return;
        }
        portal.moveTo(portalPosition.x, portalPosition.y, portalPosition.z, 0.0F, 0.0F);
        level.addFreshEntity(portal);
        portal.startOpening(
                BossPortalEntity.PortalPurpose.CHAOS_RETURN,
                boss.getArenaSessionId()
        );

        level.playSound(null, portal.getX(), portal.getY(), portal.getZ(), ModSounds.PORTAL_OPEN.get(), SoundSource.BLOCKS, 1.25F, 1.0F);
    }

    private ServerPlayer findArenaPlayer(ServerLevel level) {

        UUID sessionId =
                boss.getArenaSessionId();

        ServerPlayer killer =
                resolveKiller(level);

        if (killer != null
                && BossArenaEncounter.isArenaSession(killer)
                && (sessionId == null
                || BossArenaEncounter.isPlayerInSession(
                killer,
                sessionId
        ))) {

            return killer;
        }

        ServerPlayer nearest = null;
        double nearestDistance =
                Double.MAX_VALUE;

        for (ServerPlayer player :
                level.players()) {

            if (!player.isAlive()
                    || !BossArenaEncounter.isArenaSession(
                    player
            )) {

                continue;
            }

            if (sessionId != null
                    && !BossArenaEncounter.isPlayerInSession(
                    player,
                    sessionId
            )) {

                continue;
            }

            double distance =
                    boss.distanceToSqr(
                            player
                    );

            if (distance >= nearestDistance) {
                continue;
            }

            nearest = player;
            nearestDistance = distance;
        }

        return nearest;
    }

    private Vec3 findReturnPortalPosition(ServerLevel level, ServerPlayer player) {
        int playerY = Mth.floor(player.getY());

        for (int attempt = 0; attempt < 16; attempt++) {
            double angle = player.getRandom().nextDouble() * Math.PI * 2.0D;
            double distance = 4.0D + player.getRandom().nextDouble() * 3.5D;

            int x = Mth.floor(player.getX() + Math.cos(angle) * distance);
            int z = Mth.floor(player.getZ() + Math.sin(angle) * distance);

            for (int y = playerY + 4; y >= playerY - 6; y--) {
                BlockPos floor = new BlockPos(x, y, z);

                if (level.getBlockState(floor).getCollisionShape(level, floor).isEmpty()) {
                    continue;
                }

                BlockPos above = floor.above();

                if (!level.getBlockState(above).isAir()) {
                    continue;
                }

                if (!level.getBlockState(above.above()).isAir()) {
                    continue;
                }
                return new Vec3(x + 0.5D, y + 1.03D, z + 0.5D);
            }
        }
        return new Vec3(player.getX() + 3.5D, player.getY() + 0.03D, player.getZ());
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
        barrel.setLootTable(BARREL_LOOT_TABLE, level.random.nextLong());
        barrel.setChanged();
    }

    private void startPortalDespawning(ServerLevel level) {
        BossPortalEntity portal = resolvePortal(level);
        if (portal == null) {
            return;
        }
        portal.startDespawning();
    }

    private BossPortalEntity resolvePortal(ServerLevel level) {
        if (this.portalId == null) {
            return null;
        }

        Entity entity = level.getEntity(this.portalId);

        if (entity instanceof BossPortalEntity portal) {
            return portal;
        }
        return null;
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

    public boolean isActive() {
        return this.active;
    }

    public void save(CompoundTag parent) {
        CompoundTag tag = new CompoundTag();

        tag.putBoolean("Active", this.active);

        tag.putInt("DeathTicks", this.deathTicks);

        if (this.killerId != null) {
            tag.putUUID("Killer", this.killerId);
        }

        if (this.portalId != null) {
            tag.putUUID("Portal", this.portalId);
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

        boss.setDeathVisualTicks(this.deathTicks);

        this.killerId = tag.hasUUID("Killer") ? tag.getUUID("Killer") : null;

        this.portalId = tag.hasUUID("Portal") ? tag.getUUID("Portal") : null;
    }
}