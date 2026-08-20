package com.BlackSouls.BlackSoulsMod.util.skill;

import com.BlackSouls.BlackSoulsMod.BlackSouls;
import com.BlackSouls.BlackSoulsMod.capability.BSPlayerStats;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;

@SuppressWarnings("removal")
public class SkillIssen extends AbstractOriginalWeaponSkill {
    public SkillIssen() {
        super(Family.MAGIC_BLADE, true);
    }

    @Override public String getSkillId() { return "bs2_skill_issen"; }
    @Override public float getManaCost() { return 30.0F; }
    @Override public int getBaseCooldownTicks() { return 1600; }
    @Override public String getTranslationKey() { return "skill.blacksouls.bs2_skill_issen.name"; }
    @Override public ChatFormatting getTextColor() { return ChatFormatting.DARK_RED; }
    @Override public ResourceLocation getIcon() { return new ResourceLocation(BlackSouls.MODID, "textures/gui/skills/issen.png"); }

    @Override
    public void execute(ServerPlayer player, BSPlayerStats stats) {
        LivingEntity target = findTarget(player, 10.0D);
        if (target == null) {
            player.sendSystemMessage(Component.translatable("message.blacksouls.skill.no_target").withStyle(ChatFormatting.GRAY));
            return;
        }
        player.sendSystemMessage(Component.translatable("message.blacksouls.skill.issen.use", player.getName().getString()).withStyle(ChatFormatting.WHITE));
        player.swing(InteractionHand.MAIN_HAND, true);
        playAnimation(target, 144);
        playSound(target, BlackSouls.SWORD5_EVENT.get(), 1.0F);
        player.serverLevel().getServer().tell(new net.minecraft.server.TickTask(4, () -> playSound(target, BlackSouls.WIND1_EVENT.get(), 1.5F)));
        player.serverLevel().getServer().tell(new net.minecraft.server.TickTask(6, () -> playSound(target, BlackSouls.ATTACK3_EVENT.get(), 1.0F)));
        player.serverLevel().getServer().tell(new net.minecraft.server.TickTask(10, () -> {
            playSound(target, BlackSouls.SLASH2_EVENT.get(), 0.8F);
            playSound(target, BlackSouls.SLASH9_EVENT.get(), 1.0F);
            applyRawHit(player, target, stats.attack * 7.0D + 10000.0D, true, false, 90.0D);
        }));
    }
}
