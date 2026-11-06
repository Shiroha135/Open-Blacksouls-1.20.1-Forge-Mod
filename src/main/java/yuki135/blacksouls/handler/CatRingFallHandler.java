package yuki135.blacksouls.handler;

import yuki135.blacksouls.BlackSouls;
import net.minecraftforge.event.entity.living.LivingFallEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import top.theillusivec4.curios.api.CuriosApi;

@Mod.EventBusSubscriber(modid = BlackSouls.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class CatRingFallHandler {
    @SubscribeEvent
    public static void onLivingFall(LivingFallEvent event) {
        if (CuriosApi.getCuriosInventory(event.getEntity())
                .map(inventory -> !inventory.findCurios(BlackSouls.RING_CAT.get()).isEmpty())
                .orElse(false)) {
            event.setCanceled(true);
        }
    }

    private CatRingFallHandler() {
    }
}
