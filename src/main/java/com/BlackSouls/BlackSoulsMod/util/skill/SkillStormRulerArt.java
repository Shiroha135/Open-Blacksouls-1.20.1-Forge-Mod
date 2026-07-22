package com.BlackSouls.BlackSoulsMod.util.skill;

import com.BlackSouls.BlackSoulsMod.BlackSouls;
import com.BlackSouls.BlackSoulsMod.capability.BSPlayerStats;
import com.BlackSouls.BlackSoulsMod.handler.StatEventHandler;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;

import java.util.List;

public class SkillStormRulerArt extends AbstractOriginalWeaponSkill {
    public enum Art { STORM_KING, STORM_OVERLORD }
    private final Art art;

    public SkillStormRulerArt(Art art) {
        super(Family.STORM_RULER, art == Art.STORM_KING ? 0 : 5, art == Art.STORM_KING ? 4 : 5);
        this.art = art;
    }

    @Override public String getSkillId() { return art == Art.STORM_KING ? "bs2_skill_storm_king" : "bs2_skill_storm_overlord"; }
    @Override public float getManaCost() { return art == Art.STORM_KING ? 50.0F : 80.0F; }
    @Override public int getBaseCooldownTicks() { return 1000; }
    @Override public String getTranslationKey() { return "skill.blacksouls." + getSkillId() + ".name"; }
    @Override public ChatFormatting getTextColor() { return ChatFormatting.AQUA; }
    @Override public ResourceLocation getIcon() { return new ResourceLocation(BlackSouls.MODID, "textures/gui/skills/storm_ruler.png"); }

    @Override
    public void execute(ServerPlayer player, BSPlayerStats stats) {
        List<LivingEntity> targets = findTargets(player, 16.0D, Integer.MAX_VALUE);
        if (targets.isEmpty()) {
            player.sendSystemMessage(Component.translatable("message.blacksouls.skill.no_target").withStyle(ChatFormatting.GRAY));
            return;
        }
        String key = art == Art.STORM_KING ? "storm_king" : "storm_overlord";
        player.sendSystemMessage(Component.translatable("message.blacksouls.skill." + key + ".use", player.getName().getString()).withStyle(ChatFormatting.WHITE));
        player.swing(InteractionHand.MAIN_HAND, true);
        targets.forEach(target -> playAnimation(target, art == Art.STORM_KING ? 74 : 336));
        if (art == Art.STORM_KING) {
            playSound(player, BlackSouls.WIND5_EVENT.get(), 0.8F);
            scheduleSound(player, 1, BlackSouls.WIND8_EVENT.get(), 0.7F);
        } else {
            playSound(player, BlackSouls.MAGIC7_EVENT.get(), 0.7F);
            playSound(player, BlackSouls.WIND8_EVENT.get(), 0.7F);
            scheduleSound(player, 13, BlackSouls.WIND10_EVENT.get(), 0.5F);
        }
        int hits = art == Art.STORM_KING ? 1 : 4;
        for (int hit = 0; hit < hits; hit++) {
            int delay = art == Art.STORM_KING ? 1 : 4 + hit * 4;
            player.serverLevel().getServer().tell(new net.minecraft.server.TickTask(delay, () -> {
                for (LivingEntity target : targets) {
                    double rawDamage = 800.0D + stats.attack * 4.0D - StatEventHandler.getRpgPhysicalDefense(target) * 2.0D;
                    rawDamage *= 0.8D + Math.random() * 0.4D;
                    applyRawHit(player, target, rawDamage, true, true, 0.0D);
                }
            }));
        }
    }

    private void scheduleSound(ServerPlayer player, int delay, net.minecraft.sounds.SoundEvent sound, float pitch) {
        player.serverLevel().getServer().tell(new net.minecraft.server.TickTask(delay, () -> playSound(player, sound, pitch)));
    }
}
