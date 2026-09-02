package com.benji.oasiso.common.dimension;

import com.benji.oasiso.ModSounds;
import com.benji.oasiso.Oasiso;
import com.benji.oasiso.common.entity.ChaosPortalEntity;
import com.benji.oasiso.network.BossPortalTransitionNetwork;
import com.benji.oasiso.network.BossPortalTransitionS2CPacket;
import com.benji.oasiso.network.ModMessages;
import com.benji.oasiso.network.PocketModeS2CPacket;
import com.benji.oasiso.registry.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Mod.EventBusSubscriber(modid = Oasiso.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class ChaosPocketTransitionServer {

    private static final int TELEPORT_DELAY = 30;
    private static final int POST_TELEPORT_HOLD = 20;
    private static final Map<UUID, Transition> ACTIVE = new HashMap<>();

    private ChaosPocketTransitionServer() {
    }

    public static boolean begin(ServerPlayer player, ChaosPortalEntity portal) {
        if (!portal.canAcceptPlayer(player)) {
            return false;
        }

        if (ACTIVE.containsKey(player.getUUID())) {
            return true;
        }

        if (portal.isEntrancePortal()) {

            if (player.level().dimension().equals(Oasiso.CHAOS_DIMENSION)) {
                return false;
            }

        } else {

            if (!player.level().dimension().equals(Oasiso.CHAOS_DIMENSION)) {
                return false;
            }

            if (!PocketTravelData.isActive(player)) {
                return false;
            }
        }

        portal.setKeepAlive(true);

        ACTIVE.put(player.getUUID(), new Transition(portal.getUUID(), portal.getPortalRole()));

        BossPortalTransitionNetwork.send(player, BossPortalTransitionS2CPacket.Action.CLOSE);

        return true;
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.player.level().isClientSide || !(event.player instanceof ServerPlayer player)) {
            return;
        }

        Transition transition = ACTIVE.get(player.getUUID());

        if (transition == null) {
            return;
        }

        if (!player.isAlive()) {
            cancel(player);
            return;
        }

        freezePlayer(player);
        transition.ticks++;

        if (!transition.teleported && transition.ticks >= TELEPORT_DELAY) {
            boolean success = performTeleport(player, transition);

            if (!success) {
                cancel(player);
                return;
            }

            transition.teleported = true;
            transition.ticks = 0;
            return;
        }

        if (transition.teleported && transition.ticks >= POST_TELEPORT_HOLD) {
            BossPortalTransitionNetwork.send(player, BossPortalTransitionS2CPacket.Action.OPEN);
            ACTIVE.remove(player.getUUID());
        }
    }

    private static boolean performTeleport(ServerPlayer player, Transition transition) {
        Entity entity = player.serverLevel().getEntity(transition.portalId);

        if (!(entity instanceof ChaosPortalEntity portal)) {
            return false;
        }

        return switch (transition.role) {
            case ENTRANCE -> enterPocket(player, portal);
            case RETURN -> returnFromPocket(player, portal);
        };
    }

    private static boolean enterPocket(ServerPlayer player, ChaosPortalEntity entrance) {
        MinecraftServer server = player.getServer();

        if (server == null) {
            return false;
        }

        ServerLevel chaos = server.getLevel(Oasiso.CHAOS_DIMENSION);

        if (chaos == null) {
            return false;
        }

        PocketDimensionManager.PlatformTarget platform = PocketDimensionManager.preparePlatform(server, player.getUUID());

        if (platform == null) {
            return false;
        }

        Vec3 spawn = PocketDimensionManager.getPlayerSpawn(platform);

        PocketDimensionManager.keepDestinationReady(chaos, platform.marker(), player);

        ChaosPortalEntity.discardOwnedReturnPortals(server, player.getUUID());

        PocketTravelData.saveSource(player, entrance);
        entrance.setKeepAlive(true);

        PocketDimensionManager.ReturnPortalPlacement placement = PocketDimensionManager.getReturnPortalPlacement(platform);

        ChaosPortalEntity returnPortal = ModEntities.CHAOS_PORTAL_ENTITY.get().create(chaos);

        if (returnPortal == null) {
            return false;
        }

        Vec3 portalPos = placement.position();

        returnPortal.moveTo(portalPos.x, portalPos.y, portalPos.z, placement.yaw(), 0.0F);

        float seed = RandomSource.create().nextFloat() * 1000.0F;

        returnPortal.initializeReturnPortal(player, placement.yaw(), seed, entrance.getUUID());

        returnPortal.setLinkedPortalId(entrance.getUUID());

        entrance.setLinkedPortalId(returnPortal.getUUID());

        if (!chaos.addFreshEntity(returnPortal)) {
            return false;
        }

        PocketTravelData.setReturnPortal(player, returnPortal.getUUID());

        player.stopRiding();

        player.teleportTo(chaos, spawn.x, spawn.y, spawn.z, player.getYRot(), player.getXRot());

        ModMessages.sendToPlayer(player, new PocketModeS2CPacket(true));
        player.setDeltaMovement(Vec3.ZERO);
        player.fallDistance = 0.0F;
        player.hurtMarked = true;

        chaos.playSound(null, returnPortal.getX(), returnPortal.getY() + 1.0D, returnPortal.getZ(), ModSounds.PORTAL_OPEN.get(), SoundSource.PLAYERS, 1.25F, 1.0F);


        return true;
    }

    private static boolean returnFromPocket(ServerPlayer player, ChaosPortalEntity returnPortal) {
        PocketTravelData.ReturnTarget target = PocketTravelData.loadReturnTarget(player);

        if (target == null) {
            return false;
        }

        ServerLevel destination = target.level();

        BlockPos returnPos = BlockPos.containing(target.x(), target.y(), target.z());

        destination.getChunk(returnPos.getX() >> 4, returnPos.getZ() >> 4);

        UUID sourcePortalId = target.sourcePortal();

        returnPortal.discard();

        player.stopRiding();
        player.teleportTo(destination, target.x(), target.y(), target.z(), target.yaw(), target.pitch());
        player.setDeltaMovement(Vec3.ZERO);

        player.fallDistance = 0.0F;
        player.hurtMarked = true;

        if (sourcePortalId != null) {

            Entity source = destination.getEntity(sourcePortalId);
            if (source instanceof ChaosPortalEntity entrance) {
                entrance.setLinkedPortalId(null);
                entrance.releaseAfterUse();
            }
        }

        PocketTravelData.clear(player);
        return true;
    }

    private static void freezePlayer(ServerPlayer player) {
        player.setDeltaMovement(Vec3.ZERO);
        player.fallDistance = 0.0F;
        player.hurtMarked = true;
    }


    private static void cancel(ServerPlayer player) {
        ACTIVE.remove(player.getUUID());
        BossPortalTransitionNetwork.send(player, BossPortalTransitionS2CPacket.Action.CANCEL);
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        ACTIVE.remove(event.getEntity().getUUID());
    }


    @SubscribeEvent
    public static void onDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {

            ACTIVE.remove(player.getUUID());
        }
    }

    private static final class Transition {
        private final UUID portalId;
        private final ChaosPortalEntity.PortalRole role;
        private int ticks;
        private boolean teleported;

        private Transition(UUID portalId, ChaosPortalEntity.PortalRole role) {
            this.portalId = portalId;
            this.role = role;
        }
    }
}