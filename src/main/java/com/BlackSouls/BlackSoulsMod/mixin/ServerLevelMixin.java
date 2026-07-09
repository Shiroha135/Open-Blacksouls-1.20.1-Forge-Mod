package com.BlackSouls.BlackSoulsMod.mixin;

import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(ServerLevel.class)
public class ServerLevelMixin {
    @ModifyVariable(
            method = "sendParticles(Lnet/minecraft/core/particles/ParticleOptions;DDDIDDDD)I",
            at = @At("HEAD"),
            argsOnly = true
    )
    private <T extends ParticleOptions> int blackSouls$clampHeavyHitParticles(int count, T particle) {
        if (particle == ParticleTypes.DAMAGE_INDICATOR) {
            return Math.min(count, 12);
        }
        if (particle == ParticleTypes.CRIT || particle == ParticleTypes.ENCHANTED_HIT) {
            return Math.min(count, 8);
        }
        return count;
    }
}
