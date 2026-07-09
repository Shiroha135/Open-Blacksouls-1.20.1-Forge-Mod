package com.BlackSouls.BlackSoulsMod.util.skill;

import com.BlackSouls.BlackSoulsMod.BlackSouls;
import com.BlackSouls.BlackSoulsMod.capability.BSPlayerStats;
import com.BlackSouls.BlackSoulsMod.handler.StatEventHandler;
import com.BlackSouls.BlackSoulsMod.network.NetworkHandler;
import com.BlackSouls.BlackSoulsMod.network.packets.PacketPlayAnim;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@SuppressWarnings("resource")
public class SkillHellfireBlade extends WeaponSkill {

    @Override
    public String getSkillId() { return "bs2_skill_hellfire_blade"; }

    @Override
    public float getManaCost() { return 30.0f; } 

    @Override
    public int getBaseCooldownTicks() { return 1800; }

    @Override
    public String getTranslationKey() { return "skill.blacksouls.hellfire_blade.name"; }

    @Override
    public ChatFormatting getTextColor() { return ChatFormatting.RED; }

    @Override
    protected boolean isWeaponEquipped(Player player) {
        ItemStack mainHand = player.getMainHandItem();
        if (mainHand.isEmpty() || mainHand.getItem() != BlackSouls.DRAKE_SWORD.get()) return false;

        int upgradeLevel = 0;
        if (mainHand.hasTag() && mainHand.getTag().contains("bs2_upgrade_level")) {
            upgradeLevel = mainHand.getTag().getInt("bs2_upgrade_level");
        }
        return upgradeLevel >= 5;
    }

    @Override
    public void execute(ServerPlayer player, BSPlayerStats stats) {
        List<LivingEntity> targets = findRandomTargets(player, 10.0, 4);

        if (targets.isEmpty()) {
            player.sendSystemMessage(Component.translatable("message.blacksouls.skill.no_target").withStyle(ChatFormatting.GRAY));
            return;
        }

        player.sendSystemMessage(Component.translatable("message.blacksouls.skill.hellfire_blade.use", player.getName().getString()).withStyle(ChatFormatting.WHITE));
        player.swing(InteractionHand.MAIN_HAND, true);

        net.minecraft.server.MinecraftServer server = player.serverLevel().getServer();
        server.tell(new net.minecraft.server.TickTask(0, () -> {
            playSkillSound(player, BlackSouls.MONSTER4_EVENT.get(), 1.0f, 1.0f);
            playSkillSound(player, BlackSouls.SLASH3_EVENT.get(), 1.0f, 1.0f);
            playSkillSound(player, BlackSouls.FIRE2_EVENT.get(), 1.0f, 1.0f);
        }));
        server.tell(new net.minecraft.server.TickTask(Math.max(1, (int) Math.round(462 / 50.0)), () -> {
            playSkillSound(player, BlackSouls.FIRE8_EVENT.get(), 1.0f, 1.0f);

            for (LivingEntity target : targets) {
                playAnim(target, 179);
                applyHit(player, target, stats, true);
            }
        }));
        int hfDelay = 462;
        for (int i = 0; i < 4; i++) {
            hfDelay += 132;
            final int d = hfDelay;
            server.tell(new net.minecraft.server.TickTask(Math.max(1, (int) Math.round(d / 50.0)), () -> {
                playSkillSound(player, BlackSouls.FIRE7_EVENT.get(), 1.0f, 1.0f);
            }));
        }
    }

    private void playSkillSound(Entity source, SoundEvent sound, float volume, float pitch) {
        source.level().playSound(null, source.getX(), source.getY(), source.getZ(), sound, SoundSource.PLAYERS, volume, pitch);
    }

    private void playAnim(LivingEntity target, int animId) {
        PacketPlayAnim animPacket = new PacketPlayAnim(animId, target.getX(), target.getY() + target.getBbHeight() / 2.0F, target.getZ());
        NetworkHandler.sendToAllAround(animPacket, target);
    }

    private void applyHit(ServerPlayer player, LivingEntity target, BSPlayerStats stats, boolean isSureHit) {
        if (target == null || target.isRemoved() || target.getHealth() <= 0) return;

        double bDef = StatEventHandler.getRpgPhysicalDefense(target);

        double fireMultiplier = 1.0;
        if (BlackSouls.BUFF_BURN.isPresent() && target.hasEffect(BlackSouls.BUFF_BURN.get())) {
            fireMultiplier = 2.0;
        }

        double rawDamage = (stats.attack * 4.0 - bDef * 2.0) * fireMultiplier;
        if (rawDamage < 1.0) rawDamage = 1.0;

        double variance = 0.8 + (Math.random() * 0.4);
        rawDamage *= variance;

        float finalDamage = com.BlackSouls.BlackSoulsMod.handler.StatEventHandler.rollSkillCrit(player, (float) rawDamage);
        target.invulnerableTime = 0;

        DamageSource source = isSureHit ? player.damageSources().indirectMagic(player, player) : player.damageSources().playerAttack(player);
        target.hurt(source, finalDamage);
    }

    private List<LivingEntity> findRandomTargets(Player player, double range, int maxCount) {
        List<LivingEntity> validTargets = new ArrayList<>();
        List<Entity> list = player.level().getEntities(player, player.getBoundingBox().inflate(range));
        for (Entity e : list) {
            if (e instanceof LivingEntity le && le.isAlive() && !le.isSpectator()) {
                if (player.distanceToSqr(le) <= range * range) validTargets.add(le);
            }
        }
        Collections.shuffle(validTargets);
        return validTargets.subList(0, Math.min(validTargets.size(), maxCount));
    }
}
