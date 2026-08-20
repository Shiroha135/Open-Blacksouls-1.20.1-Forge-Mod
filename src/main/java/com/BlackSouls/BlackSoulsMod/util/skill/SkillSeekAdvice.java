package com.BlackSouls.BlackSoulsMod.util.skill;

import com.BlackSouls.BlackSoulsMod.BlackSouls;
import com.BlackSouls.BlackSoulsMod.capability.BSPlayerStats;
import com.BlackSouls.BlackSoulsMod.network.NetworkHandler;
import com.BlackSouls.BlackSoulsMod.network.packets.ClientboundAdviceVisibilityPacket;
import com.BlackSouls.BlackSoulsMod.util.SkillUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

@SuppressWarnings("removal")
public final class SkillSeekAdvice extends AbstractSkill {
    public static final String CONTROLLED_TAG = "bs2_advice_controlled";
    public static final String VISIBLE_TAG = "bs2_advice_visible";
    public static final String VISIBILITY_MIGRATION_TAG = "bs2_advice_visibility_v2";

    @Override
    public String getSkillId() {
        return "bs2_skill_seek_advice";
    }

    @Override
    public float getManaCost() {
        return 0.0F;
    }

    @Override
    public int getBaseCooldownTicks() {
        return 0;
    }

    @Override
    public double getActionCost() {
        return 0.0D;
    }

    @Override
    public String getTranslationKey() {
        return "skill.blacksouls.bs2_skill_seek_advice.name";
    }

    @Override
    public ChatFormatting getTextColor() {
        return ChatFormatting.YELLOW;
    }

    @Override
    public ResourceLocation getIcon() {
        return new ResourceLocation(BlackSouls.MODID, "textures/gui/skills/seek_advice.png");
    }

    @Override
    public boolean isUnlockedForGUI(Player player) {
        return SkillUtils.hasLearnedSkill(player, getSkillId());
    }

    @Override
    public boolean isUsableInTurnBattle() {
        return false;
    }

    @Override
    public void execute(ServerPlayer player, BSPlayerStats stats) {
        var data = SkillUtils.getPersistedData(player);
        boolean visible = !data.getBoolean(VISIBLE_TAG);
        data.putBoolean(CONTROLLED_TAG, true);
        data.putBoolean(VISIBLE_TAG, visible);
        NetworkHandler.sendToPlayer(new ClientboundAdviceVisibilityPacket(true, visible), player);
        player.sendSystemMessage(Component.translatable(visible
                ? "message.blacksouls.skill.seek_advice.visible"
                : "message.blacksouls.skill.seek_advice.hidden").withStyle(ChatFormatting.YELLOW));
    }

    public static void resetVisibility(ServerPlayer player) {
        var data = SkillUtils.getPersistedData(player);
        data.putBoolean(VISIBILITY_MIGRATION_TAG, true);
        data.remove(CONTROLLED_TAG);
        data.remove(VISIBLE_TAG);
        NetworkHandler.sendToPlayer(new ClientboundAdviceVisibilityPacket(false, true), player);
    }
}
