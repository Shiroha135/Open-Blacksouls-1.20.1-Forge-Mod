package yuki135.blacksouls.client.render;

import yuki135.blacksouls.entity.EntityOpeningActor;
import yuki135.blacksouls.story.OriginalOpeningScenes;
import yuki135.blacksouls.util.OriginalOpeningActorSprites;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;

public final class RenderOpeningActor extends EntityRenderer<EntityOpeningActor> {
    public RenderOpeningActor(EntityRendererProvider.Context context) { super(context);shadowRadius=0.25F; }

    @Override public void render(EntityOpeningActor actor,float yaw,float partialTicks,PoseStack stack,MultiBufferSource buffer,int light) {
        var player=Minecraft.getInstance().player;
        if (player==null || !player.getUUID().equals(actor.ownerId()) || actor.isInvisible() || actor.originId()<0 || actor.originId()>9 || !OriginalOpeningScenes.DIMENSION.equals(actor.level().dimension())) return;
        var sprite=OriginalOpeningActorSprites.get(actor.originId());float height=1.8F,half=height*sprite.frameWidth()/sprite.frameHeight()*0.5F;
        stack.pushPose();stack.mulPose(entityRenderDispatcher.cameraOrientation());stack.mulPose(Axis.YP.rotationDegrees(180));
        var consumer=buffer.getBuffer(RenderType.entityTranslucent(sprite.texture()));var pose=stack.last();
        vertex(consumer,pose,light,-half,0,sprite.u0(),sprite.v1());vertex(consumer,pose,light,half,0,sprite.u1(),sprite.v1());
        vertex(consumer,pose,light,half,height,sprite.u1(),sprite.v0());vertex(consumer,pose,light,-half,height,sprite.u0(),sprite.v0());stack.popPose();
    }

    private static void vertex(VertexConsumer consumer,PoseStack.Pose pose,int light,float x,float y,float u,float v) {
        consumer.vertex(pose.pose(),x,y,0).color(255,255,255,255).uv(u,v).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(pose.normal(),0,1,0).endVertex();
    }

    @Override public ResourceLocation getTextureLocation(EntityOpeningActor actor) { return OriginalOpeningActorSprites.get(actor.originId()<0 || actor.originId()>9?0:actor.originId()).texture(); }
}
