package com.benji.oasiso.common.block.entity;

import com.benji.oasiso.Oasiso;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import com.benji.oasiso.common.entity.BossPortalEntity;
import com.benji.oasiso.common.dimension.BossArenaEncounter;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import com.benji.oasiso.ModSounds;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import software.bernie.geckolib.animatable.GeoBlockEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;
import com.benji.oasiso.common.entity.PaladinEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class ChaosAltarBlockEntity extends BlockEntity implements GeoBlockEntity {

    private static final String CONTROLLER_NAME = "controller";
    private static final String CHAOS_TRIGGER = "chaos_sphere";
    private static final String DOMINANCE_TRIGGER = "dominance_sphere";


    private static final double PALADIN_LOCK_RANGE = 40.0D;
    private static final int ANIMATION_DURATION = 65;
    private static final int SAND_PARTICLE_TICK = 30;

    private static final RawAnimation IDLE_ANIMATION = RawAnimation.begin().thenLoop("idle");
    private static final RawAnimation CHAOS_ANIMATION = RawAnimation.begin().thenPlay("chaos_sphere");
    private static final RawAnimation DOMINANCE_ANIMATION = RawAnimation.begin().thenPlay("dominance_sphere");

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);


    private int animationTick = -1;

    private ActivationType currentActivation;

    public ChaosAltarBlockEntity(BlockPos pos, BlockState state) {
        super(Oasiso.CHAOS_ALTAR_BE.get(), pos, state);
    }

    public boolean activate(ActivationType activationType) {
        if (this.level == null || this.level.isClientSide || this.animationTick >= 0 || this.isBlockedByPaladin() || !(this.level instanceof ServerLevel serverLevel)) {

            return false;
        }

        this.currentActivation = activationType;

        this.animationTick = 0;


        this.triggerAnim(CONTROLLER_NAME, activationType.triggerName);

        spawnActivationEffects(serverLevel, activationType);

        return true;
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, ChaosAltarBlockEntity altar) {
        if (!(level instanceof ServerLevel serverLevel) || altar.animationTick < 0) {
            return;
        }

        altar.animationTick++;

        if (altar.animationTick == SAND_PARTICLE_TICK) {

            altar.spawnSandParticles(serverLevel);
        }

        if (altar.animationTick >= ANIMATION_DURATION) {

            ActivationType finishedActivation = altar.currentActivation;

            altar.animationTick = -1;
            altar.currentActivation = null;

            if (!serverLevel.getBlockState(pos).is(Oasiso.CHAOS_ALTAR.get())) {
                return;
            }

            serverLevel.removeBlock(pos, false);

            BossPortalEntity.PortalPurpose portalPurpose = finishedActivation == ActivationType.CHAOS ? BossPortalEntity.PortalPurpose.CHAOS : BossPortalEntity.PortalPurpose.DOMINATION;
            altar.spawnBossPortal(serverLevel, pos, portalPurpose);
        }
    }

    public boolean isBlockedByPaladin() {
        if (this.level == null) {
            return false;
        }

        Vec3 altarCenter = Vec3.atCenterOf(this.worldPosition);
        double rangeSqr = PALADIN_LOCK_RANGE * PALADIN_LOCK_RANGE;
        AABB searchBox = new AABB(this.worldPosition).inflate(PALADIN_LOCK_RANGE);


        return !this.level.getEntitiesOfClass(PaladinEntity.class, searchBox, paladin -> paladin.isAlive() && paladin.position().distanceToSqr(altarCenter) <= rangeSqr).isEmpty();
    }


    public void spawnBlockedClickEffects(ServerLevel level) {
        double x = this.worldPosition.getX() + 0.5D;
        double y = this.worldPosition.getY() + 0.75D;
        double z = this.worldPosition.getZ() + 0.5D;


        level.sendParticles(ParticleTypes.LARGE_SMOKE, x, y, z, 18, 0.42D, 0.34D, 0.42D, 0.018D);
        level.sendParticles(ParticleTypes.SMOKE, x, y, z, 14, 0.36D, 0.28D, 0.36D, 0.012D);
    }

    private void spawnBossPortal(ServerLevel level, BlockPos pos, BossPortalEntity.PortalPurpose purpose) {
        BossPortalEntity portal = Oasiso.BOSS_PORTAL.get().create(level);


        if (portal == null) {
            return;
        }


        portal.moveTo(pos.getX() + 0.5D, pos.getY() + 0.03D, pos.getZ() + 0.5D, 0.0F, 0.0F);
        if (purpose == BossPortalEntity.PortalPurpose.DOMINATION && level.dimension().equals(Oasiso.CHAOS_DIMENSION)) {

            java.util.UUID sessionId = BossArenaEncounter.findNearbySessionId(level, portal.position(), 240.0D);
            portal.startOpening(purpose, sessionId);
        } else {
            portal.startOpening(purpose);
        }

        level.addFreshEntity(portal);
        if (purpose == BossPortalEntity.PortalPurpose.CHAOS) {
            BossArenaEncounter.prepareArenaForPortal(level.getServer(), portal);
        }

        level.playSound(null, portal.getX(), portal.getY(), portal.getZ(), ModSounds.PORTAL_OPEN.get(), SoundSource.BLOCKS, 1.25F, 1.0F);
    }

    private void spawnActivationEffects(ServerLevel level, ActivationType activationType) {
        double centerX = this.worldPosition.getX() + 0.5D;
        double centerY = this.worldPosition.getY() + 0.75D;
        double centerZ = this.worldPosition.getZ() + 0.5D;

        ParticleOptions particle = activationType == ActivationType.CHAOS ? Oasiso.PURPLE_STARS.get() : Oasiso.GOLDEN_STARS.get();

        level.sendParticles(particle, centerX, centerY, centerZ, 45, 0.5D, 0.55D, 0.5D, 0.07D);

        level.playSound(null, this.worldPosition, SoundEvents.BEACON_ACTIVATE, SoundSource.BLOCKS, 1.25F, activationType == ActivationType.CHAOS ? 0.75F : 1.15F);
        level.playSound(null, this.worldPosition, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.BLOCKS, 0.8F, activationType == ActivationType.CHAOS ? 0.65F : 1.3F);
    }

    private void spawnSandParticles(ServerLevel level) {
        BlockParticleOption sandParticle = new BlockParticleOption(ParticleTypes.BLOCK, Blocks.SAND.defaultBlockState());

        double centerX = this.worldPosition.getX() + 0.5D;
        double centerY = this.worldPosition.getY() + 0.12D;
        double centerZ = this.worldPosition.getZ() + 0.5D;

        level.sendParticles(sandParticle, centerX, centerY, centerZ, 65, 0.72D, 0.12D, 0.72D, 0.12D);

        level.playSound(null, this.worldPosition, SoundEvents.SAND_BREAK, SoundSource.BLOCKS, 1.15F, 0.7F + level.random.nextFloat() * 0.15F);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        AnimationController<ChaosAltarBlockEntity> controller = new AnimationController<>(this, CONTROLLER_NAME, 0, state -> state.setAndContinue(IDLE_ANIMATION));

        controller.triggerableAnim(CHAOS_TRIGGER, CHAOS_ANIMATION);

        controller.triggerableAnim(DOMINANCE_TRIGGER, DOMINANCE_ANIMATION);

        controllers.add(controller);
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }

    public enum ActivationType {

        CHAOS(CHAOS_TRIGGER), DOMINANCE(DOMINANCE_TRIGGER);

        private final String triggerName;

        ActivationType(String triggerName) {
            this.triggerName = triggerName;
        }
    }
}