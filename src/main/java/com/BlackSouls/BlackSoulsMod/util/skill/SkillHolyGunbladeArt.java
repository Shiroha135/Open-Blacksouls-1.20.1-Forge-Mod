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

@SuppressWarnings("removal")
public class SkillHolyGunbladeArt extends AbstractOriginalWeaponSkill {
    public enum Art { CROSS_SLASH, BULLET_LOAD, VISCERAL_ATTACK }
    private final Art art;

    public SkillHolyGunbladeArt(Art art) {
        super(Family.HOLY_GUNBLADE, art == Art.VISCERAL_ATTACK ? 5 : 0);
        this.art = art;
    }

    @Override public String getSkillId() { return switch (art) {
        case CROSS_SLASH -> "bs2_skill_cross_slash";
        case BULLET_LOAD -> "bs2_skill_bullet_load";
        case VISCERAL_ATTACK -> "bs2_skill_visceral_attack";
    }; }
    @Override public float getManaCost() { return 0.0F; }
    @Override public double getActionCost() { return art == Art.BULLET_LOAD ? 0.0D : super.getActionCost(); }
    @Override public int getBaseCooldownTicks() { return switch (art) {
        case CROSS_SLASH -> 600;
        case BULLET_LOAD -> 0;
        case VISCERAL_ATTACK -> 1600;
    }; }
    @Override public String getTranslationKey() { return "skill.blacksouls." + getSkillId() + ".name"; }
    @Override public ChatFormatting getTextColor() { return ChatFormatting.GOLD; }
    @Override public ResourceLocation getIcon() { return new ResourceLocation(BlackSouls.MODID, "textures/gui/skills/" + switch (art) {
        case CROSS_SLASH -> "cross_slash.png";
        case BULLET_LOAD -> "bullet_load.png";
        case VISCERAL_ATTACK -> "visceral_attack.png";
    }); }

    @Override
    public void execute(ServerPlayer player, BSPlayerStats stats) {
        if (art == Art.BULLET_LOAD) {
            reload(player);
            return;
        }
        LivingEntity target = findTarget(player, 14.0D);
        if (target == null) {
            player.sendSystemMessage(Component.translatable("message.blacksouls.skill.no_target").withStyle(ChatFormatting.GRAY));
            return;
        }
        player.sendSystemMessage(Component.translatable("message.blacksouls.skill." + (art == Art.CROSS_SLASH ? "cross_slash" : "visceral_attack") + ".use", player.getName().getString()).withStyle(ChatFormatting.WHITE));
        player.swing(InteractionHand.MAIN_HAND, true);
        if (art == Art.CROSS_SLASH) {
            playAnimation(target, 518);
            playSound(target, BlackSouls.SLASH1_EVENT.get(), 1.0F);
            schedule(player, 2, () -> playSound(target, BlackSouls.SLASH2_EVENT.get(), 1.0F));
            schedule(player, 8, () -> playSound(target, BlackSouls.SKILL3_EVENT.get(), 1.0F));
            removeGuardEffects(target);
            applyFormulaHit(player, target, stats, 6.0D, 0.5D, 0.20D, true, false, 0.0D);
            schedule(player, 4, () -> applyFormulaHit(player, target, stats, 6.0D, 0.5D, 0.20D, true, false, 0.0D));
            schedule(player, 9, () -> performGunfire(player, target));
        } else {
            playAnimation(target, 423);
            playSound(target, BlackSouls.THUNDER7_EVENT.get(), 0.6F);
            schedule(player, 3, () -> {
                playSound(target, BlackSouls.SWORD_STAB_EVENT.get(), 0.8F);
                playSound(target, BlackSouls.SLASH2_EVENT.get(), 1.0F);
            });
            schedule(player, 16, () -> playSound(target, BlackSouls.GUCHA004A_EVENT.get(), 0.5F));
            double rawDamage = stats.attack * 4.0D - StatEventHandler.getRpgPhysicalDefense(target);
            rawDamage *= 0.8D + Math.random() * 0.4D;
            if (BlackSouls.BUFF_DEFENSELESS.isPresent() && target.hasEffect(BlackSouls.BUFF_DEFENSELESS.get())) rawDamage *= 5.0D;
            applyRawHit(player, target, rawDamage, true, true, 0.0D);
        }
    }

    public static void performGunfire(ServerPlayer player, LivingEntity target) {
        StatEventHandler.performHolyGunbladeGunfire(player, target, getAmmoMode(player));
    }

    public static int getAmmoMode(ServerPlayer player) {
        if (player.hasEffect(BlackSouls.BUFF_GUNBLADE_AMMO_III.get())) return 3;
        if (player.hasEffect(BlackSouls.BUFF_GUNBLADE_AMMO_II.get())) return 2;
        return 1;
    }

    public static void ensureAmmoState(ServerPlayer player) {
        if (!player.hasEffect(BlackSouls.BUFF_GUNBLADE_AMMO_I.get())
                && !player.hasEffect(BlackSouls.BUFF_GUNBLADE_AMMO_II.get())
                && !player.hasEffect(BlackSouls.BUFF_GUNBLADE_AMMO_III.get())) {
            player.addEffect(new MobEffectInstance(BlackSouls.BUFF_GUNBLADE_AMMO_I.get(), 72000, 0, false, false, true));
        }
    }

    private void reload(ServerPlayer player) {
        ensureAmmoState(player);
        int current = getAmmoMode(player);
        int next = current == 3 ? 1 : current + 1;
        float hpCostRate = current == 3 ? 0.01F : 0.05F;
        player.setHealth(Math.max(1.0F, player.getHealth() - player.getMaxHealth() * hpCostRate));
        player.removeEffect(BlackSouls.BUFF_GUNBLADE_AMMO_I.get());
        player.removeEffect(BlackSouls.BUFF_GUNBLADE_AMMO_II.get());
        player.removeEffect(BlackSouls.BUFF_GUNBLADE_AMMO_III.get());
        player.addEffect(new MobEffectInstance(switch (next) {
            case 2 -> BlackSouls.BUFF_GUNBLADE_AMMO_II.get();
            case 3 -> BlackSouls.BUFF_GUNBLADE_AMMO_III.get();
            default -> BlackSouls.BUFF_GUNBLADE_AMMO_I.get();
        }, 72000, 0, false, false, true));
        player.sendSystemMessage(Component.translatable("message.blacksouls.skill.bullet_load.use", player.getName().getString(), next).withStyle(ChatFormatting.WHITE));
        player.swing(InteractionHand.MAIN_HAND, true);
        playAnimation(player, 492);
        playSound(player, BlackSouls.BLOOD_SPLATTER_EVENT.get(), 0.85F);
        playSound(player, BlackSouls.GUN_GIRD1_EVENT.get(), 1.0F);
    }

    private static void removeGuardEffects(LivingEntity target) {
        target.removeEffect(BlackSouls.BUFF_KNIGHTS_GLORY.get());
        target.removeEffect(BlackSouls.BUFF_DAGGER_GUARD.get());
        target.removeEffect(BlackSouls.BUFF_COUNTER_STANCE.get());
        target.removeEffect(BlackSouls.BUFF_ECLIPSE.get());
    }

    private static void schedule(ServerPlayer player, int delay, Runnable task) {
        net.minecraft.server.MinecraftServer server = player.serverLevel().getServer();
        server.tell(new net.minecraft.server.TickTask(server.getTickCount() + delay, task));
    }
}
