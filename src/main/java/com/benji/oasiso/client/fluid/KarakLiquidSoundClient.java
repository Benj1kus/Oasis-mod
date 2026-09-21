package com.benji.oasiso.client.fluid;

import com.benji.oasiso.ModSounds;
import com.benji.oasiso.Oasiso;
import com.benji.oasiso.registry.ModKarakFluids;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Mod.EventBusSubscriber(modid = Oasiso.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class KarakLiquidSoundClient {
    private static final int MAX_SURFACE_SOUNDS = 3;
    private static final int SEARCH_RADIUS = 12;
    private static final int VERTICAL_RADIUS = 6;
    private static final double SOURCE_SPACING = 4.0;
    private static final int SCAN_INTERVAL = 20; // Раз в секунду, а не каждый кадр.

    private static final float SURFACE_VOLUME = 0.45F;
    private static final float UNDERWATER_VOLUME = 0.65F;

    private static final List<JadeLoop> surfaceLoops = new ArrayList<>();
    private static JadeLoop underwaterLoop;
    private static ClientLevel world;
    private static long clock;
    private static int scanCountdown;

    private KarakLiquidSoundClient() {
    }

    private static boolean isJade(FluidState fluid) {
        return fluid.getType().isSame(ModKarakFluids.KR_WATER.get());
    }

    private static Vec3 listener(Minecraft mc) {
        return mc.gameRenderer.getMainCamera().getEntity() != null ? mc.gameRenderer.getMainCamera().getPosition() : mc.player.getEyePosition();
    }

    private static boolean isSubmerged(Vec3 camera) {
        BlockPos pos = BlockPos.containing(camera.x, camera.y, camera.z);
        if (!world.hasChunkAt(pos)) return false;
        FluidState fluid = world.getFluidState(pos);
        return isJade(fluid) && camera.y < pos.getY() + fluid.getHeight(world, pos);
    }

    private static boolean audible(Minecraft mc, SoundSource category) {
        return mc.options.getSoundSourceVolume(SoundSource.MASTER) > 0.0F && mc.options.getSoundSourceVolume(category) > 0.0F;
    }

    private static boolean isSurface(BlockPos pos) {
        if (!world.hasChunkAt(pos) || !isJade(world.getFluidState(pos))) return false;
        BlockPos above = pos.above();
        return world.hasChunkAt(above) && !isJade(world.getFluidState(above)) && world.getBlockState(above).getCollisionShape(world, above).isEmpty();
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getInstance();
        if (world != mc.level) {
            stopAll(mc);
            world = mc.level;
            scanCountdown = 0;
        }
        if (world == null || mc.player == null) {
            stopAll(mc);
            return;
        }
        if (mc.isPaused()) return;
        clock++;
        Vec3 camera = listener(mc);

        if (isSubmerged(camera)) {
            stopSurface(mc);
            scanCountdown = 0;
            if (!audible(mc, SoundSource.AMBIENT)) {
                stopUnderwater(mc);
                return;
            }
            if (underwaterLoop != null && underwaterLoop.needsRestart(mc)) stopUnderwater(mc);
            if (underwaterLoop == null) {
                underwaterLoop = new JadeLoop(null);
                mc.getSoundManager().play(underwaterLoop);
            }
            return;
        }

        stopUnderwater(mc);
        if (!audible(mc, SoundSource.BLOCKS)) {
            stopSurface(mc);
            scanCountdown = 0;
            return;
        }
        surfaceLoops.removeIf(loop -> {
            boolean remove = loop.needsRestart(mc) || !isSurface(loop.anchor) || Vec3.atCenterOf(loop.anchor).distanceToSqr(camera) > SEARCH_RADIUS * SEARCH_RADIUS;
            if (remove) {
                loop.halt(mc);
                scanCountdown = 0;
            }
            return remove;
        });
        if (scanCountdown-- <= 0) {
            refreshSources(mc, camera);
            scanCountdown = SCAN_INTERVAL - 1;
        }
    }

    private static void refreshSources(Minecraft mc, Vec3 camera) {
        BlockPos origin = BlockPos.containing(camera.x, camera.y, camera.z);
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        List<BlockPos> candidates = new ArrayList<>();
        for (int dx = -SEARCH_RADIUS; dx <= SEARCH_RADIUS; dx++) {
            for (int dz = -SEARCH_RADIUS; dz <= SEARCH_RADIUS; dz++) {
                if (dx * dx + dz * dz > SEARCH_RADIUS * SEARCH_RADIUS) continue;
                cursor.set(origin.getX() + dx, origin.getY(), origin.getZ() + dz);
                if (!world.hasChunkAt(cursor)) continue; // Не загружаем новые чанки ради звука.
                for (int dy = -VERTICAL_RADIUS; dy <= VERTICAL_RADIUS; dy++) {
                    int y = origin.getY() + dy;
                    if (y < world.getMinBuildHeight() || y >= world.getMaxBuildHeight()) continue;
                    cursor.set(origin.getX() + dx, y, origin.getZ() + dz);
                    if (Vec3.atCenterOf(cursor).distanceToSqr(camera) <= SEARCH_RADIUS * SEARCH_RADIUS && isSurface(cursor))
                        candidates.add(cursor.immutable());
                }
            }
        }

        candidates.sort(Comparator.comparingDouble(pos -> Vec3.atCenterOf(pos).distanceToSqr(camera) - (hasSource(pos) ? 16.0 : 0.0)));
        List<BlockPos> selected = new ArrayList<>(MAX_SURFACE_SOUNDS);
        for (BlockPos pos : candidates) {
            boolean separated = true;
            for (BlockPos other : selected) {
                if (pos.distSqr(other) < SOURCE_SPACING * SOURCE_SPACING) {
                    separated = false;
                    break;
                }
            }
            if (!separated) continue;
            selected.add(pos);
            if (selected.size() == MAX_SURFACE_SOUNDS) break;
        }

        surfaceLoops.removeIf(loop -> {
            if (selected.contains(loop.anchor)) return false;
            loop.halt(mc);
            return true;
        });
        for (BlockPos pos : selected) {
            if (hasSource(pos)) continue;
            JadeLoop loop = new JadeLoop(pos);
            surfaceLoops.add(loop);
            mc.getSoundManager().play(loop);
        }
    }

    private static boolean hasSource(BlockPos pos) {
        for (JadeLoop loop : surfaceLoops) if (pos.equals(loop.anchor)) return true;
        return false;
    }

    private static void stopSurface(Minecraft mc) {
        for (JadeLoop loop : surfaceLoops) loop.halt(mc);
        surfaceLoops.clear();
    }

    private static void stopUnderwater(Minecraft mc) {
        if (underwaterLoop != null) {
            underwaterLoop.halt(mc);
            underwaterLoop = null;
        }
    }

    private static void stopAll(Minecraft mc) {
        stopSurface(mc);
        stopUnderwater(mc);
    }

    private static final class JadeLoop extends AbstractTickableSoundInstance {
        private final BlockPos anchor;
        private final ClientLevel owner;
        private final long startedAt;

        private JadeLoop(BlockPos anchor) {
            super(anchor == null ? ModSounds.KR_WATER_LOOP_UNDER.get() : ModSounds.KR_WATER_LOOP.get(), anchor == null ? SoundSource.AMBIENT : SoundSource.BLOCKS, RandomSource.create());
            this.anchor = anchor;
            this.owner = world;
            this.startedAt = clock;
            this.looping = true;
            this.delay = 0;
            this.relative = anchor == null;
            this.attenuation = anchor == null ? SoundInstance.Attenuation.NONE : SoundInstance.Attenuation.LINEAR;
            this.volume = 0.0F;
            this.pitch = 1.0F;
            if (anchor != null) {
                this.x = anchor.getX() + 0.5;
                this.y = anchor.getY() + owner.getFluidState(anchor).getHeight(owner, anchor);
                this.z = anchor.getZ() + 0.5;
            } else {
                this.x = this.y = this.z = 0.0;
            }
        }

        @Override
        public boolean canStartSilent() {
            return true;
        }

        @Override
        public void tick() {
            Minecraft mc = Minecraft.getInstance();
            if (mc.level != owner || mc.player == null) {
                stop();
                return;
            }
            float target = UNDERWATER_VOLUME;
            if (anchor != null) {
                if (!owner.hasChunkAt(anchor) || !isJade(owner.getFluidState(anchor))) {
                    stop();
                    return;
                }
                double distance = Vec3.atCenterOf(anchor).distanceTo(listener(mc));
                target = SURFACE_VOLUME * Mth.clamp((float) (SEARCH_RADIUS - distance) / 3.0F, 0.0F, 1.0F);
            }
            this.volume = Mth.lerp(0.25F, this.volume, target);
        }

        private boolean needsRestart(Minecraft mc) {
            return isStopped() || (clock - startedAt > 40 && !mc.getSoundManager().isActive(this));
        }

        private void halt(Minecraft mc) {
            stop();
            mc.getSoundManager().stop(this);
        }
    }
}
