package com.benji.oasiso.registry;

import com.benji.oasiso.Oasiso;
import com.benji.oasiso.common.block.entity.*;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModBlockEntities {

    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, Oasiso.MODID);

    public static final RegistryObject<BlockEntityType<NephritisLampBlockEntity>> NEPHRITIS_LAMP_BE = BLOCK_ENTITIES.register("nephritis_lamp", () -> BlockEntityType.Builder.of(NephritisLampBlockEntity::new, ModBlocks.NEPHRITIS_LAMP.get()).build(null));
    public static final RegistryObject<BlockEntityType<StormTotemBlockEntity>> STORM_TOTEM_BLOCK_ENTITY = BLOCK_ENTITIES.register("storm_totem", () -> BlockEntityType.Builder.of(StormTotemBlockEntity::new, ModBlocks.STORM_TOTEM.get()).build(null));
    public static final RegistryObject<BlockEntityType<StatBlockEntity>> STAT_BE = BLOCK_ENTITIES.register("stat", () -> BlockEntityType.Builder.of(StatBlockEntity::new, ModBlocks.STAT.get()).build(null));
    public static final RegistryObject<BlockEntityType<StatueBlockEntity>> STATUE_BE = BLOCK_ENTITIES.register("statue", () -> BlockEntityType.Builder.of(StatueBlockEntity::new, ModBlocks.MONKI_STATUE.get(), ModBlocks.DASHER_STATUE.get(), ModBlocks.TITANA_STATUE.get()).build(null));
    public static final RegistryObject<BlockEntityType<com.benji.oasiso.common.block.entity.LieBlockEntity>> LIE_BLOCK_BE = BLOCK_ENTITIES.register("lie_block", () -> BlockEntityType.Builder.of(com.benji.oasiso.common.block.entity.LieBlockEntity::new, ModBlocks.LIE_BLOCK.get()).build(null));
    public static final RegistryObject<BlockEntityType<ChaosAltarBlockEntity>> CHAOS_ALTAR_BE = BLOCK_ENTITIES.register("chaos_altar", () -> BlockEntityType.Builder.of(ChaosAltarBlockEntity::new, ModBlocks.CHAOS_ALTAR.get()).build(null));
    public static final RegistryObject<BlockEntityType<SandedChestBlockEntity>> SANDED_CHEST_BE = BLOCK_ENTITIES.register("sanded_chest", () -> BlockEntityType.Builder.of(SandedChestBlockEntity::new, ModBlocks.SANDED_CHEST.get()).build(null));
    public static final RegistryObject<BlockEntityType<com.benji.oasiso.common.block.entity.DoumPalmSignBlockEntity>> DOUM_PALM_SIGN_BE = BLOCK_ENTITIES.register("doum_palm_sign", () -> BlockEntityType.Builder.of(com.benji.oasiso.common.block.entity.DoumPalmSignBlockEntity::new, ModBlocks.DOUM_PALM_SIGN.get(), ModBlocks.DOUM_PALM_WALL_SIGN.get()).build(null));
    public static final RegistryObject<BlockEntityType<com.benji.oasiso.common.block.entity.DoumPalmHangingSignBlockEntity>> DOUM_PALM_HANGING_SIGN_BE = BLOCK_ENTITIES.register("doum_palm_hanging_sign", () -> BlockEntityType.Builder.of(com.benji.oasiso.common.block.entity.DoumPalmHangingSignBlockEntity::new, ModBlocks.DOUM_PALM_HANGING_SIGN.get(), ModBlocks.DOUM_PALM_WALL_HANGING_SIGN.get()).build(null));

    private ModBlockEntities() {
    }

    public static void register(IEventBus eventBus) {
        BLOCK_ENTITIES.register(eventBus);
    }
}
