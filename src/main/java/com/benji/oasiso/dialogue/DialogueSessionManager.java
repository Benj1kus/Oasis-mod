package com.benji.oasiso.dialogue;

import com.benji.oasiso.dialogue.data.DialogueDefinition;
import com.benji.oasiso.network.dialogueengine.DialogueNetwork;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.*;

public final class DialogueSessionManager {

    private static final Map<UUID, Session> ACTIVE = new HashMap<>();

    private static final Map<SourceKey, LockState> LOCKS = new HashMap<>();

    private DialogueSessionManager() {
    }

    public static boolean start(ServerPlayer player, Entity source, ResourceLocation dialogueId, Runnable onFinish) {
        return start(player, source, dialogueId, null, onFinish);
    }

    public static boolean start(ServerPlayer player, Entity source, ResourceLocation dialogueId, String onceOverride, Runnable onFinish) {
        DialogueDefinition definition = DialogueRegistry.get(dialogueId);

        if (definition == null || ACTIVE.containsKey(player.getUUID())) {
            return false;
        }

        String once = onceOverride != null ? onceOverride : definition.once;

        if (DialogueOnceTracker.hasSeen(player, source, dialogueId, once)) {
            return false;
        }

        if (definition.exclusive_source && source != null && isSourceBusy(source)) {
            return false;
        }

        UUID sessionId = UUID.randomUUID();

        Session session = new Session(sessionId, player.getUUID(), dialogueId, definition, source, onFinish);

        ACTIVE.put(player.getUUID(), session);

        if (source != null && (definition.freeze_source || definition.source_invulnerable)) {
            lockSource(source, player, dialogueId, definition.freeze_source, definition.source_invulnerable);
        }

        DialogueOnceTracker.markSeen(player, source, dialogueId, once);

        DialogueNetwork.start(player, sessionId, dialogueId, DialogueRegistry.toJson(definition));

        return true;
    }

    public static boolean isActive(ServerPlayer player) {
        return ACTIVE.containsKey(player.getUUID());
    }

    public static void finish(ServerPlayer player, UUID sessionId) {
        Session session = ACTIVE.get(player.getUUID());

        if (session == null || !session.sessionId.equals(sessionId)) {
            return;
        }

        remove(player, session, true, false);
    }

    public static void cancel(ServerPlayer player) {
        Session session = ACTIVE.get(player.getUUID());

        if (session == null) {
            return;
        }

        remove(player, session, false, true);
    }

    public static void tick(MinecraftServer server) {
        List<UUID> cancel = new ArrayList<>();

        for (Session session : ACTIVE.values()) {

            ServerPlayer player = server.getPlayerList().getPlayer(session.playerId);

            if (player == null) {
                cancel.add(session.playerId);
                continue;
            }

            Entity source = session.resolveSource(server);

            if (session.sourceId != null && source == null && session.definition.cancel_if_source_missing) {

                cancel.add(session.playerId);
            }
        }

        for (UUID playerId : cancel) {
            ServerPlayer player = server.getPlayerList().getPlayer(playerId);

            if (player != null) {
                cancel(player);
            } else {
                Session session = ACTIVE.remove(playerId);

                if (session != null) {
                    unlockSource(session, server);
                }
            }
        }

        /*
         * Custom mobs иногда двигаются даже с NoAI.
         */
        for (LockState state : LOCKS.values()) {
            Entity source = state.source;

            if (source == null || source.isRemoved()) {
                continue;
            }

            if (state.freeze) {
                source.setDeltaMovement(Vec3.ZERO);

                source.fallDistance = 0.0F;

                if (source instanceof Mob mob) {
                    mob.getNavigation().stop();
                    mob.setTarget(null);
                }
            }
        }
    }

    private static void remove(ServerPlayer player, Session session, boolean runFinish, boolean sendStop) {
        ACTIVE.remove(player.getUUID());

        unlockSource(session, player.getServer());

        if (sendStop) {
            DialogueNetwork.stop(player, session.sessionId);
        }

        if (runFinish && session.onFinish != null) {
            session.onFinish.run();
        }
    }

    private static boolean isSourceBusy(Entity source) {
        SourceKey key = new SourceKey(source.level().dimension(), source.getUUID());

        LockState state = LOCKS.get(key);

        return state != null && state.count > 0;
    }

    private static void lockSource(Entity source, ServerPlayer viewer, ResourceLocation dialogueId, boolean freeze, boolean invulnerable) {
        SourceKey key = new SourceKey(source.level().dimension(), source.getUUID());

        LockState state = LOCKS.get(key);

        if (state == null) {
            state = new LockState(source);

            LOCKS.put(key, state);
        }

        state.count++;

        state.freeze |= freeze;
        state.invulnerable |= invulnerable;

        if (freeze) {
            source.setDeltaMovement(Vec3.ZERO);

            if (source instanceof Mob mob) {
                mob.setNoAi(true);
                mob.getNavigation().stop();
                mob.setTarget(null);
            }
        }

        if (invulnerable) {
            source.setInvulnerable(true);
        }

        if (source instanceof DialogueLockable lockable) {
            lockable.setDialogueLocked(true, viewer, dialogueId);
        }
    }

    private static void unlockSource(Session session, MinecraftServer server) {
        if (session.sourceId == null || session.sourceDimension == null) {
            return;
        }

        SourceKey key = new SourceKey(session.sourceDimension, session.sourceId);

        LockState state = LOCKS.get(key);

        if (state == null) {
            return;
        }

        state.count--;

        if (state.count > 0) {
            return;
        }

        LOCKS.remove(key);

        Entity source = state.source;

        if (source == null || source.isRemoved()) {
            return;
        }

        if (source instanceof Mob mob && state.oldNoAi != null) {
            mob.setNoAi(state.oldNoAi);
        }

        source.setInvulnerable(state.oldInvulnerable);

        if (source instanceof DialogueLockable lockable) {

            ServerPlayer viewer = server != null ? server.getPlayerList().getPlayer(session.playerId) : null;

            lockable.setDialogueLocked(false, viewer, session.dialogueId);
        }
    }


    private static final class Session {

        private final UUID sessionId;
        private final UUID playerId;

        private final ResourceLocation dialogueId;

        private final DialogueDefinition definition;

        private final UUID sourceId;
        private final ResourceKey<Level> sourceDimension;

        private final Runnable onFinish;

        private Session(UUID sessionId, UUID playerId, ResourceLocation dialogueId, DialogueDefinition definition, Entity source, Runnable onFinish) {
            this.sessionId = sessionId;
            this.playerId = playerId;
            this.dialogueId = dialogueId;
            this.definition = definition;
            this.onFinish = onFinish;

            if (source != null) {
                this.sourceId = source.getUUID();

                this.sourceDimension = source.level().dimension();
            } else {
                this.sourceId = null;
                this.sourceDimension = null;
            }
        }

        private Entity resolveSource(MinecraftServer server) {
            if (sourceId == null || sourceDimension == null) {
                return null;
            }

            ServerLevel level = server.getLevel(sourceDimension);

            if (level == null) {
                return null;
            }

            return level.getEntity(sourceId);
        }
    }


    private static final class LockState {

        private final Entity source;

        private final Boolean oldNoAi;

        private final boolean oldInvulnerable;

        private int count;

        private boolean freeze;
        private boolean invulnerable;

        private LockState(Entity source) {
            this.source = source;
            this.oldInvulnerable = source.isInvulnerable();
            this.oldNoAi = source instanceof Mob mob ? mob.isNoAi() : null;
        }
    }


    private record SourceKey(ResourceKey<Level> dimension, UUID entityId) {
    }
}