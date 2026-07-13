package com.benji.oasiso;

import com.benji.oasiso.common.block.DirectionalPatternBlock;
import com.mojang.logging.LogUtils;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.eventbus.api.IEventBus;
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
    // AHUI
    public static final RegistryObject<Item> SANDSTONE_CORNER_ITEM = ITEMS.register("sandstone_corner",
            () -> new BlockItem(SANDSTONE_CORNER.get(), new Item.Properties()));

    public static final RegistryObject<Block> SANDSTONE_LINE = BLOCKS.register("sandstone_line",
            () -> new DirectionalPatternBlock(BlockBehaviour.Properties.copy(Blocks.SANDSTONE)
                    .strength(2.0F)
                    .requiresCorrectToolForDrops()));

    public static final RegistryObject<Item> SANDSTONE_LINE_ITEM = ITEMS.register("sandstone_line",
            () -> new BlockItem(SANDSTONE_LINE.get(), new Item.Properties()));

    public static final RegistryObject<Block> FLOWERY = BLOCKS.register("flowery",
            () -> new Block(BlockBehaviour.Properties.copy(Blocks.GRASS)
                    .instabreak()
                    .noOcclusion()));

    public static final RegistryObject<Item> FLOWERY_ITEM = ITEMS.register("flowery",
            () -> new BlockItem(FLOWERY.get(), new Item.Properties()));

    public static final RegistryObject<Block> CACTULO = BLOCKS.register("cactulo",
            () -> new Block(BlockBehaviour.Properties.copy(Blocks.GRASS)
                    .instabreak()
                    .noOcclusion()));

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

    public Oasiso (FMLJavaModLoadingContext context) {
        IEventBus modEventBus = context.getModEventBus();

        BLOCKS.register(modEventBus);
        ITEMS.register(modEventBus);
        BLOCK_ENTITIES.register(modEventBus);
        ENTITIES.register(modEventBus);
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
    }
}
