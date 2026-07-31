package com.BlackSouls.BlackSoulsMod.mixin.compat;

import com.BlackSouls.BlackSoulsMod.compat.scene.SceneSpawnerBounds;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "cn.zhenhongliya.blacksouls2compat.scene.SceneSpawnManager", remap = false)
public abstract class SceneSpawnManagerMixin {
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
    }
}
