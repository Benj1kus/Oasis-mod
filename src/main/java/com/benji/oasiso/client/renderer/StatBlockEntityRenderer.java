package com.benji.oasiso.client.renderer;

import com.benji.oasiso.common.block.GenDecorateBlock;
import com.benji.oasiso.common.block.entity.StatBlockEntity;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.joml.Matrix4f;

public class StatBlockEntityRenderer implements BlockEntityRenderer<StatBlockEntity> {

    public StatBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
    }

    private static class GlowColor {
        final int r1, g1, b1;
        final int r2, g2, b2;

        GlowColor(int r1, int g1, int b1, int r2, int g2, int b2) {
            this.r1 = r1; this.g1 = g1; this.b1 = b1;
            this.r2 = r2; this.g2 = g2; this.b2 = b2;
        }
    }

    @Override
    public void render(StatBlockEntity blockEntity, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        ItemStack itemStack = blockEntity.getStoredItem();
        if (itemStack.isEmpty()) return;

        poseStack.pushPose();

        float heightOffset = 1.05f;
        float forwardOffset = 0.7f;
        float scale = 0.5f;

        Direction facing = blockEntity.getBlockState().getValue(GenDecorateBlock.FACING);
        poseStack.translate(0.5, heightOffset, 0.5);
        poseStack.translate(facing.getStepX() * forwardOffset, 0, facing.getStepZ() * forwardOffset);

        long time = blockEntity.getLevel().getGameTime();
        float hover = (float) Math.sin((time + partialTick) / 10.0f) * 0.05f;
        poseStack.translate(0, hover, 0);

        poseStack.pushPose();

        float rotation = (time + partialTick) * 3.0f;
        poseStack.translate(0, 0, 0);
        poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(rotation));
        poseStack.scale(scale, scale, scale);
        poseStack.mulPose(com.mojang.math.Axis.ZP.rotationDegrees(-45.0f));

        int glowLight = 15728880;

        Minecraft.getInstance().getItemRenderer().renderStatic(
                itemStack,
                ItemDisplayContext.FIXED,
                glowLight,
                packedOverlay,
                poseStack,
                bufferSource,
                blockEntity.getLevel(),
                0
        );

        poseStack.popPose();

        renderGlowAura(poseStack, itemStack, time, partialTick);

        poseStack.popPose();
    }

    private void renderGlowAura(PoseStack poseStack, ItemStack stack, long time, float partialTick) {
        GlowColor color = getGlowColor(stack);

        float pulse = 1.0f + (float) Math.sin((time + partialTick) / 6.0f) * 0.08f;
        float radius = 0.04f * pulse;
        float height = 0.7f * pulse;

        RenderSystem.disableCull();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        RenderSystem.depthMask(false);

        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder buffer = tesselator.getBuilder();
        Matrix4f matrix = poseStack.last().pose();

        buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);

        int rMid = (color.r1 + color.r2) / 2;
        int gMid = (color.g1 + color.g2) / 2;
        int bMid = (color.b1 + color.b2) / 2;

        int segments = 8;
        float rotationSpeed = -(time + partialTick) * 1.5f;

        for (int i = 0; i < segments; i++) {
            double angle1 = Math.toRadians((i * 360.0 / segments) + rotationSpeed);
            double angle2 = Math.toRadians(((i + 1) * 360.0 / segments) + rotationSpeed);

            float x1 = (float) Math.cos(angle1) * radius;
            float z1 = (float) Math.sin(angle1) * radius;
            float x2 = (float) Math.cos(angle2) * radius;
            float z2 = (float) Math.sin(angle2) * radius;

            // Нижний сегмент: от прозрачного (Color1) на дне к плотному среднему цвету (ColorMid) по центру
            addVertex(buffer, matrix, x1, -height, z1, color.r1, color.g1, color.b1, 0);
            addVertex(buffer, matrix, x2, -height, z2, color.r1, color.g1, color.b1, 0);
            addVertex(buffer, matrix, x2, 0, z2, rMid, gMid, bMid, 140);
            addVertex(buffer, matrix, x1, 0, z1, rMid, gMid, bMid, 140);

            // Верхний сегмент: от плотного среднего цвета (ColorMid) по центру к прозрачному (Color2) на вершине
            addVertex(buffer, matrix, x1, 0, z1, rMid, gMid, bMid, 140);
            addVertex(buffer, matrix, x2, 0, z2, rMid, gMid, bMid, 140);
            addVertex(buffer, matrix, x2, height, z2, color.r2, color.g2, color.b2, 0);
            addVertex(buffer, matrix, x1, height, z1, color.r2, color.g2, color.b2, 0);
        }

        tesselator.end();
        RenderSystem.depthMask(true);
        RenderSystem.enableCull();
    }

    private void addVertex(BufferBuilder buffer, Matrix4f matrix, float x, float y, float z, int r, int g, int b, int a) {
        buffer.vertex(matrix, x, y, z).color(r, g, b, a).endVertex();
    }

    // Определяем цвета в зависимости от предмета
// Определяем цвета в зависимости от предмета
    private GlowColor getGlowColor(ItemStack stack) {
        // ИСПОЛЬЗУЕМ FORGE РЕГИСТР ДЛЯ 1.20.1
        ResourceLocation id = net.minecraftforge.registries.ForgeRegistries.ITEMS.getKey(stack.getItem());

        // На всякий случай делаем проверку на null, чтобы избежать крашей
        if (id == null) {
            return new GlowColor(240, 240, 240, 120, 120, 130); // Бело-Серый по умолчанию
        }

        String path = id.getPath();
        String namespace = id.getNamespace();

        if (!namespace.equals("minecraft") && !namespace.equals("oasiso")) {
            return new GlowColor(255, 30, 30, 255, 180, 0); // Красно-Золотистый
        }

        // 2. Розово-Фиолетовый (Эпические / Легендарные / Незерит)
        if (stack.is(Items.GOLDEN_APPLE) ||
                stack.is(Items.ENCHANTED_GOLDEN_APPLE) ||
                stack.is(Items.GOLDEN_CARROT) ||
                stack.is(Items.TOTEM_OF_UNDYING) ||
                stack.is(Items.END_CRYSTAL) ||
                stack.is(Items.ELYTRA) ||
                stack.is(Items.TRIDENT) ||
                path.contains("netherite")) {
            return new GlowColor(230, 50, 230, 100, 30, 255); // Розово-Фиолетовый
        }

        // 3. Желто-Оранжевый (Золотые инструменты и броня)
        if (path.contains("golden_") || path.startsWith("gold_")) {
            // Исключаем золотые блоки и слитки, красим только экипировку/инструменты
            if (path.contains("sword") || path.contains("pickaxe") || path.contains("axe") ||
                    path.contains("shovel") || path.contains("hoe") || path.contains("helmet") ||
                    path.contains("chestplate") || path.contains("leggings") || path.contains("boots") ||
                    path.contains("horse_armor")) {
                return new GlowColor(255, 180, 0, 255, 80, 0); // Желто-Оранжевый
            }
        }

        // 4. Голубо-Синий (Алмазная броня и инструменты)
        if (path.contains("diamond")) {
            return new GlowColor(0, 230, 255, 0, 50, 255); // Голубо-Синий
        }

        // 5. По умолчанию: Бело-Серый (Обычные предметы)
        return new GlowColor(240, 240, 240, 120, 120, 130); // Бело-Серый
    }
}