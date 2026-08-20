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

@SuppressWarnings("removal")
public class SkillFortressArt extends AbstractOriginalWeaponSkill {
    public SkillFortressArt() {
        super(Family.FORTRESS, true);
    }

    @Override public String getSkillId() { return "bs2_skill_shield_slam"; }
    @Override public float getManaCost() { return 20.0F; }
    @Override public int getBaseCooldownTicks() { return 1200; }
    @Override public String getTranslationKey() { return "skill.blacksouls.bs2_skill_shield_slam.name"; }
    @Override public ChatFormatting getTextColor() { return ChatFormatting.GOLD; }
    @Override public ResourceLocation getIcon() { return new ResourceLocation(BlackSouls.MODID, "textures/gui/skills/shield_slam.png"); }

    @Override
    public void execute(ServerPlayer player, BSPlayerStats stats) {
        List<LivingEntity> targets = findTargets(player, 8.0D, 1);
        if (targets.isEmpty()) {
            player.sendSystemMessage(Component.translatable("message.blacksouls.skill.no_target").withStyle(ChatFormatting.GRAY));
            return;
        }
        LivingEntity target = targets.get(0);
        player.sendSystemMessage(Component.translatable("message.blacksouls.skill.shield_slam.use", player.getName().getString()).withStyle(ChatFormatting.WHITE));
        player.swing(InteractionHand.MAIN_HAND, true);
        playAnimation(target, 161);
        playSound(target, BlackSouls.WIND7_EVENT.get(), 0.5F);
        for (int delay : new int[]{2, 4, 7}) {
            player.serverLevel().getServer().tell(new net.minecraft.server.TickTask(delay, () -> playSound(target, BlackSouls.BLOW4_EVENT.get(), 0.65F)));
        }
        player.serverLevel().getServer().tell(new net.minecraft.server.TickTask(7, () -> {
            double rawDamage = stats.attack * 4.0D + stats.defense * 4.0D;
            rawDamage *= 0.8D + Math.random() * 0.4D;
            applyRawHit(player, target, rawDamage, true, false, 0.0D);
        }));
    }
}
