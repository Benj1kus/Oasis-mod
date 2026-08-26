package com.benji.oasiso.common.block.entity;

import com.benji.oasiso.Oasiso;
import com.benji.oasiso.common.block.ChaosSpawnerBlock;
import com.benji.oasiso.common.entity.SandGolemEntity;
import com.benji.oasiso.config.OsirisRealmConfig;
import com.benji.oasiso.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.monster.Skeleton;
import net.minecraft.world.entity.monster.Spider;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

public class ChaosSpawnerBlockEntity extends BlockEntity {

    // Радиус считается по X/Z. По Y оставляем узкую полосу,
    // чтобы спавнеры на соседних этажах не включались одновременно.
    private static final double PLAYER_RADIUS = 40.0D;
    private static final double PLAYER_VERTICAL_RANGE = 4.0D;

    private static final int SPAWN_RADIUS = 10;
    private static final int CHECK_INTERVAL = 10;
    private static final int REWARD_POP_INTERVAL = 4;

    private static final ResourceLocation BASIC_REWARD = ResourceLocation.fromNamespaceAndPath(Oasiso.MODID, "chaos_spawner/basic");
    private static final ResourceLocation INSECTS_REWARD = ResourceLocation.fromNamespaceAndPath(Oasiso.MODID, "chaos_spawner/insects");
    private static final ResourceLocation CRUSADERS_REWARD = ResourceLocation.fromNamespaceAndPath(Oasiso.MODID, "chaos_spawner/crusaders");
    private static final ResourceLocation GOLEMS_REWARD = ResourceLocation.fromNamespaceAndPath(Oasiso.MODID, "chaos_spawner/golems");
    private static final ResourceLocation CACTUS_REWARD = ResourceLocation.fromNamespaceAndPath(Oasiso.MODID, "chaos_spawner/cactus");

    private final List<UUID> waveMobs = new ArrayList<>();
    private final List<ItemStack> rewardQueue = new ArrayList<>();

    private boolean waveActive = false;
    private boolean completed = false;
    private ChaosSpawnerBlock.Difficulty waveDifficulty = ChaosSpawnerBlock.Difficulty.NOVICE;
    private int rewardPopCooldown = 0;

    private static final EquipmentSlot[] ARMOR_SLOTS = {
            EquipmentSlot.HEAD,
            EquipmentSlot.CHEST,
            EquipmentSlot.LEGS,
            EquipmentSlot.FEET
    };

    public ChaosSpawnerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CHAOS_SPAWNER_BE.get(), pos, state);
    }

    public static void serverTick(
            Level level,
            BlockPos pos,
            BlockState state,
            ChaosSpawnerBlockEntity blockEntity
    ) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }

        blockEntity.tickRewardDispense(serverLevel, pos);

        // После победы спавнер навсегда остаётся выключенным,
        // пока сам блок не будет сломан/поставлен заново.
        if (blockEntity.completed) {
            if (state.getValue(ChaosSpawnerBlock.ACTIVE)) {
                serverLevel.setBlock(
                        pos,
                        state.setValue(ChaosSpawnerBlock.ACTIVE, false),
                        3
                );
            }
            return;
        }

        if ((serverLevel.getGameTime() + Math.abs(pos.asLong())) % CHECK_INTERVAL != 0L) {
            return;
        }

        Player player = findNearestPlayer(serverLevel, pos);
        boolean active = state.getValue(ChaosSpawnerBlock.ACTIVE);

        // Если волна уже была вызвана, новые мобы больше не создаются.
        // ACTIVE в этот момент отвечает только за визуальное состояние блока.
        if (blockEntity.waveActive) {
            blockEntity.removeDeadWaveMobs(serverLevel);

            if (blockEntity.waveMobs.isEmpty()) {
                blockEntity.completeWave(
                        serverLevel,
                        pos,
                        state
                );
                return;
            }

            if (player == null && active) {
                serverLevel.setBlock(
                        pos,
                        state.setValue(ChaosSpawnerBlock.ACTIVE, false),
                        3
                );
            } else if (player != null && !active) {
                serverLevel.setBlock(
                        pos,
                        state.setValue(ChaosSpawnerBlock.ACTIVE, true),
                        3
                );
            }

            return;
        }

        if (player == null) {
            if (active) {
                serverLevel.setBlock(
                        pos,
                        state.setValue(ChaosSpawnerBlock.ACTIVE, false),
                        3
                );
            }
            return;
        }

        if (!active) {
            serverLevel.setBlock(
                    pos,
                    state.setValue(ChaosSpawnerBlock.ACTIVE, true),
                    3
            );

            playActivationEffects(serverLevel, pos);
        }

        blockEntity.beginWave(
                serverLevel,
                pos,
                state.getValue(ChaosSpawnerBlock.DIFFICULTY),
                player
        );
    }

    private static Player findNearestPlayer(ServerLevel level, BlockPos pos) {
        double centerX = pos.getX() + 0.5D;
        double centerY = pos.getY() + 0.5D;
        double centerZ = pos.getZ() + 0.5D;
        double maxHorizontalSqr = PLAYER_RADIUS * PLAYER_RADIUS;

        AABB searchBox = new AABB(
                centerX - PLAYER_RADIUS,
                centerY - PLAYER_VERTICAL_RANGE,
                centerZ - PLAYER_RADIUS,
                centerX + PLAYER_RADIUS,
                centerY + PLAYER_VERTICAL_RANGE,
                centerZ + PLAYER_RADIUS
        );

        Player nearest = null;
        double nearestDistance = Double.MAX_VALUE;

        for (Player player : level.getEntitiesOfClass(Player.class, searchBox)) {
            if (!player.isAlive() || player.isSpectator() || player.isCreative()) {
                continue;
            }

            double dx = player.getX() - centerX;
            double dz = player.getZ() - centerZ;
            double horizontalSqr = dx * dx + dz * dz;

            if (horizontalSqr <= maxHorizontalSqr && horizontalSqr < nearestDistance) {
                nearest = player;
                nearestDistance = horizontalSqr;
            }
        }

        return nearest;
    }

    private static void playActivationEffects(ServerLevel level, BlockPos pos) {
        double x = pos.getX() + 0.5D;
        double y = pos.getY() + 0.65D;
        double z = pos.getZ() + 0.5D;

        level.playSound(
                null,
                pos,
                SoundEvents.IRON_TRAPDOOR_OPEN,
                SoundSource.BLOCKS,
                1.1F,
                0.75F
        );

        level.sendParticles(
                ParticleTypes.SOUL,
                x,
                y,
                z,
                45,
                0.55D,
                0.45D,
                0.55D,
                0.035D
        );
    }

    private static void spawnWave(
            ChaosSpawnerBlockEntity blockEntity,
            ServerLevel level,
            BlockPos origin,
            ChaosSpawnerBlock.Difficulty difficulty,
            Player triggerPlayer
    ) {
        List<SpawnEntry> directEntries = new ArrayList<>();
        Map<String, List<SpawnEntry>> randomGroups = new LinkedHashMap<>();

        for (String rawEntry : getConfiguredWave(difficulty)) {
            SpawnEntry entry = parseSpawnEntry(rawEntry);
            if (entry == null) {
                continue;
            }

            if (entry.group().isBlank()) {
                directEntries.add(entry);
            } else {
                randomGroups.computeIfAbsent(entry.group(), key -> new ArrayList<>()).add(entry);
            }
        }

        for (SpawnEntry entry : directEntries) {
            spawnConfiguredEntry(blockEntity, level, origin, difficulty, triggerPlayer, entry);
        }

        for (List<SpawnEntry> groupEntries : randomGroups.values()) {
            if (groupEntries.isEmpty()) {
                continue;
            }

            SpawnEntry selected = groupEntries.get(level.random.nextInt(groupEntries.size()));
            spawnConfiguredEntry(blockEntity, level, origin, difficulty, triggerPlayer, selected);
        }
    }

    private static List<? extends String> getConfiguredWave(ChaosSpawnerBlock.Difficulty difficulty) {
        return switch (difficulty) {
            case NOVICE -> OsirisRealmConfig.CHAOS_SPAWNER_NOVICE_MOBS.get();
            case EASY -> OsirisRealmConfig.CHAOS_SPAWNER_EASY_MOBS.get();
            case NORMAL -> OsirisRealmConfig.CHAOS_SPAWNER_NORMAL_MOBS.get();
            case INSECTS_HARD -> OsirisRealmConfig.CHAOS_SPAWNER_INSECTS_HARD_MOBS.get();
            case CACTUS -> OsirisRealmConfig.CHAOS_SPAWNER_CACTUS_MOBS.get();
            case INSECTS_BRUTAL -> OsirisRealmConfig.CHAOS_SPAWNER_INSECTS_BRUTAL_MOBS.get();
            case CRUSADERS_HARD -> OsirisRealmConfig.CHAOS_SPAWNER_CRUSADERS_HARD_MOBS.get();
            case CRUSADERS_BRUTAL -> OsirisRealmConfig.CHAOS_SPAWNER_CRUSADERS_BRUTAL_MOBS.get();
            case GOLEMS -> OsirisRealmConfig.CHAOS_SPAWNER_GOLEMS_MOBS.get();
        };
    }

    private static SpawnEntry parseSpawnEntry(String rawEntry) {
        if (rawEntry == null || rawEntry.isBlank()) {
            return null;
        }

        String[] parts = rawEntry.split("\\|", -1);
        if (parts.length < 3 || parts.length > 4) {
            return null;
        }

        ResourceLocation entityId = ResourceLocation.tryParse(parts[0].trim());
        if (entityId == null) {
            return null;
        }

        try {
            int min = Math.max(0, Integer.parseInt(parts[1].trim()));
            int max = Math.max(min, Integer.parseInt(parts[2].trim()));
            String group = parts.length == 4 ? parts[3].trim() : "";
            return new SpawnEntry(entityId, min, max, group);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private static void spawnConfiguredEntry(
            ChaosSpawnerBlockEntity blockEntity,
            ServerLevel level,
            BlockPos origin,
            ChaosSpawnerBlock.Difficulty difficulty,
            Player triggerPlayer,
            SpawnEntry entry
    ) {
        EntityType<?> entityType = ForgeRegistries.ENTITY_TYPES.getValue(entry.entityId());
        if (entityType == null) {
            return;
        }

        int count = randomBetween(level.random, entry.min(), entry.max());

        for (int i = 0; i < count; i++) {
            Entity created = entityType.create(level);
            if (!(created instanceof Mob mob)) {
                continue;
            }

            Mob spawned = finishSpawn(
                    blockEntity,
                    level,
                    origin,
                    mob,
                    configuredMob -> configureSpawnedMob(difficulty, configuredMob, triggerPlayer)
            );

            if (spawned instanceof Spider spider
                    && difficulty == ChaosSpawnerBlock.Difficulty.NORMAL
                    && level.random.nextFloat() < 0.20F) {
                spawnSpiderJockey(blockEntity, level, spider);
            }
        }
    }

    private static void configureSpawnedMob(
            ChaosSpawnerBlock.Difficulty difficulty,
            Mob mob,
            Player triggerPlayer
    ) {
        if (mob instanceof SandGolemEntity golem) {
            golem.setPlayerCreated(false);
            golem.setSpawnerHostile(true);
            golem.setTarget(triggerPlayer);
        }

        switch (difficulty) {
            case NOVICE -> {
                if (mob instanceof Zombie zombie) {
                    configureNoviceZombie(zombie);
                } else if (mob instanceof Skeleton skeleton) {
                    configureNoviceSkeleton(skeleton);
                }
            }
            case EASY -> {
                if (mob instanceof Zombie zombie) {
                    configureEasyZombie(zombie);
                } else if (mob instanceof Skeleton skeleton) {
                    configureEasySkeleton(skeleton);
                }
            }
            case NORMAL -> {
                if (mob instanceof Zombie zombie) {
                    configureNormalZombie(zombie);
                } else if (mob instanceof Skeleton skeleton) {
                    configureNormalSkeleton(skeleton);
                }
            }
            default -> {
            }
        }
    }

    private static <T extends Mob> T finishSpawn(
            ChaosSpawnerBlockEntity blockEntity,
            ServerLevel level,
            BlockPos origin,
            T mob,
            Consumer<T> config
    ) {
        BlockPos spawnPos = findSpawnPos(level, origin, mob);
        if (spawnPos == null) {
            return null;
        }

        mob.moveTo(
                spawnPos.getX() + 0.5D,
                spawnPos.getY(),
                spawnPos.getZ() + 0.5D,
                level.random.nextFloat() * 360.0F,
                0.0F
        );

        mob.finalizeSpawn(
                level,
                level.getCurrentDifficultyAt(spawnPos),
                MobSpawnType.TRIGGERED,
                null,
                null
        );

        if (config != null) {
            config.accept(mob);
        }

        mob.setPersistenceRequired();

        if (!level.addFreshEntity(mob)) {
            return null;
        }

        blockEntity.trackMob(mob);
        playSpawnEffects(level, mob);
        return mob;
    }

    private static BlockPos findSpawnPos(
            ServerLevel level,
            BlockPos origin,
            Mob mob
    ) {
        RandomSource random = level.random;
        int[] yOffsets = {0, 1, -1, 2, -2, 3, -3};

        for (int attempt = 0; attempt < 30; attempt++) {
            int dx = random.nextInt(SPAWN_RADIUS * 2 + 1) - SPAWN_RADIUS;
            int dz = random.nextInt(SPAWN_RADIUS * 2 + 1) - SPAWN_RADIUS;

            if (dx * dx + dz * dz > SPAWN_RADIUS * SPAWN_RADIUS) {
                continue;
            }

            for (int yOffset : yOffsets) {
                BlockPos candidate = origin.offset(dx, 1 + yOffset, dz);

                if (!level.getBlockState(candidate).getCollisionShape(level, candidate).isEmpty()) {
                    continue;
                }

                if (!level.getBlockState(candidate.above()).getCollisionShape(level, candidate.above()).isEmpty()) {
                    continue;
                }

                if (!level.getBlockState(candidate.below()).isFaceSturdy(level, candidate.below(), Direction.UP)) {
                    continue;
                }

                mob.moveTo(
                        candidate.getX() + 0.5D,
                        candidate.getY(),
                        candidate.getZ() + 0.5D,
                        0.0F,
                        0.0F
                );

                if (level.noCollision(mob)) {
                    return candidate;
                }
            }
        }

        return null;
    }

    private static void playSpawnEffects(ServerLevel level, Mob mob) {
        level.sendParticles(
                ParticleTypes.SOUL,
                mob.getX(),
                mob.getY() + mob.getBbHeight() * 0.5D,
                mob.getZ(),
                28,
                Math.max(0.25D, mob.getBbWidth() * 0.45D),
                Math.max(0.4D, mob.getBbHeight() * 0.35D),
                Math.max(0.25D, mob.getBbWidth() * 0.45D),
                0.035D
        );

        level.playSound(
                null,
                mob.blockPosition(),
                SoundEvents.EVOKER_PREPARE_SUMMON,
                SoundSource.HOSTILE,
                0.75F,
                0.9F + level.random.nextFloat() * 0.2F
        );
    }

    private static void spawnSpiderJockey(ChaosSpawnerBlockEntity blockEntity, ServerLevel level, Spider spider) {
        Skeleton skeleton = EntityType.SKELETON.create(level);
        if (skeleton == null) {
            return;
        }

        BlockPos pos = spider.blockPosition();

        skeleton.moveTo(
                spider.getX(),
                spider.getY(),
                spider.getZ(),
                spider.getYRot(),
                0.0F
        );

        skeleton.finalizeSpawn(
                level,
                level.getCurrentDifficultyAt(pos),
                MobSpawnType.TRIGGERED,
                null,
                null
        );

        configureNormalSkeleton(skeleton);

        skeleton.setPersistenceRequired();

        if (level.addFreshEntity(skeleton)) {
            blockEntity.trackMob(skeleton);
            skeleton.startRiding(spider, true);
            playSpawnEffects(level, skeleton);
        }
    }

    private void beginWave(
            ServerLevel level,
            BlockPos origin,
            ChaosSpawnerBlock.Difficulty difficulty,
            Player triggerPlayer
    ) {
        this.waveMobs.clear();
        this.waveActive = true;
        this.waveDifficulty = difficulty;

        spawnWave(
                this,
                level,
                origin,
                difficulty,
                triggerPlayer
        );

        // Если вообще ни одному мобу не удалось найти место для спавна,
        // награда не выдаётся. На следующей проверке спавнер попробует снова.
        if (this.waveMobs.isEmpty()) {
            this.waveActive = false;
        }

        setChanged();
    }

    private void trackMob(Mob mob) {
        this.waveMobs.add(mob.getUUID());
        setChanged();
    }

    private void removeDeadWaveMobs(ServerLevel level) {
        boolean changed = this.waveMobs.removeIf(uuid -> {
            Entity entity = level.getEntity(uuid);
            return entity == null || !entity.isAlive();
        });

        if (changed) {
            setChanged();
        }
    }

    private void completeWave(
            ServerLevel level,
            BlockPos pos,
            BlockState state
    ) {
        this.waveActive = false;
        this.completed = true;
        this.waveMobs.clear();

        level.setBlock(
                pos,
                state.setValue(ChaosSpawnerBlock.ACTIVE, false),
                3
        );

        prepareRewards(level, pos, this.waveDifficulty);
        setChanged();
    }

    private void prepareRewards(
            ServerLevel level,
            BlockPos pos,
            ChaosSpawnerBlock.Difficulty difficulty
    ) {
        ResourceLocation tableId = getRewardTable(difficulty);
        LootTable lootTable = level.getServer().getLootData().getLootTable(tableId);

        LootParams params = new LootParams.Builder(level)
                .withParameter(
                        LootContextParams.ORIGIN,
                        Vec3.atCenterOf(pos)
                )
                .create(LootContextParamSets.CHEST);

        this.rewardQueue.clear();

        for (ItemStack stack : lootTable.getRandomItems(params)) {
            for (int i = 0; i < stack.getCount(); i++) {
                ItemStack single = stack.copy();
                single.setCount(1);
                this.rewardQueue.add(single);
            }
        }

        this.rewardPopCooldown = 0;
    }

    private void tickRewardDispense(ServerLevel level, BlockPos pos) {
        if (this.rewardQueue.isEmpty()) {
            return;
        }

        if (this.rewardPopCooldown > 0) {
            this.rewardPopCooldown--;
            return;
        }

        ItemStack reward = this.rewardQueue.remove(0);

        double x = pos.getX() + 0.5D;
        double y = pos.getY() + 1.15D;
        double z = pos.getZ() + 0.5D;

        ItemEntity item = new ItemEntity(
                level,
                x,
                y,
                z,
                reward
        );

        item.setDefaultPickUpDelay();
        item.setDeltaMovement(
                (level.random.nextDouble() - 0.5D) * 0.10D,
                0.32D + level.random.nextDouble() * 0.08D,
                (level.random.nextDouble() - 0.5D) * 0.10D
        );

        level.addFreshEntity(item);

        level.sendParticles(
                ParticleTypes.LARGE_SMOKE,
                x,
                y,
                z,
                8,
                0.16D,
                0.08D,
                0.16D,
                0.015D
        );

        level.playSound(
                null,
                pos,
                SoundEvents.ITEM_PICKUP,
                SoundSource.BLOCKS,
                0.65F,
                1.15F + level.random.nextFloat() * 0.20F
        );

        this.rewardPopCooldown = REWARD_POP_INTERVAL;
        setChanged();
    }

    private static ResourceLocation getRewardTable(ChaosSpawnerBlock.Difficulty difficulty) {
        return switch (difficulty) {
            case NOVICE, EASY, NORMAL -> BASIC_REWARD;
            case INSECTS_HARD, INSECTS_BRUTAL -> INSECTS_REWARD;
            case CRUSADERS_HARD, CRUSADERS_BRUTAL -> CRUSADERS_REWARD;
            case GOLEMS -> GOLEMS_REWARD;
            case CACTUS -> CACTUS_REWARD;
        };
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);

        tag.putBoolean("WaveActive", this.waveActive);
        tag.putBoolean("Completed", this.completed);
        tag.putInt("WaveDifficulty", this.waveDifficulty.ordinal());
        tag.putInt("RewardPopCooldown", this.rewardPopCooldown);

        ListTag waveTag = new ListTag();
        for (UUID uuid : this.waveMobs) {
            waveTag.add(NbtUtils.createUUID(uuid));
        }
        tag.put("WaveMobs", waveTag);

        ListTag rewardsTag = new ListTag();
        for (ItemStack stack : this.rewardQueue) {
            CompoundTag stackTag = new CompoundTag();
            stack.save(stackTag);
            rewardsTag.add(stackTag);
        }
        tag.put("RewardQueue", rewardsTag);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);

        this.waveActive = tag.getBoolean("WaveActive");
        this.completed = tag.getBoolean("Completed");

        int difficultyIndex = tag.getInt("WaveDifficulty");
        ChaosSpawnerBlock.Difficulty[] difficulties = ChaosSpawnerBlock.Difficulty.values();
        this.waveDifficulty = difficulties[Math.max(0, Math.min(difficultyIndex, difficulties.length - 1))];

        this.rewardPopCooldown = tag.getInt("RewardPopCooldown");

        this.waveMobs.clear();
        ListTag waveTag = tag.getList("WaveMobs", Tag.TAG_INT_ARRAY);
        for (int i = 0; i < waveTag.size(); i++) {
            this.waveMobs.add(NbtUtils.loadUUID(waveTag.get(i)));
        }

        this.rewardQueue.clear();
        ListTag rewardsTag = tag.getList("RewardQueue", Tag.TAG_COMPOUND);
        for (int i = 0; i < rewardsTag.size(); i++) {
            ItemStack stack = ItemStack.of(rewardsTag.getCompound(i));
            if (!stack.isEmpty()) {
                this.rewardQueue.add(stack);
            }
        }
    }

    private static void configureNoviceZombie(Zombie zombie) {
        clearArmor(zombie);
        zombie.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
        zombie.setItemSlot(EquipmentSlot.OFFHAND, ItemStack.EMPTY);
    }

    private static void configureNoviceSkeleton(Skeleton skeleton) {
        clearArmor(skeleton);
        skeleton.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.BOW));
        skeleton.setItemSlot(EquipmentSlot.OFFHAND, ItemStack.EMPTY);
    }

    private static void configureEasyZombie(Zombie zombie) {
        clearArmor(zombie);
        zombie.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
        zombie.setItemSlot(EquipmentSlot.OFFHAND, ItemStack.EMPTY);

        RandomSource random = zombie.getRandom();

        if (random.nextFloat() < 0.65F) {
            equipRandomArmor(zombie, random, false, false);
        }

        if (random.nextFloat() < 0.45F) {
            zombie.setItemSlot(
                    EquipmentSlot.MAINHAND,
                    new ItemStack(random.nextBoolean() ? Items.IRON_SWORD : Items.GOLDEN_SWORD)
            );
        }
    }

    private static void configureEasySkeleton(Skeleton skeleton) {
        clearArmor(skeleton);
        skeleton.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.BOW));
        skeleton.setItemSlot(EquipmentSlot.OFFHAND, ItemStack.EMPTY);

        if (skeleton.getRandom().nextFloat() < 0.65F) {
            equipRandomArmor(skeleton, skeleton.getRandom(), false, false);
        }
    }

    private static void configureNormalZombie(Zombie zombie) {
        clearArmor(zombie);
        zombie.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
        zombie.setItemSlot(EquipmentSlot.OFFHAND, ItemStack.EMPTY);

        RandomSource random = zombie.getRandom();

        if (random.nextFloat() < 0.82F) {
            equipRandomArmor(zombie, random, true, true);
        }

        if (random.nextFloat() < 0.65F) {
            ItemStack sword = new ItemStack(
                    random.nextFloat() < 0.75F
                            ? Items.IRON_SWORD
                            : Items.GOLDEN_SWORD
            );

            if (random.nextFloat() < 0.65F) {
                sword = enchant(random, sword);
            }

            zombie.setItemSlot(EquipmentSlot.MAINHAND, sword);
        }
    }

    private static void configureNormalSkeleton(Skeleton skeleton) {
        clearArmor(skeleton);
        skeleton.setItemSlot(EquipmentSlot.OFFHAND, ItemStack.EMPTY);

        RandomSource random = skeleton.getRandom();

        if (random.nextFloat() < 0.82F) {
            equipRandomArmor(skeleton, random, true, true);
        }

        ItemStack bow = new ItemStack(Items.BOW);
        if (random.nextFloat() < 0.65F) {
            bow = enchant(random, bow);
        }

        skeleton.setItemSlot(EquipmentSlot.MAINHAND, bow);
    }

    private static void clearArmor(Mob mob) {
        for (EquipmentSlot slot : ARMOR_SLOTS) {
            mob.setItemSlot(slot, ItemStack.EMPTY);
        }
    }

    private static void equipRandomArmor(
            Mob mob,
            RandomSource random,
            boolean allowDiamond,
            boolean allowEnchantments
    ) {
        int pieces = randomBetween(random, 2, 3);
        List<EquipmentSlot> available = new ArrayList<>(List.of(ARMOR_SLOTS));

        for (int i = 0; i < pieces && !available.isEmpty(); i++) {
            EquipmentSlot slot = available.remove(random.nextInt(available.size()));
            ItemStack armor = createArmorPiece(slot, random, allowDiamond);

            if (allowEnchantments && random.nextFloat() < 0.60F) {
                armor = enchant(random, armor);
            }

            mob.setItemSlot(slot, armor);
        }
    }

    private static ItemStack createArmorPiece(
            EquipmentSlot slot,
            RandomSource random,
            boolean allowDiamond
    ) {
        ArmorMaterial material;

        if (allowDiamond) {
            material = random.nextFloat() < 0.15F
                    ? ArmorMaterial.DIAMOND
                    : ArmorMaterial.IRON;
        } else {
            material = random.nextBoolean()
                    ? ArmorMaterial.IRON
                    : ArmorMaterial.GOLD;
        }

        return switch (material) {
            case GOLD -> switch (slot) {
                case HEAD -> new ItemStack(Items.GOLDEN_HELMET);
                case CHEST -> new ItemStack(Items.GOLDEN_CHESTPLATE);
                case LEGS -> new ItemStack(Items.GOLDEN_LEGGINGS);
                case FEET -> new ItemStack(Items.GOLDEN_BOOTS);
                default -> ItemStack.EMPTY;
            };

            case DIAMOND -> switch (slot) {
                case HEAD -> new ItemStack(Items.DIAMOND_HELMET);
                case CHEST -> new ItemStack(Items.DIAMOND_CHESTPLATE);
                case LEGS -> new ItemStack(Items.DIAMOND_LEGGINGS);
                case FEET -> new ItemStack(Items.DIAMOND_BOOTS);
                default -> ItemStack.EMPTY;
            };

            default -> switch (slot) {
                case HEAD -> new ItemStack(Items.IRON_HELMET);
                case CHEST -> new ItemStack(Items.IRON_CHESTPLATE);
                case LEGS -> new ItemStack(Items.IRON_LEGGINGS);
                case FEET -> new ItemStack(Items.IRON_BOOTS);
                default -> ItemStack.EMPTY;
            };
        };
    }

    private static ItemStack enchant(RandomSource random, ItemStack stack) {
        return EnchantmentHelper.enchantItem(
                random,
                stack,
                randomBetween(random, 5, 30),
                false
        );
    }

    private static int randomBetween(RandomSource random, int min, int max) {
        return min + random.nextInt(max - min + 1);
    }

    private record SpawnEntry(ResourceLocation entityId, int min, int max, String group) {
    }

    private enum ArmorMaterial {
        GOLD,
        IRON,
        DIAMOND
    }
}
