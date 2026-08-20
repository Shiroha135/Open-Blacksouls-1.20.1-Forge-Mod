package com.BlackSouls.BlackSoulsMod.client.tooltip;

import com.BlackSouls.BlackSoulsMod.BlackSouls;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderTooltipEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

@Mod.EventBusSubscriber(modid = BlackSouls.MODID, value = Dist.CLIENT)
public final class ClientTooltipEvents {

    private ClientTooltipEvents() {}

    
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onTooltipPre(RenderTooltipEvent.Pre event) {

        ItemStack stack = event.getItemStack();
        ResourceLocation itemId = ForgeRegistries.ITEMS.getKey(stack.getItem());

        if (itemId == null) return;

        
        if (!BlackSouls.MODID.equals(itemId.getNamespace())) return;

        
        if (!itemId.getPath().contains("avatar_pack")) return;

        
        event.setCanceled(true);

        GuiGraphics graphics = event.getGraphics();
        long time = System.currentTimeMillis();

        Minecraft mc = Minecraft.getInstance();
        int screenWidth = mc.getWindow().getGuiScaledWidth();
        int screenHeight = mc.getWindow().getGuiScaledHeight();

        
        CustomTooltipRenderer.render(
                graphics,
                event.getFont(),
                stack,
                event.getComponents(),
                event.getX(),
                event.getY(),
                screenWidth,
                screenHeight,
                time
        );
    }
}
