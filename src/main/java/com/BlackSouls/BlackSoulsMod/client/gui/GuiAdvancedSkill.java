package com.BlackSouls.BlackSoulsMod.client.gui;

import com.BlackSouls.BlackSoulsMod.BlackSouls;
import com.BlackSouls.BlackSoulsMod.capability.BSPlayerStats;
import com.BlackSouls.BlackSoulsMod.client.ClientSkillInfo;
import com.BlackSouls.BlackSoulsMod.client.render.BSAvatarRenderer;
import com.BlackSouls.BlackSoulsMod.network.NetworkHandler;
import com.BlackSouls.BlackSoulsMod.network.packets.PacketBindSkill;
import com.BlackSouls.BlackSoulsMod.util.SkillUtils;
import com.BlackSouls.BlackSoulsMod.util.skill.AbstractSkill;
import com.BlackSouls.BlackSoulsMod.util.skill.SkillRegistry;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import com.mojang.blaze3d.platform.InputConstants;

import java.util.ArrayList;
import java.util.List;

public class GuiAdvancedSkill extends Screen {

    private static final ResourceLocation ICON_INVISIBLE = new ResourceLocation(BlackSouls.MODID, "textures/gui/skills/invisible.png");
    private static final ResourceLocation ICON_REQUIEM = new ResourceLocation(BlackSouls.MODID, "textures/gui/skills/requiem.png");
    private static final ResourceLocation ICON_GRIT = new ResourceLocation(BlackSouls.MODID, "textures/gui/skills/grit.png");
    private static final ResourceLocation ICON_SHOTGUN = new ResourceLocation(BlackSouls.MODID, "textures/gui/skills/gun.png");
    private static final ResourceLocation ICON_VORPAL = new ResourceLocation(BlackSouls.MODID, "textures/gui/skills/weapon.png");
    private static final ResourceLocation ICON_AURA_BLADE = new ResourceLocation(BlackSouls.MODID, "textures/gui/skills/weapon.png");
    private static final ResourceLocation ICON_WEAPON_BREAK = new ResourceLocation(BlackSouls.MODID, "textures/gui/skills/weapon.png");
    private static final ResourceLocation ICON_ARMOR_BREAK = new ResourceLocation(BlackSouls.MODID, "textures/gui/skills/weapon.png");
    private static final ResourceLocation ICON_DRAGON_SHOCKWAVE = new ResourceLocation(BlackSouls.MODID, "textures/gui/skills/dragon_shockwave.png");
    private static final ResourceLocation ICON_DIFFICULTY = new ResourceLocation(BlackSouls.MODID, "textures/gui/skills/difficulty.png");
    private static final ResourceLocation ICON_RADIANT_BLADE = new ResourceLocation(BlackSouls.MODID, "textures/gui/skills/weapon.png");
    private static final ResourceLocation ICON_HELLFIRE_BLADE = new ResourceLocation(BlackSouls.MODID, "textures/gui/skills/hellfire_blade.png");
    private static final ResourceLocation ICON_ULTIMATE_TRIPLE_SLASH = new ResourceLocation(BlackSouls.MODID, "textures/gui/skills/weapon.png");
    private static final ResourceLocation ICON_KNIGHTS_GLORY = new ResourceLocation(BlackSouls.MODID, "textures/gui/skills/knights_glory.png");
    private static final ResourceLocation ICON_REINFORCE = new ResourceLocation(BlackSouls.MODID, "textures/gui/skills/reinforce.png");
    private static final ResourceLocation ICON_SOUL_ARROW = new ResourceLocation(BlackSouls.MODID, "textures/gui/skills/soul_arrow.png");
    private static final ResourceLocation ICON_SOUL_LIGHT = new ResourceLocation(BlackSouls.MODID, "textures/gui/skills/soul_arrow.png");
    private static final ResourceLocation ICON_SOUL_RADIATION = new ResourceLocation(BlackSouls.MODID, "textures/gui/skills/soul_arrow.png");
    private static final ResourceLocation ICON_CARTHUS_BLOOD_CURSE = new ResourceLocation(BlackSouls.MODID, "textures/gui/skills/reinforce.png");
    private static final ResourceLocation ICON_CHRONO_CLOCK = new ResourceLocation(BlackSouls.MODID, "textures/gui/skills/chrono_clock.png");
    private static final Component TITLE = Component.translatable("gui.blacksouls.skill.title");
    private static final Component ORGANIZED = Component.translatable("gui.blacksouls.skill.organized");
    private static final Component UNDEAD = Component.translatable("gui.blacksouls.skill.undead");
    private int currentMouseX;
    private int currentMouseY;
    private final List<SkillButton> skillButtons = new ArrayList<>();

    private final int guiWidth = 360;
    private final int guiHeight = 260;
    private int guiLeft;
    private int guiTop;

    private final int topH = 75;
    private final int leftW = 100;

    private int scrollOffset = 0;
    private int maxScroll = 0;
    private static final int VISIBLE_ROWS = 5;

    public GuiAdvancedSkill() {
        super(TITLE);
    }

    @Override
    protected void init() {
        super.init();
        this.guiLeft = (this.width - this.guiWidth) / 2;
        this.guiTop = (this.height - this.guiHeight) / 2;

        this.skillButtons.clear();
        this.scrollOffset = 0;

        if (this.minecraft == null || this.minecraft.player == null) return;
        Player player = this.minecraft.player;

        if (SkillUtils.hasLearnedSkill(player, "bs2_skill_invisible_body")) addSkill("bs2_skill_invisible_body", ICON_INVISIBLE);
        if (SkillUtils.hasLearnedSkill(player, "bs2_skill_requiem")) addSkill("bs2_skill_requiem", ICON_REQUIEM);
        if (SkillUtils.hasLearnedSkill(player, "bs2_skill_grit")) addSkill("bs2_skill_grit", ICON_GRIT);

        ItemStack mainHand = player.getMainHandItem();
        ItemStack offHand = player.getOffhandItem();
        // 卡萨斯血咒学习判断
        if (SkillUtils.hasLearnedSkill(player, "bs2_skill_carthus_blood_curse")) {
            addSkill("bs2_skill_carthus_blood_curse", ICON_CARTHUS_BLOOD_CURSE);
        }

        // 魂之矢技能学习判断
        if (SkillUtils.hasLearnedSkill(player, "bs2_skill_soul_arrow")) {
            addSkill("bs2_skill_soul_arrow", ICON_SOUL_ARROW);
        }
        // 魂之光技能学习判断
        if (SkillUtils.hasLearnedSkill(player, "bs2_skill_soul_light")) {
            addSkill("bs2_skill_soul_light", ICON_SOUL_LIGHT);
        }
        // 魂之放射学习判断
        if (SkillUtils.hasLearnedSkill(player, "bs2_skill_soul_radiation")) {
            addSkill("bs2_skill_soul_radiation", ICON_SOUL_RADIATION);
        }
        // 副手判断霰弹铳给予枪火击溃判断
        if (!offHand.isEmpty() && offHand.getItem() == BlackSouls.MURDERERS_SHOTGUN.get()) {
            addSkill("bs2_skill_shotgun_blast", ICON_SHOTGUN);
        }
        // 主手判断持有沃柏尔之刃给予三段斩判断
        if (!mainHand.isEmpty() && mainHand.getItem() == BlackSouls.VORPAL_BLADE.get()) {
            addSkill("bs2_skill_vorpal_slash", ICON_VORPAL);
        }
        // 主手判断持有安多鲁之剑给予灵气刃判断
        if (!mainHand.isEmpty() && mainHand.getItem() == BlackSouls.ANDOR_SWORD.get()) {
            addSkill("bs2_skill_aura_blade", ICON_AURA_BLADE);
            // 如果强化到+5，额外添加辉耀之剑
            int upgradeLevel = 0;
            if (mainHand.hasTag() && mainHand.getTag().contains("bs2_upgrade_level")) {
                upgradeLevel = mainHand.getTag().getInt("bs2_upgrade_level");
            }
            if (upgradeLevel >= 5) {
                addSkill("bs2_skill_radiant_blade", ICON_RADIANT_BLADE);
            }
        }
        // 让骑士王之剑和骑士之剑共享武器击溃+护甲击溃
        if (!mainHand.isEmpty() && (mainHand.getItem() == BlackSouls.KNIGHT_SWORD.get() || mainHand.getItem() == BlackSouls.KNIGHT_KING_SWORD.get())) {
            addSkill("bs2_skill_weapon_break", ICON_WEAPON_BREAK);
            addSkill("bs2_skill_armor_break", ICON_ARMOR_BREAK);
        }

        // 主手判断持有骑士王之剑给予骑士的荣耀判断
        if (!mainHand.isEmpty() && mainHand.getItem() == BlackSouls.KNIGHT_KING_SWORD.get()) {
            addSkill("bs2_skill_knights_glory", ICON_KNIGHTS_GLORY);
        }
        // 主手判断持有飞龙剑给予龙之冲击波判断
        if (!mainHand.isEmpty() && mainHand.getItem() == BlackSouls.DRAKE_SWORD.get()) {
            addSkill("bs2_skill_dragon_shockwave", ICON_DRAGON_SHOCKWAVE);
            // 如果强化到+5，额外添加狱息炎剑
            int upgradeLevel = mainHand.hasTag() ? mainHand.getTag().getInt("bs2_upgrade_level") : 0;
            if (upgradeLevel >= 5) {
                addSkill("bs2_skill_hellfire_blade", ICON_HELLFIRE_BLADE);
            }
        }
        // 主手判断持有勇剑沃柏尔给予究极三段斩判断
        if (!mainHand.isEmpty() && mainHand.getItem() == BlackSouls.BRAVE_SWORD_VORPAL.get()) {
            addSkill("bs2_skill_ultimate_triple_slash", ICON_ULTIMATE_TRIPLE_SLASH);
        }

        // 勇剑沃柏尔+沃柏尔之刃都拥有强化附加技能
        if (!mainHand.isEmpty() && (mainHand.getItem() == BlackSouls.BRAVE_SWORD_VORPAL.get() || mainHand.getItem() == BlackSouls.VORPAL_BLADE.get())) {
            addSkill("bs2_skill_reinforce", ICON_REINFORCE);
        }
        // 所有角色自带难度设置技能
        if (SkillUtils.hasLearnedSkill(player, "bs2_skill_difficulty")) addSkill("bs2_skill_difficulty", ICON_DIFFICULTY);
        if (SkillUtils.hasChronoClockEquipped(player)) addSkill("bs2_skill_chrono_clock", ICON_CHRONO_CLOCK);

        // 开放 API：遍历 SkillRegistry 中所有 isUnlockedForGUI=true 的技能，自动加入 GUI。
        // 附属 mod（如 Yuki）只需注册 AbstractSkill 并 override isUnlockedForGUI/getIcon/getManaCost 即可。
        for (AbstractSkill skill : SkillRegistry.getAvailableSkills(player)) {
            String id = skill.getSkillId();
            // 跳过 BS 自带技能（上面已经手动 addSkill 过，避免重复）
            if (id.startsWith("bs2_skill_")) continue;
            ResourceLocation icon = skill.getIcon();
            addSkill(id, icon);
        }

        int totalRows = (int) Math.ceil(skillButtons.size() / 2.0);
        maxScroll = Math.max(0, totalRows - VISIBLE_ROWS);
    }

    private void addSkill(String id, ResourceLocation icon) {
        int index = skillButtons.size();
        int colsPerRow = 2;
        int col = index % colsPerRow;
        int row = index / colsPerRow;
        skillButtons.add(new SkillButton(id, icon, col, row));
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (maxScroll > 0) {
            if (delta < 0) scrollOffset++;
            else if (delta > 0) scrollOffset--;
            scrollOffset = Mth.clamp(scrollOffset, 0, maxScroll);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    @Override
    public void render(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        this.currentMouseX = mouseX;
        this.currentMouseY = mouseY;
        this.renderBackground(guiGraphics);

        if (this.minecraft == null || this.minecraft.player == null) return;
        Player player = this.minecraft.player;

        BSPlayerStats stats = player.getCapability(BSPlayerStats.CAPABILITY).resolve().orElse(null);
        if (stats == null) return;

        BSGuiUtils.drawRMWindow(guiGraphics, guiLeft, guiTop, leftW, topH);
        BSGuiUtils.drawRMWindow(guiGraphics, guiLeft + leftW, guiTop, guiWidth - leftW, topH);
        BSGuiUtils.drawRMWindow(guiGraphics, guiLeft, guiTop + topH, guiWidth, guiHeight - topH);

        guiGraphics.drawString(font, TITLE, guiLeft + 15, guiTop + 15, 0xFFFFFF, false);
        guiGraphics.fill(guiLeft + 10, guiTop + 30, guiLeft + leftW - 10, guiTop + 31, 0x55FFFFFF);
        guiGraphics.drawString(font, ORGANIZED, guiLeft + 15, guiTop + 40, 0xAAAAAA, false);

        int rightX = guiLeft + leftW;

        String avatarName = ClientSkillInfo.getAvatar();
        if (avatarName == null) avatarName = "default";
        ResourceLocation currentAvatarTex = BSAvatarRenderer.getTexture(avatarName);

        boolean isHoveringAvatar = mouseX >= rightX + 10 && mouseX <= rightX + 70 && mouseY >= guiTop + 7 && mouseY <= guiTop + 67;
        if (isHoveringAvatar) {
            guiGraphics.fill(rightX + 8, guiTop + 5, rightX + 72, guiTop + 69, 0x44FFFFFF);
        }

        RenderSystem.enableBlend();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        BSAvatarRenderer.draw(guiGraphics, currentAvatarTex, avatarName, rightX + 10, guiTop + 7, 60);
        RenderSystem.disableBlend();

        int textBaseX = rightX + 80;

        String playerName = player.getName().getString();
        int nameWidth = font.width(playerName);
        guiGraphics.drawString(font, playerName, textBaseX, guiTop + 15, 0xFFFFFF, false);
        guiGraphics.drawString(font, UNDEAD, textBaseX + nameWidth + 25, guiTop + 15, 0xFFFFFF, false);

        int tagColor = 0x5555FF;
        guiGraphics.drawString(font, "Lv", textBaseX, guiTop + 35, tagColor, false);
        guiGraphics.drawString(font, String.valueOf(stats.level), textBaseX + 20, guiTop + 35, 0xFFFFFF, false);

        int barStartX = textBaseX + 45;
        int barW = 100;
        int barH = 5;
        int yOffset = 4;

        int hpY = guiTop + 35;
        float effectiveMaxHp = player.getMaxHealth();
        double hpP = Math.max(0.0, Math.min(1.0, player.getHealth() / Math.max(1.0f, effectiveMaxHp)));
        guiGraphics.fill(barStartX, hpY + yOffset, barStartX + barW, hpY + yOffset + barH, 0xFF440000);
        guiGraphics.fill(barStartX, hpY + yOffset, barStartX + (int)(barW * hpP), hpY + yOffset + barH, 0xFFFF3333);
        guiGraphics.drawString(font, "HP", barStartX + 2, hpY, tagColor, true);
        String hpTxt = (int)player.getHealth() + " / " + (int)effectiveMaxHp;
        guiGraphics.drawString(font, hpTxt, barStartX + barW - font.width(hpTxt) - 2, hpY, 0xFFFFFF, true);
        int mpY = guiTop + 50;
        double mpP = Math.max(0.0, Math.min(1.0, stats.mp / Math.max(1.0, stats.maxMp)));
        guiGraphics.fill(barStartX, mpY + yOffset, barStartX + barW, mpY + yOffset + barH, 0xFF000044);
        guiGraphics.fill(barStartX, mpY + yOffset, barStartX + (int)(barW * mpP), mpY + yOffset + barH, 0xFF3333FF);
        guiGraphics.drawString(font, "MP", barStartX + 2, mpY, tagColor, true);
        String mpTxt = (int)stats.mp + " / " + (int)stats.maxMp;
        guiGraphics.drawString(font, mpTxt, barStartX + barW - font.width(mpTxt) - 2, mpY, 0xFFFFFF, true);

        if (maxScroll > 0) {
            int scrollX = guiLeft + guiWidth - 12;
            int scrollY = guiTop + topH + 15;
            int scrollHeight = VISIBLE_ROWS * 34;
            guiGraphics.fill(scrollX, scrollY, scrollX + 3, scrollY + scrollHeight, 0x55000000);
            int thumbHeight = Math.max(20, scrollHeight / (maxScroll + 1));
            int thumbY = scrollY + (int)(((float)scrollOffset / maxScroll) * (scrollHeight - thumbHeight));
            guiGraphics.fill(scrollX, thumbY, scrollX + 3, thumbY + thumbHeight, 0xFFFFFFFF);
        }

        super.render(guiGraphics, mouseX, mouseY, partialTicks);

        for (SkillButton btn : skillButtons) btn.draw(guiGraphics, mouseX, mouseY, stats);
        for (SkillButton btn : skillButtons) if (btn.isHovered(mouseX, mouseY)) drawSkillTooltip(guiGraphics, btn, mouseX, mouseY);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (this.minecraft == null || this.minecraft.player == null) return super.keyPressed(keyCode, scanCode, modifiers);

        if (keyCode == InputConstants.KEY_ESCAPE || this.minecraft.options.keyInventory.matches(keyCode, scanCode)) {
            this.minecraft.setScreen(new GuiPlayerStats());
            return true;
        }

        BSPlayerStats stats = this.minecraft.player.getCapability(BSPlayerStats.CAPABILITY).resolve().orElse(null);
        if (stats != null) {
            for (SkillButton btn : skillButtons) {
                if (btn.isHovered(this.currentMouseX, this.currentMouseY)) {
                    String keyName = switch (keyCode) {
                        case InputConstants.KEY_Z -> "Z";
                        case InputConstants.KEY_X -> "X";
                        case InputConstants.KEY_C -> "C";
                        case InputConstants.KEY_V -> "V";
                        default -> "";
                    };

                    if (!keyName.isEmpty()) {
                        this.minecraft.player.playSound(net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK.value(), 1.0F, 1.0F);

                        switch (keyName) {
                            case "Z" -> stats.skillZ = btn.skillId;
                            case "X" -> stats.skillX = btn.skillId;
                            case "C" -> stats.skillC = btn.skillId;
                            case "V" -> stats.skillV = btn.skillId;
                        }

                        try {
                            NetworkHandler.sendToServer(new PacketBindSkill(keyName, btn.skillId));
                        } catch (Exception ignored) {}
                        return true;
                    }
                }
            }
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    private String translationKeyFor(String skillId) {
        AbstractSkill skill = SkillRegistry.SKILLS.get(skillId);
        return skill != null ? skill.getTranslationKey() : "skill.blacksouls." + skillId + ".name";
    }

    private float getSkillCost(String skillId) {
        return switch (skillId) {
            case "bs2_skill_invisible_body" -> 20.0F;
            case "bs2_skill_requiem" -> 30.0F;
            case "bs2_skill_grit" -> 30.0F;
            case "bs2_skill_vorpal_slash" -> 5.0F;
            case "bs2_skill_shotgun_blast" -> 0.0F;
            case "bs2_skill_aura_blade" -> 15.0F;
            case "bs2_skill_dragon_shockwave" -> 15.0F;
            case "bs2_skill_difficulty" -> 0.0F;
            //新版技能蓝耗分割线
            case "bs2_skill_radiant_blade" -> 30.0F;//辉耀之剑蓝耗
            case "bs2_skill_knights_glory" -> 40.0F;//骑士的荣耀蓝耗
            case "bs2_skill_hellfire_blade" -> 30.0F;//狱息炎剑蓝耗
            case "bs2_skill_ultimate_triple_slash" -> 30.0F;// 究极三段斩蓝耗
            case "bs2_skill_soul_arrow" -> 6.0F; // 魂之矢蓝耗
            case "bs2_skill_soul_light" -> 12.0F;// 魂之光蓝耗
            case "bs2_skill_soul_radiation" -> 12.0F;// 魂之放射蓝耗
            case "bs2_skill_carthus_blood_curse" -> 0.0F;// 卡萨斯血咒蓝耗
            case "bs2_skill_chrono_clock" -> 0.0F;
            default -> {
                // 开放 API：从 SkillRegistry 查技能的 MP 蓝耗
                AbstractSkill skill = SkillRegistry.SKILLS.get(skillId);
                yield skill != null ? skill.getManaCost() : 10.0F;
            }
        };
    }

    private void drawSkillTooltip(GuiGraphics guiGraphics, SkillButton btn, int mouseX, int mouseY) {
        List<Component> lines = new ArrayList<>();
        // 开放 API：优先用 SkillRegistry 查 translationKey，找不到才回退到默认格式
        AbstractSkill skill = SkillRegistry.SKILLS.get(btn.skillId);
        String translationKey = skill != null ? skill.getTranslationKey() : "skill.blacksouls." + btn.skillId + ".name";
        lines.add(Component.translatable(translationKey).withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));

        float cost = getSkillCost(btn.skillId);
        lines.add(Component.translatable("gui.blacksouls.skill.mp_cost", String.valueOf((int)cost)).withStyle(ChatFormatting.AQUA));
        lines.add(Component.translatable("gui.blacksouls.skill.equip_hint").withStyle(ChatFormatting.GRAY));

        guiGraphics.renderComponentTooltip(font, lines, mouseX, mouseY);
    }

    private class SkillButton {
        String skillId;
        ResourceLocation icon;
        Component name;
        String costText;
        int col, row;
        int colWidth = 160;
        int size = 32;

        SkillButton(String id, ResourceLocation icon, int col, int row) {
            this.skillId = id; this.icon = icon; this.col = col; this.row = row;
            this.name = Component.translatable(translationKeyFor(id));
            this.costText = String.valueOf((int) getSkillCost(id));
        }

        boolean isHidden() { return row < scrollOffset || row >= scrollOffset + VISIBLE_ROWS; }
        int getX() { return guiLeft + 20 + (col * colWidth); }
        int getY() { return guiTop + topH + 15 + ((row - scrollOffset) * 34); }

        void draw(GuiGraphics guiGraphics, int mouseX, int mouseY, BSPlayerStats stats) {
            if (isHidden()) return;

            int x = getX(); int y = getY();
            boolean hovered = isHovered(mouseX, mouseY);

            if (hovered) {
                guiGraphics.fill(x - 2, y - 2, x + colWidth - 10, y + size + 2, 0x44FFFFFF);
            }

            String boundKeys = "";
            if (stats != null) {
                if (skillId.equals(stats.skillZ)) boundKeys += "[Z] ";
                if (skillId.equals(stats.skillX)) boundKeys += "[X] ";
                if (skillId.equals(stats.skillC)) boundKeys += "[C] ";
                if (skillId.equals(stats.skillV)) boundKeys += "[V] ";
            }

            RenderSystem.enableBlend();
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
            guiGraphics.blit(icon, x, y, size, size, 0.0F, 0.0F, size, size, size, size);
            RenderSystem.disableBlend();

            int nameWidth = font.width(name);
            guiGraphics.drawString(font, name, x + size + 8, y + 12, hovered ? 0xFFFF00 : 0xFFFFFF, false);

            if (!boundKeys.isEmpty()) {
                guiGraphics.drawString(font, boundKeys.trim(), x + size + 8 + nameWidth + 4, y + 12, 0xFFFF00, false);
            }

            guiGraphics.drawString(font, costText, x + colWidth - 15 - font.width(costText), y + 12, 0x55FFFF, false);
        }

        boolean isHovered(int mouseX, int mouseY) {
            if (isHidden()) return false;
            int x = getX(); int y = getY();
            return mouseX >= x - 2 && mouseX < x + colWidth - 10 && mouseY >= y - 2 && mouseY < y + size + 2;
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && this.minecraft != null && this.minecraft.player != null) {
            int rightX = guiLeft + leftW;
            if (mouseX >= rightX + 10 && mouseX <= rightX + 70 && mouseY >= guiTop + 7 && mouseY <= guiTop + 67) {
                this.minecraft.player.playSound(net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK.value(), 1.0F, 1.0F);
                this.minecraft.setScreen(new GuiAvatarSelect(this));
                return true;
            }
            for (SkillButton btn : skillButtons) {
                if (btn.isHovered((int)mouseX, (int)mouseY)) {
                    this.minecraft.player.playSound(net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK.value(), 1.0F, 1.0F);
                    ClientSkillInfo.currentSkill = btn.skillId;
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean isPauseScreen() { return false; }
}
