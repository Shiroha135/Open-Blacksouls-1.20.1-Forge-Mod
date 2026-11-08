package yuki135.blacksouls.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import yuki135.blacksouls.util.OriginalOpeningActorSprites.Sprite;

public final class OriginalCharacterSprite {
    private OriginalCharacterSprite() { }

    public static int facingRow(float yaw,Vec3 position,Vec3 camera) {
        double view=Math.toDegrees(Math.atan2(-(camera.x-position.x),camera.z-position.z));
        double angle=Mth.wrapDegrees((float)(view-yaw));
        if(Math.abs(angle)<=45)return 0;
        if(Math.abs(angle)>=135)return 3;
        return angle>0?2:1;
    }

    public static void render(Sprite sprite,int direction,int pattern,float height,Quaternionf camera,PoseStack poses,MultiBufferSource buffers,int light) {
        int column=sprite.column()/3*3+pattern,row=sprite.row()/4*4+direction;
        float u0=column/(float)sprite.columns(),u1=(column+1)/(float)sprite.columns();
        float v0=row/(float)sprite.rows(),v1=(row+1)/(float)sprite.rows();
        float half=height*sprite.frameWidth()/sprite.frameHeight()*.5F;
        poses.pushPose();faceCamera(poses,camera);
        var output=buffers.getBuffer(RenderType.entityCutoutNoCull(sprite.texture()));var pose=poses.last();
        float[] xs={-half,half,half,-half},ys={0,0,height,height},us={u0,u1,u1,u0},vs={v1,v1,v0,v0};
        for(int i=0;i<4;i++)output.vertex(pose.pose(),xs[i],ys[i],0).color(255,255,255,255).uv(us[i],vs[i]).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(pose.normal(),0,0,1).endVertex();
        poses.popPose();
    }

    public static void faceCamera(PoseStack poses,Quaternionf camera) {
        float yaw=(float)Math.atan2(2*(camera.w()*camera.y()+camera.x()*camera.z()),1-2*(camera.y()*camera.y()+camera.x()*camera.x()));
        poses.mulPose(Axis.YP.rotation(yaw));poses.mulPose(Axis.YP.rotationDegrees(180));
    }
}
