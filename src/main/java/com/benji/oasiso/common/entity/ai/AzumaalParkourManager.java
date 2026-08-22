package com.benji.oasiso.common.entity.ai;

import com.benji.oasiso.Oasiso;
import com.benji.oasiso.common.entity.AzumaalEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import com.benji.oasiso.ModSounds;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.templatesystem.BlockIgnoreProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public final class AzumaalParkourManager {
    //structure
    private static final ResourceLocation STRUCTURE_ID = ResourceLocation.fromNamespaceAndPath(Oasiso.MODID, "boss_parkour");


    private static final double PARKOUR_HEIGHT = 20.0D;

    private static final int RISE_TICKS = 30;
    private static final int HOLD_TICKS = 30 * 20;
    private static final int DESCEND_TICKS = 30;

    private static final double PARTICIPANT_RANGE = 96.0D;
    private static final int ENTROPY_DURATION = 20 * 20;

    private final AzumaalEntity boss;

    private Stage stage = Stage.NONE;

    private BlockPos structureOrigin;

    private int structureSizeX;
    private int structureSizeY;
    private int structureSizeZ;
    private int currentLayer;
    private int stageTick;

    private double anchorX;
    private double anchorZ;
    private double returnY;
    private double topY;

    private final Map<BlockPos, BlockState> originalBlocks = new HashMap<>();

    private final Set<UUID> participants = new HashSet<>();
    private final Set<UUID> successfulPlayers = new HashSet<>();

    public AzumaalParkourManager(AzumaalEntity boss) {
        this.boss = boss;
    }

    public boolean begin(ServerLevel level) {
        if (this.stage != Stage.NONE) {
            return false;
        }
        Optional<StructureTemplate> optional = level.getStructureManager().get(STRUCTURE_ID);
        if (optional.isEmpty()) {
            return false;
        }

        StructureTemplate template = optional.get();
        Vec3i size = template.getSize();
        if (size.getX() <= 0 || size.getY() <= 0 || size.getZ() <= 0) {
            return false;
        }

        this.structureSizeX = size.getX();
        this.structureSizeY = size.getY();
        this.structureSizeZ = size.getZ();

        int centerX = Mth.floor(boss.getX());
        int centerZ = Mth.floor(boss.getZ());

        int groundY = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, centerX, centerZ);

        this.structureOrigin = new BlockPos(centerX - this.structureSizeX / 2, groundY, centerZ - this.structureSizeZ / 2);

        this.anchorX = boss.getX();
        this.anchorZ = boss.getZ();
        this.returnY = boss.getHoverBaseY();
        this.topY = this.returnY + PARKOUR_HEIGHT;

        this.currentLayer = 0;
        this.stageTick = 0;

        this.originalBlocks.clear();
        this.participants.clear();
        this.successfulPlayers.clear();

        boss.setParkourActive(true);

        boss.setAnimState(AzumaalEntity.STATE_IDLE);
        boss.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);
        this.stage = Stage.BUILD;

        return true;
    }

    public boolean tick(ServerLevel level) {
        return switch (this.stage) {
            case BUILD -> {
                tickBuild(level);
                yield false;
            }
            case RISE -> {
                tickRise(level);
                yield false;
            }
            case HOLD -> {
                tickHold(level);
                yield false;
            }
            case DESCEND -> tickDescend(level);
            case NONE -> true;
        };
    }

    private void tickBuild(ServerLevel level) {
        lockBoss(this.returnY);
        if (this.currentLayer < this.structureSizeY) {
            placeLayer(level, this.currentLayer);
            this.currentLayer++;
        }
        if (this.currentLayer >= this.structureSizeY) {
            this.stage = Stage.RISE;
            this.stageTick = 0;
        }
    }

    private void placeLayer(ServerLevel level, int layer) {
        if (this.structureOrigin == null) {
            return;
        }
        Optional<StructureTemplate> optional = level.getStructureManager().get(STRUCTURE_ID);
        if (optional.isEmpty()) {
            return;
        }

        StructureTemplate template = optional.get();
        int worldY = this.structureOrigin.getY() + layer;

        Map<BlockPos, BlockState> beforeStates = new HashMap<>();

        for (int x = 0; x < this.structureSizeX; x++) {
            for (int z = 0; z < this.structureSizeZ; z++) {
                BlockPos pos = this.structureOrigin.offset(x, layer, z);
                beforeStates.put(pos.immutable(), level.getBlockState(pos));
            }
        }

        BoundingBox layerBox = new BoundingBox(this.structureOrigin.getX(), worldY, this.structureOrigin.getZ(), this.structureOrigin.getX() + this.structureSizeX - 1, worldY, this.structureOrigin.getZ() + this.structureSizeZ - 1);
        StructurePlaceSettings settings = new StructurePlaceSettings()
                .setIgnoreEntities(true)
                .addProcessor(BlockIgnoreProcessor.STRUCTURE_AND_AIR)
                .setBoundingBox(layerBox);

        template.placeInWorld(level, this.structureOrigin, this.structureOrigin, settings, level.getRandom(), 3);

        for (Map.Entry<BlockPos, BlockState> entry : beforeStates.entrySet()) {
            BlockPos pos = entry.getKey();
            BlockState before = entry.getValue();
            BlockState after = level.getBlockState(pos);
            if (after.equals(before)) {
                continue;
            }
            this.originalBlocks.putIfAbsent(pos, before);
        }
    }

    private void tickRise(ServerLevel level) {
        this.stageTick++;
        float progress = Mth.clamp(this.stageTick / (float) RISE_TICKS, 0.0F, 1.0F);

        progress = smoothstep(progress);
        double y = Mth.lerp(progress, this.returnY, this.topY);
        lockBoss(y);
        if (this.stageTick < RISE_TICKS) {
            return;
        }
        lockBoss(this.topY);
        collectParticipants(level);

        this.stage = Stage.HOLD;
        this.stageTick = 0;
    }

    private void tickHold(ServerLevel level) {
        this.stageTick++;
        lockBoss(this.topY);

        if (this.stageTick % 20 == 0) {
            playTimerSound(level);
        }

        if (this.stageTick < HOLD_TICKS) {
            return;
        }

        punishFailures(level);
        collapseStructure(level, true);

        this.stage = Stage.DESCEND;
        this.stageTick = 0;
    }

    private void playTimerSound(ServerLevel level) {
        for (UUID playerId : this.participants) {

            ServerPlayer player = level.getServer().getPlayerList().getPlayer(playerId);
            if (player == null || player.serverLevel() != level || !isValidParticipant(player)) {
                continue;
            }

            player.playNotifySound(ModSounds.TIMER.get(), SoundSource.HOSTILE, 1.25F, 1.0F);
        }
    }

    private boolean tickDescend(ServerLevel level) {
        this.stageTick++;

        float progress = Mth.clamp(this.stageTick / (float) DESCEND_TICKS, 0.0F, 1.0F);

        progress = smoothstep(progress);

        double y = Mth.lerp(progress, this.topY, this.returnY);

        lockBoss(y);

        if (this.stageTick < DESCEND_TICKS) {
            return false;
        }

        lockBoss(this.returnY);

        boss.setParkourActive(false);

        this.stage = Stage.NONE;
        this.stageTick = 0;
        this.currentLayer = 0;
        this.participants.clear();
        this.successfulPlayers.clear();
        this.originalBlocks.clear();
        this.structureOrigin = null;

        return true;
    }

    public void registerMeleeHit(net.minecraft.world.entity.player.Player player) {
        if (this.stage != Stage.HOLD) {
            return;
        }

        if (!this.participants.contains(player.getUUID())) {
            return;
        }
        this.successfulPlayers.add(player.getUUID());
    }

    private void collectParticipants(ServerLevel level) {
        this.participants.clear();
        this.successfulPlayers.clear();

        double rangeSqr = PARTICIPANT_RANGE * PARTICIPANT_RANGE;

        for (ServerPlayer player : level.players()) {
            if (!isValidParticipant(player)) {
                continue;
            }

            if (boss.distanceToSqr(player) > rangeSqr) {
                continue;
            }

            this.participants.add(player.getUUID());
        }
    }

    private void punishFailures(ServerLevel level) {
        for (UUID playerId : this.participants) {

            if (this.successfulPlayers.contains(playerId)) {
                continue;
            }

            ServerPlayer player = level.getServer().getPlayerList().getPlayer(playerId);

            if (player == null || player.serverLevel() != level || !isValidParticipant(player)) {
                continue;
            }

            player.addEffect(new MobEffectInstance(Oasiso.ENTROPY_EFFECT.get(), ENTROPY_DURATION, 0, false, true));
        }
    }

    private boolean isValidParticipant(ServerPlayer player) {
        return player.isAlive() && !player.isCreative() && !player.isSpectator();
    }

    private void collapseStructure(ServerLevel level, boolean particles) {
        if (this.originalBlocks.isEmpty()) {
            return;
        }

        for (Map.Entry<BlockPos, BlockState> entry : this.originalBlocks.entrySet()) {
            BlockPos pos = entry.getKey();
            BlockState original = entry.getValue();
            BlockState current = level.getBlockState(pos);

            if (particles && !current.isAir() && level.random.nextFloat() < 0.35F) {
                level.levelEvent(2001, pos, Block.getId(current));
            }
            level.setBlock(pos, original, 3);
        }


        this.originalBlocks.clear();
    }

    private void lockBoss(double y) {
        boss.setPos(this.anchorX, y, this.anchorZ);
        boss.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);
        boss.fallDistance = 0.0F;
    }

    private float smoothstep(float value) {
        return value * value * (3.0F - 2.0F * value);
    }

    public void forceCleanup(ServerLevel level) {
        collapseStructure(level, false);

        boss.setParkourActive(false);

        this.stage = Stage.NONE;
        this.currentLayer = 0;
        this.stageTick = 0;
        this.participants.clear();
        this.successfulPlayers.clear();
        this.structureOrigin = null;
    }


    public void reset() {
        this.stage = Stage.NONE;

        this.currentLayer = 0;
        this.stageTick = 0;
        this.originalBlocks.clear();
        this.participants.clear();
        this.successfulPlayers.clear();
        this.structureOrigin = null;

        boss.setParkourActive(false);
    }

    public void save(CompoundTag parent) {
        CompoundTag tag = new CompoundTag();

        tag.putString("Stage", this.stage.name());

        tag.putInt("CurrentLayer", this.currentLayer);
        tag.putInt("StageTick", this.stageTick);
        tag.putInt("SizeX", this.structureSizeX);
        tag.putInt("SizeY", this.structureSizeY);
        tag.putInt("SizeZ", this.structureSizeZ);

        tag.putDouble("AnchorX", this.anchorX);
        tag.putDouble("AnchorZ", this.anchorZ);
        tag.putDouble("ReturnY", this.returnY);
        tag.putDouble("TopY", this.topY);


        if (this.structureOrigin != null) {
            tag.putLong("Origin", this.structureOrigin.asLong());
        }

        ListTag participantList = new ListTag();

        for (UUID id : this.participants) {

            CompoundTag playerTag = new CompoundTag();

            playerTag.putUUID("UUID", id);
            playerTag.putBoolean("Success", this.successfulPlayers.contains(id));
            participantList.add(playerTag);
        }

        tag.put("Participants", participantList);
        ListTag blocks = new ListTag();

        for (Map.Entry<BlockPos, BlockState> entry : this.originalBlocks.entrySet()) {

            CompoundTag blockTag = new CompoundTag();

            blockTag.putLong("Pos", entry.getKey().asLong());
            BlockState.CODEC.encodeStart(NbtOps.INSTANCE, entry.getValue()).result().ifPresent(stateTag -> blockTag.put("State", stateTag));
            blocks.add(blockTag);
        }

        tag.put("OriginalBlocks", blocks);
        parent.put("AzumaalParkour", tag);
    }

    public void load(CompoundTag parent) {
        if (!parent.contains("AzumaalParkour", Tag.TAG_COMPOUND)) {
            return;
        }

        CompoundTag tag = parent.getCompound("AzumaalParkour");

        try {
            this.stage = Stage.valueOf(tag.getString("Stage"));
        } catch (IllegalArgumentException ignored) {
            this.stage = Stage.NONE;
        }

        this.currentLayer = tag.getInt("CurrentLayer");

        this.stageTick = tag.getInt("StageTick");

        this.structureSizeX = tag.getInt("SizeX");
        this.structureSizeY = tag.getInt("SizeY");
        this.structureSizeZ = tag.getInt("SizeZ");

        this.anchorX = tag.getDouble("AnchorX");
        this.anchorZ = tag.getDouble("AnchorZ");
        this.returnY = tag.getDouble("ReturnY");

        this.topY = tag.getDouble("TopY");
        this.structureOrigin = tag.contains("Origin") ? BlockPos.of(tag.getLong("Origin")) : null;

        this.participants.clear();
        this.successfulPlayers.clear();

        ListTag participantList = tag.getList("Participants", Tag.TAG_COMPOUND);

        for (int i = 0; i < participantList.size(); i++) {
            CompoundTag playerTag = participantList.getCompound(i);
            if (!playerTag.hasUUID("UUID")) {
                continue;
            }
            UUID id = playerTag.getUUID("UUID");
            this.participants.add(id);
            if (playerTag.getBoolean("Success")) {
                this.successfulPlayers.add(id);
            }
        }

        this.originalBlocks.clear();
        ListTag blocks = tag.getList("OriginalBlocks", Tag.TAG_COMPOUND);
        for (int i = 0; i < blocks.size(); i++) {
            CompoundTag blockTag = blocks.getCompound(i);
            Tag stateTag = blockTag.get("State");
            if (stateTag == null) {
                continue;
            }
            BlockState.CODEC.parse(NbtOps.INSTANCE, stateTag).result().ifPresent(state -> this.originalBlocks.put(BlockPos.of(blockTag.getLong("Pos")), state));
        }
    }

    public boolean isActive() {
        return this.stage != Stage.NONE;
    }

    private enum Stage {
        NONE,
        BUILD,
        RISE,
        HOLD,
        DESCEND
    }
}