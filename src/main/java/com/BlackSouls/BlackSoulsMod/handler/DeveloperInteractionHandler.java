package com.BlackSouls.BlackSoulsMod.handler;

import com.BlackSouls.BlackSoulsMod.BlackSouls;
import com.BlackSouls.BlackSoulsMod.entity.DialogueResettable;
import com.BlackSouls.BlackSoulsMod.network.WhiteBearShopService;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

@Mod.EventBusSubscriber(modid = BlackSouls.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class DeveloperInteractionHandler {
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onEntityInteract(PlayerInteractEvent.EntityInteractSpecific event) {
        resetEntity(event, event.getTarget());
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        resetEntity(event, event.getTarget());
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onBlockInteract(PlayerInteractEvent.RightClickBlock event) {
        if (!isDeveloperReset(event.getEntity().getMainHandItem(), event.getEntity().isShiftKeyDown())) {
            return;
        }
        ResourceLocation blockId = ForgeRegistries.BLOCKS.getKey(
                event.getLevel().getBlockState(event.getPos()).getBlock()
        );
        if (blockId == null
                || !blockId.getNamespace().equals("blacksouls2")
                || !blockId.getPath().equals("bai_xiong_mao_shop")) {
            return;
        }
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);
        if (event.getEntity() instanceof ServerPlayer player) {
            WhiteBearShopService.resetDialogue(player);
            player.displayClientMessage(
                    Component.translatable("message.blacksouls.dev.dialogue_reset.white_bear"),
                    true
            );
        }
    }

    private static boolean isDeveloperReset(ItemStack stack, boolean sneaking) {
        return sneaking && stack.is(BlackSouls.DEV_STAT_TOOL.get());
    }

    private static void resetEntity(PlayerInteractEvent event, net.minecraft.world.entity.Entity target) {
        if (!isDeveloperReset(event.getEntity().getMainHandItem(), event.getEntity().isShiftKeyDown())
                || !(target instanceof DialogueResettable resettable)) {
            return;
        }
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);
        if (event.getEntity() instanceof ServerPlayer player) {
            resettable.resetDialogue(player);
            player.displayClientMessage(
                    Component.translatable(
                            "message.blacksouls.dev.dialogue_reset.entity",
                            target.getDisplayName()
                    ),
                    true
            );
        }
    }

    private DeveloperInteractionHandler() {
    }
}
