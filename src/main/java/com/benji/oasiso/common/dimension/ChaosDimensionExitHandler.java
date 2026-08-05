package com.benji.oasiso.common.dimension;

import com.benji.oasiso.ModSounds;
import com.benji.oasiso.Oasiso;
import com.benji.oasiso.common.entity.KrombulEntity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Mod.EventBusSubscriber(
        modid = Oasiso.MODID,
        bus = Mod.EventBusSubscriber.Bus.FORGE
)
public final class ChaosDimensionExitHandler {

    /*
     * chaos_moon6 начинается после 30 секунд.
     */
    private static final int OUTCOME_START_TIME = 600;

    /*
     * Поглощение длится 5 секунд.
     */
    private static final int CONSUMING_TIME = 100;

    /*
     * Обе анимации Krombul имеют длину 0,75 секунды.
     */
    private static final int KROMBUL_ANIMATION_TIME = 15;

    private static final String STAY_TICKS_TAG =
            Oasiso.MODID + ":chaos_stay_ticks";

    private static final Map<UUID, ExitSequence>
            ACTIVE_SEQUENCES = new HashMap<>();

    private ChaosDimensionExitHandler() {
    }

    @SubscribeEvent
    public static void onPlayerTick(
            TickEvent.PlayerTickEvent event
    ) {
        if (event.phase != TickEvent.Phase.END
                || event.player.level().isClientSide
                || !(event.player
                instanceof ServerPlayer player)) {
            return;
        }

        ExitSequence activeSequence =
                ACTIVE_SEQUENCES.get(
                        player.getUUID()
                );


        if (activeSequence != null) {
            tickActiveSequence(
                    player,
                    activeSequence
            );
            return;
        }

        if (!player.level().dimension()
                .equals(Oasiso.CHAOS_DIMENSION)) {

            player.getPersistentData()
                    .remove(STAY_TICKS_TAG);

            return;
        }

        CompoundTag data =
                player.getPersistentData();

        int stayTicks =
                data.getInt(STAY_TICKS_TAG) + 1;

        data.putInt(
                STAY_TICKS_TAG,
                stayTicks
        );

        if (stayTicks >= OUTCOME_START_TIME) {
            beginOutcome(player);
        }
    }

    private static void beginOutcome(
            ServerPlayer player
    ) {
        MinecraftServer server =
                player.getServer();

        if (server == null) {
            return;
        }

        ServerLevel overworld =
                server.getLevel(Level.OVERWORLD);

        if (overworld == null) {
            return;
        }

        long gameDay =
                Math.floorDiv(
                        overworld.getDayTime(),
                        24_000L
                ) + 1L;

        if (gameDay % 2L == 0L) {

            playPlayerSound(
                    player,
                    ModSounds.CHAOS_DEATH.get(),
                    1.0F,
                    1.0F
            );


            playPlayerSound(
                    player,
                    ModSounds.ECHO_STARS.get(),
                    1.0F,
                    1.0F
            );

            ACTIVE_SEQUENCES.put(
                    player.getUUID(),
                    new ExitSequence(
                            ExitPhase.CONSUMING
                    )
            );

            return;
        }

        playPlayerSound(
                player,
                ModSounds.CHAOS_LIFE.get(),
                1.0F,
                1.0F
        );

        KrombulEntity actor =
                createKrombulActor(
                        player.serverLevel(),
                        player,
                        KrombulEntity.STATE_TP_START
                );

        ExitSequence sequence =
                new ExitSequence(
                        ExitPhase.KROMBUL_START
                );

        if (actor != null) {
            sequence.actorId =
                    actor.getUUID();
        }

        ACTIVE_SEQUENCES.put(
                player.getUUID(),
                sequence
        );

        spawnBurst(
                player.serverLevel(),
                player.getX(),
                player.getY() + 1.0D,
                player.getZ(),
                45
        );

        playTeleportSound(
                player.serverLevel(),
                player.getX(),
                player.getY(),
                player.getZ(),
                player.getRandom()
        );
    }

    private static void playPlayerSound(
            ServerPlayer player,
            SoundEvent sound,
            float volume,
            float pitch
    ) {

        player.playNotifySound(
                sound,
                SoundSource.AMBIENT,
                volume,
                pitch
        );
    }

    private static void tickActiveSequence(
            ServerPlayer player,
            ExitSequence sequence
    ) {
        switch (sequence.phase) {
            case CONSUMING ->
                    tickConsuming(player, sequence);

            case KROMBUL_START ->
                    tickKrombulStart(player, sequence);

            case KROMBUL_END ->
                    tickKrombulEnd(player, sequence);
        }
    }

    private static void tickConsuming(
            ServerPlayer player,
            ExitSequence sequence
    ) {
        if (!player.level().dimension()
                .equals(Oasiso.CHAOS_DIMENSION)) {
            cleanup(player);
            return;
        }

        sequence.ticks++;


        Vec3 movement =
                player.getDeltaMovement();

        player.setDeltaMovement(
                movement.x * 0.35D,
                movement.y * 0.25D,
                movement.z * 0.35D
        );

        player.hurtMarked = true;
        player.fallDistance = 0.0F;

        spawnConsumingParticles(
                player,
                sequence.ticks
        );

        if (sequence.ticks < CONSUMING_TIME) {
            return;
        }

        finishConsuming(player);
    }

    private static void spawnConsumingParticles(
            ServerPlayer player,
            int sequenceTicks
    ) {
        ServerLevel level =
                player.serverLevel();

        RandomSource random =
                player.getRandom();

        double progress =
                Math.min(
                        1.0D,
                        sequenceTicks
                                / (double) CONSUMING_TIME
                );


        double particleY =
                player.getY()
                        - 0.15D
                        + progress
                        * (player.getBbHeight() + 0.55D);

        for (int i = 0; i < 6; i++) {
            double angle =
                    random.nextDouble()
                            * Math.PI
                            * 2.0D;

            double radius =
                    0.18D
                            + random.nextDouble()
                            * player.getBbWidth()
                            * 0.75D;

            double x =
                    player.getX()
                            + Math.cos(angle) * radius;

            double y =
                    particleY
                            + (random.nextDouble() - 0.5D)
                            * 0.18D;

            double z =
                    player.getZ()
                            + Math.sin(angle) * radius;

            double velocityX =
                    (random.nextDouble() - 0.5D)
                            * 0.035D;

            double velocityY =
                    0.07D
                            + random.nextDouble()
                            * 0.07D;

            double velocityZ =
                    (random.nextDouble() - 0.5D)
                            * 0.035D;

            level.sendParticles(
                    Oasiso.PURPLE_STARS.get(),
                    x,
                    y,
                    z,
                    0,
                    velocityX,
                    velocityY,
                    velocityZ,
                    1.0D
            );
        }
    }

    private static void finishConsuming(
            ServerPlayer player
    ) {
        ACTIVE_SEQUENCES.remove(
                player.getUUID()
        );

        player.getPersistentData()
                .remove(STAY_TICKS_TAG);

        ChaosReturnData.clear(player);

        spawnBurst(
                player.serverLevel(),
                player.getX(),
                player.getY()
                        + player.getBbHeight() * 0.5D,
                player.getZ(),
                140
        );


        var damageSource =
                player.damageSources().magic();

        player.hurt(
                damageSource,
                Float.MAX_VALUE
        );


        if (player.isAlive()) {
            player.setHealth(0.0F);
            player.die(damageSource);
        }
    }


    private static void tickKrombulStart(
            ServerPlayer player,
            ExitSequence sequence
    ) {
        freezePlayer(player);

        sequence.ticks++;

        if (sequence.ticks < KROMBUL_ANIMATION_TIME) {
            return;
        }

        returnPlayerToOverworld(
                player,
                sequence
        );
    }

    private static void returnPlayerToOverworld(
            ServerPlayer player,
            ExitSequence sequence
    ) {
        MinecraftServer server =
                player.getServer();

        if (server == null) {
            cleanup(player);
            return;
        }

        ServerLevel overworld =
                server.getLevel(Level.OVERWORLD);

        if (overworld == null) {
            cleanup(player);
            return;
        }

        ServerLevel chaosLevel =
                player.serverLevel();

        discardActor(
                chaosLevel,
                sequence.actorId
        );

        spawnBurst(
                chaosLevel,
                player.getX(),
                player.getY() + 1.0D,
                player.getZ(),
                65
        );

        playTeleportSound(
                chaosLevel,
                player.getX(),
                player.getY(),
                player.getZ(),
                player.getRandom()
        );

        ChaosReturnData.ReturnLocation destination =
                ChaosReturnData.load(
                        player,
                        overworld
                );

        player.stopRiding();

        player.teleportTo(
                overworld,
                destination.x(),
                destination.y(),
                destination.z(),
                destination.yaw(),
                destination.pitch()
        );

        player.setNoGravity(false);
        player.setDeltaMovement(Vec3.ZERO);
        player.fallDistance = 0.0F;
        player.hurtMarked = true;

        spawnBurst(
                overworld,
                destination.x(),
                destination.y() + 1.0D,
                destination.z(),
                65
        );

        playTeleportSound(
                overworld,
                destination.x(),
                destination.y(),
                destination.z(),
                player.getRandom()
        );

        KrombulEntity endActor =
                createKrombulActor(
                        overworld,
                        player,
                        KrombulEntity.STATE_TP_END
                );

        sequence.actorId =
                endActor == null
                        ? null
                        : endActor.getUUID();

        sequence.phase =
                ExitPhase.KROMBUL_END;

        sequence.ticks = 0;

        player.getPersistentData()
                .remove(STAY_TICKS_TAG);

        ChaosReturnData.clear(player);
    }


    private static void tickKrombulEnd(
            ServerPlayer player,
            ExitSequence sequence
    ) {
        freezePlayer(player);

        sequence.ticks++;

        if (sequence.ticks < KROMBUL_ANIMATION_TIME) {
            return;
        }

        Entity actor =
                getActor(
                        player.serverLevel(),
                        sequence.actorId
                );

        if (actor != null) {
            spawnBurst(
                    player.serverLevel(),
                    actor.getX(),
                    actor.getY()
                            + actor.getBbHeight() * 0.5D,
                    actor.getZ(),
                    40
            );

            actor.discard();
        }

        ACTIVE_SEQUENCES.remove(
                player.getUUID()
        );
    }

    private static KrombulEntity createKrombulActor(
            ServerLevel level,
            ServerPlayer player,
            int animationState
    ) {
        KrombulEntity krombul =
                Oasiso.KROMBUL.get()
                        .create(level);

        if (krombul == null) {
            return null;
        }

        Vec3 forward =
                getHorizontalLook(player);

        double x =
                player.getX()
                        + forward.x * 2.2D;

        double y =
                player.getY() + 0.45D;

        double z =
                player.getZ()
                        + forward.z * 2.2D;

        float yaw =
                (float) (
                        Math.toDegrees(
                                Math.atan2(
                                        player.getZ() - z,
                                        player.getX() - x
                                )
                        ) - 90.0D
                );

        krombul.moveTo(
                x,
                y,
                z,
                yaw,
                0.0F
        );

        krombul.setYHeadRot(yaw);

        krombul.yBodyRot = yaw;
        krombul.yBodyRotO = yaw;

        krombul.setNoAi(true);
        krombul.setInvulnerable(true);
        krombul.setDeltaMovement(Vec3.ZERO);

        krombul.setAnimState(
                animationState
        );

        level.addFreshEntity(krombul);

        return krombul;
    }

    private static Vec3 getHorizontalLook(
            ServerPlayer player
    ) {
        Vec3 look =
                player.getLookAngle();

        Vec3 horizontal =
                new Vec3(
                        look.x,
                        0.0D,
                        look.z
                );

        if (horizontal.lengthSqr() > 0.0001D) {
            return horizontal.normalize();
        }

        double radians =
                Math.toRadians(
                        player.getYRot()
                );

        return new Vec3(
                -Math.sin(radians),
                0.0D,
                Math.cos(radians)
        );
    }

    private static void freezePlayer(
            ServerPlayer player
    ) {
        player.setDeltaMovement(Vec3.ZERO);
        player.fallDistance = 0.0F;
        player.hurtMarked = true;
    }

    private static Entity getActor(
            ServerLevel level,
            UUID actorId
    ) {
        if (actorId == null) {
            return null;
        }

        return level.getEntity(actorId);
    }

    private static void discardActor(
            ServerLevel level,
            UUID actorId
    ) {
        Entity actor =
                getActor(level, actorId);

        if (actor != null) {
            actor.discard();
        }
    }

    private static void spawnBurst(
            ServerLevel level,
            double x,
            double y,
            double z,
            int count
    ) {
        level.sendParticles(
                Oasiso.PURPLE_STARS.get(),
                x,
                y,
                z,
                count,
                0.75D,
                0.95D,
                0.75D,
                0.10D
        );
    }

    private static void playTeleportSound(
            ServerLevel level,
            double x,
            double y,
            double z,
            RandomSource random
    ) {
        SoundEvent[] sounds = {
                ModSounds.ENTROPY1.get(),
                ModSounds.ENTROPY2.get(),
                ModSounds.ENTROPY3.get()
        };

        SoundEvent selectedSound =
                sounds[random.nextInt(
                        sounds.length
                )];

        level.playSound(
                null,
                x,
                y,
                z,
                selectedSound,
                SoundSource.AMBIENT,
                1.25F,
                0.85F
                        + random.nextFloat()
                        * 0.2F
        );
    }

    @SubscribeEvent
    public static void onPlayerLogout(
            PlayerEvent.PlayerLoggedOutEvent event
    ) {
        if (event.getEntity()
                instanceof ServerPlayer player) {
            cleanup(player);
        }
    }

    @SubscribeEvent
    public static void onPlayerDeath(
            LivingDeathEvent event
    ) {
        if (event.getEntity()
                instanceof ServerPlayer player) {
            cleanup(player);
        }
    }

    private static void cleanup(
            ServerPlayer player
    ) {
        ExitSequence sequence =
                ACTIVE_SEQUENCES.remove(
                        player.getUUID()
                );

        if (sequence != null) {
            discardActor(
                    player.serverLevel(),
                    sequence.actorId
            );
        }

        player.getPersistentData()
                .remove(STAY_TICKS_TAG);
    }

    private enum ExitPhase {
        CONSUMING,
        KROMBUL_START,
        KROMBUL_END
    }

    private static final class ExitSequence {

        private ExitPhase phase;
        private int ticks;
        private UUID actorId;

        private ExitSequence(
                ExitPhase phase
        ) {
            this.phase = phase;
        }
    }
}