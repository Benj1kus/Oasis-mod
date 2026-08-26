package com.benji.oasiso.registry;

import com.benji.oasiso.Oasiso;

import static com.benji.oasiso.Oasiso.MODID;

import com.benji.oasiso.common.item.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.*;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModItems {

    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, Oasiso.MODID);

    public static final RegistryObject<Item> LIE_BLOCK_ITEM = ITEMS.register("lie_block", () -> new BlockItem(ModBlocks.LIE_BLOCK.get(), new Item.Properties()));

    public static final RegistryObject<Item> SANDSTONE_TILES_ITEM = ITEMS.register("sandstone_tiles", () -> new BlockItem(ModBlocks.SANDSTONE_TILES.get(), new Item.Properties()));
    public static final RegistryObject<Item> SANDSTONE_POLISHED_ITEM = ITEMS.register("sandstone_polished", () -> new BlockItem(ModBlocks.SANDSTONE_POLISHED.get(), new Item.Properties()));

    public static final RegistryObject<Item> CHAOS_EYE_ITEM = ITEMS.register("chaos_eye", () -> new BlockItem(ModBlocks.CHAOS_EYE.get(), new Item.Properties()));
    public static final RegistryObject<Item> SANDSTONE_COLUMN_ITEM = ITEMS.register("sandstone_column", () -> new BlockItem(ModBlocks.SANDSTONE_COLUMN.get(), new Item.Properties()));

    public static final RegistryObject<Item> SCARLET_LEAVES_ITEM = ITEMS.register("scarlet_leaves", () -> new BlockItem(ModBlocks.SCARLET_LEAVES.get(), new Item.Properties()));
    public static final RegistryObject<Item> SCARLET_GRASS_ITEM = ITEMS.register("scarlet_grass", () -> new BlockItem(ModBlocks.SCARLET_GRASS.get(), new Item.Properties()));
    public static final RegistryObject<Item> SCARLET_LOG_ITEM = ITEMS.register("scarlet_log", () -> new BlockItem(ModBlocks.SCARLET_LOG.get(), new Item.Properties()));

    public static final RegistryObject<Item> NEPHRITIS_COLUMN_ITEM = ITEMS.register("nephritis_column", () -> new BlockItem(ModBlocks.NEPHRITIS_COLUMN.get(), new Item.Properties()));
    public static final RegistryObject<Item> NEPHRITIS_BRICKS_ITEM = ITEMS.register("nephritis_bricks", () -> new BlockItem(ModBlocks.NEPHRITIS_BRICKS.get(), new Item.Properties()));
    public static final RegistryObject<Item> NEPHRITIS_COMPRESSED_ITEM = ITEMS.register("nephritis_compressed", () -> new BlockItem(ModBlocks.NEPHRITIS_COMPRESSED.get(), new Item.Properties()));
    public static final RegistryObject<Item> NEPHRITIS_SPIRAL_ITEM = ITEMS.register("nephritis_spiral", () -> new BlockItem(ModBlocks.NEPHRITIS_SPIRAL.get(), new Item.Properties()));
    public static final RegistryObject<Item> NEPHRITIS_POLISHED_ITEM = ITEMS.register("nephritis_polished", () -> new BlockItem(ModBlocks.NEPHRITIS_POLISHED.get(), new Item.Properties()));
    public static final RegistryObject<Item> NEPHRITIS_CORNER_ITEM = ITEMS.register("nephritis_corner", () -> new BlockItem(ModBlocks.NEPHRITIS_CORNER.get(), new Item.Properties()));
    public static final RegistryObject<Item> NEPHRITIS_LINE_ITEM = ITEMS.register("nephritis_line", () -> new BlockItem(ModBlocks.NEPHRITIS_LINE.get(), new Item.Properties()));

    public static final RegistryObject<Item> SANDSTONE_COLORED_ITEM = ITEMS.register("sandstone_colored", () -> new BlockItem(ModBlocks.SANDSTONE_COLORED.get(), new Item.Properties()));
    public static final RegistryObject<Item> SANDSTONE_STRIPE_ITEM = ITEMS.register("sandstone_stripe", () -> new BlockItem(ModBlocks.SANDSTONE_STRIPE.get(), new Item.Properties()));
    public static final RegistryObject<Item> SANDSTONE_FLOORB_ITEM = ITEMS.register("sandstone_floorb", () -> new BlockItem(ModBlocks.SANDSTONE_FLOORB.get(), new Item.Properties()));

    public static final RegistryObject<Item> KARAKOLIT_BLOCK_ITEM = ITEMS.register("karakolit_block", () -> new BlockItem(ModBlocks.KARAKOLIT_BLOCK.get(), new Item.Properties()));
    public static final RegistryObject<Item> NEPHRITIS_BLOCK_ITEM = ITEMS.register("nephritis_block", () -> new BlockItem(ModBlocks.NEPHRITIS_BLOCK.get(), new Item.Properties()));

    public static final RegistryObject<Item> WIZARD_EYE_ITEM = ITEMS.register("wizard_eye", () -> new BlockItem(ModBlocks.WIZARD_EYE.get(), new Item.Properties()));
    public static final RegistryObject<Item> WIZARD_COLUMN_ITEM = ITEMS.register("wizard_column", () -> new BlockItem(ModBlocks.WIZARD_COLUMN.get(), new Item.Properties()));

    public static final RegistryObject<Item> SANDSTONE_ROOF_ITEM = ITEMS.register("sandstone_roof", () -> new BlockItem(ModBlocks.SANDSTONE_ROOF.get(), new Item.Properties()));
    public static final RegistryObject<Item> SANDSTONE_BRICKED_ITEM = ITEMS.register("sandstone_bricked", () -> new BlockItem(ModBlocks.SANDSTONE_BRICKED.get(), new Item.Properties()));
    public static final RegistryObject<Item> SANDSTONE_AZAZEL_ITEM = ITEMS.register("sandstone_azazel", () -> new BlockItem(ModBlocks.SANDSTONE_AZAZEL.get(), new Item.Properties()));

    public static final RegistryObject<Item> ENTROPY_BLOCK_ITEM = ITEMS.register("entropy_block", () -> new BlockItem(ModBlocks.ENTROPY_BLOCK.get(), new Item.Properties()));
    public static final RegistryObject<Item> ENTROPY_VEIN_ITEM = ITEMS.register("entropy_vein", () -> new BlockItem(ModBlocks.ENTROPY_VEIN.get(), new Item.Properties()));
    public static final RegistryObject<Item> CHAOS_SPAWNER_ITEM = ITEMS.register("chaos_spawner", () -> new BlockItem(ModBlocks.CHAOS_SPAWNER.get(), new Item.Properties()));

    public static final RegistryObject<Item> SANDSTONE_CORNER_ITEM = ITEMS.register("sandstone_corner", () -> new BlockItem(ModBlocks.SANDSTONE_CORNER.get(), new Item.Properties()));
    public static final RegistryObject<Item> SANDSTONE_LINE_ITEM = ITEMS.register("sandstone_line", () -> new BlockItem(ModBlocks.SANDSTONE_LINE.get(), new Item.Properties()));

    public static final RegistryObject<Item> FLOWERY_ITEM = ITEMS.register("flowery", () -> new BlockItem(ModBlocks.FLOWERY.get(), new Item.Properties()));
    public static final RegistryObject<Item> CACTULO_ITEM = ITEMS.register("cactulo", () -> new BlockItem(ModBlocks.CACTULO.get(), new Item.Properties()));

    public static final RegistryObject<Item> GEN_VASE_ITEM = ITEMS.register("gen_vase", () -> new BlockItem(ModBlocks.GEN_VASE.get(), new Item.Properties()));
    public static final RegistryObject<Item> BALL_CACTUS_ITEM = ITEMS.register("ball_cactus", () -> new BlockItem(ModBlocks.BALL_CACTUS.get(), new Item.Properties()));
    public static final RegistryObject<Item> STORM_TOTEM_ITEM = ITEMS.register("storm_totem", () -> new BlockItem(ModBlocks.STORM_TOTEM.get(), new Item.Properties()));
    public static final RegistryObject<Item> AZAZEL_DESERTSTATUE_ITEM = ITEMS.register("azazel_desertstatue", () -> new BlockItem(ModBlocks.AZAZEL_DESERTSTATUE.get(), new Item.Properties()));
    public static final RegistryObject<Item> STAT_LANTERN_ITEM = ITEMS.register("stat_lantern", () -> new BlockItem(ModBlocks.STAT_LANTERN.get(), new Item.Properties()));
    public static final RegistryObject<Item> NEPHRITIS_LAMP_ITEM = ITEMS.register("nephritis_lamp", () -> new BlockItem(ModBlocks.NEPHRITIS_LAMP.get(), new Item.Properties()));
    public static final RegistryObject<Item> STAT_ITEM = ITEMS.register("stat", () -> new BlockItem(ModBlocks.STAT.get(), new Item.Properties()));
    public static final RegistryObject<Item> SANDED_CHEST_ITEM = ITEMS.register("sanded_chest", () -> new GeoBlockItem(ModBlocks.SANDED_CHEST.get(), new Item.Properties(), ResourceLocation.fromNamespaceAndPath(MODID, "geo/sanded_chest.geo.json"), ResourceLocation.fromNamespaceAndPath(MODID, "textures/block/sanded_chest.png"), ResourceLocation.fromNamespaceAndPath(MODID, "animations/sanded_chest.animation.json"), ResourceLocation.fromNamespaceAndPath(MODID, "textures/block/empty.png")));
    public static final RegistryObject<Item> DASHER_STATUE_ITEM = ITEMS.register("dasher_statue", () -> new GeoBlockItem(ModBlocks.DASHER_STATUE.get(), new Item.Properties(), ResourceLocation.fromNamespaceAndPath(MODID, "geo/dasher_statue.geo.json"), ResourceLocation.fromNamespaceAndPath(MODID, "textures/block/dasher_statue.png"), ResourceLocation.fromNamespaceAndPath(MODID, "animations/empty.json"), ResourceLocation.fromNamespaceAndPath(MODID, "textures/block/empty.png")));
    public static final RegistryObject<Item> MONKI_STATUE_ITEM = ITEMS.register("monki_statue", () -> new GeoBlockItem(ModBlocks.MONKI_STATUE.get(), new Item.Properties(), ResourceLocation.fromNamespaceAndPath(MODID, "geo/monki_statue.geo.json"), ResourceLocation.fromNamespaceAndPath(MODID, "textures/block/monki_statue.png"), ResourceLocation.fromNamespaceAndPath(MODID, "animations/empty.animation.json"), ResourceLocation.fromNamespaceAndPath(MODID, "textures/block/empty.png")));
    public static final RegistryObject<Item> SKELET_BELIEVER_ITEM = ITEMS.register("skelet_believer", () -> new BlockItem(ModBlocks.SKELET_BELIEVER.get(), new Item.Properties()));
    public static final RegistryObject<Item> SAND_GOOSE_ITEM = ITEMS.register("sand_goose", () -> new BlockItem(ModBlocks.SAND_GOOSE.get(), new Item.Properties()));
    public static final RegistryObject<Item> CHAOS_ALTAR_ITEM = ITEMS.register("chaos_altar", () -> new GeoBlockItem(ModBlocks.CHAOS_ALTAR.get(), new Item.Properties(), ResourceLocation.fromNamespaceAndPath(MODID, "geo/chaos_altar.geo.json"), ResourceLocation.fromNamespaceAndPath(MODID, "textures/block/chaos_altar.png"), ResourceLocation.fromNamespaceAndPath(MODID, "animations/chaos_altar.animation.json"), ResourceLocation.fromNamespaceAndPath(MODID, "textures/block/empty.png")));
    public static final RegistryObject<Item> TITANA_STATUE_ITEM = ITEMS.register("titana_statue", () -> new GeoBlockItem(ModBlocks.TITANA_STATUE.get(), new Item.Properties(), ResourceLocation.fromNamespaceAndPath(MODID, "geo/titana_statue.geo.json"), ResourceLocation.fromNamespaceAndPath(MODID, "textures/block/titana_statue.png"), ResourceLocation.fromNamespaceAndPath(MODID, "animations/empty.animation.json"), ResourceLocation.fromNamespaceAndPath(MODID, "textures/block/empty.png")));
    public static final RegistryObject<Item> CACTOS_ITEM = ITEMS.register("cactos", () -> new BlockItem(ModBlocks.CACTOS.get(), new Item.Properties()));

    public static final RegistryObject<Item> KARAKOLIT_INGOT = ITEMS.register("karakolit_ingot", () -> new Item(new Item.Properties().stacksTo(64)));
    public static final RegistryObject<Item> NEPHRITIS = ITEMS.register("nephritis", () -> new Item(new Item.Properties().stacksTo(16)));
    public static final RegistryObject<Item> NEPHRITIS_CORE = ITEMS.register("nephritis_core", () -> new Item(new Item.Properties().stacksTo(1)));

    public static final RegistryObject<Item> ORB_CHAOS = ITEMS.register("orb_chaos", () -> new Item(new Item.Properties().stacksTo(1)));
    public static final RegistryObject<Item> ORB_DOMINATION = ITEMS.register("orb_domination", () -> new Item(new Item.Properties().stacksTo(1)));
    public static final RegistryObject<Item> KARAKOLIT_KEY = ITEMS.register("karakolit_key", () -> new Item(new Item.Properties().stacksTo(1)));

    public static final RegistryObject<Item> ENCHANTED_BOOK_HAMMER = ITEMS.register("enchanted_book_hammer", () -> new HammerEnchantmentBookItem(new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON)));
    public static final RegistryObject<Item> BOMBUL_BOTTLE = ITEMS.register("bombul_bottle", () -> new BombulBottleItem(new Item.Properties().stacksTo(1)));
    public static final RegistryObject<Item> BOMBUL_BOTTLE_EMPTY = ITEMS.register("bombul_bottle_empty", () -> new Item(new Item.Properties().stacksTo(16)));
    public static final RegistryObject<Item> TITANA_HAMMER = ITEMS.register("titana_hammer", () -> new TitanaHammerItem(new Item.Properties().stacksTo(1)));

    public static final RegistryObject<Item> SUPER_GOLD_HELMET = ITEMS.register("super_gold_helmet", () -> new SuperGoldArmorItem(ArmorItem.Type.HELMET, new Item.Properties().stacksTo(1)));
    public static final RegistryObject<Item> SUPER_GOLD_CHESTPLATE = ITEMS.register("super_gold_chestplate", () -> new SuperGoldArmorItem(ArmorItem.Type.CHESTPLATE, new Item.Properties().stacksTo(1)));
    public static final RegistryObject<Item> SUPER_GOLD_LEGGINGS = ITEMS.register("super_gold_leggings", () -> new SuperGoldArmorItem(ArmorItem.Type.LEGGINGS, new Item.Properties().stacksTo(1)));
    public static final RegistryObject<Item> SUPER_GOLD_BOOTS = ITEMS.register("super_gold_boots", () -> new SuperGoldArmorItem(ArmorItem.Type.BOOTS, new Item.Properties().stacksTo(1)));

    public static final RegistryObject<Item> CASSASIN_SPAWN_EGG = ITEMS.register("cassasin_spawn_egg", () -> new net.minecraftforge.common.ForgeSpawnEggItem(ModEntities.CRUSADER_ASSASIN, 0xFFFFFF, 0xFFFFFF, new Item.Properties()));
    public static final RegistryObject<Item> CTANK_SPAWN_EGG = ITEMS.register("ctank_spawn_egg", () -> new net.minecraftforge.common.ForgeSpawnEggItem(ModEntities.CRUSADER_TANK, 0xFFFFFF, 0xFFFFFF, new Item.Properties()));
    public static final RegistryObject<Item> CWARRIOR_SPAWN_EGG = ITEMS.register("cwarrior_spawn_egg", () -> new net.minecraftforge.common.ForgeSpawnEggItem(ModEntities.CRUSADER_WARRIOR, 0xFFFFFF, 0xFFFFFF, new Item.Properties()));
    public static final RegistryObject<Item> CWIZARD_SPAWN_EGG = ITEMS.register("cwizard_spawn_egg", () -> new net.minecraftforge.common.ForgeSpawnEggItem(ModEntities.CRUSADER_WIZARD, 0xFFFFFF, 0xFFFFFF, new Item.Properties()));
    public static final RegistryObject<Item> CPALADIN_SPAWN_EGG = ITEMS.register("cpaladin_spawn_egg", () -> new net.minecraftforge.common.ForgeSpawnEggItem(ModEntities.PALADIN, 0xFFFFFF, 0xFFFFFF, new Item.Properties()));
    public static final RegistryObject<Item> MONKI_SPAWN_EGG = ITEMS.register("monki_spawn_egg", () -> new net.minecraftforge.common.ForgeSpawnEggItem(ModEntities.MONKI, 0xFFFFFF, 0xFFFFFF, new Item.Properties()));
    public static final RegistryObject<Item> KROMBUL_SPAWN_EGG = ITEMS.register("krombul_spawn_egg", () -> new net.minecraftforge.common.ForgeSpawnEggItem(ModEntities.KROMBUL, 0xFFFFFF, 0xFFFFFF, new Item.Properties()));
    public static final RegistryObject<Item> BOMBUL_SPAWN_EGG = ITEMS.register("bombul_spawn_egg", () -> new net.minecraftforge.common.ForgeSpawnEggItem(ModEntities.BOMBUL, 0xFFFFFF, 0xFFFFFF, new Item.Properties()));
    public static final RegistryObject<Item> TITANA_SPAWN_EGG = ITEMS.register("titana_spawn_egg", () -> new net.minecraftforge.common.ForgeSpawnEggItem(ModEntities.TITANA, 0xFFFFFF, 0xFFFFFF, new Item.Properties()));
    public static final RegistryObject<Item> CACTO_SPAWN_EGG = ITEMS.register("cacto_spawn_egg", () -> new net.minecraftforge.common.ForgeSpawnEggItem(ModEntities.CACTO, 0xFFFFFF, 0xFFFFFF, new Item.Properties()));

    public static final RegistryObject<Item> DASHER_SPAWN_EGG = ITEMS.register("dasher_spawn_egg", () -> new net.minecraftforge.common.ForgeSpawnEggItem(ModEntities.DASHER, 0xFFFFFF, 0xFFFFFF, new Item.Properties()));
    public static final RegistryObject<Item> CASER_SPAWN_EGG = ITEMS.register("caser_spawn_egg", () -> new net.minecraftforge.common.ForgeSpawnEggItem(ModEntities.CASER, 0xFFFFFF, 0xFFFFFF, new Item.Properties()));
    public static final RegistryObject<Item> SAND_GOLEM_SPAWN_EGG = ITEMS.register("sand_golem_spawn_egg", () -> new net.minecraftforge.common.ForgeSpawnEggItem(ModEntities.SAND_GOLEM, 0xFFFFFF, 0xFFFFFF, new Item.Properties()));

    public static final RegistryObject<Item> DOUM_PALM_SIGN_ITEM = ITEMS.register("doum_palm_sign", () -> new SignItem(new Item.Properties().stacksTo(16), ModBlocks.DOUM_PALM_SIGN.get(), ModBlocks.DOUM_PALM_WALL_SIGN.get()));
    public static final RegistryObject<Item> DOUM_PALM_HANGING_SIGN_ITEM = ITEMS.register("doum_palm_hanging_sign", () -> new HangingSignItem(ModBlocks.DOUM_PALM_HANGING_SIGN.get(), ModBlocks.DOUM_PALM_WALL_HANGING_SIGN.get(), new Item.Properties().stacksTo(16)));
    public static final RegistryObject<Item> DOUM_PALM_BUTTON_ITEM = ITEMS.register("doum_palm_button", () -> new BlockItem(ModBlocks.DOUM_PALM_BUTTON.get(), new Item.Properties()));
    public static final RegistryObject<Item> DOUM_PALM_PRESSURE_PLATE_ITEM = ITEMS.register("doum_palm_pressure_plate", () -> new BlockItem(ModBlocks.DOUM_PALM_PRESSURE_PLATE.get(), new Item.Properties()));
    public static final RegistryObject<Item> DOUM_PALM_SLAB_ITEM = ITEMS.register("doum_palm_slab", () -> new BlockItem(ModBlocks.DOUM_PALM_SLAB.get(), new Item.Properties()));
    public static final RegistryObject<Item> DOUM_PALM_STAIRS_ITEM = ITEMS.register("doum_palm_stairs", () -> new BlockItem(ModBlocks.DOUM_PALM_STAIRS.get(), new Item.Properties()));
    public static final RegistryObject<Item> DOUM_PALM_WOOD_ITEM = ITEMS.register("doum_palm_wood", () -> new BlockItem(ModBlocks.DOUM_PALM_WOOD.get(), new Item.Properties()));
    public static final RegistryObject<Item> STRIPPED_DOUM_PALM_WOOD_ITEM = ITEMS.register("stripped_doum_palm_wood", () -> new BlockItem(ModBlocks.STRIPPED_DOUM_PALM_WOOD.get(), new Item.Properties()));
    public static final RegistryObject<Item> DOUM_PALM_LOG_ITEM = ITEMS.register("doum_palm_log", () -> new BlockItem(ModBlocks.DOUM_PALM_LOG.get(), new Item.Properties()));
    public static final RegistryObject<Item> STRIPPED_DOUM_PALM_LOG_ITEM = ITEMS.register("stripped_doum_palm_log", () -> new BlockItem(ModBlocks.STRIPPED_DOUM_PALM_LOG.get(), new Item.Properties()));
    public static final RegistryObject<Item> DOUM_PALM_PLANKS_ITEM = ITEMS.register("doum_palm_planks", () -> new BlockItem(ModBlocks.DOUM_PALM_PLANKS.get(), new Item.Properties()));
    public static final RegistryObject<Item> DOUM_PALM_LEAVES_ITEM = ITEMS.register("doum_palm_leaves", () -> new BlockItem(ModBlocks.DOUM_PALM_LEAVES.get(), new Item.Properties()));
    public static final RegistryObject<Item> DOUM_PALM_SAPLING_ITEM = ITEMS.register("doum_palm_sapling", () -> new BlockItem(ModBlocks.DOUM_PALM_SAPLING.get(), new Item.Properties()));
    public static final RegistryObject<Item> DOUM_PALM_DOOR_ITEM = ITEMS.register("doum_palm_door", () -> new BlockItem(ModBlocks.DOUM_PALM_DOOR.get(), new Item.Properties()));
    public static final RegistryObject<Item> DOUM_PALM_TRAPDOOR_ITEM = ITEMS.register("doum_palm_trapdoor", () -> new BlockItem(ModBlocks.DOUM_PALM_TRAPDOOR.get(), new Item.Properties()));
    public static final RegistryObject<Item> DOUM_PALM_FENCE_ITEM = ITEMS.register("doum_palm_fence", () -> new BlockItem(ModBlocks.DOUM_PALM_FENCE.get(), new Item.Properties()));
    public static final RegistryObject<Item> DOUM_PALM_FENCE_GATE_ITEM = ITEMS.register("doum_palm_fence_gate", () -> new BlockItem(ModBlocks.DOUM_PALM_FENCE_GATE.get(), new Item.Properties()));
    public static final RegistryObject<Item> DOUM_PALM_BOAT_ITEM = ITEMS.register("doum_palm_boat", () -> new DoumPalmBoatItem(false, new Item.Properties().stacksTo(1)));
    public static final RegistryObject<Item> DOUM_PALM_CHEST_BOAT_ITEM = ITEMS.register("doum_palm_chest_boat", () -> new DoumPalmBoatItem(true, new Item.Properties().stacksTo(1)));

    private ModItems() {
    }

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}
