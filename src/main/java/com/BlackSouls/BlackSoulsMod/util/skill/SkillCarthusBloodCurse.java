package com.BlackSouls.BlackSoulsMod.util.skill;

import com.BlackSouls.BlackSoulsMod.BlackSouls;
import com.BlackSouls.BlackSoulsMod.capability.BSPlayerStats;
import com.BlackSouls.BlackSoulsMod.handler.StatEventHandler;
import com.BlackSouls.BlackSoulsMod.network.NetworkHandler;
import com.BlackSouls.BlackSoulsMod.network.packets.PacketPlayAnim;
import com.BlackSouls.BlackSoulsMod.util.SkillUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;

@SuppressWarnings("removal")
public class SkillCarthusBloodCurse extends AbstractSkill {

    @Override
    public String getSkillId() { return "bs2_skill_carthus_blood_curse"; }

    @Override
    public float getManaCost() { return 0.0f; } 

    @Override
    public int getBaseCooldownTicks() { return 200; } 

    @Override
    public String getTranslationKey() { return "skill.blacksouls.bs2_skill_carthus_blood_curse.name"; }

    @Override
    public ChatFormatting getTextColor() { return ChatFormatting.DARK_RED; }

    @Override
    public ResourceLocation getIcon() { return new ResourceLocation(BlackSouls.MODID, "textures/gui/skills/original/carthus_blood_curse.png"); }

    @Override
    public boolean isUnlockedForGUI(Player player) {
        return SkillUtils.hasLearnedSkill(player, getSkillId());
    }

    @Override
    public void execute(ServerPlayer player, BSPlayerStats stats) {
        player.swing(InteractionHand.MAIN_HAND, true);

        player.sendSystemMessage(Component.translatable("message.blacksouls.skill.carthus_blood_curse.use", player.getName().getString()).withStyle(ChatFormatting.WHITE));

        playAnim(player, 56);

        net.minecraft.server.MinecraftServer server = player.serverLevel().getServer();
        server.tell(new net.minecraft.server.TickTask(0, () -> {
            playSkillSound(player, BlackSouls.DARKNESS5_EVENT.get(), 1.0f, 1.0f);
        }));
        server.tell(new net.minecraft.server.TickTask(Math.max(1, (int) Math.round(396 / 50.0)), () -> {
            if (player.isAlive()) {
                playSkillSound(player, BlackSouls.THUNDER1_EVENT.get(), 1.0f, 1.0f);

                double baseDamage = 300.0;
                double variance = 0.8 + (Math.random() * 0.4); // 0.8 ~ 1.2
                float finalDamage = (float) (baseDamage * variance);

                player.hurt(player.damageSources().indirectMagic(player, player), finalDamage);

                if (player.isAlive()) {
                    stats.restoreMP(100.0);
                    StatEventHandler.syncToClient(player);
                }
            }
        }));
    }

    private void playSkillSound(Player player, SoundEvent sound, float volume, float pitch) {
        player.level().playSound(null, player.getX(), player.getY() + player.getBbHeight() / 2.0, player.getZ(), sound, SoundSource.PLAYERS, volume, pitch);
    }

    private void playAnim(Player player, int animId) {
        PacketPlayAnim animPacket = new PacketPlayAnim(animId, player.getX(), player.getY() + player.getBbHeight() / 2.0F, player.getZ());
        NetworkHandler.sendToAllAround(animPacket, player);
    }
}
