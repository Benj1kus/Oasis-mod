package com.benji.oasiso.registry;

import com.benji.oasiso.Oasiso;

import static com.benji.oasiso.Oasiso.MODID;

import com.benji.oasiso.common.entity.*;
import com.benji.oasiso.common.entity.projectile.CactoProjEntity;
import com.benji.oasiso.common.entity.projectile.DesertBallEntity;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModEntities {

    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, Oasiso.MODID);

    public static final RegistryObject<EntityType<KrombulEntity>> KROMBUL = ENTITIES.register("krombul", () -> EntityType.Builder.of(KrombulEntity::new, MobCategory.CREATURE).sized(0.75F, 1.25F).clientTrackingRange(8).build(ResourceLocation.fromNamespaceAndPath(MODID, "krombul").toString()));
    public static final RegistryObject<EntityType<AzumaalEntity>> AZUMAAL = ENTITIES.register("azumaal", () -> EntityType.Builder.of(AzumaalEntity::new, MobCategory.MONSTER).sized(1.375F, 6.25F).build(ResourceLocation.fromNamespaceAndPath(MODID, "azumaal").toString()));
    public static final RegistryObject<EntityType<ChaosBombEntity>> CHAOS_BOMB = ENTITIES.register("chaos_bomb", () -> EntityType.Builder.of(ChaosBombEntity::new, MobCategory.MONSTER).sized(0.75F, 0.875F).build(ResourceLocation.fromNamespaceAndPath(MODID, "chaos_bomb").toString()));
    public static final RegistryObject<EntityType<EyelidEntity>> EYELID = ENTITIES.register("eyelid", () -> EntityType.Builder.<EyelidEntity>of(EyelidEntity::new, MobCategory.MISC).sized(1.2F, 1.2F).clientTrackingRange(12).updateInterval(1).build(ResourceLocation.fromNamespaceAndPath(MODID, "eyelid").toString()));
    public static final RegistryObject<EntityType<CircleHintEntity>> CIRCLE_HINT = ENTITIES.register("circle_hint", () -> EntityType.Builder.of(CircleHintEntity::new, MobCategory.MONSTER).sized(2.5F, 0.125F).build(ResourceLocation.fromNamespaceAndPath(MODID, "circle_hint").toString()));
    public static final RegistryObject<EntityType<BattleHintArrowEntity>> BATTLE_HINT_ARROW = ENTITIES.register("battle_hint_arrow", () -> EntityType.Builder.of(BattleHintArrowEntity::new, MobCategory.MONSTER).sized(4.25F, 0.125F).build(ResourceLocation.fromNamespaceAndPath(MODID, "battle_hint_arrow").toString()));
    public static final RegistryObject<EntityType<BossPortalEntity>> BOSS_PORTAL = ENTITIES.register("boss_portal", () -> EntityType.Builder.of(BossPortalEntity::new, MobCategory.MONSTER).sized(3.375F, 0.1875F).build(ResourceLocation.fromNamespaceAndPath(MODID, "boss_portal").toString()));
    public static final RegistryObject<EntityType<DamageNumberEntity>> DAMAGE_NUMBER = ENTITIES.register("damage_number", () -> EntityType.Builder.<DamageNumberEntity>of(DamageNumberEntity::new, MobCategory.MISC).sized(0.01F, 0.01F).clientTrackingRange(8).updateInterval(1).build(ResourceLocation.fromNamespaceAndPath(MODID, "damage_number").toString()));
    public static final RegistryObject<EntityType<WizardPillarEntity>> WIZARD_PILLAR_ENTITY = ENTITIES.register("wizard_pillar_entity", () -> EntityType.Builder.<WizardPillarEntity>of(WizardPillarEntity::new, MobCategory.MISC).sized(0.1F, 0.1F).clientTrackingRange(64).updateInterval(1).build("wizard_pillar_entity"));
    public static final RegistryObject<EntityType<MonkiEntity>> MONKI = ENTITIES.register("monki", () -> EntityType.Builder.of(MonkiEntity::new, MobCategory.MONSTER).sized(0.625F, 1.25F).build(ResourceLocation.fromNamespaceAndPath(MODID, "monki").toString()));
    public static final RegistryObject<EntityType<CrusaderTankEntity>> CRUSADER_TANK = ENTITIES.register("crusader_tank", () -> EntityType.Builder.of(CrusaderTankEntity::new, MobCategory.MONSTER).sized(1.50F, 3.00F).build(ResourceLocation.fromNamespaceAndPath(MODID, "crusader_tank").toString()));
    public static final RegistryObject<EntityType<CrusaderWarriorEntity>> CRUSADER_WARRIOR = ENTITIES.register("crusader_warrior", () -> EntityType.Builder.of(CrusaderWarriorEntity::new, MobCategory.MONSTER).sized(0.625F, 2.25F).build(ResourceLocation.fromNamespaceAndPath(MODID, "crusader_warrior").toString()));
    public static final RegistryObject<EntityType<PaladinEntity>> PALADIN = ENTITIES.register("paladin", () -> EntityType.Builder.of(PaladinEntity::new, MobCategory.MONSTER).sized(0.625F, 2.25F).build(ResourceLocation.fromNamespaceAndPath(MODID, "paladin").toString()));
    public static final RegistryObject<EntityType<SwordHeartEntity>> SWORD_HEART = ENTITIES.register("sword_heart", () -> EntityType.Builder.of(SwordHeartEntity::new, MobCategory.MONSTER).sized(0.25F, 0.25F).build(ResourceLocation.fromNamespaceAndPath(MODID, "sword_heart").toString()));
    public static final RegistryObject<EntityType<CrusaderAssasinEntity>> CRUSADER_ASSASIN = ENTITIES.register("crusader_assasin", () -> EntityType.Builder.of(CrusaderAssasinEntity::new, MobCategory.MONSTER).sized(0.625F, 2.25F).build(ResourceLocation.fromNamespaceAndPath(MODID, "crusader_assasin").toString()));
    public static final RegistryObject<EntityType<CrusaderWizardEntity>> CRUSADER_WIZARD = ENTITIES.register("crusader_wizard", () -> EntityType.Builder.of(CrusaderWizardEntity::new, MobCategory.MONSTER).sized(0.625F, 2.25F).build(ResourceLocation.fromNamespaceAndPath(MODID, "crusader_wizard").toString()));
    public static final RegistryObject<EntityType<GasterEntity>> GASTER = ENTITIES.register("gaster", () -> EntityType.Builder.of(GasterEntity::new, MobCategory.MONSTER).sized(1.0F, 2.5625F).build(ResourceLocation.fromNamespaceAndPath(MODID, "gaster").toString()));
    public static final RegistryObject<EntityType<DesertBallEntity>> DESERT_BALL = ENTITIES.register("desertball", () -> EntityType.Builder.<DesertBallEntity>of(DesertBallEntity::new, MobCategory.MISC).sized(0.2F, 0.2F).clientTrackingRange(4).updateInterval(10).build(ResourceLocation.fromNamespaceAndPath(MODID, "desertball").toString()));
    public static final RegistryObject<EntityType<CactoProjEntity>> CACTO_PROJ = ENTITIES.register("cacto_proj", () -> EntityType.Builder.<CactoProjEntity>of(CactoProjEntity::new, MobCategory.MISC).sized(0.125F, 0.125F).clientTrackingRange(4).updateInterval(20).build(ResourceLocation.fromNamespaceAndPath(MODID, "cacto_proj").toString()));
    public static final RegistryObject<EntityType<MonkiBigEntity>> MONKI_BIG = ENTITIES.register("monki_big", () -> EntityType.Builder.of(MonkiBigEntity::new, MobCategory.MONSTER).sized(1.75F, 3.75F).build(ResourceLocation.fromNamespaceAndPath(MODID, "monki_big").toString()));
    public static final RegistryObject<EntityType<TitanaEntity>> TITANA = ENTITIES.register("titana", () -> EntityType.Builder.of(TitanaEntity::new, MobCategory.MONSTER).sized(2.5F, 3.75F).build(ResourceLocation.fromNamespaceAndPath(MODID, "titana").toString()));
    public static final RegistryObject<EntityType<SandGolemEntity>> SAND_GOLEM = ENTITIES.register("sand_golem", () -> EntityType.Builder.of(SandGolemEntity::new, MobCategory.MONSTER).sized(2.5F, 3.75F).build(ResourceLocation.fromNamespaceAndPath(MODID, "sand_golem").toString()));
    public static final RegistryObject<EntityType<CaserEntity>> CASER = ENTITIES.register("caser", () -> EntityType.Builder.of(CaserEntity::new, MobCategory.CREATURE).sized(2.5F, 3.75F).build(ResourceLocation.fromNamespaceAndPath(MODID, "caser").toString()));
    public static final RegistryObject<EntityType<SandHandEntity>> SAND_HAND = ENTITIES.register("sand_hand", () -> EntityType.Builder.<SandHandEntity>of(SandHandEntity::new, MobCategory.MISC).sized(1.5F, 2.0F).build(ResourceLocation.fromNamespaceAndPath(MODID, "sand_hand").toString()));
    public static final RegistryObject<EntityType<DasherEntity>> DASHER = ENTITIES.register("dasher", () -> EntityType.Builder.of(DasherEntity::new, MobCategory.MONSTER).sized(1.25F, 5.0F).build(ResourceLocation.fromNamespaceAndPath(MODID, "dasher").toString()));
    public static final RegistryObject<EntityType<BombulEntity>> BOMBUL = ENTITIES.register("bombul", () -> EntityType.Builder.of(BombulEntity::new, MobCategory.MONSTER).sized(1.25F, 3.0F).build(ResourceLocation.fromNamespaceAndPath(MODID, "bombul").toString()));
    public static final RegistryObject<EntityType<CactoEntity>> CACTO = ENTITIES.register("cacto", () -> EntityType.Builder.of(CactoEntity::new, MobCategory.MONSTER).sized(0.625F, 1.5F).build(ResourceLocation.fromNamespaceAndPath(MODID, "cacto").toString()));
    public static final RegistryObject<EntityType<DoumPalmBoatEntity>> DOUM_PALM_BOAT = ENTITIES.register("doum_palm_boat", () -> EntityType.Builder.<DoumPalmBoatEntity>of(DoumPalmBoatEntity::new, MobCategory.MISC).sized(1.375F, 0.5625F).clientTrackingRange(10).build(ResourceLocation.fromNamespaceAndPath(MODID, "doum_palm_boat").toString()));
    public static final RegistryObject<EntityType<DoumPalmChestBoatEntity>> DOUM_PALM_CHEST_BOAT = ENTITIES.register("doum_palm_chest_boat", () -> EntityType.Builder.<DoumPalmChestBoatEntity>of(DoumPalmChestBoatEntity::new, MobCategory.MISC).sized(1.375F, 0.5625F).clientTrackingRange(10).build(ResourceLocation.fromNamespaceAndPath(MODID, "doum_palm_chest_boat").toString()));

    private ModEntities() {
    }

    public static void register(IEventBus eventBus) {
        ENTITIES.register(eventBus);
    }
}
