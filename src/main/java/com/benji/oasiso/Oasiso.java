package com.benji.oasiso;

import com.benji.oasiso.client.renderer.CactoRenderer;
import com.benji.oasiso.client.renderer.MonkiRenderer;
import com.benji.oasiso.common.block.*;
import com.benji.oasiso.common.block.entity.SandedChestBlockEntity;
import com.benji.oasiso.common.block.entity.StatBlockEntity;
import com.benji.oasiso.common.entity.*;
import com.benji.oasiso.common.entity.projectile.CactoProjEntity;
import com.benji.oasiso.common.entity.projectile.DesertBallEntity;
import com.benji.oasiso.common.item.GeoBlockItem;
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

    public static final RegistryObject<CreativeModeTab> OASISO_TAB = CREATIVE_MODE_TABS.register("oasiso_tab",
            () -> CreativeModeTab.builder()
                    .icon(() -> new ItemStack(FLOWERY_ITEM.get()))
                    .title(Component.translatable("creativetab.oasiso_tab"))
                    .displayItems((parameters, output) -> {
                        for (RegistryObject<Item> item : ITEMS.getEntries()) {
                            output.accept(item.get());
                        }
                    })
                    .build()
    );

    public static final RegistryObject<Block> GEN_VASE = BLOCKS.register("gen_vase",
            () -> new GenDecorateBlock(
                    BlockBehaviour.Properties.copy(Blocks.DECORATED_POT)
                            .sound(SoundType.DECORATED_POT)
                            .instabreak()
                            .noOcclusion()
            ));

    public static final RegistryObject<Item> GEN_VASE_ITEM = ITEMS.register("gen_vase",
            () -> new BlockItem(GEN_VASE.get(), new Item.Properties()));

    public static final RegistryObject<Block> STAT_LANTERN = BLOCKS.register("stat_lantern",
            () -> new GenDecorateBlock(BlockBehaviour.Properties.copy(Blocks.STONE)
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

    //BLOCK ENTITIES

    public static final RegistryObject<BlockEntityType<StatBlockEntity>> STAT_BE = BLOCK_ENTITIES.register("stat",
            () -> BlockEntityType.Builder.of(StatBlockEntity::new, STAT.get()).build(null));

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

    public Oasiso(FMLJavaModLoadingContext context) {
        IEventBus modEventBus = context.getModEventBus();

        BLOCKS.register(modEventBus);
        ITEMS.register(modEventBus);
        BLOCK_ENTITIES.register(modEventBus);
        ENTITIES.register(modEventBus);
        ModSounds.SOUNDS.register(modEventBus);
        CREATIVE_MODE_TABS.register(modEventBus);
        STRUCTURE_TYPES.register(modEventBus);

        modEventBus.addListener(this::addCreative);

        MinecraftForge.EVENT_BUS.register(this);
    }

    private void addCreative(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.NATURAL_BLOCKS || event.getTabKey() == CreativeModeTabs.BUILDING_BLOCKS) {
            event.accept(SANDSTONE_BRICKED_ITEM);
            event.accept(SANDSTONE_LINE_ITEM);
            event.accept(SANDSTONE_CORNER_ITEM);
            event.accept(SANDSTONE_ROOF_ITEM);
            event.accept(SANDSTONE_FLOORB_ITEM);
            event.accept(SANDSTONE_TILES_ITEM);
            event.accept(SANDSTONE_STRIPE_ITEM);
            event.accept(SANDSTONE_COLORED_ITEM);
            event.accept(SANDSTONE_COLUMN_ITEM);
            event.accept(FLOWERY_ITEM);
            event.accept(CACTULO_ITEM);

        }
        if (event.getTabKey() == CreativeModeTabs.FUNCTIONAL_BLOCKS) {
            event.accept(GEN_VASE_ITEM);
            event.accept(STAT_LANTERN_ITEM);
            event.accept(STAT);
            event.accept(SANDED_CHEST_ITEM);
        }
    }

    @Mod.EventBusSubscriber(modid = MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static class ModEvents {
        @SubscribeEvent
        public static void registerAttributes(EntityAttributeCreationEvent event) {
            event.put(MONKI.get(), MonkiEntity.createAttributes().build());
            event.put(MONKI_BIG.get(), MonkiBigEntity.createAttributes().build());
            event.put(TITANA.get(), TitanaEntity.createAttributes().build());
            event.put(SAND_HAND.get(), SandHandEntity.createAttributes().build());
            event.put(DASHER.get(), DasherEntity.createAttributes().build());
            event.put(CACTO.get(), CactoEntity.createAttributes().build());
        }
    }
}
