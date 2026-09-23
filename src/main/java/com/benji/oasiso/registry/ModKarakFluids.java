package com.benji.oasiso.registry;

import com.benji.oasiso.Oasiso;
import com.benji.oasiso.common.fluid.EntropyFluidType;
import com.benji.oasiso.common.fluid.KarakFluidType;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fluids.FluidType;
import net.minecraftforge.fluids.ForgeFlowingFluid;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModKarakFluids {
    public static final DeferredRegister<FluidType> FLUID_TYPES = DeferredRegister.create(ForgeRegistries.Keys.FLUID_TYPES, Oasiso.MODID);
    public static final DeferredRegister<Fluid> FLUIDS = DeferredRegister.create(ForgeRegistries.FLUIDS, Oasiso.MODID);

    public static final RegistryObject<FluidType> KR_WATER_TYPE = FLUID_TYPES.register("kr_water", KarakFluidType::new);
    public static final RegistryObject<FlowingFluid> KR_WATER = FLUIDS.register("kr_water", () -> new ForgeFlowingFluid.Source(krWaterProperties()));
    public static final RegistryObject<FlowingFluid> FLOWING_KR_WATER = FLUIDS.register("flowing_kr_water", () -> new ForgeFlowingFluid.Flowing(krWaterProperties()));

    public static final RegistryObject<FluidType> ENTROPY_WATER_TYPE = FLUID_TYPES.register("entropy_water", EntropyFluidType::new);
    public static final RegistryObject<FlowingFluid> ENTROPY_WATER = FLUIDS.register("entropy_water", () -> new ForgeFlowingFluid.Source(entropyWaterProperties()));
    public static final RegistryObject<FlowingFluid> FLOWING_ENTROPY_WATER = FLUIDS.register("flowing_entropy_water", () -> new ForgeFlowingFluid.Flowing(entropyWaterProperties()));

    private static ForgeFlowingFluid.Properties krWaterProperties() {
        return new ForgeFlowingFluid.Properties(KR_WATER_TYPE, KR_WATER, FLOWING_KR_WATER).bucket(ModItems.KR_WATER_BUCKET).block(ModBlocks.KR_WATER).slopeFindDistance(4).levelDecreasePerBlock(1).tickRate(5).explosionResistance(100.0F);
    }

    private static ForgeFlowingFluid.Properties entropyWaterProperties() {
        return new ForgeFlowingFluid.Properties(ENTROPY_WATER_TYPE, ENTROPY_WATER, FLOWING_ENTROPY_WATER).bucket(ModItems.ENTROPY_WATER_BUCKET).block(ModBlocks.ENTROPY_WATER).slopeFindDistance(4).levelDecreasePerBlock(2).tickRate(20).explosionResistance(100.0F);
    }

    public static void register(IEventBus bus) {
        FLUID_TYPES.register(bus);
        FLUIDS.register(bus);
    }

    private ModKarakFluids() {
    }
}