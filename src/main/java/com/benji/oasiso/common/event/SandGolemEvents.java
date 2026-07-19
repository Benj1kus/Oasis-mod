package com.benji.oasiso.common.event;

import com.benji.oasiso.Oasiso;
import com.benji.oasiso.common.entity.SandGolemEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.npc.WanderingTrader;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;

@Mod.EventBusSubscriber(modid = Oasiso.MODID)
public class SandGolemEvents {

    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        BlockState state = event.getState();
        Player player = event.getPlayer();
        Level level = player.level();

        if (!level.isClientSide() && (state.is(Blocks.GOLD_BLOCK) || state.is(Blocks.RAW_GOLD_BLOCK))) {
            List<SandGolemEntity> golems = level.getEntitiesOfClass(SandGolemEntity.class, player.getBoundingBox().inflate(15.0D));
            for (SandGolemEntity golem : golems) {
                if (!golem.isPlayerCreated()) {
                    golem.setTarget(player);
                }
            }
        }
    }

    @SubscribeEvent
    public static void onTraderAttacked(LivingDamageEvent event) {
        if (!event.getEntity().level().isClientSide() && event.getEntity() instanceof WanderingTrader trader) {
            if (event.getSource().getEntity() instanceof Player player) {

                List<SandGolemEntity> golems = trader.level().getEntitiesOfClass(SandGolemEntity.class, trader.getBoundingBox().inflate(15.0D));
                for (SandGolemEntity golem : golems) {
                    if (!golem.isPlayerCreated()) {
                        golem.setTarget(player);
                    }
                }
            }
        }
    }

    @SubscribeEvent
    public static void onBlockPlace(BlockEvent.EntityPlaceEvent event) {
        if (event.getLevel() instanceof ServerLevel level) {
            BlockState placed = event.getPlacedBlock();

            if (placed.is(Blocks.CARVED_PUMPKIN) || placed.is(Blocks.JACK_O_LANTERN)) {
                BlockPos pos = event.getPos();
                BlockPos body = pos.below();
                BlockPos leg = body.below();

                if (isSandstone(level.getBlockState(body)) && isSandstone(level.getBlockState(leg))) {

                    boolean isXAxis = isSandstone(level.getBlockState(body.east())) && isSandstone(level.getBlockState(body.west()));
                    boolean isZAxis = isSandstone(level.getBlockState(body.north())) && isSandstone(level.getBlockState(body.south()));

                    if (isXAxis || isZAxis) {
                        level.setBlock(pos, Blocks.AIR.defaultBlockState(), 2);
                        level.setBlock(body, Blocks.AIR.defaultBlockState(), 2);
                        level.setBlock(leg, Blocks.AIR.defaultBlockState(), 2);

                        if (isXAxis) {
                            level.setBlock(body.east(), Blocks.AIR.defaultBlockState(), 2);
                            level.setBlock(body.west(), Blocks.AIR.defaultBlockState(), 2);
                        } else {
                            level.setBlock(body.north(), Blocks.AIR.defaultBlockState(), 2);
                            level.setBlock(body.south(), Blocks.AIR.defaultBlockState(), 2);
                        }

                        SandGolemEntity golem = Oasiso.SAND_GOLEM.get().create(level);
                        if (golem != null) {
                            golem.moveTo(pos.getX() + 0.5D, leg.getY(), pos.getZ() + 0.5D, 0.0F, 0.0F);
                            golem.setPlayerCreated(true);
                            level.addFreshEntity(golem);

                            level.sendParticles(
                                    new BlockParticleOption(ParticleTypes.FALLING_DUST, Blocks.SAND.defaultBlockState()),
                                    pos.getX() + 0.5D, body.getY() + 0.5D, pos.getZ() + 0.5D,
                                    100, 1.0D, 1.5D, 1.0D, 0.1D
                            );
                        }
                    }
                }
            }
        }
    }
    
    private static boolean isSandstone(BlockState state) {
        return state.is(Blocks.SANDSTONE) || state.is(Blocks.CHISELED_SANDSTONE) || state.is(Blocks.CUT_SANDSTONE) || state.is(Blocks.SMOOTH_SANDSTONE);
    }
}