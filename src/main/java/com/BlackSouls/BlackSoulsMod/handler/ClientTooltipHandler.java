package com.BlackSouls.BlackSoulsMod.handler;

import com.BlackSouls.BlackSoulsMod.BlackSouls;
import com.BlackSouls.BlackSoulsMod.client.tooltip.SpongeNameTooltipComponent;
import com.mojang.datafixers.util.Either;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderTooltipEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = BlackSouls.MODID, value = Dist.CLIENT)
public class ClientTooltipHandler {

    @SubscribeEvent
    public static void onTooltipGather(RenderTooltipEvent.GatherComponents event) {
        if (!isFlowNameItem(event.getItemStack())) {
            return;
        }

        if (!event.getTooltipElements().isEmpty() && event.getTooltipElements().get(0).left().isPresent()) {
            TooltipComponent customTitle = new SpongeNameTooltipComponent(event.getItemStack().getHoverName(), getFlowStyle(event.getItemStack()));
            event.getTooltipElements().set(0, Either.right(customTitle));
        }
    }

    private static String getFlowStyle(ItemStack stack) {
        ResourceLocation regName = net.minecraftforge.registries.ForgeRegistries.ITEMS.getKey(stack.getItem());
        if (regName == null) {
            return "sponge";
        }
        String path = regName.getPath();
        if (path.equals("cosmilite_bar") || path.equals("ascendant_spirit_essence")) {
            return "cosmic";
        }
        return "sponge";
    }

    private static boolean isFlowNameItem(ItemStack stack) {
        if (stack.isEmpty()) return false;
        ResourceLocation regName = net.minecraftforge.registries.ForgeRegistries.ITEMS.getKey(stack.getItem());
        if (regName == null) {
            return false;
        }
        String path = regName.getPath();
        return path.equals("cosmilite_bar")
                || path.equals("ascendant_spirit_essence");
    }
}
