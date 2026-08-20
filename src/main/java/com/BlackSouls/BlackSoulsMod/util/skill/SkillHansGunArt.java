package com.BlackSouls.BlackSoulsMod.util.skill;

import com.BlackSouls.BlackSoulsMod.BlackSouls;
import com.BlackSouls.BlackSoulsMod.capability.BSPlayerStats;
import com.BlackSouls.BlackSoulsMod.handler.StatEventHandler;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;

@SuppressWarnings("removal")
public class SkillHansGunArt extends AbstractOriginalWeaponSkill {
    public enum Art { GUNPOWDER_REPLENISH, QUICK_RELOAD }
    private final Art art;

    public SkillHansGunArt(Art art) {
        super(Family.HANS_GUN, art == Art.QUICK_RELOAD ? 5 : 0);
        this.art = art;
    }

    @Override public String getSkillId() { return art == Art.GUNPOWDER_REPLENISH ? "bs2_skill_gunpowder_replenish" : "bs2_skill_quick_reload"; }
    @Override public float getManaCost() { return art == Art.GUNPOWDER_REPLENISH ? 10.0F : 11.0F; }
    @Override public int getBaseCooldownTicks() { return art == Art.GUNPOWDER_REPLENISH ? 0 : 600; }
    @Override public String getTranslationKey() { return "skill.blacksouls." + getSkillId() + ".name"; }
    @Override public ChatFormatting getTextColor() { return ChatFormatting.YELLOW; }
    @Override public ResourceLocation getIcon() { return new ResourceLocation(BlackSouls.MODID,
            "textures/gui/skills/" + (art == Art.GUNPOWDER_REPLENISH ? "gunpowder_replenish.png" : "quick_reload.png")); }

    @Override
    public void execute(ServerPlayer player, BSPlayerStats stats) {
        String key = art == Art.GUNPOWDER_REPLENISH ? "gunpowder_replenish" : "quick_reload";
        player.sendSystemMessage(Component.translatable("message.blacksouls.skill." + key + ".use", player.getName().getString()).withStyle(ChatFormatting.WHITE));
        player.swing(InteractionHand.MAIN_HAND, true);
        playAnimation(player, 111);
        playSound(player, BlackSouls.EQUIP1_EVENT.get(), 1.5F);
        if (art == Art.GUNPOWDER_REPLENISH) {
            StatEventHandler.applyAttackUp(player, 1000);
            StatEventHandler.applyAttackUp(player, 1000);
        } else {
            StatEventHandler.applySpeedUp(player, 1000);
            player.addEffect(new MobEffectInstance(BlackSouls.BUFF_QUICK_RELOAD.get(), 400, 0));
            player.addEffect(new MobEffectInstance(BlackSouls.BUFF_QUICK_RELOAD_CRIT.get(), 600, 0));
        }
    }
}
