package com.benji.oasiso;

import com.benji.oasiso.client.renderer.CactoRenderer;
import com.benji.oasiso.client.renderer.MonkiRenderer;
import com.benji.oasiso.network.BossPortalTransitionNetwork;
import com.benji.oasiso.common.block.entity.*;
import com.benji.oasiso.common.effect.ChaosChamberEffect;
import com.benji.oasiso.network.dialogue.BossDialogueNetwork;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import com.benji.oasiso.common.block.ChaosAltarBlock;
import net.minecraft.world.level.Level;
import com.benji.oasiso.common.block.*;
import com.benji.oasiso.common.enchantment.HammerPowerEnchantment;
import com.benji.oasiso.common.block.ChaosPortalBlock;
import net.minecraft.world.item.enchantment.Enchantment;
import com.benji.oasiso.common.effect.BombulBuffEffect;
import com.benji.oasiso.common.entity.*;
import com.benji.oasiso.common.entity.projectile.CactoProjEntity;
import com.benji.oasiso.common.dispenser.ModDispenserBehaviors;
import com.benji.oasiso.common.entity.projectile.DesertBallEntity;
import com.benji.oasiso.common.item.*;
import com.benji.oasiso.network.ModMessages;
import com.mojang.logging.LogUtils;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
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
import com.benji.oasiso.common.effect.EntropyEffect;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.world.effect.MobEffect;
import com.benji.oasiso.common.item.HammerEnchantmentBookItem;
import org.slf4j.Logger;

@Mod(Oasiso.MODID)
public class Oasiso {
    public static final String MODID = "oasiso";
    private static final Logger LOGGER = LogUtils.getLogger();
    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, MODID);
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, MODID);
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, MODID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, MODID);
    public static final DeferredRegister<StructureType<?>> STRUCTURE_TYPES =
            DeferredRegister.create(net.minecraft.core.registries.Registries.STRUCTURE_TYPE, MODID);

    public static final ResourceKey<Level> CHAOS_DIMENSION =
            ResourceKey.create(
                    Registries.DIMENSION,
                    ResourceLocation.fromNamespaceAndPath(
                            MODID,
                            "chaos_dimension"
                    )
            );

    public static final ResourceLocation CHAOS_SKY_EFFECTS =
            ResourceLocation.fromNamespaceAndPath(
                    MODID,
                    "chaos_sky"
            );

    public static final DeferredRegister<Enchantment> ENCHANTMENTS =
            DeferredRegister.create(
                    ForgeRegistries.ENCHANTMENTS,
                    MODID
            );

    public static final DeferredRegister<MobEffect> MOB_EFFECTS =
            DeferredRegister.create(ForgeRegistries.MOB_EFFECTS, MODID);

    public static final DeferredRegister<ParticleType<?>> PARTICLES =
            DeferredRegister.create(ForgeRegistries.PARTICLE_TYPES, MODID);

    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MODID);

    public static final net.minecraft.world.level.block.state.properties.BlockSetType DOUM_PALM_SET_TYPE =
            net.minecraft.world.level.block.state.properties.BlockSetType.register(
                    new net.minecraft.world.level.block.state.properties.BlockSetType(Oasiso.MODID + ":doum_palm")
            );

    public static final net.minecraft.world.level.block.state.properties.WoodType DOUM_PALM_WOOD_TYPE =
            net.minecraft.world.level.block.state.properties.WoodType.register(
                    new net.minecraft.world.level.block.state.properties.WoodType(Oasiso.MODID + ":doum_palm", DOUM_PALM_SET_TYPE)
            );

    //ENCHANTMENTS
    public static final RegistryObject<Enchantment> HAMMER_POWER =
            ENCHANTMENTS.register(
                    "hammer_power",
                    HammerPowerEnchantment::new
            );

    //HITBOXES
    private static final net.minecraft.world.phys.shapes.VoxelShape MONKI_SHAPE = Block.box(3.0D, 0.0D, 3.0D, 13.0D, 20.0D, 13.0D);
    private static final net.minecraft.world.phys.shapes.VoxelShape DASHER_SHAPE = Block.box(0.0D, 0.0D, 0.0D, 16.0D, 50.0D, 16.0D);
    private static final net.minecraft.world.phys.shapes.VoxelShape TITANA_SHAPE = Block.box(-6.0D, 0.0D, -6.0D, 22.0D, 40.0D, 22.0D);

    //EFFECT & PARTICLES

    public static final RegistryObject<SimpleParticleType> WIZARD_PIXELS =
            PARTICLES.register(
                    "wizard_pixels",
                    () -> new SimpleParticleType(false)
            );

    public static final RegistryObject<MobEffect> ENTROPY_EFFECT =
            MOB_EFFECTS.register("entropy", EntropyEffect::new);

    public static final RegistryObject<MobEffect> BOMBUL_BUFF_EFFECT =
            MOB_EFFECTS.register(
                    "bombul_buff",
                    BombulBuffEffect::new
            );

    public static final RegistryObject<MobEffect>
            CHAOS_CHAMBER_EFFECT =
            MOB_EFFECTS.register(
                    "chaos_chamber",
                    ChaosChamberEffect::new
            );

    public static final RegistryObject<SimpleParticleType> PURPLE_STARS =
            PARTICLES.register("purple_stars", () -> new SimpleParticleType(false));

    public static final RegistryObject<SimpleParticleType> GOLDEN_STARS =
            PARTICLES.register(
                    "golden_stars",
                    () -> new SimpleParticleType(false)
            );

    public static final RegistryObject<SimpleParticleType> GOLDEN_HEART =
            PARTICLES.register(
                    "golden_heart",
                    () -> new SimpleParticleType(false)
            );

    public static final RegistryObject<SimpleParticleType>
            CHAOS_BOMB_CENTER_SMOKE =
            PARTICLES.register(
                    "chaos_bomb_center_smoke",
                    () -> new SimpleParticleType(
                            false
                    )
            );

    public static final RegistryObject<SimpleParticleType>
            CHAOS_BOMB_FIRE_SMOKE =
            PARTICLES.register(
                    "chaos_bomb_fire_smoke",
                    () -> new SimpleParticleType(
                            false
                    )
            );

    public static final RegistryObject<SimpleParticleType>
            CHAOS_BOMB_SPARKS =
            PARTICLES.register(
                    "chaos_bomb_sparks",
                    () -> new SimpleParticleType(
                            false
                    )
            );

// BLOCKS

    public static final RegistryObject<Block> LIE_BLOCK = BLOCKS.register("lie_block",
            () -> new LieBlock(BlockBehaviour.Properties.copy(Blocks.STONE)
                    .sound(SoundType.SCULK_CATALYST )
                    .strength(80.0F)
                    .noOcclusion()
                    .explosionResistance(3_600_000.0F)
                    .dynamicShape()
                    .requiresCorrectToolForDrops()));

    public static final RegistryObject<Item> LIE_BLOCK_ITEM = ITEMS.register("lie_block",
            () -> new BlockItem(LIE_BLOCK.get(), new Item.Properties()));


    public static final RegistryObject<Block> SANDSTONE_TILES = BLOCKS.register("sandstone_tiles",
            () -> new DirectionalPatternBlock(BlockBehaviour.Properties.copy(Blocks.SANDSTONE)
                    .strength(2.0F)
                    .requiresCorrectToolForDrops()));

    public static final RegistryObject<Item> SANDSTONE_TILES_ITEM = ITEMS.register("sandstone_tiles",
            () -> new BlockItem(SANDSTONE_TILES.get(), new Item.Properties()));

    public static final RegistryObject<Block> SANDSTONE_POLISHED = BLOCKS.register("sandstone_polished",
            () -> new Block(BlockBehaviour.Properties.copy(Blocks.SANDSTONE)
                    .strength(2.0F)
                    .requiresCorrectToolForDrops()));

    public static final RegistryObject<Item> SANDSTONE_POLISHED_ITEM = ITEMS.register("sandstone_polished",
            () -> new BlockItem(SANDSTONE_POLISHED.get(), new Item.Properties()));

    public static final RegistryObject<Block> CHAOS_EYE =
            BLOCKS.register(
                    "chaos_eye",
                    () -> new ChaosEyeBlock(
                            BlockBehaviour.Properties
                                    .copy(Blocks.AMETHYST_BLOCK)
                                    .instabreak()
                                    .noOcclusion()
                                    .requiresCorrectToolForDrops()
                    )
            );

    public static final RegistryObject<Item> CHAOS_EYE_ITEM = ITEMS.register("chaos_eye",
            () -> new BlockItem(CHAOS_EYE.get(), new Item.Properties()));

    public static final RegistryObject<Block> SANDSTONE_COLUMN = BLOCKS.register("sandstone_column",
            () -> new RotatedPillarBlock(BlockBehaviour.Properties.copy(Blocks.SANDSTONE)
                    .strength(2.0F)
                    .requiresCorrectToolForDrops()));

    public static final RegistryObject<Item> SANDSTONE_COLUMN_ITEM = ITEMS.register("sandstone_column",
            () -> new BlockItem(SANDSTONE_COLUMN.get(), new Item.Properties()));


    //DELTARUNE EASTER EGG
    public static final RegistryObject<Block> SCARLET_LEAVES = BLOCKS.register("scarlet_leaves",
            () -> new Block(BlockBehaviour.Properties.copy(Blocks.SPONGE)
                    .strength(1.0F)
                    .noOcclusion()
            ));

    public static final RegistryObject<Item> SCARLET_LEAVES_ITEM = ITEMS.register("scarlet_leaves",
            () -> new BlockItem(SCARLET_LEAVES.get(), new Item.Properties()));

    public static final RegistryObject<Block> SCARLET_GRASS = BLOCKS.register("scarlet_grass",
            () -> new Block(BlockBehaviour.Properties.copy(Blocks.SPONGE)
                    .strength(1.0F)
            ));

    public static final RegistryObject<Item> SCARLET_GRASS_ITEM = ITEMS.register("scarlet_grass",
            () -> new BlockItem(SCARLET_GRASS.get(), new Item.Properties()));

    public static final RegistryObject<Block> SCARLET_LOG = BLOCKS.register("scarlet_log",
            () -> new RotatedPillarBlock(BlockBehaviour.Properties.copy(Blocks.OAK_WOOD)
                    .strength(4.0F)
                    .requiresCorrectToolForDrops()));

    public static final RegistryObject<Item> SCARLET_LOG_ITEM = ITEMS.register("scarlet_log",
            () -> new BlockItem(SCARLET_LOG.get(), new Item.Properties()));

//NEPHRITIS

    public static final RegistryObject<Block> NEPHRITIS_COLUMN = BLOCKS.register("nephritis_column",
            () -> new RotatedPillarBlock(BlockBehaviour.Properties.copy(Blocks.STONE)
                    .strength(6.0F)
                    .requiresCorrectToolForDrops()));

    public static final RegistryObject<Item> NEPHRITIS_COLUMN_ITEM = ITEMS.register("nephritis_column",
            () -> new BlockItem(NEPHRITIS_COLUMN.get(), new Item.Properties()));

    public static final RegistryObject<Block> NEPHRITIS_BRICKS = BLOCKS.register("nephritis_bricks",
            () -> new RotatedPillarBlock(BlockBehaviour.Properties.copy(Blocks.STONE)
                    .strength(8.0F)
                    .requiresCorrectToolForDrops()));

    public static final RegistryObject<Item> NEPHRITIS_BRICKS_ITEM = ITEMS.register("nephritis_bricks",
            () -> new BlockItem(NEPHRITIS_BRICKS.get(), new Item.Properties()));

    public static final RegistryObject<Block> NEPHRITIS_COMPRESSED = BLOCKS.register("nephritis_compressed",
            () -> new Block(BlockBehaviour.Properties.copy(Blocks.STONE)
                    .strength(10.0F)
                    .requiresCorrectToolForDrops()));

    public static final RegistryObject<Item> NEPHRITIS_COMPRESSED_ITEM = ITEMS.register("nephritis_compressed",
            () -> new BlockItem(NEPHRITIS_COMPRESSED.get(), new Item.Properties()));

    public static final RegistryObject<Block> NEPHRITIS_SPIRAL = BLOCKS.register("nephritis_spiral",
            () -> new Block(BlockBehaviour.Properties.copy(Blocks.STONE)
                    .strength(5.0F)
                    .requiresCorrectToolForDrops()));

    public static final RegistryObject<Item> NEPHRITIS_SPIRAL_ITEM = ITEMS.register("nephritis_spiral",
            () -> new BlockItem(NEPHRITIS_SPIRAL.get(), new Item.Properties()));

    public static final RegistryObject<Block> NEPHRITIS_POLISHED = BLOCKS.register("nephritis_polished",
            () -> new Block(BlockBehaviour.Properties.copy(Blocks.STONE)
                    .strength(3.0F)
                    .requiresCorrectToolForDrops()));

    public static final RegistryObject<Item> NEPHRITIS_POLISHED_ITEM = ITEMS.register("nephritis_polished",
            () -> new BlockItem(NEPHRITIS_POLISHED.get(), new Item.Properties()));


    public static final RegistryObject<Block> NEPHRITIS_CORNER = BLOCKS.register("nephritis_corner",
            () -> new DirectionalPillarBlock(BlockBehaviour.Properties.copy(Blocks.STONE)
                    .strength(2.0F)
                    .requiresCorrectToolForDrops()));

    public static final RegistryObject<Item> NEPHRITIS_CORNER_ITEM = ITEMS.register("nephritis_corner",
            () -> new BlockItem(NEPHRITIS_CORNER.get(), new Item.Properties()));

    public static final RegistryObject<Block> NEPHRITIS_LINE = BLOCKS.register("nephritis_line",
            () -> new DirectionalPillarBlock(BlockBehaviour.Properties.copy(Blocks.STONE)
                    .strength(2.0F)
                    .requiresCorrectToolForDrops()));

    public static final RegistryObject<Item> NEPHRITIS_LINE_ITEM = ITEMS.register("nephritis_line",
            () -> new BlockItem(NEPHRITIS_LINE.get(), new Item.Properties()));


    //=======

    public static final RegistryObject<Block> SANDSTONE_COLORED = BLOCKS.register("sandstone_colored",
            () -> new Block(BlockBehaviour.Properties.copy(Blocks.SANDSTONE)
                    .strength(2.0F)
                    .requiresCorrectToolForDrops()));

    public static final RegistryObject<Item> SANDSTONE_COLORED_ITEM = ITEMS.register("sandstone_colored",
            () -> new BlockItem(SANDSTONE_COLORED.get(), new Item.Properties()));

    public static final RegistryObject<Block> SANDSTONE_STRIPE = BLOCKS.register("sandstone_stripe",
            () -> new RotatedPillarBlock(BlockBehaviour.Properties.copy(Blocks.SANDSTONE)
                    .strength(2.0F)
                    .requiresCorrectToolForDrops()));

    public static final RegistryObject<Item> SANDSTONE_STRIPE_ITEM = ITEMS.register("sandstone_stripe",
            () -> new BlockItem(SANDSTONE_STRIPE.get(), new Item.Properties()));

    public static final RegistryObject<Block> SANDSTONE_FLOORB = BLOCKS.register("sandstone_floorb",
            () -> new Block(BlockBehaviour.Properties.copy(Blocks.SANDSTONE)
                    .strength(2.0F)
                    .requiresCorrectToolForDrops()));

    public static final RegistryObject<Item> SANDSTONE_FLOORB_ITEM = ITEMS.register("sandstone_floorb",
            () -> new BlockItem(SANDSTONE_FLOORB.get(), new Item.Properties()));

    public static final RegistryObject<Block> KARAKOLIT_BLOCK = BLOCKS.register("karakolit_block",
            () -> new Block(BlockBehaviour.Properties.copy(Blocks.NETHERITE_BLOCK)
                    .strength(5.0F)
                    .requiresCorrectToolForDrops()));

    public static final RegistryObject<Item> KARAKOLIT_BLOCK_ITEM = ITEMS.register("karakolit_block",
            () -> new BlockItem(KARAKOLIT_BLOCK.get(), new Item.Properties()));


    public static final RegistryObject<Block> NEPHRITIS_BLOCK = BLOCKS.register("nephritis_block",
            () -> new NephritisBlock(BlockBehaviour.Properties.copy(Blocks.RAW_GOLD_BLOCK)
                    .strength(5.0F)
                    .requiresCorrectToolForDrops()));

    public static final RegistryObject<Item> NEPHRITIS_BLOCK_ITEM = ITEMS.register("nephritis_block",
            () -> new BlockItem(NEPHRITIS_BLOCK.get(), new Item.Properties()));


    public static final RegistryObject<Block> WIZARD_EYE = BLOCKS.register("wizard_eye",
            () -> new Block(BlockBehaviour.Properties.copy(Blocks.STONE)
                    .strength(5.0F)
                    .requiresCorrectToolForDrops()));

    public static final RegistryObject<Item> WIZARD_EYE_ITEM = ITEMS.register("wizard_eye",
            () -> new BlockItem(WIZARD_EYE.get(), new Item.Properties()));

    public static final RegistryObject<Block> WIZARD_COLUMN = BLOCKS.register("wizard_column",
            () -> new Block(BlockBehaviour.Properties.copy(Blocks.STONE)
                    .strength(5.0F)
                    .requiresCorrectToolForDrops()));

    public static final RegistryObject<Item> WIZARD_COLUMN_ITEM = ITEMS.register("wizard_column",
            () -> new BlockItem(WIZARD_COLUMN.get(), new Item.Properties()));


    public static final RegistryObject<Block> SANDSTONE_ROOF = BLOCKS.register("sandstone_roof",
            () -> new Block(BlockBehaviour.Properties.copy(Blocks.SANDSTONE)
                    .strength(2.0F)
                    .requiresCorrectToolForDrops()));

    public static final RegistryObject<Item> SANDSTONE_ROOF_ITEM = ITEMS.register("sandstone_roof",
            () -> new BlockItem(SANDSTONE_ROOF.get(), new Item.Properties()));

    public static final RegistryObject<Block> SANDSTONE_BRICKED = BLOCKS.register("sandstone_bricked",
            () -> new Block(BlockBehaviour.Properties.copy(Blocks.SANDSTONE)
                    .strength(2.0F)
                    .requiresCorrectToolForDrops()));

    public static final RegistryObject<Item> SANDSTONE_BRICKED_ITEM = ITEMS.register("sandstone_bricked",
            () -> new BlockItem(SANDSTONE_BRICKED.get(), new Item.Properties()));

    public static final RegistryObject<Block> SANDSTONE_AZAZEL = BLOCKS.register("sandstone_azazel",
            () -> new Block(BlockBehaviour.Properties.copy(Blocks.SANDSTONE)
                    .strength(2.0F)
                    .requiresCorrectToolForDrops()));

    public static final RegistryObject<Item> SANDSTONE_AZAZEL_ITEM = ITEMS.register("sandstone_azazel",
            () -> new BlockItem(SANDSTONE_AZAZEL.get(), new Item.Properties()));

    public static final RegistryObject<Block> ENTROPY_BLOCK = BLOCKS.register("entropy_block",
            () -> new EntropyBlock(BlockBehaviour.Properties.copy(Blocks.AMETHYST_BLOCK)
                    .lightLevel(state -> 20)
                    .strength(10.0F)
                    .requiresCorrectToolForDrops()));

    public static final RegistryObject<Item> ENTROPY_BLOCK_ITEM = ITEMS.register("entropy_block",
            () -> new BlockItem(ENTROPY_BLOCK.get(), new Item.Properties()));

    public static final RegistryObject<Block> CHAOS_PORTAL =
            BLOCKS.register(
                    "chaos_portal",
                    () -> new ChaosPortalBlock(
                            BlockBehaviour.Properties.copy(
                                    Blocks.NETHER_PORTAL
                            )
                    )
            );

    public static final RegistryObject<Block> ENTROPY_VEIN = BLOCKS.register("entropy_vein",
            () -> new EntropyVeinBlock(BlockBehaviour.Properties.copy(Blocks.SCULK_VEIN)
                    .sound(SoundType.SMALL_AMETHYST_BUD )
                    .lightLevel(state -> 10)
                    .noCollission()
                    .noOcclusion()
                    .replaceable()
                    .strength(0.2F)));

    public static final RegistryObject<Item> ENTROPY_VEIN_ITEM = ITEMS.register("entropy_vein",
            () -> new BlockItem(ENTROPY_VEIN.get(), new Item.Properties()));

    public static final RegistryObject<Block> SANDSTONE_CORNER = BLOCKS.register("sandstone_corner",
            () -> new DirectionalPillarBlock(BlockBehaviour.Properties.copy(Blocks.SANDSTONE)
                    .strength(2.0F)
                    .requiresCorrectToolForDrops()));

    public static final RegistryObject<Item> SANDSTONE_CORNER_ITEM = ITEMS.register("sandstone_corner",
            () -> new BlockItem(SANDSTONE_CORNER.get(), new Item.Properties()));

    public static final RegistryObject<Block> SANDSTONE_LINE = BLOCKS.register("sandstone_line",
            () -> new DirectionalPillarBlock(BlockBehaviour.Properties.copy(Blocks.SANDSTONE)
                    .strength(2.0F)
                    .requiresCorrectToolForDrops()));

    public static final RegistryObject<Item> SANDSTONE_LINE_ITEM = ITEMS.register("sandstone_line",
            () -> new BlockItem(SANDSTONE_LINE.get(), new Item.Properties()));

    public static final RegistryObject<Block> FLOWERY = BLOCKS.register("flowery",
            () -> new OasisoFlowerBlock(
                    MobEffects.SATURATION,
                    1,
                    BlockBehaviour.Properties.copy(Blocks.DANDELION)
                            .instabreak()
                            .noOcclusion()
            ));

    public static final RegistryObject<Item> FLOWERY_ITEM = ITEMS.register("flowery",
            () -> new BlockItem(FLOWERY.get(), new Item.Properties()));

    public static final RegistryObject<Block> CACTULO = BLOCKS.register("cactulo",
            () -> new CactuloBlock(
                    MobEffects.SATURATION,
                    1,
                    BlockBehaviour.Properties.copy(Blocks.DEAD_BUSH)
                            .instabreak()
                            .noOcclusion()
            ));

    public static final RegistryObject<Item> CACTULO_ITEM = ITEMS.register("cactulo",
            () -> new BlockItem(CACTULO.get(), new Item.Properties()));

    //FLOWER POT
    public static final RegistryObject<Block> POTTED_FLOWERY = BLOCKS.register("potted_flowery",
            () -> new FlowerPotBlock(
                    () -> (FlowerPotBlock) Blocks.FLOWER_POT,
                    FLOWERY,
                    BlockBehaviour.Properties.copy(Blocks.POTTED_DANDELION)
                            .instabreak()
                            .noOcclusion()
            ));

    public static final RegistryObject<Block> POTTED_CACTULO = BLOCKS.register("potted_cactulo",
            () -> new FlowerPotBlock(
                    () -> (FlowerPotBlock) Blocks.FLOWER_POT,
                    CACTULO,
                    BlockBehaviour.Properties.copy(Blocks.POTTED_DANDELION)
                            .instabreak()
                            .noOcclusion()
            ));

    public static final RegistryObject<Block> GEN_VASE = BLOCKS.register("gen_vase",
            () -> new GenDecorateBlock(
                    BlockBehaviour.Properties.copy(Blocks.DECORATED_POT)
                            .sound(SoundType.DECORATED_POT)
                            .instabreak()
                            .noOcclusion()
            ));

    public static final RegistryObject<Item> GEN_VASE_ITEM = ITEMS.register("gen_vase",
            () -> new BlockItem(GEN_VASE.get(), new Item.Properties()));

    public static final RegistryObject<Block> BALL_CACTUS = BLOCKS.register("ball_cactus",
            () -> new com.benji.oasiso.common.block.BallCactusBlock( // <--- Полный путь здесь!
                    BlockBehaviour.Properties.copy(Blocks.WHITE_WOOL)
                            .sound(SoundType.WOOL )
                            .instabreak()
                            .noOcclusion()
                            .randomTicks()
            ));

    public static final RegistryObject<Item> BALL_CACTUS_ITEM = ITEMS.register("ball_cactus",
            () -> new BlockItem(BALL_CACTUS.get(), new Item.Properties()));

    public static final RegistryObject<Block> AZAZEL_DESERTSTATUE = BLOCKS.register("azazel_desertstatue",
            () -> new AzazelDecorateBlock(
                    BlockBehaviour.Properties.copy(Blocks.STONE)
                            .sound(SoundType.NETHER_BRICKS)
                            .strength(2.0F)
                            .noOcclusion()));

    public static final RegistryObject<Block> STORM_TOTEM =
            BLOCKS.register(
                    "storm_totem",

                    () ->
                            new StormTotemBlock(
                                    BlockBehaviour.Properties
                                            .copy(
                                                    Blocks.STONE
                                            )
                                            .sound(
                                                    SoundType.NETHER_BRICKS
                                            )
                                            .strength(
                                                    5.0F
                                            )
                                            .requiresCorrectToolForDrops()
                                            .noOcclusion()
                            )
            );

    public static final RegistryObject<Item> STORM_TOTEM_ITEM = ITEMS.register("storm_totem",
            () -> new BlockItem(STORM_TOTEM.get(), new Item.Properties()));

    public static final RegistryObject<Item> AZAZEL_DESERTSTATUE_ITEM = ITEMS.register("azazel_desertstatue",
            () -> new BlockItem(AZAZEL_DESERTSTATUE.get(), new Item.Properties()));

    public static final RegistryObject<Block> STAT_LANTERN = BLOCKS.register("stat_lantern",
            () -> new GenericDecorateBlock(BlockBehaviour.Properties.copy(Blocks.STONE)
                    .lightLevel(state -> 10)
                    .requiresCorrectToolForDrops()
                    .strength(2.0F)
                    .noOcclusion()));

    public static final RegistryObject<Item> STAT_LANTERN_ITEM = ITEMS.register("stat_lantern",
            () -> new BlockItem(STAT_LANTERN.get(), new Item.Properties()));

    public static final RegistryObject<Block> NEPHRITIS_LAMP =
            BLOCKS.register(
                    "nephritis_lamp",

                    () -> new NephritisLampBlock(
                            BlockBehaviour.Properties
                                    .copy(Blocks.STONE)
                                    .lightLevel(state -> 15)
                                    .requiresCorrectToolForDrops()
                                    .strength(8.0F)
                                    .noOcclusion()
                    )
            );

    public static final RegistryObject<Item> NEPHRITIS_LAMP_ITEM = ITEMS.register("nephritis_lamp",
            () -> new BlockItem(NEPHRITIS_LAMP.get(), new Item.Properties()));

    public static final RegistryObject<Block> STAT = BLOCKS.register("stat",
            () -> new StatBlock(BlockBehaviour.Properties.copy(Blocks.STONE)
                    .strength(2.0F)
                    .requiresCorrectToolForDrops()));

    public static final RegistryObject<Item> STAT_ITEM = ITEMS.register("stat",
            () -> new BlockItem(STAT.get(), new Item.Properties()));

    public static final RegistryObject<Block> SANDED_CHEST = BLOCKS.register("sanded_chest",
            () -> new SandedChestBlock(BlockBehaviour.Properties.copy(Blocks.OAK_WOOD)
                    .strength(2.0F)
                    .requiresCorrectToolForDrops()));

    public static final RegistryObject<Item> SANDED_CHEST_ITEM = ITEMS.register("sanded_chest",
            () -> new GeoBlockItem(
                    SANDED_CHEST.get(),
                    new Item.Properties(),
                    ResourceLocation.fromNamespaceAndPath(MODID, "geo/sanded_chest.geo.json"),
                    ResourceLocation.fromNamespaceAndPath(MODID, "textures/block/sanded_chest.png"),
                    ResourceLocation.fromNamespaceAndPath(MODID, "animations/sanded_chest.animation.json"),
                    ResourceLocation.fromNamespaceAndPath(MODID, "textures/block/empty.png")
            ));

    public static final RegistryObject<Block> DASHER_STATUE = BLOCKS.register("dasher_statue",
            () -> new StatueBlock(DASHER_SHAPE, BlockBehaviour.Properties.copy(Blocks.STONE)
                    .strength(200.0F)
                    .requiresCorrectToolForDrops()
                    .noOcclusion()));

    public static final RegistryObject<Item> DASHER_STATUE_ITEM = ITEMS.register("dasher_statue",
            () -> new GeoBlockItem(
                    DASHER_STATUE.get(),
                    new Item.Properties(),
                    ResourceLocation.fromNamespaceAndPath(MODID, "geo/dasher_statue.geo.json"),
                    ResourceLocation.fromNamespaceAndPath(MODID, "textures/block/dasher_statue.png"),
                    ResourceLocation.fromNamespaceAndPath(MODID, "animations/empty.json"),
                    ResourceLocation.fromNamespaceAndPath(MODID, "textures/block/empty.png")
            ));

    public static final RegistryObject<Block> MONKI_STATUE = BLOCKS.register("monki_statue",
            () -> new StatueBlock(MONKI_SHAPE, BlockBehaviour.Properties.copy(Blocks.STONE)
                    .strength(100.0F)
                    .requiresCorrectToolForDrops()
                    .noOcclusion()));

    public static final RegistryObject<Item> MONKI_STATUE_ITEM = ITEMS.register("monki_statue",
            () -> new GeoBlockItem(
                    MONKI_STATUE.get(),
                    new Item.Properties(),
                    ResourceLocation.fromNamespaceAndPath(MODID, "geo/monki_statue.geo.json"),
                    ResourceLocation.fromNamespaceAndPath(MODID, "textures/block/monki_statue.png"),
                    ResourceLocation.fromNamespaceAndPath(MODID, "animations/empty.animation.json"),
                    ResourceLocation.fromNamespaceAndPath(MODID, "textures/block/empty.png")
            ));

    public static final RegistryObject<Block> SKELET_BELIEVER =
            BLOCKS.register(
                    "skelet_believer",
                    () -> new SkeletBlock(
                            BlockBehaviour.Properties
                                    .copy(Blocks.SOUL_SAND)
                                    .strength(5.0F)
                                    .requiresCorrectToolForDrops()
                                    .noOcclusion()
                    )
            );

    public static final RegistryObject<Item> SKELET_BELIEVER_ITEM = ITEMS.register("skelet_believer",
            () -> new BlockItem(SKELET_BELIEVER.get(), new Item.Properties()));

    public static final RegistryObject<Block> SAND_GOOSE =
            BLOCKS.register(
                    "sand_goose",
                    () -> new AnnoyingGooseBlock(
                            BlockBehaviour.Properties
                                    .copy(Blocks.STONE)
                                    .strength(2.0F)
                                    .requiresCorrectToolForDrops()
                                    .noOcclusion()
                    )
            );

    public static final RegistryObject<Item> SAND_GOOSE_ITEM = ITEMS.register("sand_goose",
            () -> new BlockItem(SAND_GOOSE.get(), new Item.Properties()));

    public static final RegistryObject<Block> CHAOS_ALTAR =
            BLOCKS.register(
                    "chaos_altar",
                    () -> new ChaosAltarBlock(
                            BlockBehaviour.Properties
                                    .copy(Blocks.STONE)
                                    .strength(100.0F)
                                    .requiresCorrectToolForDrops()
                                    .noOcclusion()
                    )
            );

    public static final RegistryObject<Item> CHAOS_ALTAR_ITEM = ITEMS.register("chaos_altar",
            () -> new GeoBlockItem(
                    CHAOS_ALTAR.get(),
                    new Item.Properties(),
                    ResourceLocation.fromNamespaceAndPath(MODID, "geo/chaos_altar.geo.json"),
                    ResourceLocation.fromNamespaceAndPath(MODID, "textures/block/chaos_altar.png"),
                    ResourceLocation.fromNamespaceAndPath(MODID, "animations/chaos_altar.animation.json"),
                    ResourceLocation.fromNamespaceAndPath(MODID, "textures/block/empty.png")
            ));

    public static final RegistryObject<Block> TITANA_STATUE = BLOCKS.register("titana_statue",
            () -> new StatueBlock(TITANA_SHAPE, BlockBehaviour.Properties.copy(Blocks.STONE)
                    .strength(300.0F)
                    .requiresCorrectToolForDrops()
                    .noOcclusion()));

    public static final RegistryObject<Item> TITANA_STATUE_ITEM = ITEMS.register("titana_statue",
            () -> new GeoBlockItem(
                    TITANA_STATUE.get(),
                    new Item.Properties(),
                    ResourceLocation.fromNamespaceAndPath(MODID, "geo/titana_statue.geo.json"),
                    ResourceLocation.fromNamespaceAndPath(MODID, "textures/block/titana_statue.png"),
                    ResourceLocation.fromNamespaceAndPath(MODID, "animations/empty.animation.json"),
                    ResourceLocation.fromNamespaceAndPath(MODID, "textures/block/empty.png")
            ));

    public static final RegistryObject<Block> CACTOS = BLOCKS.register("cactos",
            () -> new CactosBlock(BlockBehaviour.Properties.copy(Blocks.CACTUS)
                    .noOcclusion()
                    .instabreak()));

    public static final RegistryObject<Item> CACTOS_ITEM = ITEMS.register("cactos",
            () -> new BlockItem(CACTOS.get(), new Item.Properties()));
//ITEMS:

    public static final RegistryObject<Item> KARAKOLIT_INGOT = ITEMS.register("karakolit_ingot",
            () -> new Item(new Item.Properties().stacksTo(64)));

    public static final RegistryObject<Item> NEPHRITIS = ITEMS.register("nephritis",
            () -> new Item(new Item.Properties().stacksTo(16)));

    public static final RegistryObject<Item> NEPHRITIS_CORE = ITEMS.register("nephritis_core",
            () -> new Item(new Item.Properties().stacksTo(1)));

    public static final RegistryObject<Item> ORB_CHAOS = ITEMS.register("orb_chaos",
            () -> new Item(new Item.Properties().stacksTo(1)));

    public static final RegistryObject<Item> ORB_DOMINATION = ITEMS.register("orb_domination",
            () -> new Item(new Item.Properties().stacksTo(1)));


    public static final RegistryObject<Item> ENCHANTED_BOOK_HAMMER =
            ITEMS.register(
                    "enchanted_book_hammer",
                    () -> new HammerEnchantmentBookItem(
                            new Item.Properties()
                                    .stacksTo(1)
                                    .rarity(Rarity.UNCOMMON)
                    )
            );

    public static final RegistryObject<Item> BOMBUL_BOTTLE =
            ITEMS.register(
                    "bombul_bottle",
                    () -> new BombulBottleItem(
                            new Item.Properties().stacksTo(1)
                    )
            );

    public static final RegistryObject<Item> BOMBUL_BOTTLE_EMPTY =
            ITEMS.register(
                    "bombul_bottle_empty",
                    () -> new Item(
                            new Item.Properties().stacksTo(16)
                    )
            );

    public static final RegistryObject<Item> TITANA_HAMMER = ITEMS.register("titana_hammer",
            () -> new TitanaHammerItem(new Item.Properties().stacksTo(1)));

//ARMOR:

    public static final RegistryObject<Item> SUPER_GOLD_HELMET = ITEMS.register("super_gold_helmet",
            () -> new SuperGoldArmorItem(ArmorItem.Type.HELMET, new Item.Properties().stacksTo(1)));

    public static final RegistryObject<Item> SUPER_GOLD_CHESTPLATE = ITEMS.register("super_gold_chestplate",
            () -> new SuperGoldArmorItem(ArmorItem.Type.CHESTPLATE, new Item.Properties().stacksTo(1)));

    public static final RegistryObject<Item> SUPER_GOLD_LEGGINGS = ITEMS.register("super_gold_leggings",
            () -> new SuperGoldArmorItem(ArmorItem.Type.LEGGINGS, new Item.Properties().stacksTo(1)));

    public static final RegistryObject<Item> SUPER_GOLD_BOOTS = ITEMS.register("super_gold_boots",
            () -> new SuperGoldArmorItem(ArmorItem.Type.BOOTS, new Item.Properties().stacksTo(1)));


    //BLOCK ENTITIES

    public static final RegistryObject
            <
                    BlockEntityType<NephritisLampBlockEntity>
                    >
            NEPHRITIS_LAMP_BE =

            BLOCK_ENTITIES.register(
                    "nephritis_lamp",

                    () -> BlockEntityType.Builder
                            .of(
                                    NephritisLampBlockEntity::new,
                                    NEPHRITIS_LAMP.get()
                            )
                            .build(
                                    null
                            )
            );

    public static final RegistryObject<BlockEntityType<StormTotemBlockEntity>>
            STORM_TOTEM_BLOCK_ENTITY =
            BLOCK_ENTITIES.register(
                    "storm_totem",

                    () ->
                            BlockEntityType.Builder
                                    .of(
                                            StormTotemBlockEntity::new,
                                            STORM_TOTEM.get()
                                    )
                                    .build(
                                            null
                                    )
            );

    public static final RegistryObject<BlockEntityType<StatBlockEntity>> STAT_BE = BLOCK_ENTITIES.register("stat",
            () -> BlockEntityType.Builder.of(StatBlockEntity::new, STAT.get()).build(null));

    public static final RegistryObject<BlockEntityType<StatueBlockEntity>> STATUE_BE = BLOCK_ENTITIES.register("statue",
            () -> BlockEntityType.Builder.of(StatueBlockEntity::new,
                    MONKI_STATUE.get(), DASHER_STATUE.get(), TITANA_STATUE.get()
            ).build(null));

    public static final RegistryObject<BlockEntityType<com.benji.oasiso.common.block.entity.LieBlockEntity>> LIE_BLOCK_BE = BLOCK_ENTITIES.register("lie_block",
            () -> BlockEntityType.Builder.of(com.benji.oasiso.common.block.entity.LieBlockEntity::new, LIE_BLOCK.get()).build(null));

    public static final RegistryObject<BlockEntityType<ChaosAltarBlockEntity>> CHAOS_ALTAR_BE = BLOCK_ENTITIES.register("chaos_altar",
            () -> BlockEntityType.Builder.of(ChaosAltarBlockEntity::new, CHAOS_ALTAR.get()).build(null));

    public static final RegistryObject<BlockEntityType<SandedChestBlockEntity>> SANDED_CHEST_BE = BLOCK_ENTITIES.register("sanded_chest",
            () -> BlockEntityType.Builder.of(SandedChestBlockEntity::new, SANDED_CHEST.get()).build(null));
    //============================
    // ENTITIES
    public static final RegistryObject<EntityType<KrombulEntity>> KROMBUL =
            ENTITIES.register(
                    "krombul",
                    () -> EntityType.Builder.of(
                                    KrombulEntity::new,
                                    MobCategory.CREATURE
                            )
                            .sized(0.75F, 1.25F)
                            .clientTrackingRange(8)
                            .build(
                                    ResourceLocation.fromNamespaceAndPath(
                                            MODID,
                                            "krombul"
                                    ).toString()
                            )
            );
    //BOSS:


    public static final RegistryObject<EntityType<AzumaalEntity>> AZUMAAL = ENTITIES.register("azumaal",
            () -> EntityType.Builder.of(AzumaalEntity::new, MobCategory.MONSTER)
                    .sized(1.375F, 6.25F)
                    .build(ResourceLocation.fromNamespaceAndPath(MODID, "azumaal").toString()));

    public static final RegistryObject<EntityType<ChaosBombEntity>> CHAOS_BOMB = ENTITIES.register("chaos_bomb",
            () -> EntityType.Builder.of(ChaosBombEntity::new, MobCategory.MONSTER)
                    .sized(0.75F, 0.875F)
                    .build(ResourceLocation.fromNamespaceAndPath(MODID, "chaos_bomb").toString()));

    public static final RegistryObject<EntityType<EyelidEntity>>
            EYELID =
            ENTITIES.register(
                    "eyelid",

                    () -> EntityType.Builder
                            .<EyelidEntity>of(
                                    EyelidEntity::new,
                                    MobCategory.MISC
                            )

                            .sized(
                                    1.2F,
                                    1.2F
                            )

                            .clientTrackingRange(
                                    12
                            )

                            .updateInterval(
                                    1
                            )

                            .build(
                                    ResourceLocation
                                            .fromNamespaceAndPath(
                                                    MODID,
                                                    "eyelid"
                                            )
                                            .toString()
                            )
            );

    public static final RegistryObject<EntityType<CircleHintEntity>> CIRCLE_HINT = ENTITIES.register("circle_hint",
            () -> EntityType.Builder.of(CircleHintEntity::new, MobCategory.MONSTER)
                    .sized(2.5F, 0.125F)
                    .build(ResourceLocation.fromNamespaceAndPath(MODID, "circle_hint").toString()));

    public static final RegistryObject<EntityType<BattleHintArrowEntity>> BATTLE_HINT_ARROW = ENTITIES.register("battle_hint_arrow",
            () -> EntityType.Builder.of(BattleHintArrowEntity::new, MobCategory.MONSTER)
                    .sized(4.25F, 0.125F)
                    .build(ResourceLocation.fromNamespaceAndPath(MODID, "battle_hint_arrow").toString()));

    public static final RegistryObject<EntityType<BossPortalEntity>> BOSS_PORTAL = ENTITIES.register("boss_portal",
            () -> EntityType.Builder.of(BossPortalEntity::new, MobCategory.MONSTER)
                    .sized(3.375F, 0.1875F)
                    .build(ResourceLocation.fromNamespaceAndPath(MODID, "boss_portal").toString()));

    public static final RegistryObject<EntityType<DamageNumberEntity>>
            DAMAGE_NUMBER =
            ENTITIES.register(
                    "damage_number",

                    () -> EntityType.Builder
                            .<DamageNumberEntity>of(
                                    DamageNumberEntity::new,
                                    MobCategory.MISC
                            )
                            .sized(
                                    0.01F,
                                    0.01F
                            )
                            .clientTrackingRange(
                                    8
                            )
                            .updateInterval(
                                    1
                            )
                            .build(
                                    ResourceLocation
                                            .fromNamespaceAndPath(
                                                    MODID,
                                                    "damage_number"
                                            )
                                            .toString()
                            )
            );

    //=======

    public static final RegistryObject<EntityType<WizardPillarEntity>> WIZARD_PILLAR_ENTITY =
            ENTITIES.register(
                    "wizard_pillar_entity",
                    () -> EntityType.Builder.<WizardPillarEntity>of(
                                    WizardPillarEntity::new,
                                    MobCategory.MISC
                            )
                            .sized(0.1F, 0.1F)
                            .clientTrackingRange(64)
                            .updateInterval(1)
                            .build("wizard_pillar_entity")
            );

    public static final RegistryObject<EntityType<MonkiEntity>> MONKI = ENTITIES.register("monki",
            () -> EntityType.Builder.of(MonkiEntity::new, MobCategory.MONSTER)
                    .sized(0.625F, 1.25F)
                    .build(ResourceLocation.fromNamespaceAndPath(MODID, "monki").toString()));

    public static final RegistryObject<EntityType<CrusaderTankEntity>> CRUSADER_TANK = ENTITIES.register("crusader_tank",
            () -> EntityType.Builder.of(CrusaderTankEntity::new, MobCategory.MONSTER)
                    .sized(1.50F, 3.00F)
                    .build(ResourceLocation.fromNamespaceAndPath(MODID, "crusader_tank").toString()));

    public static final RegistryObject<EntityType<CrusaderWarriorEntity>> CRUSADER_WARRIOR = ENTITIES.register("crusader_warrior",
            () -> EntityType.Builder.of(CrusaderWarriorEntity::new, MobCategory.MONSTER)
                    .sized(0.625F, 2.25F)
                    .build(ResourceLocation.fromNamespaceAndPath(MODID, "crusader_warrior").toString()));

    public static final RegistryObject<EntityType<PaladinEntity>> PALADIN = ENTITIES.register("paladin",
            () -> EntityType.Builder.of(PaladinEntity::new, MobCategory.MONSTER)
                    .sized(0.625F, 2.25F)
                    .build(ResourceLocation.fromNamespaceAndPath(MODID, "paladin").toString()));

    public static final RegistryObject<EntityType<SwordHeartEntity>> SWORD_HEART = ENTITIES.register("sword_heart",
            () -> EntityType.Builder.of(SwordHeartEntity::new, MobCategory.MONSTER)
                    .sized(0.25F, 0.25F)
                    .build(ResourceLocation.fromNamespaceAndPath(MODID, "sword_heart").toString()));


    public static final RegistryObject<EntityType<CrusaderAssasinEntity>> CRUSADER_ASSASIN = ENTITIES.register("crusader_assasin",
            () -> EntityType.Builder.of(CrusaderAssasinEntity::new, MobCategory.MONSTER)
                    .sized(0.625F, 2.25F)
                    .build(ResourceLocation.fromNamespaceAndPath(MODID, "crusader_assasin").toString()));

    public static final RegistryObject<EntityType<CrusaderWizardEntity>> CRUSADER_WIZARD = ENTITIES.register("crusader_wizard",
            () -> EntityType.Builder.of(CrusaderWizardEntity::new, MobCategory.MONSTER)
                    .sized(0.625F, 2.25F)
                    .build(ResourceLocation.fromNamespaceAndPath(MODID, "crusader_wizard").toString()));

    public static final RegistryObject<EntityType<GasterEntity>> GASTER = ENTITIES.register("gaster",
            () -> EntityType.Builder.of(GasterEntity::new, MobCategory.MONSTER)
                    .sized(1.0F, 2.5625F)
                    .build(ResourceLocation.fromNamespaceAndPath(MODID, "gaster").toString()));

    public static final RegistryObject<EntityType<DesertBallEntity>> DESERT_BALL = ENTITIES.register("desertball",
            () -> EntityType.Builder.<DesertBallEntity>of(DesertBallEntity::new, MobCategory.MISC)
                    .sized(0.2F, 0.2F)
                    .clientTrackingRange(4)
                    .updateInterval(10)
                    .build(ResourceLocation.fromNamespaceAndPath(MODID, "desertball").toString()));

    public static final RegistryObject<EntityType<CactoProjEntity>> CACTO_PROJ = ENTITIES.register("cacto_proj",
            () -> EntityType.Builder.<CactoProjEntity>of(CactoProjEntity::new, MobCategory.MISC)
                    .sized(0.125F, 0.125F)
                    .clientTrackingRange(4)
                    .updateInterval(20)
                    .build(ResourceLocation.fromNamespaceAndPath(MODID, "cacto_proj").toString()));

    public static final RegistryObject<EntityType<MonkiBigEntity>> MONKI_BIG = ENTITIES.register("monki_big",
            () -> EntityType.Builder.of(MonkiBigEntity::new, MobCategory.MONSTER)
                    .sized(1.75F, 3.75F)
                    .build(ResourceLocation.fromNamespaceAndPath(MODID, "monki_big").toString()));

    public static final RegistryObject<EntityType<TitanaEntity>> TITANA = ENTITIES.register("titana",
            () -> EntityType.Builder.of(TitanaEntity::new, MobCategory.MONSTER)
                    .sized(2.5F, 3.75F)
                    .build(ResourceLocation.fromNamespaceAndPath(MODID, "titana").toString()));

    public static final RegistryObject<EntityType<SandGolemEntity>> SAND_GOLEM = ENTITIES.register("sand_golem",
            () -> EntityType.Builder.of(SandGolemEntity::new, MobCategory.MONSTER)
                    .sized(2.5F, 3.75F)
                    .build(ResourceLocation.fromNamespaceAndPath(MODID, "sand_golem").toString()));

    public static final RegistryObject<EntityType<CaserEntity>> CASER = ENTITIES.register("caser",
            () -> EntityType.Builder.of(CaserEntity::new, MobCategory.CREATURE)
                    .sized(2.5F, 3.75F)
                    .build(ResourceLocation.fromNamespaceAndPath(MODID, "caser").toString()));

    public static final RegistryObject<EntityType<SandHandEntity>> SAND_HAND = ENTITIES.register("sand_hand",
            () -> EntityType.Builder.<SandHandEntity>of(SandHandEntity::new, MobCategory.MISC)
                    .sized(1.5F, 2.0F)
                    .build(ResourceLocation.fromNamespaceAndPath(MODID, "sand_hand").toString()));

    public static final RegistryObject<EntityType<DasherEntity>> DASHER = ENTITIES.register("dasher",
            () -> EntityType.Builder.of(DasherEntity::new, MobCategory.MONSTER)
                    .sized(1.25F, 5.0F)
                    .build(ResourceLocation.fromNamespaceAndPath(MODID, "dasher").toString()));

    public static final RegistryObject<EntityType<BombulEntity>> BOMBUL = ENTITIES.register("bombul",
            () -> EntityType.Builder.of(BombulEntity::new, MobCategory.MONSTER)
                    .sized(1.25F, 3.0F)
                    .build(ResourceLocation.fromNamespaceAndPath(MODID, "bombul").toString()));

    public static final RegistryObject<EntityType<CactoEntity>> CACTO = ENTITIES.register("cacto",
            () -> EntityType.Builder.of(CactoEntity::new, MobCategory.MONSTER)
                    .sized(0.625F, 1.5F)
                    .build(ResourceLocation.fromNamespaceAndPath(MODID, "cacto").toString()));

    //=====================
    //SPAWN EGGS:
    public static final RegistryObject<Item> CASSASIN_SPAWN_EGG = ITEMS.register("cassasin_spawn_egg",
            () -> new net.minecraftforge.common.ForgeSpawnEggItem(
                    CRUSADER_ASSASIN,
                    0xFFFFFF,
                    0xFFFFFF,
                    new Item.Properties()
            ));

    public static final RegistryObject<Item> CTANK_SPAWN_EGG = ITEMS.register("ctank_spawn_egg",
            () -> new net.minecraftforge.common.ForgeSpawnEggItem(
                    CRUSADER_TANK,
                    0xFFFFFF,
                    0xFFFFFF,
                    new Item.Properties()
            ));

    public static final RegistryObject<Item> CWARRIOR_SPAWN_EGG = ITEMS.register("cwarrior_spawn_egg",
            () -> new net.minecraftforge.common.ForgeSpawnEggItem(
                    CRUSADER_WARRIOR,
                    0xFFFFFF,
                    0xFFFFFF,
                    new Item.Properties()
            ));

    public static final RegistryObject<Item> CWIZARD_SPAWN_EGG = ITEMS.register("cwizard_spawn_egg",
            () -> new net.minecraftforge.common.ForgeSpawnEggItem(
                    CRUSADER_WIZARD,
                    0xFFFFFF,
                    0xFFFFFF,
                    new Item.Properties()
            ));

    public static final RegistryObject<Item> CPALADIN_SPAWN_EGG = ITEMS.register("cpaladin_spawn_egg",
            () -> new net.minecraftforge.common.ForgeSpawnEggItem(
                    PALADIN,
                    0xFFFFFF,
                    0xFFFFFF,
                    new Item.Properties()
            ));

    public static final RegistryObject<Item> MONKI_SPAWN_EGG = ITEMS.register("monki_spawn_egg",
            () -> new net.minecraftforge.common.ForgeSpawnEggItem(
                    MONKI,
                    0xFFFFFF,
                    0xFFFFFF,
                    new Item.Properties()
            ));

    public static final RegistryObject<Item> KROMBUL_SPAWN_EGG = ITEMS.register("krombul_spawn_egg",
            () -> new net.minecraftforge.common.ForgeSpawnEggItem(
                    KROMBUL,
                    0xFFFFFF,
                    0xFFFFFF,
                    new Item.Properties()
            ));

    public static final RegistryObject<Item> BOMBUL_SPAWN_EGG = ITEMS.register("bombul_spawn_egg",
            () -> new net.minecraftforge.common.ForgeSpawnEggItem(
                    BOMBUL,
                    0xFFFFFF,
                    0xFFFFFF,
                    new Item.Properties()
            ));

    public static final RegistryObject<Item> TITANA_SPAWN_EGG = ITEMS.register("titana_spawn_egg",
            () -> new net.minecraftforge.common.ForgeSpawnEggItem(
                    TITANA,
                    0xFFFFFF,
                    0xFFFFFF,
                    new Item.Properties()
            ));

    public static final RegistryObject<Item> CACTO_SPAWN_EGG = ITEMS.register("cacto_spawn_egg",
            () -> new net.minecraftforge.common.ForgeSpawnEggItem(
                    CACTO,
                    0xFFFFFF,
                    0xFFFFFF,
                    new Item.Properties()
            ));

    public static final RegistryObject<Item> DASHER_SPAWN_EGG = ITEMS.register("dasher_spawn_egg",
            () -> new net.minecraftforge.common.ForgeSpawnEggItem(
                    DASHER,
                    0xFFFFFF,
                    0xFFFFFF,
                    new Item.Properties()
            ));

    public static final RegistryObject<Item> CASER_SPAWN_EGG = ITEMS.register("caser_spawn_egg",
            () -> new net.minecraftforge.common.ForgeSpawnEggItem(
                    CASER,
                    0xFFFFFF,
                    0xFFFFFF,
                    new Item.Properties()
            ));

    public static final RegistryObject<Item> SAND_GOLEM_SPAWN_EGG = ITEMS.register("sand_golem_spawn_egg",
            () -> new net.minecraftforge.common.ForgeSpawnEggItem(
                    SAND_GOLEM,
                    0xFFFFFF,
                    0xFFFFFF,
                    new Item.Properties()
            ));


    // NEW WOOD TYPE:

    // BLOCKS DOUM PALM
    public static final RegistryObject<Block> DOUM_PALM_LOG = BLOCKS.register("doum_palm_log",
            () -> new RotatedPillarBlock(BlockBehaviour.Properties.copy(Blocks.JUNGLE_LOG)));

    public static final RegistryObject<Block> DOUM_PALM_WOOD = BLOCKS.register("doum_palm_wood",
            () -> new RotatedPillarBlock(BlockBehaviour.Properties.copy(Blocks.JUNGLE_WOOD)));

    public static final RegistryObject<Block> STRIPPED_DOUM_PALM_LOG = BLOCKS.register("stripped_doum_palm_log",
            () -> new RotatedPillarBlock(BlockBehaviour.Properties.copy(Blocks.STRIPPED_JUNGLE_LOG)));

    public static final RegistryObject<Block> STRIPPED_DOUM_PALM_WOOD = BLOCKS.register("stripped_doum_palm_wood",
            () -> new RotatedPillarBlock(BlockBehaviour.Properties.copy(Blocks.STRIPPED_JUNGLE_WOOD)));

    public static final RegistryObject<Block> DOUM_PALM_PLANKS = BLOCKS.register("doum_palm_planks",
            () -> new Block(BlockBehaviour.Properties.copy(Blocks.JUNGLE_PLANKS)));

    public static final RegistryObject<Block> DOUM_PALM_SLAB = BLOCKS.register("doum_palm_slab",
            () -> new SlabBlock(BlockBehaviour.Properties.copy(Blocks.JUNGLE_SLAB)));

    public static final RegistryObject<Block> DOUM_PALM_STAIRS = BLOCKS.register("doum_palm_stairs",
            () -> new StairBlock(
                    () -> DOUM_PALM_PLANKS.get().defaultBlockState(),
                    BlockBehaviour.Properties.copy(Blocks.JUNGLE_STAIRS)
            ));

    public static final RegistryObject<Block> DOUM_PALM_LEAVES = BLOCKS.register("doum_palm_leaves",
            () -> new LeavesBlock(BlockBehaviour.Properties.copy(Blocks.JUNGLE_LEAVES).noOcclusion()));

    public static final RegistryObject<Block> DOUM_PALM_SAPLING = BLOCKS.register("doum_palm_sapling",
            () -> new com.benji.oasiso.common.block.DoumPalmSaplingBlock(
                    new net.minecraft.world.level.block.grower.JungleTreeGrower(),
                    BlockBehaviour.Properties.copy(Blocks.JUNGLE_SAPLING)
            ));

    public static final RegistryObject<Block> DOUM_PALM_DOOR = BLOCKS.register("doum_palm_door",
            () -> new DoorBlock(BlockBehaviour.Properties.copy(Blocks.JUNGLE_DOOR), net.minecraft.world.level.block.state.properties.BlockSetType.JUNGLE));

    public static final RegistryObject<Block> DOUM_PALM_TRAPDOOR = BLOCKS.register("doum_palm_trapdoor",
            () -> new TrapDoorBlock(BlockBehaviour.Properties.copy(Blocks.JUNGLE_TRAPDOOR), net.minecraft.world.level.block.state.properties.BlockSetType.JUNGLE));

    public static final RegistryObject<Block> DOUM_PALM_FENCE = BLOCKS.register("doum_palm_fence",
            () -> new FenceBlock(BlockBehaviour.Properties.copy(Blocks.JUNGLE_FENCE)));

    public static final RegistryObject<Block> DOUM_PALM_FENCE_GATE = BLOCKS.register("doum_palm_fence_gate",
            () -> new FenceGateBlock(BlockBehaviour.Properties.copy(Blocks.JUNGLE_FENCE_GATE), Oasiso.DOUM_PALM_WOOD_TYPE));

    public static final RegistryObject<Block> DOUM_PALM_BUTTON = BLOCKS.register("doum_palm_button",
            () -> new ButtonBlock(BlockBehaviour.Properties.copy(Blocks.JUNGLE_BUTTON),
                    net.minecraft.world.level.block.state.properties.BlockSetType.JUNGLE, 30, true));

    public static final RegistryObject<Block> DOUM_PALM_PRESSURE_PLATE = BLOCKS.register("doum_palm_pressure_plate",
            () -> new PressurePlateBlock(PressurePlateBlock.Sensitivity.EVERYTHING,
                    BlockBehaviour.Properties.copy(Blocks.JUNGLE_PRESSURE_PLATE),
                    net.minecraft.world.level.block.state.properties.BlockSetType.JUNGLE));

    public static final RegistryObject<Block> DOUM_PALM_SIGN = BLOCKS.register("doum_palm_sign",
            () -> new StandingSignBlock(BlockBehaviour.Properties.copy(Blocks.JUNGLE_SIGN), Oasiso.DOUM_PALM_WOOD_TYPE) {
                @Override
                public net.minecraft.world.level.block.entity.BlockEntity newBlockEntity(net.minecraft.core.BlockPos pos, net.minecraft.world.level.block.state.BlockState state) {
                    return new com.benji.oasiso.common.block.entity.DoumPalmSignBlockEntity(pos, state);
                }
            });

    public static final RegistryObject<Block> DOUM_PALM_WALL_SIGN = BLOCKS.register("doum_palm_wall_sign",
            () -> new WallSignBlock(BlockBehaviour.Properties.copy(Blocks.JUNGLE_WALL_SIGN), Oasiso.DOUM_PALM_WOOD_TYPE) {
                @Override
                public net.minecraft.world.level.block.entity.BlockEntity newBlockEntity(net.minecraft.core.BlockPos pos, net.minecraft.world.level.block.state.BlockState state) {
                    return new com.benji.oasiso.common.block.entity.DoumPalmSignBlockEntity(pos, state);
                }
            });

    public static final RegistryObject<Block> DOUM_PALM_HANGING_SIGN = BLOCKS.register("doum_palm_hanging_sign",
            () -> new CeilingHangingSignBlock(BlockBehaviour.Properties.copy(Blocks.JUNGLE_HANGING_SIGN), Oasiso.DOUM_PALM_WOOD_TYPE) {
                @Override
                public net.minecraft.world.level.block.entity.BlockEntity newBlockEntity(net.minecraft.core.BlockPos pos, net.minecraft.world.level.block.state.BlockState state) {
                    return new com.benji.oasiso.common.block.entity.DoumPalmHangingSignBlockEntity(pos, state);
                }
            });

    public static final RegistryObject<Block> DOUM_PALM_WALL_HANGING_SIGN = BLOCKS.register("doum_palm_wall_hanging_sign",
            () -> new WallHangingSignBlock(BlockBehaviour.Properties.copy(Blocks.JUNGLE_WALL_HANGING_SIGN), Oasiso.DOUM_PALM_WOOD_TYPE) {
                @Override
                public net.minecraft.world.level.block.entity.BlockEntity newBlockEntity(net.minecraft.core.BlockPos pos, net.minecraft.world.level.block.state.BlockState state) {
                    return new com.benji.oasiso.common.block.entity.DoumPalmHangingSignBlockEntity(pos, state);
                }
            });


    // ITEMS DOUM PALM
    public static final RegistryObject<Item> DOUM_PALM_SIGN_ITEM = ITEMS.register("doum_palm_sign",
            () -> new SignItem(new Item.Properties().stacksTo(16), DOUM_PALM_SIGN.get(), DOUM_PALM_WALL_SIGN.get()));

    public static final RegistryObject<Item> DOUM_PALM_HANGING_SIGN_ITEM = ITEMS.register("doum_palm_hanging_sign",
            () -> new HangingSignItem(DOUM_PALM_HANGING_SIGN.get(), DOUM_PALM_WALL_HANGING_SIGN.get(), new Item.Properties().stacksTo(16)));

    public static final RegistryObject<BlockEntityType<com.benji.oasiso.common.block.entity.DoumPalmSignBlockEntity>> DOUM_PALM_SIGN_BE = BLOCK_ENTITIES.register("doum_palm_sign",
            () -> BlockEntityType.Builder.of(com.benji.oasiso.common.block.entity.DoumPalmSignBlockEntity::new, DOUM_PALM_SIGN.get(), DOUM_PALM_WALL_SIGN.get()).build(null));

    public static final RegistryObject<BlockEntityType<com.benji.oasiso.common.block.entity.DoumPalmHangingSignBlockEntity>> DOUM_PALM_HANGING_SIGN_BE = BLOCK_ENTITIES.register("doum_palm_hanging_sign",
            () -> BlockEntityType.Builder.of(com.benji.oasiso.common.block.entity.DoumPalmHangingSignBlockEntity::new, DOUM_PALM_HANGING_SIGN.get(), DOUM_PALM_WALL_HANGING_SIGN.get()).build(null));

    public static final RegistryObject<Item> DOUM_PALM_BUTTON_ITEM = ITEMS.register("doum_palm_button",
            () -> new BlockItem(DOUM_PALM_BUTTON.get(), new Item.Properties()));

    public static final RegistryObject<Item> DOUM_PALM_PRESSURE_PLATE_ITEM = ITEMS.register("doum_palm_pressure_plate",
            () -> new BlockItem(DOUM_PALM_PRESSURE_PLATE.get(), new Item.Properties()));

    public static final RegistryObject<Item> DOUM_PALM_SLAB_ITEM = ITEMS.register("doum_palm_slab", () -> new BlockItem(DOUM_PALM_SLAB.get(), new Item.Properties()));
    public static final RegistryObject<Item> DOUM_PALM_STAIRS_ITEM = ITEMS.register("doum_palm_stairs", () -> new BlockItem(DOUM_PALM_STAIRS.get(), new Item.Properties()));

    public static final RegistryObject<Item> DOUM_PALM_WOOD_ITEM = ITEMS.register("doum_palm_wood", () -> new BlockItem(DOUM_PALM_WOOD.get(), new Item.Properties()));
    public static final RegistryObject<Item> STRIPPED_DOUM_PALM_WOOD_ITEM = ITEMS.register("stripped_doum_palm_wood", () -> new BlockItem(STRIPPED_DOUM_PALM_WOOD.get(), new Item.Properties()));
    public static final RegistryObject<Item> DOUM_PALM_LOG_ITEM = ITEMS.register("doum_palm_log", () -> new BlockItem(DOUM_PALM_LOG.get(), new Item.Properties()));
    public static final RegistryObject<Item> STRIPPED_DOUM_PALM_LOG_ITEM = ITEMS.register("stripped_doum_palm_log", () -> new BlockItem(STRIPPED_DOUM_PALM_LOG.get(), new Item.Properties()));
    public static final RegistryObject<Item> DOUM_PALM_PLANKS_ITEM = ITEMS.register("doum_palm_planks", () -> new BlockItem(DOUM_PALM_PLANKS.get(), new Item.Properties()));
    public static final RegistryObject<Item> DOUM_PALM_LEAVES_ITEM = ITEMS.register("doum_palm_leaves", () -> new BlockItem(DOUM_PALM_LEAVES.get(), new Item.Properties()));
    public static final RegistryObject<Item> DOUM_PALM_SAPLING_ITEM = ITEMS.register("doum_palm_sapling", () -> new BlockItem(DOUM_PALM_SAPLING.get(), new Item.Properties()));
    public static final RegistryObject<Item> DOUM_PALM_DOOR_ITEM = ITEMS.register("doum_palm_door", () -> new BlockItem(DOUM_PALM_DOOR.get(), new Item.Properties()));
    public static final RegistryObject<Item> DOUM_PALM_TRAPDOOR_ITEM = ITEMS.register("doum_palm_trapdoor", () -> new BlockItem(DOUM_PALM_TRAPDOOR.get(), new Item.Properties()));
    public static final RegistryObject<Item> DOUM_PALM_FENCE_ITEM = ITEMS.register("doum_palm_fence", () -> new BlockItem(DOUM_PALM_FENCE.get(), new Item.Properties()));
    public static final RegistryObject<Item> DOUM_PALM_FENCE_GATE_ITEM = ITEMS.register("doum_palm_fence_gate", () -> new BlockItem(DOUM_PALM_FENCE_GATE.get(), new Item.Properties()));



    //BOAT

    public static final RegistryObject<EntityType<DoumPalmBoatEntity>> DOUM_PALM_BOAT = ENTITIES.register("doum_palm_boat",
            () -> EntityType.Builder.<DoumPalmBoatEntity>of(DoumPalmBoatEntity::new, MobCategory.MISC)
                    .sized(1.375F, 0.5625F)
                    .clientTrackingRange(10)
                    .build(ResourceLocation.fromNamespaceAndPath(MODID, "doum_palm_boat").toString()));

    public static final RegistryObject<EntityType<DoumPalmChestBoatEntity>> DOUM_PALM_CHEST_BOAT = ENTITIES.register("doum_palm_chest_boat",
            () -> EntityType.Builder.<DoumPalmChestBoatEntity>of(DoumPalmChestBoatEntity::new, MobCategory.MISC)
                    .sized(1.375F, 0.5625F)
                    .clientTrackingRange(10)
                    .build(ResourceLocation.fromNamespaceAndPath(MODID, "doum_palm_chest_boat").toString()));

    public static final RegistryObject<Item> DOUM_PALM_BOAT_ITEM = ITEMS.register("doum_palm_boat",
            () -> new DoumPalmBoatItem(false, new Item.Properties().stacksTo(1)));

    public static final RegistryObject<Item> DOUM_PALM_CHEST_BOAT_ITEM = ITEMS.register("doum_palm_chest_boat",
            () -> new DoumPalmBoatItem(true, new Item.Properties().stacksTo(1)));
//============================================

    public static final RegistryObject<CreativeModeTab> OASISO_TAB = CREATIVE_MODE_TABS.register("oasiso_tab",
            () -> CreativeModeTab.builder()
                    .icon(() -> new ItemStack(NEPHRITIS_CORE.get()))
                    .title(Component.translatable("creativetab.oasiso_tab"))
                    .displayItems((parameters, output) -> {
                        for (RegistryObject<Item> item : ITEMS.getEntries()) {
                            output.accept(item.get());
                        }
                    })
                    .build()
    );

    public Oasiso(FMLJavaModLoadingContext context) {
        IEventBus modEventBus = context.getModEventBus();

        BLOCKS.register(modEventBus);
        ITEMS.register(modEventBus);
        BLOCK_ENTITIES.register(modEventBus);
        ENTITIES.register(modEventBus);
        ModSounds.SOUNDS.register(modEventBus);
        CREATIVE_MODE_TABS.register(modEventBus);
        STRUCTURE_TYPES.register(modEventBus);
        ENCHANTMENTS.register(modEventBus);
        MOB_EFFECTS.register(modEventBus);
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
            ((FlowerPotBlock) Blocks.FLOWER_POT).addPlant(FLOWERY.getId(), POTTED_FLOWERY);
            ((FlowerPotBlock) Blocks.FLOWER_POT).addPlant(CACTULO.getId(), POTTED_CACTULO);
        });
    }

    private void addCreative(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.NATURAL_BLOCKS || event.getTabKey() == CreativeModeTabs.BUILDING_BLOCKS) {
            event.accept(SANDSTONE_BRICKED_ITEM);
            event.accept(SANDSTONE_LINE_ITEM);
            event.accept(SANDSTONE_CORNER_ITEM);
            event.accept(SANDSTONE_AZAZEL_ITEM);
            event.accept(SANDSTONE_ROOF_ITEM);
            event.accept(SANDSTONE_FLOORB_ITEM);
            event.accept(SANDSTONE_TILES_ITEM);
            event.accept(SANDSTONE_STRIPE_ITEM);
            event.accept(SANDSTONE_COLORED_ITEM);
            event.accept(SANDSTONE_COLUMN_ITEM);
            event.accept(FLOWERY_ITEM);
            event.accept(CACTULO_ITEM);
            event.accept(CACTOS_ITEM);
            event.accept(NEPHRITIS_BLOCK_ITEM);
            event.accept(KARAKOLIT_BLOCK_ITEM);

        }
        if (event.getTabKey() == CreativeModeTabs.FUNCTIONAL_BLOCKS) {
            event.accept(GEN_VASE_ITEM);
            event.accept(STAT_LANTERN_ITEM);
            event.accept(STAT);
            event.accept(SANDED_CHEST_ITEM);
            event.accept(DASHER_STATUE_ITEM);
            event.accept(TITANA_STATUE_ITEM);
            event.accept(AZAZEL_DESERTSTATUE_ITEM);
            event.accept(MONKI_STATUE_ITEM);
        }
        if (event.getTabKey() == CreativeModeTabs.SPAWN_EGGS) {
            event.accept(CACTO_SPAWN_EGG);
            event.accept(MONKI_SPAWN_EGG);
            event.accept(TITANA_SPAWN_EGG);
            event.accept(DASHER_SPAWN_EGG);
            event.accept(SAND_GOLEM_SPAWN_EGG);
            event.accept(CASER_SPAWN_EGG);

        }
        if (event.getTabKey() == CreativeModeTabs.COMBAT) {
            event.accept(SUPER_GOLD_BOOTS);
            event.accept(SUPER_GOLD_HELMET);
            event.accept(SUPER_GOLD_CHESTPLATE);
            event.accept(SUPER_GOLD_LEGGINGS);
            event.accept(TITANA_HAMMER);
        }
        if (event.getTabKey() == CreativeModeTabs.INGREDIENTS) {
            event.accept(ENCHANTED_BOOK_HAMMER);
            event.accept(NEPHRITIS);
            event.accept(NEPHRITIS_CORE);
            event.accept(KARAKOLIT_INGOT);
        }
    }

    @Mod.EventBusSubscriber(modid = MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static class ModEvents {
        @SubscribeEvent
        public static void registerAttributes(EntityAttributeCreationEvent event) {
            event.put(MONKI.get(), MonkiEntity.createAttributes().build());
            event.put(GASTER.get(), GasterEntity.createAttributes().build());
            event.put(BOSS_PORTAL.get(), BossPortalEntity.createAttributes().build());
            event.put(AZUMAAL.get(), AzumaalEntity.createAttributes().build());
            event.put(CHAOS_BOMB.get(), ChaosBombEntity.createAttributes().build());
            event.put(SAND_GOLEM.get(), SandGolemEntity.createAttributes().build());
            event.put(CASER.get(), CaserEntity.createAttributes().build());
            event.put(CRUSADER_TANK.get(), CrusaderTankEntity.createAttributes().build());
            event.put(CRUSADER_WARRIOR.get(), CrusaderWarriorEntity.createAttributes().build());
            event.put(PALADIN.get(), PaladinEntity.createAttributes().build());
            event.put(SWORD_HEART.get(), SwordHeartEntity.createAttributes().build());
            event.put(CRUSADER_WIZARD.get(), CrusaderWizardEntity.createAttributes().build());
            event.put(CRUSADER_ASSASIN.get(), CrusaderAssasinEntity.createAttributes().build());
            event.put(MONKI_BIG.get(), MonkiBigEntity.createAttributes().build());
            event.put(BATTLE_HINT_ARROW.get(), BattleHintArrowEntity.createAttributes().build());
            event.put(CIRCLE_HINT.get(), CircleHintEntity.createAttributes().build());
            event.put(TITANA.get(), TitanaEntity.createAttributes().build());
            event.put(SAND_HAND.get(), SandHandEntity.createAttributes().build());
            event.put(KROMBUL.get(), KrombulEntity.createAttributes().build());
            event.put(BOMBUL.get(), BombulEntity.createAttributes().build());
            event.put(DASHER.get(), DasherEntity.createAttributes().build());
            event.put(CACTO.get(), CactoEntity.createAttributes().build());
        }
    }

    @Mod.EventBusSubscriber(modid = MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
    public static class ForgeEvents {
        @SubscribeEvent
        public static void onToolModification(net.minecraftforge.event.level.BlockEvent.BlockToolModificationEvent event) {
            if (event.getToolAction() == net.minecraftforge.common.ToolActions.AXE_STRIP) {
                if (event.getState().is(DOUM_PALM_LOG.get())) {
                    event.setFinalState(STRIPPED_DOUM_PALM_LOG.get().defaultBlockState()
                            .setValue(RotatedPillarBlock.AXIS, event.getState().getValue(RotatedPillarBlock.AXIS)));
                }
                else if (event.getState().is(DOUM_PALM_WOOD.get())) {
                    event.setFinalState(STRIPPED_DOUM_PALM_WOOD.get().defaultBlockState()
                            .setValue(RotatedPillarBlock.AXIS, event.getState().getValue(RotatedPillarBlock.AXIS)));
                }
            }
        }
    }
}