package yuki135.blacksouls.mixin.client;

import net.minecraft.client.Camera;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import yuki135.blacksouls.client.OriginalRoleplayView;

@Mixin(Camera.class)
public abstract class RoleplayCameraMixin {
    @Shadow protected abstract void setPosition(Vec3 position);
    @Shadow protected abstract void setRotation(float yaw,float pitch);
    @Shadow private boolean detached;

    @Inject(method="setup",at=@At("TAIL"))
    private void blacksouls$roleplayCamera(BlockGetter level,Entity entity,boolean thirdPerson,boolean reverse,float partialTick,CallbackInfo callback) {
        var frame=OriginalRoleplayView.camera(entity,partialTick);
        if(frame==null)return;
        detached=true;setRotation(frame.yaw(),frame.pitch());setPosition(frame.position());
    }
}
