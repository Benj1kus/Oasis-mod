package com.benji.oasiso;

import com.benji.oasiso.common.block.entity.*;
import com.benji.oasiso.common.dispenser.ModDispenserBehaviors;
import com.benji.oasiso.common.enchantment.HammerPowerEnchantment;
import com.benji.oasiso.common.entity.*;
import com.benji.oasiso.common.entity.projectile.CactoProjEntity;
import com.benji.oasiso.common.entity.projectile.DesertBallEntity;
import com.benji.oasiso.network.BossPortalTransitionNetwork;
import com.benji.oasiso.network.ModMessages;
import com.benji.oasiso.network.dialogue.BossDialogueNetwork;
import com.benji.oasiso.config.OsirisRealmConfig;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;
import com.benji.oasiso.registry.ModBlockEntities;
import com.benji.oasiso.registry.ModBlocks;
import com.benji.oasiso.registry.ModEffects;
import com.benji.oasiso.registry.ModEntities;
import com.benji.oasiso.registry.ModItems;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FlowerPotBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

@Mod(Oasiso.MODID)
public class Oasiso {

    public static final String MODID = "oasiso";

    public static final DeferredRegister<StructureType<?>> STRUCTURE_TYPES = DeferredRegister.create(Registries.STRUCTURE_TYPE, MODID);

    public static final DeferredRegister<Enchantment> ENCHANTMENTS = DeferredRegister.create(ForgeRegistries.ENCHANTMENTS, MODID);

    public static final DeferredRegister<ParticleType<?>> PARTICLES = DeferredRegister.create(ForgeRegistries.PARTICLE_TYPES, MODID);

    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MODID);

    public static final ResourceKey<Level> CHAOS_DIMENSION = ResourceKey.create(Registries.DIMENSION, ResourceLocation.fromNamespaceAndPath(MODID, "chaos_dimension"));

    public static final ResourceLocation CHAOS_SKY_EFFECTS = ResourceLocation.fromNamespaceAndPath(MODID, "chaos_sky");

    public static final RegistryObject<Enchantment> HAMMER_POWER = ENCHANTMENTS.register("hammer_power", HammerPowerEnchantment::new);

    public static final RegistryObject<SimpleParticleType> WIZARD_PIXELS = PARTICLES.register("wizard_pixels", () -> new SimpleParticleType(false));
    public static final RegistryObject<SimpleParticleType> PURPLE_STARS = PARTICLES.register("purple_stars", () -> new SimpleParticleType(false));
    public static final RegistryObject<SimpleParticleType> GOLDEN_STARS = PARTICLES.register("golden_stars", () -> new SimpleParticleType(false));
    public static final RegistryObject<SimpleParticleType> GOLDEN_HEART = PARTICLES.register("golden_heart", () -> new SimpleParticleType(false));
    public static final RegistryObject<SimpleParticleType> CHAOS_BOMB_CENTER_SMOKE = PARTICLES.register("chaos_bomb_center_smoke", () -> new SimpleParticleType(false));
    public static final RegistryObject<SimpleParticleType> CHAOS_BOMB_FIRE_SMOKE = PARTICLES.register("chaos_bomb_fire_smoke", () -> new SimpleParticleType(false));
    public static final RegistryObject<SimpleParticleType> CHAOS_BOMB_SPARKS = PARTICLES.register("chaos_bomb_sparks", () -> new SimpleParticleType(false));
    public static final RegistryObject<SimpleParticleType> ENTROPY_GRAVITY_TRAIL = PARTICLES.register("entropy_gravity_trail", () -> new SimpleParticleType(false));
    public static final RegistryObject<SimpleParticleType> MELTED_SPLASH = PARTICLES.register("melted_splash", () -> new SimpleParticleType(false));
    public static final RegistryObject<SimpleParticleType> ARM_SMOKE = PARTICLES.register("arm_smoke", () -> new SimpleParticleType(false));
    public static final RegistryObject<SimpleParticleType> MOUTH_SMOKE = PARTICLES.register("mouth_smoke", () -> new SimpleParticleType(false));

    public static final RegistryObject<CreativeModeTab> OASISO_TAB = CREATIVE_MODE_TABS.register("oasiso_tab", () -> CreativeModeTab.builder().icon(() -> new ItemStack(ModItems.NEPHRITIS_CORE.get())).title(Component.translatable("creativetab.oasiso_tab")).displayItems((parameters, output) -> {
        for (RegistryObject<Item> item : ModItems.ITEMS.getEntries()) {
            output.accept(item.get());
        }
    }).build());

    public static final DeferredRegister<Block> BLOCKS = ModBlocks.BLOCKS;
    public static final DeferredRegister<Item> ITEMS = ModItems.ITEMS;
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = ModBlockEntities.BLOCK_ENTITIES;
    public static final DeferredRegister<EntityType<?>> ENTITIES = ModEntities.ENTITIES;
    public static final DeferredRegister<MobEffect> MOB_EFFECTS = ModEffects.MOB_EFFECTS;

    public static final net.minecraft.world.level.block.state.properties.BlockSetType DOUM_PALM_SET_TYPE = ModBlocks.DOUM_PALM_SET_TYPE;

    public static final net.minecraft.world.level.block.state.properties.WoodType DOUM_PALM_WOOD_TYPE = ModBlocks.DOUM_PALM_WOOD_TYPE;

    // Blocks
    public static final RegistryObject<Block> LIE_BLOCK = ModBlocks.LIE_BLOCK;
    public static final RegistryObject<Block> SANDSTONE_TILES = ModBlocks.SANDSTONE_TILES;
    public static final RegistryObject<Block> SANDSTONE_POLISHED = ModBlocks.SANDSTONE_POLISHED;
    public static final RegistryObject<Block> CHAOS_EYE = ModBlocks.CHAOS_EYE;
    public static final RegistryObject<Block> SANDSTONE_COLUMN = ModBlocks.SANDSTONE_COLUMN;
    public static final RegistryObject<Block> SCARLET_LEAVES = ModBlocks.SCARLET_LEAVES;
    public static final RegistryObject<Block> SCARLET_GRASS = ModBlocks.SCARLET_GRASS;
    public static final RegistryObject<Block> SCARLET_LOG = ModBlocks.SCARLET_LOG;
    public static final RegistryObject<Block> NEPHRITIS_COLUMN = ModBlocks.NEPHRITIS_COLUMN;
    public static final RegistryObject<Block> NEPHRITIS_BRICKS = ModBlocks.NEPHRITIS_BRICKS;
    public static final RegistryObject<Block> NEPHRITIS_COMPRESSED = ModBlocks.NEPHRITIS_COMPRESSED;
    public static final RegistryObject<Block> NEPHRITIS_SPIRAL = ModBlocks.NEPHRITIS_SPIRAL;
    public static final RegistryObject<Block> NEPHRITIS_POLISHED = ModBlocks.NEPHRITIS_POLISHED;
    public static final RegistryObject<Block> NEPHRITIS_CORNER = ModBlocks.NEPHRITIS_CORNER;
    public static final RegistryObject<Block> NEPHRITIS_LINE = ModBlocks.NEPHRITIS_LINE;
    public static final RegistryObject<Block> SANDSTONE_COLORED = ModBlocks.SANDSTONE_COLORED;
    public static final RegistryObject<Block> SANDSTONE_STRIPE = ModBlocks.SANDSTONE_STRIPE;
    public static final RegistryObject<Block> SANDSTONE_FLOORB = ModBlocks.SANDSTONE_FLOORB;
    public static final RegistryObject<Block> KARAKOLIT_BLOCK = ModBlocks.KARAKOLIT_BLOCK;
    public static final RegistryObject<Block> NEPHRITIS_BLOCK = ModBlocks.NEPHRITIS_BLOCK;
    public static final RegistryObject<Block> WIZARD_EYE = ModBlocks.WIZARD_EYE;
    public static final RegistryObject<Block> WIZARD_COLUMN = ModBlocks.WIZARD_COLUMN;
    public static final RegistryObject<Block> SANDSTONE_ROOF = ModBlocks.SANDSTONE_ROOF;
    public static final RegistryObject<Block> SANDSTONE_BRICKED = ModBlocks.SANDSTONE_BRICKED;
    public static final RegistryObject<Block> SANDSTONE_AZAZEL = ModBlocks.SANDSTONE_AZAZEL;
    public static final RegistryObject<Block> ENTROPY_BLOCK = ModBlocks.ENTROPY_BLOCK;
    public static final RegistryObject<Block> CHAOS_PORTAL = ModBlocks.CHAOS_PORTAL;
    public static final RegistryObject<Block> ENTROPY_VEIN = ModBlocks.ENTROPY_VEIN;
    public static final RegistryObject<Block> SANDSTONE_CORNER = ModBlocks.SANDSTONE_CORNER;
    public static final RegistryObject<Block> SANDSTONE_LINE = ModBlocks.SANDSTONE_LINE;
    public static final RegistryObject<Block> FLOWERY = ModBlocks.FLOWERY;
    public static final RegistryObject<Block> CACTULO = ModBlocks.CACTULO;
    public static final RegistryObject<Block> POTTED_FLOWERY = ModBlocks.POTTED_FLOWERY;
    public static final RegistryObject<Block> POTTED_CACTULO = ModBlocks.POTTED_CACTULO;
    public static final RegistryObject<Block> GEN_VASE = ModBlocks.GEN_VASE;
    public static final RegistryObject<Block> BALL_CACTUS = ModBlocks.BALL_CACTUS;
    public static final RegistryObject<Block> AZAZEL_DESERTSTATUE = ModBlocks.AZAZEL_DESERTSTATUE;
    public static final RegistryObject<Block> STORM_TOTEM = ModBlocks.STORM_TOTEM;
    public static final RegistryObject<Block> STAT_LANTERN = ModBlocks.STAT_LANTERN;
    public static final RegistryObject<Block> NEPHRITIS_LAMP = ModBlocks.NEPHRITIS_LAMP;
    public static final RegistryObject<Block> STAT = ModBlocks.STAT;
    public static final RegistryObject<Block> SANDED_CHEST = ModBlocks.SANDED_CHEST;
    public static final RegistryObject<Block> DASHER_STATUE = ModBlocks.DASHER_STATUE;
    public static final RegistryObject<Block> MONKI_STATUE = ModBlocks.MONKI_STATUE;
    public static final RegistryObject<Block> SKELET_BELIEVER = ModBlocks.SKELET_BELIEVER;
    public static final RegistryObject<Block> SAND_GOOSE = ModBlocks.SAND_GOOSE;
    public static final RegistryObject<Block> CHAOS_ALTAR = ModBlocks.CHAOS_ALTAR;
    public static final RegistryObject<Block> TITANA_STATUE = ModBlocks.TITANA_STATUE;
    public static final RegistryObject<Block> CACTOS = ModBlocks.CACTOS;
    public static final RegistryObject<Block> DOUM_PALM_LOG = ModBlocks.DOUM_PALM_LOG;
    public static final RegistryObject<Block> DOUM_PALM_WOOD = ModBlocks.DOUM_PALM_WOOD;
    public static final RegistryObject<Block> STRIPPED_DOUM_PALM_LOG = ModBlocks.STRIPPED_DOUM_PALM_LOG;
    public static final RegistryObject<Block> STRIPPED_DOUM_PALM_WOOD = ModBlocks.STRIPPED_DOUM_PALM_WOOD;
    public static final RegistryObject<Block> DOUM_PALM_PLANKS = ModBlocks.DOUM_PALM_PLANKS;
    public static final RegistryObject<Block> DOUM_PALM_SLAB = ModBlocks.DOUM_PALM_SLAB;
    public static final RegistryObject<Block> DOUM_PALM_STAIRS = ModBlocks.DOUM_PALM_STAIRS;
    public static final RegistryObject<Block> DOUM_PALM_LEAVES = ModBlocks.DOUM_PALM_LEAVES;
    public static final RegistryObject<Block> DOUM_PALM_SAPLING = ModBlocks.DOUM_PALM_SAPLING;
    public static final RegistryObject<Block> DOUM_PALM_DOOR = ModBlocks.DOUM_PALM_DOOR;
    public static final RegistryObject<Block> DOUM_PALM_TRAPDOOR = ModBlocks.DOUM_PALM_TRAPDOOR;
    public static final RegistryObject<Block> DOUM_PALM_FENCE = ModBlocks.DOUM_PALM_FENCE;
    public static final RegistryObject<Block> DOUM_PALM_FENCE_GATE = ModBlocks.DOUM_PALM_FENCE_GATE;
    public static final RegistryObject<Block> DOUM_PALM_BUTTON = ModBlocks.DOUM_PALM_BUTTON;
    public static final RegistryObject<Block> DOUM_PALM_PRESSURE_PLATE = ModBlocks.DOUM_PALM_PRESSURE_PLATE;
    public static final RegistryObject<Block> DOUM_PALM_SIGN = ModBlocks.DOUM_PALM_SIGN;
    public static final RegistryObject<Block> DOUM_PALM_WALL_SIGN = ModBlocks.DOUM_PALM_WALL_SIGN;
    public static final RegistryObject<Block> DOUM_PALM_HANGING_SIGN = ModBlocks.DOUM_PALM_HANGING_SIGN;
    public static final RegistryObject<Block> DOUM_PALM_WALL_HANGING_SIGN = ModBlocks.DOUM_PALM_WALL_HANGING_SIGN;

    // Items
    public static final RegistryObject<Item> LIE_BLOCK_ITEM = ModItems.LIE_BLOCK_ITEM;
    public static final RegistryObject<Item> SANDSTONE_TILES_ITEM = ModItems.SANDSTONE_TILES_ITEM;
    public static final RegistryObject<Item> SANDSTONE_POLISHED_ITEM = ModItems.SANDSTONE_POLISHED_ITEM;
    public static final RegistryObject<Item> CHAOS_EYE_ITEM = ModItems.CHAOS_EYE_ITEM;
    public static final RegistryObject<Item> SANDSTONE_COLUMN_ITEM = ModItems.SANDSTONE_COLUMN_ITEM;
    public static final RegistryObject<Item> SCARLET_LEAVES_ITEM = ModItems.SCARLET_LEAVES_ITEM;
    public static final RegistryObject<Item> SCARLET_GRASS_ITEM = ModItems.SCARLET_GRASS_ITEM;
    public static final RegistryObject<Item> SCARLET_LOG_ITEM = ModItems.SCARLET_LOG_ITEM;
    public static final RegistryObject<Item> NEPHRITIS_COLUMN_ITEM = ModItems.NEPHRITIS_COLUMN_ITEM;
    public static final RegistryObject<Item> NEPHRITIS_BRICKS_ITEM = ModItems.NEPHRITIS_BRICKS_ITEM;
    public static final RegistryObject<Item> NEPHRITIS_COMPRESSED_ITEM = ModItems.NEPHRITIS_COMPRESSED_ITEM;
    public static final RegistryObject<Item> NEPHRITIS_SPIRAL_ITEM = ModItems.NEPHRITIS_SPIRAL_ITEM;
    public static final RegistryObject<Item> NEPHRITIS_POLISHED_ITEM = ModItems.NEPHRITIS_POLISHED_ITEM;
    public static final RegistryObject<Item> NEPHRITIS_CORNER_ITEM = ModItems.NEPHRITIS_CORNER_ITEM;
    public static final RegistryObject<Item> NEPHRITIS_LINE_ITEM = ModItems.NEPHRITIS_LINE_ITEM;
    public static final RegistryObject<Item> SANDSTONE_COLORED_ITEM = ModItems.SANDSTONE_COLORED_ITEM;
    public static final RegistryObject<Item> SANDSTONE_STRIPE_ITEM = ModItems.SANDSTONE_STRIPE_ITEM;
    public static final RegistryObject<Item> SANDSTONE_FLOORB_ITEM = ModItems.SANDSTONE_FLOORB_ITEM;
    public static final RegistryObject<Item> KARAKOLIT_BLOCK_ITEM = ModItems.KARAKOLIT_BLOCK_ITEM;
    public static final RegistryObject<Item> NEPHRITIS_BLOCK_ITEM = ModItems.NEPHRITIS_BLOCK_ITEM;
    public static final RegistryObject<Item> WIZARD_EYE_ITEM = ModItems.WIZARD_EYE_ITEM;
    public static final RegistryObject<Item> WIZARD_COLUMN_ITEM = ModItems.WIZARD_COLUMN_ITEM;
    public static final RegistryObject<Item> SANDSTONE_ROOF_ITEM = ModItems.SANDSTONE_ROOF_ITEM;
    public static final RegistryObject<Item> SANDSTONE_BRICKED_ITEM = ModItems.SANDSTONE_BRICKED_ITEM;
    public static final RegistryObject<Item> SANDSTONE_AZAZEL_ITEM = ModItems.SANDSTONE_AZAZEL_ITEM;
    public static final RegistryObject<Item> ENTROPY_BLOCK_ITEM = ModItems.ENTROPY_BLOCK_ITEM;
    public static final RegistryObject<Item> ENTROPY_VEIN_ITEM = ModItems.ENTROPY_VEIN_ITEM;
    public static final RegistryObject<Item> SANDSTONE_CORNER_ITEM = ModItems.SANDSTONE_CORNER_ITEM;
    public static final RegistryObject<Item> SANDSTONE_LINE_ITEM = ModItems.SANDSTONE_LINE_ITEM;
    public static final RegistryObject<Item> FLOWERY_ITEM = ModItems.FLOWERY_ITEM;
    public static final RegistryObject<Item> CACTULO_ITEM = ModItems.CACTULO_ITEM;
    public static final RegistryObject<Item> GEN_VASE_ITEM = ModItems.GEN_VASE_ITEM;
    public static final RegistryObject<Item> BALL_CACTUS_ITEM = ModItems.BALL_CACTUS_ITEM;
    public static final RegistryObject<Item> STORM_TOTEM_ITEM = ModItems.STORM_TOTEM_ITEM;
    public static final RegistryObject<Item> AZAZEL_DESERTSTATUE_ITEM = ModItems.AZAZEL_DESERTSTATUE_ITEM;
    public static final RegistryObject<Item> STAT_LANTERN_ITEM = ModItems.STAT_LANTERN_ITEM;
    public static final RegistryObject<Item> NEPHRITIS_LAMP_ITEM = ModItems.NEPHRITIS_LAMP_ITEM;
    public static final RegistryObject<Item> STAT_ITEM = ModItems.STAT_ITEM;
    public static final RegistryObject<Item> SANDED_CHEST_ITEM = ModItems.SANDED_CHEST_ITEM;
    public static final RegistryObject<Item> DASHER_STATUE_ITEM = ModItems.DASHER_STATUE_ITEM;
    public static final RegistryObject<Item> MONKI_STATUE_ITEM = ModItems.MONKI_STATUE_ITEM;
    public static final RegistryObject<Item> SKELET_BELIEVER_ITEM = ModItems.SKELET_BELIEVER_ITEM;
    public static final RegistryObject<Item> SAND_GOOSE_ITEM = ModItems.SAND_GOOSE_ITEM;
    public static final RegistryObject<Item> CHAOS_ALTAR_ITEM = ModItems.CHAOS_ALTAR_ITEM;
    public static final RegistryObject<Item> TITANA_STATUE_ITEM = ModItems.TITANA_STATUE_ITEM;
    public static final RegistryObject<Item> CACTOS_ITEM = ModItems.CACTOS_ITEM;
    public static final RegistryObject<Item> KARAKOLIT_INGOT = ModItems.KARAKOLIT_INGOT;
    public static final RegistryObject<Item> NEPHRITIS = ModItems.NEPHRITIS;
    public static final RegistryObject<Item> NEPHRITIS_CORE = ModItems.NEPHRITIS_CORE;
    public static final RegistryObject<Item> ORB_CHAOS = ModItems.ORB_CHAOS;
    public static final RegistryObject<Item> ORB_DOMINATION = ModItems.ORB_DOMINATION;
    public static final RegistryObject<Item> ENCHANTED_BOOK_HAMMER = ModItems.ENCHANTED_BOOK_HAMMER;
    public static final RegistryObject<Item> BOMBUL_BOTTLE = ModItems.BOMBUL_BOTTLE;
    public static final RegistryObject<Item> BOMBUL_BOTTLE_EMPTY = ModItems.BOMBUL_BOTTLE_EMPTY;
    public static final RegistryObject<Item> TITANA_HAMMER = ModItems.TITANA_HAMMER;
    public static final RegistryObject<Item> SUPER_GOLD_HELMET = ModItems.SUPER_GOLD_HELMET;
    public static final RegistryObject<Item> SUPER_GOLD_CHESTPLATE = ModItems.SUPER_GOLD_CHESTPLATE;
    public static final RegistryObject<Item> SUPER_GOLD_LEGGINGS = ModItems.SUPER_GOLD_LEGGINGS;
    public static final RegistryObject<Item> SUPER_GOLD_BOOTS = ModItems.SUPER_GOLD_BOOTS;
    public static final RegistryObject<Item> AZUMALIT_HELMET = ModItems.AZUMALIT_HELMET;
    public static final RegistryObject<Item> AZUMALIT_CHESTPLATE = ModItems.AZUMALIT_CHESTPLATE;
    public static final RegistryObject<Item> AZUMALIT_LEGGINGS = ModItems.AZUMALIT_LEGGINGS;
    public static final RegistryObject<Item> AZUMALIT_BOOTS = ModItems.AZUMALIT_BOOTS;
    public static final RegistryObject<Item> CASSASIN_SPAWN_EGG = ModItems.CASSASIN_SPAWN_EGG;
    public static final RegistryObject<Item> CTANK_SPAWN_EGG = ModItems.CTANK_SPAWN_EGG;
    public static final RegistryObject<Item> CWARRIOR_SPAWN_EGG = ModItems.CWARRIOR_SPAWN_EGG;
    public static final RegistryObject<Item> CWIZARD_SPAWN_EGG = ModItems.CWIZARD_SPAWN_EGG;
    public static final RegistryObject<Item> CPALADIN_SPAWN_EGG = ModItems.CPALADIN_SPAWN_EGG;
    public static final RegistryObject<Item> MONKI_SPAWN_EGG = ModItems.MONKI_SPAWN_EGG;
    public static final RegistryObject<Item> KROMBUL_SPAWN_EGG = ModItems.KROMBUL_SPAWN_EGG;
    public static final RegistryObject<Item> BOMBUL_SPAWN_EGG = ModItems.BOMBUL_SPAWN_EGG;
    public static final RegistryObject<Item> TITANA_SPAWN_EGG = ModItems.TITANA_SPAWN_EGG;
    public static final RegistryObject<Item> CACTO_SPAWN_EGG = ModItems.CACTO_SPAWN_EGG;
    public static final RegistryObject<Item> DASHER_SPAWN_EGG = ModItems.DASHER_SPAWN_EGG;
    public static final RegistryObject<Item> CASER_SPAWN_EGG = ModItems.CASER_SPAWN_EGG;
    public static final RegistryObject<Item> SAND_GOLEM_SPAWN_EGG = ModItems.SAND_GOLEM_SPAWN_EGG;
    public static final RegistryObject<Item> DOUM_PALM_SIGN_ITEM = ModItems.DOUM_PALM_SIGN_ITEM;
    public static final RegistryObject<Item> DOUM_PALM_HANGING_SIGN_ITEM = ModItems.DOUM_PALM_HANGING_SIGN_ITEM;
    public static final RegistryObject<Item> DOUM_PALM_BUTTON_ITEM = ModItems.DOUM_PALM_BUTTON_ITEM;
    public static final RegistryObject<Item> DOUM_PALM_PRESSURE_PLATE_ITEM = ModItems.DOUM_PALM_PRESSURE_PLATE_ITEM;
    public static final RegistryObject<Item> DOUM_PALM_SLAB_ITEM = ModItems.DOUM_PALM_SLAB_ITEM;
    public static final RegistryObject<Item> DOUM_PALM_STAIRS_ITEM = ModItems.DOUM_PALM_STAIRS_ITEM;
    public static final RegistryObject<Item> DOUM_PALM_WOOD_ITEM = ModItems.DOUM_PALM_WOOD_ITEM;
    public static final RegistryObject<Item> STRIPPED_DOUM_PALM_WOOD_ITEM = ModItems.STRIPPED_DOUM_PALM_WOOD_ITEM;
    public static final RegistryObject<Item> DOUM_PALM_LOG_ITEM = ModItems.DOUM_PALM_LOG_ITEM;
    public static final RegistryObject<Item> STRIPPED_DOUM_PALM_LOG_ITEM = ModItems.STRIPPED_DOUM_PALM_LOG_ITEM;
    public static final RegistryObject<Item> DOUM_PALM_PLANKS_ITEM = ModItems.DOUM_PALM_PLANKS_ITEM;
    public static final RegistryObject<Item> DOUM_PALM_LEAVES_ITEM = ModItems.DOUM_PALM_LEAVES_ITEM;
    public static final RegistryObject<Item> DOUM_PALM_SAPLING_ITEM = ModItems.DOUM_PALM_SAPLING_ITEM;
    public static final RegistryObject<Item> DOUM_PALM_DOOR_ITEM = ModItems.DOUM_PALM_DOOR_ITEM;
    public static final RegistryObject<Item> DOUM_PALM_TRAPDOOR_ITEM = ModItems.DOUM_PALM_TRAPDOOR_ITEM;
    public static final RegistryObject<Item> DOUM_PALM_FENCE_ITEM = ModItems.DOUM_PALM_FENCE_ITEM;
    public static final RegistryObject<Item> DOUM_PALM_FENCE_GATE_ITEM = ModItems.DOUM_PALM_FENCE_GATE_ITEM;
    public static final RegistryObject<Item> DOUM_PALM_BOAT_ITEM = ModItems.DOUM_PALM_BOAT_ITEM;
    public static final RegistryObject<Item> DOUM_PALM_CHEST_BOAT_ITEM = ModItems.DOUM_PALM_CHEST_BOAT_ITEM;

    // Block entities
    public static final RegistryObject<BlockEntityType<NephritisLampBlockEntity>> NEPHRITIS_LAMP_BE = ModBlockEntities.NEPHRITIS_LAMP_BE;
    public static final RegistryObject<BlockEntityType<StormTotemBlockEntity>> STORM_TOTEM_BLOCK_ENTITY = ModBlockEntities.STORM_TOTEM_BLOCK_ENTITY;
    public static final RegistryObject<BlockEntityType<StatBlockEntity>> STAT_BE = ModBlockEntities.STAT_BE;
    public static final RegistryObject<BlockEntityType<StatueBlockEntity>> STATUE_BE = ModBlockEntities.STATUE_BE;
    public static final RegistryObject<BlockEntityType<com.benji.oasiso.common.block.entity.LieBlockEntity>> LIE_BLOCK_BE = ModBlockEntities.LIE_BLOCK_BE;
    public static final RegistryObject<BlockEntityType<ChaosAltarBlockEntity>> CHAOS_ALTAR_BE = ModBlockEntities.CHAOS_ALTAR_BE;
    public static final RegistryObject<BlockEntityType<SandedChestBlockEntity>> SANDED_CHEST_BE = ModBlockEntities.SANDED_CHEST_BE;
    public static final RegistryObject<BlockEntityType<com.benji.oasiso.common.block.entity.DoumPalmSignBlockEntity>> DOUM_PALM_SIGN_BE = ModBlockEntities.DOUM_PALM_SIGN_BE;
    public static final RegistryObject<BlockEntityType<com.benji.oasiso.common.block.entity.DoumPalmHangingSignBlockEntity>> DOUM_PALM_HANGING_SIGN_BE = ModBlockEntities.DOUM_PALM_HANGING_SIGN_BE;

    // Entities
    public static final RegistryObject<EntityType<ScarabEntity>> SCARAB = ModEntities.SCARAB;
    public static final RegistryObject<EntityType<KrombulEntity>> KROMBUL = ModEntities.KROMBUL;
    public static final RegistryObject<EntityType<AzumaalEntity>> AZUMAAL = ModEntities.AZUMAAL;
    public static final RegistryObject<EntityType<ChaosBombEntity>> CHAOS_BOMB = ModEntities.CHAOS_BOMB;
    public static final RegistryObject<EntityType<EyelidEntity>> EYELID = ModEntities.EYELID;
    public static final RegistryObject<EntityType<CircleHintEntity>> CIRCLE_HINT = ModEntities.CIRCLE_HINT;
    public static final RegistryObject<EntityType<BattleHintArrowEntity>> BATTLE_HINT_ARROW = ModEntities.BATTLE_HINT_ARROW;
    public static final RegistryObject<EntityType<BossPortalEntity>> BOSS_PORTAL = ModEntities.BOSS_PORTAL;
    public static final RegistryObject<EntityType<DamageNumberEntity>> DAMAGE_NUMBER = ModEntities.DAMAGE_NUMBER;
    public static final RegistryObject<EntityType<WizardPillarEntity>> WIZARD_PILLAR_ENTITY = ModEntities.WIZARD_PILLAR_ENTITY;
    public static final RegistryObject<EntityType<MonkiEntity>> MONKI = ModEntities.MONKI;
    public static final RegistryObject<EntityType<CrusaderTankEntity>> CRUSADER_TANK = ModEntities.CRUSADER_TANK;
    public static final RegistryObject<EntityType<CrusaderWarriorEntity>> CRUSADER_WARRIOR = ModEntities.CRUSADER_WARRIOR;
    public static final RegistryObject<EntityType<PaladinEntity>> PALADIN = ModEntities.PALADIN;
    public static final RegistryObject<EntityType<SwordHeartEntity>> SWORD_HEART = ModEntities.SWORD_HEART;
    public static final RegistryObject<EntityType<CrusaderAssasinEntity>> CRUSADER_ASSASIN = ModEntities.CRUSADER_ASSASIN;
    public static final RegistryObject<EntityType<CrusaderWizardEntity>> CRUSADER_WIZARD = ModEntities.CRUSADER_WIZARD;
    public static final RegistryObject<EntityType<GasterEntity>> GASTER = ModEntities.GASTER;
    public static final RegistryObject<EntityType<DesertBallEntity>> DESERT_BALL = ModEntities.DESERT_BALL;
    public static final RegistryObject<EntityType<CactoProjEntity>> CACTO_PROJ = ModEntities.CACTO_PROJ;
    public static final RegistryObject<EntityType<MonkiBigEntity>> MONKI_BIG = ModEntities.MONKI_BIG;
    public static final RegistryObject<EntityType<TitanaEntity>> TITANA = ModEntities.TITANA;
    public static final RegistryObject<EntityType<SandGolemEntity>> SAND_GOLEM = ModEntities.SAND_GOLEM;
    public static final RegistryObject<EntityType<CaserEntity>> CASER = ModEntities.CASER;
    public static final RegistryObject<EntityType<SandHandEntity>> SAND_HAND = ModEntities.SAND_HAND;
    public static final RegistryObject<EntityType<DasherEntity>> DASHER = ModEntities.DASHER;
    public static final RegistryObject<EntityType<BombulEntity>> BOMBUL = ModEntities.BOMBUL;
    public static final RegistryObject<EntityType<CactoEntity>> CACTO = ModEntities.CACTO;
    public static final RegistryObject<EntityType<DoumPalmBoatEntity>> DOUM_PALM_BOAT = ModEntities.DOUM_PALM_BOAT;
    public static final RegistryObject<EntityType<DoumPalmChestBoatEntity>> DOUM_PALM_CHEST_BOAT = ModEntities.DOUM_PALM_CHEST_BOAT;

    // Effects
    public static final RegistryObject<MobEffect> ENTROPY_EFFECT = ModEffects.ENTROPY_EFFECT;
    public static final RegistryObject<MobEffect> BOMBUL_BUFF_EFFECT = ModEffects.BOMBUL_BUFF_EFFECT;
    public static final RegistryObject<MobEffect> CHAOS_CHAMBER_EFFECT = ModEffects.CHAOS_CHAMBER_EFFECT;
    public static final RegistryObject<MobEffect> SMELL_OF_SIN_EFFECT = ModEffects.SMELL_OF_SIN_EFFECT;

    public Oasiso(FMLJavaModLoadingContext context) {
        context.registerConfig(
                ModConfig.Type.COMMON,
                OsirisRealmConfig.SPEC,
                "Osiris' Realm Config.toml"
        );

        IEventBus modEventBus = context.getModEventBus();

        ModBlocks.register(modEventBus);
        ModItems.register(modEventBus);
        ModBlockEntities.register(modEventBus);
        ModEntities.register(modEventBus);
        ModSounds.SOUNDS.register(modEventBus);
        CREATIVE_MODE_TABS.register(modEventBus);
        STRUCTURE_TYPES.register(modEventBus);
        ENCHANTMENTS.register(modEventBus);
        ModEffects.register(modEventBus);
        PARTICLES.register(modEventBus);
        modEventBus.addListener(this::setup);
        modEventBus.addListener(this::addCreative);
        MinecraftForge.EVENT_BUS.register(this);
    }

    private void setup(final FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            ModDispenserBehaviors.register();
            ModMessages.register();
            BossPortalTransitionNetwork.register();
            BossDialogueNetwork.register();

            ((FlowerPotBlock) Blocks.FLOWER_POT).addPlant(ModBlocks.FLOWERY.getId(), ModBlocks.POTTED_FLOWERY);

            ((FlowerPotBlock) Blocks.FLOWER_POT).addPlant(ModBlocks.CACTULO.getId(), ModBlocks.POTTED_CACTULO);
        });
    }

    private void addCreative(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.NATURAL_BLOCKS || event.getTabKey() == CreativeModeTabs.BUILDING_BLOCKS) {

            event.accept(ModItems.SANDSTONE_BRICKED_ITEM);
            event.accept(ModItems.SANDSTONE_LINE_ITEM);
            event.accept(ModItems.SANDSTONE_CORNER_ITEM);
            event.accept(ModItems.SANDSTONE_AZAZEL_ITEM);
            event.accept(ModItems.SANDSTONE_ROOF_ITEM);
            event.accept(ModItems.SANDSTONE_FLOORB_ITEM);
            event.accept(ModItems.SANDSTONE_TILES_ITEM);
            event.accept(ModItems.SANDSTONE_STRIPE_ITEM);
            event.accept(ModItems.SANDSTONE_COLORED_ITEM);
            event.accept(ModItems.SANDSTONE_COLUMN_ITEM);
            event.accept(ModItems.FLOWERY_ITEM);
            event.accept(ModItems.CACTULO_ITEM);
            event.accept(ModItems.CACTOS_ITEM);
            event.accept(ModItems.NEPHRITIS_BLOCK_ITEM);
            event.accept(ModItems.KARAKOLIT_BLOCK_ITEM);
        }

        if (event.getTabKey() == CreativeModeTabs.FUNCTIONAL_BLOCKS) {
            event.accept(ModItems.GEN_VASE_ITEM);
            event.accept(ModItems.STAT_LANTERN_ITEM);
            event.accept(ModBlocks.STAT);
            event.accept(ModItems.SANDED_CHEST_ITEM);
            event.accept(ModItems.DASHER_STATUE_ITEM);
            event.accept(ModItems.TITANA_STATUE_ITEM);
            event.accept(ModItems.AZAZEL_DESERTSTATUE_ITEM);
            event.accept(ModItems.MONKI_STATUE_ITEM);
        }

        if (event.getTabKey() == CreativeModeTabs.SPAWN_EGGS) {
            event.accept(ModItems.CACTO_SPAWN_EGG);
            event.accept(ModItems.MONKI_SPAWN_EGG);
            event.accept(ModItems.TITANA_SPAWN_EGG);
            event.accept(ModItems.DASHER_SPAWN_EGG);
            event.accept(ModItems.SAND_GOLEM_SPAWN_EGG);
            event.accept(ModItems.CASER_SPAWN_EGG);
        }

        if (event.getTabKey() == CreativeModeTabs.COMBAT) {
            event.accept(ModItems.SUPER_GOLD_BOOTS);
            event.accept(ModItems.SUPER_GOLD_HELMET);
            event.accept(ModItems.SUPER_GOLD_CHESTPLATE);
            event.accept(ModItems.SUPER_GOLD_LEGGINGS);
            event.accept(ModItems.TITANA_HAMMER);
        }

        if (event.getTabKey() == CreativeModeTabs.INGREDIENTS) {
            event.accept(ModItems.ENCHANTED_BOOK_HAMMER);
            event.accept(ModItems.NEPHRITIS);
            event.accept(ModItems.NEPHRITIS_CORE);
            event.accept(ModItems.KARAKOLIT_INGOT);
        }
    }

    @Mod.EventBusSubscriber(modid = MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static class ModEvents {

        @SubscribeEvent
        public static void registerAttributes(EntityAttributeCreationEvent event) {
            event.put(ModEntities.SCARAB.get(), ScarabEntity.createAttributes().build());
            event.put(ModEntities.MONKI.get(), MonkiEntity.createAttributes().build());
            event.put(ModEntities.GASTER.get(), GasterEntity.createAttributes().build());
            event.put(ModEntities.BOSS_PORTAL.get(), BossPortalEntity.createAttributes().build());
            event.put(ModEntities.AZUMAAL.get(), AzumaalEntity.createAttributes().build());
            event.put(ModEntities.CHAOS_BOMB.get(), ChaosBombEntity.createAttributes().build());
            event.put(ModEntities.SAND_GOLEM.get(), SandGolemEntity.createAttributes().build());
            event.put(ModEntities.CASER.get(), CaserEntity.createAttributes().build());
            event.put(ModEntities.CRUSADER_TANK.get(), CrusaderTankEntity.createAttributes().build());
            event.put(ModEntities.CRUSADER_WARRIOR.get(), CrusaderWarriorEntity.createAttributes().build());
            event.put(ModEntities.PALADIN.get(), PaladinEntity.createAttributes().build());
            event.put(ModEntities.SWORD_HEART.get(), SwordHeartEntity.createAttributes().build());
            event.put(ModEntities.CRUSADER_WIZARD.get(), CrusaderWizardEntity.createAttributes().build());
            event.put(ModEntities.CRUSADER_ASSASIN.get(), CrusaderAssasinEntity.createAttributes().build());
            event.put(ModEntities.MONKI_BIG.get(), MonkiBigEntity.createAttributes().build());
            event.put(ModEntities.BATTLE_HINT_ARROW.get(), BattleHintArrowEntity.createAttributes().build());
            event.put(ModEntities.CIRCLE_HINT.get(), CircleHintEntity.createAttributes().build());
            event.put(ModEntities.TITANA.get(), TitanaEntity.createAttributes().build());
            event.put(ModEntities.SAND_HAND.get(), SandHandEntity.createAttributes().build());
            event.put(ModEntities.KROMBUL.get(), KrombulEntity.createAttributes().build());
            event.put(ModEntities.BOMBUL.get(), BombulEntity.createAttributes().build());
            event.put(ModEntities.DASHER.get(), DasherEntity.createAttributes().build());
            event.put(ModEntities.CACTO.get(), CactoEntity.createAttributes().build());
        }
    }

    @Mod.EventBusSubscriber(modid = MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
    public static class ForgeEvents {

        @SubscribeEvent
        public static void onToolModification(net.minecraftforge.event.level.BlockEvent.BlockToolModificationEvent event) {
            if (event.getToolAction() != net.minecraftforge.common.ToolActions.AXE_STRIP) {
                return;
            }

            if (event.getState().is(ModBlocks.DOUM_PALM_LOG.get())) {
                event.setFinalState(ModBlocks.STRIPPED_DOUM_PALM_LOG.get().defaultBlockState().setValue(RotatedPillarBlock.AXIS, event.getState().getValue(RotatedPillarBlock.AXIS)));
            } else if (event.getState().is(ModBlocks.DOUM_PALM_WOOD.get())) {
                event.setFinalState(ModBlocks.STRIPPED_DOUM_PALM_WOOD.get().defaultBlockState().setValue(RotatedPillarBlock.AXIS, event.getState().getValue(RotatedPillarBlock.AXIS)));
            }
        }
    }
}
