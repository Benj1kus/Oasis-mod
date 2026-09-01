package com.benji.oasiso.common.chain;

import com.benji.oasiso.ModSounds;
import com.benji.oasiso.Oasiso;
import com.benji.oasiso.common.event.AzumalitArmorHandler;
import com.benji.oasiso.common.item.AzumalitArmorItem;
import com.benji.oasiso.common.waypoint.AzumalitWaypointManager;
import com.benji.oasiso.network.AzumalitChainSyncPacket;
import com.benji.oasiso.network.ModMessages;
import com.benji.oasiso.registry.ModItems;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Mod.EventBusSubscriber(modid = Oasiso.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class AzumalitChainManager {

    public static final double TARGET_RADIUS = 10.0D;

    public static final int CHAIN_SUM_TICK = 17;
    public static final int FIRST_LINK_BUILD_TICKS = 10;
    public static final int MIN_LINK_BUILD_TICKS = 3;
    public static final int LINK_ACCELERATION_PER_STEP = 1;

    @Deprecated
    public static final int LINK_BUILD_TICKS = FIRST_LINK_BUILD_TICKS;

    public static final int AFTER_BUILD_HOLD_TICKS = 6;
    public static final int DAMAGE_STEP_TICKS = 3;
    public static final int AFTER_DAMAGE_HOLD_TICKS = 8;

    public static final int CHAIN_COOLDOWN_TICKS = 30 * 20;

    private static final float FIRST_TARGET_DAMAGE = 100.0F;
    private static final float DAMAGE_FALLOFF = 10.0F;
    private static final float MIN_TARGET_DAMAGE = 10.0F;

    private static final Map<UUID, ChainSession> SESSIONS = new HashMap<>();

    private AzumalitChainManager() {
    }

    public static int getLinkBuildTicks(int linkIndex) {
        int normalizedIndex = Math.max(1, linkIndex);

        return Math.max(MIN_LINK_BUILD_TICKS, FIRST_LINK_BUILD_TICKS - (normalizedIndex - 1) * LINK_ACCELERATION_PER_STEP);
    }

    public static long getTargetActivationOffset(int targetIndex) {
        if (targetIndex <= 0) {
            return 0L;
        }

        long ticks = 0L;

        for (int linkIndex = 1; linkIndex <= targetIndex; linkIndex++) {
            ticks += getLinkBuildTicks(linkIndex);
        }

        return ticks;
    }

    public static long getBuildDurationTicks(int targetCount) {
        return targetCount <= 1 ? 0L : getTargetActivationOffset(targetCount - 1);
    }

    public static boolean requestCast(ServerPlayer player) {
        if (player == null || !player.isAlive() || player.isSpectator() || !AzumalitArmorItem.isWearingFullSet(player)) {
            return false;
        }

        if (SESSIONS.containsKey(player.getUUID()) || player.getCooldowns().isOnCooldown(ModItems.AZUMALIT_CHESTPLATE.get()) || AzumalitArmorItem.isGuardAnimationActive(player) || AzumalitArmorItem.isWaypointAnimationActive(player) || AzumalitWaypointManager.isCasting(player)) {
            return false;
        }

        ServerLevel level = player.serverLevel();
        long gameTime = level.getGameTime();

        AzumalitArmorHandler.cancelActiveArmAttack(player);

        ChainSession session = new ChainSession(player.getUUID(), level.dimension(), gameTime, gameTime + CHAIN_SUM_TICK, gameTime + AzumalitArmorItem.CHAIN_ANIMATION_TICKS);

        SESSIONS.put(player.getUUID(), session);

        AzumalitArmorItem.triggerChestAnimation(player, AzumalitArmorItem.TRIGGER_CHAIN);

        ModMessages.sendToPlayer(player, AzumalitChainSyncPacket.castStarted(player.getId()));

        return true;
    }

    public static boolean isCasting(ServerPlayer player) {
        return player != null && SESSIONS.containsKey(player.getUUID());
    }

    public static void cancel(ServerPlayer player) {
        if (player == null) {
            return;
        }

        ChainSession session = SESSIONS.remove(player.getUUID());

        if (session == null) {
            return;
        }

        releaseTargets(player.getServer() == null ? null : player.getServer().getLevel(session.dimension), session);

        ModMessages.sendToTrackingAndSelf(player, AzumalitChainSyncPacket.stopped(player.getId()));
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.player.level().isClientSide || !(event.player instanceof ServerPlayer player)) {
            return;
        }

        ChainSession session = SESSIONS.get(player.getUUID());

        if (session == null) {
            return;
        }

        tickSession(player, session);
    }

    private static void tickSession(ServerPlayer player, ChainSession session) {
        if (!player.isAlive() || !AzumalitArmorItem.isWearingFullSet(player) || player.serverLevel().dimension() != session.dimension) {
            cancel(player);
            return;
        }

        ServerLevel level = player.serverLevel();
        long gameTime = level.getGameTime();

        if (!session.chainStarted && gameTime >= session.sumAt) {
            beginChain(level, player, session, gameTime);
        }

        if (session.chainStarted) {
            freezeTargets(level, session);

            if (!session.targets.isEmpty()) {
                playActivationChimes(level, session, gameTime);
                damageReadyTargets(level, player, session, gameTime);
            }
        }

        if (!session.cooldownStarted && gameTime >= session.animationEndAt) {
            session.cooldownStarted = true;
            player.getCooldowns().addCooldown(ModItems.AZUMALIT_CHESTPLATE.get(), CHAIN_COOLDOWN_TICKS);
        }

        boolean visualAttackFinished = !session.chainStarted ? gameTime >= session.animationEndAt : gameTime >= session.finishAt;

        if (visualAttackFinished && gameTime >= session.animationEndAt) {
            finish(player, session);
        }
    }

    private static void beginChain(ServerLevel level, ServerPlayer owner, ChainSession session, long gameTime) {
        session.chainStarted = true;
        session.chainStartAt = gameTime;

        List<LivingEntity> targets = collectTargets(level, owner);

        if (targets.isEmpty()) {
            session.finishAt = session.animationEndAt;
            return;
        }

        orderTargets(owner, targets);

        for (LivingEntity target : targets) {
            session.targets.add(target.getUUID());
            session.anchors.put(target.getUUID(), target.position());
        }

        long buildFinishAt = gameTime + getBuildDurationTicks(targets.size());
        session.damageStartAt = buildFinishAt + AFTER_BUILD_HOLD_TICKS;
        session.finishAt = session.damageStartAt + (long) targets.size() * DAMAGE_STEP_TICKS + AFTER_DAMAGE_HOLD_TICKS;

        List<Integer> entityIds = targets.stream().map(LivingEntity::getId).toList();

        ModMessages.sendToTrackingAndSelf(owner, AzumalitChainSyncPacket.chainStarted(owner.getId(), gameTime, entityIds));
    }

    private static List<LivingEntity> collectTargets(ServerLevel level, ServerPlayer owner) {
        AABB area = owner.getBoundingBox().inflate(TARGET_RADIUS);
        double radiusSqr = TARGET_RADIUS * TARGET_RADIUS;

        return new ArrayList<>(level.getEntitiesOfClass(LivingEntity.class, area, entity -> isValidTarget(owner, entity) && owner.distanceToSqr(entity) <= radiusSqr));
    }

    private static boolean isValidTarget(ServerPlayer owner, LivingEntity entity) {
        if (entity == owner || !entity.isAlive() || entity instanceof ArmorStand) {
            return false;
        }

        if (entity instanceof Player player) {
            return !player.isCreative() && !player.isSpectator();
        }

        return true;
    }

    private static void orderTargets(ServerPlayer owner, List<LivingEntity> targets) {
        if (targets.size() <= 1) {
            return;
        }

        int firstIndex = owner.getRandom().nextInt(targets.size());
        LivingEntity first = targets.remove(firstIndex);

        List<LivingEntity> ordered = new ArrayList<>();
        ordered.add(first);

        LivingEntity current = first;

        while (!targets.isEmpty()) {
            int nearestIndex = 0;
            double nearestDistance = Double.MAX_VALUE;

            for (int i = 0; i < targets.size(); i++) {
                LivingEntity candidate = targets.get(i);
                double distance = current.distanceToSqr(candidate);

                if (distance < nearestDistance) {
                    nearestDistance = distance;
                    nearestIndex = i;
                }
            }

            current = targets.remove(nearestIndex);
            ordered.add(current);
        }

        targets.addAll(ordered);
    }

    private static void freezeTargets(ServerLevel level, ChainSession session) {
        for (UUID targetId : session.targets) {
            LivingEntity target = resolveTarget(level, targetId);
            Vec3 anchor = session.anchors.get(targetId);

            if (target == null || anchor == null || !target.isAlive()) {
                continue;
            }

            if (target instanceof Mob mob) {
                mob.getNavigation().stop();
            }

            target.setDeltaMovement(Vec3.ZERO);
            target.fallDistance = 0.0F;
            target.teleportTo(anchor.x, anchor.y, anchor.z);
        }
    }

    private static void playActivationChimes(ServerLevel level, ChainSession session, long gameTime) {
        while (session.nextChimeIndex < session.targets.size()) {
            long activationAt = session.chainStartAt + getTargetActivationOffset(session.nextChimeIndex);

            if (gameTime < activationAt) {
                return;
            }

            LivingEntity target = resolveTarget(level, session.targets.get(session.nextChimeIndex));

            if (target != null) {
                float pitch = Math.min(2.0F, 0.82F + session.nextChimeIndex * 0.10F);

                level.playSound(null, target.getX(), target.getY() + target.getBbHeight() * 0.55D, target.getZ(), ModSounds.BOMB_SPAWN.get(), SoundSource.PLAYERS, 1.25F, pitch);
            }

            session.nextChimeIndex++;
        }
    }

    private static void damageReadyTargets(ServerLevel level, ServerPlayer owner, ChainSession session, long gameTime) {
        while (session.nextDamageIndex < session.targets.size()) {
            long damageAt = session.damageStartAt + (long) session.nextDamageIndex * DAMAGE_STEP_TICKS;

            if (gameTime < damageAt) {
                return;
            }

            int targetIndex = session.nextDamageIndex;
            LivingEntity target = resolveTarget(level, session.targets.get(targetIndex));

            if (target != null && target.isAlive()) {
                float damage = Math.max(MIN_TARGET_DAMAGE, FIRST_TARGET_DAMAGE - targetIndex * DAMAGE_FALLOFF);

                target.hurt(level.damageSources().playerAttack(owner), damage);
            }

            session.nextDamageIndex++;
        }
    }

    private static void finish(ServerPlayer player, ChainSession session) {
        SESSIONS.remove(player.getUUID());
        releaseTargets(player.serverLevel(), session);

        ModMessages.sendToTrackingAndSelf(player, AzumalitChainSyncPacket.stopped(player.getId()));
    }

    private static void releaseTargets(ServerLevel level, ChainSession session) {
        if (level == null) {
            return;
        }

        for (UUID targetId : session.targets) {
            LivingEntity target = resolveTarget(level, targetId);

            if (target != null) {
                target.setDeltaMovement(Vec3.ZERO);
            }
        }
    }

    private static LivingEntity resolveTarget(ServerLevel level, UUID targetId) {
        if (level == null || targetId == null) {
            return null;
        }

        if (level.getEntity(targetId) instanceof LivingEntity living) {
            return living;
        }

        return null;
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            cancel(player);
        }
    }

    @SubscribeEvent
    public static void onDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            cancel(player);
        }
    }

    private static final class ChainSession {
        private final UUID ownerId;
        private final ResourceKey<Level> dimension;
        private final long castStartedAt;
        private final long sumAt;
        private final long animationEndAt;

        private final List<UUID> targets = new ArrayList<>();
        private final Map<UUID, Vec3> anchors = new HashMap<>();

        private boolean chainStarted;
        private boolean cooldownStarted;

        private long chainStartAt = Long.MAX_VALUE;
        private long damageStartAt = Long.MAX_VALUE;
        private long finishAt = Long.MAX_VALUE;

        private int nextChimeIndex;
        private int nextDamageIndex;

        private ChainSession(UUID ownerId, ResourceKey<Level> dimension, long castStartedAt, long sumAt, long animationEndAt) {
            this.ownerId = ownerId;
            this.dimension = dimension;
            this.castStartedAt = castStartedAt;
            this.sumAt = sumAt;
            this.animationEndAt = animationEndAt;
        }
    }
}
