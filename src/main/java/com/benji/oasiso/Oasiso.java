package com.benji.oasiso;

import com.benji.oasiso.client.renderer.CactoRenderer;
import com.benji.oasiso.client.renderer.MonkiRenderer;
import com.benji.oasiso.common.block.*;
import com.benji.oasiso.common.block.entity.SandedChestBlockEntity;
import com.benji.oasiso.common.block.entity.StatBlockEntity;
import com.benji.oasiso.common.block.entity.StatueBlockEntity;
import com.benji.oasiso.common.entity.*;
import com.benji.oasiso.common.entity.projectile.CactoProjEntity;
import com.benji.oasiso.common.entity.projectile.DesertBallEntity;
import com.benji.oasiso.common.item.GeoBlockItem;
import com.benji.oasiso.common.item.SuperGoldArmorItem;
import com.benji.oasiso.common.item.TitanaHammerItem;
import com.benji.oasiso.network.ModMessages;
import com.mojang.logging.LogUtils;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SoundType;
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

    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MODID);

    //HITBOXES
    private static final net.minecraft.world.phys.shapes.VoxelShape MONKI_SHAPE = Block.box(3.0D, 0.0D, 3.0D, 13.0D, 20.0D, 13.0D);
    private static final net.minecraft.world.phys.shapes.VoxelShape DASHER_SHAPE = Block.box(0.0D, 0.0D, 0.0D, 16.0D, 50.0D, 16.0D);
    private static final net.minecraft.world.phys.shapes.VoxelShape TITANA_SHAPE = Block.box(-6.0D, 0.0D, -6.0D, 22.0D, 40.0D, 22.0D);

// BLOCKS

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

    public static final RegistryObject<Block> SANDSTONE_COLUMN = BLOCKS.register("sandstone_column",
            () -> new RotatedPillarBlock(BlockBehaviour.Properties.copy(Blocks.SANDSTONE)
                    .strength(2.0F)
                    .requiresCorrectToolForDrops()));

    public static final RegistryObject<Item> SANDSTONE_COLUMN_ITEM = ITEMS.register("sandstone_column",
            () -> new BlockItem(SANDSTONE_COLUMN.get(), new Item.Properties()));

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
            () -> new Block(BlockBehaviour.Properties.copy(Blocks.RAW_GOLD_BLOCK)
                    .strength(5.0F)
                    .requiresCorrectToolForDrops()));

    public static final RegistryObject<Item> NEPHRITIS_BLOCK_ITEM = ITEMS.register("nephritis_block",
            () -> new BlockItem(NEPHRITIS_BLOCK.get(), new Item.Properties()));

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


    public static final RegistryObject<Block> SANDSTONE_CORNER = BLOCKS.register("sandstone_corner",
            () -> new DirectionalPatternBlock(BlockBehaviour.Properties.copy(Blocks.SANDSTONE)
                    .strength(2.0F)
                    .requiresCorrectToolForDrops()));

    public static final RegistryObject<Item> SANDSTONE_CORNER_ITEM = ITEMS.register("sandstone_corner",
            () -> new BlockItem(SANDSTONE_CORNER.get(), new Item.Properties()));

    public static final RegistryObject<Block> SANDSTONE_LINE = BLOCKS.register("sandstone_line",
            () -> new DirectionalPatternBlock(BlockBehaviour.Properties.copy(Blocks.SANDSTONE)
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

    public static final RegistryObject<Block> GEN_VASE = BLOCKS.register("gen_vase",
            () -> new GenDecorateBlock(
                    BlockBehaviour.Properties.copy(Blocks.DECORATED_POT)
                            .sound(SoundType.DECORATED_POT)
                            .instabreak()
                            .noOcclusion()
            ));

    public static final RegistryObject<Item> GEN_VASE_ITEM = ITEMS.register("gen_vase",
            () -> new BlockItem(GEN_VASE.get(), new Item.Properties()));

    public static final RegistryObject<Block> AZAZEL_DESERTSTATUE = BLOCKS.register("azazel_desertstatue",
            () -> new GenericDecorateBlock(
                    BlockBehaviour.Properties.copy(Blocks.STONE)
                            .sound(SoundType.NETHER_BRICKS)
                            .strength(2.0F)
                            .noOcclusion()));

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

    public static final RegistryObject<BlockEntityType<StatBlockEntity>> STAT_BE = BLOCK_ENTITIES.register("stat",
            () -> BlockEntityType.Builder.of(StatBlockEntity::new, STAT.get()).build(null));

    public static final RegistryObject<BlockEntityType<StatueBlockEntity>> STATUE_BE = BLOCK_ENTITIES.register("statue",
            () -> BlockEntityType.Builder.of(StatueBlockEntity::new,
                    MONKI_STATUE.get(), DASHER_STATUE.get(), TITANA_STATUE.get()
            ).build(null));

    public static final RegistryObject<BlockEntityType<SandedChestBlockEntity>> SANDED_CHEST_BE = BLOCK_ENTITIES.register("sanded_chest",
            () -> BlockEntityType.Builder.of(SandedChestBlockEntity::new, SANDED_CHEST.get()).build(null));
    //============================
    // ENTITIES
    public static final RegistryObject<EntityType<MonkiEntity>> MONKI = ENTITIES.register("monki",
            () -> EntityType.Builder.of(MonkiEntity::new, MobCategory.MONSTER)
                    .sized(0.625F, 1.25F)
                    .build(ResourceLocation.fromNamespaceAndPath(MODID, "monki").toString()));

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

    public static final RegistryObject<EntityType<CactoEntity>> CACTO = ENTITIES.register("cacto",
            () -> EntityType.Builder.of(CactoEntity::new, MobCategory.MONSTER)
                    .sized(0.625F, 1.5F)
                    .build(ResourceLocation.fromNamespaceAndPath(MODID, "cacto").toString()));

    //=====================
    //SPAWN EGGS:
    public static final RegistryObject<Item> MONKI_SPAWN_EGG = ITEMS.register("monki_spawn_egg",
            () -> new net.minecraftforge.common.ForgeSpawnEggItem(
                    MONKI,
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

        modEventBus.addListener(this::setup);
        modEventBus.addListener(this::addCreative);

        MinecraftForge.EVENT_BUS.register(this);
    }

    private void setup(final FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            ModMessages.register();
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

        }
        if (event.getTabKey() == CreativeModeTabs.COMBAT) {
            event.accept(SUPER_GOLD_BOOTS);
            event.accept(SUPER_GOLD_HELMET);
            event.accept(SUPER_GOLD_CHESTPLATE);
            event.accept(SUPER_GOLD_LEGGINGS);
            event.accept(TITANA_HAMMER);
        }
        if (event.getTabKey() == CreativeModeTabs.INGREDIENTS) {
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
            event.put(CASER.get(), CaserEntity.createAttributes().build());
            event.put(MONKI_BIG.get(), MonkiBigEntity.createAttributes().build());
            event.put(TITANA.get(), TitanaEntity.createAttributes().build());
            event.put(SAND_HAND.get(), SandHandEntity.createAttributes().build());
            event.put(DASHER.get(), DasherEntity.createAttributes().build());
            event.put(CACTO.get(), CactoEntity.createAttributes().build());
        }
    }
}
