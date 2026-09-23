package com.benji.oasiso.common.world;

import com.benji.oasiso.ModSounds;
import com.benji.oasiso.Oasiso;
import com.benji.oasiso.common.block.EntropyBlock;
import com.benji.oasiso.common.entity.EntropyCreatureEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public final class EntropyCreatureSpawns extends SavedData {
    public static final double RADIUS = 30.0;
    private static final int COOLDOWN = 20 * 60;
    private static final int MIN_DELAY = 20;
    private static final int MAX_DELAY = 100;

    private static final ResourceLocation CREATURE_ID = ResourceLocation.fromNamespaceAndPath(Oasiso.MODID, "entropy_creature");
    private final List<Wave> waves = new ArrayList<>();

    public static EntropyCreatureSpawns get(ServerLevel level) {
        return level.getServer().overworld().getDataStorage().computeIfAbsent(EntropyCreatureSpawns::load, EntropyCreatureSpawns::new, "oasiso_entropy_creature_waves");
    }

    public void check(ServerLevel level, BlockPos source) {
        if (level.getDifficulty() == Difficulty.PEACEFUL || !level.getGameRules().getBoolean(GameRules.RULE_DOMOBSPAWNING))
            return;
        long now = level.getServer().overworld().getGameTime();
        Vec3 center = Vec3.atCenterOf(source);
        boolean playerNearby = level.players().stream().anyMatch(p -> p.isAlive() && !p.isCreative() && !p.isSpectator() && p.distanceToSqr(center) <= RADIUS * RADIUS);
        if (!playerNearby) return;

        String dimension = level.dimension().location().toString();
        Wave own = null;
        Iterator<Wave> iterator = waves.iterator();
        while (iterator.hasNext()) {
            Wave wave = iterator.next();
            if (wave.members.isEmpty() && wave.pending == 0 && now >= wave.readyAt) {
                iterator.remove();
                setDirty();
                continue;
            }
            if (!wave.dimension.equals(dimension)) continue;
            if (wave.pending > 0 && now > wave.readyAt + 100 && level.players().stream().noneMatch(p -> p.isAlive() && !p.isCreative() && !p.isSpectator() && p.distanceToSqr(Vec3.atCenterOf(wave.source)) <= RADIUS * RADIUS)) {
                iterator.remove();
                setDirty();
                continue;
            }
            if (wave.pending > 0 && level.hasChunkAt(wave.source) && !(level.getBlockState(wave.source).getBlock() instanceof EntropyBlock)) {
                iterator.remove();
                setDirty();
                continue;
            }
            if (wave.source.distSqr(source) <= RADIUS * RADIUS) {
                if (!wave.members.isEmpty() || wave.pending == 0) return;
                if (!wave.source.equals(source)) return; // Сосед не создаёт свою волну.
                own = wave;
            }
        }
        if (own != null && now < own.readyAt) return;
        if (!level.getEntitiesOfClass(EntropyCreatureEntity.class, new AABB(source).inflate(RADIUS), e -> e.isAlive() && e.distanceToSqr(center) <= RADIUS * RADIUS).isEmpty())
            return;

        if (own == null) {
            own = new Wave(UUID.randomUUID(), dimension, source.immutable());
            own.pending = 1 + level.random.nextInt(2);
            own.readyAt = now + MIN_DELAY + level.random.nextInt(MAX_DELAY - MIN_DELAY + 1);
            waves.add(own);
            setDirty();
            return;
        }
        if (now < own.readyAt) return;

        int desired = own.pending;
        for (int i = 0; i < desired; i++) {
            EntropyCreatureEntity mob = createAtSafePosition(level, source);
            if (mob == null) break;
            mob.setPersistenceRequired();
            mob.setEntropyWave(own.id);
            mob.beginSpawnAnimation();
            if (!level.addFreshEntity(mob)) continue;
            own.members.add(mob.getUUID());
            level.playSound(null, mob.blockPosition(), ModSounds.CREATURE_SPAWN.get(), SoundSource.HOSTILE, 1F, 1F);
            spawnSoilParticles(level, mob.blockPosition());
        }
        if (own.members.isEmpty()) {
            own.readyAt = now + MIN_DELAY + level.random.nextInt(MAX_DELAY - MIN_DELAY + 1);
        } else {
            own.pending = 0;
        }
        setDirty();
    }

    public void removed(ServerLevel level, UUID waveId, UUID member) {
        for (Wave wave : waves) {
            if (wave.id.equals(waveId) && wave.members.remove(member)) {
                if (wave.members.isEmpty()) {
                    wave.pending = 0;
                    wave.readyAt = level.getServer().overworld().getGameTime() + COOLDOWN;
                }
                setDirty();
                return;
            }
        }
    }

    private static EntropyCreatureEntity createAtSafePosition(ServerLevel level, BlockPos source) {
        EntityType<?> type = ForgeRegistries.ENTITY_TYPES.getValue(CREATURE_ID);
        if (type == null) return null;
        Entity entity = type.create(level);
        if (!(entity instanceof EntropyCreatureEntity mob)) return null;

        for (int attempt = 0; attempt < 32; attempt++) {
            int x = source.getX() + level.random.nextInt(11) - 5;
            int z = source.getZ() + level.random.nextInt(11) - 5;
            for (int dy = 3; dy >= -4; dy--) {
                BlockPos floor = new BlockPos(x, source.getY() + dy, z);
                if (!level.hasChunkAt(floor)) break;
                if (!level.getBlockState(floor).isFaceSturdy(level, floor, Direction.UP)) continue;
                mob.moveTo(x + 0.5, floor.getY() + 1, z + 0.5, level.random.nextFloat() * 360F, 0F);
                AABB box = mob.getBoundingBox();
                boolean loaded = true;
                for (BlockPos p : BlockPos.betweenClosed(BlockPos.containing(box.minX, box.minY, box.minZ), BlockPos.containing(box.maxX, box.maxY, box.maxZ))) {
                    if (!level.hasChunkAt(p)) {
                        loaded = false;
                        break;
                    }
                }
                if (!loaded || !level.getWorldBorder().isWithinBounds(box) || !level.noCollision(mob, box) || level.containsAnyLiquid(box) || !level.getEntities(mob, box, e -> e.isAlive() && !e.isSpectator()).isEmpty())
                    continue;
                mob.finalizeSpawn(level, level.getCurrentDifficultyAt(mob.blockPosition()), MobSpawnType.MOB_SUMMONED, null, null);
                return mob;
            }
        }
        mob.discard();
        return null;
    }

    private static void spawnSoilParticles(ServerLevel level, BlockPos feet) {
        var soil = ForgeRegistries.BLOCKS.getValue(ResourceLocation.fromNamespaceAndPath(Oasiso.MODID, "entropy_soil"));
        if (soil == null) return;
        BlockParticleOption particle = new BlockParticleOption(ParticleTypes.BLOCK, soil.defaultBlockState());

        for (int dx = -2; dx <= 2; dx++)
            for (int dz = -2; dz <= 2; dz++) {
                for (int dy = 1; dy >= -3; dy--) {
                    BlockPos floor = feet.offset(dx, dy, dz);
                    if (!level.hasChunkAt(floor)) break;
                    var shape = level.getBlockState(floor).getCollisionShape(level, floor);
                    if (shape.isEmpty() || !level.getBlockState(floor.above()).getCollisionShape(level, floor.above()).isEmpty())
                        continue;
                    for (int n = 0; n < 3; n++) {
                        level.sendParticles(particle, floor.getX() + level.random.nextDouble(), floor.getY() + shape.max(Direction.Axis.Y) + 0.05, floor.getZ() + level.random.nextDouble(), 0, (level.random.nextDouble() - .5) * .18, .14 + level.random.nextDouble() * .16, (level.random.nextDouble() - .5) * .18, 1);
                    }
                    break;
                }
            }
    }

    public static EntropyCreatureSpawns load(CompoundTag tag) {
        EntropyCreatureSpawns data = new EntropyCreatureSpawns();
        ListTag list = tag.getList("Waves", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag entry = list.getCompound(i);
            Wave wave = new Wave(entry.getUUID("Id"), entry.getString("Dimension"), BlockPos.of(entry.getLong("Source")));
            wave.readyAt = entry.getLong("ReadyAt");
            wave.pending = entry.getInt("Pending");
            ListTag members = entry.getList("Members", Tag.TAG_COMPOUND);
            for (int j = 0; j < members.size(); j++) wave.members.add(members.getCompound(j).getUUID("Id"));
            data.waves.add(wave);
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        ListTag list = new ListTag();
        for (Wave wave : waves) {
            CompoundTag entry = new CompoundTag();
            entry.putUUID("Id", wave.id);
            entry.putString("Dimension", wave.dimension);
            entry.putLong("Source", wave.source.asLong());
            entry.putLong("ReadyAt", wave.readyAt);
            entry.putInt("Pending", wave.pending);
            ListTag members = new ListTag();
            for (UUID id : wave.members) {
                CompoundTag member = new CompoundTag();
                member.putUUID("Id", id);
                members.add(member);
            }
            entry.put("Members", members);
            list.add(entry);
        }
        tag.put("Waves", list);
        return tag;
    }

    private static final class Wave {
        final UUID id;
        final String dimension;
        final BlockPos source;
        final Set<UUID> members = new HashSet<>();
        int pending;
        long readyAt;

        Wave(UUID id, String dimension, BlockPos source) {
            this.id = id;
            this.dimension = dimension;
            this.source = source;
        }
    }
}
