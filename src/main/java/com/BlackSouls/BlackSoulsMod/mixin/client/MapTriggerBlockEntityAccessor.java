package com.BlackSouls.BlackSoulsMod.mixin.client;

import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.gen.Invoker;

@Pseudo
@Mixin(targets = "cn.zhenhongliya.blacksouls2compat.blockentity.MapTriggerBlockEntity", remap = false)
public interface MapTriggerBlockEntityAccessor {
    @Invoker(value = "getCamouflageState", remap = false)
    BlockState blacksouls$getCamouflageState();
}
