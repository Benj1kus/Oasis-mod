package com.benji.oasiso.common.entity;

import com.benji.oasiso.Oasiso;
import com.benji.oasiso.common.item.EntropyChestplateGloveItem;
import com.benji.oasiso.common.util.MeltedNephritisEffects;
import com.benji.oasiso.registry.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.NoteBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.entity.IEntityAdditionalSpawnData;
import net.minecraftforge.network.NetworkHooks;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class EntropyPhysicsBlockEntity extends Entity implements IEntityAdditionalSpawnData {

    public static final int MODE_PULLING = 0;
    public static final int MODE_HELD = 1;
    public static final int MODE_DROPPED = 2;
    public static final int MODE_THROWN = 3;
    public static final int MODE_SETTLED = 4;

    private static final EntityDataAccessor<Integer> MODE = SynchedEntityData.defineId(EntropyPhysicsBlockEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> NEPHRITIS_COATED = SynchedEntityData.defineId(EntropyPhysicsBlockEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<CompoundTag> STRUCTURE_DATA = SynchedEntityData.defineId(EntropyPhysicsBlockEntity.class, EntityDataSerializers.COMPOUND_TAG);

    private static final int PULL_SHAKE_TICKS = 8;

    private static final double HOLD_FORWARD = 2.15D;
    private static final double HOLD_SIDE = 0.72D;
    private static final double HOLD_DOWN = 0.55D;
    private static final double HOLD_FOLLOW_FACTOR = 0.38D;
    private static final double THROW_SPEED_SOFT = 1.68D;
    private static final double THROW_SPEED_HARD = 1.14D;
    private static final double THROW_UP_BONUS = 0.10D;

    private static final double GRAVITY = 0.050D;
    private static final double AIR_DRAG = 0.985D;

    private static final double NORMAL_GROUND_FRICTION = 0.72D;
    private static final double ICE_GROUND_FRICTION = 0.975D;
    private static final double LOG_GROUND_FRICTION = 0.82D;
    private static final double SOFT_BLOCK_BOUNCE = 0.64D;
    private static final double HARD_BLOCK_BOUNCE = 0.24D;
    private static final double SLIME_BOUNCE = 0.92D;
    private static final double LOG_MIN_BOUNCE = 0.74D;
    private static final double DROPPED_BOUNCE_MULTIPLIER = 0.62D;

    private static final int SETTLE_REQUIRED_TICKS = 10;
    private static final double SETTLE_SPEED_SQR = 0.010D;
    private static final int MAX_FREE_TICKS = 20 * 45;

    private static final float TNT_EXPLOSION_POWER = 4.0F;
    private static final int NOTE_IMPACT_COOLDOWN_TICKS = 3;

    private static final int MAX_ATTACHED_BLOCKS = 64;
    private static final int MAX_STRUCTURE_RADIUS = 5;
    private static final Set<Block> FRAGILE_GLASS_BLOCKS = Set.of(Blocks.GLASS, Blocks.GLASS_PANE, Blocks.WHITE_STAINED_GLASS, Blocks.WHITE_STAINED_GLASS_PANE, Blocks.ORANGE_STAINED_GLASS, Blocks.ORANGE_STAINED_GLASS_PANE, Blocks.MAGENTA_STAINED_GLASS, Blocks.MAGENTA_STAINED_GLASS_PANE, Blocks.LIGHT_BLUE_STAINED_GLASS, Blocks.LIGHT_BLUE_STAINED_GLASS_PANE, Blocks.YELLOW_STAINED_GLASS, Blocks.YELLOW_STAINED_GLASS_PANE, Blocks.LIME_STAINED_GLASS, Blocks.LIME_STAINED_GLASS_PANE, Blocks.PINK_STAINED_GLASS, Blocks.PINK_STAINED_GLASS_PANE, Blocks.GRAY_STAINED_GLASS, Blocks.GRAY_STAINED_GLASS_PANE, Blocks.LIGHT_GRAY_STAINED_GLASS, Blocks.LIGHT_GRAY_STAINED_GLASS_PANE, Blocks.CYAN_STAINED_GLASS, Blocks.CYAN_STAINED_GLASS_PANE, Blocks.PURPLE_STAINED_GLASS, Blocks.PURPLE_STAINED_GLASS_PANE, Blocks.BLUE_STAINED_GLASS, Blocks.BLUE_STAINED_GLASS_PANE, Blocks.BROWN_STAINED_GLASS, Blocks.BROWN_STAINED_GLASS_PANE, Blocks.GREEN_STAINED_GLASS, Blocks.GREEN_STAINED_GLASS_PANE, Blocks.RED_STAINED_GLASS, Blocks.RED_STAINED_GLASS_PANE, Blocks.BLACK_STAINED_GLASS, Blocks.BLACK_STAINED_GLASS_PANE);

    private BlockState blockState = Blocks.STONE.defaultBlockState();
    private float sourceHardness = 1.5F;
    private CompoundTag blockEntityData;

    private final List<StructurePart> attachedParts = new ArrayList<>();
    private boolean shatterOnImpact;

    private UUID ownerId;
    private int heldHand = 0;
    private int pullTicks = PULL_SHAKE_TICKS;
    private int settleTicks;
    private int freeTicks;

    private int lastHitEntityId = -1;
    private int hitCooldown;
    private int noteSequence;
    private int noteImpactCooldown;

    private float visualYaw;
    private float visualYawO;
    private float visualPitch = 16.0F;
    private float visualPitchO = 16.0F;
    private float visualRoll = 10.0F;
    private float visualRollO = 10.0F;

    private boolean settledPoseChosen;
    private float settledYaw;
    private float settledPitch;
    private float settledRoll;

    public EntropyPhysicsBlockEntity(EntityType<? extends EntropyPhysicsBlockEntity> type, Level level) {
        super(type, level);
    }

    public void initializeFromBlock(BlockState state, float hardness, CompoundTag blockEntityData, ServerPlayer owner, InteractionHand hand, BlockPos sourcePos, boolean nephritisCoated) {
        this.blockState = state;
        this.sourceHardness = Math.max(0.0F, hardness);
        this.blockEntityData = blockEntityData == null ? null : blockEntityData.copy();
        this.ownerId = owner.getUUID();
        this.heldHand = hand == InteractionHand.MAIN_HAND ? 0 : 1;
        this.pullTicks = PULL_SHAKE_TICKS;
        this.setMode(MODE_PULLING);
        this.setNephritisCoated(nephritisCoated);
        this.attachedParts.clear();
        syncStructureData();

        this.setPos(sourcePos.getX() + 0.5D, sourcePos.getY(), sourcePos.getZ() + 0.5D);
        this.setDeltaMovement(Vec3.ZERO);
        this.noPhysics = true;
        this.setNoGravity(true);
    }

    public void initializeFromBlock(BlockState state, float hardness, CompoundTag blockEntityData, ServerPlayer owner, InteractionHand hand, BlockPos sourcePos) {
        initializeFromBlock(state, hardness, blockEntityData, owner, hand, sourcePos, false);
    }

    private void initializeLooseFragment(BlockState state, CompoundTag data, Vec3 position, Vec3 velocity) {
        this.blockState = state;
        this.blockEntityData = data == null ? null : data.copy();
        this.sourceHardness = Math.max(0.0F, state.getDestroySpeed(this.level(), BlockPos.containing(position)));
        this.ownerId = null;
        this.heldHand = 0;
        this.pullTicks = 0;
        this.settleTicks = 0;
        this.freeTicks = 0;
        this.shatterOnImpact = true;
        this.setNephritisCoated(false);
        this.setMode(MODE_THROWN);
        this.setPos(position.x, position.y, position.z);
        this.setDeltaMovement(velocity);
        this.noPhysics = false;
        this.setNoGravity(true);
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(MODE, MODE_PULLING);
        this.entityData.define(NEPHRITIS_COATED, false);
        this.entityData.define(STRUCTURE_DATA, new CompoundTag());
    }

    public int getMode() {
        return this.entityData.get(MODE);
    }

    private void setMode(int mode) {
        this.entityData.set(MODE, mode);
    }

    public boolean isNephritisCoated() {
        return this.entityData.get(NEPHRITIS_COATED);
    }

    private void setNephritisCoated(boolean coated) {
        this.entityData.set(NEPHRITIS_COATED, coated);
    }

    public boolean isSettledPhysical() {
        return getMode() == MODE_SETTLED;
    }

    public List<StructurePart> getAttachedParts() {
        return Collections.unmodifiableList(this.attachedParts);
    }

    public BlockState getCarriedBlockState() {
        return this.blockState;
    }

    public float getVisualYaw(float partialTick) {
        return Mth.rotLerp(partialTick, this.visualYawO, this.visualYaw);
    }

    public float getVisualPitch(float partialTick) {
        return Mth.lerp(partialTick, this.visualPitchO, this.visualPitch);
    }

    public float getVisualRoll(float partialTick) {
        return Mth.lerp(partialTick, this.visualRollO, this.visualRoll);
    }

    public double getPullWiggle(float partialTick) {
        if (getMode() != MODE_PULLING) {
            return 0.0D;
        }

        return Math.sin((this.tickCount + partialTick) * 3.8D) * 0.045D;
    }

    @Override
    public void tick() {
        super.tick();
        this.fallDistance = 0.0F;

        tickVisualRotation();

        if (this.level().isClientSide) {
            return;
        }

        if (!(this.level() instanceof ServerLevel level)) {
            return;
        }

        if (this.hitCooldown > 0) {
            this.hitCooldown--;
        }
        if (this.noteImpactCooldown > 0) {
            this.noteImpactCooldown--;
        }

        switch (getMode()) {
            case MODE_PULLING -> tickPulling(level);
            case MODE_HELD -> tickHeld(level);
            case MODE_DROPPED, MODE_THROWN -> tickFreePhysics(level);
            case MODE_SETTLED -> tickSettled();
            default -> {
            }
        }

        if (isNephritisCoated() && this.random.nextInt(68) == 0) {
            MeltedNephritisEffects.spawnIdle(level, getRandomStructureSurfacePosition());
        }
    }

    private void tickSettled() {
        this.noPhysics = false;
        this.setNoGravity(true);
        this.setDeltaMovement(Vec3.ZERO);
        this.setOnGround(true);
    }

    private void tickPulling(ServerLevel level) {
        ServerPlayer owner = resolveOwner(level);
        if (!canStillBeHeldBy(owner)) {
            releaseFromPlayer();
            clearOwnerReference(level);
            return;
        }

        this.noPhysics = true;
        this.setNoGravity(true);
        this.setDeltaMovement(Vec3.ZERO);

        this.pullTicks--;

        if (this.pullTicks <= 0) {
            this.setMode(MODE_HELD);
            level.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 0.75F, 1.35F);

            level.sendParticles(Oasiso.ENTROPY_GRAVITY_TRAIL.get(), this.getX(), this.getY() + 0.5D, this.getZ(), 10, 0.38D, 0.38D, 0.38D, 0.0D);
        }
    }

    private void tickHeld(ServerLevel level) {
        ServerPlayer owner = resolveOwner(level);
        if (!canStillBeHeldBy(owner)) {
            releaseFromPlayer();
            clearOwnerReference(level);
            return;
        }

        this.noPhysics = true;
        this.setNoGravity(true);

        Vec3 target = getHeldTarget(owner);
        Vec3 difference = target.subtract(this.position());

        double follow = difference.lengthSqr() > 16.0D ? 0.62D : HOLD_FOLLOW_FACTOR;

        Vec3 step = difference.scale(follow);
        this.setDeltaMovement(step);
        this.setPos(this.getX() + step.x, this.getY() + step.y, this.getZ() + step.z);
    }

    private void tickFreePhysics(ServerLevel level) {
        this.noPhysics = false;
        this.setNoGravity(true);
        this.freeTicks++;

        Vec3 start = this.position();
        Vec3 velocity = this.getDeltaMovement();

        double gravity = this.isInWater() ? GRAVITY * 0.28D : GRAVITY;
        double drag = this.isInWater() ? 0.84D : AIR_DRAG;

        velocity = new Vec3(velocity.x * drag, (velocity.y - gravity) * drag, velocity.z * drag);

        if (getMode() == MODE_THROWN && velocity.lengthSqr() > 0.015D) {
            Vec3 end = start.add(velocity);
            BlockHitResult blockHit = level.clip(new ClipContext(start, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));

            Vec3 entityTraceEnd = blockHit.getType() == HitResult.Type.MISS ? end : blockHit.getLocation();

            EntityHitResult entityHit = net.minecraft.world.entity.projectile.ProjectileUtil.getEntityHitResult(level, this, start, entityTraceEnd, this.getBoundingBox().expandTowards(velocity).inflate(0.28D), this::canHitEntity);

            if (entityHit != null) {
                if (isTntBlock()) {
                    explodeTnt(level);
                    return;
                }

                velocity = hitLivingEntity(level, entityHit.getEntity(), velocity);
                if (this.isRemoved()) {
                    return;
                }
            }
        }

        boolean structureHitX = Math.abs(velocity.x) > 1.0E-6D && !canStructureMove(level, new Vec3(velocity.x, 0.0D, 0.0D));
        boolean structureHitY = Math.abs(velocity.y) > 1.0E-6D && !canStructureMove(level, new Vec3(0.0D, velocity.y, 0.0D));
        boolean structureHitZ = Math.abs(velocity.z) > 1.0E-6D && !canStructureMove(level, new Vec3(0.0D, 0.0D, velocity.z));

        Vec3 moveVelocity = new Vec3(structureHitX ? 0.0D : velocity.x, structureHitY ? 0.0D : velocity.y, structureHitZ ? 0.0D : velocity.z);

        this.setDeltaMovement(moveVelocity);
        this.move(MoverType.SELF, moveVelocity);

        Vec3 actualMovement = this.position().subtract(start);
        boolean hitX = structureHitX || Math.abs(actualMovement.x - moveVelocity.x) > 1.0E-4D;
        boolean hitY = structureHitY || Math.abs(actualMovement.y - moveVelocity.y) > 1.0E-4D;
        boolean hitZ = structureHitZ || Math.abs(actualMovement.z - moveVelocity.z) > 1.0E-4D;

        boolean collided = hitX || hitY || hitZ;

        if (isMagmaBlock()) {
            igniteTouchedBlocks(level);
        }

        if (collided && this.shatterOnImpact && this.freeTicks > 2) {
            shatterLooseFragment(level);
            return;
        }

        if (collided && getMode() == MODE_THROWN) {
            if (isTntBlock()) {
                explodeTnt(level);
                return;
            }

            if (isFragileGlassBlock()) {
                shatterGlass(level);
                return;
            }
        }

        if (collided && isHoneyBlock()) {
            this.setDeltaMovement(Vec3.ZERO);
            this.settleTicks = SETTLE_REQUIRED_TICKS;
            tryPlaceBack(level);
            return;
        }

        double bounce = getBounceCoefficient();
        if (getMode() != MODE_THROWN) {
            bounce *= DROPPED_BOUNCE_MULTIPLIER;
        }

        double nextX = hitX ? -velocity.x * bounce : velocity.x;
        double nextY = hitY ? -velocity.y * bounce : velocity.y;
        double nextZ = hitZ ? -velocity.z * bounce : velocity.z;

        boolean structureGrounded = this.onGround() || (!this.attachedParts.isEmpty() && hitY && velocity.y < 0.0D);

        if (structureGrounded) {
            double friction = getGroundFriction();
            nextX *= friction;
            nextZ *= friction;

            if (Math.abs(nextY) < 0.085D) {
                nextY = 0.0D;
            }
        }

        if (collided && velocity.lengthSqr() > 0.08D) {
            if (isNoteBlock()) {
                playNextNote(level);
            } else {
                level.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.STONE_HIT, SoundSource.BLOCKS, 0.55F, 0.85F + this.random.nextFloat() * 0.25F);
            }
        }

        this.setDeltaMovement(nextX, nextY, nextZ);

        double speedSqr = this.getDeltaMovement().lengthSqr();
        double settleThreshold = isIceBlock() ? SETTLE_SPEED_SQR * 0.28D : SETTLE_SPEED_SQR;
        if (structureGrounded && speedSqr <= settleThreshold) {
            this.settleTicks++;
        } else {
            this.settleTicks = 0;
        }

        if (this.settleTicks >= SETTLE_REQUIRED_TICKS || this.freeTicks >= MAX_FREE_TICKS) {
            tryPlaceBack(level);
        }
    }

    private boolean canStructureMove(ServerLevel level, Vec3 delta) {
        if (this.attachedParts.isEmpty()) {
            return true;
        }

        Vec3 rootCenter = this.position().add(0.0D, 0.5D, 0.0D);

        for (StructurePart part : this.attachedParts) {
            Vec3 rotatedOffset = rotateLocal(new Vec3(part.offset.getX(), part.offset.getY(), part.offset.getZ()));

            Vec3 center = rootCenter.add(rotatedOffset).add(delta);
            AABB partBox = new AABB(center.x - 0.49D, center.y - 0.49D, center.z - 0.49D, center.x + 0.49D, center.y + 0.49D, center.z + 0.49D);

            if (!level.noCollision(this, partBox)) {
                return false;
            }
        }

        return true;
    }

    private Vec3 hitLivingEntity(ServerLevel level, Entity target, Vec3 velocity) {
        if (this.hitCooldown > 0 && target.getId() == this.lastHitEntityId) {
            return velocity;
        }

        float damage = getImpactDamage();
        boolean hurt = target.hurt(this.damageSources().fallingBlock(this), damage);

        if (hurt) {
            this.lastHitEntityId = target.getId();
            this.hitCooldown = 6;

            level.playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.ANVIL_LAND, SoundSource.PLAYERS, 0.55F, 1.35F);

            level.sendParticles(Oasiso.ENTROPY_GRAVITY_TRAIL.get(), target.getX(), target.getY() + target.getBbHeight() * 0.5D, target.getZ(), 7, 0.22D, 0.22D, 0.22D, 0.0D);
        }

        return velocity.scale(0.72D);
    }

    private boolean canHitEntity(Entity entity) {
        if (!(entity instanceof LivingEntity) || !entity.isAlive() || entity == this) {
            return false;
        }
        if (entity instanceof Player) {
            return false;
        }

        ServerPlayer owner = this.level() instanceof ServerLevel level ? resolveOwner(level) : null;

        return owner == null || entity != owner;
    }

    public float getImpactDamage() {
        float structureMassBonus = 1.0F + Math.min(0.45F, this.attachedParts.size() * 0.025F);
        return Mth.clamp((5.0F + this.sourceHardness * 1.5F) * structureMassBonus, 5.0F, 36.0F);
    }

    private double getHardnessFactor() {
        double capped = Mth.clamp(this.sourceHardness, 0.0F, 50.0F);
        return Math.log1p(capped) / Math.log1p(50.0D);
    }

    private double getThrowSpeed() {
        double base = Mth.lerp(getHardnessFactor(), THROW_SPEED_SOFT, THROW_SPEED_HARD);
        if (this.attachedParts.isEmpty()) {
            return base;
        }

        double massFactor = Mth.clamp(1.0D / Math.sqrt(1.0D + this.attachedParts.size() * 0.24D), 0.58D, 1.0D);
        return base * massFactor;
    }

    private double getBounceCoefficient() {
        if (isSlimeBlock()) {
            return SLIME_BOUNCE;
        }

        double hardnessBounce = Mth.lerp(getHardnessFactor(), SOFT_BLOCK_BOUNCE, HARD_BLOCK_BOUNCE);
        if (isLogBlock()) {
            return Math.max(hardnessBounce, LOG_MIN_BOUNCE);
        }

        double structureFactor = Mth.clamp(1.0D - this.attachedParts.size() * 0.012D, 0.78D, 1.0D);
        return hardnessBounce * structureFactor;
    }

    private double getGroundFriction() {
        if (isIceBlock()) {
            return ICE_GROUND_FRICTION;
        }

        if (isLogBlock()) {
            return LOG_GROUND_FRICTION;
        }

        return NORMAL_GROUND_FRICTION;
    }

    private boolean isSlimeBlock() {
        return this.blockState.is(Blocks.SLIME_BLOCK);
    }

    private boolean isHoneyBlock() {
        return this.blockState.is(Blocks.HONEY_BLOCK);
    }

    private boolean isLogBlock() {
        return this.blockState.is(BlockTags.LOGS);
    }

    private boolean isIceBlock() {
        return this.blockState.is(Blocks.ICE) || this.blockState.is(Blocks.PACKED_ICE) || this.blockState.is(Blocks.BLUE_ICE) || this.blockState.is(Blocks.FROSTED_ICE);
    }

    private boolean isTntBlock() {
        return this.blockState.is(Blocks.TNT);
    }

    private boolean isMagmaBlock() {
        return this.blockState.is(Blocks.MAGMA_BLOCK);
    }

    private boolean isFragileGlassBlock() {
        return FRAGILE_GLASS_BLOCKS.contains(this.blockState.getBlock());
    }

    private boolean isNoteBlock() {
        return this.blockState.is(Blocks.NOTE_BLOCK);
    }

    private void explodeTnt(ServerLevel level) {
        if (this.isRemoved()) {
            return;
        }

        clearOwnerReference(level);

        level.explode(this, this.getX(), this.getY() + 0.45D, this.getZ(), TNT_EXPLOSION_POWER, Level.ExplosionInteraction.TNT);

        this.discard();
    }

    private void shatterGlass(ServerLevel level) {
        if (this.isRemoved()) {
            return;
        }

        level.levelEvent(2001, BlockPos.containing(this.getX(), this.getY() + 0.45D, this.getZ()), Block.getId(this.blockState));

        clearOwnerReference(level);
        this.discard();
    }

    private void shatterLooseFragment(ServerLevel level) {
        if (this.isRemoved()) {
            return;
        }

        BlockPos pos = BlockPos.containing(this.getX(), this.getY() + 0.35D, this.getZ());

        level.levelEvent(2001, pos, Block.getId(this.blockState));

        ItemStack dropped = new ItemStack(this.blockState.getBlock());
        if (!dropped.isEmpty()) {
            Block.popResource(level, pos, dropped);
        }

        this.discard();
    }

    private void igniteTouchedBlocks(ServerLevel level) {
        AABB touchBox = this.getBoundingBox().inflate(0.055D);

        int minX = Mth.floor(touchBox.minX);
        int minY = Mth.floor(touchBox.minY);
        int minZ = Mth.floor(touchBox.minZ);
        int maxX = Mth.floor(touchBox.maxX);
        int maxY = Mth.floor(touchBox.maxY);
        int maxZ = Mth.floor(touchBox.maxZ);

        for (BlockPos touched : BlockPos.betweenClosed(minX, minY, minZ, maxX, maxY, maxZ)) {
            BlockState touchedState = level.getBlockState(touched);
            if (touchedState.isAir()) {
                continue;
            }

            for (Direction direction : Direction.values()) {
                BlockPos firePos = touched.relative(direction);

                if (!level.getBlockState(firePos).canBeReplaced() || !level.getFluidState(firePos).isEmpty()) {
                    continue;
                }

                BlockState fireState = BaseFireBlock.getState(level, firePos);
                if (!fireState.canSurvive(level, firePos)) {
                    continue;
                }

                level.setBlock(firePos, fireState, Block.UPDATE_ALL);
            }
        }
    }

    private void playNextNote(ServerLevel level) {
        if (this.noteImpactCooldown > 0) {
            return;
        }

        int baseNote = this.blockState.hasProperty(NoteBlock.NOTE) ? this.blockState.getValue(NoteBlock.NOTE) : 0;

        int note = Math.floorMod(baseNote + this.noteSequence, 25);
        float pitch = (float) Math.pow(2.0D, (note - 12) / 12.0D);

        level.playSound(null, this.getX(), this.getY() + 0.5D, this.getZ(), this.blockState.getValue(NoteBlock.INSTRUMENT).getSoundEvent().value(), SoundSource.RECORDS, 1.0F, pitch);

        this.noteSequence = (this.noteSequence + 1) % 25;
        this.noteImpactCooldown = NOTE_IMPACT_COOLDOWN_TICKS;
    }

    public void throwFrom(ServerPlayer player) {
        if (this.level().isClientSide || (getMode() != MODE_HELD && getMode() != MODE_PULLING)) {
            return;
        }

        this.setMode(MODE_THROWN);
        this.noPhysics = false;
        this.setNoGravity(true);
        this.pullTicks = 0;
        this.settleTicks = 0;
        this.freeTicks = 0;
        this.settledPoseChosen = false;

        Vec3 look = player.getLookAngle().normalize();
        Vec3 velocity = look.scale(getThrowSpeed()).add(player.getDeltaMovement().scale(0.35D)).add(0.0D, THROW_UP_BONUS, 0.0D);

        this.setDeltaMovement(velocity);

        this.level().playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.TRIDENT_THROW, SoundSource.PLAYERS, 0.8F, 1.15F);
    }

    public void releaseFromPlayer() {
        if (this.level().isClientSide) {
            return;
        }

        if (getMode() != MODE_HELD && getMode() != MODE_PULLING) {
            return;
        }

        ServerPlayer owner = this.level() instanceof ServerLevel level ? resolveOwner(level) : null;

        this.setMode(MODE_DROPPED);
        this.noPhysics = false;
        this.setNoGravity(true);
        this.pullTicks = 0;
        this.settleTicks = 0;
        this.freeTicks = 0;
        this.settledPoseChosen = false;

        Vec3 inherited = owner == null ? Vec3.ZERO : owner.getDeltaMovement().scale(0.35D);

        this.setDeltaMovement(inherited);

        if (this.level() instanceof ServerLevel level) {
            level.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.PISTON_CONTRACT, SoundSource.PLAYERS, 0.55F, 1.45F);
        }
    }

    public InteractionResult pickupSettled(ServerPlayer player, InteractionHand hand, ItemStack glove) {
        if (!isNephritisCoated() || getMode() != MODE_SETTLED) {
            return InteractionResult.PASS;
        }

        if (EntropyChestplateGloveItem.hasHeldBlock(glove)) {
            return InteractionResult.FAIL;
        }

        this.ownerId = player.getUUID();
        this.heldHand = hand == InteractionHand.MAIN_HAND ? 0 : 1;
        this.pullTicks = 0;
        this.settleTicks = 0;
        this.freeTicks = 0;
        this.settledPoseChosen = false;

        this.setMode(MODE_HELD);
        this.noPhysics = true;
        this.setNoGravity(true);
        this.setDeltaMovement(Vec3.ZERO);

        EntropyChestplateGloveItem.bindHeldBlock(glove, this.getUUID());
        player.getInventory().setChanged();
        player.containerMenu.broadcastChanges();

        this.level().playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.PISTON_EXTEND, SoundSource.PLAYERS, 0.60F, 1.45F);

        return InteractionResult.CONSUME;
    }
    public InteractionResult bondCurrentStructure(ServerLevel level, ServerPlayer player, ItemStack meltedNephritis) {
        if (!isNephritisCoated() || getMode() != MODE_SETTLED) {
            return InteractionResult.PASS;
        }

        boolean changed = false;

        for (StructurePart part : this.attachedParts) {
            if (!part.bonded) {
                part.bonded = true;
                changed = true;
            }
        }

        MeltedNephritisEffects.spawnBurst(level, this.position().add(0.0D, 0.5D, 0.0D));
        level.playSound(null, this.getX(), this.getY() + 0.5D, this.getZ(), SoundEvents.HONEYCOMB_WAX_ON, SoundSource.PLAYERS, 0.95F, 1.0F);
        if (changed) {
            if (!player.getAbilities().instabuild) {
                meltedNephritis.shrink(1);
            }

            syncStructureData();
            player.getInventory().setChanged();
            player.containerMenu.broadcastChanges();
        }

        return InteractionResult.CONSUME;
    }

    public InteractionResult removeNephritis(ServerLevel level, ServerPlayer player, InteractionHand hand, ItemStack axe) {
        if (!isNephritisCoated() || getMode() != MODE_SETTLED) {
            return InteractionResult.PASS;
        }

        if (!materializeBondedAndReleaseLoose(level)) {
            return InteractionResult.FAIL;
        }

        MeltedNephritisEffects.spawnBurst(level, this.position().add(0.0D, 0.5D, 0.0D));
        level.playSound(null, this.getX(), this.getY() + 0.5D, this.getZ(), SoundEvents.AXE_WAX_OFF, SoundSource.PLAYERS, 1.0F, 1.0F);

        axe.hurtAndBreak(1, player, broken -> broken.broadcastBreakEvent(hand));

        return InteractionResult.CONSUME;
    }

    public InteractionResult attachBlock(ServerLevel level, ServerPlayer player, InteractionHand hand, ItemStack heldStack, BlockItem blockItem, Vec3 localHit) {
        if (!isNephritisCoated() || getMode() != MODE_SETTLED) {
            return InteractionResult.PASS;
        }

        if (this.attachedParts.size() >= MAX_ATTACHED_BLOCKS) {
            return InteractionResult.FAIL;
        }

        BlockState state = blockItem.getBlock().defaultBlockState();

        if (!EntropyChestplateGloveItem.canAttachBlockState(level, this.blockPosition(), state)) {
            return InteractionResult.PASS;
        }

        Vec3 localRootCentered = new Vec3(localHit.x, localHit.y - 0.5D, localHit.z);

        Vec3 structureHit = inverseRotateWorld(localRootCentered);
        BlockPos anchor = findNearestStructureOffset(structureHit);

        Vec3 anchorCenter = new Vec3(anchor.getX(), anchor.getY(), anchor.getZ());

        Direction face = inferClickedFace(structureHit.subtract(anchorCenter));
        BlockPos targetOffset = anchor.relative(face);

        if (Math.abs(targetOffset.getX()) > MAX_STRUCTURE_RADIUS || Math.abs(targetOffset.getY()) > MAX_STRUCTURE_RADIUS || Math.abs(targetOffset.getZ()) > MAX_STRUCTURE_RADIUS) {
            return InteractionResult.FAIL;
        }

        if (isOffsetOccupied(targetOffset)) {
            return InteractionResult.FAIL;
        }

        if (!canAttachAtWorldPosition(level, targetOffset)) {
            return InteractionResult.FAIL;
        }

        state = prepareAttachedState(state, player, face);

        this.attachedParts.add(new StructurePart(targetOffset, state, null, false));

        syncStructureData();

        if (!player.getAbilities().instabuild) {
            heldStack.shrink(1);
        }

        level.playSound(null, this.getX(), this.getY() + 0.5D, this.getZ(), state.getSoundType().getPlaceSound(), SoundSource.BLOCKS, 0.75F, 1.0F);

        player.swing(hand, true);
        player.getInventory().setChanged();
        player.containerMenu.broadcastChanges();

        return InteractionResult.CONSUME;
    }

    private boolean canAttachAtWorldPosition(ServerLevel level, BlockPos localOffset) {
        Vec3 center = this.position().add(0.0D, 0.5D, 0.0D).add(rotateLocal(new Vec3(localOffset.getX(), localOffset.getY(), localOffset.getZ())));

        AABB cube = new AABB(center.x - 0.49D, center.y - 0.49D, center.z - 0.49D, center.x + 0.49D, center.y + 0.49D, center.z + 0.49D);

        return level.noCollision(this, cube);
    }

    private BlockPos findNearestStructureOffset(Vec3 structureHit) {
        BlockPos best = BlockPos.ZERO;
        double bestDistance = structureHit.distanceToSqr(Vec3.ZERO);

        for (StructurePart part : this.attachedParts) {
            Vec3 center = new Vec3(part.offset.getX(), part.offset.getY(), part.offset.getZ());

            double distance = structureHit.distanceToSqr(center);
            if (distance < bestDistance) {
                bestDistance = distance;
                best = part.offset;
            }
        }

        return best;
    }

    private static Direction inferClickedFace(Vec3 relative) {
        double ax = Math.abs(relative.x);
        double ay = Math.abs(relative.y);
        double az = Math.abs(relative.z);

        if (ay >= ax && ay >= az) {
            return relative.y >= 0.0D ? Direction.UP : Direction.DOWN;
        }

        if (ax >= az) {
            return relative.x >= 0.0D ? Direction.EAST : Direction.WEST;
        }

        return relative.z >= 0.0D ? Direction.SOUTH : Direction.NORTH;
    }

    private static BlockState prepareAttachedState(BlockState state, Player player, Direction clickedFace) {
        if (state.hasProperty(BlockStateProperties.WATERLOGGED)) {
            state = state.setValue(BlockStateProperties.WATERLOGGED, false);
        }

        if (state.hasProperty(BlockStateProperties.HORIZONTAL_FACING)) {
            state = state.setValue(BlockStateProperties.HORIZONTAL_FACING, player.getDirection().getOpposite());
        } else if (state.hasProperty(BlockStateProperties.FACING)) {
            state = state.setValue(BlockStateProperties.FACING, clickedFace);
        }

        if (state.hasProperty(BlockStateProperties.AXIS)) {
            state = state.setValue(BlockStateProperties.AXIS, clickedFace.getAxis());
        }

        return state;
    }

    private boolean isOffsetOccupied(BlockPos offset) {
        if (offset.equals(BlockPos.ZERO)) {
            return true;
        }

        for (StructurePart part : this.attachedParts) {
            if (part.offset.equals(offset)) {
                return true;
            }
        }

        return false;
    }

    private boolean materializeBondedAndReleaseLoose(ServerLevel level) {
        List<StructurePart> bonded = new ArrayList<>();
        List<StructurePart> loose = new ArrayList<>();

        for (StructurePart part : this.attachedParts) {
            (part.bonded ? bonded : loose).add(part);
        }

        BlockPos base = findStructurePlacementBase(level, bonded);
        if (base == null) {
            return false;
        }

        BlockState placedRootState = rotateStateForPlacement(this.blockState);
        placeSinglePart(level, base, placedRootState, this.blockEntityData);

        for (StructurePart part : bonded) {
            BlockPos rotatedOffset = rotateOffsetForPlacement(part.offset);
            placeSinglePart(level, base.offset(rotatedOffset), rotateStateForPlacement(part.state), part.blockEntityData);
        }

        Vec3 rootCenter = this.position().add(0.0D, 0.5D, 0.0D);

        for (StructurePart part : loose) {
            Vec3 rotatedOffset = rotateLocal(new Vec3(part.offset.getX(), part.offset.getY(), part.offset.getZ()));

            Vec3 fragmentPosition = rootCenter.add(rotatedOffset).add(0.0D, -0.5D, 0.0D);

            Vec3 outward = new Vec3(rotatedOffset.x, 0.0D, rotatedOffset.z);
            if (outward.lengthSqr() < 1.0E-4D) {
                outward = new Vec3(this.random.nextDouble() - 0.5D, 0.0D, this.random.nextDouble() - 0.5D);
            }

            outward = outward.normalize();

            Vec3 velocity = outward.scale(0.28D + this.random.nextDouble() * 0.20D).add(0.0D, 0.32D + this.random.nextDouble() * 0.16D, 0.0D);
            EntropyPhysicsBlockEntity fragment = new EntropyPhysicsBlockEntity(ModEntities.ENTROPY_PHYSICS_BLOCK.get(), level);
            fragment.initializeLooseFragment(part.state, part.blockEntityData, fragmentPosition, velocity);

            level.addFreshEntity(fragment);
        }

        clearOwnerReference(level);
        this.discard();
        return true;
    }

    private BlockPos findStructurePlacementBase(ServerLevel level, List<StructurePart> bonded) {
        BlockPos origin = BlockPos.containing(this.getX(), this.getY() + 0.05D, this.getZ());

        for (int dy = 0; dy <= 5; dy++) {
            BlockPos center = origin.above(dy);

            BlockPos[] candidates = new BlockPos[]{center, center.north(), center.south(), center.east(), center.west()};

            for (BlockPos candidate : candidates) {
                if (canMaterializeStructureAt(level, candidate, bonded)) {
                    return candidate;
                }
            }
        }

        return null;
    }

    private boolean canMaterializeStructureAt(ServerLevel level, BlockPos rootPos, List<StructurePart> bonded) {
        BlockState rootState = rotateStateForPlacement(this.blockState);
        if (!canReplaceForStructure(level, rootPos, rootState)) {
            return false;
        }

        for (StructurePart part : bonded) {
            BlockPos offset = rotateOffsetForPlacement(part.offset);
            BlockPos pos = rootPos.offset(offset);
            BlockState state = rotateStateForPlacement(part.state);

            if (!canReplaceForStructure(level, pos, state)) {
                return false;
            }
        }

        return true;
    }

    private boolean canReplaceForStructure(ServerLevel level, BlockPos pos, BlockState state) {
        BlockState replaced = level.getBlockState(pos);

        if (!replaced.canBeReplaced() || !level.getFluidState(pos).isEmpty()) {
            return false;
        }

        if (!state.canSurvive(level, pos)) {
            return false;
        }

        AABB placeBox = new AABB(pos).deflate(0.035D);
        return level.noCollision(this, placeBox);
    }

    private static void placeSinglePart(ServerLevel level, BlockPos pos, BlockState state, CompoundTag data) {
        level.setBlock(pos, state, Block.UPDATE_ALL | Block.UPDATE_SUPPRESS_DROPS);

        restoreBlockEntityData(level, pos, state, data);
    }

    private static void restoreBlockEntityData(ServerLevel level, BlockPos pos, BlockState state, CompoundTag sourceData) {
        if (sourceData == null) {
            return;
        }

        BlockEntity restored = level.getBlockEntity(pos);
        if (restored == null) {
            return;
        }

        CompoundTag data = sourceData.copy();
        data.putInt("x", pos.getX());
        data.putInt("y", pos.getY());
        data.putInt("z", pos.getZ());

        restored.load(data);
        restored.setChanged();
        level.sendBlockUpdated(pos, state, state, Block.UPDATE_ALL);
    }

    private BlockPos rotateOffsetForPlacement(BlockPos localOffset) {
        Vec3 rotated = rotateLocalUsingSettledPose(new Vec3(localOffset.getX(), localOffset.getY(), localOffset.getZ()));

        return new BlockPos((int) Math.round(rotated.x), (int) Math.round(rotated.y), (int) Math.round(rotated.z));
    }

    private BlockState rotateStateForPlacement(BlockState state) {
        if (state.hasProperty(BlockStateProperties.FACING)) {
            Direction old = state.getValue(BlockStateProperties.FACING);
            state = state.setValue(BlockStateProperties.FACING, rotateDirectionForPlacement(old));
        }

        if (state.hasProperty(BlockStateProperties.HORIZONTAL_FACING)) {
            Direction old = state.getValue(BlockStateProperties.HORIZONTAL_FACING);
            Direction rotated = rotateDirectionForPlacement(old);

            if (rotated.getAxis() != Direction.Axis.Y) {
                state = state.setValue(BlockStateProperties.HORIZONTAL_FACING, rotated);
            }
        }

        if (state.hasProperty(BlockStateProperties.AXIS)) {
            Direction.Axis oldAxis = state.getValue(BlockStateProperties.AXIS);
            Direction basis = switch (oldAxis) {
                case X -> Direction.EAST;
                case Y -> Direction.UP;
                case Z -> Direction.SOUTH;
            };

            state = state.setValue(BlockStateProperties.AXIS, rotateDirectionForPlacement(basis).getAxis());
        }

        return state;
    }

    private Direction rotateDirectionForPlacement(Direction direction) {
        Vec3 normal = new Vec3(direction.getStepX(), direction.getStepY(), direction.getStepZ());

        Vec3 rotated = rotateLocalUsingSettledPose(normal);
        return Direction.getNearest((float) rotated.x, (float) rotated.y, (float) rotated.z);
    }

    private Vec3 rotateLocal(Vec3 vector) {
        return rotateEuler(vector, this.visualYaw, this.visualPitch, this.visualRoll);
    }

    private Vec3 inverseRotateWorld(Vec3 vector) {
        Vec3 v = rotateY(vector, -this.visualYaw);
        v = rotateX(v, -this.visualPitch);
        return rotateZ(v, -this.visualRoll);
    }

    private Vec3 rotateLocalUsingSettledPose(Vec3 vector) {
        float yaw = this.settledPoseChosen ? this.settledYaw : snapToQuarterTurn(this.visualYaw);
        float pitch = this.settledPoseChosen ? this.settledPitch : snapToQuarterTurn(this.visualPitch);
        float roll = this.settledPoseChosen ? this.settledRoll : snapToQuarterTurn(this.visualRoll);
        return rotateEuler(vector, yaw, pitch, roll);
    }

    private static Vec3 rotateEuler(Vec3 vector, float yawDeg, float pitchDeg, float rollDeg) {
        Vec3 v = rotateZ(vector, rollDeg);
        v = rotateX(v, pitchDeg);
        return rotateY(v, yawDeg);
    }

    private static Vec3 rotateX(Vec3 v, float degrees) {
        double r = Math.toRadians(degrees);
        double c = Math.cos(r);
        double s = Math.sin(r);
        return new Vec3(v.x, v.y * c - v.z * s, v.y * s + v.z * c);
    }

    private static Vec3 rotateY(Vec3 v, float degrees) {
        double r = Math.toRadians(degrees);
        double c = Math.cos(r);
        double s = Math.sin(r);
        return new Vec3(v.x * c + v.z * s, v.y, -v.x * s + v.z * c);
    }

    private static Vec3 rotateZ(Vec3 v, float degrees) {
        double r = Math.toRadians(degrees);
        double c = Math.cos(r);
        double s = Math.sin(r);
        return new Vec3(v.x * c - v.y * s, v.x * s + v.y * c, v.z);
    }

    private Vec3 getHeldTarget(ServerPlayer player) {
        Vec3 look = player.getLookAngle().normalize();

        Vec3 horizontalForward = new Vec3(look.x, 0.0D, look.z);
        if (horizontalForward.lengthSqr() < 1.0E-5D) {
            double yaw = Math.toRadians(player.getYRot());
            horizontalForward = new Vec3(-Math.sin(yaw), 0.0D, Math.cos(yaw));
        } else {
            horizontalForward = horizontalForward.normalize();
        }

        Vec3 right = new Vec3(-horizontalForward.z, 0.0D, horizontalForward.x);

        HumanoidArm physicalArm;
        if (this.heldHand == 0) {
            physicalArm = player.getMainArm();
        } else {
            physicalArm = player.getMainArm() == HumanoidArm.RIGHT ? HumanoidArm.LEFT : HumanoidArm.RIGHT;
        }

        double side = physicalArm == HumanoidArm.RIGHT ? 1.0D : -1.0D;

        return player.getEyePosition().add(look.scale(HOLD_FORWARD)).add(right.scale(HOLD_SIDE * side)).add(0.0D, -HOLD_DOWN, 0.0D);
    }

    private ServerPlayer resolveOwner(ServerLevel level) {
        if (this.ownerId == null) {
            return null;
        }

        ServerPlayer player = level.getServer().getPlayerList().getPlayer(this.ownerId);
        if (player == null || player.serverLevel() != level || !player.isAlive()) {
            return null;
        }

        return player;
    }

    private boolean canStillBeHeldBy(ServerPlayer player) {
        if (player == null || player.isSpectator()) {
            return false;
        }

        return EntropyChestplateGloveItem.isBoundTo(player.getMainHandItem(), this.getUUID()) || EntropyChestplateGloveItem.isBoundTo(player.getOffhandItem(), this.getUUID());
    }

    private void tryPlaceBack(ServerLevel level) {
        if (this.shatterOnImpact) {
            shatterLooseFragment(level);
            return;
        }

        if (isNephritisCoated()) {
            settleAsPhysical(level);
            return;
        }

        BlockPos base = BlockPos.containing(this.getX(), this.getY() + 0.05D, this.getZ());

        BlockPos[] candidates = new BlockPos[]{base, base.above(), base.north(), base.south(), base.east(), base.west(), base.above().north(), base.above().south(), base.above().east(), base.above().west()};

        for (BlockPos pos : candidates) {
            if (!canPlaceAt(level, pos)) {
                continue;
            }

            level.setBlockAndUpdate(pos, this.blockState);
            restoreBlockEntityData(level, pos);
            clearOwnerReference(level);
            this.discard();
            return;
        }

        this.settleTicks = 0;
        this.freeTicks = Math.min(this.freeTicks, MAX_FREE_TICKS - 20);
        this.setDeltaMovement(this.getDeltaMovement().add((this.random.nextDouble() - 0.5D) * 0.035D, 0.04D, (this.random.nextDouble() - 0.5D) * 0.035D));
    }

    private void settleAsPhysical(ServerLevel level) {
        this.setMode(MODE_SETTLED);
        this.setDeltaMovement(Vec3.ZERO);
        this.setNoGravity(true);
        this.noPhysics = false;
        this.settleTicks = 0;
        this.freeTicks = 0;
        this.settledPoseChosen = false;

        MeltedNephritisEffects.spawnIdle(level, this.position().add(0.0D, 0.5D, 0.0D));
    }

    private void restoreBlockEntityData(ServerLevel level, BlockPos pos) {
        restoreBlockEntityData(level, pos, this.blockState, this.blockEntityData);
    }

    private boolean canPlaceAt(ServerLevel level, BlockPos pos) {
        BlockState replaced = level.getBlockState(pos);

        if (!replaced.canBeReplaced() || !level.getFluidState(pos).isEmpty()) {
            return false;
        }

        if (!this.blockState.canSurvive(level, pos)) {
            return false;
        }

        AABB placeBox = new AABB(pos).deflate(0.035D);
        return level.noCollision(this, placeBox);
    }

    private void clearOwnerReference(ServerLevel level) {
        ServerPlayer owner = resolveOwner(level);
        if (owner != null) {
            EntropyChestplateGloveItem.clearReferenceFromInventory(owner, this.getUUID());
        }
        this.ownerId = null;
    }

    private Vec3 getRandomStructureSurfacePosition() {
        Vec3 rootCenter = this.position().add(0.0D, 0.5D, 0.0D);

        if (this.attachedParts.isEmpty() || this.random.nextInt(this.attachedParts.size() + 1) == 0) {
            return rootCenter.add((this.random.nextDouble() - 0.5D) * 0.75D, (this.random.nextDouble() - 0.5D) * 0.75D, (this.random.nextDouble() - 0.5D) * 0.75D);
        }

        StructurePart part = this.attachedParts.get(this.random.nextInt(this.attachedParts.size()));
        Vec3 offset = rotateLocal(new Vec3(part.offset.getX(), part.offset.getY(), part.offset.getZ()));

        return rootCenter.add(offset).add((this.random.nextDouble() - 0.5D) * 0.75D, (this.random.nextDouble() - 0.5D) * 0.75D, (this.random.nextDouble() - 0.5D) * 0.75D);
    }

    private void tickVisualRotation() {
        this.visualYawO = this.visualYaw;
        this.visualPitchO = this.visualPitch;
        this.visualRollO = this.visualRoll;

        int mode = getMode();
        double speed = this.getDeltaMovement().length();

        if (mode == MODE_PULLING) {
            this.settledPoseChosen = false;
            this.visualYaw += 3.0F;
            this.visualPitch = 12.0F + (float) Math.sin(this.tickCount * 0.55D) * 4.0F;
            this.visualRoll = 10.0F + (float) Math.cos(this.tickCount * 0.42D) * 3.0F;
            return;
        }

        if (mode == MODE_HELD) {
            this.settledPoseChosen = false;
            this.visualYaw += 2.1F;
            this.visualPitch = 18.0F + (float) Math.sin(this.tickCount * 0.12D) * 4.5F;
            this.visualRoll = 12.0F + (float) Math.cos(this.tickCount * 0.10D) * 3.5F;
            return;
        }

        if (mode == MODE_SETTLED) {
            if (!this.settledPoseChosen) {
                this.settledYaw = snapToQuarterTurn(this.visualYaw);
                this.settledPitch = snapToQuarterTurn(this.visualPitch);
                this.settledRoll = snapToQuarterTurn(this.visualRoll);
                this.settledPoseChosen = true;
            }

            this.visualYaw = approachAngle(this.visualYaw, this.settledYaw, 5.0F);
            this.visualPitch = approachAngle(this.visualPitch, this.settledPitch, 5.0F);
            this.visualRoll = approachAngle(this.visualRoll, this.settledRoll, 5.0F);
            return;
        }

        this.settledPoseChosen = false;

        // Non-coated blocks keep exactly the old minimum 5 degree spin.
        double minSpin = isNephritisCoated() ? 0.8D : 5.0D;
        double speedFactor = isNephritisCoated() ? 13.0D : 11.0D;
        float spin = (float) Mth.clamp(minSpin + speed * speedFactor, minSpin, 24.0D);

        this.visualYaw += spin * 0.74F;
        this.visualPitch += spin;
        this.visualRoll += spin * 0.61F;
    }

    private static float snapToQuarterTurn(float angle) {
        return Math.round(angle / 90.0F) * 90.0F;
    }

    private static float approachAngle(float current, float target, float maxStep) {
        float difference = Mth.wrapDegrees(target - current);
        difference = Mth.clamp(difference, -maxStep, maxStep);
        return Mth.wrapDegrees(current + difference);
    }

    private void syncStructureData() {
        this.entityData.set(STRUCTURE_DATA, writeStructureTag());
    }

    private CompoundTag writeStructureTag() {
        CompoundTag root = new CompoundTag();
        ListTag list = new ListTag();

        for (StructurePart part : this.attachedParts) {
            CompoundTag tag = new CompoundTag();
            tag.putInt("X", part.offset.getX());
            tag.putInt("Y", part.offset.getY());
            tag.putInt("Z", part.offset.getZ());
            tag.putInt("State", Block.getId(part.state));
            tag.putBoolean("Bonded", part.bonded);

            if (part.blockEntityData != null) {
                tag.put("BlockEntity", part.blockEntityData.copy());
            }

            list.add(tag);
        }

        root.put("Parts", list);
        return root;
    }

    private void readStructureTag(CompoundTag root) {
        this.attachedParts.clear();
        ListTag list = root.getList("Parts", Tag.TAG_COMPOUND);

        for (int i = 0; i < list.size() && this.attachedParts.size() < MAX_ATTACHED_BLOCKS; i++) {
            CompoundTag tag = list.getCompound(i);
            BlockState state = Block.stateById(tag.getInt("State"));

            if (state == null || state.isAir()) {
                continue;
            }

            BlockPos offset = new BlockPos(tag.getInt("X"), tag.getInt("Y"), tag.getInt("Z"));

            if (offset.equals(BlockPos.ZERO)) {
                continue;
            }

            CompoundTag data = tag.contains("BlockEntity", Tag.TAG_COMPOUND) ? tag.getCompound("BlockEntity").copy() : null;

            this.attachedParts.add(new StructurePart(offset, state, data, tag.getBoolean("Bonded")));
        }
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> accessor) {
        super.onSyncedDataUpdated(accessor);

        if (accessor == STRUCTURE_DATA && this.level().isClientSide) {
            readStructureTag(this.entityData.get(STRUCTURE_DATA));
        }
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putInt("CarriedBlockState", Block.getId(this.blockState));
        tag.putFloat("SourceHardness", this.sourceHardness);
        tag.putInt("GravityMode", getMode());
        tag.putBoolean("NephritisCoated", isNephritisCoated());
        tag.putBoolean("ShatterOnImpact", this.shatterOnImpact);
        tag.putInt("HeldHand", this.heldHand);
        tag.putInt("PullTicks", this.pullTicks);
        tag.putInt("SettleTicks", this.settleTicks);
        tag.putInt("FreeTicks", this.freeTicks);
        tag.putInt("NoteSequence", this.noteSequence);
        tag.putFloat("VisualYaw", this.visualYaw);
        tag.putFloat("VisualPitch", this.visualPitch);
        tag.putFloat("VisualRoll", this.visualRoll);
        tag.put("AttachedStructure", writeStructureTag());

        if (this.blockEntityData != null) {
            tag.put("CarriedBlockEntityData", this.blockEntityData.copy());
        }

        if (this.ownerId != null) {
            tag.putUUID("GravityOwner", this.ownerId);
        }
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        BlockState loaded = Block.stateById(tag.getInt("CarriedBlockState"));
        this.blockState = loaded == null ? Blocks.STONE.defaultBlockState() : loaded;
        this.sourceHardness = tag.getFloat("SourceHardness");
        this.setMode(tag.getInt("GravityMode"));
        this.setNephritisCoated(tag.getBoolean("NephritisCoated"));
        this.shatterOnImpact = tag.getBoolean("ShatterOnImpact");
        this.heldHand = tag.getInt("HeldHand");
        this.pullTicks = tag.getInt("PullTicks");
        this.settleTicks = tag.getInt("SettleTicks");
        this.freeTicks = tag.getInt("FreeTicks");
        this.noteSequence = tag.getInt("NoteSequence");
        this.visualYaw = this.visualYawO = tag.getFloat("VisualYaw");
        this.visualPitch = this.visualPitchO = tag.getFloat("VisualPitch");
        this.visualRoll = this.visualRollO = tag.getFloat("VisualRoll");
        this.blockEntityData = tag.contains("CarriedBlockEntityData", Tag.TAG_COMPOUND) ? tag.getCompound("CarriedBlockEntityData").copy() : null;
        this.ownerId = tag.hasUUID("GravityOwner") ? tag.getUUID("GravityOwner") : null;

        if (tag.contains("AttachedStructure", Tag.TAG_COMPOUND)) {
            readStructureTag(tag.getCompound("AttachedStructure"));
        } else {
            this.attachedParts.clear();
        }

        syncStructureData();

        boolean heldLike = getMode() == MODE_PULLING || getMode() == MODE_HELD;
        this.noPhysics = heldLike;
        this.setNoGravity(true);
    }

    @Override
    public void writeSpawnData(FriendlyByteBuf buffer) {
        buffer.writeVarInt(Block.getId(this.blockState));
        buffer.writeFloat(this.sourceHardness);
        buffer.writeBoolean(isNephritisCoated());
        buffer.writeBoolean(this.shatterOnImpact);
        buffer.writeNbt(writeStructureTag());
        buffer.writeFloat(this.visualYaw);
        buffer.writeFloat(this.visualPitch);
        buffer.writeFloat(this.visualRoll);
    }

    @Override
    public void readSpawnData(FriendlyByteBuf buffer) {
        BlockState loaded = Block.stateById(buffer.readVarInt());
        this.blockState = loaded == null ? Blocks.STONE.defaultBlockState() : loaded;
        this.sourceHardness = buffer.readFloat();
        this.setNephritisCoated(buffer.readBoolean());
        this.shatterOnImpact = buffer.readBoolean();

        CompoundTag structure = buffer.readNbt();
        if (structure != null) {
            readStructureTag(structure);
            this.entityData.set(STRUCTURE_DATA, structure.copy());
        }

        this.visualYaw = this.visualYawO = buffer.readFloat();
        this.visualPitch = this.visualPitchO = buffer.readFloat();
        this.visualRoll = this.visualRollO = buffer.readFloat();
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }

    @Override
    public boolean isPickable() {
        return isNephritisCoated() && getMode() == MODE_SETTLED;
    }

    private int getStructureRadiusCells() {
        int radius = 1;

        for (StructurePart part : this.attachedParts) {
            radius = Math.max(radius, Math.max(Math.abs(part.offset.getX()), Math.max(Math.abs(part.offset.getY()), Math.abs(part.offset.getZ()))));
        }

        return radius;
    }

    @Override
    public float getPickRadius() {
        if (!isPickable()) {
            return 0.0F;
        }

        return Math.min(MAX_STRUCTURE_RADIUS + 0.5F, getStructureRadiusCells() + 0.65F);
    }

    @Override
    public AABB getBoundingBoxForCulling() {
        if (this.attachedParts.isEmpty()) {
            return super.getBoundingBoxForCulling();
        }

        double radius = Math.min(MAX_STRUCTURE_RADIUS + 1.5D, getStructureRadiusCells() + 1.25D);

        return this.getBoundingBox().inflate(radius);
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public boolean hurt(net.minecraft.world.damagesource.DamageSource source, float amount) {
        return false;
    }


    public static final class StructurePart {
        private final BlockPos offset;
        private final BlockState state;
        private final CompoundTag blockEntityData;
        private boolean bonded;

        private StructurePart(BlockPos offset, BlockState state, CompoundTag blockEntityData, boolean bonded) {
            this.offset = offset;
            this.state = state;
            this.blockEntityData = blockEntityData;
            this.bonded = bonded;
        }

        public BlockPos offset() {
            return this.offset;
        }

        public BlockState state() {
            return this.state;
        }

        public boolean bonded() {
            return this.bonded;
        }
    }

}
