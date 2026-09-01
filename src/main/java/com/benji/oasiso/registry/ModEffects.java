package com.benji.oasiso.registry;

import com.benji.oasiso.Oasiso;
import com.benji.oasiso.common.effect.BombulBuffEffect;
import com.benji.oasiso.common.effect.ChaosChamberEffect;
import com.benji.oasiso.common.effect.EntropyEffect;
import net.minecraft.world.effect.MobEffect;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import com.benji.oasiso.common.effect.SmellOfSinEffect;

public final class ModEffects {

    public static final DeferredRegister<MobEffect> MOB_EFFECTS = DeferredRegister.create(ForgeRegistries.MOB_EFFECTS, Oasiso.MODID);

    public static final RegistryObject<MobEffect> ENTROPY_EFFECT = MOB_EFFECTS.register("entropy", EntropyEffect::new);
    public static final RegistryObject<MobEffect> BOMBUL_BUFF_EFFECT = MOB_EFFECTS.register("bombul_buff", BombulBuffEffect::new);
    public static final RegistryObject<MobEffect> CHAOS_CHAMBER_EFFECT = MOB_EFFECTS.register("chaos_chamber", ChaosChamberEffect::new);
    public static final RegistryObject<MobEffect> SMELL_OF_SIN_EFFECT = MOB_EFFECTS.register("smell_of_sin", SmellOfSinEffect::new);
    private ModEffects() {
    }

    public static void register(IEventBus eventBus) {
        MOB_EFFECTS.register(eventBus);
    }
}
