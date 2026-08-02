package com.BlackSouls.BlackSoulsMod.mixin;

import com.BlackSouls.BlackSoulsMod.network.NetworkHandler;
import com.BlackSouls.BlackSoulsMod.network.packets.ClientboundOpenFogGatePromptPacket;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.registries.ForgeRegistries;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "cn.zhenhongliya.blacksouls2compat.block.FogGateBlock", remap = false)
public abstract class FogGateBlockMixin {
    private static final ResourceLocation BLACKSOULS$DEVELOPER_SCEPTER =
            new ResourceLocation("blacksouls", "dev_stat_tool");

    @Inject(method = {"use", "m_6227_"}, at = @At("HEAD"), cancellable = true, remap = false)
    private void blacksouls$openTraversalPrompt(
            BlockState state,
            Level level,
            BlockPos pos,
            Player player,
            InteractionHand hand,
            BlockHitResult hit,
            CallbackInfoReturnable<InteractionResult> callback
    ) {
        ItemStack held = player.getItemInHand(hand);
        if (player.getAbilities().instabuild
                && BLACKSOULS$DEVELOPER_SCEPTER.equals(ForgeRegistries.ITEMS.getKey(held.getItem()))) {
            return;
        }
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
            NetworkHandler.sendToPlayer(new ClientboundOpenFogGatePromptPacket(pos), serverPlayer);
        }
        callback.setReturnValue(InteractionResult.sidedSuccess(level.isClientSide));
    }

    @Inject(method = {"animateTick", "m_214162_"}, at = @At("HEAD"), cancellable = true, remap = false)
    private void blacksouls$disableFogParticles(
            BlockState state,
            Level level,
            BlockPos pos,
            RandomSource random,
            org.spongepowered.asm.mixin.injection.callback.CallbackInfo callback
    ) {
        callback.cancel();
    }
}
