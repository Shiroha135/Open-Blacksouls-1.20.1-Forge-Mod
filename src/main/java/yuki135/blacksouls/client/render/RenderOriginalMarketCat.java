package yuki135.blacksouls.client.render;

import yuki135.blacksouls.entity.EntityOriginalMarketCat;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.ResourceLocation;

public final class RenderOriginalMarketCat extends EntityRenderer<EntityOriginalMarketCat> {
    public RenderOriginalMarketCat(EntityRendererProvider.Context context) {
        super(context);
        shadowRadius = 0.0F;
        shadowStrength = 0.0F;
    }

    @Override
    public boolean shouldRender(EntityOriginalMarketCat entity, Frustum frustum, double x, double y, double z) {
        return false;
    }

    @Override
    public void render(EntityOriginalMarketCat entity, float yaw, float partialTicks,
                       PoseStack poseStack, MultiBufferSource buffer, int light) {
    }

    @Override
    public ResourceLocation getTextureLocation(EntityOriginalMarketCat entity) {
        return TextureAtlas.LOCATION_BLOCKS;
    }
}
