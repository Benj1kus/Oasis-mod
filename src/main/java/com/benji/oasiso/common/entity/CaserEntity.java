package com.benji.oasiso.common.entity;

import com.benji.oasiso.ModSounds;
import com.benji.oasiso.Oasiso;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.List;

public class CaserEntity extends Monster implements GeoEntity, GlowmaskEntity {
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public static final EntityDataAccessor<Integer> SPIN_TICKS = SynchedEntityData.defineId(CaserEntity.class, EntityDataSerializers.INT);
    public static final EntityDataAccessor<ItemStack> REWARD_ITEM = SynchedEntityData.defineId(CaserEntity.class, EntityDataSerializers.ITEM_STACK);
    
    public static final EntityDataAccessor<Float> FIXED_YAW = SynchedEntityData.defineId(CaserEntity.class, EntityDataSerializers.FLOAT);

    private boolean hasFloated = false;

    public CaserEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        this.setNoGravity(true);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(SPIN_TICKS, 0);
        this.entityData.define(REWARD_ITEM, ItemStack.EMPTY);
        this.entityData.define(FIXED_YAW, 0.0F);
    }

    public void setFixedYaw(float yaw) {
        this.entityData.set(FIXED_YAW, yaw);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag compound) {
        super.addAdditionalSaveData(compound);
        compound.putBoolean("HasFloated", this.hasFloated);
        compound.putFloat("FixedYaw", this.entityData.get(FIXED_YAW));
    }

    @Override
    public void readAdditionalSaveData(CompoundTag compound) {
        super.readAdditionalSaveData(compound);
        this.hasFloated = compound.getBoolean("HasFloated");
        this.entityData.set(FIXED_YAW, compound.getFloat("FixedYaw"));
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 100.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0D);
    }

    @Override
    protected void registerGoals() {
        // nothgig
    }

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (hand == InteractionHand.MAIN_HAND && this.entityData.get(SPIN_TICKS) <= 0 && stack.is(Oasiso.NEPHRITIS.get())) {
            if (!player.isCreative()) {
                stack.shrink(1);
            }
            startSpinning(player);
            return InteractionResult.sidedSuccess(this.level().isClientSide);
        }
        return super.mobInteract(player, hand);
    }

    private void startSpinning(Player player) {
        if (!this.level().isClientSide) {
            try {
                LootTable table = this.level().getServer().getLootData().getLootTable(ResourceLocation.fromNamespaceAndPath(Oasiso.MODID, "gameplay/caser_loot"));
                LootParams params = new LootParams.Builder((ServerLevel) this.level())
                        .create(LootContextParamSets.EMPTY);

                List<ItemStack> items = table.getRandomItems(params);
                ItemStack reward = items.isEmpty() ? new ItemStack(Items.DIRT) : items.get(0);

                this.entityData.set(REWARD_ITEM, reward);
                this.entityData.set(SPIN_TICKS, 100);

            } catch (Exception e) {
                this.entityData.set(REWARD_ITEM, new ItemStack(Items.DIAMOND));
                this.entityData.set(SPIN_TICKS, 100);
            }
        }
    }

    @Override
    public void tick() {
        super.tick();

        float yaw = this.entityData.get(FIXED_YAW);
        this.setYRot(yaw);
        this.yRotO = yaw;
        this.yBodyRot = yaw;
        this.yBodyRotO = yaw;
        this.yHeadRot = yaw;
        this.yHeadRotO = yaw;

        if (!this.level().isClientSide) {
            this.setDeltaMovement(0, 0, 0);

            if (!this.hasFloated) {
                this.setPos(this.getX(), this.getY() + 1.5D, this.getZ());
                this.hasFloated = true;
            }
        }

        int ticks = this.entityData.get(SPIN_TICKS);

        if (ticks > 0) {
            if (ticks > 60 && ticks % 3 == 0) playSpinSound();
            else if (ticks > 30 && ticks <= 60 && ticks % 6 == 0) playSpinSound();
            else if (ticks > 10 && ticks <= 30 && ticks % 12 == 0) playSpinSound();

            if (!this.level().isClientSide) {
                this.entityData.set(SPIN_TICKS, ticks - 1);

                if (ticks == 1) {
                    ItemStack reward = this.entityData.get(REWARD_ITEM);

                    ItemEntity droppedItem = new ItemEntity(this.level(), this.getX(), this.getY(), this.getZ(), reward);
                    droppedItem.setDeltaMovement(0, 0.3, 0);
                    this.level().addFreshEntity(droppedItem);

                    int rarity = getRarityLevel(reward);

                    if (rarity >= 2) {
                        this.level().playSound(null, this.blockPosition(), ModSounds.CASER_SUCCESS.get(), SoundSource.NEUTRAL, 1.0F, 1.0F);
                        ((ServerLevel) this.level()).sendParticles(ParticleTypes.TOTEM_OF_UNDYING, this.getX(), this.getY(), this.getZ(), 30, 0.5, 0.5, 0.5, 0.2);
                        this.triggerAnim("action_controller", "victory");
                    } else if (rarity == 0) {
                        this.level().playSound(null, this.blockPosition(), ModSounds.CASER_DEFAULT.get(), SoundSource.NEUTRAL, 1.0F, 1.0F);
                        this.triggerAnim("action_controller", "lose");
                    } else {
                        this.level().playSound(null, this.blockPosition(), ModSounds.CASER_DEFAULT.get(), SoundSource.NEUTRAL, 1.0F, 1.0F);
                    }

                    this.entityData.set(REWARD_ITEM, ItemStack.EMPTY);
                }
            }
        }
    }

    private void playSpinSound() {
        this.playSound(ModSounds.CASER_SPIN.get(), 0.8F, 1.0F + (this.random.nextFloat() - 0.5F) * 0.2F);
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (!this.level().isClientSide && source.getEntity() instanceof Player) {
            this.playSound(ModSounds.CASER_HIT.get(), 1.0F, 1.0F);
            this.triggerAnim("action_controller", "defend");
        }
        return false;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "idle_controller", 5, event -> {
            return event.setAndContinue(RawAnimation.begin().thenLoop("idle"));
        }));

        controllers.add(new AnimationController<>(this, "action_controller", 5, event -> PlayState.CONTINUE)
                .triggerableAnim("defend", RawAnimation.begin().thenPlay("defend"))
                .triggerableAnim("victory", RawAnimation.begin().thenPlay("victory"))
                .triggerableAnim("lose", RawAnimation.begin().thenPlay("lose"))
        );
    }

    public static int getRarityLevel(ItemStack stack) {
        Item item = stack.getItem();
        if (stack.isEmpty()) return 0;

        if (item == Oasiso.TITANA_HAMMER.get() || item == Oasiso.SUPER_GOLD_HELMET.get() ||
                item == Oasiso.SUPER_GOLD_CHESTPLATE.get() || item == Oasiso.SUPER_GOLD_LEGGINGS.get() ||
                item == Oasiso.SUPER_GOLD_BOOTS.get()) return 3;

        if (item == Items.NETHERITE_INGOT || item == Oasiso.KARAKOLIT_INGOT.get() ||
                item == Oasiso.NEPHRITIS_CORE.get() || item == Oasiso.TITANA_STATUE_ITEM.get() ||
                item == Oasiso.MONKI_STATUE_ITEM.get() || item == Oasiso.DASHER_STATUE_ITEM.get()) return 2;

        if (item == Items.DEAD_BUSH || item == Items.SAND || item == Items.SPIDER_EYE ||
                item == Items.ROTTEN_FLESH || item == Items.GLASS_BOTTLE || item == Items.STICK ||
                item == Items.DANDELION) return 0;

        return 1;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        SoundEvent[] sounds = { ModSounds.CASER1.get(), ModSounds.CASER2.get(), ModSounds.CASER3.get() };
        return sounds[this.random.nextInt(sounds.length)];
    }

    @Override
    public ResourceLocation getGlowmaskTexture() {
        return ResourceLocation.fromNamespaceAndPath(Oasiso.MODID, "textures/entity/emissive/caser_emissive.png");
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() { return this.cache; }
}