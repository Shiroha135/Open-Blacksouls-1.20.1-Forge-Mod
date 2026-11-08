package yuki135.blacksouls.client.gui;

import yuki135.blacksouls.BlackSouls;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.OptionsScreen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.client.gui.screens.worldselection.SelectWorldScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.client.gui.ModListScreen;
import org.jetbrains.annotations.NotNull;
import com.mojang.blaze3d.systems.RenderSystem;
import java.util.List;

@SuppressWarnings("removal")
public class GuiBSMainMenu extends Screen {

    private static final ResourceLocation BG_TEXTURE = new ResourceLocation(BlackSouls.MODID, "textures/gui/main_menu_bg.png");

    private static final List<String> LABELS = List.of("SINGLEPLAYER", "MULTIPLAYER", "OPTIONS", "MODS", "QUIT GAME");

    private int rmBoxX;
    private int rmBoxY;
    private int rmBoxWidth;
    private int rmBoxHeight;
    private int rowHeight;
    private int padding;
    private float menuScale;

    public GuiBSMainMenu() {
        super(Component.literal("Black Souls Main Menu"));
    }

    @Override
    protected void init() {
        super.init();
        this.clearWidgets();

        this.menuScale = Math.min(this.width / 640.0F, this.height / 480.0F);
        this.rmBoxWidth = Math.round(160 * menuScale);
        this.rowHeight = Math.max(12, Math.round(24 * menuScale));
        this.padding = Math.max(6, Math.round(12 * menuScale));
        this.rmBoxHeight = rowHeight * LABELS.size() + padding * 2;
        this.rmBoxX = (this.width - rmBoxWidth) / 2;
        this.rmBoxY = Math.max(0, Math.min(this.height - rmBoxHeight, Math.round(this.height * .8F - rmBoxHeight / 2.0F)));

        int btnWidth = rmBoxWidth - padding * 2;
        int btnHeight = rowHeight;
        int startX = this.rmBoxX + padding;
        int startY = this.rmBoxY + padding;

        this.addRenderableWidget(new BSMenuTextButton(startX, startY, btnWidth, btnHeight, Component.literal("SINGLEPLAYER"), btn -> {
            if (this.minecraft != null) this.minecraft.setScreen(new SelectWorldScreen(this));
        }));
        this.addRenderableWidget(new BSMenuTextButton(startX, startY + rowHeight, btnWidth, btnHeight, Component.literal("MULTIPLAYER"), btn -> {
            if (this.minecraft != null) this.minecraft.setScreen(new JoinMultiplayerScreen(this));
        }));
        this.addRenderableWidget(new BSMenuTextButton(startX, startY + rowHeight * 2, btnWidth, btnHeight, Component.literal("OPTIONS"), btn -> {
            if (this.minecraft != null) this.minecraft.setScreen(new OptionsScreen(this, this.minecraft.options));
        }));
        this.addRenderableWidget(new BSMenuTextButton(startX, startY + rowHeight * 3, btnWidth, btnHeight, Component.literal("MODS"), btn -> {
            if (this.minecraft != null) this.minecraft.setScreen(new ModListScreen(this));
        }));
        this.addRenderableWidget(new BSMenuTextButton(startX, startY + rowHeight * 4, btnWidth, btnHeight, Component.literal("QUIT GAME"), btn -> {
            if (this.minecraft != null) this.minecraft.stop();
        }));
    }

    @Override
    public void render(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        
        guiGraphics.flush();
        guiGraphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.disableDepthTest();
        RenderSystem.disableBlend();
        guiGraphics.blit(BG_TEXTURE, 0, 0, this.width, this.height, 0, 0, 544, 416, 544, 416);
        RenderSystem.enableDepthTest();

        
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(0, 0, 10);

        BSGuiUtils.drawRMWindow(guiGraphics, rmBoxX, rmBoxY, rmBoxWidth, rmBoxHeight,
                Math.max(1, Math.round(16 * menuScale)), Math.max(1, Math.round(64 * menuScale)));

        guiGraphics.pose().popPose();

        
        int textPadding = Math.max(1, Math.round(2 * menuScale));
        int longest = LABELS.stream().mapToInt(label -> font.width(label) + 1).max().orElse(1);
        float textScale = Math.min(2 * menuScale, (rmBoxWidth - padding * 2.0F - textPadding * 2.0F) / longest);
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(0, 0, 20);
        for (var child : children()) if (child instanceof BSMenuTextButton button) button.setTextLayout(textScale, textPadding);
        super.render(guiGraphics, mouseX, mouseY, partialTicks);
        guiGraphics.flush();
        guiGraphics.pose().popPose();

        
        String authorText = "By @BiliBili HatsuYukiAya";
        guiGraphics.drawString(this.font, authorText, this.width - this.font.width(authorText) - 2, this.height - 10, 0xFFFFFF, true);
    }
}
