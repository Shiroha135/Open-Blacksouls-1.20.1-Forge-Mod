package yuki135.blacksouls.client.render;

import yuki135.blacksouls.entity.EntityRabbitKnight;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

public final class RenderSourceRabbitKnight extends EntityRenderer<EntityRabbitKnight> {
    private final SwitchableMmdRenderer<EntityRabbitKnight> body;
    private final RenderRabbitKnight sprite;

    public RenderSourceRabbitKnight(EntityRendererProvider.Context context) {
        super(context);
        body = new SwitchableMmdRenderer<>(context, "黑兔骑士", RenderRabbitKnight::new, .32F);
        sprite = new RenderRabbitKnight(context);
        shadowRadius = .32F;
    }

    public static boolean usesSourceBody(EntityRabbitKnight entity) {
        return entity.isOriginalSourceManaged() && "$うさぎ騎士".equals(entity.originalRouteState().getString("CharacterName"));
    }

    @Override
    public void render(EntityRabbitKnight entity, float yaw, float partialTicks, PoseStack poseStack,
                       MultiBufferSource buffer, int light) {
        shadowStrength = entity.isInvisible() ? 0.0F : entity.getMmdRenderOpacity();
        if (usesSourceBody(entity)) body.render(entity, yaw, partialTicks, poseStack, buffer, light);
        else sprite.render(entity, yaw, partialTicks, poseStack, buffer, light);
    }

    @Override
    public ResourceLocation getTextureLocation(EntityRabbitKnight entity) {
        return usesSourceBody(entity) ? body.getTextureLocation(entity) : sprite.getTextureLocation(entity);
    }
}
