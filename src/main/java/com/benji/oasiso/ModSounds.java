package com.benji.oasiso;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModSounds {
    public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, Oasiso.MODID);

    public static final RegistryObject<SoundEvent> MAGNETIC = registerSoundEvent("magnetic");


    //monki
    public static final RegistryObject<SoundEvent> MONKI1 = registerSoundEvent("monki1");
    public static final RegistryObject<SoundEvent> MONKI2 = registerSoundEvent("monki2");
    public static final RegistryObject<SoundEvent> MONKI3 = registerSoundEvent("monki3");
    public static final RegistryObject<SoundEvent> MONKI_HIT = registerSoundEvent("monki_hit");
    public static final RegistryObject<SoundEvent> MONKI_DEATH = registerSoundEvent("monki_death");

    //dasher
    public static final RegistryObject<SoundEvent> DASHER1 = registerSoundEvent("dasher1");
    public static final RegistryObject<SoundEvent> DASHER2 = registerSoundEvent("dasher2");
    public static final RegistryObject<SoundEvent> DASHER3 = registerSoundEvent("dasher3");
    public static final RegistryObject<SoundEvent> DASH = registerSoundEvent("dash");
    public static final RegistryObject<SoundEvent> DASHER_HIT = registerSoundEvent("dasher_hit");
    public static final RegistryObject<SoundEvent> DASHER_ATTACK = registerSoundEvent("dasher_attack");
    public static final RegistryObject<SoundEvent> DASHER_DEATH = registerSoundEvent("dasher_death");

    //titana
    public static final RegistryObject<SoundEvent> TITANA1 = registerSoundEvent("titana1");
    public static final RegistryObject<SoundEvent> TITANA2 = registerSoundEvent("titana2");
    public static final RegistryObject<SoundEvent> TITANA3 = registerSoundEvent("titana3");
    public static final RegistryObject<SoundEvent> TITANA_HIT = registerSoundEvent("titana_hit");
    public static final RegistryObject<SoundEvent> TITANA_STEP = registerSoundEvent("titana_step");
    public static final RegistryObject<SoundEvent> TITANA_ATTACK = registerSoundEvent("titana_attack");
    public static final RegistryObject<SoundEvent> TITANA_DEATH = registerSoundEvent("titana_death");

    //cacto
    public static final RegistryObject<SoundEvent> CACTO1 = registerSoundEvent("cacto1");
    public static final RegistryObject<SoundEvent> CACTO2 = registerSoundEvent("cacto2");
    public static final RegistryObject<SoundEvent> CACTO3 = registerSoundEvent("cacto3");
    public static final RegistryObject<SoundEvent> CACTO_HIT = registerSoundEvent("cacto_hit");
    public static final RegistryObject<SoundEvent> CACTO_DEATH = registerSoundEvent("cacto_death");


    public static final RegistryObject<SoundEvent> YES = registerSoundEvent("yes");

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
