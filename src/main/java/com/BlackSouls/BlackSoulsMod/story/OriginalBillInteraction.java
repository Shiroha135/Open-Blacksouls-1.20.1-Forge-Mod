package com.BlackSouls.BlackSoulsMod.story;

import com.BlackSouls.BlackSoulsMod.BlackSouls;
import com.BlackSouls.BlackSoulsMod.compat.scene.OriginalBillPlacement;
import com.BlackSouls.BlackSoulsMod.compat.scene.OriginalNpcPlacementData;
import com.BlackSouls.BlackSoulsMod.compat.scene.OriginalNpcPlacements;
import com.BlackSouls.BlackSoulsMod.entity.EntityOriginalNpc;
import com.BlackSouls.BlackSoulsMod.util.BSOriginalNpcData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = BlackSouls.MODID)
public final class OriginalBillInteraction {
    private OriginalBillInteraction() {
    }

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        var position = event.getPos();
        if (event.getHand() != InteractionHand.MAIN_HAND || !(event.getEntity() instanceof ServerPlayer player)
                || position.getX() != OriginalBillPlacement.LADDER.getX() || position.getZ() != OriginalBillPlacement.LADDER.getZ()
                || position.getY() < -37 || position.getY() > -35 || !tryInteract(player)) return;
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);
    }

    public static boolean tryInteract(ServerPlayer player) {
        if (!OriginalNpcPlacements.hasDeployedMap(player.serverLevel())) return false;
        var site = OriginalBillPlacement.sites().stream().filter(value -> value.npcId().equals(OriginalBillPlacement.PROXY_NPC)).findFirst().orElseThrow();
        if (player.position().distanceToSqr(site.position()) > 64.0D
                || !OriginalNpcPlacements.chunksReady(player.serverLevel(), site)
                || site.landmarks().stream().anyMatch(landmark -> !landmark.matches(player.serverLevel()))) return false;
        for (var candidate : OriginalBillPlacement.sites()) OriginalNpcPlacements.install(player, candidate.npcId());
        var data = OriginalNpcPlacementData.get(player.serverLevel());
        var uuid = data.entityId(OriginalBillPlacement.PROXY_NPC);
        if (uuid == null || !(player.serverLevel().getEntity(uuid) instanceof EntityOriginalNpc proxy)
                || !OriginalBillPlacement.PROXY_NPC.equals(proxy.getNpcId()) || !proxy.isAlive()
                || proxy.position().distanceToSqr(site.position()) > 0.015625D) return false;
        OriginalNpcService.refreshPage(proxy, player);
        var page = proxy.currentPage();
        if (page == null || page.commands().size() <= 1) return false;
        OriginalNpcService.interact(player, proxy);
        return true;
    }
}
