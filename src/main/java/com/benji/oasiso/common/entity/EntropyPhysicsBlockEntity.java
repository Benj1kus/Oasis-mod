package com.benji.oasiso.common.entity;

import com.benji.oasiso.Oasiso;
import com.benji.oasiso.common.item.EntropyChestplateGloveItem;
import com.benji.oasiso.common.util.MeltedNephritisEffects;
import com.benji.oasiso.registry.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
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
import net.minecraft.world.level.material.FluidState;
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
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class EntropyPhysicsBlockEntity extends Entity implements IEntityAdditionalSpawnData {

    public static final int MODE_PULLING = 0;
    public static final int MODE_HELD = 1;
    public static final int MODE_DROPPED = 2;
    public static final int MODE_THROWN = 3;
    public static final int MODE_SETTLED = 4;

    private static final EntityDataAccessor<Integer> MODE = SynchedEntityData.defineId(EntropyPhysicsBlockEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> CARRIED_BLOCK_STATE = SynchedEntityData.defineId(EntropyPhysicsBlockEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> NEPHRITIS_COATED = SynchedEntityData.defineId(EntropyPhysicsBlockEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<CompoundTag> STRUCTURE_DATA = SynchedEntityData.defineId(EntropyPhysicsBlockEntity.class, EntityDataSerializers.COMPOUND_TAG);

    private static final int PULL_SHAKE_TICKS = 8;

    private static final double HOLD_FORWARD = 2.15D;
    private static final double HOLD_SIDE = 0.72D;
    private static final double HOLD_DOWN = 0.55D;
    private static final double HOLD_FOLLOW_FACTOR = 0.38D;

    /*
     * One glove contributes this much lifting power. Mass is derived from the
     * hardness of EVERY block in the structure, so helpers genuinely add
     * together instead of merely changing an animation speed.
     */
    private static final double LIFT_POWER_PER_PLAYER = 18.0D;
    private static final double MIN_HOLD_FOLLOW_FACTOR = 0.035D;
    private static final double MAX_HEAVY_SAG = 2.35D;
    private static final double THROW_HELPER_BONUS = 0.18D;
    private static final double MAX_THROW_HELPER_MULTIPLIER = 1.65D;

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
    private static final float TNT_EXPLOSION_SCALE = 1.55F;
    private static final float TNT_EXPLOSION_MAX = 13.0F;
    private static final int NOTE_IMPACT_COOLDOWN_TICKS = 3;
    private static final int THROW_PLAYER_IMMUNITY_TICKS = 8;

    private static final int MAX_ATTACHED_BLOCKS = 64;
    private static final int MAX_STRUCTURE_RADIUS = 5;
    private static final double STRUCTURE_RAYCAST_REACH = 7.0D;
    private static final Set<Block> FRAGILE_GLASS_BLOCKS = Set.of(Blocks.GLASS, Blocks.GLASS_PANE, Blocks.WHITE_STAINED_GLASS, Blocks.WHITE_STAINED_GLASS_PANE, Blocks.ORANGE_STAINED_GLASS, Blocks.ORANGE_STAINED_GLASS_PANE, Blocks.MAGENTA_STAINED_GLASS, Blocks.MAGENTA_STAINED_GLASS_PANE, Blocks.LIGHT_BLUE_STAINED_GLASS, Blocks.LIGHT_BLUE_STAINED_GLASS_PANE, Blocks.YELLOW_STAINED_GLASS, Blocks.YELLOW_STAINED_GLASS_PANE, Blocks.LIME_STAINED_GLASS, Blocks.LIME_STAINED_GLASS_PANE, Blocks.PINK_STAINED_GLASS, Blocks.PINK_STAINED_GLASS_PANE, Blocks.GRAY_STAINED_GLASS, Blocks.GRAY_STAINED_GLASS_PANE, Blocks.LIGHT_GRAY_STAINED_GLASS, Blocks.LIGHT_GRAY_STAINED_GLASS_PANE, Blocks.CYAN_STAINED_GLASS, Blocks.CYAN_STAINED_GLASS_PANE, Blocks.PURPLE_STAINED_GLASS, Blocks.PURPLE_STAINED_GLASS_PANE, Blocks.BLUE_STAINED_GLASS, Blocks.BLUE_STAINED_GLASS_PANE, Blocks.BROWN_STAINED_GLASS, Blocks.BROWN_STAINED_GLASS_PANE, Blocks.GREEN_STAINED_GLASS, Blocks.GREEN_STAINED_GLASS_PANE, Blocks.RED_STAINED_GLASS, Blocks.RED_STAINED_GLASS_PANE, Blocks.BLACK_STAINED_GLASS, Blocks.BLACK_STAINED_GLASS_PANE);

    private BlockState blockState = Blocks.STONE.defaultBlockState();
    private float sourceHardness = 1.5F;
    private CompoundTag blockEntityData;

    private final List<StructurePart> attachedParts = new ArrayList<>();
    private boolean shatterOnImpact;
    private StructurePhysicsProfile cachedPhysicsProfile;
    private boolean physicsProfileDirty = true;

    /*
     * ownerId/heldHand are retained as a primary-holder compatibility mirror.
     * Actual control is multi-player and lives in holderHands.
     */
    private UUID ownerId;
    private int heldHand = 0;
    private final Map<UUID, Integer> holderHands = new LinkedHashMap<>();
    private int pullTicks = PULL_SHAKE_TICKS;
    private int settleTicks;
    private int freeTicks;

    private int lastHitEntityId = -1;
    private int hitCooldown;
    private int noteSequence;
    private int noteImpactCooldown;
    private final Set<UUID> throwProtectedPlayers = new HashSet<>();

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
        setCarriedBlockState(state);
        this.sourceHardness = Math.max(0.0F, hardness);
        this.blockEntityData = blockEntityData == null ? null : blockEntityData.copy();
        setSingleHolder(owner, hand);
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
        setCarriedBlockState(state);
        this.blockEntityData = data == null ? null : data.copy();
        this.sourceHardness = Math.max(0.0F, state.getDestroySpeed(this.level(), BlockPos.containing(position)));
        this.ownerId = null;
        this.heldHand = 0;
        this.holderHands.clear();
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

    private void initializeExtractedPart(BlockState state, CompoundTag data, ServerPlayer owner, InteractionHand hand, Vec3 worldCenter) {
        setCarriedBlockState(state);
        this.blockEntityData = data == null ? null : data.copy();
        this.sourceHardness = Math.max(0.0F, state.getDestroySpeed(this.level(), BlockPos.containing(worldCenter)));
        setSingleHolder(owner, hand);
        this.pullTicks = PULL_SHAKE_TICKS;
        this.settleTicks = 0;
        this.freeTicks = 0;
        this.shatterOnImpact = false;
        this.setNephritisCoated(false);
        this.attachedParts.clear();
        syncStructureData();

        this.setMode(MODE_PULLING);
        this.setPos(worldCenter.x, worldCenter.y - 0.5D, worldCenter.z);
        this.setDeltaMovement(Vec3.ZERO);
        this.noPhysics = true;
        this.setNoGravity(true);
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(MODE, MODE_PULLING);
        this.entityData.define(CARRIED_BLOCK_STATE, Block.getId(Blocks.STONE.defaultBlockState()));
        this.entityData.define(NEPHRITIS_COATED, false);
        this.entityData.define(STRUCTURE_DATA, new CompoundTag());
    }

    private void setCarriedBlockState(BlockState state) {
        this.blockState = state == null ? Blocks.STONE.defaultBlockState() : state;
        this.physicsProfileDirty = true;
        this.entityData.set(CARRIED_BLOCK_STATE, Block.getId(this.blockState));
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
        List<HolderControl> holders = collectActiveHolders(level);
        if (holders.isEmpty()) {
            dropFromGravityControl(level);
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
        List<HolderControl> holders = collectActiveHolders(level);
        if (holders.isEmpty()) {
            dropFromGravityControl(level);
            return;
        }

        this.noPhysics = true;
        this.setNoGravity(true);

        Vec3 target = Vec3.ZERO;
        for (HolderControl holder : holders) {
            target = target.add(getHeldTarget(holder.player(), holder.handIndex()));
        }
        target = target.scale(1.0D / holders.size());

        StructurePhysicsProfile profile = getStructurePhysicsProfile();
        double liftRatio = getLiftRatio(profile.totalMass(), holders.size());
        double sag = Mth.clamp((1.0D - liftRatio) * MAX_HEAVY_SAG, 0.0D, MAX_HEAVY_SAG);
        target = target.add(0.0D, -sag, 0.0D);

        Vec3 difference = target.subtract(this.position());
        double baseFollow = difference.lengthSqr() > 16.0D ? 0.62D : HOLD_FOLLOW_FACTOR;
        double follow = Mth.clamp(baseFollow * Mth.clamp(liftRatio, 0.09D, 1.15D), MIN_HOLD_FOLLOW_FACTOR, 0.68D);

        Vec3 step = difference.scale(follow);
        if (liftRatio < 0.55D && step.y > 0.0D) {
            step = new Vec3(step.x, step.y * Mth.clamp(liftRatio / 0.55D, 0.08D, 1.0D), step.z);
        }

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
                if (hasTntBlocks()) {
                    detonateTntParts(level);
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

        if (hasMagmaBlocks() && this.tickCount % 4 == 0) {
            igniteTouchedBlocks(level);
        }

        if (collided && this.shatterOnImpact && this.freeTicks > 2) {
            shatterLooseFragment(level);
            return;
        }

        if (collided && getMode() == MODE_THROWN) {
            if (hasTntBlocks()) {
                detonateTntParts(level);
                return;
            }

            if (hasFragileGlassBlocks()) {
                shatterFragileParts(level);
                return;
            }
        }

        if (collided && hasHoneyBlocks()) {
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
            if (hasNoteBlocks()) {
                playNextNote(level);
            } else {
                level.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.STONE_HIT, SoundSource.BLOCKS, 0.55F, 0.85F + this.random.nextFloat() * 0.25F);
            }
        }

        this.setDeltaMovement(nextX, nextY, nextZ);

        double speedSqr = this.getDeltaMovement().lengthSqr();
        double settleThreshold = hasIceBlocks() ? SETTLE_SPEED_SQR * 0.28D : SETTLE_SPEED_SQR;
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

        if (entity instanceof Player player) {
            if (player.isSpectator() || player.getAbilities().instabuild) {
                return false;
            }
            if (this.freeTicks <= THROW_PLAYER_IMMUNITY_TICKS && this.throwProtectedPlayers.contains(player.getUUID())) {
                return false;
            }
        }

        ServerPlayer owner = this.level() instanceof ServerLevel level ? resolveOwner(level) : null;

        return owner == null || entity != owner;
    }

    public float getImpactDamage() {
        StructurePhysicsProfile profile = getStructurePhysicsProfile();

        double hardnessContribution = 4.5D + profile.averageHardness() * 1.15D;
        double massContribution = 1.0D + Math.log1p(profile.totalMass()) * 0.34D;

        return (float) Mth.clamp(hardnessContribution * massContribution, 5.0D, 60.0D);
    }

    private double getHardnessFactor() {
        double capped = Mth.clamp(getStructurePhysicsProfile().averageHardness(), 0.0D, 50.0D);
        return Math.log1p(capped) / Math.log1p(50.0D);
    }

    private double getThrowSpeed() {
        StructurePhysicsProfile profile = getStructurePhysicsProfile();
        double base = Mth.lerp(getHardnessFactor(), THROW_SPEED_SOFT, THROW_SPEED_HARD);

        double onePlayerRatio = LIFT_POWER_PER_PLAYER / Math.max(1.0D, profile.totalMass());
        double massFactor = Mth.clamp(Math.sqrt(onePlayerRatio), 0.24D, 1.0D);

        int holderCount = Math.max(1, getActiveHolderCount());
        double helperMultiplier = Math.min(MAX_THROW_HELPER_MULTIPLIER, 1.0D + (holderCount - 1) * THROW_HELPER_BONUS);

        return base * massFactor * helperMultiplier;
    }

    private double getBounceCoefficient() {
        StructurePhysicsProfile profile = getStructurePhysicsProfile();
        double capped = Mth.clamp(profile.averageHardness(), 0.0D, 50.0D);
        double hardnessFactor = Math.log1p(capped) / Math.log1p(50.0D);
        double bounce = Mth.lerp(hardnessFactor, SOFT_BLOCK_BOUNCE, HARD_BLOCK_BOUNCE);

        if (profile.logRatio() > 0.0D) {
            bounce = Math.max(bounce, Mth.lerp(profile.logRatio(), bounce, LOG_MIN_BOUNCE));
        }

        if (profile.slimeRatio() > 0.0D) {
            double slimeInfluence = Mth.clamp(profile.slimeRatio() * 1.75D, 0.0D, 1.0D);
            bounce = Mth.lerp(slimeInfluence, bounce, SLIME_BOUNCE);
        }

        if (profile.honeyRatio() > 0.0D) {
            double honeyDamping = Mth.clamp(1.0D - profile.honeyRatio() * 1.35D, 0.18D, 1.0D);
            bounce *= honeyDamping;
        }

        return Mth.clamp(bounce, 0.08D, 0.96D);
    }

    private double getGroundFriction() {
        StructurePhysicsProfile profile = getStructurePhysicsProfile();
        double friction = NORMAL_GROUND_FRICTION;

        if (profile.logRatio() > 0.0D) {
            friction = Mth.lerp(Mth.clamp(profile.logRatio() * 1.3D, 0.0D, 1.0D), friction, LOG_GROUND_FRICTION);
        }

        if (profile.iceRatio() > 0.0D) {
            friction = Mth.lerp(Mth.clamp(profile.iceRatio() * 1.8D, 0.0D, 1.0D), friction, ICE_GROUND_FRICTION);
        }

        if (profile.honeyRatio() > 0.0D) {
            friction = Mth.lerp(Mth.clamp(profile.honeyRatio() * 1.8D, 0.0D, 1.0D), friction, 0.42D);
        }

        return Mth.clamp(friction, 0.35D, 0.985D);
    }

    private boolean isSlimeState(BlockState state) {
        return state.is(Blocks.SLIME_BLOCK);
    }

    private boolean isHoneyState(BlockState state) {
        return state.is(Blocks.HONEY_BLOCK);
    }

    private boolean isLogState(BlockState state) {
        return state.is(BlockTags.LOGS);
    }

    private boolean isIceState(BlockState state) {
        return state.is(Blocks.ICE) || state.is(Blocks.PACKED_ICE) || state.is(Blocks.BLUE_ICE) || state.is(Blocks.FROSTED_ICE);
    }

    private boolean isTntState(BlockState state) {
        return state.is(Blocks.TNT);
    }

    private boolean isMagmaState(BlockState state) {
        return state.is(Blocks.MAGMA_BLOCK);
    }

    private boolean isFragileGlassState(BlockState state) {
        return FRAGILE_GLASS_BLOCKS.contains(state.getBlock());
    }

    private boolean isNoteState(BlockState state) {
        return state.is(Blocks.NOTE_BLOCK);
    }

    private boolean hasSlimeBlocks() {
        return getStructurePhysicsProfile().slimeCount() > 0;
    }

    private boolean hasHoneyBlocks() {
        return getStructurePhysicsProfile().honeyCount() > 0;
    }

    private boolean hasIceBlocks() {
        return getStructurePhysicsProfile().iceCount() > 0;
    }

    private boolean hasTntBlocks() {
        return getStructurePhysicsProfile().tntCount() > 0;
    }

    private boolean hasMagmaBlocks() {
        return getStructurePhysicsProfile().magmaCount() > 0;
    }

    private boolean hasFragileGlassBlocks() {
        return getStructurePhysicsProfile().glassCount() > 0;
    }

    private boolean hasNoteBlocks() {
        return getStructurePhysicsProfile().noteCount() > 0;
    }

    private StructurePhysicsProfile getStructurePhysicsProfile() {
        if (!this.physicsProfileDirty && this.cachedPhysicsProfile != null) {
            return this.cachedPhysicsProfile;
        }

        PhysicsAccumulator accumulator = new PhysicsAccumulator();
        accumulator.accept(this.blockState, Math.max(0.0D, this.sourceHardness));

        for (StructurePart part : this.attachedParts) {
            accumulator.accept(part.state, getStateHardness(part.state));
        }

        this.cachedPhysicsProfile = accumulator.finish();
        this.physicsProfileDirty = false;
        return this.cachedPhysicsProfile;
    }

    private double getStateHardness(BlockState state) {
        float hardness = state.getDestroySpeed(this.level(), this.blockPosition());
        if (hardness < 0.0F) {
            hardness = EntropyChestplateGloveItem.MAX_PICKUP_HARDNESS;
        }

        return Mth.clamp(hardness, 0.0F, EntropyChestplateGloveItem.MAX_PICKUP_HARDNESS);
    }

    private static double massFromHardness(double hardness) {
        return 0.65D + Math.sqrt(Math.max(0.0D, hardness) + 1.0D) * 0.45D;
    }

    private double getLiftRatio(double totalMass, int holderCount) {
        if (holderCount <= 0) {
            return 0.0D;
        }

        return holderCount * LIFT_POWER_PER_PLAYER / Math.max(1.0D, totalMass);
    }

    public double getTotalStructureMass() {
        return getStructurePhysicsProfile().totalMass();
    }

    public int getStructureBlockCount() {
        return 1 + this.attachedParts.size();
    }

    private void detonateTntParts(ServerLevel level) {
        StructurePhysicsProfile profile = getStructurePhysicsProfile();
        int tntCount = profile.tntCount();

        if (tntCount <= 0 || this.isRemoved()) {
            return;
        }

        float power = TNT_EXPLOSION_POWER;
        if (tntCount > 1) {
            power += (float) (Math.sqrt(tntCount - 1.0D) * TNT_EXPLOSION_SCALE);
        }
        power = Math.min(power, TNT_EXPLOSION_MAX);

        boolean rootWasTnt = isTntState(this.blockState);

        level.explode(this, this.getX(), this.getY() + 0.45D, this.getZ(), power, Level.ExplosionInteraction.TNT);

        removeSpecialParts(level, this::isTntState, rootWasTnt);
    }

    private void shatterFragileParts(ServerLevel level) {
        if (!hasFragileGlassBlocks() || this.isRemoved()) {
            return;
        }

        boolean rootWasGlass = isFragileGlassState(this.blockState);
        removeSpecialParts(level, this::isFragileGlassState, rootWasGlass);
    }

    private void removeSpecialParts(ServerLevel level, java.util.function.Predicate<BlockState> predicate, boolean removeRoot) {
        Vec3 oldRootCenter = this.position().add(0.0D, 0.5D, 0.0D);

        Iterator<StructurePart> iterator = this.attachedParts.iterator();
        while (iterator.hasNext()) {
            StructurePart part = iterator.next();
            if (!predicate.test(part.state)) {
                continue;
            }

            Vec3 worldCenter = oldRootCenter.add(rotateLocal(new Vec3(part.offset.getX(), part.offset.getY(), part.offset.getZ())));

            level.levelEvent(2001, BlockPos.containing(worldCenter), Block.getId(part.state));
            iterator.remove();
        }

        if (!removeRoot) {
            syncStructureData();
            return;
        }

        level.levelEvent(2001, BlockPos.containing(this.getX(), this.getY() + 0.45D, this.getZ()), Block.getId(this.blockState));

        StructurePart replacement = null;
        for (StructurePart part : this.attachedParts) {
            if (!predicate.test(part.state)) {
                replacement = part;
                break;
            }
        }

        if (replacement == null) {
            clearOwnerReference(level);
            this.discard();
            return;
        }

        BlockPos replacementOffset = replacement.offset;
        Vec3 replacementWorldCenter = oldRootCenter.add(rotateLocal(new Vec3(replacementOffset.getX(), replacementOffset.getY(), replacementOffset.getZ())));

        setCarriedBlockState(replacement.state);
        this.blockEntityData = replacement.blockEntityData == null ? null : replacement.blockEntityData.copy();
        this.sourceHardness = (float) getStateHardness(replacement.state);

        List<StructurePart> remapped = new ArrayList<>();
        for (StructurePart part : this.attachedParts) {
            if (part == replacement) {
                continue;
            }

            BlockPos newOffset = part.offset.subtract(replacementOffset);
            remapped.add(new StructurePart(newOffset, part.state, part.blockEntityData == null ? null : part.blockEntityData.copy(), part.bonded));
        }

        this.attachedParts.clear();
        this.attachedParts.addAll(remapped);
        this.setPos(replacementWorldCenter.x, replacementWorldCenter.y - 0.5D, replacementWorldCenter.z);
        syncStructureData();
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
        List<Vec3> centers = getStructureWorldCenters();

        for (Vec3 center : centers) {
            AABB touchBox = new AABB(center.x - 0.50D, center.y - 0.50D, center.z - 0.50D, center.x + 0.50D, center.y + 0.50D, center.z + 0.50D).inflate(0.055D);

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
    }

    private List<Vec3> getStructureWorldCenters() {
        Vec3 rootCenter = this.position().add(0.0D, 0.5D, 0.0D);
        List<Vec3> centers = new ArrayList<>(1 + this.attachedParts.size());
        centers.add(rootCenter);

        for (StructurePart part : this.attachedParts) {
            centers.add(rootCenter.add(rotateLocal(new Vec3(part.offset.getX(), part.offset.getY(), part.offset.getZ()))));
        }

        return centers;
    }

    private BlockState findFirstState(java.util.function.Predicate<BlockState> predicate) {
        if (predicate.test(this.blockState)) {
            return this.blockState;
        }

        for (StructurePart part : this.attachedParts) {
            if (predicate.test(part.state)) {
                return part.state;
            }
        }

        return null;
    }

    private void playNextNote(ServerLevel level) {
        if (this.noteImpactCooldown > 0) {
            return;
        }

        BlockState noteState = findFirstState(this::isNoteState);
        if (noteState == null || !noteState.hasProperty(NoteBlock.INSTRUMENT)) {
            return;
        }

        int baseNote = noteState.hasProperty(NoteBlock.NOTE) ? noteState.getValue(NoteBlock.NOTE) : 0;
        int note = Math.floorMod(baseNote + this.noteSequence, 25);
        float pitch = (float) Math.pow(2.0D, (note - 12) / 12.0D);

        level.playSound(null, this.getX(), this.getY() + 0.5D, this.getZ(), noteState.getValue(NoteBlock.INSTRUMENT).getSoundEvent().value(), SoundSource.RECORDS, 1.0F, pitch);

        this.noteSequence = (this.noteSequence + 1) % 25;
        this.noteImpactCooldown = NOTE_IMPACT_COOLDOWN_TICKS;
    }

    public void throwFrom(ServerPlayer player) {
        if (this.level().isClientSide || (getMode() != MODE_HELD && getMode() != MODE_PULLING) || !(this.level() instanceof ServerLevel level)) {
            return;
        }

        List<HolderControl> activeHolders = collectActiveHolders(level);
        boolean initiatorIsHolder = activeHolders.stream().anyMatch(holder -> holder.player().getUUID().equals(player.getUUID()));

        if (!initiatorIsHolder) {
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
        this.throwProtectedPlayers.clear();
        for (HolderControl holder : activeHolders) {
            this.throwProtectedPlayers.add(holder.player().getUUID());
        }

        clearOwnerReference(level);

        this.level().playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.TRIDENT_THROW, SoundSource.PLAYERS, 0.8F, 1.15F);
    }

    public void releaseHolder(ServerPlayer player) {
        if (this.level().isClientSide || !(this.level() instanceof ServerLevel level)) {
            return;
        }

        if (getMode() != MODE_HELD && getMode() != MODE_PULLING) {
            return;
        }

        boolean removed = this.holderHands.remove(player.getUUID()) != null;
        EntropyChestplateGloveItem.clearReferenceFromInventory(player, this.getUUID());

        if (!removed) {
            return;
        }

        List<HolderControl> remaining = collectActiveHolders(level);
        if (!remaining.isEmpty()) {
            updatePrimaryHolder(remaining);
            return;
        }

        dropFromGravityControl(level, player.getDeltaMovement().scale(0.35D));
    }

    public void releaseFromPlayer() {
        if (this.level().isClientSide || !(this.level() instanceof ServerLevel level)) {
            return;
        }

        if (getMode() != MODE_HELD && getMode() != MODE_PULLING) {
            return;
        }

        ServerPlayer owner = resolveOwner(level);
        Vec3 inherited = owner == null ? Vec3.ZERO : owner.getDeltaMovement().scale(0.35D);
        clearOwnerReference(level);
        dropFromGravityControl(level, inherited);
    }

    private void dropFromGravityControl(ServerLevel level) {
        dropFromGravityControl(level, Vec3.ZERO);
    }

    private void dropFromGravityControl(ServerLevel level, Vec3 inheritedVelocity) {
        if (getMode() != MODE_HELD && getMode() != MODE_PULLING) {
            return;
        }

        this.setMode(MODE_DROPPED);
        this.noPhysics = false;
        this.setNoGravity(true);
        this.pullTicks = 0;
        this.settleTicks = 0;
        this.freeTicks = 0;
        this.settledPoseChosen = false;
        this.setDeltaMovement(inheritedVelocity);

        this.ownerId = null;
        this.heldHand = 0;
        this.holderHands.clear();

        level.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.PISTON_CONTRACT, SoundSource.PLAYERS, 0.55F, 1.45F);
    }

    public boolean canBeGrabbedWithGlove() {
        if (this.isRemoved()) {
            return false;
        }

        if (getMode() == MODE_SETTLED) {
            return isNephritisCoated();
        }

        return true;
    }

    public boolean isHeldBy(ServerPlayer player) {
        return player != null && this.holderHands.containsKey(player.getUUID());
    }

    public InteractionResult grabWithGlove(ServerPlayer player, InteractionHand hand, ItemStack glove) {
        if (!canBeGrabbedWithGlove()) {
            return InteractionResult.PASS;
        }

        UUID existing = EntropyChestplateGloveItem.getHeldBlockId(glove);
        if (existing != null && !existing.equals(this.getUUID())) {
            return InteractionResult.FAIL;
        }

        int handIndex = hand == InteractionHand.MAIN_HAND ? 0 : 1;
        boolean alreadyControlled = !this.holderHands.isEmpty() && (getMode() == MODE_HELD || getMode() == MODE_PULLING);

        this.holderHands.put(player.getUUID(), handIndex);
        this.throwProtectedPlayers.clear();
        if (this.ownerId == null) {
            this.ownerId = player.getUUID();
            this.heldHand = handIndex;
        }

        EntropyChestplateGloveItem.bindHeldBlock(glove, this.getUUID());
        player.getInventory().setChanged();
        player.containerMenu.broadcastChanges();

        if (!alreadyControlled) {
            this.shatterOnImpact = false;
            this.setMode(MODE_PULLING);
            this.pullTicks = Math.min(PULL_SHAKE_TICKS, 5);
            this.settleTicks = 0;
            this.freeTicks = 0;
            this.settledPoseChosen = false;
            this.noPhysics = true;
            this.setNoGravity(true);
            this.setDeltaMovement(Vec3.ZERO);
        }

        this.level().playSound(null, this.getX(), this.getY(), this.getZ(), alreadyControlled ? SoundEvents.AMETHYST_BLOCK_RESONATE : SoundEvents.PISTON_EXTEND, SoundSource.PLAYERS, 0.60F, alreadyControlled ? 1.65F : 1.45F);

        return InteractionResult.CONSUME;
    }

    public InteractionResult pickupSettled(ServerPlayer player, InteractionHand hand, ItemStack glove) {
        return grabWithGlove(player, hand, glove);
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

    public InteractionResult detachAttachedBlock(ServerLevel level, ServerPlayer player, InteractionHand hand, ItemStack glove) {
        if (!isNephritisCoated() || getMode() != MODE_SETTLED || this.attachedParts.isEmpty() || !(glove.getItem() instanceof EntropyChestplateGloveItem) || EntropyChestplateGloveItem.hasHeldBlock(glove)) {

            return InteractionResult.PASS;
        }

        StructureRayHit hit = raycastStructure(player);
        if (hit == null || hit.offset().equals(BlockPos.ZERO)) {
            return InteractionResult.FAIL;
        }

        StructurePart selected = findPartAtOffset(hit.offset());
        if (selected == null) {
            return InteractionResult.FAIL;
        }

        Vec3 worldCenter = this.position().add(0.0D, 0.5D, 0.0D).add(rotateLocal(new Vec3(selected.offset.getX(), selected.offset.getY(), selected.offset.getZ())));

        EntropyPhysicsBlockEntity extracted = new EntropyPhysicsBlockEntity(ModEntities.ENTROPY_PHYSICS_BLOCK.get(), level);

        extracted.initializeExtractedPart(selected.state, selected.blockEntityData, player, hand, worldCenter);

        if (!level.addFreshEntity(extracted)) {
            return InteractionResult.FAIL;
        }

        this.attachedParts.remove(selected);
        syncStructureData();

        EntropyChestplateGloveItem.bindHeldBlock(glove, extracted.getUUID());

        level.playSound(null, worldCenter.x, worldCenter.y, worldCenter.z, SoundEvents.PISTON_EXTEND, SoundSource.PLAYERS, 0.72F, 1.28F);

        level.sendParticles(Oasiso.ENTROPY_GRAVITY_TRAIL.get(), worldCenter.x, worldCenter.y, worldCenter.z, 12, 0.30D, 0.30D, 0.30D, 0.0D);

        player.swing(hand, true);
        player.getInventory().setChanged();
        player.containerMenu.broadcastChanges();

        return InteractionResult.CONSUME;
    }

    public InteractionResult detachAttachedBlock(ServerLevel level, ServerPlayer player, InteractionHand hand, Vec3 ignoredLocalHit) {
        ItemStack glove = player.getItemInHand(hand);
        return detachAttachedBlock(level, player, hand, glove);
    }

    public InteractionResult attachBlock(ServerLevel level, ServerPlayer player, InteractionHand hand, ItemStack heldStack, BlockItem blockItem, Vec3 ignoredLocalHit) {
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

        AttachmentPreview preview = getAttachmentPreview(level, player);

        if (preview == null) {
            return InteractionResult.FAIL;
        }

        BlockPos targetOffset = preview.targetOffset();
        Direction face = preview.face();

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

    public AttachmentPreview getAttachmentPreview(Level level, Player player) {
        if (!isNephritisCoated() || getMode() != MODE_SETTLED || player == null || this.attachedParts.size() >= MAX_ATTACHED_BLOCKS) {

            return null;
        }

        StructureRayHit hit = raycastStructure(player);
        if (hit == null) {
            return null;
        }

        BlockPos targetOffset = hit.offset().relative(hit.face());

        if (Math.abs(targetOffset.getX()) > MAX_STRUCTURE_RADIUS || Math.abs(targetOffset.getY()) > MAX_STRUCTURE_RADIUS || Math.abs(targetOffset.getZ()) > MAX_STRUCTURE_RADIUS) {

            return null;
        }

        if (isOffsetOccupied(targetOffset)) {
            return null;
        }

        if (!canAttachAtWorldPosition(level, targetOffset)) {
            return null;
        }

        return new AttachmentPreview(targetOffset.immutable(), hit.face());
    }

    public AttachmentPreview getAttachmentPreview(Level level, Vec3 localHit) {
        if (!isNephritisCoated() || getMode() != MODE_SETTLED || localHit == null || this.attachedParts.size() >= MAX_ATTACHED_BLOCKS) {
            return null;
        }

        Vec3 localRootCentered = new Vec3(localHit.x, localHit.y - 0.5D, localHit.z);
        Vec3 structureHit = inverseRotateWorld(localRootCentered);
        BlockPos anchor = findNearestStructureOffset(structureHit);
        Vec3 anchorCenter = new Vec3(anchor.getX(), anchor.getY(), anchor.getZ());
        Direction face = inferClickedFace(structureHit.subtract(anchorCenter));
        BlockPos targetOffset = anchor.relative(face);

        if (Math.abs(targetOffset.getX()) > MAX_STRUCTURE_RADIUS || Math.abs(targetOffset.getY()) > MAX_STRUCTURE_RADIUS || Math.abs(targetOffset.getZ()) > MAX_STRUCTURE_RADIUS || isOffsetOccupied(targetOffset) || !canAttachAtWorldPosition(level, targetOffset)) {

            return null;
        }

        return new AttachmentPreview(targetOffset.immutable(), face);
    }

    public DetachmentPreview getDetachmentPreview(Player player) {
        if (!isNephritisCoated() || getMode() != MODE_SETTLED || player == null || this.attachedParts.isEmpty()) {

            return null;
        }

        StructureRayHit hit = raycastStructure(player);
        if (hit == null || hit.offset().equals(BlockPos.ZERO) || findPartAtOffset(hit.offset()) == null) {
            return null;
        }

        return new DetachmentPreview(hit.offset().immutable());
    }

    private StructurePart findPartAtOffset(BlockPos offset) {
        for (StructurePart part : this.attachedParts) {
            if (part.offset.equals(offset)) {
                return part;
            }
        }

        return null;
    }

    private StructureRayHit raycastStructure(Player player) {
        Vec3 worldStart = player.getEyePosition();
        Vec3 worldEnd = worldStart.add(player.getLookAngle().normalize().scale(STRUCTURE_RAYCAST_REACH));
        Vec3 rootCenter = this.position().add(0.0D, 0.5D, 0.0D);

        Vec3 localStart = inverseRotateWorld(worldStart.subtract(rootCenter));
        Vec3 localEnd = inverseRotateWorld(worldEnd.subtract(rootCenter));

        StructureRayHit best = raycastCube(localStart, localEnd, BlockPos.ZERO);

        for (StructurePart part : this.attachedParts) {
            StructureRayHit candidate = raycastCube(localStart, localEnd, part.offset);

            if (candidate != null && (best == null || candidate.distanceSqr() < best.distanceSqr())) {
                best = candidate;
            }
        }

        return best;
    }

    private static StructureRayHit raycastCube(Vec3 start, Vec3 end, BlockPos offset) {
        Vec3 delta = end.subtract(start);

        double minX = offset.getX() - 0.5D;
        double minY = offset.getY() - 0.5D;
        double minZ = offset.getZ() - 0.5D;
        double maxX = offset.getX() + 0.5D;
        double maxY = offset.getY() + 0.5D;
        double maxZ = offset.getZ() + 0.5D;

        double enter = 0.0D;
        double exit = 1.0D;
        Direction enterFace = null;
        Direction exitFace = null;

        AxisClip x = clipAxis(start.x, delta.x, minX, maxX, Direction.WEST, Direction.EAST, enter, exit);
        if (x == null) return null;
        enter = x.enter();
        exit = x.exit();
        if (x.enterFace() != null) enterFace = x.enterFace();
        if (x.exitFace() != null) exitFace = x.exitFace();

        AxisClip y = clipAxis(start.y, delta.y, minY, maxY, Direction.DOWN, Direction.UP, enter, exit);
        if (y == null) return null;
        if (y.enter() > enter + 1.0E-9D) enterFace = y.enterFace();
        if (y.exit() < exit - 1.0E-9D) exitFace = y.exitFace();
        enter = y.enter();
        exit = y.exit();

        AxisClip z = clipAxis(start.z, delta.z, minZ, maxZ, Direction.NORTH, Direction.SOUTH, enter, exit);
        if (z == null) return null;
        if (z.enter() > enter + 1.0E-9D) enterFace = z.enterFace();
        if (z.exit() < exit - 1.0E-9D) exitFace = z.exitFace();
        enter = z.enter();
        exit = z.exit();

        double t = enter >= 0.0D ? enter : exit;
        if (t < 0.0D || t > 1.0D) {
            return null;
        }

        Direction face = enter >= 0.0D ? enterFace : exitFace;
        if (face == null) {
            Vec3 point = start.add(delta.scale(t));
            face = inferClickedFace(point.subtract(new Vec3(offset.getX(), offset.getY(), offset.getZ())));
        }

        Vec3 hitPoint = start.add(delta.scale(t));
        return new StructureRayHit(offset.immutable(), face, start.distanceToSqr(hitPoint));
    }

    private static AxisClip clipAxis(double start, double delta, double min, double max, Direction minFace, Direction maxFace, double currentEnter, double currentExit) {
        if (Math.abs(delta) < 1.0E-9D) {
            return start >= min && start <= max ? new AxisClip(currentEnter, currentExit, null, null) : null;
        }

        double tMin = (min - start) / delta;
        double tMax = (max - start) / delta;
        Direction nearFace = minFace;
        Direction farFace = maxFace;

        if (tMin > tMax) {
            double swap = tMin;
            tMin = tMax;
            tMax = swap;

            Direction faceSwap = nearFace;
            nearFace = farFace;
            farFace = faceSwap;
        }

        double enter = Math.max(currentEnter, tMin);
        double exit = Math.min(currentExit, tMax);

        if (enter > exit) {
            return null;
        }

        return new AxisClip(enter, exit, tMin >= currentEnter ? nearFace : null, tMax <= currentExit ? farFace : null);
    }

    private boolean canAttachAtWorldPosition(Level level, BlockPos localOffset) {
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
        BlockState placementState = prepareStateForFluid(level, pos, state);

        return replaced.canBeReplaced() && placementState.canSurvive(level, pos);
    }

    private static void placeSinglePart(ServerLevel level, BlockPos pos, BlockState state, CompoundTag data) {
        BlockState placementState = prepareStateForFluid(level, pos, state);

        level.setBlock(pos, placementState, Block.UPDATE_ALL | Block.UPDATE_SUPPRESS_DROPS);

        restoreBlockEntityData(level, pos, placementState, data);
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

    private Vec3 getHeldTarget(ServerPlayer player, int handIndex) {
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
        if (handIndex == 0) {
            physicalArm = player.getMainArm();
        } else {
            physicalArm = player.getMainArm() == HumanoidArm.RIGHT ? HumanoidArm.LEFT : HumanoidArm.RIGHT;
        }

        double side = physicalArm == HumanoidArm.RIGHT ? 1.0D : -1.0D;

        return player.getEyePosition().add(look.scale(HOLD_FORWARD)).add(right.scale(HOLD_SIDE * side)).add(0.0D, -HOLD_DOWN, 0.0D);
    }

    private void setSingleHolder(ServerPlayer player, InteractionHand hand) {
        this.holderHands.clear();
        int handIndex = hand == InteractionHand.MAIN_HAND ? 0 : 1;
        this.holderHands.put(player.getUUID(), handIndex);
        this.ownerId = player.getUUID();
        this.heldHand = handIndex;
    }

    private List<HolderControl> collectActiveHolders(ServerLevel level) {
        List<HolderControl> active = new ArrayList<>();
        Iterator<Map.Entry<UUID, Integer>> iterator = this.holderHands.entrySet().iterator();

        while (iterator.hasNext()) {
            Map.Entry<UUID, Integer> entry = iterator.next();
            ServerPlayer player = level.getServer().getPlayerList().getPlayer(entry.getKey());

            if (player == null || player.serverLevel() != level || !player.isAlive() || player.isSpectator()) {

                iterator.remove();
                continue;
            }

            boolean mainBound = EntropyChestplateGloveItem.isBoundTo(player.getMainHandItem(), this.getUUID());

            boolean offBound = EntropyChestplateGloveItem.isBoundTo(player.getOffhandItem(), this.getUUID());

            if (!mainBound && !offBound) {
                EntropyChestplateGloveItem.clearReferenceFromInventory(player, this.getUUID());
                iterator.remove();
                continue;
            }

            int actualHand = mainBound ? 0 : 1;
            entry.setValue(actualHand);
            active.add(new HolderControl(player, actualHand));
        }

        if (!active.isEmpty()) {
            updatePrimaryHolder(active);
        } else {
            this.ownerId = null;
            this.heldHand = 0;
        }

        return active;
    }

    private void updatePrimaryHolder(List<HolderControl> holders) {
        if (holders.isEmpty()) {
            this.ownerId = null;
            this.heldHand = 0;
            return;
        }

        HolderControl primary = holders.get(0);
        this.ownerId = primary.player().getUUID();
        this.heldHand = primary.handIndex();
    }

    private int getActiveHolderCount() {
        return this.holderHands.size();
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
            BlockState placementState = prepareStateForFluid(level, pos, this.blockState);

            if (!canPlaceAt(level, pos, placementState)) {
                continue;
            }

            level.setBlock(pos, placementState, Block.UPDATE_ALL | Block.UPDATE_SUPPRESS_DROPS);
            restoreBlockEntityData(level, pos, placementState, this.blockEntityData);
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

    private boolean canPlaceAt(ServerLevel level, BlockPos pos, BlockState placementState) {
        BlockState replaced = level.getBlockState(pos);

        if (!replaced.canBeReplaced()) {
            return false;
        }

        if (!placementState.canSurvive(level, pos)) {
            return false;
        }

        AABB placeBox = new AABB(pos).deflate(0.035D);
        return level.noCollision(this, placeBox);
    }

    private static BlockState prepareStateForFluid(ServerLevel level, BlockPos pos, BlockState state) {
        if (!state.hasProperty(BlockStateProperties.WATERLOGGED)) {
            return state;
        }

        FluidState fluid = level.getFluidState(pos);
        return state.setValue(BlockStateProperties.WATERLOGGED, fluid.is(FluidTags.WATER));
    }

    private void clearOwnerReference(ServerLevel level) {
        for (UUID holderId : new ArrayList<>(this.holderHands.keySet())) {
            ServerPlayer holder = level.getServer().getPlayerList().getPlayer(holderId);
            if (holder != null) {
                EntropyChestplateGloveItem.clearReferenceFromInventory(holder, this.getUUID());
            }
        }

        this.holderHands.clear();
        this.ownerId = null;
        this.heldHand = 0;
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
        this.physicsProfileDirty = true;
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
        this.physicsProfileDirty = true;
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

        if (accessor == CARRIED_BLOCK_STATE) {
            BlockState state = Block.stateById(this.entityData.get(CARRIED_BLOCK_STATE));
            this.blockState = state == null ? Blocks.STONE.defaultBlockState() : state;
            this.physicsProfileDirty = true;
        }

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

        ListTag holdersTag = new ListTag();
        for (Map.Entry<UUID, Integer> entry : this.holderHands.entrySet()) {
            CompoundTag holderTag = new CompoundTag();
            holderTag.putUUID("Player", entry.getKey());
            holderTag.putInt("Hand", entry.getValue());
            holdersTag.add(holderTag);
        }
        tag.put("GravityHolders", holdersTag);

        if (this.ownerId != null) {
            tag.putUUID("GravityOwner", this.ownerId);
        }
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        BlockState loaded = Block.stateById(tag.getInt("CarriedBlockState"));
        setCarriedBlockState(loaded == null ? Blocks.STONE.defaultBlockState() : loaded);
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

        this.holderHands.clear();
        if (tag.contains("GravityHolders", Tag.TAG_LIST)) {
            ListTag holdersTag = tag.getList("GravityHolders", Tag.TAG_COMPOUND);

            for (int i = 0; i < holdersTag.size(); i++) {
                CompoundTag holderTag = holdersTag.getCompound(i);
                if (!holderTag.hasUUID("Player")) {
                    continue;
                }

                this.holderHands.put(holderTag.getUUID("Player"), Mth.clamp(holderTag.getInt("Hand"), 0, 1));
            }
        } else if (this.ownerId != null) {
            this.holderHands.put(this.ownerId, Mth.clamp(this.heldHand, 0, 1));
        }

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
        setCarriedBlockState(loaded == null ? Blocks.STONE.defaultBlockState() : loaded);
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
        return canBeGrabbedWithGlove();
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


    private final class PhysicsAccumulator {
        private int blockCount;
        private double totalMass;
        private double weightedHardness;
        private int slimeCount;
        private int honeyCount;
        private int iceCount;
        private int tntCount;
        private int magmaCount;
        private int logCount;
        private int glassCount;
        private int noteCount;

        private void accept(BlockState state, double hardness) {
            double safeHardness = Mth.clamp(hardness, 0.0D, EntropyChestplateGloveItem.MAX_PICKUP_HARDNESS);

            double mass = massFromHardness(safeHardness);

            this.blockCount++;
            this.totalMass += mass;
            this.weightedHardness += safeHardness * mass;

            if (isSlimeState(state)) this.slimeCount++;
            if (isHoneyState(state)) this.honeyCount++;
            if (isIceState(state)) this.iceCount++;
            if (isTntState(state)) this.tntCount++;
            if (isMagmaState(state)) this.magmaCount++;
            if (isLogState(state)) this.logCount++;
            if (isFragileGlassState(state)) this.glassCount++;
            if (isNoteState(state)) this.noteCount++;
        }

        private StructurePhysicsProfile finish() {
            int count = Math.max(1, this.blockCount);
            double mass = Math.max(0.01D, this.totalMass);

            return new StructurePhysicsProfile(count, mass, this.weightedHardness / mass, this.slimeCount, this.honeyCount, this.iceCount, this.tntCount, this.magmaCount, this.logCount, this.glassCount, this.noteCount, this.slimeCount / (double) count, this.honeyCount / (double) count, this.iceCount / (double) count, this.logCount / (double) count);
        }
    }

    private record StructurePhysicsProfile(int blockCount, double totalMass, double averageHardness, int slimeCount,
                                           int honeyCount, int iceCount, int tntCount, int magmaCount, int logCount,
                                           int glassCount, int noteCount, double slimeRatio, double honeyRatio,
                                           double iceRatio, double logRatio) {
    }

    private record HolderControl(ServerPlayer player, int handIndex) {
    }

    public record AttachmentPreview(BlockPos targetOffset, Direction face) {
    }

    public record DetachmentPreview(BlockPos sourceOffset) {
    }

    private record StructureRayHit(BlockPos offset, Direction face, double distanceSqr) {
    }

    private record AxisClip(double enter, double exit, Direction enterFace, Direction exitFace) {
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
