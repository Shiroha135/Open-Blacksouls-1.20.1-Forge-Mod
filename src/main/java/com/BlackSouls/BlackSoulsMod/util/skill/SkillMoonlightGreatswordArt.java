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

public class SkillMoonlightGreatswordArt extends AbstractOriginalWeaponSkill {
    public enum Art { MOONLIGHT_BLADE, MOONLIGHT_BREAK }
    private final Art art;

    public SkillMoonlightGreatswordArt(Art art) {
        super(Family.MOONLIGHT_GREATSWORD, art == Art.MOONLIGHT_BREAK ? 5 : 0);
        this.art = art;
    }

    @Override public String getSkillId() { return art == Art.MOONLIGHT_BLADE ? "bs2_skill_moonlight_blade" : "bs2_skill_moonlight_break"; }
    @Override public float getManaCost() { return art == Art.MOONLIGHT_BLADE ? 25.0F : 40.0F; }
    @Override public int getBaseCooldownTicks() { return 2600; }
    @Override public String getTranslationKey() { return "skill.blacksouls." + getSkillId() + ".name"; }
    @Override public ChatFormatting getTextColor() { return ChatFormatting.AQUA; }
    @Override public ResourceLocation getIcon() { return new ResourceLocation(BlackSouls.MODID, "textures/gui/skills/moonlight.png"); }

    @Override
    public void execute(ServerPlayer player, BSPlayerStats stats) {
        List<LivingEntity> targets = art == Art.MOONLIGHT_BLADE
                ? findTargets(player, 10.0D, 1)
                : findTargets(player, 14.0D, Integer.MAX_VALUE);
        if (targets.isEmpty()) {
            player.sendSystemMessage(Component.translatable("message.blacksouls.skill.no_target").withStyle(ChatFormatting.GRAY));
            return;
        }
        String key = art == Art.MOONLIGHT_BLADE ? "moonlight_blade" : "moonlight_break";
        player.sendSystemMessage(Component.translatable("message.blacksouls.skill." + key + ".use", player.getName().getString()).withStyle(ChatFormatting.WHITE));
        player.swing(InteractionHand.MAIN_HAND, true);
        targets.forEach(target -> playAnimation(target, art == Art.MOONLIGHT_BLADE ? 178 : 177));
        if (art == Art.MOONLIGHT_BLADE) playBladeSounds(player);
        else playBreakSounds(player);

        int hits = art == Art.MOONLIGHT_BLADE ? 3 : 2;
        for (int hit = 0; hit < hits; hit++) {
            int delay = art == Art.MOONLIGHT_BLADE ? 6 + hit * 4 : 6 + hit * 6;
            player.serverLevel().getServer().tell(new net.minecraft.server.TickTask(delay, () -> {
                for (LivingEntity target : targets) {
                    double multiplier = art == Art.MOONLIGHT_BLADE ? 4.0D : 6.0D;
                    double rawDamage = stats.attack * multiplier + stats.magicAttack * multiplier
                            - StatEventHandler.getRpgPhysicalDefense(target) * 2.0D
                            - StatEventHandler.getRpgMagicDefense(target) * 2.0D;
                    rawDamage *= 0.8D + Math.random() * 0.4D;
                    applyRawHit(player, target, rawDamage, true, true, 0.0D);
                }
            }));
        }
    }

    private void playBladeSounds(ServerPlayer player) {
        playSound(player, BlackSouls.BATTLE3_EVENT.get(), 1.0F);
        scheduleSound(player, 6, BlackSouls.SAINT6_EVENT.get(), 1.0F);
        scheduleSound(player, 6, BlackSouls.RAISE1_EVENT.get(), 1.0F);
        scheduleSound(player, 14, BlackSouls.FLASH1_EVENT.get(), 0.5F);
        scheduleSound(player, 14, BlackSouls.EARTH1_EVENT.get(), 0.5F);
        scheduleSound(player, 14, BlackSouls.DAMAGE2_EVENT.get(), 0.5F);
    }

    private void playBreakSounds(ServerPlayer player) {
        for (int delay = 0; delay <= 12; delay += 3) scheduleSound(player, delay, BlackSouls.ICE2_EVENT.get(), 1.5F);
        for (int delay = 3; delay <= 15; delay += 3) scheduleSound(player, delay, BlackSouls.SAINT3_EVENT.get(), 1.0F);
        scheduleSound(player, 5, BlackSouls.MAGIC2_EVENT.get(), 1.0F);
    }

    private void scheduleSound(ServerPlayer player, int delay, net.minecraft.sounds.SoundEvent sound, float pitch) {
        player.serverLevel().getServer().tell(new net.minecraft.server.TickTask(delay, () -> playSound(player, sound, pitch)));
    }
}
