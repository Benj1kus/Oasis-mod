package com.benji.oasiso.common.block.entity;

import com.benji.oasiso.ModSounds;
import com.benji.oasiso.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.ArrayList;
import java.util.List;

public final class AzumalitCrystalBlockEntity extends BlockEntity {
    public static final double RANGE = 20.0;
    public static final int IMPACT_TICKS = 2;
    public static final int LIFE_TICKS = 26;
    private static final int MIN_DELAY = 200;
    private static final int MAX_DELAY = 600;
    private static final double PLAYER_RANGE = 32.0;

    private long nextStrike;
    private long strikeStart = -1;
    private long seed;
    private Direction hitFace = Direction.UP;
    private List<Vec3> path = List.of();
    private boolean impactPending;

    public AzumalitCrystalBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.AZUMALIT_CRYSTAL_BE.get(), pos, state);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, AzumalitCrystalBlockEntity crystal) {
        if (level instanceof ServerLevel server) crystal.tickServer(server);
    }

    private void tickServer(ServerLevel server) {
        long now = server.getGameTime();
        if (impactPending && now >= strikeStart + IMPACT_TICKS) {
            impactPending = false;

            if (now <= strikeStart + IMPACT_TICKS + 2 && !path.isEmpty()) {
                Vec3 hit = path.get(path.size() - 1);
                server.playSound(null, hit.x, hit.y, hit.z, ModSounds.CRYSTAL_HIT.get(), SoundSource.BLOCKS, 0.65F, 0.90F + server.random.nextFloat() * 0.20F);
            }
        }
        if (nextStrike == 0 || nextStrike > now + MAX_DELAY) {
            nextStrike = now + delay(server.random);
            setChanged();
            return;
        }
        if (now < nextStrike) return;
        nextStrike = now + delay(server.random);
        setChanged();
        if (!server.hasNearbyAlivePlayer(worldPosition.getX() + .5, worldPosition.getY() + .5, worldPosition.getZ() + .5, PLAYER_RANGE))
            return;

        Vec3 source = Vec3.atCenterOf(worldPosition);
        BlockHitResult hit = null;
        for (int attempt = 0; attempt < 24; attempt++) {
            Vec3 direction = new Vec3(server.random.nextDouble() * 2 - 1, server.random.nextDouble() * 2 - 1, server.random.nextDouble() * 2 - 1);
            if (direction.lengthSqr() < .05) continue;
            Vec3 end = source.add(direction.normalize().scale(RANGE));
            if (!loaded(server, source, end)) continue;
            BlockHitResult test = clip(server, source, end);
            if (test.getType() != HitResult.Type.BLOCK || test.getBlockPos().equals(worldPosition)) continue;
            double distance = test.getLocation().distanceTo(source);
            if (distance < .65 || distance > RANGE) continue;
            hit = test;
            break;
        }
        if (hit == null) {
            nextStrike = now + 40 + server.random.nextInt(61); // Рядом только воздух — повторим позже.
            return;
        }
        Vec3 normal = Vec3.atLowerCornerOf(hit.getDirection().getNormal());
        Vec3 end = hit.getLocation().add(normal.scale(.035));

        seed = server.random.nextLong();
        hitFace = hit.getDirection();
        path = buildPath(server, source, end, seed);
        strikeStart = now;
        impactPending = true;
        setChanged();
        BlockState state = getBlockState();
        server.sendBlockUpdated(worldPosition, state, state, Block.UPDATE_CLIENTS);
    }

    private static int delay(RandomSource random) {
        return MIN_DELAY + random.nextInt(MAX_DELAY - MIN_DELAY + 1);
    }

    private List<Vec3> buildPath(ServerLevel level, Vec3 from, Vec3 to, long seed) {
        Vec3 axis = to.subtract(from).normalize();
        Vec3 side = axis.cross(Math.abs(axis.y) > .9 ? new Vec3(1, 0, 0) : new Vec3(0, 1, 0)).normalize();
        Vec3 up = axis.cross(side).normalize();
        int count = Math.max(4, Math.min(12, (int) Math.ceil(from.distanceTo(to) * 1.4)));
        for (double amplitude : new double[]{.40, .20, .07, 0}) {
            RandomSource random = RandomSource.create(seed);
            List<Vec3> points = new ArrayList<>();
            points.add(from);
            for (int i = 1; i < count; i++) {
                double t = i / (double) count, envelope = Math.sin(t * Math.PI);
                double width = Math.min(amplitude, from.distanceTo(to) * .15) * envelope;
                points.add(from.lerp(to, t).add(side.scale((random.nextDouble() * 2 - 1) * width)).add(up.scale((random.nextDouble() * 2 - 1) * width)));
            }
            points.add(to);
            boolean clear = true;
            for (int i = 1; i < points.size(); i++) {
                if (!loaded(level, points.get(i - 1), points.get(i))) {
                    clear = false;
                    break;
                }
                BlockHitResult hit = clip(level, points.get(i - 1), points.get(i));
                if (hit.getType() == HitResult.Type.BLOCK) {
                    clear = false;
                    break;
                }
            }
            if (clear) return List.copyOf(points);
        }
        return List.of(from, to);
    }

    private BlockHitResult clip(Level level, Vec3 from, Vec3 to) {
        return level.clip(new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, null) {
            @Override
            public VoxelShape getBlockShape(BlockState state, BlockGetter world, BlockPos pos) {
                return pos.equals(worldPosition) ? Shapes.empty() : super.getBlockShape(state, world, pos);
            }
        });
    }

    private static boolean loaded(Level level, Vec3 from, Vec3 to) {
        int minX = (int) Math.floor(Math.min(from.x, to.x)) >> 4, maxX = (int) Math.floor(Math.max(from.x, to.x)) >> 4;
        int minZ = (int) Math.floor(Math.min(from.z, to.z)) >> 4, maxZ = (int) Math.floor(Math.max(from.z, to.z)) >> 4;
        for (int x = minX; x <= maxX; x++)
            for (int z = minZ; z <= maxZ; z++)
                if (!level.hasChunk(x, z)) return false;
        return true;
    }

    public long strikeStart() {
        return strikeStart;
    }

    public long seed() {
        return seed;
    }

    public Direction hitFace() {
        return hitFace;
    }

    public List<Vec3> path() {
        return path;
    }

    @Override
    public AABB getRenderBoundingBox() {
        return new AABB(worldPosition).inflate(RANGE + 2);
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putLong("NextArc", nextStrike);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        nextStrike = tag.getLong("NextArc");
        strikeStart = tag.contains("ArcStart") ? tag.getLong("ArcStart") : -1;
        seed = tag.getLong("ArcSeed");
        hitFace = Direction.from3DDataValue(tag.getInt("ArcFace"));
        List<Vec3> points = new ArrayList<>();
        ListTag list = tag.getList("ArcPath", Tag.TAG_COMPOUND);
        for (int i = 0; i < Math.min(16, list.size()); i++) {
            CompoundTag p = list.getCompound(i);
            double x = p.getDouble("px"), y = p.getDouble("py"), z = p.getDouble("pz");
            if (Double.isFinite(x) && Double.isFinite(y) && Double.isFinite(z)) points.add(new Vec3(x, y, z));
        }
        path = List.copyOf(points);
    }

    @Override
    public CompoundTag getUpdateTag() {
        CompoundTag tag = new CompoundTag();
        boolean active = level != null && strikeStart >= 0 && level.getGameTime() - strikeStart < LIFE_TICKS;
        tag.putLong("ArcStart", active ? strikeStart : -1);
        if (active) {
            tag.putLong("ArcSeed", seed);
            tag.putInt("ArcFace", hitFace.get3DDataValue());
            ListTag points = new ListTag();
            for (Vec3 p : path) {
                CompoundTag entry = new CompoundTag();
                entry.putDouble("px", p.x);
                entry.putDouble("py", p.y);
                entry.putDouble("pz", p.z);
                points.add(entry);
            }
            tag.put("ArcPath", points);
        }
        return tag;
    }

    @Override
    public void handleUpdateTag(CompoundTag tag) {
        load(tag);
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
