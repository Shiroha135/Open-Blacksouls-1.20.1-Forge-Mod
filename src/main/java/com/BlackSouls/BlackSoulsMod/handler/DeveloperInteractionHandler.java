package com.BlackSouls.BlackSoulsMod.handler;

import com.BlackSouls.BlackSoulsMod.BlackSouls;
import com.BlackSouls.BlackSoulsMod.capability.BonfireEntry;
import com.BlackSouls.BlackSoulsMod.entity.DialogueResettable;
import com.BlackSouls.BlackSoulsMod.entity.EntityRedHood;
import com.BlackSouls.BlackSoulsMod.network.NetworkHandler;
import com.BlackSouls.BlackSoulsMod.network.WhiteBearShopService;
import com.BlackSouls.BlackSoulsMod.network.packets.ClientboundBonfireEditorPacket;
import com.BlackSouls.BlackSoulsMod.util.BonfireMetadata;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionHand;
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
        if (event.getHand() != InteractionHand.MAIN_HAND
                || !isDeveloperReset(event.getEntity().getMainHandItem(), event.getEntity().isShiftKeyDown())) {
            return;
        }
        if (event.getLevel().getBlockState(event.getPos()).is(BlockTags.CAMPFIRES)) {
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.SUCCESS);
            if (event.getEntity() instanceof ServerPlayer player) {
                if (!player.isCreative()) {
                    player.displayClientMessage(Component.translatable("message.blacksouls.dev.no_permission"), true);
                    return;
                }
                if (!BonfireMetadata.isSupported(event.getLevel(), event.getPos())) {
                    player.displayClientMessage(
                            Component.translatable("message.blacksouls.bonfire.editor.unsupported"),
                            true
                    );
                    return;
                }
                BonfireMetadata.Data metadata = BonfireMetadata.read(event.getLevel(), event.getPos());
                BonfireEntry entry = new BonfireEntry(
                        GlobalPos.of(event.getLevel().dimension(), event.getPos()),
                        metadata.name(),
                        metadata.description()
                );
                NetworkHandler.sendToPlayer(new ClientboundBonfireEditorPacket(entry), player);
            }
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
        if (target instanceof EntityRedHood
                && event.getEntity().getMainHandItem().is(BlackSouls.DEV_STAT_TOOL.get())) {
            return;
        }
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
