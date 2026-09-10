package com.benji.oasiso.common.block.entity;

import com.benji.oasiso.Oasiso;
import com.benji.oasiso.common.entity.EntropyPhysicsBlockEntity;
import com.benji.oasiso.registry.ModBlockEntities;
import com.benji.oasiso.registry.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import com.benji.oasiso.ModSounds;
import net.minecraft.server.level.ServerPlayer;

import java.util.*;

public class EntropyConnectorBlockEntity extends BlockEntity {

    public static final int MIN_DISTANCE = 2;
    public static final int MAX_DISTANCE = 10;
    public static final int PHASE_LAUNCHING = 0;
    public static final int PHASE_PULLING = 1;
    private static final int MIN_LAUNCH_TICKS = 4;
    private static final int MAX_LAUNCH_TICKS = 10;
    private static final int MAX_PHYSICAL_PULL_TICKS = 20 * 10;
    private static final double DOCK_DISTANCE = 0.42D;
    private static final double DOCK_DISTANCE_SQR = DOCK_DISTANCE * DOCK_DISTANCE;
    private static final int MAX_CONNECTED_STRUCTURE_BLOCKS = 256;

    private static final int CONNECTION_VALIDATION_INTERVAL = 20;
    private static final int PLATFORM_HINT_TICKS = 60;

    private final EnumMap<Direction, PullState> activePulls = new EnumMap<>(Direction.class);
    private final EnumSet<Direction> connectedSides = EnumSet.noneOf(Direction.class);

    private int connectionValidationTicks;
    private long platformHintUntil;

    public EntropyConnectorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ENTROPY_CONNECTOR_BE.get(), pos, state);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, EntropyConnectorBlockEntity connector) {
        connector.tickServer();
    }

    private void tickServer() {
        if (!(this.level instanceof ServerLevel serverLevel)) {
            return;
        }

        boolean changed = tickConnectionValidation(serverLevel);

        if (this.activePulls.isEmpty()) {

            if (changed) {
                sync();
            }

            return;
        }

        Iterator<Map.Entry<Direction, PullState>> iterator = this.activePulls.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<Direction, PullState> entry = iterator.next();
            PullState pull = entry.getValue();
            long age = serverLevel.getGameTime() - pull.phaseStartGameTime();
            if (pull.phase() == PHASE_LAUNCHING) {

                if (age < pull.durationTicks()) {
                    continue;
                }

                PullState physicalPull = beginPhysicalPull(serverLevel, pull);

                if (physicalPull == null) {
                    iterator.remove();
                } else {
                    entry.setValue(physicalPull);
                }
                changed = true;
                continue;
            }
            EntropyPhysicsBlockEntity physics = resolvePhysicsEntity(serverLevel, pull);

            if (physics == null) {
                if (age > 40) {
                    iterator.remove();
                    changed = true;
                }

                continue;
            }

            if (!physics.isConnectorPullMode()) {
                iterator.remove();
                changed = true;
                continue;
            }

            BlockPos dockPos = this.worldPosition.relative(pull.side());
            Vec3 dockEntityPosition = new Vec3(dockPos.getX() + 0.5D, dockPos.getY(), dockPos.getZ() + 0.5D);
            if (physics.position().distanceToSqr(dockEntityPosition) <= DOCK_DISTANCE_SQR) {

                if (physics.finishConnectorDock(serverLevel, dockPos)) {

                    registerDockedConnection(pull.side(), dockPos);
                    playDockFx(dockPos);
                    iterator.remove();
                    changed = true;
                }
                continue;
            }
            if (age >= MAX_PHYSICAL_PULL_TICKS) {

                physics.abortConnectorPull();

                iterator.remove();
                changed = true;
            }
        }

        if (changed) {
            sync();
        }
    }

    public void showPlatformHint() {
        if (this.level == null || this.level.isClientSide) {
            return;
        }

        this.platformHintUntil = this.level.getGameTime() + PLATFORM_HINT_TICKS;

        sync();
    }

    public float getPlatformHintAlpha(float partialTick) {
        if (this.level == null) {
            return 0.0F;
        }

        double now = this.level.getGameTime() + partialTick;
        double remaining = this.platformHintUntil - now;

        if (remaining <= 0.0D) {
            return 0.0F;
        }

        double age = PLATFORM_HINT_TICKS - remaining;
        float fadeIn = Mth.clamp((float) age / 5.0F, 0.0F, 1.0F);
        float fadeOut = Mth.clamp((float) remaining / 10.0F, 0.0F, 1.0F);
        return Math.min(fadeIn, fadeOut);
    }

    public boolean activatePlatform(ServerPlayer player) {
        if (!(this.level instanceof ServerLevel serverLevel)) {

            return false;
        }
        if (!this.activePulls.isEmpty()) {
            return false;
        }

        CapturedStructure structure = captureConnectedStructure(serverLevel, this.worldPosition);
        if (structure == null || structure.blocks().isEmpty()) {

            return false;
        }

        CapturedBlock root = structure.root();
        List<EntropyPhysicsBlockEntity.ConnectorStructurePart> structureParts = new ArrayList<>();

        for (CapturedBlock block : structure.blocks()) {

            if (block.pos().equals(structure.rootPos())) {
                continue;
            }

            BlockPos offset = block.pos().subtract(structure.rootPos());
            structureParts.add(new EntropyPhysicsBlockEntity.ConnectorStructurePart(offset, block.state(), block.blockEntityData()));
        }

        float hardness = root.state().getDestroySpeed(serverLevel, root.pos());
        EntropyPhysicsBlockEntity platform = new EntropyPhysicsBlockEntity(ModEntities.ENTROPY_PHYSICS_BLOCK.get(), serverLevel);
        platform.initializeAsMovingPlatform(root.state(), hardness, root.blockEntityData(), structure.rootPos(), structureParts);
        removeCapturedStructure(serverLevel, structure);

        if (!serverLevel.addFreshEntity(platform)) {
            restoreCapturedStructure(serverLevel, structure);
            return false;
        }

        Vec3 center = Vec3.atCenterOf(this.worldPosition);
        serverLevel.sendParticles(Oasiso.WIZARD_PIXELS.get(), center.x, center.y, center.z, 22, 0.38D, 0.38D, 0.38D, 0.025D);
        serverLevel.playSound(null, this.worldPosition, ModSounds.AZUMAAL_IDLE1.get(), SoundSource.BLOCKS, 0.85F, 1.10F);

        return true;
    }

    private boolean tickConnectionValidation(ServerLevel level) {
        this.connectionValidationTicks++;
        if (this.connectionValidationTicks < CONNECTION_VALIDATION_INTERVAL) {
            return false;
        }

        this.connectionValidationTicks = 0;

        return validateConnectedSides(level);
    }

    private boolean validateConnectedSides(ServerLevel level) {
        boolean changed = false;
        Iterator<Direction> iterator = this.connectedSides.iterator();

        while (iterator.hasNext()) {
            Direction side = iterator.next();

            BlockPos neighbourPos = this.worldPosition.relative(side);
            BlockState neighbourState = level.getBlockState(neighbourPos);

            if (neighbourState.isAir()) {
                iterator.remove();
                changed = true;
                continue;
            }
            if (neighbourState.is(com.benji.oasiso.registry.ModBlocks.ENTROPY_CONNECTOR.get())) {
                if (!(level.getBlockEntity(neighbourPos) instanceof EntropyConnectorBlockEntity other) || !other.connectedSides.contains(side.getOpposite())) {
                    iterator.remove();
                    changed = true;
                }
            }
        }

        return changed;
    }

    private void registerDockedConnection(Direction side, BlockPos dockPos) {
        this.connectedSides.add(side);
        if (this.level != null && this.level.getBlockEntity(dockPos) instanceof EntropyConnectorBlockEntity otherConnector) {
            otherConnector.connectedSides.add(side.getOpposite());
            otherConnector.sync();
        }

        sync();
    }

    public boolean tryStartPull(Direction side, BlockPos sourcePos) {
        if (this.level == null || this.level.isClientSide) {
            return false;
        }

        if (!side.getAxis().isHorizontal()) {
            return false;
        }

        if (!canAcceptOnSide(side)) {
            return false;
        }

        int distance = axisDistance(side, sourcePos);

        if (distance < MIN_DISTANCE || distance > MAX_DISTANCE) {

            return false;
        }

        if (!isExactlyOnAxis(side, sourcePos)) {
            return false;
        }

        if (!isPathClear(side, distance, sourcePos)) {
            return false;
        }

        BlockState sourceState = this.level.getBlockState(sourcePos);

        if (sourceState.isAir()) {
            return false;
        }

        if (sourceState.getDestroySpeed(this.level, sourcePos) < 0.0F) {

            return false;
        }

        CompoundTag blockEntityData = null;
        BlockEntity sourceBlockEntity = this.level.getBlockEntity(sourcePos);

        if (!(sourceBlockEntity instanceof EntropyConnectorBlockEntity) && isClaimedByConnector(sourcePos)) {
            return false;
        }

        if (sourceBlockEntity instanceof EntropyConnectorBlockEntity otherConnector && !otherConnector.activePulls.isEmpty()) {

            return false;
        }

        if (sourceBlockEntity != null) {
            blockEntityData = sourceBlockEntity.saveWithoutMetadata();
        }

        int launchTicks = Mth.clamp(2 + distance, MIN_LAUNCH_TICKS, MAX_LAUNCH_TICKS);
        PullState pull = new PullState(side, sourcePos.immutable(), sourceState, blockEntityData, PHASE_LAUNCHING, this.level.getGameTime(), launchTicks, null);
        this.activePulls.put(side, pull);

        playLaunchFx();

        sync();

        return true;
    }

    private boolean isClaimedByConnector(BlockPos blockPos) {
        if (this.level == null) {
            return false;
        }
        for (Direction fromBlock : Direction.Plane.HORIZONTAL) {
            BlockPos connectorPos = blockPos.relative(fromBlock);
            if (!(this.level.getBlockEntity(connectorPos) instanceof EntropyConnectorBlockEntity connector)) {
                continue;
            }
            Direction connectorToBlock = fromBlock.getOpposite();
            if (connector.connectedSides.contains(connectorToBlock)) {
                return true;
            }
        }

        return false;
    }

    @Nullable
    private CapturedStructure captureConnectedStructure(ServerLevel level, BlockPos rootPos) {
        LinkedHashMap<BlockPos, CapturedBlock> blocks = new LinkedHashMap<>();
        ArrayDeque<BlockPos> connectorQueue = new ArrayDeque<>();
        Set<BlockPos> visitedConnectors = new HashSet<>();
        connectorQueue.add(rootPos.immutable());

        while (!connectorQueue.isEmpty()) {
            BlockPos connectorPos = connectorQueue.removeFirst();
            if (!visitedConnectors.add(connectorPos)) {
                continue;
            }
            if (!rootPos.equals(this.worldPosition) && connectorPos.equals(this.worldPosition)) {
                return null;
            }

            if (!(level.getBlockEntity(connectorPos) instanceof EntropyConnectorBlockEntity connector)) {
                return null;
            }
            if (connector.hasActivePulls()) {
                return null;
            }
            connector.validateConnectedSides(level);
            captureBlock(level, connectorPos, blocks);

            for (Direction side : Direction.Plane.HORIZONTAL) {
                if (!connector.connectedSides.contains(side)) {
                    continue;
                }

                BlockPos neighbourPos = connectorPos.relative(side);
                BlockState neighbourState = level.getBlockState(neighbourPos);

                if (neighbourState.isAir()) {
                    continue;
                }
                if (neighbourState.is(com.benji.oasiso.registry.ModBlocks.ENTROPY_CONNECTOR.get())) {
                    if (!(level.getBlockEntity(neighbourPos) instanceof EntropyConnectorBlockEntity other)) {
                        continue;
                    }
                    if (!other.connectedSides.contains(side.getOpposite())) {
                        continue;
                    }
                    captureBlock(level, neighbourPos, blocks);
                    connectorQueue.addLast(neighbourPos.immutable());

                } else {
                    captureBlock(level, neighbourPos, blocks);
                }
                if (blocks.size() > MAX_CONNECTED_STRUCTURE_BLOCKS) {
                    return null;
                }
            }
        }
        CapturedBlock root = blocks.get(rootPos);
        if (root == null) {
            return null;
        }
        return new CapturedStructure(rootPos.immutable(), root, new ArrayList<>(blocks.values()));
    }

    private static void captureBlock(ServerLevel level, BlockPos pos, Map<BlockPos, CapturedBlock> blocks) {
        BlockPos immutable = pos.immutable();
        if (blocks.containsKey(immutable)) {
            return;
        }
        BlockState state = level.getBlockState(immutable);
        if (state.isAir()) {
            return;
        }
        CompoundTag blockEntityData = null;
        BlockEntity blockEntity = level.getBlockEntity(immutable);

        if (blockEntity != null) {
            blockEntityData = blockEntity.saveWithoutMetadata();
        }
        blocks.put(immutable, new CapturedBlock(immutable, state, blockEntityData));
    }

    private boolean canMoveStructureToDock(ServerLevel level, CapturedStructure structure, BlockPos dockRoot) {
        BlockPos translation = dockRoot.subtract(structure.rootPos());
        Set<BlockPos> sourcePositions = new HashSet<>();

        for (CapturedBlock block : structure.blocks()) {
            sourcePositions.add(block.pos());
        }
        for (CapturedBlock block : structure.blocks()) {

            BlockPos destination = block.pos().offset(translation);
            if (sourcePositions.contains(destination)) {
                continue;
            }
            if (!level.getBlockState(destination).canBeReplaced()) {
                return false;
            }
        }

        return true;
    }

    @Nullable
    private PullState beginPhysicalPull(ServerLevel level, PullState pull) {
        BlockPos sourcePos = pull.sourcePos();
        BlockState current = level.getBlockState(sourcePos);

        if (!current.equals(pull.movedState())) {
            return null;
        }

        int distance = axisDistance(pull.side(), sourcePos);
        if (!isPathClear(pull.side(), distance, sourcePos)) {
            return null;
        }

        CapturedStructure structure;

        if (current.is(com.benji.oasiso.registry.ModBlocks.ENTROPY_CONNECTOR.get())) {

            structure = captureConnectedStructure(level, sourcePos);

            if (structure == null) {
                return null;
            }

            if (level.getBlockEntity(sourcePos) instanceof EntropyConnectorBlockEntity sourceConnector && sourceConnector.isSideConnected(pull.side().getOpposite())) {

                return null;
            }

        } else {

            LinkedHashMap<BlockPos, CapturedBlock> single = new LinkedHashMap<>();
            captureBlock(level, sourcePos, single);
            CapturedBlock root = single.get(sourcePos);

            if (root == null) {
                return null;
            }
            structure = new CapturedStructure(sourcePos.immutable(), root, new ArrayList<>(single.values()));
        }

        BlockPos dockRoot = this.worldPosition.relative(pull.side());

        if (!canMoveStructureToDock(level, structure, dockRoot)) {

            return null;
        }

        CapturedBlock root = structure.root();
        float hardness = root.state().getDestroySpeed(level, root.pos());
        List<EntropyPhysicsBlockEntity.ConnectorStructurePart> structureParts = new ArrayList<>();

        for (CapturedBlock block : structure.blocks()) {
            if (block.pos().equals(structure.rootPos())) {
                continue;
            }

            BlockPos offset = block.pos().subtract(structure.rootPos());
            structureParts.add(new EntropyPhysicsBlockEntity.ConnectorStructurePart(offset, block.state(), block.blockEntityData()));
        }

        EntropyPhysicsBlockEntity physics = new EntropyPhysicsBlockEntity(ModEntities.ENTROPY_PHYSICS_BLOCK.get(), level);
        physics.initializeForConnectorPull(root.state(), hardness, root.blockEntityData(), structure.rootPos(), this.worldPosition, pull.side(), structureParts);

        removeCapturedStructure(level, structure);

        if (!level.addFreshEntity(physics)) {
            restoreCapturedStructure(level, structure);

            return null;
        }
        playLatchFx(sourcePos);
        return new PullState(pull.side(), pull.sourcePos(), root.state(), root.blockEntityData(), PHASE_PULLING, level.getGameTime(), MAX_PHYSICAL_PULL_TICKS, physics.getUUID());
    }

    private static void removeCapturedStructure(ServerLevel level, CapturedStructure structure) {
        for (CapturedBlock block : structure.blocks()) {
            level.removeBlockEntity(block.pos());
        }
        for (CapturedBlock block : structure.blocks()) {
            level.setBlock(block.pos(), Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL | Block.UPDATE_SUPPRESS_DROPS);
        }
    }

    private static void restoreCapturedStructure(ServerLevel level, CapturedStructure structure) {
        for (CapturedBlock block : structure.blocks()) {
            level.setBlock(block.pos(), block.state(), Block.UPDATE_ALL | Block.UPDATE_SUPPRESS_DROPS);
        }
        for (CapturedBlock block : structure.blocks()) {
            if (block.blockEntityData() == null) {
                continue;
            }
            restoreBlock(level, block.pos(), block.state(), block.blockEntityData());
        }
    }

    public boolean canAcceptOnSide(Direction side) {
        if (this.level == null || !side.getAxis().isHorizontal()) {
            return false;
        }

        if (this.activePulls.containsKey(side)) {
            return false;
        }

        BlockPos dockPos = this.worldPosition.relative(side);

        return this.level.getBlockState(dockPos).isAir();
    }

    public boolean shouldShowArrow(Direction side) {
        if (this.level == null || !side.getAxis().isHorizontal()) {
            return false;
        }

        if (this.activePulls.containsKey(side)) {
            return false;
        }

        return this.level.getBlockState(this.worldPosition.relative(side)).isAir();
    }

    private record CapturedBlock(
            BlockPos pos,
            BlockState state,
            @Nullable CompoundTag blockEntityData
    ) {
    }

    private record CapturedStructure(
            BlockPos rootPos,
            CapturedBlock root,
            List<CapturedBlock> blocks
    ) {
    }

    public Iterable<PullState> getActivePulls() {
        return this.activePulls.values();
    }


    public boolean isSideConnected(Direction side) {
        return side != null && side.getAxis().isHorizontal() && this.connectedSides.contains(side);
    }

    public boolean hasActivePulls() {
        return !this.activePulls.isEmpty();
    }

    private void connectSide(Direction side) {
        if (side == null || !side.getAxis().isHorizontal()) {
            return;
        }

        if (this.connectedSides.add(side)) {
            sync();
        }
    }

    private void disconnectSide(Direction side) {
        if (side == null) {
            return;
        }

        if (this.connectedSides.remove(side)) {
            sync();
        }
    }

    @Nullable
    private EntropyPhysicsBlockEntity resolvePhysicsEntity(ServerLevel level, PullState pull) {
        UUID uuid = pull.physicsEntityId();

        if (uuid == null) {
            return null;
        }

        if (level.getEntity(uuid) instanceof EntropyPhysicsBlockEntity physics) {
            return physics;
        }
        return null;
    }

    private boolean isPathClear(Direction side, int distance, BlockPos sourcePos) {
        if (this.level == null) {
            return false;
        }
        for (int i = 1; i < distance; i++) {
            BlockPos check = this.worldPosition.relative(side, i);
            if (check.equals(sourcePos)) {
                continue;
            }
            if (!this.level.getBlockState(check).isAir()) {
                return false;
            }
        }

        return true;
    }

    private boolean isExactlyOnAxis(Direction side, BlockPos sourcePos) {
        int distance = axisDistance(side, sourcePos);

        if (distance <= 0) {
            return false;
        }

        return this.worldPosition.relative(side, distance).equals(sourcePos);
    }

    private int axisDistance(Direction side, BlockPos other) {
        BlockPos delta = other.subtract(this.worldPosition);

        return switch (side) {
            case NORTH -> -delta.getZ();
            case SOUTH -> delta.getZ();
            case WEST -> -delta.getX();
            case EAST -> delta.getX();
            default -> 0;
        };
    }

    private void playLaunchFx() {
        if (!(this.level instanceof ServerLevel level)) {
            return;
        }
        level.playSound(null, this.worldPosition, SoundEvents.SLIME_SQUISH_SMALL, SoundSource.BLOCKS, 0.55F, 1.25F);
    }

    private void playLatchFx(BlockPos pos) {
        if (!(this.level instanceof ServerLevel level)) {
            return;
        }

        level.playSound(null, pos, SoundEvents.SLIME_JUMP_SMALL, SoundSource.BLOCKS, 0.85F, 1.15F);
        spawnWizardPixels(level, Vec3.atCenterOf(pos));
    }

    private void playDockFx(BlockPos pos) {
        if (!(this.level instanceof ServerLevel level)) {

            return;
        }

        level.playSound(null, pos, SoundEvents.SLIME_BLOCK_PLACE, SoundSource.BLOCKS, 1.0F, 0.95F);
        spawnWizardPixels(level, Vec3.atCenterOf(pos));
        spawnWizardPixels(level, Vec3.atCenterOf(this.worldPosition));
    }

    private static void spawnWizardPixels(ServerLevel level, Vec3 center) {
        SimpleParticleType particle = Oasiso.WIZARD_PIXELS.get();
        level.sendParticles(particle, center.x, center.y, center.z, 14, 0.22D, 0.22D, 0.22D, 0.015D);
    }

    private static void restoreBlock(ServerLevel level, BlockPos pos, BlockState state, @Nullable CompoundTag blockEntityData) {
        level.setBlock(pos, state, Block.UPDATE_ALL);

        if (blockEntityData == null) {
            return;
        }

        BlockEntity restored = level.getBlockEntity(pos);
        if (restored == null) {
            return;
        }

        CompoundTag data = blockEntityData.copy();
        data.putInt("x", pos.getX());
        data.putInt("y", pos.getY());
        data.putInt("z", pos.getZ());

        restored.load(data);
        restored.setChanged();

        level.sendBlockUpdated(pos, state, state, Block.UPDATE_ALL);
    }

    private void sync() {
        this.setChanged();
        if (this.level == null || this.level.isClientSide) {
            return;
        }
        BlockState state = this.getBlockState();
        this.level.sendBlockUpdated(this.worldPosition, state, state, Block.UPDATE_CLIENTS);
    }

    @Override
    public CompoundTag getUpdateTag() {
        CompoundTag tag = new CompoundTag();
        saveAdditional(tag);
        return tag;
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);

        ListTag pulls = new ListTag();

        for (PullState pull : this.activePulls.values()) {

            CompoundTag pullTag = new CompoundTag();

            pullTag.putInt("Side", pull.side().get3DDataValue());
            pullTag.putLong("SourcePos", pull.sourcePos().asLong());
            pullTag.putInt("MovedStateId", Block.getId(pull.movedState()));
            pullTag.putInt("Phase", pull.phase());
            pullTag.putLong("PhaseStartGameTime", pull.phaseStartGameTime());
            pullTag.putInt("DurationTicks", pull.durationTicks());

            if (pull.movedBlockEntityTag() != null) {
                pullTag.put("MovedBlockEntity", pull.movedBlockEntityTag().copy());
            }

            if (pull.physicsEntityId() != null) {
                pullTag.putUUID("PhysicsEntity", pull.physicsEntityId());
            }

            pulls.add(pullTag);
        }

        int connectionMask = 0;

        for (Direction side : Direction.Plane.HORIZONTAL) {
            if (this.connectedSides.contains(side)) {
                connectionMask |= 1 << side.get2DDataValue();
            }
        }

        tag.putLong("PlatformHintUntil", this.platformHintUntil);

        tag.putInt("ConnectedSides", connectionMask);

        tag.put("ActivePulls", pulls);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);

        this.connectedSides.clear();

        int connectionMask = tag.getInt("ConnectedSides");
        for (Direction side : Direction.Plane.HORIZONTAL) {
            if ((connectionMask & (1 << side.get2DDataValue())) != 0) {
                this.connectedSides.add(side);
            }
        }

        this.platformHintUntil = tag.getLong("PlatformHintUntil");
        this.activePulls.clear();

        if (!tag.contains("ActivePulls", Tag.TAG_LIST)) {
            return;
        }

        ListTag pulls = tag.getList("ActivePulls", Tag.TAG_COMPOUND);
        for (int i = 0; i < pulls.size(); i++) {

            CompoundTag pullTag = pulls.getCompound(i);
            Direction side = Direction.from3DDataValue(pullTag.getInt("Side"));

            if (!side.getAxis().isHorizontal()) {
                continue;
            }

            BlockPos sourcePos = BlockPos.of(pullTag.getLong("SourcePos"));
            BlockState movedState = Block.stateById(pullTag.getInt("MovedStateId"));

            if (movedState == null || movedState.isAir()) {

                continue;
            }

            CompoundTag movedBe = pullTag.contains("MovedBlockEntity", Tag.TAG_COMPOUND) ? pullTag.getCompound("MovedBlockEntity").copy() : null;
            UUID physicsEntity = pullTag.hasUUID("PhysicsEntity") ? pullTag.getUUID("PhysicsEntity") : null;
            PullState pull = new PullState(side, sourcePos, movedState, movedBe, pullTag.getInt("Phase"), pullTag.getLong("PhaseStartGameTime"), pullTag.getInt("DurationTicks"), physicsEntity);

            this.activePulls.put(side, pull);
        }
    }

    @Override
    public AABB getRenderBoundingBox() {
        return new AABB(this.worldPosition).inflate(MAX_DISTANCE + 2.0D);
    }

    public record PullState(Direction side, BlockPos sourcePos, BlockState movedState,
                            @Nullable CompoundTag movedBlockEntityTag, int phase, long phaseStartGameTime,
                            int durationTicks, @Nullable UUID physicsEntityId) {
    }
}