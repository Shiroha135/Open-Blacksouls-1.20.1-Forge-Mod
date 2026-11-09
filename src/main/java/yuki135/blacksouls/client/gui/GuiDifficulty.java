package yuki135.blacksouls.client.gui;

import yuki135.blacksouls.BlackSouls;
import yuki135.blacksouls.client.OriginalNpcPresentationController;
import yuki135.blacksouls.network.NetworkHandler;
import yuki135.blacksouls.network.packets.ClientboundOriginalNpcPresentationPacket;
import yuki135.blacksouls.network.packets.PacketSetDifficulty;
import yuki135.blacksouls.network.packets.ServerboundSimpleActionPacket;
import yuki135.blacksouls.util.DifficultyManager;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.glfw.GLFW;
import java.util.List;

public class GuiDifficulty extends net.minecraft.client.gui.screens.Screen {
    private static final int ROW_HEIGHT = 24;
    private static final int VISIBLE_ROWS = 6;
    private Stage stage = Stage.INTRO;
    private int revealedCodePoints;
    private int selected;
    private int listOffset;

    public GuiDifficulty() {
        super(Component.translatable("gui.blacksouls.difficulty.title"));
        NetworkHandler.sendToServer(new ServerboundSimpleActionPacket(
                ServerboundSimpleActionPacket.Action.VISIT_DIFFICULTY_STATUE));
    }

    @Override
    protected void init() { keepSelectionVisible(); }

    @Override
    public void tick() {
        if (stage != Stage.LEVELS && !isTextComplete()) revealedCodePoints += 3;
    }

    private float scale() { return height / 480.0F; }
    private int canvasWidth() { return Math.round(width / scale()); }
    private int messageTop() { return 360; }

    private List<Component> options() {
        if (stage == Stage.CONFIRM) return List.of(Component.translatable("gui.blacksouls.difficulty.yes"),
                Component.translatable("gui.blacksouls.difficulty.no"));
        return java.util.stream.IntStream.rangeClosed(0, 9)
                .mapToObj(level -> (Component) Component.translatable("gui.blacksouls.difficulty.lv", level)).toList();
    }

    private int menuWidth() {
        int widest = options().stream().mapToInt(font::width).max().orElse(0) * 2 + 8;
        return Math.min(canvasWidth(), Math.max(96, widest) + 24);
    }

    private int menuTop() {
        return messageTop() - Math.min(VISIBLE_ROWS, options().size()) * ROW_HEIGHT - 24;
    }

    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        graphics.pose().pushPose();
        graphics.pose().scale(scale(), scale(), 1);
        int canvasWidth = canvasWidth();
        if (stage != Stage.LEVELS) {
            int top = messageTop();
            graphics.fillGradient(0, top, canvasWidth, top + 12, 0, 0xA0000000);
            graphics.fill(0, top + 12, canvasWidth, 468, 0xA0000000);
            graphics.fillGradient(0, 468, canvasWidth, 480, 0xA0000000, 0);
            graphics.pose().pushPose();
            graphics.pose().scale(2, 2, 1);
            int y = (top + 16) / 2;
            for (var line : font.split(Component.literal(visibleText()), Math.max(1, (canvasWidth - 24) / 2))) {
                graphics.drawString(font, line, 6, y, 0xFFFFFFFF, true);
                y += 12;
            }
            graphics.pose().popPose();
            if (stage == Stage.INTRO && isTextComplete()) graphics.drawCenteredString(font, "▼", canvasWidth / 2, 468, 0xFFFFFFFF);
        }
        if (stage == Stage.LEVELS || stage == Stage.CONFIRM && isTextComplete()) {
            var options = options();
            int rows = Math.min(VISIBLE_ROWS, options.size());
            int left = canvasWidth - menuWidth();
            int top = menuTop();
            BSGuiUtils.drawRMWindow(graphics, left, top, menuWidth(), rows * ROW_HEIGHT + 24);
            for (int row = 0; row < rows; row++) {
                int index = row + listOffset;
                int y = top + 12 + row * ROW_HEIGHT;
                if (index == selected) BSGuiUtils.drawRMSelection(graphics, left + 12, y, menuWidth() - 24, ROW_HEIGHT);
                graphics.pose().pushPose();
                graphics.pose().scale(2, 2, 1);
                graphics.drawString(font, options.get(index), (left + 16) / 2, (y + 4) / 2, 0xFFFFFFFF, true);
                graphics.pose().popPose();
            }
            if (listOffset > 0) graphics.drawCenteredString(font, "▲", left + menuWidth() / 2, top + 2, 0xFFFFFFFF);
            if (listOffset + rows < options.size()) graphics.drawCenteredString(font, "▼", left + menuWidth() / 2, messageTop() - 10, 0xFFFFFFFF);
        }
        graphics.pose().popPose();
    }

    @Override
    public void mouseMoved(double mouseX, double mouseY) {
        if (stage == Stage.INTRO || stage == Stage.CONFIRM && !isTextComplete()) return;
        int row = hoveredRow(mouseX, mouseY);
        if (row >= 0 && selected != row) { selected = row; play(BlackSouls.CURSOR1_EVENT.get()); }
    }

    private int hoveredRow(double mouseX, double mouseY) {
        double x = mouseX / scale(), y = mouseY / scale();
        int row = (int) Math.floor((y - menuTop() - 12) / ROW_HEIGHT);
        return x >= canvasWidth() - menuWidth() + 12 && x < canvasWidth() - 12
                && row >= 0 && row < Math.min(VISIBLE_ROWS, options().size()) ? listOffset + row : -1;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == GLFW.GLFW_MOUSE_BUTTON_RIGHT) return cancel();
        if (button != GLFW.GLFW_MOUSE_BUTTON_LEFT) return false;
        if (stage != Stage.LEVELS && !isTextComplete()) { revealAll(); return true; }
        if (stage == Stage.INTRO) return confirm();
        int row = hoveredRow(mouseX, mouseY);
        if (row >= 0) { selected = row; return confirm(); }
        return true;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (stage == Stage.LEVELS && delta != 0) moveSelection(delta > 0 ? -1 : 1);
        return true;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) return cancel();
        if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER || keyCode == GLFW.GLFW_KEY_SPACE
                || keyCode == GLFW.GLFW_KEY_Z) return confirm();
        if (stage == Stage.LEVELS || stage == Stage.CONFIRM && isTextComplete()) {
            if (keyCode == GLFW.GLFW_KEY_UP || keyCode == GLFW.GLFW_KEY_W) moveSelection(-1);
            if (keyCode == GLFW.GLFW_KEY_DOWN || keyCode == GLFW.GLFW_KEY_S) moveSelection(1);
            if (keyCode == GLFW.GLFW_KEY_PAGE_DOWN) moveSelection(VISIBLE_ROWS);
            if (keyCode == GLFW.GLFW_KEY_PAGE_UP) moveSelection(-VISIBLE_ROWS);
        }
        return true;
    }

    private void moveSelection(int delta) {
        selected = Math.floorMod(selected + delta, options().size());
        keepSelectionVisible();
        play(BlackSouls.CURSOR1_EVENT.get());
    }

    private void keepSelectionVisible() {
        if (selected < listOffset) listOffset = selected;
        if (selected >= listOffset + VISIBLE_ROWS) listOffset = selected - VISIBLE_ROWS + 1;
        listOffset = Math.max(0, Math.min(listOffset, options().size() - Math.min(VISIBLE_ROWS, options().size())));
    }

    private boolean confirm() {
        if (stage != Stage.LEVELS && !isTextComplete()) { revealAll(); return true; }
        play(BlackSouls.SWORD1_EVENT.get());
        if (stage == Stage.INTRO) setStage(Stage.CONFIRM);
        else if (stage == Stage.CONFIRM) {
            if (selected == 0) setStage(Stage.LEVELS); else onClose();
        } else {
            NetworkHandler.sendToServer(new PacketSetDifficulty(selected));
            if (minecraft != null && minecraft.player != null && minecraft.level != null) {
                OriginalNpcPresentationController.apply(new ClientboundOriginalNpcPresentationPacket(224,
                        minecraft.player.getId(), minecraft.level.dimension().location(), List.of(0D, 0D, 0D, 255D, 20D), ""));
            }
            onClose();
        }
        return true;
    }

    private boolean cancel() {
        if (stage == Stage.LEVELS) { play(BlackSouls.SWORD3_EVENT.get()); return true; }
        play(BlackSouls.SWORD3_EVENT.get());
        onClose();
        return true;
    }

    private void setStage(Stage value) {
        stage = value; revealedCodePoints = 0; selected = 0; listOffset = 0;
    }

    private String fullText() {
        if (stage == Stage.INTRO) {
            String text = Component.translatable("gui.blacksouls.difficulty.deaths", DifficultyManager.deathCount).getString();
            return DifficultyManager.loopCount > 0 ? text + "\n\n\n" + Component.translatable(
                    "gui.blacksouls.difficulty.loops", DifficultyManager.loopCount).getString() : text;
        }
        return Component.translatable("gui.blacksouls.difficulty.ask").getString() + "\n"
                + Component.translatable("gui.blacksouls.difficulty.explain").getString() + "\n"
                + Component.translatable("gui.blacksouls.difficulty.current_original", DifficultyManager.currentDifficulty).getString();
    }

    private String visibleText() {
        String full = fullText();
        int count = Math.min(revealedCodePoints, full.codePointCount(0, full.length()));
        return full.substring(0, full.offsetByCodePoints(0, count));
    }

    private boolean isTextComplete() {
        String full = fullText();
        return revealedCodePoints >= full.codePointCount(0, full.length());
    }

    private void revealAll() {
        String full = fullText(); revealedCodePoints = full.codePointCount(0, full.length());
    }

    private void play(net.minecraft.sounds.SoundEvent sound) {
        if (minecraft != null) minecraft.getSoundManager().play(SimpleSoundInstance.forUI(sound, 1, 1));
    }

    @Override
    public boolean isPauseScreen() { return false; }
    private enum Stage { INTRO, CONFIRM, LEVELS }

    @net.minecraftforge.fml.common.Mod.EventBusSubscriber(modid="blacksouls", value=net.minecraftforge.api.distmarker.Dist.CLIENT)
    public static final class Presentation {
        @net.minecraftforge.eventbus.api.SubscribeEvent
        public static void hud(net.minecraftforge.client.event.RenderGuiEvent.Pre event) {
            if (net.minecraft.client.Minecraft.getInstance().screen instanceof GuiDifficulty) event.setCanceled(true);
        }

        @net.minecraftforge.eventbus.api.SubscribeEvent
        public static void hand(net.minecraftforge.client.event.RenderHandEvent event) {
            if (net.minecraft.client.Minecraft.getInstance().screen instanceof GuiDifficulty) event.setCanceled(true);
        }

        @net.minecraftforge.eventbus.api.SubscribeEvent(priority=net.minecraftforge.eventbus.api.EventPriority.HIGHEST)
        public static void player(net.minecraftforge.client.event.RenderPlayerEvent.Pre event) {
            var minecraft = net.minecraft.client.Minecraft.getInstance();
            if (minecraft.screen instanceof GuiDifficulty && event.getEntity() == minecraft.player) event.setCanceled(true);
        }
    }
}
