package com.benji.oasiso.client.fluid;

import com.benji.oasiso.Oasiso;
import com.benji.oasiso.registry.ModKarakFluids;
import net.minecraft.client.Minecraft;
import net.minecraft.client.ParticleStatus;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.util.PriorityQueue;

@Mod.EventBusSubscriber(modid = Oasiso.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class KarakLiquidVfx {
    public static final int RANGE = 14;
    private static final int Y_RANGE = 8;
    private static final int MAX_SURFACES = 1024;
    private static final float MAX_SHORE_DISTANCE = 6.0F;
    private static final int SHORE_PADDING = 7;
    private static final Direction[] HORIZONTAL = {Direction.WEST, Direction.EAST, Direction.NORTH, Direction.SOUTH};
    static final List<Surface> SURFACES = new ArrayList<>();
    static ClientLevel world;
    static int ticks;
    static float immersion, previousImmersion;

    static boolean isJade(FluidState fluid) {
        return fluid.getType().isSame(ModKarakFluids.KR_WATER.get());
    }

    static boolean contains(ClientLevel level, double x, double y, double z) {
        BlockPos p = BlockPos.containing(x, y, z);
        FluidState f = level.getFluidState(p);
        return isJade(f) && y < p.getY() + f.getHeight(level, p);
    }

    @SubscribeEvent
    public static void tick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getInstance();
        if (world != mc.level) {
            world = mc.level;
            SURFACES.clear();
            ticks = 0;
            immersion = previousImmersion = 0;
            KarakLiquidParticle.resetBudget(world);
        }
        if (world == null || mc.isPaused()) return;
        Vec3 camera = mc.gameRenderer.getMainCamera().getPosition();
        boolean under = contains(world, camera.x, camera.y, camera.z);
        previousImmersion = immersion;
        immersion = Mth.clamp(immersion + (under ? 0.14F : -0.10F), 0, 1);
        if (ticks++ % 8 == 0) scan(camera);
        ParticleStatus quality = mc.options.particles().get();
        if (quality == ParticleStatus.MINIMAL) return;
        int underwaterCount = quality == ParticleStatus.DECREASED ? 1 : 2;
        if (under) {
            for (int i = 0; i < underwaterCount; i++) {
                for (int attempt = 0; attempt < 8; attempt++) {
                    double x = camera.x + (world.random.nextDouble() - .5) * 5;
                    double y = camera.y + (world.random.nextDouble() - .5) * 4;
                    double z = camera.z + (world.random.nextDouble() - .5) * 5;
                    if (contains(world, x, y, z) && world.getBlockState(BlockPos.containing(x, y, z)).getCollisionShape(world, BlockPos.containing(x, y, z)).isEmpty()) {
                        world.addParticle(Oasiso.KARAK_BUBBLES.get(), x, y, z, 0, 0, 0);
                        break;
                    }
                }
            }
        }
        if (SURFACES.isEmpty()) return;
        if (ticks % (quality == ParticleStatus.DECREASED ? 8 : 4) == 0) spawnSurface(false);
        if (ticks % (quality == ParticleStatus.DECREASED ? 28 : 14) == 0) spawnSurface(true);
    }

    private static void spawnSurface(boolean steam) {
        Surface s = SURFACES.get(world.random.nextInt(SURFACES.size()));
        if (!isJade(world.getFluidState(s.pos)) || !world.getBlockState(s.pos.above()).isAir()) return;
        double u = .15 + world.random.nextDouble() * .7, v = .15 + world.random.nextDouble() * .7;
        double h = s.height((float) u, (float) v);
        world.addParticle(steam ? Oasiso.KARAK_STEAM.get() : Oasiso.KARAK_BUBBLES.get(), s.pos.getX() + u, s.pos.getY() + h + .025, s.pos.getZ() + v, 0, steam ? 0 : .15 + world.random.nextDouble() * .45, 0);
    }

    private static void scan(Vec3 eye) {
        SURFACES.clear();
        BlockPos center = BlockPos.containing(eye);
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        Map<BlockPos, Float> shoreDistances = new HashMap<>();
        int scanRange = RANGE + SHORE_PADDING;
        for (int z = -scanRange; z <= scanRange; z++)
            for (int x = -scanRange; x <= scanRange; x++) {
                if (x * x + z * z > scanRange * scanRange) continue;
                for (int y = -Y_RANGE; y <= Y_RANGE; y++) {
                    p.set(center.getX() + x, center.getY() + y, center.getZ() + z);
                    if (!world.hasChunkAt(p)) continue;
                    if (!isJade(world.getFluidState(p)) || !world.getBlockState(p.above()).isAir()) continue;
                    BlockPos pos = p.immutable();
                    shoreDistances.put(pos, MAX_SHORE_DISTANCE);
                    if (x * x + z * z > RANGE * RANGE) continue;
                    int mask = 0;
                    for (int i = 0; i < 4; i++) {
                        BlockPos n = pos.relative(HORIZONTAL[i]);
                        if (world.hasChunkAt(n) && world.getBlockState(n).isFaceSturdy(world, n, HORIZONTAL[i].getOpposite()))
                            mask |= 1 << i;
                    }
                    SURFACES.add(new Surface(pos, mask, corner(pos, -1, -1), corner(pos, 1, -1), corner(pos, 1, 1), corner(pos, -1, 1)));
                }
            }
        SURFACES.sort(Comparator.comparingDouble(s -> s.pos.distToCenterSqr(eye.x, eye.y, eye.z)));
        if (SURFACES.size() > MAX_SURFACES) SURFACES.subList(MAX_SURFACES, SURFACES.size()).clear();
        if (!SURFACES.isEmpty()) {
            Map<BlockPos, Float> bankDistances = new HashMap<>(shoreDistances);
            measureShoreDistances(shoreDistances, false);
            measureShoreDistances(bankDistances, true);
            for (Surface s : SURFACES) {
                s.depthNw = vertexDistance(shoreDistances, s.pos, -1, -1);
                s.depthNe = vertexDistance(shoreDistances, s.pos, 1, -1);
                s.depthSe = vertexDistance(shoreDistances, s.pos, 1, 1);
                s.depthSw = vertexDistance(shoreDistances, s.pos, -1, 1);
                s.bankNw = bankVertexDistance(bankDistances, s.pos, -1, -1);
                s.bankNe = bankVertexDistance(bankDistances, s.pos, 1, -1);
                s.bankSe = bankVertexDistance(bankDistances, s.pos, 1, 1);
                s.bankSw = bankVertexDistance(bankDistances, s.pos, -1, 1);
            }
        }
    }


    private static void measureShoreDistances(Map<BlockPos, Float> distances, boolean solidBanksOnly) {
        PriorityQueue<ShoreStep> queue = new PriorityQueue<>(Comparator.comparingDouble(ShoreStep::distance));
        for (Map.Entry<BlockPos, Float> entry : distances.entrySet()) {
            BlockPos pos = entry.getKey();
            for (Direction dir : HORIZONTAL) {
                BlockPos neighbor = pos.relative(dir);
                boolean loaded = world.hasChunkAt(neighbor);
                boolean boundary = solidBanksOnly ? loaded && world.getBlockState(neighbor).isFaceSturdy(world, neighbor, dir.getOpposite()) : !loaded || !isJade(world.getFluidState(neighbor));
                if (boundary) {
                    entry.setValue(0.5F);
                    queue.add(new ShoreStep(pos, 0.5F));
                    break;
                }
            }
        }
        while (!queue.isEmpty()) {
            ShoreStep step = queue.poll();
            if (step.distance() > distances.get(step.pos())) continue;
            for (int dz = -1; dz <= 1; dz++)
                for (int dx = -1; dx <= 1; dx++) {
                    if (dx == 0 && dz == 0) continue;
                    BlockPos next = step.pos().offset(dx, 0, dz);
                    Float previous = distances.get(next);
                    if (previous == null) continue;
                    boolean diagonal = dx != 0 && dz != 0;
                    if (diagonal && (!distances.containsKey(step.pos().offset(dx, 0, 0)) || !distances.containsKey(step.pos().offset(0, 0, dz))))
                        continue;
                    float distance = step.distance() + (diagonal ? 1.41421356F : 1.0F);
                    if (distance >= previous) continue;
                    distances.put(next, distance);
                    queue.add(new ShoreStep(next, distance));
                }
        }
    }

    private static float vertexDistance(Map<BlockPos, Float> distances, BlockPos pos, int dx, int dz) {
        return (distances.getOrDefault(pos, 0.0F) + distances.getOrDefault(pos.offset(dx, 0, 0), 0.0F) + distances.getOrDefault(pos.offset(0, 0, dz), 0.0F) + distances.getOrDefault(pos.offset(dx, 0, dz), 0.0F)) * 0.25F;
    }

    private static float bankVertexDistance(Map<BlockPos, Float> distances, BlockPos pos, int dx, int dz) {
        float sum = 0.0F;
        int count = 0;
        for (BlockPos p : new BlockPos[]{pos, pos.offset(dx, 0, 0), pos.offset(0, 0, dz), pos.offset(dx, 0, dz)}) {
            Float value = distances.get(p);
            if (value != null) {
                sum += value;
                count++;
            } else if (world.hasChunkAt(p)) {
                for (Direction direction : HORIZONTAL) {
                    if (world.getBlockState(p).isFaceSturdy(world, p, direction)) {
                        count++;
                        break;
                    }
                }
            }
        }
        return count == 0 ? MAX_SHORE_DISTANCE : sum / count;
    }

    private record ShoreStep(BlockPos pos, float distance) {
    }

    private static float ownHeight(BlockPos p) {
        FluidState f = world.getFluidState(p);
        if (isJade(f)) return isJade(world.getFluidState(p.above())) ? 1 : f.getOwnHeight();
        return world.getBlockState(p).isSolid() ? -1 : 0;
    }

    private static float corner(BlockPos p, int dx, int dz) {
        float a = ownHeight(p), b = ownHeight(p.offset(dx, 0, 0)), c = ownHeight(p.offset(0, 0, dz));
        if (a >= 1 || b >= 1 || c >= 1) return 1;
        float d = (b > 0 || c > 0) ? ownHeight(p.offset(dx, 0, dz)) : -1;
        if (d >= 1) return 1;
        float total = 0, weight = 0;
        for (float h : new float[]{a, b, c, d})
            if (h >= 0) {
                float w = h >= .8F ? 10 : 1;
                total += h * w;
                weight += w;
            }
        return weight == 0 ? 0 : total / weight;
    }

    static final class Surface {
        final BlockPos pos;
        final int mask;
        final float nw, ne, se, sw;
        float depthNw, depthNe, depthSe, depthSw;
        float bankNw, bankNe, bankSe, bankSw;

        Surface(BlockPos pos, int mask, float nw, float ne, float se, float sw) {
            this.pos = pos;
            this.mask = mask;
            this.nw = nw;
            this.ne = ne;
            this.se = se;
            this.sw = sw;
        }

        float height(float u, float v) {
            return Mth.lerp(v, Mth.lerp(u, nw, ne), Mth.lerp(u, sw, se));
        }
    }

    private KarakLiquidVfx() {
    }
}
