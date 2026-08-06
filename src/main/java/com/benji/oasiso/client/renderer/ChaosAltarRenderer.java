package com.benji.oasiso.client.renderer;

import com.benji.oasiso.Oasiso;
import com.benji.oasiso.client.model.ChaosAltarModel;
import com.benji.oasiso.common.block.entity.ChaosAltarBlockEntity;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.renderer.GeoBlockRenderer;
import software.bernie.geckolib.renderer.layer.AutoGlowingGeoLayer;

public class ChaosAltarRenderer
        extends GeoBlockRenderer<ChaosAltarBlockEntity> {

    private static final ResourceLocation
            EMISSIVE_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(
                    Oasiso.MODID,
                    "textures/block/emissive/"
                            + "chaos_altar_emissive.png"
            );

    public ChaosAltarRenderer(
            BlockEntityRendererProvider.Context context
    ) {
        super(
                new ChaosAltarModel()
        );

        this.addRenderLayer(
                new AutoGlowingGeoLayer
                        <ChaosAltarBlockEntity>(this) {

                    @Override
                    protected RenderType getRenderType(
                            ChaosAltarBlockEntity animatable
                    ) {
                        return RenderType.eyes(
                                EMISSIVE_TEXTURE
                        );
                    }
                }
        );
    }
}