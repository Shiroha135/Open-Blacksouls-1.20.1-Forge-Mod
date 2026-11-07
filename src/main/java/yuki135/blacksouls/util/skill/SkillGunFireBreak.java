package yuki135.blacksouls.util.skill;

import yuki135.blacksouls.BlackSouls;
import yuki135.blacksouls.capability.BSPlayerStats;
import yuki135.blacksouls.util.BSOriginalSkillData;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Player;

public final class SkillGunFireBreak extends AbstractWeaponCombatSkill {
    @Override public String getSkillId() { return "bs2_skill_gun_fire_break"; }
    @Override public float getManaCost() { return 0.0F; }
    @Override public double getActionCost() { return 0.0D; }
    @Override public String getTranslationKey() { return "skill.blacksouls.bs2_skill_gun_fire_break.name"; }
    @Override public ChatFormatting getTextColor() { return ChatFormatting.DARK_GRAY; }
    @Override public ResourceLocation getIcon() { return ResourceLocation.fromNamespaceAndPath(BlackSouls.MODID,"textures/gui/skills/original/gun_fire_break.png"); }
    @Override public int getBaseCooldownTicks() { return Math.multiplyExact(getTurnCooldownRounds(),200); }
    @Override public int getTurnCooldownRounds() {
        var source=BSOriginalSkillData.get(20);
        if (source==null || source.cooldown()!=2) throw new IllegalStateException("Missing or mismatched original gunfire cooldown");
        return source.cooldown();
    }
    @Override public boolean requiresTurnBattleTarget() { return true; }
    public int getTurnBattleSourceSkillId() { return 20; }

    @Override protected boolean isWeaponEquipped(Player player) { return player!=null && player.getOffhandItem().is(BlackSouls.SILVER_PISTOL.get()); }

    @Override public void execute(ServerPlayer player,BSPlayerStats stats) {
        if (player==null || stats==null || !isWeaponEquipped(player)) return;
        var target=findTarget(player,24.0D);
        if (target==null) { player.sendSystemMessage(Component.translatable("message.blacksouls.skill.no_target").withStyle(ChatFormatting.GRAY));return; }
        player.swing(InteractionHand.OFF_HAND,true);playAnimation(target,217);
        player.sendSystemMessage(Component.translatable("message.blacksouls.skill.gun_fire_break.use",player.getName().getString()).withStyle(ChatFormatting.WHITE));
        if (applyFormulaHit(player,target,stats,3.0D,2.0D,0.2D,true,true,0.0D) && target.isAlive() && BlackSouls.BUFF_DEFENSELESS.isPresent()) target.addEffect(new MobEffectInstance(BlackSouls.BUFF_DEFENSELESS.get(),400,0));
    }
}
