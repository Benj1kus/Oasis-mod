package com.benji.oasiso;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModSounds {
    public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, Oasiso.MODID);

    public static final RegistryObject<SoundEvent> MAGNETIC = registerSoundEvent("magnetic");
    public static final RegistryObject<SoundEvent> HONK = registerSoundEvent("honk");
    public static final RegistryObject<SoundEvent> TOWER_PLACE = registerSoundEvent("tower_place");
    public static final RegistryObject<SoundEvent> CAST = registerSoundEvent("cast");

    public static final RegistryObject<SoundEvent> PALADIN_VOICE = registerSoundEvent("paladin_voice");
    public static final RegistryObject<SoundEvent> OSIRIS_VOICE = registerSoundEvent("osiris_voice");


    public static final RegistryObject<SoundEvent> GASTER = registerSoundEvent("gaster");
    public static final RegistryObject<SoundEvent> SANDSTORM = registerSoundEvent("sandstorm");

    //AZUMAAL

    public static final RegistryObject<SoundEvent> AZUMAAL_IDLE1 = registerSoundEvent("azumaal_idle1");
    public static final RegistryObject<SoundEvent> AZUMAAL_IDLE2 = registerSoundEvent("azumaal_idle2");
    public static final RegistryObject<SoundEvent> AZUMAAL_IDLE3 = registerSoundEvent("azumaal_idle3");
    public static final RegistryObject<SoundEvent> AZUMAAL_HIT = registerSoundEvent("azumaal_hit");
    public static final RegistryObject<SoundEvent> AZUMAAL_DEATH = registerSoundEvent("azumaal_death");
    public static final RegistryObject<SoundEvent> SUMMON_CAST = registerSoundEvent("summon_cast");
    public static final RegistryObject<SoundEvent> EYELID_SHOT = registerSoundEvent("eyelid_shot");
    public static final RegistryObject<SoundEvent> FRAME = registerSoundEvent("frame");
    public static final RegistryObject<SoundEvent> SCISSORS = registerSoundEvent("scissors");
    public static final RegistryObject<SoundEvent> BOMB_SPAWN = registerSoundEvent("bomb_spawn");
    public static final RegistryObject<SoundEvent> TIMER = registerSoundEvent("timer");
    public static final RegistryObject<SoundEvent> EYE_ATTACK = registerSoundEvent("eye_attack");
    public static final RegistryObject<SoundEvent> SWING = registerSoundEvent("swing");
    public static final RegistryObject<SoundEvent> CLONES = registerSoundEvent("clones");
    public static final RegistryObject<SoundEvent> BOSS_SPAWN = registerSoundEvent("boss_spawn");
    public static final RegistryObject<SoundEvent> PORTAL_OPEN = registerSoundEvent("portal_open");
    public static final RegistryObject<SoundEvent> AZUMAAL_SONG = registerSoundEvent("azumaal_song");


    public static final RegistryObject<SoundEvent> AZUMAAL_LOOPED = registerSoundEvent("azumaal_looped");


    //DIMENSION
    public static final RegistryObject<SoundEvent> ECHO1 = registerSoundEvent("echo1");
    public static final RegistryObject<SoundEvent> ECHO2 = registerSoundEvent("echo2");
    public static final RegistryObject<SoundEvent> ECHO3 = registerSoundEvent("echo3");

    public static final RegistryObject<SoundEvent> DIMENSION_AMBIENT = registerSoundEvent("dimension_ambient");
    public static final RegistryObject<SoundEvent> GOD_SCREAM = registerSoundEvent("god_scream");
    public static final RegistryObject<SoundEvent> ECHO_STARS = registerSoundEvent("echo_stars");
    public static final RegistryObject<SoundEvent> CHAOS_DEATH = registerSoundEvent("chaos_death");
    public static final RegistryObject<SoundEvent> CHAOS_LIFE = registerSoundEvent("chaos_life");

    //BOMBUL

    public static final RegistryObject<SoundEvent> BOMBUL_HURT = registerSoundEvent("bombul_hurt");
    public static final RegistryObject<SoundEvent> BOMBUL_IDLE = registerSoundEvent("bombul_idle");
    public static final RegistryObject<SoundEvent> SUPPORT = registerSoundEvent("support");

    public static final RegistryObject<SoundEvent> VOICES = registerSoundEvent("voices");
    public static final RegistryObject<SoundEvent> WHITE_FLASH = registerSoundEvent("white_flash");

    public static final RegistryObject<SoundEvent> ENTROPY1 = registerSoundEvent("entropy1");
    public static final RegistryObject<SoundEvent> ENTROPY2 = registerSoundEvent("entropy2");
    public static final RegistryObject<SoundEvent> ENTROPY3 = registerSoundEvent("entropy3");

    //caser
    public static final RegistryObject<SoundEvent> CASER1 = registerSoundEvent("caser1");
    public static final RegistryObject<SoundEvent> CASER2 = registerSoundEvent("caser2");
    public static final RegistryObject<SoundEvent> CASER3 = registerSoundEvent("caser3");
    public static final RegistryObject<SoundEvent> CASER_HIT = registerSoundEvent("caser_hit");
    public static final RegistryObject<SoundEvent> CASER_DEFAULT= registerSoundEvent("caser_default");
    public static final RegistryObject<SoundEvent> CASER_SUCCESS= registerSoundEvent("caser_success");
    public static final RegistryObject<SoundEvent> CASER_SPIN= registerSoundEvent("caser_spin");

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

    //sandgolem

    public static final RegistryObject<SoundEvent> SANDGOLEM1 = registerSoundEvent("sandgolem1");
    public static final RegistryObject<SoundEvent> SANDGOLEM2 = registerSoundEvent("sandgolem2");
    public static final RegistryObject<SoundEvent> SANDGOLEM3 = registerSoundEvent("sandgolem3");
    public static final RegistryObject<SoundEvent> SANDGOLEM_HIT = registerSoundEvent("sandgolem_hit");
    public static final RegistryObject<SoundEvent> SANDGOLEM_STEP = registerSoundEvent("sandgolem_step");


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
        return SOUNDS.register(name, () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(Oasiso.MODID, name)));
    }
}
