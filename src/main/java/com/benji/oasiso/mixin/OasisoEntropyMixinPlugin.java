package com.benji.oasiso.mixin;

import net.minecraftforge.fml.loading.FMLLoader;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.List;
import java.util.Set;

public final class OasisoEntropyMixinPlugin implements IMixinConfigPlugin {

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        var mods = FMLLoader.getLoadingModList();
        if (mods == null) return false;

        boolean hasEmbeddium = mods.getModFileById("embeddium") != null;
        if (mixinClassName.contains("EmbeddiumEntropyBlockMixin")) {
            return hasEmbeddium;
        }
        if (mixinClassName.contains("EntropyBlockModelMixin")) {
            return !hasEmbeddium;
        }

        return true;
    }

    @Override public void onLoad(String mixinPackage) {}
    @Override public String getRefMapperConfig() { return null; }
    @Override public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {}
    @Override public List<String> getMixins() { return null; }
    @Override public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {}
    @Override public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {}
}