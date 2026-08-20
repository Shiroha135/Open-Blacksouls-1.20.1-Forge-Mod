package com.BlackSouls.BlackSoulsMod.util.skill;

import com.BlackSouls.BlackSoulsMod.BlackSouls;
import com.BlackSouls.BlackSoulsMod.capability.BSPlayerStats;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;

@SuppressWarnings("removal")
public class SkillHasso extends AbstractOriginalWeaponSkill {
    public SkillHasso() {
        super(Family.MAGIC_BLADE, false);
    }

    @Override public String getSkillId() { return "bs2_skill_hasso"; }
    @Override public float getManaCost() { return 10.0F; }
    @Override public int getBaseCooldownTicks() { return 600; }
    @Override public String getTranslationKey() { return "skill.blacksouls.bs2_skill_hasso.name"; }
    @Override public ChatFormatting getTextColor() { return ChatFormatting.DARK_RED; }
    @Override public ResourceLocation getIcon() { return new ResourceLocation(BlackSouls.MODID, "textures/gui/skills/hasso.png"); }

    @Override
    public void execute(ServerPlayer player, BSPlayerStats stats) {
        player.sendSystemMessage(Component.translatable("message.blacksouls.skill.hasso.use", player.getName().getString()).withStyle(ChatFormatting.WHITE));
        playAnimation(player, 43);
        playSound(player, BlackSouls.MAGIC1_EVENT.get(), 0.8F);
        player.addEffect(new MobEffectInstance(BlackSouls.BUFF_HASSO.get(), 400, 0));
        player.serverLevel().getServer().tell(new net.minecraft.server.TickTask(10, () -> playSound(player, BlackSouls.UP1_EVENT.get(), 1.5F)));
    }
}
