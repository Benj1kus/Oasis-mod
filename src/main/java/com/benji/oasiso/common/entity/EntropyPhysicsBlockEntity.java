package com.benji.oasiso.common.entity;

import com.benji.oasiso.Oasiso;
import com.benji.oasiso.common.item.EntropyChestplateGloveItem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.nbt.CompoundTag;
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
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.NoteBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.entity.IEntityAdditionalSpawnData;
import net.minecraftforge.network.NetworkHooks;

import java.util.Set;
import java.util.UUID;

public class EntropyPhysicsBlockEntity extends Entity implements IEntityAdditionalSpawnData {

    public static final int MODE_PULLING = 0;
    public static final int MODE_HELD = 1;
    public static final int MODE_DROPPED = 2;
    public static final int MODE_THROWN = 3;

    private static final EntityDataAccessor<Integer> MODE = SynchedEntityData.defineId(EntropyPhysicsBlockEntity.class, EntityDataSerializers.INT);

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
    private static final Set<Block> FRAGILE_GLASS_BLOCKS = Set.of(Blocks.GLASS, Blocks.GLASS_PANE, Blocks.WHITE_STAINED_GLASS, Blocks.WHITE_STAINED_GLASS_PANE, Blocks.ORANGE_STAINED_GLASS, Blocks.ORANGE_STAINED_GLASS_PANE, Blocks.MAGENTA_STAINED_GLASS, Blocks.MAGENTA_STAINED_GLASS_PANE, Blocks.LIGHT_BLUE_STAINED_GLASS, Blocks.LIGHT_BLUE_STAINED_GLASS_PANE, Blocks.YELLOW_STAINED_GLASS, Blocks.YELLOW_STAINED_GLASS_PANE, Blocks.LIME_STAINED_GLASS, Blocks.LIME_STAINED_GLASS_PANE, Blocks.PINK_STAINED_GLASS, Blocks.PINK_STAINED_GLASS_PANE, Blocks.GRAY_STAINED_GLASS, Blocks.GRAY_STAINED_GLASS_PANE, Blocks.LIGHT_GRAY_STAINED_GLASS, Blocks.LIGHT_GRAY_STAINED_GLASS_PANE, Blocks.CYAN_STAINED_GLASS, Blocks.CYAN_STAINED_GLASS_PANE, Blocks.PURPLE_STAINED_GLASS, Blocks.PURPLE_STAINED_GLASS_PANE, Blocks.BLUE_STAINED_GLASS, Blocks.BLUE_STAINED_GLASS_PANE, Blocks.BROWN_STAINED_GLASS, Blocks.BROWN_STAINED_GLASS_PANE, Blocks.GREEN_STAINED_GLASS, Blocks.GREEN_STAINED_GLASS_PANE, Blocks.RED_STAINED_GLASS, Blocks.RED_STAINED_GLASS_PANE, Blocks.BLACK_STAINED_GLASS, Blocks.BLACK_STAINED_GLASS_PANE);

    private BlockState blockState = Blocks.STONE.defaultBlockState();
    private float sourceHardness = 1.5F;
    private CompoundTag blockEntityData;

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

    public EntropyPhysicsBlockEntity(EntityType<? extends EntropyPhysicsBlockEntity> type, Level level) {
        super(type, level);
    }

    public void initializeFromBlock(BlockState state, float hardness, CompoundTag blockEntityData, ServerPlayer owner, InteractionHand hand, BlockPos sourcePos) {
        this.blockState = state;
        this.sourceHardness = Math.max(0.0F, hardness);
        this.blockEntityData = blockEntityData == null ? null : blockEntityData.copy();
        this.ownerId = owner.getUUID();
        this.heldHand = hand == InteractionHand.MAIN_HAND ? 0 : 1;
        this.pullTicks = PULL_SHAKE_TICKS;
        this.setMode(MODE_PULLING);

        this.setPos(sourcePos.getX() + 0.5D, sourcePos.getY(), sourcePos.getZ() + 0.5D);

        this.setDeltaMovement(Vec3.ZERO);
        this.noPhysics = true;
        this.setNoGravity(true);
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(MODE, MODE_PULLING);
    }

    public int getMode() {
        return this.entityData.get(MODE);
    }

    private void setMode(int mode) {
        this.entityData.set(MODE, mode);
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
        }
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

        this.setDeltaMovement(velocity);
        this.move(MoverType.SELF, velocity);

        Vec3 actualMovement = this.position().subtract(start);
        boolean hitX = Math.abs(actualMovement.x - velocity.x) > 1.0E-4D;
        boolean hitY = Math.abs(actualMovement.y - velocity.y) > 1.0E-4D;
        boolean hitZ = Math.abs(actualMovement.z - velocity.z) > 1.0E-4D;

        boolean collided = hitX || hitY || hitZ;

        if (isMagmaBlock()) {
            igniteTouchedBlocks(level);
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

        if (this.onGround()) {
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
        if (this.onGround() && speedSqr <= settleThreshold) {
            this.settleTicks++;
        } else {
            this.settleTicks = 0;
        }

        if (this.settleTicks >= SETTLE_REQUIRED_TICKS || this.freeTicks >= MAX_FREE_TICKS) {
            tryPlaceBack(level);
        }
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
        return Mth.clamp(5.0F + this.sourceHardness * 1.5F, 5.0F, 30.0F);
    }

    private double getHardnessFactor() {
        double capped = Mth.clamp(this.sourceHardness, 0.0F, 50.0F);
        return Math.log1p(capped) / Math.log1p(50.0D);
    }

    private double getThrowSpeed() {
        return Mth.lerp(getHardnessFactor(), THROW_SPEED_SOFT, THROW_SPEED_HARD);
    }

    private double getBounceCoefficient() {
        if (isSlimeBlock()) {
            return SLIME_BOUNCE;
        }

        double hardnessBounce = Mth.lerp(getHardnessFactor(), SOFT_BLOCK_BOUNCE, HARD_BLOCK_BOUNCE);
        if (isLogBlock()) {
            return Math.max(hardnessBounce, LOG_MIN_BOUNCE);
        }

        return hardnessBounce;
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

        Vec3 inherited = owner == null ? Vec3.ZERO : owner.getDeltaMovement().scale(0.35D);

        this.setDeltaMovement(inherited);

        if (this.level() instanceof ServerLevel level) {
            level.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.PISTON_CONTRACT, SoundSource.PLAYERS, 0.55F, 1.45F);
        }
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

    private void restoreBlockEntityData(ServerLevel level, BlockPos pos) {
        if (this.blockEntityData == null) {
            return;
        }

        BlockEntity restored = level.getBlockEntity(pos);
        if (restored == null) {
            return;
        }

        CompoundTag data = this.blockEntityData.copy();
        data.putInt("x", pos.getX());
        data.putInt("y", pos.getY());
        data.putInt("z", pos.getZ());

        restored.load(data);
        restored.setChanged();
        level.sendBlockUpdated(pos, this.blockState, this.blockState, Block.UPDATE_ALL);
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
    }

    private void tickVisualRotation() {
        this.visualYawO = this.visualYaw;
        this.visualPitchO = this.visualPitch;
        this.visualRollO = this.visualRoll;

        int mode = getMode();
        double speed = this.getDeltaMovement().length();

        if (mode == MODE_PULLING) {
            this.visualYaw += 3.0F;
            this.visualPitch = 12.0F + (float) Math.sin(this.tickCount * 0.55D) * 4.0F;
            this.visualRoll = 10.0F + (float) Math.cos(this.tickCount * 0.42D) * 3.0F;
            return;
        }

        if (mode == MODE_HELD) {
            this.visualYaw += 2.1F;
            this.visualPitch = 18.0F + (float) Math.sin(this.tickCount * 0.12D) * 4.5F;
            this.visualRoll = 12.0F + (float) Math.cos(this.tickCount * 0.10D) * 3.5F;
            return;
        }

        float spin = (float) Mth.clamp(5.0D + speed * 11.0D, 5.0D, 24.0D);
        this.visualYaw += spin * 0.74F;
        this.visualPitch += spin;
        this.visualRoll += spin * 0.61F;
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putInt("CarriedBlockState", Block.getId(this.blockState));
        tag.putFloat("SourceHardness", this.sourceHardness);
        tag.putInt("GravityMode", getMode());
        tag.putInt("HeldHand", this.heldHand);
        tag.putInt("PullTicks", this.pullTicks);
        tag.putInt("SettleTicks", this.settleTicks);
        tag.putInt("FreeTicks", this.freeTicks);
        tag.putInt("NoteSequence", this.noteSequence);

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
        this.heldHand = tag.getInt("HeldHand");
        this.pullTicks = tag.getInt("PullTicks");
        this.settleTicks = tag.getInt("SettleTicks");
        this.freeTicks = tag.getInt("FreeTicks");
        this.noteSequence = tag.getInt("NoteSequence");
        this.blockEntityData = tag.contains("CarriedBlockEntityData") ? tag.getCompound("CarriedBlockEntityData").copy() : null;
        this.ownerId = tag.hasUUID("GravityOwner") ? tag.getUUID("GravityOwner") : null;

        boolean heldLike = getMode() == MODE_PULLING || getMode() == MODE_HELD;
        this.noPhysics = heldLike;
        this.setNoGravity(true);
    }

    @Override
    public void writeSpawnData(FriendlyByteBuf buffer) {
        buffer.writeVarInt(Block.getId(this.blockState));
        buffer.writeFloat(this.sourceHardness);
    }

    @Override
    public void readSpawnData(FriendlyByteBuf buffer) {
        BlockState loaded = Block.stateById(buffer.readVarInt());
        this.blockState = loaded == null ? Blocks.STONE.defaultBlockState() : loaded;
        this.sourceHardness = buffer.readFloat();
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public boolean hurt(net.minecraft.world.damagesource.DamageSource source, float amount) {
        return false;
    }

}
