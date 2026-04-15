package ru.pb.ahst.client;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.TheEndPortalRenderer;
import net.minecraft.resources.ResourceLocation;
import ru.pb.ahst.AHSkillTree;

public class StarSkyRenderer {
    private static final ResourceLocation STAR_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(AHSkillTree.MOD_ID, "textures/entity/star.png");
            //ResourceLocation.withDefaultNamespace("textures/entity/end_portal.png");

    public static final RenderType STAR_SKY = starSky();

    private static RenderType starSky() {
        return RenderType.create(
                "star_sky",
                DefaultVertexFormat.POSITION,
                VertexFormat.Mode.QUADS,
                1536,
                false,
                false,
                RenderType.CompositeState.builder()
                        .setShaderState(RenderType.RENDERTYPE_END_PORTAL_SHADER)
                        .setTextureState(new RenderType.MultiTextureStateShard.Builder()
                                .add(TheEndPortalRenderer.END_SKY_LOCATION, false, false)
                                .add(STAR_TEXTURE, false, false)
                                .build()
                        )
                        .createCompositeState(false));
    }
}
