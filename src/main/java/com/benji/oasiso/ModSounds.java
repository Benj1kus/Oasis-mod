package com.benji.oasiso;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModSounds {
    public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, Oasiso.MODID);

    public static final RegistryObject<SoundEvent> FLOWERY1 = registerSoundEvent("flowery1");
    public static final RegistryObject<SoundEvent> FLOWERY2 = registerSoundEvent("flowery2");
    public static final RegistryObject<SoundEvent> FLOWERY3 = registerSoundEvent("flowery3");
    public static final RegistryObject<SoundEvent> FLOWERY4 = registerSoundEvent("flowery4");
    public static final RegistryObject<SoundEvent> FLOWERY5 = registerSoundEvent("flowery5");
    public static final RegistryObject<SoundEvent> FLOWERY6 = registerSoundEvent("flowery6");
    public static final RegistryObject<SoundEvent> FLOWERY7 = registerSoundEvent("flowery7");
    public static final RegistryObject<SoundEvent> FLOWERY8 = registerSoundEvent("flowery8");
    public static final RegistryObject<SoundEvent> FLOWERY9 = registerSoundEvent("flowery9");
    public static final RegistryObject<SoundEvent> FLOWERY10 = registerSoundEvent("flowery10");
    public static final RegistryObject<SoundEvent> FLOWERY11 = registerSoundEvent("flowery11");
    public static final RegistryObject<SoundEvent> FLOWERY12 = registerSoundEvent("flowery12");
    public static final RegistryObject<SoundEvent> FLOWERY13 = registerSoundEvent("flowery13");

    private static RegistryObject<SoundEvent> registerSoundEvent(String name) {
        return SOUNDS.register(name, () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(Oasiso.MODID, name)));
    }
}
