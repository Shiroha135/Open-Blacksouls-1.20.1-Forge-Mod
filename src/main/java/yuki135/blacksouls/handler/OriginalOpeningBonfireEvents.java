package yuki135.blacksouls.handler;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.event.level.ChunkWatchEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import yuki135.blacksouls.BlackSouls;
import yuki135.blacksouls.story.OriginalOpeningBonfireFlow;

@Mod.EventBusSubscriber(modid = BlackSouls.MODID)
public final class OriginalOpeningBonfireEvents {
    private OriginalOpeningBonfireEvents() {
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void rightClick(PlayerInteractEvent.RightClickBlock event) {
        if (!OriginalOpeningBonfireFlow.isHidden(event.getEntity(), event.getPos())) return;
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.FAIL);
        if (event.getEntity() instanceof ServerPlayer player) OriginalOpeningBonfireFlow.refreshBlock(player);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void leftClick(PlayerInteractEvent.LeftClickBlock event) {
        if (!OriginalOpeningBonfireFlow.isHidden(event.getEntity(), event.getPos())) return;
        event.setCanceled(true);
        if (event.getEntity() instanceof ServerPlayer player) OriginalOpeningBonfireFlow.refreshBlock(player);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void breaking(BlockEvent.BreakEvent event) {
        if (OriginalOpeningBonfireFlow.isHidden(event.getPlayer(), event.getPos())) event.setCanceled(true);
    }

    @SubscribeEvent
    public static void watch(ChunkWatchEvent.Watch event) {
        if (event.getPos().x == OriginalOpeningBonfireFlow.FALLING_BONFIRE.getX() >> 4
                && event.getPos().z == OriginalOpeningBonfireFlow.FALLING_BONFIRE.getZ() >> 4) {
            OriginalOpeningBonfireFlow.refreshBlock(event.getPlayer());
        }
    }

    @SubscribeEvent
    public static void login(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) OriginalOpeningBonfireFlow.refreshBlock(player);
    }

    @SubscribeEvent
    public static void respawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) OriginalOpeningBonfireFlow.refreshBlock(player);
    }

    @SubscribeEvent
    public static void dimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) OriginalOpeningBonfireFlow.refreshBlock(player);
    }

    @SubscribeEvent
    public static void tick(TickEvent.PlayerTickEvent event) {
        if (event.phase == TickEvent.Phase.END && event.player instanceof ServerPlayer player
                && player.tickCount % 20 == 0 && OriginalOpeningBonfireFlow.isHidden(player, OriginalOpeningBonfireFlow.FALLING_BONFIRE)) {
            OriginalOpeningBonfireFlow.refreshBlock(player);
        }
    }
}
