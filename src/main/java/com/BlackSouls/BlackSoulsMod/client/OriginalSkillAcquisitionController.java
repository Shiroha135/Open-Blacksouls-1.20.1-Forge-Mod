package com.BlackSouls.BlackSoulsMod.client;

import com.BlackSouls.BlackSoulsMod.BlackSouls;
import com.BlackSouls.BlackSoulsMod.client.gui.GuiOriginalNpcDialogue;
import com.BlackSouls.BlackSoulsMod.network.packets.ClientboundOriginalSkillInfoPacket;
import com.BlackSouls.BlackSoulsMod.story.OriginalSkillAcquisitionState;
import com.BlackSouls.BlackSoulsMod.util.OriginalSkillAcquisitionData;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.opengl.GL11;

@Mod.EventBusSubscriber(modid = BlackSouls.MODID, value = Dist.CLIENT)
public final class OriginalSkillAcquisitionController {
    private static final OriginalSkillAcquisitionState STATE = new OriginalSkillAcquisitionState();
    private static final ResourceLocation ICONS = ResourceLocation.fromNamespaceAndPath(BlackSouls.MODID, "textures/gui/battle/icon_set.png");
    private static final ResourceLocation BODY_FONT = ResourceLocation.fromNamespaceAndPath(BlackSouls.MODID, "original_skill_info_body");
    private static final ResourceLocation TITLE_FONT = ResourceLocation.fromNamespaceAndPath(BlackSouls.MODID, "original_skill_info_title");
    private static ResourceLocation dimension;

    private OriginalSkillAcquisitionController() {
    }

    public static void apply(ClientboundOriginalSkillInfoPacket packet) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || !minecraft.level.dimension().location().equals(packet.dimension())) return;
        if (dimension != null && !dimension.equals(packet.dimension())) STATE.reset();
        long now = System.nanoTime();
        STATE.advance(now, minecraft.isPaused());
        if (!STATE.accept(packet.revision(), packet.skillId(), packet.actorOperand(), packet.learned(), packet.clear(), now)) return;
        dimension = packet.dimension();
        if (!packet.clear() && packet.learned()) {
            minecraft.getSoundManager().play(SimpleSoundInstance.forUI(BlackSouls.MAGIC1_EVENT.get(), 0.8F, 0.8F));
        }
        if (!mapScene(minecraft)) STATE.clearEntries();
    }

    private static boolean mapScene(Minecraft minecraft) {
        return minecraft.screen == null || minecraft.screen instanceof GuiOriginalNpcDialogue || minecraft.screen instanceof ChatScreen;
    }

    private static void advance() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.player == null || dimension != null
                && !dimension.equals(minecraft.level.dimension().location())) {
            reset();
            return;
        }
        if (!mapScene(minecraft)) {
            STATE.clearEntries();
            return;
        }
        STATE.advance(System.nanoTime(), minecraft.isPaused());
    }

    @SubscribeEvent
    public static void onTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END) advance();
    }

    @SubscribeEvent
    public static void onLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        reset();
    }

    @SubscribeEvent
    public static void onUnload(LevelEvent.Unload event) {
        if (event.getLevel() instanceof Level level && level.isClientSide()) reset();
    }

    public static void reset() {
        STATE.reset();
        dimension = null;
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void render(RenderGuiEvent.Post event) {
        advance();
        if (STATE.views().isEmpty()) return;
        Minecraft minecraft = Minecraft.getInstance();
        GuiGraphics graphics = event.getGuiGraphics();
        var canvas = OriginalSkillAcquisitionState.canvas(graphics.guiWidth(), graphics.guiHeight());
        graphics.flush();
        boolean blend = GL11.glIsEnabled(GL11.GL_BLEND);
        float[] color = RenderSystem.getShaderColor().clone();
        graphics.pose().pushPose();
        graphics.enableScissor((int) Math.floor(canvas.x()), (int) Math.floor(canvas.y()),
                (int) Math.ceil(canvas.x() + 640 * canvas.scale()), (int) Math.ceil(canvas.y() + 480 * canvas.scale()));
        try {
            graphics.pose().translate(canvas.x(), canvas.y(), 0);
            graphics.pose().scale((float) canvas.scale(), (float) canvas.scale(), 1.0F);
            RenderSystem.enableBlend();
            for (var window : STATE.views()) {
                graphics.enableScissor((int) Math.floor(canvas.x() - 4 * canvas.scale()),
                        (int) Math.floor(canvas.y() + window.contentY() * canvas.scale()),
                        (int) Math.ceil(canvas.x() + 644 * canvas.scale()),
                        (int) Math.ceil(canvas.y() + (window.contentY() + 46) * canvas.scale()));
                try {
                    draw(graphics, minecraft, window);
                    graphics.flush();
                } finally {
                    graphics.disableScissor();
                }
            }
            graphics.flush();
        } finally {
            graphics.flush();
            graphics.setColor(color[0], color[1], color[2], color[3]);
            if (!blend) RenderSystem.disableBlend();
            graphics.disableScissor();
            graphics.pose().popPose();
        }
    }

    private static void draw(GuiGraphics graphics, Minecraft minecraft, OriginalSkillAcquisitionState.View window) {
        if (window.opacity() == 0) return;
        int x = window.contentX(), y = window.contentY();
        int alpha = window.opacity() << 24;
        int background = window.backgroundAlpha() << 24;
        Component title = text(OriginalSkillAcquisitionData.title(window.actorOperand(), window.learned()), TITLE_FONT);
        Component name = text(OriginalSkillAcquisitionData.name(), BODY_FONT);
        Component description = text(OriginalSkillAcquisitionData.description(), BODY_FONT);
        graphics.fill(x, y + 14, x + 644, y + 38, background);
        graphics.fill(x, y, x + minecraft.font.width(title) + 4, y + 14, background);
        int icon = OriginalSkillAcquisitionData.iconIndex();
        graphics.setColor(1, 1, 1, window.opacity() / 255.0F);
        graphics.blit(ICONS, x + 4, y + 14, icon % 16 * 24, icon / 16 * 24, 24, 24, 384, 1300);
        graphics.setColor(1, 1, 1, 1);
        drawText(graphics, minecraft, title, x + 4, y, alpha | 0xFFFFFF, alpha | 0x402080);
        int outline = (window.opacity() * 128 / 255) << 24;
        drawText(graphics, minecraft, name, x + 28, y + 15, alpha | 0xFFFFFF, outline);
        drawText(graphics, minecraft, description, x + 204, y + 15, alpha | 0xFFFFFF, outline);
    }

    private static Component text(String value, ResourceLocation font) {
        return Component.literal(value).withStyle(Style.EMPTY.withFont(font));
    }

    private static void drawText(GuiGraphics graphics, Minecraft minecraft, Component value, int x, int y, int color, int outline) {
        graphics.drawString(minecraft.font, value, x + 1, y, outline, false);
        graphics.drawString(minecraft.font, value, x - 1, y, outline, false);
        graphics.drawString(minecraft.font, value, x, y + 1, outline, false);
        graphics.drawString(minecraft.font, value, x, y - 1, outline, false);
        graphics.drawString(minecraft.font, value, x, y, color, false);
    }
}
