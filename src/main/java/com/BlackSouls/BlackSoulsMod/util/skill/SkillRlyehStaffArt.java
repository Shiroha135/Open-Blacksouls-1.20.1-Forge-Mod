package com.BlackSouls.BlackSoulsMod.util.skill;

import com.BlackSouls.BlackSoulsMod.BlackSouls;
import com.BlackSouls.BlackSoulsMod.capability.BSPlayerStats;
import com.BlackSouls.BlackSoulsMod.util.SkillUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;

@SuppressWarnings("removal")
public class SkillRlyehStaffArt extends AbstractOriginalWeaponSkill {
    public SkillRlyehStaffArt() {
        super(Family.RLYEH_STAFF, 5);
    }

    @Override public String getSkillId() { return "bs2_skill_mental_focus"; }
    @Override public float getManaCost() { return 100.0F; }
    @Override public int getBaseCooldownTicks() { return 0; }
    @Override public String getTranslationKey() { return "skill.blacksouls.bs2_skill_mental_focus.name"; }
    @Override public ChatFormatting getTextColor() { return ChatFormatting.AQUA; }
    @Override public ResourceLocation getIcon() { return new ResourceLocation(BlackSouls.MODID, "textures/gui/skills/mental_focus.png"); }

    @Override
    public void execute(ServerPlayer player, BSPlayerStats stats) {
        player.sendSystemMessage(Component.translatable("message.blacksouls.skill.mental_focus.use", player.getName().getString()).withStyle(ChatFormatting.WHITE));
        player.swing(InteractionHand.MAIN_HAND, true);
        playAnimation(player, 350);
        playSound(player, BlackSouls.WIND2_EVENT.get(), 1.0F);
        schedule(player, 9, () -> playSound(player, BlackSouls.SAINT7_EVENT.get(), 1.0F));
        schedule(player, 11, () -> {
            playSound(player, BlackSouls.ITEM3_EVENT.get(), 1.5F);
            SkillUtils.clearAllCooldownsExceptChrono(player);
        });
    }

    private void schedule(ServerPlayer player, int delay, Runnable task) {
        net.minecraft.server.MinecraftServer server = player.serverLevel().getServer();
        server.tell(new net.minecraft.server.TickTask(server.getTickCount() + delay, task));
    }
}
