package yuki135.blacksouls.client.render;

import yuki135.blacksouls.entity.EntityOpeningActor;
import yuki135.blacksouls.client.OriginalOpeningController;
import yuki135.blacksouls.story.OriginalOpeningScenes;
import yuki135.blacksouls.util.OriginalOpeningActorSprites;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

public final class RenderOpeningActor extends EntityRenderer<EntityOpeningActor> {
    public RenderOpeningActor(EntityRendererProvider.Context context) { super(context);shadowRadius=0.25F; }

    @Override public boolean shouldRender(EntityOpeningActor actor,Frustum frustum,double cameraX,double cameraY,double cameraZ) {
        var player=Minecraft.getInstance().player;
        return player!=null && player.getUUID().equals(actor.ownerId()) && OriginalOpeningController.professionVisible(actor.originId())
                && super.shouldRender(actor,frustum,cameraX,cameraY,cameraZ);
    }

    @Override public void render(EntityOpeningActor actor,float yaw,float partialTicks,PoseStack stack,MultiBufferSource buffer,int light) {
        var player=Minecraft.getInstance().player;
        if (player==null || !player.getUUID().equals(actor.ownerId()) || actor.isInvisible() || actor.originId()<0 || actor.originId()>9 || !OriginalOpeningScenes.DIMENSION.equals(actor.level().dimension())) return;
        if (!OriginalOpeningController.professionVisible(actor.originId())) return;
        var sprite=OriginalOpeningActorSprites.get(actor.originId());
        float facing=net.minecraft.util.Mth.rotLerp(partialTicks,actor.yRotO,actor.getYRot());
        facing=yuki135.blacksouls.client.OriginalRoleplayView.actorYaw(actor,facing);
        int row=actor.originId()>0?0:OriginalCharacterSprite.facingRow(facing,actor.getPosition(partialTicks),entityRenderDispatcher.camera.getPosition());
        OriginalCharacterSprite.render(sprite,row,1,1.8F,entityRenderDispatcher.cameraOrientation(),stack,buffer,light);
    }

    @Override public ResourceLocation getTextureLocation(EntityOpeningActor actor) { return OriginalOpeningActorSprites.get(actor.originId()<0 || actor.originId()>9?0:actor.originId()).texture(); }
}
