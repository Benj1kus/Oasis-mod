package com.benji.oasiso.client;

import com.benji.oasiso.Oasiso;
import com.benji.oasiso.client.particle.*;
import com.benji.oasiso.client.renderer.*;
import com.benji.oasiso.registry.ModBlockEntities;
import com.benji.oasiso.registry.ModEntities;
import net.minecraftforge.client.event.RegisterParticleProvidersEvent;
import com.benji.oasiso.client.dimension.ChaosDimensionEffects;
import net.minecraftforge.client.event.RegisterDimensionSpecialEffectsEvent;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;


@Mod.EventBusSubscriber(
        modid = Oasiso.MODID,
        bus = Mod.EventBusSubscriber.Bus.MOD,
        value = Dist.CLIENT
)
public class ClientModEvents {


    @SubscribeEvent
    public static void registerDimensionEffects(
            RegisterDimensionSpecialEffectsEvent event
    ) {
        event.register(
                Oasiso.CHAOS_SKY_EFFECTS,
                new ChaosDimensionEffects()
        );
    }

    @SubscribeEvent
    public static void registerParticleProviders(
            RegisterParticleProvidersEvent event
    ) {
        event.registerSpriteSet(
                Oasiso.PURPLE_STARS.get(),
                PurpleStarsParticle.Provider::new
        );
        event.registerSpriteSet(
                Oasiso.CHAOS_BOMB_CENTER_SMOKE.get(),
                ChaosBombCenterSmokeParticle.Provider::new
        );

        event.registerSpriteSet(
                Oasiso.ARM_SMOKE.get(),
                ArmSmokeParticle.Provider::new
        );

        event.registerSpriteSet(
                Oasiso.MOUTH_SMOKE.get(),
                MouthSmokeParticleProvider::new
        );

        event.registerSpriteSet(
                Oasiso.CHAOS_BOMB_FIRE_SMOKE.get(),
                ChaosBombFireSmokeParticle.Provider::new
        );

        event.registerSpriteSet(
                Oasiso.CHAOS_BOMB_SPARKS.get(),
                ChaosBombSparksParticle.Provider::new
        );
        event.registerSpriteSet(
                Oasiso.GOLDEN_STARS.get(),
                GoldenStarsParticle.Provider::new
        );

        event.registerSpriteSet(
                Oasiso.WIZARD_PIXELS.get(),
                WizardPixelParticle.Provider::new
        );

        event.registerSpriteSet(
                Oasiso.GOLDEN_HEART.get(),
                GoldenHeartParticle.Provider::new
        );
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {

        event.registerEntityRenderer(
                Oasiso.MONKI.get(),
                MonkiRenderer::new
        );

        event.registerBlockEntityRenderer(
                Oasiso.LIE_BLOCK_BE.get(),
                com.benji.oasiso.client.renderer.LieBlockRenderer::new
        );

        event.registerBlockEntityRenderer(
                Oasiso.STORM_TOTEM_BLOCK_ENTITY.get(),
                StormTotemRenderer::new
        );

        event.registerBlockEntityRenderer(
                ModBlockEntities.MEMORY_PUZ_BE.get(),
                MemoryPuzzleBlockRenderer::new
        );

        event.registerBlockEntityRenderer(
                ModBlockEntities.MEMORY_CORE_BE.get(),
                MemoryCoreBlockRenderer::new
        );

        event.registerBlockEntityRenderer(
                Oasiso.DOUM_PALM_SIGN_BE.get(),
                net.minecraft.client.renderer.blockentity.SignRenderer::new
        );

        event.registerBlockEntityRenderer(
                Oasiso.DOUM_PALM_HANGING_SIGN_BE.get(),
                net.minecraft.client.renderer.blockentity.HangingSignRenderer::new
        );

        event.registerEntityRenderer(
                Oasiso.SAND_GOLEM.get(),
                SandGolemRenderer::new
        );

        event.registerEntityRenderer(
                Oasiso.DAMAGE_NUMBER.get(),
                DamageNumberRenderer::new
        );

        event.registerEntityRenderer(
                Oasiso.WIZARD_PILLAR_ENTITY.get(),
                WizardPillarRenderer::new
        );

        event.registerEntityRenderer(
                Oasiso.BATTLE_HINT_ARROW.get(),
                BattleHintArrowRenderer::new
        );

        event.registerEntityRenderer(
                Oasiso.CIRCLE_HINT.get(),
                CircleHintRenderer::new
        );


        event.registerEntityRenderer(net.minecraft.world.entity.EntityType.WANDERING_TRADER, com.benji.oasiso.client.renderer.DesertWanderingTraderRenderer::new);

        event.registerEntityRenderer(
                Oasiso.DESERT_BALL.get(),
                DesertBallRenderer::new
        );

        event.registerEntityRenderer(
                Oasiso.DOUM_PALM_BOAT.get(),
                context -> new DoumPalmBoatRenderer(context, false)
        );

        event.registerEntityRenderer(
                Oasiso.DOUM_PALM_CHEST_BOAT.get(),
                context -> new DoumPalmBoatRenderer(context, true)
        );

        event.registerEntityRenderer(
                Oasiso.CACTO_PROJ.get(),
                CactoProjRenderer::new
        );

        event.registerEntityRenderer(
                Oasiso.MONKI_BIG.get(),
                MonkiBigRenderer::new
        );

        event.registerEntityRenderer(
                Oasiso.SCARAB.get(),
                ScarabRenderer::new
        );

        event.registerEntityRenderer(
                Oasiso.TITANA.get(),
                TitanaRenderer::new
        );

        event.registerEntityRenderer(
                Oasiso.KROMBUL.get(),
                KrombulRenderer::new
        );

        event.registerEntityRenderer(
                Oasiso.CASER.get(),
                CaserRenderer::new
        );

        event.registerEntityRenderer(
                Oasiso.CRUSADER_TANK.get(),
                CrusaderTankRenderer::new
        );

        event.registerEntityRenderer(
                Oasiso.CRUSADER_WARRIOR.get(),
                CrusaderWarriorRenderer::new
        );

        event.registerEntityRenderer(
                Oasiso.PALADIN.get(),
                PaladinRenderer::new
        );

        event.registerEntityRenderer(
                Oasiso.SWORD_HEART.get(),
                SwordHeartRenderer::new
        );

        event.registerEntityRenderer(
                Oasiso.CRUSADER_WIZARD.get(),
                CrusaderWizardRenderer::new
        );

        event.registerEntityRenderer(
                Oasiso.CRUSADER_ASSASIN.get(),
                CrusaderAssasinRenderer::new
        );

        event.registerEntityRenderer(
                ModEntities.CHAOS_PORTAL_ENTITY.get(),
                ChaosPortalEntityRenderer::new
        );

        event.registerEntityRenderer(
                Oasiso.BOMBUL.get(),
                BombulRenderer::new
        );

        event.registerEntityRenderer(
                Oasiso.GASTER.get(),
                GasterRenderer::new
        );


        event.registerEntityRenderer(
                Oasiso.AZUMAAL.get(),
                AzumaalRenderer::new
        );

        event.registerEntityRenderer(
                Oasiso.BOSS_PORTAL.get(),
                BossPortalRenderer::new
        );

        event.registerEntityRenderer(
                Oasiso.CHAOS_BOMB.get(),
                ChaosBombRenderer::new
        );

        event.registerEntityRenderer(
                Oasiso.SAND_HAND.get(),
                SandHandRenderer::new
        );

        event.registerEntityRenderer(
                Oasiso.EYELID.get(),
                EyelidRenderer::new
        );

        event.registerEntityRenderer(
                Oasiso.DASHER.get(),
                DasherRenderer::new
        );


        event.registerEntityRenderer(
                Oasiso.CACTO.get(),
                CactoRenderer::new
        );

        event.registerBlockEntityRenderer(
                Oasiso.STAT_BE.get(),
                StatBlockEntityRenderer::new
        );

        event.registerBlockEntityRenderer(
                Oasiso.STATUE_BE.get(),
                StatueRenderer::new
        );

        event.registerBlockEntityRenderer(
                Oasiso.SANDED_CHEST_BE.get(),
                SandedChestRenderer::new
        );

        event.registerBlockEntityRenderer(
                Oasiso.CHAOS_ALTAR_BE.get(),
                ChaosAltarRenderer::new
        );

        event.registerBlockEntityRenderer(
                ModBlockEntities.MOUTH_POINT_BE.get(),
                MouthPointRenderer::new
        );

    }
}