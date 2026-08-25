package com.benji.oasiso.registry;

import com.benji.oasiso.Oasiso;
import com.benji.oasiso.common.block.*;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.BlockSetType;
import net.minecraft.world.level.block.state.properties.WoodType;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModBlocks {

    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, Oasiso.MODID);

    public static final BlockSetType DOUM_PALM_SET_TYPE = BlockSetType.register(new BlockSetType(Oasiso.MODID + ":doum_palm"));

    public static final WoodType DOUM_PALM_WOOD_TYPE = WoodType.register(new WoodType(Oasiso.MODID + ":doum_palm", DOUM_PALM_SET_TYPE));

    private static final VoxelShape MONKI_SHAPE = Block.box(3.0D, 0.0D, 3.0D, 13.0D, 20.0D, 13.0D);

    private static final VoxelShape DASHER_SHAPE = Block.box(0.0D, 0.0D, 0.0D, 16.0D, 50.0D, 16.0D);

    private static final VoxelShape TITANA_SHAPE = Block.box(-6.0D, 0.0D, -6.0D, 22.0D, 40.0D, 22.0D);

    public static final RegistryObject<Block> LIE_BLOCK = BLOCKS.register("lie_block", () -> new LieBlock(BlockBehaviour.Properties.copy(Blocks.STONE).sound(SoundType.SCULK_CATALYST).strength(80.0F).noOcclusion().explosionResistance(3_600_000.0F).dynamicShape().requiresCorrectToolForDrops()));
    public static final RegistryObject<Block> SANDSTONE_TILES = BLOCKS.register("sandstone_tiles", () -> new DirectionalPatternBlock(BlockBehaviour.Properties.copy(Blocks.SANDSTONE).strength(2.0F).requiresCorrectToolForDrops()));
    public static final RegistryObject<Block> SANDSTONE_POLISHED = BLOCKS.register("sandstone_polished", () -> new Block(BlockBehaviour.Properties.copy(Blocks.SANDSTONE).strength(2.0F).requiresCorrectToolForDrops()));
    public static final RegistryObject<Block> CHAOS_EYE = BLOCKS.register("chaos_eye", () -> new ChaosEyeBlock(BlockBehaviour.Properties.copy(Blocks.AMETHYST_BLOCK).instabreak().noOcclusion().requiresCorrectToolForDrops()));
    public static final RegistryObject<Block> SANDSTONE_COLUMN = BLOCKS.register("sandstone_column", () -> new RotatedPillarBlock(BlockBehaviour.Properties.copy(Blocks.SANDSTONE).strength(2.0F).requiresCorrectToolForDrops()));
    public static final RegistryObject<Block> SCARLET_LEAVES = BLOCKS.register("scarlet_leaves", () -> new Block(BlockBehaviour.Properties.copy(Blocks.SPONGE).strength(1.0F).noOcclusion()));
    public static final RegistryObject<Block> SCARLET_GRASS = BLOCKS.register("scarlet_grass", () -> new Block(BlockBehaviour.Properties.copy(Blocks.SPONGE).strength(1.0F)));
    public static final RegistryObject<Block> SCARLET_LOG = BLOCKS.register("scarlet_log", () -> new RotatedPillarBlock(BlockBehaviour.Properties.copy(Blocks.OAK_WOOD).strength(4.0F).requiresCorrectToolForDrops()));
    public static final RegistryObject<Block> NEPHRITIS_COLUMN = BLOCKS.register("nephritis_column", () -> new RotatedPillarBlock(BlockBehaviour.Properties.copy(Blocks.STONE).strength(6.0F).requiresCorrectToolForDrops()));
    public static final RegistryObject<Block> NEPHRITIS_BRICKS = BLOCKS.register("nephritis_bricks", () -> new RotatedPillarBlock(BlockBehaviour.Properties.copy(Blocks.STONE).strength(8.0F).requiresCorrectToolForDrops()));
    public static final RegistryObject<Block> NEPHRITIS_COMPRESSED = BLOCKS.register("nephritis_compressed", () -> new Block(BlockBehaviour.Properties.copy(Blocks.STONE).strength(10.0F).requiresCorrectToolForDrops()));
    public static final RegistryObject<Block> NEPHRITIS_SPIRAL = BLOCKS.register("nephritis_spiral", () -> new Block(BlockBehaviour.Properties.copy(Blocks.STONE).strength(5.0F).requiresCorrectToolForDrops()));
    public static final RegistryObject<Block> NEPHRITIS_POLISHED = BLOCKS.register("nephritis_polished", () -> new Block(BlockBehaviour.Properties.copy(Blocks.STONE).strength(3.0F).requiresCorrectToolForDrops()));
    public static final RegistryObject<Block> NEPHRITIS_CORNER = BLOCKS.register("nephritis_corner", () -> new DirectionalPillarBlock(BlockBehaviour.Properties.copy(Blocks.STONE).strength(2.0F).requiresCorrectToolForDrops()));
    public static final RegistryObject<Block> NEPHRITIS_LINE = BLOCKS.register("nephritis_line", () -> new DirectionalPillarBlock(BlockBehaviour.Properties.copy(Blocks.STONE).strength(2.0F).requiresCorrectToolForDrops()));
    public static final RegistryObject<Block> SANDSTONE_COLORED = BLOCKS.register("sandstone_colored", () -> new Block(BlockBehaviour.Properties.copy(Blocks.SANDSTONE).strength(2.0F).requiresCorrectToolForDrops()));
    public static final RegistryObject<Block> SANDSTONE_STRIPE = BLOCKS.register("sandstone_stripe", () -> new RotatedPillarBlock(BlockBehaviour.Properties.copy(Blocks.SANDSTONE).strength(2.0F).requiresCorrectToolForDrops()));
    public static final RegistryObject<Block> SANDSTONE_FLOORB = BLOCKS.register("sandstone_floorb", () -> new Block(BlockBehaviour.Properties.copy(Blocks.SANDSTONE).strength(2.0F).requiresCorrectToolForDrops()));
    public static final RegistryObject<Block> KARAKOLIT_BLOCK = BLOCKS.register("karakolit_block", () -> new Block(BlockBehaviour.Properties.copy(Blocks.NETHERITE_BLOCK).strength(5.0F).requiresCorrectToolForDrops()));
    public static final RegistryObject<Block> NEPHRITIS_BLOCK = BLOCKS.register("nephritis_block", () -> new NephritisBlock(BlockBehaviour.Properties.copy(Blocks.RAW_GOLD_BLOCK).strength(5.0F).requiresCorrectToolForDrops()));
    public static final RegistryObject<Block> WIZARD_EYE = BLOCKS.register("wizard_eye", () -> new Block(BlockBehaviour.Properties.copy(Blocks.STONE).strength(5.0F).requiresCorrectToolForDrops()));
    public static final RegistryObject<Block> WIZARD_COLUMN = BLOCKS.register("wizard_column", () -> new Block(BlockBehaviour.Properties.copy(Blocks.STONE).strength(5.0F).requiresCorrectToolForDrops()));
    public static final RegistryObject<Block> SANDSTONE_ROOF = BLOCKS.register("sandstone_roof", () -> new Block(BlockBehaviour.Properties.copy(Blocks.SANDSTONE).strength(2.0F).requiresCorrectToolForDrops()));
    public static final RegistryObject<Block> SANDSTONE_BRICKED = BLOCKS.register("sandstone_bricked", () -> new Block(BlockBehaviour.Properties.copy(Blocks.SANDSTONE).strength(2.0F).requiresCorrectToolForDrops()));
    public static final RegistryObject<Block> SANDSTONE_AZAZEL = BLOCKS.register("sandstone_azazel", () -> new Block(BlockBehaviour.Properties.copy(Blocks.SANDSTONE).strength(2.0F).requiresCorrectToolForDrops()));
    public static final RegistryObject<Block> ENTROPY_BLOCK = BLOCKS.register("entropy_block", () -> new EntropyBlock(BlockBehaviour.Properties.copy(Blocks.AMETHYST_BLOCK).lightLevel(state -> 20).strength(10.0F).requiresCorrectToolForDrops()));
    public static final RegistryObject<Block> CHAOS_PORTAL = BLOCKS.register("chaos_portal", () -> new ChaosPortalBlock(BlockBehaviour.Properties.copy(Blocks.NETHER_PORTAL)));
    public static final RegistryObject<Block> ENTROPY_VEIN = BLOCKS.register("entropy_vein", () -> new EntropyVeinBlock(BlockBehaviour.Properties.copy(Blocks.SCULK_VEIN).sound(SoundType.SMALL_AMETHYST_BUD).lightLevel(state -> 10).noCollission().noOcclusion().replaceable().strength(0.2F)));
    public static final RegistryObject<Block> SANDSTONE_CORNER = BLOCKS.register("sandstone_corner", () -> new DirectionalPillarBlock(BlockBehaviour.Properties.copy(Blocks.SANDSTONE).strength(2.0F).requiresCorrectToolForDrops()));
    public static final RegistryObject<Block> SANDSTONE_LINE = BLOCKS.register("sandstone_line", () -> new DirectionalPillarBlock(BlockBehaviour.Properties.copy(Blocks.SANDSTONE).strength(2.0F).requiresCorrectToolForDrops()));
    public static final RegistryObject<Block> FLOWERY = BLOCKS.register("flowery", () -> new OasisoFlowerBlock(MobEffects.SATURATION, 1, BlockBehaviour.Properties.copy(Blocks.DANDELION).instabreak().noOcclusion()));
    public static final RegistryObject<Block> CACTULO = BLOCKS.register("cactulo", () -> new CactuloBlock(MobEffects.SATURATION, 1, BlockBehaviour.Properties.copy(Blocks.DEAD_BUSH).instabreak().noOcclusion()));
    public static final RegistryObject<Block> POTTED_FLOWERY = BLOCKS.register("potted_flowery", () -> new FlowerPotBlock(() -> (FlowerPotBlock) Blocks.FLOWER_POT, FLOWERY, BlockBehaviour.Properties.copy(Blocks.POTTED_DANDELION).instabreak().noOcclusion()));
    public static final RegistryObject<Block> POTTED_CACTULO = BLOCKS.register("potted_cactulo", () -> new FlowerPotBlock(() -> (FlowerPotBlock) Blocks.FLOWER_POT, CACTULO, BlockBehaviour.Properties.copy(Blocks.POTTED_DANDELION).instabreak().noOcclusion()));
    public static final RegistryObject<Block> GEN_VASE = BLOCKS.register("gen_vase", () -> new GenDecorateBlock(BlockBehaviour.Properties.copy(Blocks.DECORATED_POT).sound(SoundType.DECORATED_POT).instabreak().noOcclusion()));
    public static final RegistryObject<Block> BALL_CACTUS = BLOCKS.register("ball_cactus", () -> new com.benji.oasiso.common.block.BallCactusBlock(BlockBehaviour.Properties.copy(Blocks.WHITE_WOOL).sound(SoundType.WOOL).instabreak().noOcclusion().randomTicks()));
    public static final RegistryObject<Block> AZAZEL_DESERTSTATUE = BLOCKS.register("azazel_desertstatue", () -> new AzazelDecorateBlock(BlockBehaviour.Properties.copy(Blocks.STONE).sound(SoundType.NETHER_BRICKS).strength(2.0F).noOcclusion()));
    public static final RegistryObject<Block> STORM_TOTEM = BLOCKS.register("storm_totem", () -> new StormTotemBlock(BlockBehaviour.Properties.copy(Blocks.STONE).sound(SoundType.NETHER_BRICKS).strength(5.0F).requiresCorrectToolForDrops().noOcclusion()));
    public static final RegistryObject<Block> STAT_LANTERN = BLOCKS.register("stat_lantern", () -> new GenericDecorateBlock(BlockBehaviour.Properties.copy(Blocks.STONE).lightLevel(state -> 10).requiresCorrectToolForDrops().strength(2.0F).noOcclusion()));
    public static final RegistryObject<Block> NEPHRITIS_LAMP = BLOCKS.register("nephritis_lamp", () -> new NephritisLampBlock(BlockBehaviour.Properties.copy(Blocks.STONE).lightLevel(state -> 15).requiresCorrectToolForDrops().strength(8.0F).noOcclusion()));
    public static final RegistryObject<Block> STAT = BLOCKS.register("stat", () -> new StatBlock(BlockBehaviour.Properties.copy(Blocks.STONE).strength(2.0F).requiresCorrectToolForDrops()));
    public static final RegistryObject<Block> SANDED_CHEST = BLOCKS.register("sanded_chest", () -> new SandedChestBlock(BlockBehaviour.Properties.copy(Blocks.OAK_WOOD).strength(2.0F).requiresCorrectToolForDrops()));
    public static final RegistryObject<Block> DASHER_STATUE = BLOCKS.register("dasher_statue", () -> new StatueBlock(DASHER_SHAPE, BlockBehaviour.Properties.copy(Blocks.STONE).strength(200.0F).requiresCorrectToolForDrops().noOcclusion()));
    public static final RegistryObject<Block> MONKI_STATUE = BLOCKS.register("monki_statue", () -> new StatueBlock(MONKI_SHAPE, BlockBehaviour.Properties.copy(Blocks.STONE).strength(100.0F).requiresCorrectToolForDrops().noOcclusion()));
    public static final RegistryObject<Block> SKELET_BELIEVER = BLOCKS.register("skelet_believer", () -> new SkeletBlock(BlockBehaviour.Properties.copy(Blocks.SOUL_SAND).strength(5.0F).requiresCorrectToolForDrops().noOcclusion()));
    public static final RegistryObject<Block> SAND_GOOSE = BLOCKS.register("sand_goose", () -> new AnnoyingGooseBlock(BlockBehaviour.Properties.copy(Blocks.STONE).strength(2.0F).requiresCorrectToolForDrops().noOcclusion()));
    public static final RegistryObject<Block> CHAOS_ALTAR = BLOCKS.register("chaos_altar", () -> new ChaosAltarBlock(BlockBehaviour.Properties.copy(Blocks.STONE).strength(100.0F).requiresCorrectToolForDrops().noOcclusion()));
    public static final RegistryObject<Block> TITANA_STATUE = BLOCKS.register("titana_statue", () -> new StatueBlock(TITANA_SHAPE, BlockBehaviour.Properties.copy(Blocks.STONE).strength(300.0F).requiresCorrectToolForDrops().noOcclusion()));
    public static final RegistryObject<Block> CACTOS = BLOCKS.register("cactos", () -> new CactosBlock(BlockBehaviour.Properties.copy(Blocks.CACTUS).noOcclusion().instabreak()));
    public static final RegistryObject<Block> DOUM_PALM_LOG = BLOCKS.register("doum_palm_log", () -> new RotatedPillarBlock(BlockBehaviour.Properties.copy(Blocks.JUNGLE_LOG)));
    public static final RegistryObject<Block> DOUM_PALM_WOOD = BLOCKS.register("doum_palm_wood", () -> new RotatedPillarBlock(BlockBehaviour.Properties.copy(Blocks.JUNGLE_WOOD)));
    public static final RegistryObject<Block> STRIPPED_DOUM_PALM_LOG = BLOCKS.register("stripped_doum_palm_log", () -> new RotatedPillarBlock(BlockBehaviour.Properties.copy(Blocks.STRIPPED_JUNGLE_LOG)));
    public static final RegistryObject<Block> STRIPPED_DOUM_PALM_WOOD = BLOCKS.register("stripped_doum_palm_wood", () -> new RotatedPillarBlock(BlockBehaviour.Properties.copy(Blocks.STRIPPED_JUNGLE_WOOD)));
    public static final RegistryObject<Block> DOUM_PALM_PLANKS = BLOCKS.register("doum_palm_planks", () -> new Block(BlockBehaviour.Properties.copy(Blocks.JUNGLE_PLANKS)));
    public static final RegistryObject<Block> DOUM_PALM_SLAB = BLOCKS.register("doum_palm_slab", () -> new SlabBlock(BlockBehaviour.Properties.copy(Blocks.JUNGLE_SLAB)));
    public static final RegistryObject<Block> DOUM_PALM_STAIRS = BLOCKS.register("doum_palm_stairs", () -> new StairBlock(() -> DOUM_PALM_PLANKS.get().defaultBlockState(), BlockBehaviour.Properties.copy(Blocks.JUNGLE_STAIRS)));
    public static final RegistryObject<Block> DOUM_PALM_LEAVES = BLOCKS.register("doum_palm_leaves", () -> new LeavesBlock(BlockBehaviour.Properties.copy(Blocks.JUNGLE_LEAVES).noOcclusion()));
    public static final RegistryObject<Block> DOUM_PALM_SAPLING = BLOCKS.register("doum_palm_sapling", () -> new com.benji.oasiso.common.block.DoumPalmSaplingBlock(new net.minecraft.world.level.block.grower.JungleTreeGrower(), BlockBehaviour.Properties.copy(Blocks.JUNGLE_SAPLING)));
    public static final RegistryObject<Block> DOUM_PALM_DOOR = BLOCKS.register("doum_palm_door", () -> new DoorBlock(BlockBehaviour.Properties.copy(Blocks.JUNGLE_DOOR), net.minecraft.world.level.block.state.properties.BlockSetType.JUNGLE));
    public static final RegistryObject<Block> DOUM_PALM_TRAPDOOR = BLOCKS.register("doum_palm_trapdoor", () -> new TrapDoorBlock(BlockBehaviour.Properties.copy(Blocks.JUNGLE_TRAPDOOR), net.minecraft.world.level.block.state.properties.BlockSetType.JUNGLE));
    public static final RegistryObject<Block> DOUM_PALM_FENCE = BLOCKS.register("doum_palm_fence", () -> new FenceBlock(BlockBehaviour.Properties.copy(Blocks.JUNGLE_FENCE)));
    public static final RegistryObject<Block> DOUM_PALM_FENCE_GATE = BLOCKS.register("doum_palm_fence_gate", () -> new FenceGateBlock(BlockBehaviour.Properties.copy(Blocks.JUNGLE_FENCE_GATE), DOUM_PALM_WOOD_TYPE));
    public static final RegistryObject<Block> DOUM_PALM_BUTTON = BLOCKS.register("doum_palm_button", () -> new ButtonBlock(BlockBehaviour.Properties.copy(Blocks.JUNGLE_BUTTON), net.minecraft.world.level.block.state.properties.BlockSetType.JUNGLE, 30, true));
    public static final RegistryObject<Block> DOUM_PALM_PRESSURE_PLATE = BLOCKS.register("doum_palm_pressure_plate", () -> new PressurePlateBlock(PressurePlateBlock.Sensitivity.EVERYTHING, BlockBehaviour.Properties.copy(Blocks.JUNGLE_PRESSURE_PLATE), net.minecraft.world.level.block.state.properties.BlockSetType.JUNGLE));
    public static final RegistryObject<Block> DOUM_PALM_SIGN = BLOCKS.register("doum_palm_sign", () -> new StandingSignBlock(BlockBehaviour.Properties.copy(Blocks.JUNGLE_SIGN), DOUM_PALM_WOOD_TYPE) {
        @Override
        public net.minecraft.world.level.block.entity.BlockEntity newBlockEntity(net.minecraft.core.BlockPos pos, net.minecraft.world.level.block.state.BlockState state) {
            return new com.benji.oasiso.common.block.entity.DoumPalmSignBlockEntity(pos, state);
        }
    });
    public static final RegistryObject<Block> DOUM_PALM_WALL_SIGN = BLOCKS.register("doum_palm_wall_sign", () -> new WallSignBlock(BlockBehaviour.Properties.copy(Blocks.JUNGLE_WALL_SIGN), DOUM_PALM_WOOD_TYPE) {
        @Override
        public net.minecraft.world.level.block.entity.BlockEntity newBlockEntity(net.minecraft.core.BlockPos pos, net.minecraft.world.level.block.state.BlockState state) {
            return new com.benji.oasiso.common.block.entity.DoumPalmSignBlockEntity(pos, state);
        }
    });
    public static final RegistryObject<Block> DOUM_PALM_HANGING_SIGN = BLOCKS.register("doum_palm_hanging_sign", () -> new CeilingHangingSignBlock(BlockBehaviour.Properties.copy(Blocks.JUNGLE_HANGING_SIGN), DOUM_PALM_WOOD_TYPE) {
        @Override
        public net.minecraft.world.level.block.entity.BlockEntity newBlockEntity(net.minecraft.core.BlockPos pos, net.minecraft.world.level.block.state.BlockState state) {
            return new com.benji.oasiso.common.block.entity.DoumPalmHangingSignBlockEntity(pos, state);
        }
    });
    public static final RegistryObject<Block> DOUM_PALM_WALL_HANGING_SIGN = BLOCKS.register("doum_palm_wall_hanging_sign", () -> new WallHangingSignBlock(BlockBehaviour.Properties.copy(Blocks.JUNGLE_WALL_HANGING_SIGN), DOUM_PALM_WOOD_TYPE) {
        @Override
        public net.minecraft.world.level.block.entity.BlockEntity newBlockEntity(net.minecraft.core.BlockPos pos, net.minecraft.world.level.block.state.BlockState state) {
            return new com.benji.oasiso.common.block.entity.DoumPalmHangingSignBlockEntity(pos, state);
        }
    });

    private ModBlocks() {
    }

    public static void register(IEventBus eventBus) {
        BLOCKS.register(eventBus);
    }
}
