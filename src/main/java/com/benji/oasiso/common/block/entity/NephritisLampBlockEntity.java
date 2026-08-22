package com.benji.oasiso.common.block.entity;

import com.benji.oasiso.Oasiso;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;


public class NephritisLampBlockEntity extends BlockEntity {

    private static final int APPEAR_TICKS = 25;
    private static final int FADE_TICKS = 25;

    private static final Map<ClientSourceKey, ClientSource> CLIENT_SOURCES = new HashMap<>();

    public NephritisLampBlockEntity(BlockPos pos, BlockState state) {
        super(Oasiso.NEPHRITIS_LAMP_BE.get(),
                pos, state);
    }

    public static void clientTick(Level level, BlockPos pos, BlockState state, NephritisLampBlockEntity blockEntity) {
        touchClientSource(level.dimension(), pos, level.getGameTime());
    }

    private static void touchClientSource(ResourceKey<Level> dimension, BlockPos pos, long gameTime) {
        ClientSourceKey key = new ClientSourceKey(dimension, pos.immutable());

        ClientSource source = CLIENT_SOURCES.computeIfAbsent(key, ignored -> new ClientSource());

        source.targetIntensity = 1.0F;
        source.lastSeenGameTime = gameTime;
    }


    public static void tickClientSources(ResourceKey<Level> currentDimension, long gameTime) {
        Iterator<Map.Entry<ClientSourceKey, ClientSource>> iterator = CLIENT_SOURCES.entrySet().iterator();

        while (iterator.hasNext()) {

            Map.Entry<ClientSourceKey, ClientSource> entry = iterator.next();
            ClientSourceKey key = entry.getKey();
            ClientSource source = entry.getValue();

            if (!key.dimension().equals(currentDimension)) {
                iterator.remove();
                continue;
            }

            if (gameTime - source.lastSeenGameTime > 2L) {
                source.targetIntensity = 0.0F;
            }


            float speed = source.targetIntensity > source.visualIntensity ? 1.0F / APPEAR_TICKS : 1.0F / FADE_TICKS;

            source.visualIntensity = Mth.approach(source.visualIntensity, source.targetIntensity, speed);

            if (source.visualIntensity <= 0.001F && source.targetIntensity <= 0.001F && gameTime - source.lastSeenGameTime > 4L) {
                iterator.remove();
            }
        }
    }

    public static List<ClientLampSource> getClientSources(ResourceKey<Level> dimension) {
        List<ClientLampSource> result = new ArrayList<>();

        for (Map.Entry<ClientSourceKey, ClientSource> entry : CLIENT_SOURCES.entrySet()) {

            if (!entry.getKey().dimension().equals(dimension)) {
                continue;
            }


            float intensity = entry.getValue().visualIntensity;

            if (intensity <= 0.001F) {
                continue;
            }

            result.add(new ClientLampSource(entry.getKey().pos(),
                    intensity));
        }


        return result;
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


    public record ClientLampSource(BlockPos pos, float intensity) {
    }
}