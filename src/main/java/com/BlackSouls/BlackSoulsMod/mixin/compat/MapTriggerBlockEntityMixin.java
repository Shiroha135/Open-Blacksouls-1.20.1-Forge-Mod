package com.BlackSouls.BlackSoulsMod.mixin.compat;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "cn.zhenhongliya.blacksouls2compat.blockentity.MapTriggerBlockEntity", remap = false)
public abstract class MapTriggerBlockEntityMixin {
    @Unique
    private static final String BLACKSOULS_CURRENT_SCENE = "Blacksouls2CurrentScene";
    @Unique
    private static final String BLACKSOULS_LAST_SCENE_TRIGGER = "BlacksoulsLastSceneTrigger";
    @Unique
    private static final String BLACKSOULS_LAST_SCENE_TRIGGER_TICK = "BlacksoulsLastSceneTriggerTick";
    @Unique
    private static final long BLACKSOULS_REENTRY_GRACE_TICKS = 10L;

    @Inject(
            method = "trigger",
            at = @At(
                    value = "INVOKE",
                    target = "Lcn/zhenhongliya/blacksouls2compat/scene/SceneSpawnManager;enterScene(Lnet/minecraft/server/level/ServerPlayer;Ljava/lang/String;)V",
                    remap = false
            ),
            remap = false
    )
    private void blacksouls$allowSameSceneReentry(ServerPlayer player, CallbackInfo callback) {
        BlockEntity blockEntity = (BlockEntity) (Object) this;
        Level sourceLevel = blockEntity.getLevel();
        String sourceDimension = sourceLevel == null ? "" : sourceLevel.dimension().location().toString();
        String source = sourceDimension + "#" + blockEntity.getBlockPos().asLong();
        long tick = player.serverLevel().getGameTime();
        CompoundTag data = player.getPersistentData();
        String previousSource = data.getString(BLACKSOULS_LAST_SCENE_TRIGGER);
        long previousTick = data.getLong(BLACKSOULS_LAST_SCENE_TRIGGER_TICK);
        boolean newActivation = !source.equals(previousSource)
                || tick < previousTick
                || tick - previousTick > BLACKSOULS_REENTRY_GRACE_TICKS;
        data.putString(BLACKSOULS_LAST_SCENE_TRIGGER, source);
        data.putLong(BLACKSOULS_LAST_SCENE_TRIGGER_TICK, tick);
        if (newActivation) {
            data.remove(BLACKSOULS_CURRENT_SCENE);
        }
    }
}
