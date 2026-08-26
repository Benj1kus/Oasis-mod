package com.benji.oasiso.common.entity.ai;

import com.benji.oasiso.config.OsirisRealmConfig;

import com.benji.oasiso.Oasiso;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

public final class AzumaalStunManager {

    private static final int PARTICLE_INTERVAL = 2;
    private static final String DATA_TAG = "AzumaalActiveStuns";

    private final Map<UUID, StunData> activeStuns = new HashMap<>();

    public void stun(ServerPlayer player) {

        StunData data = new StunData(player.getX(), player.getY(), player.getZ(), OsirisRealmConfig.AZUMAAL_STUN_DURATION.get());
        this.activeStuns.put(player.getUUID(), data);

        player.addEffect(new MobEffectInstance(MobEffects.DARKNESS, OsirisRealmConfig.AZUMAAL_STUN_DEBUFF_DURATION.get(), 0, false, true));
        player.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, OsirisRealmConfig.AZUMAAL_STUN_DEBUFF_DURATION.get(), 0, false, true));
        player.addEffect(new MobEffectInstance(Oasiso.ENTROPY_EFFECT.get(), OsirisRealmConfig.AZUMAAL_STUN_DEBUFF_DURATION.get(), 0, false, true));

        player.setDeltaMovement(Vec3.ZERO);

        player.hurtMarked = true;
        player.fallDistance = 0.0F;

        spawnInitialBurst(player.serverLevel(), player);
    }

    public void tick(ServerLevel level) {
        if (this.activeStuns.isEmpty()) {
            return;
        }
        Iterator<Map.Entry<UUID, StunData>> iterator = this.activeStuns.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, StunData> entry = iterator.next();
            ServerPlayer player = level.getServer().getPlayerList().getPlayer(entry.getKey());

            if (player == null || !player.isAlive() || player.serverLevel() != level || player.isCreative() || player.isSpectator()) {
                iterator.remove();
                continue;
            }

            StunData stun = entry.getValue();
            lockPlayer(player, stun);

            if (stun.ticksRemaining % PARTICLE_INTERVAL == 0) {
                spawnBindingParticles(level, player, stun);
            }

            stun.ticksRemaining--;
            if (stun.ticksRemaining <= 0) {
                spawnReleaseBurst(level, player);
                iterator.remove();
            }
        }
    }

    private void lockPlayer(ServerPlayer player, StunData stun) {
        player.setDeltaMovement(Vec3.ZERO);
        player.hurtMarked = true;
        player.fallDistance = 0.0F;
        player.connection.teleport(stun.x, stun.y, stun.z, player.getYRot(), player.getXRot());
    }

    private void spawnInitialBurst(ServerLevel level, ServerPlayer player) {
        level.sendParticles(Oasiso.PURPLE_STARS.get(), player.getX(), player.getY() + player.getBbHeight() * 0.5D, player.getZ(), 30, 0.55D, player.getBbHeight() * 0.42D, 0.55D, 0.09D);
    }

    private void spawnBindingParticles(ServerLevel level, ServerPlayer player, StunData stun) {

        final int rings = 3;
        final int particlesPerRing = 4;

        double radius = Math.max(0.55D, player.getBbWidth() * 0.9D);
        double height = player.getBbHeight();
        double elapsed = OsirisRealmConfig.AZUMAAL_STUN_DURATION.get() - stun.ticksRemaining;
        double mainPhase = elapsed * 0.28D;

        for (int ring = 0; ring < rings; ring++) {

            double ringY = player.getY() + height * (0.24D + ring * 0.26D);
            double direction = ring % 2 == 0 ? 1.0D : -1.0D;
            double phase = mainPhase * direction + ring * 0.75D;

            for (int point = 0; point < particlesPerRing; point++) {
                double angle = phase + point * (Math.PI * 2.0D / particlesPerRing);

                double x = player.getX() + Math.cos(angle) * radius;
                double z = player.getZ() + Math.sin(angle) * radius;
                double y = ringY + Math.sin(angle * 2.0D + mainPhase) * 0.07D;

                level.sendParticles(Oasiso.PURPLE_STARS.get(), x, y, z, 1, 0.015D, 0.015D, 0.015D, 0.0D);
            }
        }
    }

    private void spawnReleaseBurst(ServerLevel level, ServerPlayer player) {
        level.sendParticles(Oasiso.PURPLE_STARS.get(), player.getX(), player.getY() + player.getBbHeight() * 0.5D, player.getZ(), 18, 0.5D, player.getBbHeight() * 0.35D, 0.5D, 0.08D);
    }

    public void reset() {
        this.activeStuns.clear();
    }

    public void save(CompoundTag parent) {
        ListTag list = new ListTag();

        for (Map.Entry<UUID, StunData> entry : this.activeStuns.entrySet()) {

            CompoundTag stunTag = new CompoundTag();

            stunTag.putUUID("Player", entry.getKey());

            StunData data = entry.getValue();

            stunTag.putDouble("X", data.x);
            stunTag.putDouble("Y", data.y);
            stunTag.putDouble("Z", data.z);
            stunTag.putInt("Ticks", data.ticksRemaining);

            list.add(stunTag);
        }
        parent.put(DATA_TAG, list);
    }

    public void load(CompoundTag parent) {
        this.activeStuns.clear();
        if (!parent.contains(DATA_TAG, Tag.TAG_LIST)) {
            return;
        }

        ListTag list = parent.getList(DATA_TAG, Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {

            CompoundTag stunTag = list.getCompound(i);
            if (!stunTag.hasUUID("Player")) {
                continue;
            }
            int ticks = stunTag.getInt("Ticks");
            if (ticks <= 0) {
                continue;
            }
            this.activeStuns.put(stunTag.getUUID("Player"), new StunData(stunTag.getDouble("X"), stunTag.getDouble("Y"), stunTag.getDouble("Z"), ticks));
        }
    }

    private static final class StunData {

        private final double x;
        private final double y;
        private final double z;

        private int ticksRemaining;

        private StunData(double x, double y, double z, int ticksRemaining) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.ticksRemaining = ticksRemaining;
        }
    }
}