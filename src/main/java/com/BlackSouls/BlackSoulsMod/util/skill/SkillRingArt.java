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
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

@SuppressWarnings("removal")
public class SkillRingArt extends AbstractWeaponCombatSkill {
    public enum Art {
        STRONG_CRUSH,
        JUGGLING_EVASION
    }

    private final Art art;

    public SkillRingArt(Art art) {
        this.art = art;
    }

    @Override
    public String getSkillId() {
        return art == Art.STRONG_CRUSH ? "bs2_skill_strong_crush" : "bs2_skill_juggling_evasion";
    }

    @Override
    public float getManaCost() {
        return art == Art.STRONG_CRUSH ? 0.0F : 3.0F;
    }

    @Override
    public int getBaseCooldownTicks() {
        return art == Art.STRONG_CRUSH ? 200 : 600;
    }

    @Override
    public String getTranslationKey() {
        return "skill.blacksouls." + getSkillId() + ".name";
    }

    @Override
    public ChatFormatting getTextColor() {
        return ChatFormatting.AQUA;
    }

    @Override
    public ResourceLocation getIcon() {
        return new ResourceLocation(BlackSouls.MODID, art == Art.STRONG_CRUSH
                ? "textures/gui/skills/strong_crush.png"
                : "textures/mob_effect/juggling_evasion.png");
    }

    @Override
    protected boolean isWeaponEquipped(Player player) {
        return true;
    }

    @Override
    public boolean isUnlockedForGUI(Player player) {
        return StatEventHandler.getBaubleCount(player, art == Art.STRONG_CRUSH
                ? BlackSouls.RING_REBELLION.get()
                : BlackSouls.RING_DULL_WOOD_GRAIN.get()) > 0;
    }

    @Override
    public void execute(ServerPlayer player, BSPlayerStats stats) {
        if (art == Art.JUGGLING_EVASION) {
            player.addEffect(new MobEffectInstance(BlackSouls.BUFF_JUGGLING_EVASION.get(), 600, 0));
            player.level().playSound(null, player.blockPosition(), BlackSouls.EVASION1_EVENT.get(),
                    net.minecraft.sounds.SoundSource.PLAYERS, 0.8F, 1.0F);
            StatEventHandler.applyStats(player);
            StatEventHandler.syncToClient(player);
            player.sendSystemMessage(Component.translatable(
                    "message.blacksouls.skill.juggling_evasion.use", player.getName().getString()).withStyle(ChatFormatting.WHITE));
            return;
        }

        LivingEntity target = findTarget(player, 8.0D);
        if (target == null) {
            player.sendSystemMessage(Component.translatable("message.blacksouls.skill.no_target").withStyle(ChatFormatting.GRAY));
            return;
        }

        player.swing(InteractionHand.MAIN_HAND, true);
        playAnimation(target, 240);
        playSound(target, BlackSouls.EVASION1_EVENT.get(), 1.0F);
        player.sendSystemMessage(Component.translatable(
                "message.blacksouls.skill.strong_crush.use", player.getName().getString()).withStyle(ChatFormatting.WHITE));

        net.minecraft.server.MinecraftServer server = player.serverLevel().getServer();
        server.tell(new net.minecraft.server.TickTask(server.getTickCount() + 5, () -> {
            if (!target.isAlive() || target.isRemoved()) {
                return;
            }
            playSound(target, BlackSouls.ICE11_EVENT.get(), 1.2F);
            playSound(target, BlackSouls.BLOW7_EVENT.get(), 0.5F);
            target.addEffect(new MobEffectInstance(BlackSouls.BUFF_DEFENSELESS.get(), 400, 1));
        }));
    }
}

