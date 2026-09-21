package com.benji.oasiso.client.particle;

import com.benji.oasiso.Oasiso;
import com.benji.oasiso.registry.ModBlocks;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.ParticleStatus;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.List;

@Mod.EventBusSubscriber(modid = Oasiso.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class KarakSandDustClient {
    private static final ResourceLocation SLAP = ResourceLocation.fromNamespaceAndPath(Oasiso.MODID, "textures/gui/dust_slap.png");
    private static final int MAX_STAINS = 5;
    private static final int SEARCH_RADIUS = 7;
    private static final List<BlockPos> CEILINGS = new ArrayList<>();
    private static final List<Stain> STAINS = new ArrayList<>();
    private static ClientLevel world;
    private static int ticks, slapCooldown;

    private static boolean sand(BlockState state) {
        return state.is(ModBlocks.KR_SAND.get()) || state.is(ModBlocks.KR_GRASS.get());
    }

    private static boolean exposed(BlockPos pos) {
        return world.hasChunkAt(pos) && sand(world.getBlockState(pos)) && world.getBlockState(pos.below()).isAir();
    }

    @SubscribeEvent
    public static void tick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getInstance();
        if (world != mc.level) {
            world = mc.level;
            CEILINGS.clear();
            STAINS.clear();
            ticks = slapCooldown = 0;
            KarakSandDustParticle.Provider.reset(world);
        }
        if (world == null || mc.player == null || mc.isPaused()) return;
        STAINS.removeIf(stain -> ++stain.age >= stain.life);
        Vec3 camera = mc.gameRenderer.getMainCamera().getPosition();
        if (ticks++ % 10 == 0) scan(BlockPos.containing(camera));
        ParticleStatus quality = mc.options.particles().get();
        if (quality != ParticleStatus.MINIMAL && !CEILINGS.isEmpty() && ticks % (quality == ParticleStatus.DECREASED ? 4 : 2) == 0) {
            BlockPos p = CEILINGS.get(world.random.nextInt(CEILINGS.size()));
            if (exposed(p))
                world.addParticle(Oasiso.KR_SAND_DUST.get(), p.getX() + .1 + world.random.nextDouble() * .8, p.getY() - .03, p.getZ() + .1 + world.random.nextDouble() * .8, 0, 0, 0);
        }
        BlockPos overhead = findOverhead(mc.player.getEyePosition());
        if (overhead == null || mc.player.isSpectator()) {
            slapCooldown = 0;
            return;
        }
        if (quality == ParticleStatus.MINIMAL) return;
        if (slapCooldown > 0) {
            slapCooldown--;
            return;
        }
        slapCooldown = 12 + world.random.nextInt(17);
        if (STAINS.size() < MAX_STAINS) STAINS.add(new Stain(world));
        world.addParticle(Oasiso.KR_SAND_DUST.get(), overhead.getX() + .2 + world.random.nextDouble() * .6, overhead.getY() - .03, overhead.getZ() + .2 + world.random.nextDouble() * .6, 0, 0, 0);
    }

    private static BlockPos findOverhead(Vec3 eye) {
        BlockPos base = BlockPos.containing(eye);
        for (int i = 1; i <= 5; i++) {
            BlockPos p = base.above(i);
            if (exposed(p)) return p;
            if (!world.getBlockState(p).isAir()) return null;
        }
        return null;
    }

    private static void scan(BlockPos center) {
        CEILINGS.clear();
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        for (int x = -SEARCH_RADIUS; x <= SEARCH_RADIUS; x++)
            for (int z = -SEARCH_RADIUS; z <= SEARCH_RADIUS; z++) {
                if (x * x + z * z > SEARCH_RADIUS * SEARCH_RADIUS) continue;
                for (int y = -2; y <= 8; y++) {
                    p.set(center.getX() + x, center.getY() + y, center.getZ() + z);
                    if (exposed(p)) CEILINGS.add(p.immutable());
                }
            }
    }

    @SubscribeEvent
    public static void render(RenderGuiEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.level != world || mc.options.hideGui || mc.screen != null || STAINS.isEmpty())
            return;
        GuiGraphics g = event.getGuiGraphics();
        g.flush();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        float[] oldColor = RenderSystem.getShaderColor().clone();
        try {
            for (Stain stain : STAINS) {
                float age = stain.age + (mc.isPaused() ? 0 : event.getPartialTick());
                float t = Mth.clamp(age / stain.life, 0, 1);
                float fade = 1 - t * t * (3 - 2 * t);
                RenderSystem.setShaderColor(1, 1, 1, .78F * fade);
                int size = Math.max(16, Math.round(Math.min(g.guiWidth(), g.guiHeight()) * stain.size));
                g.pose().pushPose();
                try {
                    g.pose().translate(stain.x * g.guiWidth(), stain.y * g.guiHeight(), 0);
                    g.pose().mulPose(Axis.ZP.rotationDegrees(stain.angle));
                    g.blit(SLAP, -size / 2, -size / 2, 0.0F, 0.0F, size, size, size, size);
                } finally {
                    g.pose().popPose();
                }
            }
        } finally {
            RenderSystem.setShaderColor(oldColor[0], oldColor[1], oldColor[2], oldColor[3]);
            RenderSystem.disableBlend();
            RenderSystem.depthMask(true);
            RenderSystem.enableDepthTest();
        }
    }

    private static final class Stain {
        final float x, y, size, angle;
        final int life;
        int age;

        Stain(ClientLevel level) {
            x = .10F + level.random.nextFloat() * .80F;
            y = .10F + level.random.nextFloat() * .80F;
            size = .17F + level.random.nextFloat() * .15F;
            angle = level.random.nextFloat() * 360;
            life = 55 + level.random.nextInt(26);
        }
    }

    private KarakSandDustClient() {
    }
}
