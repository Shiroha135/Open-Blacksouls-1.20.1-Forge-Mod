package yuki135.blacksouls.handler;

import yuki135.blacksouls.capability.BSPlayerStats;
import yuki135.blacksouls.BlackSouls;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = BlackSouls.MODID)
@SuppressWarnings("removal")
public class CapabilityHandler {

    public static final ResourceLocation BS_STATS_CAP = new ResourceLocation(BlackSouls.MODID, "bs_stats");

    @SubscribeEvent
    public static void attachCapability(AttachCapabilitiesEvent<Entity> event) {
        if (event.getObject() instanceof Player) {
            event.addCapability(BS_STATS_CAP, new BSPlayerStats.Provider());
        }
    }

    @SubscribeEvent
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        event.getEntity().getCapability(BSPlayerStats.CAPABILITY).invalidate();
    }
}