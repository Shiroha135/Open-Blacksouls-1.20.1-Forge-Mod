package com.BlackSouls.BlackSoulsMod.util.skill;

import com.BlackSouls.BlackSoulsMod.BlackSouls;
import com.BlackSouls.BlackSoulsMod.capability.BSPlayerStats;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;

import java.util.List;

public class SkillJudgmentScytheArt extends AbstractOriginalWeaponSkill {
    public enum Art { SOUL_HARVEST, TRUE_SOUL_HARVEST }
    private final Art art;

    public SkillJudgmentScytheArt(Art art) {
        super(Family.JUDGMENT_SCYTHE, art == Art.TRUE_SOUL_HARVEST ? 5 : 0);
        this.art = art;
    }

    @Override public String getSkillId() { return art == Art.SOUL_HARVEST ? "bs2_skill_soul_harvest" : "bs2_skill_true_soul_harvest"; }
    @Override public float getManaCost() { return art == Art.SOUL_HARVEST ? 6.0F : 66.0F; }
    @Override public int getBaseCooldownTicks() { return art == Art.SOUL_HARVEST ? 0 : 1200; }
    @Override public String getTranslationKey() { return "skill.blacksouls." + getSkillId() + ".name"; }
    @Override public ChatFormatting getTextColor() { return ChatFormatting.DARK_PURPLE; }
    @Override public ResourceLocation getIcon() { return new ResourceLocation(BlackSouls.MODID, "textures/gui/skills/soul_harvest.png"); }

    @Override
    public void execute(ServerPlayer player, BSPlayerStats stats) {
        List<LivingEntity> targets = art == Art.SOUL_HARVEST
                ? findTargets(player, 8.0D, 1)
                : findTargets(player, 12.0D, Integer.MAX_VALUE);
        if (targets.isEmpty()) {
            player.sendSystemMessage(Component.translatable("message.blacksouls.skill.no_target").withStyle(ChatFormatting.GRAY));
            return;
        }
        String key = art == Art.SOUL_HARVEST ? "soul_harvest" : "true_soul_harvest";
        player.sendSystemMessage(Component.translatable("message.blacksouls.skill." + key + ".use", player.getName().getString()).withStyle(ChatFormatting.WHITE));
        player.swing(InteractionHand.MAIN_HAND, true);
        targets.forEach(target -> playAnimation(target, 221));
        playSound(player, BlackSouls.DARKNESS5_EVENT.get(), 1.2F);
        playSound(player, BlackSouls.SWORD3_EVENT.get(), 1.0F);
        player.serverLevel().getServer().tell(new net.minecraft.server.TickTask(2, () -> playSound(player, BlackSouls.SLASH2_EVENT.get(), 1.0F)));
        player.serverLevel().getServer().tell(new net.minecraft.server.TickTask(3, () -> playSound(player, BlackSouls.SWORD4_EVENT.get(), 1.0F)));
        if (art == Art.SOUL_HARVEST) {
            int level = getUpgradeLevel(player);
            double damage = level >= 4 ? 6666.0D : level >= 2 ? 666.0D : 66.0D;
            player.serverLevel().getServer().tell(new net.minecraft.server.TickTask(3, () -> applyRawHit(player, targets.get(0), damage, false, true, 0.0D)));
        } else {
            for (int hit = 0; hit < 6; hit++) {
                int delay = 3 + hit * 2;
                player.serverLevel().getServer().tell(new net.minecraft.server.TickTask(delay, () -> {
                    for (LivingEntity target : targets) applyRawHit(player, target, 6666.0D, false, true, 0.0D);
                }));
            }
        }
    }
}
