package com.benji.oasiso.common.item;

import com.benji.oasiso.Oasiso;
import com.benji.oasiso.common.entity.SandHandEntity;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.renderer.GeoItemRenderer;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.List;
import java.util.function.Consumer;

public class TitanaHammerItem extends SwordItem implements GeoItem {
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public TitanaHammerItem(Properties properties) {
        super(SuperGoldTier.INSTANCE, 15, -3.5F, properties);
    }

    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        if (!target.level().isClientSide() && attacker instanceof Player player) {

            boolean hasHammerPower = EnchantmentHelper.getItemEnchantmentLevel(Oasiso.HAMMER_POWER.get(), stack) > 0;

            int requiredHits = hasHammerPower ? 3 : 5;

            double attackRadius = hasHammerPower ? 20.0D : 10.0D;

            int hits = stack.getOrCreateTag().getInt("TitanaHits") + 1;

            if (hits >= requiredHits) {
                hits = 0;

                ServerLevel serverLevel = (ServerLevel) target.level();

                serverLevel.playSound(null, attacker.blockPosition(), SoundEvents.EVOKER_PREPARE_SUMMON, SoundSource.PLAYERS, 1.0F, 0.8F);

                double attackRadiusSqr = attackRadius * attackRadius;

                List<LivingEntity> entities = serverLevel.getEntitiesOfClass(LivingEntity.class, attacker.getBoundingBox().inflate(attackRadius), entity -> entity != attacker && entity.isAlive() && !entity.isSpectator() && entity.distanceToSqr(attacker) <= attackRadiusSqr);

                for (LivingEntity entity : entities) {
                    SandHandEntity hand = Oasiso.SAND_HAND.get().create(serverLevel);

                    if (hand == null) {
                        continue;
                    }

                    hand.moveTo(entity.getX(), entity.getY(), entity.getZ());

                    hand.setOwner(attacker);

                    serverLevel.addFreshEntity(hand);

                    serverLevel.sendParticles(new BlockParticleOption(ParticleTypes.FALLING_DUST, Blocks.SAND.defaultBlockState()), entity.getX(), entity.getY() + 0.1D, entity.getZ(), 20, 0.4D, 0.0D, 0.4D, 0.1D);
                }
            }

            stack.getOrCreateTag().putInt("TitanaHits", hits);
        }

        return super.hurtEnemy(stack, target, attacker);
    }

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(new IClientItemExtensions() {
            private GeoItemRenderer<TitanaHammerItem> renderer;

            @Override
            public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                if (this.renderer == null) {
                    this.renderer = new GeoItemRenderer<>(new com.benji.oasiso.client.model.TitanaHammerModel());
                }
                return this.renderer;
            }
        });
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "controller", 0, event -> {
            return PlayState.CONTINUE;
        }));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }
}