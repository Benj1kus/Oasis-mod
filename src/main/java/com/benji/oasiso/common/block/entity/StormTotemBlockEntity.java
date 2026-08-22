package com.benji.oasiso.common.block.entity;

import com.benji.oasiso.Oasiso;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class StormTotemBlockEntity extends BlockEntity {

    public static final double STORM_RADIUS = 15.0D;
    private static final int BUILDUP_TICKS = 20 * 4;
    private static final int FADE_TICKS = 20 * 3;
    private static final double INNER_PUSH = 0.022D;
    private static final double OUTER_PUSH = 0.055D;

    private static final String INTENSITY_TAG = "StormIntensity";

    private float stormIntensity;

    private float lastSyncedIntensity = -1.0F;

    private static final Map<ClientSourceKey, ClientSource> CLIENT_SOURCES = new HashMap<>();

    public StormTotemBlockEntity(BlockPos pos, BlockState state) {
        super(Oasiso.STORM_TOTEM_BLOCK_ENTITY.get(),
                pos, state);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, StormTotemBlockEntity blockEntity) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }

        boolean playerNearby = blockEntity.hasPlayerNearby(serverLevel);

        float targetIntensity = playerNearby ? 1.0F : 0.0F;
        float speed = targetIntensity > blockEntity.stormIntensity ? 1.0F / BUILDUP_TICKS : 1.0F / FADE_TICKS;
        float previousIntensity = blockEntity.stormIntensity;

        blockEntity.stormIntensity = Mth.approach(blockEntity.stormIntensity, targetIntensity, speed);
        blockEntity.stormIntensity = Mth.clamp(blockEntity.stormIntensity, 0.0F, 1.0F);

        if (blockEntity.stormIntensity > 0.01F) {
            blockEntity.pushPlayers(serverLevel);
        }

        if (Math.abs(previousIntensity - blockEntity.stormIntensity) > 0.0001F) {

            blockEntity.setChanged();

            if (serverLevel.getGameTime() % 4L == 0L || blockEntity.stormIntensity == 0.0F || blockEntity.stormIntensity == 1.0F) {
                blockEntity.sync();
            }
        }
    }

    public static void clientTick(Level level, BlockPos pos, BlockState state, StormTotemBlockEntity blockEntity) {
        touchClientSource(level.dimension(), pos, blockEntity.stormIntensity, level.getGameTime());
    }


    private boolean hasPlayerNearby(ServerLevel level) {
        double centerX = this.worldPosition.getX() + 0.5D;
        double centerY = this.worldPosition.getY() + 0.8D;
        double centerZ = this.worldPosition.getZ() + 0.5D;

        double rangeSqr = STORM_RADIUS * STORM_RADIUS;

        for (ServerPlayer player : level.players()) {

            if (!player.isAlive() || player.isSpectator()) {
                continue;
            }

            if (player.distanceToSqr(centerX, centerY, centerZ) <= rangeSqr) {
                return true;
            }
        }
        return false;
    }

    private void pushPlayers(ServerLevel level) {
        double centerX = this.worldPosition.getX() + 0.5D;
        double centerZ = this.worldPosition.getZ() + 0.5D;

        double radiusSqr = STORM_RADIUS * STORM_RADIUS;

        for (ServerPlayer player : level.players()) {

            if (!player.isAlive() || player.isCreative() || player.isSpectator()) {
                continue;
            }

            double deltaX = player.getX() - centerX;
            double deltaZ = player.getZ() - centerZ;

            double distanceSqr = deltaX * deltaX + deltaZ * deltaZ;

            if (distanceSqr > radiusSqr || distanceSqr < 0.0025D) {
                continue;
            }

            double distance = Math.sqrt(distanceSqr);
            double normalized = distance / STORM_RADIUS;
            double outerStrength = Mth.clamp((normalized - 0.72D) / 0.28D, 0.0D, 1.0D);
            double push = Mth.lerp(outerStrength, INNER_PUSH, OUTER_PUSH) * this.stormIntensity;

            double directionX = deltaX / distance;
            double directionZ = deltaZ / distance;

            player.push(directionX * push, 0.002D * this.stormIntensity, directionZ * push);
            player.hurtMarked = true;
        }
    }

    private void sync() {
        if (!(this.level instanceof ServerLevel serverLevel)) {
            return;
        }

        BlockState state = this.getBlockState();
        serverLevel.sendBlockUpdated(this.worldPosition, state, state, Block.UPDATE_CLIENTS);

        this.lastSyncedIntensity = this.stormIntensity;
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putFloat(INTENSITY_TAG, this.stormIntensity);
    }


    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        this.stormIntensity = Mth.clamp(tag.getFloat(INTENSITY_TAG), 0.0F, 1.0F);
    }

    @Override
    public CompoundTag getUpdateTag() {
        CompoundTag tag = super.getUpdateTag();
        tag.putFloat(INTENSITY_TAG, this.stormIntensity);
        return tag;
    }


    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    public float getStormIntensity() {
        return this.stormIntensity;
    }

    private static void touchClientSource(ResourceKey<Level> dimension, BlockPos pos, float targetIntensity, long gameTime) {
        ClientSourceKey key = new ClientSourceKey(dimension, pos.immutable());

        ClientSource source = CLIENT_SOURCES.computeIfAbsent(key, ignored -> new ClientSource());

        source.targetIntensity = Mth.clamp(targetIntensity, 0.0F, 1.0F);
        source.lastSeenGameTime = gameTime;
    }

    public static void tickClientSources(ResourceKey<Level> dimension, long gameTime) {
        Iterator<Map.Entry<ClientSourceKey, ClientSource>> iterator = CLIENT_SOURCES.entrySet().iterator();

        while (iterator.hasNext()) {

            Map.Entry<ClientSourceKey, ClientSource> entry = iterator.next();

            ClientSourceKey key = entry.getKey();
            ClientSource source = entry.getValue();

            if (!key.dimension.equals(dimension)) {
                continue;
            }

            if (gameTime - source.lastSeenGameTime > 2L) {
                source.targetIntensity = 0.0F;
            }

            source.visualIntensity = Mth.approach(source.visualIntensity, source.targetIntensity, 1.0F / 60.0F);

            if (source.visualIntensity <= 0.001F && source.targetIntensity <= 0.001F && gameTime - source.lastSeenGameTime > 4L) {

                iterator.remove();
            }
        }
    }


    public static List<ClientStormSource> getClientSources(ResourceKey<Level> dimension) {
        List<ClientStormSource> result = new ArrayList<>();

        for (Map.Entry<ClientSourceKey, ClientSource> entry : CLIENT_SOURCES.entrySet()) {

            if (!entry.getKey().dimension.equals(dimension)) {
                continue;
            }

            float intensity = entry.getValue().visualIntensity;

            if (intensity <= 0.001F) {
                continue;
            }

            result.add(new ClientStormSource(entry.getKey().pos,
                    intensity));
        }
        return result;
    }

    public static float getClientStormStrength(ResourceKey<Level> dimension, Vec3 cameraPosition) {
        float strongest = 0.0F;

        for (ClientStormSource source : getClientSources(dimension)) {
            BlockPos pos = source.pos();

            Vec3 center = new Vec3(pos.getX() + 0.5D, pos.getY() + 0.8D, pos.getZ() + 0.5D);

            double distance = cameraPosition.distanceTo(center);

            if (distance >= STORM_RADIUS) {

                continue;
            }

            float edge = Mth.clamp((float) ((STORM_RADIUS - distance) / 2.0D), 0.0F, 1.0F);
            float strength = source.intensity() * edge;

            strongest = Math.max(strongest, strength);
        }


        return strongest;
    }


    public static void clearClientSources() {
        CLIENT_SOURCES.clear();
    }
    private record ClientSourceKey(ResourceKey<Level> dimension, BlockPos pos) {
    }


    private static final class ClientSource {
        private float targetIntensity;
        private float visualIntensity;
        private long lastSeenGameTime;
    }


    public record ClientStormSource(BlockPos pos, float intensity) {
    }
}