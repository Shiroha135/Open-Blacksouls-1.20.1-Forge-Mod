package yuki135.blacksouls.client.gui;

import yuki135.blacksouls.BlackSouls;
import yuki135.blacksouls.capability.BSPlayerStats;
import yuki135.blacksouls.client.render.DevGlassRenderer;
import yuki135.blacksouls.network.NetworkHandler;
import yuki135.blacksouls.network.packets.PacketDevAction;
import yuki135.blacksouls.network.packets.PacketDevSetStats;
import yuki135.blacksouls.network.packets.ClientboundDevBonfiresPacket;
import yuki135.blacksouls.network.packets.ServerboundDevBonfirePacket;
import yuki135.blacksouls.util.skill.SkillRegistry;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Comparator;
import java.util.Locale;

public class GuiDevPanel extends Screen {
    private enum Tab {
        ATTRIBUTES("gui.blacksouls.dev_panel.tab.attributes"),
        SWITCHES("gui.blacksouls.dev_panel.tab.switches"),
        SKILLS("gui.blacksouls.dev_panel.tab.skills"),
        BONFIRES("gui.blacksouls.dev_panel.tab.bonfires"),
        STORY("剧情 / 事件测试");

        private final String key;

        Tab(String key) {
            this.key = key;
        }
    }

    private record FieldSpec(EditBox box, Component label, int color) {
    }

    private final List<FieldSpec> fields = new ArrayList<>();
    private EditBox fieldLevel;
    private EditBox fieldHp;
    private EditBox fieldMp;
    private EditBox fieldAtk;
    private EditBox fieldDef;
    private EditBox fieldMAtk;
    private EditBox fieldMDef;
    private EditBox fieldLuck;
    private EditBox fieldSpeed;
    private EditBox fieldSouls;
    private EditBox fieldSen;
    private Tab activeTab = Tab.ATTRIBUTES;
    private int panelX;
    private int panelY;
    private int panelWidth;
    private int panelHeight;
    private int navWidth;
    private float noCooldownAnimation;
    private float limitBreakAnimation;
    private Component status = Component.empty();
    private long statusUntil;
    private EditBox bonfireSearch;
    private List<ClientboundDevBonfiresPacket.Entry> bonfires = List.of();
    private List<ClientboundDevBonfiresPacket.Entry> filteredBonfires = List.of();
    private GlobalPos selectedBonfire;
    private int bonfireScroll;
    private boolean bonfiresLoading;
    private boolean bonfiresRequested;

    public GuiDevPanel() {
        super(Component.translatable("gui.blacksouls.dev_panel.title"));
    }

    @Override
    protected void init() {
        super.init();
        this.panelWidth = Math.max(360, Math.min(620, this.width - 24));
        this.panelWidth = Math.min(this.panelWidth, this.width - 8);
        this.panelHeight = Math.max(214, Math.min(370, this.height - 24));
        this.panelHeight = Math.min(this.panelHeight, this.height - 8);
        this.panelX = (this.width - this.panelWidth) / 2;
        this.panelY = (this.height - this.panelHeight) / 2;
        this.navWidth = Math.max(104, Math.min(136, this.panelWidth / 3));
        this.clearWidgets();
        this.fields.clear();

        BSPlayerStats stats = getStats();
        this.fieldLevel = createField(stats == null ? "1" : String.valueOf(stats.level));
        this.fieldHp = createField(stats == null ? "0" : trim(stats.bonusHp));
        this.fieldMp = createField(stats == null ? "0" : trim(stats.bonusMp));
        this.fieldAtk = createField(stats == null ? "0" : trim(stats.bonusAtk));
        this.fieldDef = createField(stats == null ? "0" : trim(stats.bonusDef));
        this.fieldMAtk = createField(stats == null ? "0" : trim(stats.bonusMatk));
        this.fieldMDef = createField(stats == null ? "0" : trim(stats.bonusMdef));
        this.fieldLuck = createField(stats == null ? "0" : trim(stats.bonusLuc));
        this.fieldSpeed = createField(stats == null ? "0" : trim(stats.bonusSpeed));
        this.fieldSouls = createField(stats == null ? "0" : String.valueOf(stats.souls));
        this.fieldSen = createField(stats == null ? "0" : String.valueOf(stats.sen));

        this.fields.add(new FieldSpec(fieldLevel, Component.translatable("gui.blacksouls.dev_panel.level"), 0xFFE8F4FF));
        this.fields.add(new FieldSpec(fieldHp, Component.translatable("gui.blacksouls.dev_panel.bonus_hp"), 0xFFFF8E9E));
        this.fields.add(new FieldSpec(fieldMp, Component.translatable("gui.blacksouls.dev_panel.bonus_mp"), 0xFF7AB8FF));
        this.fields.add(new FieldSpec(fieldAtk, Component.translatable("gui.blacksouls.dev_panel.bonus_atk"), 0xFFFFB36A));
        this.fields.add(new FieldSpec(fieldDef, Component.translatable("gui.blacksouls.dev_panel.bonus_def"), 0xFF8FD2FF));
        this.fields.add(new FieldSpec(fieldMAtk, Component.translatable("gui.blacksouls.dev_panel.bonus_matk"), 0xFFD990FF));
        this.fields.add(new FieldSpec(fieldMDef, Component.translatable("gui.blacksouls.dev_panel.bonus_mdef"), 0xFF84E5E8));
        this.fields.add(new FieldSpec(fieldLuck, Component.translatable("gui.blacksouls.dev_panel.bonus_luc"), 0xFFFFE47A));
        this.fields.add(new FieldSpec(fieldSpeed, Component.translatable("gui.blacksouls.dev_panel.bonus_speed"), 0xFF8EEA9C));
        this.fields.add(new FieldSpec(fieldSouls, Component.translatable("gui.blacksouls.dev_panel.souls"), 0xFFFFD56A));
        this.fields.add(new FieldSpec(fieldSen, Component.translatable("gui.blacksouls.dev_panel.sen"), 0xFFFF82C8));
        String search = this.bonfireSearch == null ? "" : this.bonfireSearch.getValue();
        this.bonfireSearch = createField("");
        this.bonfireSearch.setMaxLength(128);
        this.bonfireSearch.setValue(search);
        this.bonfireSearch.setHint(Component.translatable("gui.blacksouls.dev_panel.bonfires.search"));
        this.bonfireSearch.setX(panelX + navWidth + 18);
        this.bonfireSearch.setY(panelY + 63);
        this.bonfireSearch.setWidth(panelWidth - navWidth - 110);
        this.bonfireSearch.setResponder(value -> filterBonfires());
        layoutFields();
        updateFieldVisibility();
        this.noCooldownAnimation = stats != null && stats.developerNoCooldown ? 1.0F : 0.0F;
        this.limitBreakAnimation = stats != null && stats.developerLimitBreak ? 1.0F : 0.0F;
    }

    private EditBox createField(String value) {
        EditBox box = new EditBox(this.font, 0, 0, 72, 16, Component.empty());
        box.setValue(value);
        box.setBordered(false);
        box.setMaxLength(24);
        box.setTextColor(0xFFF6FAFF);
        box.setTextColorUneditable(0xFFB4C4D8);
        this.addRenderableWidget(box);
        return box;
    }

    private void layoutFields() {
        int contentX = panelX + navWidth + 14;
        int contentWidth = panelWidth - navWidth - 28;
        int columnGap = 10;
        int columnWidth = (contentWidth - columnGap) / 2;
        int labelWidth = Math.max(45, Math.min(62, columnWidth / 2));
        int fieldWidth = Math.max(48, columnWidth - labelWidth - 4);
        int startY = panelY + 62;
        int rowStep = Math.max(20, Math.min(28, (panelHeight - 92) / 6));
        for (int i = 0; i < fields.size(); i++) {
            int column = i < 6 ? 0 : 1;
            int row = i < 6 ? i : i - 6;
            int x = contentX + column * (columnWidth + columnGap) + labelWidth;
            int y = startY + row * rowStep - 4;
            EditBox box = fields.get(i).box();
            box.setX(x);
            box.setY(y);
            box.setWidth(fieldWidth);
        }
    }

    private void updateFieldVisibility() {
        boolean visible = this.activeTab == Tab.ATTRIBUTES;
        for (FieldSpec field : this.fields) {
            field.box().visible = visible;
        }
        this.bonfireSearch.visible = this.activeTab == Tab.BONFIRES;
    }

    private BSPlayerStats getStats() {
        if (this.minecraft == null || this.minecraft.player == null) {
            return null;
        }
        return this.minecraft.player.getCapability(BSPlayerStats.CAPABILITY).resolve().orElse(null);
    }

    private void sendStats() {
        int level = parseInt(fieldLevel.getValue(), 1);
        double hp = parseDouble(fieldHp.getValue(), 0.0D);
        double mp = parseDouble(fieldMp.getValue(), 0.0D);
        double attack = parseDouble(fieldAtk.getValue(), 0.0D);
        double defense = parseDouble(fieldDef.getValue(), 0.0D);
        double magicAttack = parseDouble(fieldMAtk.getValue(), 0.0D);
        double magicDefense = parseDouble(fieldMDef.getValue(), 0.0D);
        double luck = parseDouble(fieldLuck.getValue(), 0.0D);
        double speed = parseDouble(fieldSpeed.getValue(), 0.0D);
        long souls = parseLong(fieldSouls.getValue(), 0L);
        int sen = parseInt(fieldSen.getValue(), 0);
        NetworkHandler.INSTANCE.sendToServer(new PacketDevSetStats(
                level, hp, mp, attack, defense, magicAttack, magicDefense, luck, speed, souls, sen
        ));
        setStatus(Component.translatable("gui.blacksouls.dev_panel.status.applied"));
        playClick();
    }

    private void resetStats() {
        fieldLevel.setValue("1");
        fieldHp.setValue("0");
        fieldMp.setValue("0");
        fieldAtk.setValue("0");
        fieldDef.setValue("0");
        fieldMAtk.setValue("0");
        fieldMDef.setValue("0");
        fieldLuck.setValue("0");
        fieldSpeed.setValue("0");
        fieldSouls.setValue("0");
        fieldSen.setValue("100");
        sendStats();
        setStatus(Component.translatable("gui.blacksouls.dev_panel.status.reset"));
    }

    private void setStatus(Component message) {
        this.status = message;
        this.statusUntil = System.currentTimeMillis() + 2600L;
    }

    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        DevGlassRenderer.beginFrame(graphics);
        graphics.fill(0, 0, this.width, this.height, 0x2A07101C);
        DevGlassRenderer.panel(graphics, panelX, panelY, panelWidth, panelHeight, 16.0F, 0.94F, true);
        DevGlassRenderer.fill(graphics, panelX + 8, panelY + 8, navWidth - 12, panelHeight - 16, 12.0F, 0x45101D2D);

        graphics.drawString(this.font, Component.literal("BLACKSOULS"), panelX + 18, panelY + 18, 0xFFEAF7FF, false);
        graphics.drawString(this.font, Component.translatable("gui.blacksouls.dev_panel.subtitle"), panelX + 18, panelY + 32, 0xFF77D9FF, false);
        renderTabs(graphics, mouseX, mouseY);

        int contentX = panelX + navWidth + 14;
        graphics.drawString(this.font, Component.translatable(activeTab.key), contentX, panelY + 18, 0xFFF7FBFF, false);
        graphics.drawString(this.font, Component.translatable("gui.blacksouls.dev_panel.tab_hint." + activeTab.name().toLowerCase()), contentX, panelY + 32, 0xFFA9B9CC, false);

        if (activeTab == Tab.ATTRIBUTES) {
            renderAttributes(graphics, mouseX, mouseY);
        } else if (activeTab == Tab.SWITCHES) {
            renderSwitches(graphics, mouseX, mouseY);
        } else if (activeTab == Tab.SKILLS) {
            renderSkills(graphics, mouseX, mouseY);
        } else {
            renderBonfires(graphics, mouseX, mouseY);
        }

        if (System.currentTimeMillis() < statusUntil && !status.getString().isEmpty()) {
            graphics.drawString(this.font, status, contentX, panelY + panelHeight - 16, 0xFF92F3D2, false);
        }
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    private void renderTabs(GuiGraphics graphics, int mouseX, int mouseY) {
        for (int i = 0; i < Tab.values().length; i++) {
            Tab tab = Tab.values()[i];
            int x = panelX + 16;
            int y = panelY + 64 + i * Math.min(36,(panelHeight-74)/Tab.values().length);
            int width = navWidth - 28;
            boolean selected = tab == activeTab;
            boolean hovered = inside(mouseX, mouseY, x, y, width, 28);
            DevGlassRenderer.fill(graphics, x, y, width, 28, 8.0F,
                    selected ? 0x704C91BA : hovered ? 0x493A6684 : 0x251A2B3C);
            graphics.drawString(this.font, tab==Tab.STORY?Component.literal(tab.key):Component.translatable(tab.key), x + 10, y + 10,
                    selected ? 0xFFFFFFFF : 0xFFC0CDDB, false);
        }
    }

    private void renderAttributes(GuiGraphics graphics, int mouseX, int mouseY) {
        for (FieldSpec field : fields) {
            EditBox box = field.box();
            int x = box.getX();
            int y = box.getY();
            int labelX = x - Math.max(45, Math.min(62, (panelWidth - navWidth - 38) / 4));
            boolean focused = box.isFocused();
            boolean hovered = inside(mouseX, mouseY, x - 4, y - 2, box.getWidth() + 8, box.getHeight() + 4);
            DevGlassRenderer.fill(graphics, x - 4, y - 2, box.getWidth() + 8, box.getHeight() + 4, 6.0F,
                    focused ? 0x6D315B78 : hovered ? 0x4B29475D : 0x35162534);
            graphics.drawString(this.font, field.label(), labelX, y + 4, field.color(), false);
        }
        int contentX = panelX + navWidth + 14;
        int contentWidth = panelWidth - navWidth - 30;
        int buttonY = panelY + panelHeight - 38;
        int buttonWidth = Math.max(68, (contentWidth - 10) / 2);
        drawButton(graphics, contentX, buttonY, buttonWidth, 22,
                Component.translatable("gui.blacksouls.dev_panel.apply"), mouseX, mouseY, 0xFF70D8FF);
        drawButton(graphics, contentX + buttonWidth + 10, buttonY, buttonWidth, 22,
                Component.translatable("gui.blacksouls.dev_panel.reset"), mouseX, mouseY, 0xFFFF8EB4);
    }

    private void renderSwitches(GuiGraphics graphics, int mouseX, int mouseY) {
        BSPlayerStats stats = getStats();
        boolean noCooldown = stats != null && stats.developerNoCooldown;
        boolean limitBreak = stats != null && stats.developerLimitBreak;
        this.noCooldownAnimation = Mth.lerp(0.22F, this.noCooldownAnimation, noCooldown ? 1.0F : 0.0F);
        this.limitBreakAnimation = Mth.lerp(0.22F, this.limitBreakAnimation, limitBreak ? 1.0F : 0.0F);
        int contentX = panelX + navWidth + 14;
        int contentWidth = panelWidth - navWidth - 30;
        int firstY = panelY + 66;
        int cardHeight = Math.max(58, Math.min(78, (panelHeight - 104) / 2));
        renderToggleCard(graphics, contentX, firstY, contentWidth, cardHeight,
                Component.translatable("gui.blacksouls.dev_panel.no_cooldown"),
                Component.translatable("gui.blacksouls.dev_panel.no_cooldown.desc"),
                noCooldownAnimation, mouseX, mouseY);
        renderToggleCard(graphics, contentX, firstY + cardHeight + 12, contentWidth, cardHeight,
                Component.translatable("gui.blacksouls.dev_panel.limit_break"),
                Component.translatable("gui.blacksouls.dev_panel.limit_break.desc"),
                limitBreakAnimation, mouseX, mouseY);
    }

    private void renderToggleCard(GuiGraphics graphics, int x, int y, int width, int height,
                                  Component title, Component description, float progress, int mouseX, int mouseY) {
        boolean hovered = inside(mouseX, mouseY, x, y, width, height);
        DevGlassRenderer.fill(graphics, x, y, width, height, 10.0F, hovered ? 0x4D223B50 : 0x3517283A);
        graphics.drawString(this.font, title, x + 12, y + 12, 0xFFF5FAFF, false);
        int descriptionWidth = Math.max(80, width - 76);
        List<net.minecraft.util.FormattedCharSequence> lines = this.font.split(description, descriptionWidth);
        for (int i = 0; i < Math.min(2, lines.size()); i++) {
            graphics.drawString(this.font, lines.get(i), x + 12, y + 28 + i * 10, 0xFFA7B6C8, false);
        }
        int toggleX = x + width - 48;
        int toggleY = y + (height - 18) / 2;
        int trackColor = mixColor(0xFF354453, 0xFF4CC7E9, progress);
        DevGlassRenderer.fill(graphics, toggleX, toggleY, 36, 18, 9.0F, trackColor);
        float knobX = toggleX + 3.0F + progress * 18.0F;
        DevGlassRenderer.fill(graphics, knobX, toggleY + 3, 12, 12, 6.0F, 0xFFF4FBFF);
    }

    private void renderSkills(GuiGraphics graphics, int mouseX, int mouseY) {
        BSPlayerStats stats = getStats();
        int total = SkillRegistry.getSkillBookSkillIds().size();
        int learned = 0;
        if (stats != null) {
            for (String skillId : SkillRegistry.getSkillBookSkillIds()) {
                if (stats.unlockedSkills.contains(skillId)) {
                    learned++;
                }
            }
        }
        int contentX = panelX + navWidth + 14;
        int contentWidth = panelWidth - navWidth - 30;
        int cardY = panelY + 66;
        DevGlassRenderer.fill(graphics, contentX, cardY, contentWidth, 52, 10.0F, 0x3517283A);
        graphics.drawString(this.font, Component.translatable("gui.blacksouls.dev_panel.skills.progress"), contentX + 12, cardY + 11, 0xFFF4FAFF, false);
        graphics.drawString(this.font, Component.literal(learned + " / " + total), contentX + 12, cardY + 29, 0xFF75DEFF, false);
        int buttonY = cardY + 66;
        drawButton(graphics, contentX, buttonY, contentWidth, 32,
                Component.translatable("gui.blacksouls.dev_panel.skills.unlock_all"), mouseX, mouseY, 0xFF78E7C4);
        drawButton(graphics, contentX, buttonY + 44, contentWidth, 32,
                Component.translatable("gui.blacksouls.dev_panel.skills.forget_all"), mouseX, mouseY, 0xFFFF8FAE);
        graphics.drawString(this.font, Component.translatable("gui.blacksouls.dev_panel.skills.scope"),
                contentX + 4, buttonY + 84, 0xFF94A6BA, false);
    }

    private void drawButton(GuiGraphics graphics, int x, int y, int width, int height,
                            Component label, int mouseX, int mouseY, int accent) {
        boolean hovered = inside(mouseX, mouseY, x, y, width, height);
        DevGlassRenderer.fill(graphics, x, y, width, height, 8.0F, hovered ? 0x5E31546D : 0x3B1C3042);
        graphics.drawCenteredString(this.font, label, x + width / 2, y + (height - this.font.lineHeight) / 2, hovered ? 0xFFFFFFFF : 0xFFD8E3ED);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            for (int i = 0; i < Tab.values().length; i++) {
                int x = panelX + 16;
                int y = panelY + 64 + i * Math.min(36,(panelHeight-74)/Tab.values().length);
                if (inside(mouseX, mouseY, x, y, navWidth - 28, 28)) {
                    if(Tab.values()[i]==Tab.STORY){this.minecraft.setScreen(new GuiDevStory(this));return true;}
                    this.activeTab = Tab.values()[i];
                    this.setFocused(null);
                    updateFieldVisibility();
                    if (this.activeTab == Tab.BONFIRES && !bonfiresRequested) {
                        requestBonfires(false);
                    }
                    playClick();
                    return true;
                }
            }
            int contentX = panelX + navWidth + 14;
            int contentWidth = panelWidth - navWidth - 30;
            if (activeTab == Tab.ATTRIBUTES) {
                int buttonY = panelY + panelHeight - 38;
                int buttonWidth = Math.max(68, (contentWidth - 10) / 2);
                if (inside(mouseX, mouseY, contentX, buttonY, buttonWidth, 22)) {
                    sendStats();
                    return true;
                }
                if (inside(mouseX, mouseY, contentX + buttonWidth + 10, buttonY, buttonWidth, 22)) {
                    resetStats();
                    return true;
                }
            } else if (activeTab == Tab.SWITCHES) {
                int firstY = panelY + 66;
                int cardHeight = Math.max(58, Math.min(78, (panelHeight - 104) / 2));
                BSPlayerStats stats = getStats();
                if (stats != null && inside(mouseX, mouseY, contentX, firstY, contentWidth, cardHeight)) {
                    stats.developerNoCooldown = !stats.developerNoCooldown;
                    NetworkHandler.INSTANCE.sendToServer(new PacketDevAction(
                            PacketDevAction.Action.SET_NO_COOLDOWN, stats.developerNoCooldown
                    ));
                    setStatus(Component.translatable(stats.developerNoCooldown
                            ? "message.blacksouls.dev.no_cooldown.enabled"
                            : "message.blacksouls.dev.no_cooldown.disabled"));
                    playClick();
                    return true;
                }
                if (stats != null && inside(mouseX, mouseY, contentX, firstY + cardHeight + 12, contentWidth, cardHeight)) {
                    stats.developerLimitBreak = !stats.developerLimitBreak;
                    NetworkHandler.INSTANCE.sendToServer(new PacketDevAction(
                            PacketDevAction.Action.SET_LIMIT_BREAK, stats.developerLimitBreak
                    ));
                    setStatus(Component.translatable(stats.developerLimitBreak
                            ? "message.blacksouls.dev.limit_break.enabled"
                            : "message.blacksouls.dev.limit_break.disabled"));
                    playClick();
                    return true;
                }
            } else if (activeTab == Tab.SKILLS) {
                int cardY = panelY + 66;
                int buttonY = cardY + 66;
                if (inside(mouseX, mouseY, contentX, buttonY, contentWidth, 32)) {
                    NetworkHandler.INSTANCE.sendToServer(new PacketDevAction(
                            PacketDevAction.Action.UNLOCK_ALL_BOOK_SKILLS, true
                    ));
                    setStatus(Component.translatable("gui.blacksouls.dev_panel.status.skills_unlocked"));
                    playClick();
                    return true;
                }
                if (inside(mouseX, mouseY, contentX, buttonY + 44, contentWidth, 32)) {
                    NetworkHandler.INSTANCE.sendToServer(new PacketDevAction(
                            PacketDevAction.Action.FORGET_ALL_BOOK_SKILLS, false
                    ));
                    setStatus(Component.translatable("gui.blacksouls.dev_panel.status.skills_forgotten"));
                    playClick();
                    return true;
                }
            } else if (activeTab == Tab.BONFIRES) {
                if (inside(mouseX, mouseY, contentX + contentWidth - 64, panelY + 58, 64, 22)) {
                    if (!bonfiresLoading) {
                        requestBonfires(true);
                        playClick();
                    }
                    return true;
                }
                if (inside(mouseX, mouseY, contentX, panelY + panelHeight - 38, contentWidth, 22)) {
                    if (selectedBonfire != null && !bonfiresLoading) {
                        NetworkHandler.sendToServer(new ServerboundDevBonfirePacket(selectedBonfire));
                        playClick();
                        this.onClose();
                    }
                    return true;
                }
                int rows = visibleBonfireRows();
                if (inside(mouseX, mouseY, contentX, panelY + 92, contentWidth, rows * 34)) {
                    int index = bonfireScroll + ((int) mouseY - panelY - 92) / 34;
                    if (index >= 0 && index < filteredBonfires.size()) {
                        selectedBonfire = filteredBonfires.get(index).pos();
                        playClick();
                    }
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private void requestBonfires(boolean refresh) {
        bonfiresLoading = true;
        bonfiresRequested = true;
        NetworkHandler.sendToServer(new ServerboundDevBonfirePacket(refresh));
    }

    public void receiveBonfires(List<ClientboundDevBonfiresPacket.Entry> entries, boolean successful) {
        bonfiresLoading = false;
        bonfires = entries.stream().sorted(Comparator.comparing(entry -> bonfireName(entry.name()))).toList();
        filterBonfires();
        if (!successful) {
            setStatus(Component.translatable("gui.blacksouls.dev_panel.bonfires.failed"));
        }
    }

    private void filterBonfires() {
        String search = bonfireSearch == null ? "" : bonfireSearch.getValue().strip().toLowerCase(Locale.ROOT);
        filteredBonfires = bonfires.stream().filter(entry -> {
            var pos = entry.pos().pos();
            String text = bonfireName(entry.name()) + " " + entry.pos().dimension().location() + " "
                    + pos.getX() + " " + pos.getY() + " " + pos.getZ();
            return text.toLowerCase(Locale.ROOT).contains(search);
        }).toList();
        bonfireScroll = 0;
        if (filteredBonfires.stream().noneMatch(entry -> entry.pos().equals(selectedBonfire))) {
            selectedBonfire = filteredBonfires.isEmpty() ? null : filteredBonfires.get(0).pos();
        }
    }

    private static String bonfireName(String name) {
        return I18n.exists(name) ? I18n.get(name) : name;
    }

    private int visibleBonfireRows() {
        return Math.max(1, (panelHeight - 140) / 34);
    }

    private void renderBonfires(GuiGraphics graphics, int mouseX, int mouseY) {
        int x = panelX + navWidth + 14;
        int width = panelWidth - navWidth - 30;
        DevGlassRenderer.fill(graphics, x, panelY + 58, width - 74, 22, 6.0F,
                bonfireSearch.isFocused() ? 0x70315B78 : 0x45162534);
        drawButton(graphics, x + width - 64, panelY + 58, 64, 22,
                Component.translatable("gui.blacksouls.dev_panel.bonfires.refresh"), mouseX, mouseY, 0xFF70D8FF);
        int rows = visibleBonfireRows();
        int y = panelY + 92;
        if (bonfiresLoading || filteredBonfires.isEmpty()) {
            graphics.drawString(font, Component.translatable(bonfiresLoading
                    ? "gui.blacksouls.dev_panel.bonfires.loading" : "gui.blacksouls.dev_panel.bonfires.empty"),
                    x + 4, y + 10, 0xFFB6C7D8, false);
        }
        if (!bonfiresLoading) {
            graphics.enableScissor(x, y, x + width, y + rows * 34);
            for (int row = 0; row < rows && row + bonfireScroll < filteredBonfires.size(); row++) {
                var entry = filteredBonfires.get(row + bonfireScroll);
                int rowY = y + row * 34;
                boolean selected = entry.pos().equals(selectedBonfire);
                boolean hovered = inside(mouseX, mouseY, x, rowY, width, 30);
                DevGlassRenderer.fill(graphics, x, rowY, width - 6, 30, 7.0F,
                        selected ? 0x704C91BA : hovered ? 0x493A6684 : 0x251A2B3C);
                String state = I18n.get(entry.activated() ? "gui.blacksouls.dev_panel.bonfires.activated" : "gui.blacksouls.dev_panel.bonfires.inactive");
                graphics.drawString(font, font.plainSubstrByWidth(bonfireName(entry.name()), Math.max(20, width - font.width(state) - 32)),
                        x + 8, rowY + 5, 0xFFF4FAFF, false);
                graphics.drawString(font, state, x + width - font.width(state) - 14, rowY + 5,
                        entry.activated() ? 0xFF92E8C5 : 0xFFB5C5D6, false);
                var pos = entry.pos().pos();
                String location = entry.pos().dimension().location() + "  " + pos.getX() + ", " + pos.getY() + ", " + pos.getZ();
                graphics.drawString(font, font.plainSubstrByWidth(location, width - 24), x + 8, rowY + 17, 0xFFA9BDD0, false);
            }
            graphics.disableScissor();
            if (filteredBonfires.size() > rows) {
                float trackHeight = rows * 34.0F;
                float thumbHeight = Math.max(12.0F, trackHeight * rows / filteredBonfires.size());
                float thumbY = y + (trackHeight - thumbHeight) * bonfireScroll / (filteredBonfires.size() - rows);
                DevGlassRenderer.fill(graphics, x + width - 3, thumbY, 2, thumbHeight, 1.0F, 0xA877C6E8);
            }
        }
        drawButton(graphics, x, panelY + panelHeight - 38, width, 22,
                Component.translatable("gui.blacksouls.dev_panel.bonfires.teleport"), mouseX, mouseY, 0xFF70D8FF);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (activeTab == Tab.BONFIRES && inside(mouseX, mouseY, panelX + navWidth + 14, panelY + 92,
                panelWidth - navWidth - 30, visibleBonfireRows() * 34)) {
            bonfireScroll = Mth.clamp(bonfireScroll - (int) Math.signum(delta), 0,
                    Math.max(0, filteredBonfires.size() - visibleBonfireRows()));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    @Override
    public void removed() {
        DevGlassRenderer.release();
        super.removed();
    }

    private void playClick() {
        if (this.minecraft != null && BlackSouls.CURSOR1_EVENT != null && BlackSouls.CURSOR1_EVENT.isPresent()) {
            this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI(BlackSouls.CURSOR1_EVENT.get(), 1.0F, 1.0F));
        }
    }

    private static boolean inside(double mouseX, double mouseY, int x, int y, int width, int height) {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }

    private static int mixColor(int from, int to, float progress) {
        float value = Mth.clamp(progress, 0.0F, 1.0F);
        int alpha = Math.round(Mth.lerp(value, from >>> 24 & 255, to >>> 24 & 255));
        int red = Math.round(Mth.lerp(value, from >> 16 & 255, to >> 16 & 255));
        int green = Math.round(Mth.lerp(value, from >> 8 & 255, to >> 8 & 255));
        int blue = Math.round(Mth.lerp(value, from & 255, to & 255));
        return alpha << 24 | red << 16 | green << 8 | blue;
    }

    private static String trim(double value) {
        return value == Math.rint(value) ? Long.toString((long) value) : Double.toString(value);
    }

    private static int parseInt(String value, int fallback) {
        try {
            return Integer.parseInt(value);
        } catch (Exception ignored) {
            return fallback;
        }
    }

    private static long parseLong(String value, long fallback) {
        try {
            return Long.parseLong(value);
        } catch (Exception ignored) {
            return fallback;
        }
    }

    private static double parseDouble(String value, double fallback) {
        try {
            return Double.parseDouble(value);
        } catch (Exception ignored) {
            return fallback;
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (super.keyPressed(keyCode, scanCode, modifiers)) {
            return true;
        }
        if (this.getFocused() instanceof EditBox box && box.visible) {
            return false;
        }
        if (this.minecraft != null && this.minecraft.options.keyInventory.matches(keyCode, scanCode)) {
            this.onClose();
            return true;
        }
        return false;
    }
}
