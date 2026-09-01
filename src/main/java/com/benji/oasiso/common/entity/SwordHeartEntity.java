package com.benji.oasiso.common.entity;

import com.benji.oasiso.Oasiso;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;
import com.benji.oasiso.ModSounds;
import net.minecraft.sounds.SoundSource;

import java.util.Optional;
import java.util.UUID;

public class SwordHeartEntity extends Monster implements GeoEntity, GlowmaskEntity {

    private static final EntityDataAccessor<Optional<UUID>> OWNER_UUID = SynchedEntityData.defineId(SwordHeartEntity.class, EntityDataSerializers.OPTIONAL_UUID);


    private static final EntityDataAccessor<Integer> QTE_SLOT = SynchedEntityData.defineId(SwordHeartEntity.class, EntityDataSerializers.INT);

    private static final int HOLY_OUTLINE_COLOR = 0xFFD45A;

    private static final RawAnimation IDLE_ANIMATION = RawAnimation.begin().thenLoop("idle");


    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);


    private boolean destroyed;


    public SwordHeartEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        this.noPhysics = true;
        this.setNoGravity(true);
    }


    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, 1.0D).add(Attributes.MOVEMENT_SPEED, 0.0D).add(Attributes.KNOCKBACK_RESISTANCE, 1.0D).add(Attributes.ATTACK_DAMAGE, 0.0D).add(Attributes.FOLLOW_RANGE, 0.0D);
    }


    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();

        this.entityData.define(OWNER_UUID, Optional.empty());

        this.entityData.define(QTE_SLOT, 0);
    }

    @Override
    protected void registerGoals() {
    }

    public void setOwnerUuid(UUID uuid) {
        this.entityData.set(OWNER_UUID, Optional.ofNullable(uuid));
    }


    public UUID getOwnerUuid() {
        return this.entityData.get(OWNER_UUID).orElse(null);
    }


    public void setQteSlot(int slot) {
        this.entityData.set(QTE_SLOT, Math.max(0, Math.min(2, slot)));
    }


    public int getQteSlot() {
        return this.entityData.get(QTE_SLOT);
    }

    @Override
    public void tick() {
        super.tick();

        this.noPhysics = true;
        this.setNoGravity(true);
        this.setDeltaMovement(Vec3.ZERO);

        if (this.level().isClientSide) {
            return;
        }

        if (!(this.level() instanceof ServerLevel level)) {
            return;
        }

        UUID ownerUuid = this.getOwnerUuid();
        if (ownerUuid == null) {
            this.discard();
            return;
        }
        Entity entity = level.getEntity(ownerUuid);

        if (!(entity instanceof PaladinEntity owner) || !owner.isAlive() || !owner.isQteActive()) {
            this.discard();
            return;
        }

        Vec3 anchor = owner.getQteServerAnchor(this.getQteSlot());
        this.setPos(anchor.x, anchor.y, anchor.z);
        this.setYRot(owner.getYRot());
        this.yBodyRot = owner.yBodyRot;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (this.destroyed) {
            return false;
        }
        if (!(source.getEntity() instanceof Player player) || player.isSpectator()) {

            return false;
        }
        if (this.level().isClientSide) {
            return true;
        }
        if (!(this.level() instanceof ServerLevel level)) {
            return false;
        }
        this.destroyed = true;
        level.playSound(null, this.getX(), this.getY() + this.getBbHeight() * 0.5D, this.getZ(), ModSounds.HEART_KILL.get(), SoundSource.HOSTILE, 1.0F, 1.0F);
        level.sendParticles(Oasiso.PURPLE_STARS.get(), this.getX(), this.getY() + this.getBbHeight() * 0.5D, this.getZ(), 35, 0.35D, 0.35D, 0.35D, 0.08D);


        UUID ownerUuid = this.getOwnerUuid();
        if (ownerUuid != null) {
            Entity entity = level.getEntity(ownerUuid);
            if (entity instanceof PaladinEntity owner) {
                owner.onSwordHeartDestroyed();
            }
        }
        this.discard();
        return true;
    }


    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "controller", 0,

                event -> event.setAndContinue(IDLE_ANIMATION)));
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        UUID owner = this.getOwnerUuid();
        if (owner != null) {
            tag.putUUID("QteOwner", owner);
        }
        tag.putInt("QteSlot", this.getQteSlot());
    }


    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.hasUUID("QteOwner")) {
            this.setOwnerUuid(tag.getUUID("QteOwner"));
        }
        this.setQteSlot(tag.getInt("QteSlot"));
    }

    @Override
    public boolean isCurrentlyGlowing() {
        if (this.level().isClientSide) {
            return this.isAlive();
        }

        return super.isCurrentlyGlowing();
    }

    @Override
    public int getTeamColor() {
        return HOLY_OUTLINE_COLOR;
    }


    @Override
    public ResourceLocation getGlowmaskTexture() {
        return ResourceLocation.fromNamespaceAndPath(Oasiso.MODID, "textures/entity/emissive/" + "sword_heart_emissive.png");
    }


    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {

        return this.cache;
    }
}