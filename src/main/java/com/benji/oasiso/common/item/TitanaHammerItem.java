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

            int hits = stack.getOrCreateTag().getInt("TitanaHits") + 1;

            if (hits >= 5) {
                hits = 0;

                ServerLevel sl = (ServerLevel) target.level();

                sl.playSound(null, attacker.blockPosition(), SoundEvents.EVOKER_PREPARE_SUMMON, SoundSource.PLAYERS, 1.0F, 0.8F);

                List<LivingEntity> entities = sl.getEntitiesOfClass(LivingEntity.class, attacker.getBoundingBox().inflate(10.0D));

                for (LivingEntity e : entities) {
                    if (e != attacker && e.isAlive() && !e.isSpectator()) {

                        SandHandEntity hand = Oasiso.SAND_HAND.get().create(sl);
                        if (hand != null) {
                            hand.moveTo(e.getX(), e.getY(), e.getZ());
                            hand.setOwner(attacker);
                            sl.addFreshEntity(hand);

                            sl.sendParticles(
                                    new BlockParticleOption(ParticleTypes.FALLING_DUST, Blocks.SAND.defaultBlockState()),
                                    e.getX(), e.getY() + 0.1, e.getZ(),
                                    20, 0.4, 0, 0.4, 0.1
                            );
                        }
                    }
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
    public AnimatableInstanceCache getAnimatableInstanceCache() { return this.cache; }
}