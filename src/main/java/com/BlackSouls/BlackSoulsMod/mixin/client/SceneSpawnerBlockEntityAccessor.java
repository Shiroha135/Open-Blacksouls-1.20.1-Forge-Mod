package com.BlackSouls.BlackSoulsMod.mixin.client;

import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.gen.Invoker;

@Pseudo
@Mixin(targets = "cn.zhenhongliya.blacksouls2compat.blockentity.SceneSpawnerBlockEntity", remap = false)
public interface SceneSpawnerBlockEntityAccessor {
    @Invoker(value = "getCamouflageState", remap = false)
    BlockState blacksouls$getCamouflageState();

    @Invoker(value = "getYaw", remap = false)
    float blacksouls$getYaw();
}
