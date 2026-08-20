package com.BlackSouls.BlackSoulsMod.handler;

import com.BlackSouls.BlackSoulsMod.BlackSouls;
import com.BlackSouls.BlackSoulsMod.client.render.GuiGradientTextRenderer;
import com.BlackSouls.BlackSoulsMod.client.render.KawaseBlurRenderer;
import com.BlackSouls.BlackSoulsMod.client.render.SkijaGlassRenderer;
import com.BlackSouls.BlackSoulsMod.mixin.client.ClientTextTooltipAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
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
    private static final int[] LIEF_TITLE_COLORS = {0x7CFF72, 0xE7FFFF, 0x67C8FF, 0xB680FF, 0xFF78F0};

    @SubscribeEvent(priority = EventPriority.HIGHEST, receiveCanceled = true)
    public static void onRenderGuiPre(RenderGuiEvent.Pre event) {
        KawaseBlurRenderer.beginFrame();
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
        String titleText = event.getItemStack().getHoverName().getString().replaceAll("§[0-9a-fk-or]", "");
        GuiGradientTextRenderer.draw(graphics, font, titleText, x + padding, textY, LIEF_TITLE_COLORS);
        textY += components.get(0).getHeight();
        if (components.size() > 1) {
            textY += 2;
        }
        Matrix4f matrix = graphics.pose().last().pose();
        MultiBufferSource.BufferSource buffer = graphics.bufferSource();
        for (int i = 1; i < components.size(); i++) {
            ClientTooltipComponent component = components.get(i);
            if (component instanceof ClientTextTooltipAccessor textComponent) {
                font.drawInBatch(textComponent.blacksouls$getText(), x + padding, textY,
                        -1, false, matrix, buffer, Font.DisplayMode.NORMAL, 0, 15728880);
            } else {
                component.renderText(font, x + padding, textY, matrix, buffer);
            }
            textY += component.getHeight();
        }
        buffer.endBatch();
        graphics.pose().popPose();
    }

}
