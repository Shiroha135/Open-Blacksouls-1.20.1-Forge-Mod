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
public class SkillMadBowArt extends AbstractOriginalWeaponSkill {
    public SkillMadBowArt() {
        super(Family.MAD_BOW, 0);
    }

    @Override public String getSkillId() { return "bs2_skill_mad_bird_call"; }
    @Override public float getManaCost() { return 30.0F; }
    @Override public int getBaseCooldownTicks() { return 2000; }
    @Override public String getTranslationKey() { return "skill.blacksouls.bs2_skill_mad_bird_call.name"; }
    @Override public ChatFormatting getTextColor() { return ChatFormatting.RED; }
    @Override public ResourceLocation getIcon() { return new ResourceLocation(BlackSouls.MODID, "textures/gui/skills/mad_bird_call.png"); }

    @Override
    public void execute(ServerPlayer player, BSPlayerStats stats) {
        player.sendSystemMessage(Component.translatable("message.blacksouls.skill.mad_bird_call.use", player.getName().getString()).withStyle(ChatFormatting.WHITE));
        player.swing(InteractionHand.MAIN_HAND, true);
        playAnimation(player, 282);
        playSound(player, BlackSouls.PUSH_EVENT.get(), 1.0F);
        schedule(player, 1, () -> playSound(player, BlackSouls.BIRD_CRY_EVENT.get(), 1.2F));
        player.addEffect(new MobEffectInstance(BlackSouls.BUFF_MAD_BIRD_CALL.get(), 1000, 0));
        StatEventHandler.applyStats(player);
        StatEventHandler.syncToClient(player);
    }

    private void schedule(ServerPlayer player, int delay, Runnable task) {
        net.minecraft.server.MinecraftServer server = player.serverLevel().getServer();
        server.tell(new net.minecraft.server.TickTask(server.getTickCount() + delay, task));
    }
}
