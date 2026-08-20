package com.BlackSouls.BlackSoulsMod.handler;

import com.BlackSouls.BlackSoulsMod.BlackSouls;
import com.BlackSouls.BlackSoulsMod.client.render.KawaseBlurRenderer;
import com.BlackSouls.BlackSoulsMod.client.render.SkijaGlassRenderer;
import com.BlackSouls.BlackSoulsMod.client.tooltip.SpongeNameTooltipComponent;
import com.mojang.datafixers.util.Either;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.client.event.RenderTooltipEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import org.joml.Matrix4f;
import org.joml.Vector4i;

import java.util.List;

@Mod.EventBusSubscriber(modid = BlackSouls.MODID, value = Dist.CLIENT)
public class ClientTooltipHandler {

    private static final float TOOLTIP_RADIUS = 18.0F;
    private static final float TOOLTIP_STROKE = 1.25F;

    @SubscribeEvent(priority = EventPriority.HIGHEST, receiveCanceled = true)
    public static void onRenderGuiPre(RenderGuiEvent.Pre event) {
        KawaseBlurRenderer.beginFrame();
    }

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

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onTooltipPre(RenderTooltipEvent.Pre event) {
        ItemStack stack = event.getItemStack();
        ResourceLocation itemId = ForgeRegistries.ITEMS.getKey(stack.getItem());
        if (itemId == null || !BlackSouls.MODID.equals(itemId.getNamespace())
                || !"ring_lief".equals(itemId.getPath())) {
            return;
        }
        event.setCanceled(true);
        renderGlassTooltip(event);
    }

    private static void renderGlassTooltip(RenderTooltipEvent.Pre event) {
        GuiGraphics graphics = event.getGraphics();
        Font font = event.getFont();
        List<ClientTooltipComponent> components = event.getComponents();
        if (components.isEmpty()) {
            return;
        }

        int contentWidth = 0;
        int contentHeight = 0;
        for (int i = 0; i < components.size(); i++) {
            ClientTooltipComponent component = components.get(i);
            contentWidth = Math.max(contentWidth, component.getWidth(font));
            contentHeight += component.getHeight();
            if (i == 0 && components.size() > 1) {
                contentHeight += 2;
            }
        }

        int padding = 7;
        int boxWidth = contentWidth + padding * 2;
        int boxHeight = contentHeight + padding * 2;
        int x = event.getX() + 12;
        int y = event.getY() - 12;
        Minecraft minecraft = Minecraft.getInstance();
        int screenWidth = minecraft.getWindow().getGuiScaledWidth();
        int screenHeight = minecraft.getWindow().getGuiScaledHeight();
        if (x + boxWidth > screenWidth - 6) {
            x = event.getX() - boxWidth - 12;
        }
        if (y + boxHeight > screenHeight - 6) {
            y = screenHeight - boxHeight - 6;
        }
        x = Math.max(6, x);
        y = Math.max(6, y);

        graphics.pose().pushPose();
        graphics.pose().translate(0.0F, 0.0F, 520.0F);
        SkijaGlassRenderer.panel(graphics, x, y, boxWidth, boxHeight,
                TOOLTIP_RADIUS, 0.85F, false, true, 0.31F);
        SkijaGlassRenderer.stroke(graphics, x, y, boxWidth, boxHeight,
                TOOLTIP_RADIUS, TOOLTIP_STROKE, new Vector4i(124, 255, 114, 255));

        int textY = y + padding;
        Matrix4f matrix = graphics.pose().last().pose();
        MultiBufferSource.BufferSource buffer = graphics.bufferSource();
        for (int i = 0; i < components.size(); i++) {
            ClientTooltipComponent component = components.get(i);
            component.renderText(font, x + padding, textY, matrix, buffer);
            textY += component.getHeight();
            if (i == 0 && components.size() > 1) {
                textY += 2;
            }
        }
        buffer.endBatch();
        graphics.pose().popPose();
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
