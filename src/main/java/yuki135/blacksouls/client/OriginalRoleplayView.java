package yuki135.blacksouls.client;

import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderHandEvent;
import net.minecraftforge.client.event.RenderPlayerEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import yuki135.blacksouls.capability.BSPlayerStats;
import yuki135.blacksouls.client.gui.GuiDialogueEnhanced;
import yuki135.blacksouls.client.gui.GuiOriginalNpcDialogue;
import yuki135.blacksouls.client.render.OriginalCharacterSprite;
import yuki135.blacksouls.entity.EntityOpeningActor;
import yuki135.blacksouls.story.OriginalOpeningCoda;
import yuki135.blacksouls.story.OriginalOpeningScenes;
import yuki135.blacksouls.story.OriginalOpeningProfile.Stage;
import yuki135.blacksouls.util.OriginalOpeningActorSprites;

@Mod.EventBusSubscriber(modid="blacksouls",value=Dist.CLIENT)
public final class OriginalRoleplayView {
    private static CameraType previousCamera;
    private static Entity partner;
    private static boolean enabled;
    private static double walkDistance;
    private static Vec3 previousPosition;
    private static float movementYaw;
    private static boolean moving;
    private static boolean choosing;

    public record CameraFrame(Vec3 position,float yaw,float pitch) { }

    private OriginalRoleplayView() { }

    public static int chosenOrigin() {
        var minecraft=Minecraft.getInstance();
        if(minecraft.player==null)return 0;
        var opening=OriginalOpeningController.state();
        if(opening!=null&&opening.chosenOrigin()>0&&opening.stage()!=Stage.COMPLETE)return opening.chosenOrigin();
        var stats=minecraft.player.getCapability(BSPlayerStats.CAPABILITY).resolve().orElse(null);
        if(stats==null||stats.openingProfile().hasRejectedData())return 0;
        return stats.openingProfile().realOriginId();
    }

    public static boolean active() {
        var minecraft=Minecraft.getInstance();
        if(selectingOrigin())return false;
        return minecraft.level!=null&&minecraft.player!=null&&!minecraft.player.isSpectator()
                &&chosenOrigin()>0&&scripted();
    }

    private static boolean scripted() {
        var minecraft=Minecraft.getInstance();var opening=OriginalOpeningController.state();
        return opening!=null&&opening.locked()||OriginalOpeningPresentationController.locksInput()
                ||minecraft.screen instanceof GuiOriginalNpcDialogue||minecraft.screen instanceof GuiDialogueEnhanced;
    }

    private static boolean characterVisible() {
        var minecraft=Minecraft.getInstance();
        return minecraft.level!=null&&minecraft.player!=null&&!minecraft.player.isSpectator()&&!selectingOrigin()&&chosenOrigin()>0;
    }

    private static boolean selectingOrigin() {
        var opening=OriginalOpeningController.state();
        return opening!=null&&(opening.stage()==Stage.NAME_PENDING||opening.stage()==Stage.ORIGIN_SELECTION
                ||opening.stage()==Stage.ORIGIN_CONFIRMED&&opening.command()>=OriginalOpeningCoda.ORIGIN_ANIMATION
                &&opening.command()<=OriginalOpeningCoda.ORIGIN_TRANSFER);
    }

    private static boolean originFrontView() {
        var opening=OriginalOpeningController.state();
        return opening!=null&&opening.stage()==Stage.ORIGIN_CONFIRMED&&!selectingOrigin();
    }

    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent event) {
        if(event.phase!=TickEvent.Phase.END)return;
        var minecraft=Minecraft.getInstance();
        boolean selecting=selectingOrigin();
        if(!active()) {
            if(enabled)restore();
            if(selecting&&!choosing)minecraft.options.setCameraType(CameraType.FIRST_PERSON);
            partner=null;
        }
        choosing=selecting;
        if(minecraft.player==null||minecraft.level==null)return;
        if(active()) {
        if(!enabled) { previousCamera=minecraft.options.getCameraType();enabled=true; }
        minecraft.options.setCameraType(originFrontView()?CameraType.THIRD_PERSON_FRONT:CameraType.THIRD_PERSON_BACK);
        partner=findPartner();
        }
        var position=minecraft.player.position();
        var delta=previousPosition==null?Vec3.ZERO:position.subtract(previousPosition);
        moving=delta.horizontalDistanceSqr()>.0001&&delta.horizontalDistanceSqr()<4;
        if(moving) {walkDistance+=delta.horizontalDistance();movementYaw=yaw(delta);}
        previousPosition=position;
    }

    private static Entity findPartner() {
        var minecraft=Minecraft.getInstance();var opening=OriginalOpeningController.state();
        if(opening!=null&&opening.locked()&&!opening.awaitingReady()&&opening.notice().isEmpty()
                &&opening.stage()!=Stage.FALLING&&opening.stage()!=Stage.ARRIVAL_PENDING&&opening.stage()!=Stage.NAME_PENDING) {
            int wanted=opening.focusOrigin()>0?opening.focusOrigin():0;
            for(var entity:minecraft.level.entitiesForRendering())if(entity instanceof EntityOpeningActor actor&&actor.originId()==wanted
                    &&minecraft.player.getUUID().equals(actor.ownerId())&&!actor.isInvisible()&&actor.distanceToSqr(minecraft.player)<100
                    &&(wanted>0||!OriginalOpeningCoda.active(opening.command())||OriginalOpeningCoda.aliceVisible(opening.command())))return actor;
        }
        int id=minecraft.screen instanceof GuiOriginalNpcDialogue dialogue?dialogue.cameraEntityId()
                :minecraft.screen instanceof GuiDialogueEnhanced dialogue?dialogue.cameraEntityId():-1;
        var target=id<0?null:minecraft.level.getEntity(id);
        return target!=null&&target.isAlive()&&!target.isInvisible()&&target.distanceToSqr(minecraft.player)<100?target:null;
    }

    public static CameraFrame camera(Entity entity,float partialTick) {
        var minecraft=Minecraft.getInstance();
        if(!active()||entity!=minecraft.player)return null;
        var player=minecraft.player;var position=player.getPosition(partialTick);
        Entity other=findPartner();partner=other;
        Vec3 target,desired;
        if(originFrontView()) {
            partner=null;
            target=position.add(0,.95,0);
            desired=target.add(0,1.55,5.2);
        } else if(other!=null) {
            var otherPosition=other.getPosition(partialTick);
            target=position.lerp(otherPosition,.5).add(0,.85,0);
            var axis=otherPosition.subtract(position).multiply(1,0,1).normalize();
            if(axis.lengthSqr()<.01)axis=new Vec3(1,0,0);
            var side=new Vec3(-axis.z,0,axis.x);
            if(side.z>0||Math.abs(side.z)<.01&&side.x>0)side=side.scale(-1);
            desired=target.add(side.scale(5.7+Math.min(2,position.distanceTo(otherPosition)*.25))).add(0,3.5,0);
            var alternative=target.subtract(side.scale(5.7+Math.min(2,position.distanceTo(otherPosition)*.25))).add(0,3.5,0);
            var first=unobstructed(target,desired);
            var second=unobstructed(target,alternative);
            desired=first.distanceToSqr(target)>=second.distanceToSqr(target)?first:second;
        } else {
            float yaw=Mth.rotLerp(partialTick,player.yRotO,player.getYRot());
            float pitch=Mth.clamp(Mth.lerp(partialTick,player.xRotO,player.getXRot())+23,12,62);
            target=position.add(0,1.0,0);
            desired=target.subtract(Vec3.directionFromRotation(pitch,yaw).scale(5.6));
        }
        desired=unobstructed(target,desired);
        var look=target.subtract(desired);
        return new CameraFrame(desired,yaw(look),(float)-Math.toDegrees(Math.atan2(look.y,look.horizontalDistance())));
    }

    private static Vec3 unobstructed(Vec3 target,Vec3 desired) {
        var minecraft=Minecraft.getInstance();
        var hit=minecraft.level.clip(new ClipContext(target,desired,ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,minecraft.player));
        return hit.getType()==HitResult.Type.MISS?desired:hit.getLocation().add(target.subtract(desired).normalize().scale(.2));
    }

    public static float actorYaw(Entity actor,float fallback) {
        var player=Minecraft.getInstance().player;
        if(partner==actor&&player!=null)return yaw(player.position().subtract(actor.position()));
        return fallback;
    }

    @SubscribeEvent(priority=EventPriority.HIGHEST) public static void hidePlayer(RenderPlayerEvent.Pre event) {
        if(characterVisible()&&event.getEntity()==Minecraft.getInstance().player)event.setCanceled(true);
    }

    @SubscribeEvent(priority=EventPriority.HIGHEST) public static void hideHand(RenderHandEvent event) {if(active())event.setCanceled(true);}

    @SubscribeEvent public static void sprite(RenderLevelStageEvent event) {
        if(event.getStage()!=RenderLevelStageEvent.Stage.AFTER_ENTITIES||!characterVisible())return;
        var minecraft=Minecraft.getInstance();var player=minecraft.player;
        if(!active()&&minecraft.getCameraEntity()==player&&minecraft.options.getCameraType().isFirstPerson())return;
        if(player.isInvisible())return;
        int selected=chosenOrigin();var sprite=OriginalOpeningActorSprites.player(selected);
        var position=player.getPosition(event.getPartialTick());var camera=event.getCamera().getPosition();
        float facing=originFrontView()?yaw(camera.subtract(position)):partner!=null?yaw(partner.position().subtract(player.position())):moving?movementYaw:Mth.rotLerp(event.getPartialTick(),player.yRotO,player.getYRot());
        int row=OriginalCharacterSprite.facingRow(facing,position,camera);
        int pattern=moving?new int[]{1,2,1,0}[(int)(walkDistance/.18)%4]:1;
        var poses=event.getPoseStack();poses.pushPose();poses.translate(position.x-camera.x,position.y-camera.y,position.z-camera.z);
        var buffers=minecraft.renderBuffers().bufferSource();
        OriginalCharacterSprite.render(sprite,row,pattern,1.8F,event.getCamera().rotation(),poses,buffers,LevelRenderer.getLightColor(minecraft.level,player.blockPosition()));
        buffers.endBatch(net.minecraft.client.renderer.RenderType.entityCutoutNoCull(sprite.texture()));poses.popPose();
    }

    private static float yaw(Vec3 direction) {return (float)Math.toDegrees(Math.atan2(-direction.x,direction.z));}

    private static void restore() {
        if(enabled&&previousCamera!=null)Minecraft.getInstance().options.setCameraType(previousCamera);
        enabled=false;previousCamera=null;partner=null;previousPosition=null;walkDistance=0;moving=false;
    }

    @SubscribeEvent public static void logout(ClientPlayerNetworkEvent.LoggingOut event) {restore();choosing=false;}
}
