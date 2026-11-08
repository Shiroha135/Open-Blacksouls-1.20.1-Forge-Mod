package yuki135.blacksouls.client.gui;

import yuki135.blacksouls.BlackSouls;
import yuki135.blacksouls.client.ClientSkillInfo;
import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("removal")
public class GuiAvatarSelect extends Screen {

    private static final int VISIBLE_ROWS = 3;
    private static final int COLS = 4;
    private static final int AVATAR_SIZE = 60;
    private static final int SPACING = 80;

    private static final int FACE_SHEET_WIDTH = 384;
    private static final int FACE_SHEET_HEIGHT = 192;
    private static final int FACE_CELL_SIZE = 96;
    private static final int FACE_SHEET_COLS = 4;
    private static final int FACE_SHEET_MAX_INDEX = 7;

    private final int guiWidth = 360;
    private final int guiHeight = 280;

    private int guiLeft;
    private int guiTop;
    private int scrollOffset = 0;
    private int maxScroll = 0;
    private List<String> visibleAvatars = List.of();
    private List<ResourceLocation> visibleAvatarTextures = List.of();

    private final java.util.Map<String, Integer> expressionMap = new java.util.HashMap<>();

    private int getExpression(String avatarId) {
        return expressionMap.getOrDefault(avatarId, 0);
    }

    private void setExpression(String avatarId, int value) {
        expressionMap.put(avatarId, value);
    }

    private final Screen parentScreen;

    public GuiAvatarSelect(Screen parentScreen) {
        super(Component.literal("Avatar Select"));
        this.parentScreen = parentScreen;
    }

    @Override
    protected void init() {
        super.init();
        this.guiLeft = (this.width - this.guiWidth) / 2;
        this.guiTop = (this.height - this.guiHeight) / 2;
        refreshVisibleAvatars(ClientSkillInfo.getUnlockedAvatars());
    }

    @Override
    public void tick() {
        super.tick();
        var avatars=ClientSkillInfo.getUnlockedAvatars();
        if(!avatars.equals(visibleAvatars))refreshVisibleAvatars(avatars);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (maxScroll > 0) {
            if (delta > 0) {
                scrollOffset--;
            } else if (delta < 0) {
                scrollOffset++;
            }

            scrollOffset = net.minecraft.util.Mth.clamp(scrollOffset, 0, maxScroll);
            return true;
        }

        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    @Override
    public void render(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        this.renderBackground(guiGraphics);

        BSGuiUtils.drawRMWindow(guiGraphics, guiLeft, guiTop, guiWidth, guiHeight);

        String title = I18n.get("gui.blacksouls.avatar.title");
        guiGraphics.drawString(
                this.font,
                title,
                this.width / 2 - this.font.width(title) / 2,
                guiTop + 15,
                0xFFFFFF,
                false
        );

        int startX = guiLeft + 25;
        int startY = guiTop + 40;

        List<String> avatars = this.visibleAvatars;

        for (int i = 0; i < avatars.size(); i++) {
            int row = i / COLS;
            int col = i % COLS;

            if (row < scrollOffset || row >= scrollOffset + VISIBLE_ROWS) {
                continue;
            }

            int drawX = startX + (col * SPACING);
            int drawY = startY + ((row - scrollOffset) * SPACING);

            String avatarId = avatars.get(i);
            ResourceLocation tex = this.visibleAvatarTextures.get(i);

            boolean hovered = mouseX >= drawX && mouseX <= drawX + AVATAR_SIZE
                    && mouseY >= drawY && mouseY <= drawY + AVATAR_SIZE;


            if (hovered) {
                guiGraphics.fill(
                        drawX - 2,
                        drawY - 2,
                        drawX + AVATAR_SIZE + 2,
                        drawY + AVATAR_SIZE + 2,
                        0x66FFFFFF
                );
            }

            if (avatarId.equals(ClientSkillInfo.getAvatar())) {
                guiGraphics.fill(
                        drawX - 2,
                        drawY - 2,
                        drawX + AVATAR_SIZE + 2,
                        drawY + AVATAR_SIZE + 2,
                        0x88FFFF00
                );
            }

            RenderSystem.enableBlend();
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

            drawAvatar(guiGraphics, tex, avatarId, drawX, drawY);

            RenderSystem.disableBlend();

            String displayNameKey = "avatar.blacksouls." + avatarId;
            String displayName = I18n.exists(displayNameKey) ? I18n.get(displayNameKey) : avatarId;

            guiGraphics.pose().pushPose();
            guiGraphics.pose().scale(0.6F, 0.6F, 1.0F);

            int textX = (int) ((drawX + AVATAR_SIZE / 2.0F) / 0.6F) - this.font.width(displayName) / 2;
            int textY = (int) ((drawY + AVATAR_SIZE + 5) / 0.6F);

            guiGraphics.drawString(this.font, displayName, textX, textY, 0xAAAAAA, false);

            guiGraphics.pose().popPose();
        }

        super.render(guiGraphics, mouseX, mouseY, partialTicks);
    }

    private void drawAvatar(GuiGraphics guiGraphics, ResourceLocation tex, String avatarId, int drawX, int drawY) {
        if (isExpressionSheet(avatarId)) {
            int expression = net.minecraft.util.Mth.clamp(getExpression(avatarId), 0, FACE_SHEET_MAX_INDEX);
            int cell=avatarId.endsWith("_mz_sheet")?144:FACE_CELL_SIZE;
            int u = (expression % FACE_SHEET_COLS) * cell;
            int v = (expression / FACE_SHEET_COLS) * cell;

            guiGraphics.blit(
                    tex,
                    drawX,
                    drawY,
                    AVATAR_SIZE,
                    AVATAR_SIZE,
                    u,
                    v,
                    cell,
                    cell,
                    cell*4,
                    cell*2
            );
        } else {
            guiGraphics.blit(
                    tex,
                    drawX,
                    drawY,
                    AVATAR_SIZE,
                    AVATAR_SIZE,
                    0,
                    0,
                    96,
                    96,
                    96,
                    96
            );
        }
    }

    private boolean isExpressionSheet(String avatarId) {
        return avatarId.endsWith("_sheet");
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && this.minecraft != null) {
            int startX = guiLeft + 25;
            int startY = guiTop + 40;

            List<String> avatars = this.visibleAvatars;

            for (int i = 0; i < avatars.size(); i++) {
                int row = i / COLS;
                int col = i % COLS;

                if (row < scrollOffset || row >= scrollOffset + VISIBLE_ROWS) {
                    continue;
                }

                int drawX = startX + (col * SPACING);
                int drawY = startY + ((row - scrollOffset) * SPACING);

                if (mouseX >= drawX && mouseX <= drawX + AVATAR_SIZE
                        && mouseY >= drawY && mouseY <= drawY + AVATAR_SIZE) {

                    this.minecraft.getSoundManager().play(
                            SimpleSoundInstance.forUI(BlackSouls.CURSOR1_EVENT.get(), 1.0F, 1.0F)
                    );

                    String selectedAvatar = avatars.get(i);

                    ClientSkillInfo.setAvatar(selectedAvatar);

                    if (selectedAvatar.endsWith("_sheet")) {
                        ClientSkillInfo.setAvatarExpression(getExpression(selectedAvatar));
                    } else {
                        ClientSkillInfo.setAvatarExpression(0);
                    }
                    this.minecraft.setScreen(this.parentScreen);
                    return true;
                }
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    private String getHoveredAvatar(double mouseX, double mouseY) {
        int startX = guiLeft + 25;
        int startY = guiTop + 40;

        List<String> avatars = this.visibleAvatars;

        for (int i = 0; i < avatars.size(); i++) {
            int row = i / COLS;
            int col = i % COLS;

            if (row < scrollOffset || row >= scrollOffset + VISIBLE_ROWS) {
                continue;
            }

            int drawX = startX + (col * SPACING);
            int drawY = startY + ((row - scrollOffset) * SPACING);

            if (mouseX >= drawX && mouseX <= drawX + AVATAR_SIZE
                    && mouseY >= drawY && mouseY <= drawY + AVATAR_SIZE) {
                return avatars.get(i);
            }
        }

        return null;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (this.minecraft != null) {
            double mouseX = this.minecraft.mouseHandler.xpos() * this.width / this.minecraft.getWindow().getScreenWidth();
            double mouseY = this.minecraft.mouseHandler.ypos() * this.height / this.minecraft.getWindow().getScreenHeight();

            String hoveredAvatar = getHoveredAvatar(mouseX, mouseY);

            if (hoveredAvatar != null && isExpressionSheet(hoveredAvatar)) {
                int current = getExpression(hoveredAvatar);
                int next = current;

                if (keyCode == InputConstants.KEY_RIGHT) {
                    next = (current + 1) % 8;
                } else if (keyCode == InputConstants.KEY_LEFT) {
                    next = (current + 7) % 8;
                }

                if (next != current) {
                    setExpression(hoveredAvatar, next);

                    if (hoveredAvatar.equals(ClientSkillInfo.getAvatar())) {
                        ClientSkillInfo.setAvatarExpression(next);
                    }

                    return true;
                }
            }

            if (keyCode == InputConstants.KEY_ESCAPE
                    || this.minecraft.options.keyInventory.matches(keyCode, scanCode)) {
                this.minecraft.setScreen(this.parentScreen);
                return true;
            }
        }

        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private void refreshVisibleAvatars(List<String> avatars) {
        ArrayList<ResourceLocation> textures = new ArrayList<>(avatars.size());
        for (String avatarId : avatars) {
            textures.add(new ResourceLocation(BlackSouls.MODID, "textures/gui/avatars/" + avatarId + ".png"));
        }

        this.visibleAvatars = List.copyOf(avatars);
        this.visibleAvatarTextures = List.copyOf(textures);
        int totalRows = (this.visibleAvatars.size() + COLS - 1) / COLS;
        this.maxScroll = Math.max(0, totalRows - VISIBLE_ROWS);
        this.scrollOffset = net.minecraft.util.Mth.clamp(this.scrollOffset, 0, this.maxScroll);
    }
}
