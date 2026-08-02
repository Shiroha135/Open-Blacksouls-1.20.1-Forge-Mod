package com.BlackSouls.BlackSoulsMod.mixin.compat;

import com.BlackSouls.BlackSoulsMod.compat.scene.SceneSpawnerBounds;
import com.BlackSouls.BlackSoulsMod.compat.scene.SceneSpawnerBossData;
import com.BlackSouls.BlackSoulsMod.compat.scene.SceneSpawnerBossState;
import com.BlackSouls.BlackSoulsMod.network.NetworkHandler;
import com.BlackSouls.BlackSoulsMod.network.packets.ClientboundCurrentScenePacket;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "cn.zhenhongliya.blacksouls2compat.scene.SceneSpawnManager", remap = false)
public abstract class SceneSpawnManagerMixin {
    @Inject(method = "enterScene", at = @At("RETURN"), remap = false)
    private static void blacksouls$syncCurrentScene(ServerPlayer player, String sceneId, CallbackInfo callback) {
        NetworkHandler.sendToPlayer(new ClientboundCurrentScenePacket(sceneId), player);
    }

    @Inject(method = "spawn", at = @At("RETURN"), remap = false)
    private static void blacksouls$attachBounds(ServerLevel level, BlockPos pos, ItemStack template, float yaw,
                                                 CallbackInfoReturnable<LivingEntity> callback) {
        LivingEntity entity = callback.getReturnValue();
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (entity == null || !(blockEntity instanceof SceneSpawnerBounds bounds)) {
            return;
        }
        CompoundTag data = entity.getPersistentData();
        data.putInt(SceneSpawnerBounds.ORIGIN_X_TAG, pos.getX());
        data.putInt(SceneSpawnerBounds.ORIGIN_Y_TAG, pos.getY() + 1);
        data.putInt(SceneSpawnerBounds.ORIGIN_Z_TAG, pos.getZ());
        data.putInt(SceneSpawnerBounds.RANGE_X_TAG, bounds.blacksouls$getRangeX());
        data.putInt(SceneSpawnerBounds.RANGE_Z_TAG, bounds.blacksouls$getRangeZ());
        if (entity instanceof Mob mob) {
            data.putBoolean(SceneSpawnerBounds.ORIGINAL_NO_AI_TAG, mob.isNoAi());
        }
        if (blockEntity instanceof SceneSpawnerBossState bossState && bossState.blacksouls$isBossMode()) {
            data.putBoolean(SceneSpawnerBossState.ENTITY_BOSS_TAG, true);
            data.putString(SceneSpawnerBossState.ENTITY_SCENE_ID_TAG, bossState.blacksouls$getSceneId());
        }
    }

    @Inject(method = "hasActiveEntity", at = @At("HEAD"), cancellable = true, remap = false)
    private static void blacksouls$keepDefeatedBossDead(ServerLevel level, BlockPos pos,
                                                        CallbackInfoReturnable<Boolean> callback) {
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity instanceof SceneSpawnerBossState bossState
                && bossState.blacksouls$isBossMode()
                && SceneSpawnerBossData.get(level.getServer()).isDefeated(
                SceneSpawnerBossData.spawnerKey(level.dimension(), pos))) {
            callback.setReturnValue(true);
        }
    }
}
