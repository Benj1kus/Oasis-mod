package com.benji.oasiso.common.entity;

import com.benji.oasiso.Oasiso;
import com.benji.oasiso.common.block.entity.ApolAltarBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.*;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.*;
import net.minecraftforge.network.NetworkHooks;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegisterEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.*;

public class ApolSummoningEntity extends Entity {
    public static final int SIZE = 8, RISE = 20, LIFT_TICKS = 60, ACTIVATE_TICKS = 20;
    public static final int BEAM_START = LIFT_TICKS + ACTIVATE_TICKS;
    public static final int SPAWN_AT = BEAM_START + 60, FINISH_AT = BEAM_START + 120;
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(Oasiso.MODID, "apol_summoning");
    private static final EntityDataAccessor<Integer> AGE = SynchedEntityData.defineId(ApolSummoningEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<CompoundTag> DATA = SynchedEntityData.defineId(ApolSummoningEntity.class, EntityDataSerializers.COMPOUND_TAG);
    private static final EntityDataAccessor<Boolean> SETTLED = SynchedEntityData.defineId(ApolSummoningEntity.class, EntityDataSerializers.BOOLEAN);
    public static EntityType<ApolSummoningEntity> TYPE;
    private boolean spawned;
    private int liftHeight = RISE;
    private boolean terrainChange;
    private final ApolArenaCollision[] collisionRows = new ApolArenaCollision[8];
    private UUID bossId;
    private UUID summoner;
    private BlockPos origin = BlockPos.ZERO;
    private BlockState altar = Blocks.AIR.defaultBlockState();
    private final BlockState[] surface = new BlockState[64];

    public ApolSummoningEntity(EntityType<? extends ApolSummoningEntity> type, Level level) {
        super(type, level);
        setNoGravity(true);
        blocksBuilding = false;
        Arrays.fill(surface, Blocks.STONE.defaultBlockState());
    }

    @Mod.EventBusSubscriber(modid = Oasiso.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static final class Registration {
        @SubscribeEvent
        public static void register(RegisterEvent e) {
            e.register(Registries.ENTITY_TYPE, helper -> {
                TYPE = EntityType.Builder.<ApolSummoningEntity>of(ApolSummoningEntity::new, MobCategory.MISC).sized(8, 8).clientTrackingRange(12).updateInterval(1).fireImmune().build(ID.toString());
                helper.register(ID, TYPE);
                ResourceLocation collisionId = ResourceLocation.fromNamespaceAndPath(Oasiso.MODID, "apol_arena_collision");
                ApolArenaCollision.TYPE = EntityType.Builder.<ApolArenaCollision>of(ApolArenaCollision::new, MobCategory.MISC).sized(1, 8).noSave().fireImmune().clientTrackingRange(12).updateInterval(1).build(collisionId.toString());
                helper.register(collisionId, ApolArenaCollision.TYPE);
            });
        }
    }

    @Override
    protected void defineSynchedData() {
        entityData.define(AGE, 0);
        entityData.define(DATA, new CompoundTag());
        entityData.define(SETTLED, false);
    }

    public int age() {
        return entityData.get(AGE);
    }

    public boolean settled() {
        return entityData.get(SETTLED);
    }

    public BlockPos origin() {
        return origin;
    }

    public BlockPos raisedAltar() {
        return origin.above(liftHeight);
    }

    public BlockState altarState() {
        return altar;
    }

    public BlockState surface(int x, int z) {
        return surface[z * 8 + x];
    }

    public static BlockState soil() {
        Block block = ForgeRegistries.BLOCKS.getValue(ResourceLocation.fromNamespaceAndPath(Oasiso.MODID, "entropy_soil"));
        return block == null ? Blocks.AIR.defaultBlockState() : block.defaultBlockState();
    }

    public static double riseAt(double age) {
        return RISE * Math.max(0, Math.min(1, age / LIFT_TICKS));
    }

    public float beamAge(float partial) {
        return age() - BEAM_START + partial;
    }

    public Vec3 beamBase() {
        return Vec3.atBottomCenterOf(raisedAltar()).add(0, .03, 0);
    }

    public AABB reservedVolume() {
        return new AABB(origin.getX() - 3, origin.getY() - 8, origin.getZ() - 3, origin.getX() + 5, raisedAltar().getY() + 12, origin.getZ() + 5);
    }

    private static boolean reject(Player player, String key) {
        String fallback = switch (key) {
            case "protected" -> "nope";
            case "busy" -> "you already summon him here dumbass";
            case "border" -> "out of bounds bro";
            case "peaceful" -> "are you serious? on peaceful mode?";
            default -> "hmm u r stupid probably";
        };
        player.displayClientMessage(Component.translatableWithFallback("message.oasiso.apol_altar." + key, fallback), true);
        return false;
    }

    public static boolean begin(ServerLevel level, BlockPos pos, Player player) {
        if (player.isSpectator() || !player.mayBuild()) return false;
        if (!(level.getBlockEntity(pos) instanceof ApolAltarBlockEntity be) || be.isActivated()) return false;
        if (level.getDifficulty() == Difficulty.PEACEFUL) return reject(player, "peaceful");
        if (TYPE == null || ApolArenaCollision.TYPE == null || soil().isAir()) return reject(player, "registry");
        EntityType<?> bossType = ForgeRegistries.ENTITY_TYPES.getValue(ResourceLocation.fromNamespaceAndPath(Oasiso.MODID, "apollyon"));
        if (bossType == null || !(bossType.create(level) instanceof ApollyonEntity)) return reject(player, "registry");
        ApolSummoningEntity ritual = TYPE.create(level);
        if (ritual == null) return false;
        ritual.origin = pos.immutable();
        ritual.altar = level.getBlockState(pos);
        ritual.summoner = player.getUUID();
        ritual.liftHeight = Math.max(0, Math.min(RISE, level.getMaxBuildHeight() - 1 - pos.getY()));
        BlockPos min = pos.offset(-3, 0, -3), max = pos.offset(4, 0, 4);
        if (!level.getWorldBorder().isWithinBounds(min) || !level.getWorldBorder().isWithinBounds(max))
            return reject(player, "border");
        ritual.loadArenaChunks(level);
        for (ApolSummoningEntity other : level.getEntitiesOfClass(ApolSummoningEntity.class, ritual.reservedVolume().inflate(24)))
            if (other.reservedVolume().intersects(ritual.reservedVolume())) return reject(player, "busy");
        ritual.terrainChange = true;
        try {
            for (int z = 0; z < 8; z++)
                for (int x = 0; x < 8; x++)
                    if (ApolArenaShape.contains(x, z))
                        for (int y = Math.max(level.getMinBuildHeight(), pos.getY() - 8); y <= ritual.clearTop(level); y++) {
                            BlockPos q = new BlockPos(pos.getX() + x - 3, y, pos.getZ() + z - 3);
                            if (!level.mayInteract(player, q)) return reject(player, "protected");
                            BlockState state = level.getBlockState(q);
                            if (!state.isAir() && net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(new net.minecraftforge.event.level.BlockEvent.BreakEvent(level, q, state, player)))
                                return reject(player, "protected");
                        }
        } finally {
            ritual.terrainChange = false;
        }
        BlockPos below = pos.below();
        BlockState top = level.getBlockState(below);
        if (level.getBlockEntity(below) != null || !top.getFluidState().isEmpty() || top.getDestroySpeed(level, below) < 0 || top.getBlock() instanceof FallingBlock || !Block.isShapeFullBlock(top.getCollisionShape(level, below)))
            top = soil();
        Arrays.fill(ritual.surface, top);
        ritual.publishData();
        ritual.updatePosition();
        if (!level.addFreshEntity(ritual)) return false;
        ritual.clearRoute(level, Math.max(level.getMinBuildHeight(), pos.getY() - 8), ritual.clearTop(level));
        ritual.ensureCollisionRows(level);
        ritual.raiseIntersectingEntities(pos.getY() - 8, pos.getY() + .05, true);
        player.stopRiding();
        Vec3 center = new Vec3(pos.getX() + 1, pos.getY() + .05, pos.getZ() + 1);
        Vec3 direction = new Vec3(player.getX() - center.x, 0, player.getZ() - center.z);
        if (direction.lengthSqr() < .01) direction = new Vec3(0, 0, 1);
        Vec3 start = center.add(direction.normalize().scale(2));
        teleportOnto(player, start.x, start.y, start.z);
        return true;
    }

    public boolean isTerrainChange() {
        return terrainChange;
    }

    public boolean reserves(BlockPos pos) {
        return ApolArenaShape.contains(pos.getX() - origin.getX() + 3, pos.getZ() - origin.getZ() + 3) && pos.getY() >= origin.getY() - 8 && pos.getY() <= raisedAltar().getY() + 11;
    }

    private int clearTop(ServerLevel level) {
        return Math.min(level.getMaxBuildHeight() - 1, raisedAltar().getY() + 11);
    }

    private void loadArenaChunks(ServerLevel level) {
        for (int cz = (origin.getZ() - 3) >> 4; cz <= (origin.getZ() + 4) >> 4; cz++)
            for (int cx = (origin.getX() - 3) >> 4; cx <= (origin.getX() + 4) >> 4; cx++) level.getChunk(cx, cz);
    }

    private void clearRoute(ServerLevel level, int minY, int maxY) {
        terrainChange = true;
        try {
            for (int z = 0; z < 8; z++)
                for (int x = 0; x < 8; x++)
                    if (ApolArenaShape.contains(x, z))
                        for (int y = Math.max(level.getMinBuildHeight(), minY); y <= Math.min(level.getMaxBuildHeight() - 1, maxY); y++) {
                            BlockPos q = new BlockPos(origin.getX() + x - 3, y, origin.getZ() + z - 3);
                            BlockState state = level.getBlockState(q);
                            if (state.isAir()) continue;
                            level.setBlock(q, Blocks.AIR.defaultBlockState(), 18);
                        }
        } finally {
            terrainChange = false;
        }
    }

    private void ensureCollisionRows(ServerLevel level) {
        for (int row = 0; row < 8; row++) {
            ApolArenaCollision part = collisionRows[row];
            if (part == null || part.isRemoved()) {
                part = ApolArenaCollision.TYPE.create(level);
                if (part == null) continue;
                part.attach(this, row);
                if (level.addFreshEntity(part)) collisionRows[row] = part;
            } else part.sync();
        }
    }

    private void removeCollisionRows() {
        for (ApolArenaCollision part : collisionRows) if (part != null) part.discard();
    }

    private boolean overlapsPlatform(Entity e) {
        AABB b = e.getBoundingBox();
        double x0 = origin.getX() - 3, z0 = origin.getZ() - 3;
        return ApolArenaShape.intersects(b.minX - x0, b.minZ - z0, b.maxX - x0, b.maxZ - z0);
    }

    private static void teleportOnto(Entity e, double x, double y, double z) {
        if (e instanceof net.minecraft.server.level.ServerPlayer player)
            player.connection.teleport(x, y, z, player.getYRot(), player.getXRot());
        else e.teleportTo(x, y, z);
        e.fallDistance = 0;
        e.setOnGround(true);
        e.setDeltaMovement(e.getDeltaMovement().multiply(1, 0, 1));
    }

    private void raiseIntersectingEntities(double bottom, double top, boolean initial) {
        AABB area = new AABB(origin.getX() - 3, bottom, origin.getZ() - 3, origin.getX() + 5, top + 1, origin.getZ() + 5);
        for (Entity e : level().getEntities(this, area, e -> !(e instanceof ApolArenaCollision) && !e.isSpectator() && !e.isPassenger())) {
            if (!overlapsPlatform(e) || e.getY() >= top) continue;
            if (level().isClientSide && !(e instanceof Player p && p.isLocalPlayer())) continue;
            double distance = top - e.getY();
            if (initial || distance > .51) {
                if (!level().isClientSide) teleportOnto(e, e.getX(), top, e.getZ());
            } else {
                e.move(MoverType.PISTON, new Vec3(0, distance, 0));
                e.fallDistance = 0;
                e.setOnGround(true);
                if (e.getDeltaMovement().y < 0) e.setDeltaMovement(e.getDeltaMovement().multiply(1, 0, 1));
            }
        }
    }

    private void publishData() {
        CompoundTag data = new CompoundTag();
        data.putLong("Origin", origin.asLong());
        data.putInt("LiftHeight", liftHeight);
        data.put("Altar", NbtUtils.writeBlockState(altar));
        ListTag list = new ListTag();
        for (BlockState state : surface) list.add(NbtUtils.writeBlockState(state));
        data.put("Surface", list);
        entityData.set(DATA, data);
    }

    private void readVisualData(CompoundTag data) {
        if (!data.contains("Origin")) return;
        origin = BlockPos.of(data.getLong("Origin"));
        liftHeight = data.contains("LiftHeight") ? Math.max(0, Math.min(RISE, data.getInt("LiftHeight"))) : RISE;
        var lookup = level().holderLookup(Registries.BLOCK);
        altar = NbtUtils.readBlockState(lookup, data.getCompound("Altar"));
        ListTag list = data.getList("Surface", Tag.TAG_COMPOUND);
        for (int i = 0; i < 64; i++)
            surface[i] = i < list.size() ? NbtUtils.readBlockState(lookup, list.getCompound(i)) : soil();
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
        super.onSyncedDataUpdated(key);
        if (DATA.equals(key)) readVisualData(entityData.get(DATA));
    }

    private void updatePosition() {
        setPos(origin.getX() + 1, origin.getY() - 8 + riseAt(age()) * liftHeight / RISE, origin.getZ() + 1);
    }

    @Override
    public void lerpTo(double x, double y, double z, float yaw, float pitch, int steps, boolean teleport) {
    }

    @Override
    public boolean canBeCollidedWith() {
        return false;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    public boolean isInvulnerableTo(net.minecraft.world.damagesource.DamageSource source) {
        return true;
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 192 * 192;
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }

    @Override
    public void tick() {
        super.tick();
        if (entityData.get(DATA).isEmpty()) return;
        if (!level().isClientSide && !settled()) loadArenaChunks((ServerLevel) level());
        int oldAge = age();
        if (!level().isClientSide && (oldAge < LIFT_TICKS || settled())) entityData.set(AGE, oldAge + 1);
        double oldTop = getY() + 8;
        updatePosition();
        double dy = getY() + 8 - oldTop;
        if (!settled()) {
            if (!level().isClientSide) {
                ServerLevel server = (ServerLevel) level();
                clearRoute(server, (int) Math.floor(getY()), Math.min(clearTop(server), (int) Math.ceil(getY() + 11)));
                ensureCollisionRows(server);
            }
            if (dy >= 0 && dy <= .51) carryStandingEntities(oldTop, dy);
        }
        if (level().isClientSide) return;
        ServerLevel server = (ServerLevel) level();
        if (age() >= LIFT_TICKS && !settled()) {
            if (!settle(server)) return;
        }
        if (age() >= LIFT_TICKS && age() < SPAWN_AT && age() % 3 == 0)
            server.sendParticles(Oasiso.ENTROPY_LIGHTNING.get(), origin.getX() + .5, raisedAltar().getY() + 1, origin.getZ() + .5, 3, .7, .9, .7, .03);
        if (age() >= SPAWN_AT && !spawned) {
            if (server.getDifficulty() == Difficulty.PEACEFUL) {
                entityData.set(AGE, SPAWN_AT - 1);
                return;
            }
            if (!spawnBoss(server)) {
                entityData.set(AGE, SPAWN_AT - 1);
                return;
            }
        }
        if (age() >= FINISH_AT && spawned) discard();
    }

    private void carryStandingEntities(double oldTop, double dy) {
        raiseIntersectingEntities(getY(), getY() + 8 + .02, false);
        AABB top = new AABB(getX() - 4, oldTop + .7, getZ() - 4, getX() + 4, oldTop + 1.3, getZ() + 4);
        for (Entity e : level().getEntities(this, top, e -> !(e instanceof ApolArenaCollision) && !e.isSpectator() && !e.isPassenger())) {
            if (Math.abs(e.getX() - (origin.getX() + .5)) >= .8 || Math.abs(e.getZ() - (origin.getZ() + .5)) >= .8 || Math.abs(e.getY() - (oldTop + 1)) > .2 || e.getDeltaMovement().y > .12)
                continue;
            if (level().isClientSide && !(e instanceof Player p && p.isLocalPlayer())) continue;
            e.move(MoverType.PISTON, new Vec3(0, dy, 0));
            e.fallDistance = 0;
            e.setOnGround(true);
            if (e.getDeltaMovement().y < 0) e.setDeltaMovement(e.getDeltaMovement().multiply(1, 0, 1));
        }
    }

    private boolean settle(ServerLevel server) {
        BlockPos base = raisedAltar();
        // Arena wins over late terrain updates too; never wait for an empty landing box.
        clearRoute(server, base.getY() - 8, clearTop(server));
        raiseIntersectingEntities(base.getY() - 8, base.getY() + .02, false);
        for (int y = -8; y < 0; y++)
            for (int z = 0; z < 8; z++)
                for (int x = 0; x < 8; x++)
                    if (ApolArenaShape.contains(x, z)) {
                        BlockPos q = base.offset(x - 3, y, z - 3);
                        if (q.getY() >= server.getMinBuildHeight())
                            server.setBlock(q, y == -1 ? surface(x, z) : soil(), 18);
                    }
        server.setBlock(base, altar, 3);
        if (server.getBlockEntity(base) instanceof ApolAltarBlockEntity be) be.activate();
        entityData.set(SETTLED, true);
        blocksBuilding = false;
        removeCollisionRows();
        for (int y = -8; y < 0; y++)
            for (int z = 0; z < 8; z++)
                for (int x = 0; x < 8; x++)
                    if (ApolArenaShape.contains(x, z) && (y == -8 || y == -1 || ApolArenaShape.rim(x, z))) {
                        BlockPos q = base.offset(x - 3, y, z - 3);
                        if (q.getY() >= server.getMinBuildHeight())
                            server.updateNeighborsAt(q, server.getBlockState(q).getBlock());
                    }
        return true;
    }

    private boolean spawnBoss(ServerLevel server) {
        if (bossId != null && server.getEntity(bossId) instanceof ApollyonEntity) {
            spawned = true;
            return true;
        }
        EntityType<?> type = ForgeRegistries.ENTITY_TYPES.getValue(ResourceLocation.fromNamespaceAndPath(Oasiso.MODID, "apollyon"));
        Entity created = type == null ? null : type.create(server);
        if (!(created instanceof ApollyonEntity boss)) return false;
        boss.moveTo(origin.getX() + .5, raisedAltar().getY() + 1.1, origin.getZ() + .5, 0, 0);
        boss.finalizeSpawn(server, server.getCurrentDifficultyAt(raisedAltar()), MobSpawnType.EVENT, null, null);
        boss.setPersistenceRequired();
        Player player = summoner == null ? null : server.getPlayerByUUID(summoner);
        if (player != null && player.isAlive() && !player.isCreative() && !player.isSpectator()) boss.setTarget(player);
        bossId = boss.getUUID();
        if (!server.addFreshEntity(boss)) {
            bossId = null;
            return false;
        }
        spawned = true;
        if (server.getBlockState(raisedAltar()).getBlock() == altar.getBlock())
            server.destroyBlock(raisedAltar(), false);
        server.sendParticles(Oasiso.ENTROPY_LIGHTNING.get(), boss.getX(), boss.getY() + 1, boss.getZ(), 36, 2, 2, 2, .12);
        server.sendParticles(net.minecraft.core.particles.ParticleTypes.EXPLOSION_EMITTER, boss.getX(), boss.getY(), boss.getZ(), 1, 0, 0, 0, 0);
        server.playSound(null, raisedAltar(), net.minecraft.sounds.SoundEvents.GENERIC_EXPLODE, net.minecraft.sounds.SoundSource.HOSTILE, 2, .7F);
        return true;
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.put("Platform", entityData.get(DATA).copy());
        tag.putInt("Age", age());
        tag.putBoolean("Settled", settled());
        tag.putBoolean("Spawned", spawned);
        if (bossId != null) tag.putUUID("Boss", bossId);
        if (summoner != null) tag.putUUID("Summoner", summoner);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        entityData.set(DATA, tag.getCompound("Platform"));
        readVisualData(entityData.get(DATA));
        entityData.set(AGE, tag.getInt("Age"));
        entityData.set(SETTLED, tag.getBoolean("Settled"));
        spawned = tag.getBoolean("Spawned");
        bossId = tag.hasUUID("Boss") ? tag.getUUID("Boss") : null;
        summoner = tag.hasUUID("Summoner") ? tag.getUUID("Summoner") : null;
        blocksBuilding = false;
        updatePosition();
    }
}
