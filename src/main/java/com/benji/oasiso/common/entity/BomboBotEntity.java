package com.benji.oasiso.common.entity;

import com.benji.oasiso.Oasiso;
import com.benji.oasiso.common.combat.ScienceCombatNetwork;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.EnumSet;
import java.util.UUID;

public class BomboBotEntity extends Monster implements GeoEntity, GlowmaskEntity {
    public static final double BLAST_RADIUS = 4.0;
    public static final float BLAST_POWER = 1.0F;
    private static final int CONTACT_FUSE = 10, SUMMONED_LIFETIME = 2400;
    private static final RawAnimation ATTACK = RawAnimation.begin().thenLoop("attack");
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private UUID scientistOwner, preferredPlayer;
    private boolean exploded;
    private int fuse = -1, summonedAge, repath;

    public BomboBotEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, 1.0D).add(Attributes.MOVEMENT_SPEED, 0.40D).add(Attributes.KNOCKBACK_RESISTANCE, 0.20D).add(Attributes.ATTACK_DAMAGE, 0.0D).add(Attributes.FOLLOW_RANGE, 40.0D);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new PursuePlayerGoal());
    }

    public void setScientistOwner(CrusaderScienceEntity owner, Player player) {
        scientistOwner = owner.getUUID();
        preferredPlayer = player.getUUID();
        setTarget(player);
    }

    private boolean valid(Player player) {
        return player != null && player.isAlive() && !player.isCreative() && !player.isSpectator() && player.level() == level() && distanceToSqr(player) <= 64 * 64;
    }

    private Player choosePlayer() {
        Player preferred = preferredPlayer == null ? null : level().getPlayerByUUID(preferredPlayer);
        if (valid(preferred)) return preferred;
        Player closest = null;
        double best = 40 * 40;
        for (Player player : level().players()) {
            double d = distanceToSqr(player);
            if (valid(player) && d < best) {
                closest = player;
                best = d;
            }
        }
        return closest;
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide || exploded || !isAlive()) return;
        if (scientistOwner != null && ++summonedAge >= SUMMONED_LIFETIME) {
            discard();
            return;
        }
        if (fuse < 0 && getTarget() instanceof Player player && valid(player) && distanceToSqr(player) <= 1.65 * 1.65 && getSensing().hasLineOfSight(player)) {
            fuse = CONTACT_FUSE;
            level().playSound(null, blockPosition(), SoundEvents.CREEPER_PRIMED, SoundSource.HOSTILE, .45F, 1.65F);
        }
        if (fuse >= 0 && --fuse <= 0) explode();
    }

    @Override
    public void die(DamageSource source) {
        if (exploded) return;
        super.die(source);
        if (!level().isClientSide) explode();
    }

    private void explode() {
        if (exploded || !(level() instanceof ServerLevel server)) return;
        exploded = true;
        Vec3 center = position().add(0, .25, 0);
        ScienceCombatNetwork.blast(server, center);
        server.playSound(null, center.x, center.y, center.z, SoundEvents.GENERIC_EXPLODE, SoundSource.HOSTILE, 1.1F, 1.25F + random.nextFloat() * .2F);
        DamageSource source = server.damageSources().explosion(this, this);
        for (LivingEntity target : server.getEntitiesOfClass(LivingEntity.class, new AABB(center, center).inflate(BLAST_RADIUS), e -> e.isAlive() && e != this)) {
            if (target.isSpectator() || (target instanceof Player p && p.isCreative())) continue;
            if (scientistOwner != null && scientistOwner.equals(target.getUUID())) continue;
            if (target instanceof BomboBotEntity bot && scientistOwner != null && scientistOwner.equals(bot.scientistOwner))
                continue;
            double distance = target.position().distanceTo(center) / BLAST_RADIUS;
            if (distance > 1) continue;
            double exposure = Explosion.getSeenPercent(center, target);
            double impact = (1 - distance) * exposure;
            if (impact <= 0) continue;

            float damage = (float) ((impact * impact + impact) * .5 * 7 * (BLAST_POWER * 2) + 1);
            if (target.hurt(source, damage)) {
                Vec3 push = target.position().add(0, target.getBbHeight() * .5, 0).subtract(center).normalize();
                target.setDeltaMovement(target.getDeltaMovement().add(push.scale(impact * .65)));
                target.hurtMarked = true;
            }
        }
        discard();
    }

    private final class PursuePlayerGoal extends Goal {
        PursuePlayerGoal() {
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            return choosePlayer() != null;
        }

        @Override
        public boolean canContinueToUse() {
            return choosePlayer() != null && !exploded;
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void stop() {
            getNavigation().stop();
            setTarget(null);
        }

        @Override
        public void tick() {
            Player player = choosePlayer();
            if (player == null) return;
            setTarget(player);
            getLookControl().setLookAt(player, 30, 30);
            if (--repath <= 0) {
                repath = 8;
                getNavigation().moveTo(player, 1.0D);
            }
        }
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (scientistOwner != null) tag.putUUID("ScienceOwner", scientistOwner);
        if (preferredPlayer != null) tag.putUUID("BomboPreferredPlayer", preferredPlayer);
        tag.putInt("BomboFuse", fuse);
        tag.putInt("BomboAge", summonedAge);
        tag.putBoolean("BomboExploded", exploded);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        scientistOwner = tag.hasUUID("ScienceOwner") ? tag.getUUID("ScienceOwner") : null;
        preferredPlayer = tag.hasUUID("BomboPreferredPlayer") ? tag.getUUID("BomboPreferredPlayer") : null;
        fuse = tag.contains("BomboFuse") ? Math.max(-1, Math.min(CONTACT_FUSE, tag.getInt("BomboFuse"))) : -1;
        summonedAge = Math.max(0, tag.getInt("BomboAge"));
        exploded = tag.getBoolean("BomboExploded");
        if (exploded) discard();
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "controller", 5, event -> event.setAndContinue(ATTACK)));
    }

    @Override
    public ResourceLocation getGlowmaskTexture() {
        return ResourceLocation.fromNamespaceAndPath(Oasiso.MODID, "textures/entity/emissive/bombo_bot_emissive.png");
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
}
