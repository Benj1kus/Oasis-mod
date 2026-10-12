package com.benji.oasiso.registry;

import com.benji.oasiso.Oasiso;
import com.benji.oasiso.common.block.EntropyWellPartBlock;
import com.benji.oasiso.common.block.entity.EntropyWellBlockEntity;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.*;

public final class EntropyWellRegistry {
    private static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, Oasiso.MODID);
    private static final DeferredRegister<BlockEntityType<?>> ENTITIES = DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, Oasiso.MODID);
    public static final RegistryObject<Block> PART = BLOCKS.register("entropy_well_part", () -> new EntropyWellPartBlock(BlockBehaviour.Properties.copy(Blocks.STONE).strength(100F).requiresCorrectToolForDrops().noOcclusion().noLootTable()));
    public static final RegistryObject<BlockEntityType<EntropyWellBlockEntity>> WELL = ENTITIES.register("entropy_well", () -> BlockEntityType.Builder.of(EntropyWellBlockEntity::new, ModBlocks.ENTROPY_WELL.get()).build(null));

    public static void register(IEventBus bus) {
        BLOCKS.register(bus);
        ENTITIES.register(bus);
    }

    private EntropyWellRegistry() {
    }
}
