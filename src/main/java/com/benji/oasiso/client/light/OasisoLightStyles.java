package com.benji.oasiso.client.light;

import com.benji.oasiso.Oasiso;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.registries.ForgeRegistries;
import com.benji.oasiso.config.OsirisRealmConfig;

import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;

import net.minecraft.world.entity.EntityType;

public final class OasisoLightStyles {
    public static int maxLights() {
        return OsirisRealmConfig.MAX_LIGHTS.get();
    }
    public static int searchRadius() {
        return OsirisRealmConfig.SEARCH_RADIUS.get();
    }

    public static final int SHADOW_SIZE = 16;
    public static final int RAYS_PER_TICK = 256;

    public static boolean isEnabled() {
        try {
            return OsirisRealmConfig.CUSTOM_LIGHTING_ENABLED != null && OsirisRealmConfig.CUSTOM_LIGHTING_ENABLED.get();
        } catch (Exception e) {
            return true;
        }
    }

    //ENTROPY style (my style you can add your here or in register)
    public static final Style ENTROPY = new Style(0x2754F5, 0xDD8BEA, 5.5F, 1.80F, 2.3F, 0.18F, 0.24F, 1.55F);

    private static final Map<ResourceLocation, Style> STYLES = new HashMap<>();

    private static final Map<ResourceLocation, Map<String, Style>> BONE_STYLES = new HashMap<>();
    private static final Map<EntityType<?>, Map<String, Style>> BONE_CACHE = new IdentityHashMap<>();
    public static final Style COWRIT_GLOW = new Style(0xFF954A, 0xFDFFA8, 2F, 2.0F, 2F, 0.15F, 0.22F, 0F);

    static {
        registerBone("cowrit", "bott", COWRIT_GLOW);
        // registering blocks:
        register("entropy_lantern", ENTROPY);
        register("apol_altar", ENTROPY);
        register("not_azumalit_crystal", new Style(0xE817FF, 0xFF87F4, 5.0F, 1.8F, 2.0F, 0.15F, 0.20F, 0.5F));
        register("azumalit_crystal", new Style(0xE817FF, 0xFF87F4, 5.0F, 1.8F, 2.0F, 0.15F, 0.20F, 0.5F));
        register("nephritis_lamp", new Style(0x0FFFBB, 0x0FFFEB, 5.0F, 1.8F, 2.0F, 0.15F, 0.20F, 1.5F));
        register("gluminus", new Style(0xFFCA38, 0xFA6D2D, 5.0F, 1.8F, 2.0F, 0.15F, 0.20F, 1F));
        // register("karak_lamp", new Style(0x88FFFF, 0xD899FF,5.0F, 0.8F, 2.0F, 0.15F, 0.20F, 0.5F)); btw this is example tho
    }

    private OasisoLightStyles() {
    }

    public static void register(String blockId, Style style) {
        ResourceLocation id = blockId.contains(":") ? ResourceLocation.parse(blockId) : ResourceLocation.fromNamespaceAndPath(Oasiso.MODID, blockId);
        STYLES.put(id, style);
    }


    public static void registerBone(String entityId, String bone, Style style) {
        ResourceLocation id = entityId.contains(":") ? ResourceLocation.parse(entityId) : ResourceLocation.fromNamespaceAndPath(Oasiso.MODID, entityId);
        BONE_STYLES.computeIfAbsent(id, ignored -> new HashMap<>()).put(bone, style);
        BONE_CACHE.clear();
    }

    static Style boneStyle(EntityType<?> type, String name) {
        return BONE_CACHE.computeIfAbsent(type, key -> BONE_STYLES.getOrDefault(ForgeRegistries.ENTITY_TYPES.getKey(key), Map.of())).get(name);
    }

    static Map<Block, Style> resolved() {
        Map<Block, Style> result = new IdentityHashMap<>();
        STYLES.forEach((id, style) -> {
            if (ForgeRegistries.BLOCKS.containsKey(id)) result.put(ForgeRegistries.BLOCKS.getValue(id), style);
        });
        return result;
    }

    public static void register(String blockId, Style style, float height) {
        register(blockId, new Style(style.color(), style.accent(), style.radius(), style.strength(), style.fogRadius(), style.fogDensity(), style.halo(), height));
    }

    public record Style(int color, int accent, float radius, float strength, float fogRadius, float fogDensity,
                        float halo, float height) {
        public Style {
            //LIMITS!!!! :c
            if (!Float.isFinite(radius) || radius < 0.5F || radius > 8.0F || !Float.isFinite(strength) || strength < 0 || strength > 2 || !Float.isFinite(fogRadius) || fogRadius < 0 || fogRadius > radius || !Float.isFinite(fogDensity) || fogDensity < 0 || fogDensity > 0.6F || !Float.isFinite(halo) || halo < 0 || halo > 1 || !Float.isFinite(height) || height < -3 || height > 3)
                throw new IllegalArgumentException("YOU STUPID IDIOT");
        }
    }
}
