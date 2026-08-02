package com.BlackSouls.BlackSoulsMod.mixin.compat;

import com.BlackSouls.BlackSoulsMod.compat.scene.SceneSpawnerBossData;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "cn.zhenhongliya.blacksouls2compat.block.SceneSpawnerBlock", remap = false)
public abstract class SceneSpawnerBlockMixin {
    @Inject(method = {"onRemove", "m_6810_"}, at = @At("HEAD"), remap = false)
    private void blacksouls$forgetRemovedBoss(BlockState state, Level level, BlockPos pos,
                                               BlockState newState, boolean moving,
                                               CallbackInfo callback) {
        if (!state.is(newState.getBlock()) && level instanceof ServerLevel serverLevel) {
            SceneSpawnerBossData.get(serverLevel.getServer()).clear(
                    SceneSpawnerBossData.spawnerKey(serverLevel.dimension(), pos));
        }
    }
}
