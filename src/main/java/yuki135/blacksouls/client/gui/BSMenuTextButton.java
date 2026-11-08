package yuki135.blacksouls.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import com.mojang.blaze3d.systems.RenderSystem;
import org.jetbrains.annotations.NotNull;

public class BSMenuTextButton extends Button {
    private float preferredTextScale = 1.0F;
    private int textPadding = 2;

    public BSMenuTextButton(int x, int y, int width, int height, Component message, OnPress onPress) {
        super(x, y, width, height, message, onPress, DEFAULT_NARRATION);
    }

    public void setTextLayout(float scale, int padding) {
        this.preferredTextScale = scale;
        this.textPadding = padding;
    }

    public float textScale(Font font) {
        return Math.min(preferredTextScale, Math.min((width - textPadding * 2.0F) / Math.max(1, font.width(getMessage()) + 1),
                Math.max(1, height - 2.0F) / (font.lineHeight + 1.0F)));
    }
    @Override
    public void renderWidget(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        int x = this.getX();
        int y = this.getY();
        guiGraphics.flush();
        guiGraphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
        if (this.isHoveredOrFocused()) {
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            guiGraphics.blit(BSGuiUtils.RM_WINDOW_SKIN, x, y, this.width, this.height, 64, 64, 32, 32, 128, 128);
            RenderSystem.disableBlend();
        }
        int textColor = this.active ? 0xFFFFFFFF : 0xFFA0A0A0;
        Font font = Minecraft.getInstance().font;
        float textScale = textScale(font);
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(x + textPadding, y + (this.height - (font.lineHeight + 1) * textScale) / 2.0F, 1.0F);
        guiGraphics.pose().scale(textScale, textScale, 1.0F);
        guiGraphics.drawString(font, this.getMessage(), 0, 0, textColor, true);
        guiGraphics.flush();
        guiGraphics.pose().popPose();
    }
}
