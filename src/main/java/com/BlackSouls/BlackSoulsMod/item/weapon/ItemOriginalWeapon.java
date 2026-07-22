package com.BlackSouls.BlackSoulsMod.item.weapon;

import com.BlackSouls.BlackSoulsMod.BlackSouls;
import com.BlackSouls.BlackSoulsMod.handler.StatEventHandler;
import com.BlackSouls.BlackSoulsMod.network.NetworkHandler;
import com.BlackSouls.BlackSoulsMod.network.packets.PacketPlayAnim;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class ItemOriginalWeapon extends ItemBSWeaponBase {
    public enum Profile {
        MAGIC_BLADE, DEMON_GOD_BLADE, MAGICIANS_STAFF, ALL_CREATION_STAFF,
        DOUBLE_EDGED_GREATSWORD, RAGNAROK, MEAT_CLEAVER_GREATAXE, SLAUGHTERER_GREATAXE,
        MACE, DIVINE_PUNISHMENT_MACE, HALBERD, BAHAMUT,
        BEAST_HUNTER_SAW, BEAST_SLAYING_SAW_SWORD, SHIELD_GUARD_FORTRESS, GUARDIAN_FORTRESS,
        DARK_SWORD, DARK_BLADE, BROKEN_SWORD, GRUDGE_SWORD,
        WARHAMMER, ABERRANT_WARHAMMER, KNUCKLE_DUSTER, KAISER_GAUNTLET, UCHIGATANA, KISHIN_BLADE,
        GREAT_IRON_BALL, JUDGMENT_SCYTHE, STORM_RULER, DEMON_STAFF,
        MOONLIGHT_GREATSWORD, CORRUPT_JABBERWOCK_SCYTHE,
        MIRANDA_AXE, RLYEH_STAFF, DEEP_SEA_KNIGHTS_ANCHOR,
        LOST_SWORD, GLACHID, SLAUGHTERERS_CHAINSAW,
        MOCK_TURTLE_SOUP_LADLE, DIVINE_ANGEL_DUAL_SWORDS, HOLY_GUNBLADE,
        MARY_SUES_BRANCH_STAFF, EUNICES_RAPIER, RAIDENS_DUAL_AXES
    }

    private final Profile profile;

    public ItemOriginalWeapon(Profile profile, Properties properties) {
        super(Tiers.DIAMOND, 0, -2.4F, properties);
        this.profile = profile;
    }

    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        if (!attacker.level().isClientSide() && attacker instanceof ServerPlayer player) {
            playAttackEffects(player, target);
            if (profile == Profile.KNUCKLE_DUSTER || profile == Profile.KAISER_GAUNTLET) {
                player.serverLevel().getServer().tell(new net.minecraft.server.TickTask(7, () -> {
                    if (!target.isRemoved() && target.isAlive()) {
                        StatEventHandler.performOriginalWeaponExtraHit(player, target, 4.0D);
                    }
                }));
            }
            if (profile == Profile.GREAT_IRON_BALL) {
                StatEventHandler.performIronBallSweep(player, target);
            } else if (profile == Profile.DIVINE_ANGEL_DUAL_SWORDS) {
                int aura = getDualSwordAura(player);
                for (int hit = 1; hit <= aura; hit++) {
                    int delay = hit * 2;
                    scheduleTask(player, delay, () -> {
                        if (!target.isRemoved() && target.isAlive()) {
                            StatEventHandler.performOriginalWeaponExtraHit(player, target, 3.0D, 2.0D);
                        }
                    });
                }
                scheduleTask(player, aura * 2 + 1, () -> {
                    int nextAura = Math.min(7, aura + 1);
                    player.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                            BlackSouls.BUFF_DUAL_SWORD_AURA.get(), 400, nextAura - 1, false, false, true
                    ));
                    StatEventHandler.applyStats(player);
                    StatEventHandler.syncToClient(player);
                });
            } else if (profile == Profile.HOLY_GUNBLADE) {
                scheduleTask(player, 6, () -> com.BlackSouls.BlackSoulsMod.util.skill.SkillHolyGunbladeArt.performGunfire(player, target));
            } else if (profile == Profile.MARY_SUES_BRANCH_STAFF) {
                StatEventHandler.performMarySueSweep(player, target);
            } else if (profile == Profile.SLAUGHTERERS_CHAINSAW
                    && BlackSouls.BUFF_SLAUGHTER_MODE.isPresent()
                    && player.hasEffect(BlackSouls.BUFF_SLAUGHTER_MODE.get())) {
                for (int hit = 1; hit < 4; hit++) {
                    int delay = hit * 3;
                    scheduleTask(player, delay, () -> {
                        if (!target.isRemoved() && target.isAlive()) {
                            playAttackEffects(player, target);
                            StatEventHandler.performOriginalWeaponExtraHit(player, target, 4.0D, 2.0D);
                        }
                    });
                }
            }
        }
        return super.hurtEnemy(stack, target, attacker);
    }

    public void playAttackEffects(ServerPlayer player, LivingEntity target) {
        int animationId = switch (profile) {
            case MAGIC_BLADE -> 126;
            case DEMON_GOD_BLADE -> 126;
            case MAGICIANS_STAFF, ALL_CREATION_STAFF -> 213;
            case MEAT_CLEAVER_GREATAXE, SLAUGHTERER_GREATAXE -> 130;
            case DOUBLE_EDGED_GREATSWORD, RAGNAROK -> 520;
            case MACE, DIVINE_PUNISHMENT_MACE -> 211;
            case HALBERD, BAHAMUT -> 230;
            case BEAST_HUNTER_SAW, BEAST_SLAYING_SAW_SWORD -> 232;
            case SHIELD_GUARD_FORTRESS, GUARDIAN_FORTRESS -> 160;
            case DARK_SWORD, DARK_BLADE -> 166;
            case BROKEN_SWORD, GRUDGE_SWORD -> 7;
            case WARHAMMER, ABERRANT_WARHAMMER -> 235;
            case KNUCKLE_DUSTER, KAISER_GAUNTLET -> 239;
            case UCHIGATANA, KISHIN_BLADE -> 340;
            case GREAT_IRON_BALL -> 129;
            case JUDGMENT_SCYTHE -> 220;
            case STORM_RULER -> 131;
            case DEMON_STAFF -> 19;
            case MOONLIGHT_GREATSWORD -> 176;
            case CORRUPT_JABBERWOCK_SCYTHE -> 318;
            case MIRANDA_AXE -> 140;
            case RLYEH_STAFF -> 349;
            case DEEP_SEA_KNIGHTS_ANCHOR -> 359;
            case LOST_SWORD -> 375;
            case GLACHID -> 379;
            case SLAUGHTERERS_CHAINSAW -> 384;
            case MOCK_TURTLE_SOUP_LADLE -> 388;
            case DIVINE_ANGEL_DUAL_SWORDS -> {
                int hits = getDualSwordAura(player) + 1;
                yield hits >= 8 ? 516 : hits >= 4 ? 517 : 515;
            }
            case HOLY_GUNBLADE -> 519;
            case MARY_SUES_BRANCH_STAFF -> 0;
            case EUNICES_RAPIER -> 548;
            case RAIDENS_DUAL_AXES -> 551;
        };
        if (animationId > 0) {
            NetworkHandler.sendToAllAround(new PacketPlayAnim(animationId, target.getX(), target.getY() + target.getBbHeight() / 2.0F, target.getZ()), target);
        }

        switch (profile) {
            case MAGIC_BLADE -> {
                playSound(target, BlackSouls.SWORD5_EVENT.get(), 1.0F);
                playSound(target, BlackSouls.SWORD4_EVENT.get(), 1.0F);
            }
            case DEMON_GOD_BLADE -> {
                playSound(target, BlackSouls.SWORD5_EVENT.get(), 1.0F);
                playSound(target, BlackSouls.SWORD4_EVENT.get(), 1.0F);
            }
            case MAGICIANS_STAFF, ALL_CREATION_STAFF -> {
                playSound(target, BlackSouls.EVASION1_EVENT.get(), 1.0F);
                scheduleSound(player, target, 3, BlackSouls.BLOW3_EVENT.get(), 0.8F);
            }
            case MEAT_CLEAVER_GREATAXE, SLAUGHTERER_GREATAXE -> playSound(target, BlackSouls.SLASH9_EVENT.get(), 1.0F);
            case DOUBLE_EDGED_GREATSWORD, RAGNAROK -> playSound(target, BlackSouls.SLASH4_EVENT.get(), 1.0F);
            case MACE, DIVINE_PUNISHMENT_MACE -> {
                playSound(target, BlackSouls.WIND7_EVENT.get(), 0.8F);
                scheduleSound(player, target, 4, BlackSouls.SAINT9_EVENT.get(), 0.5F);
                scheduleSound(player, target, 4, BlackSouls.BLOW2_EVENT.get(), 1.0F);
            }
            case HALBERD, BAHAMUT -> {
                playSound(target, BlackSouls.SLASH9_EVENT.get(), 1.0F);
                scheduleSound(player, target, 2, BlackSouls.BLOW7_EVENT.get(), 0.5F);
                scheduleSound(player, target, 2, BlackSouls.EARTH1_EVENT.get(), 1.0F);
            }
            case BEAST_HUNTER_SAW, BEAST_SLAYING_SAW_SWORD -> {
                playSound(target, BlackSouls.SLASH2_EVENT.get(), 1.0F);
                scheduleSound(player, target, 1, BlackSouls.SWORD4_EVENT.get(), 1.0F);
                scheduleSound(player, target, 1, BlackSouls.SWORD5_EVENT.get(), 1.0F);
            }
            case SHIELD_GUARD_FORTRESS, GUARDIAN_FORTRESS -> {
                playSound(target, BlackSouls.SWORD1_EVENT.get(), 1.0F);
                scheduleSound(player, target, 1, BlackSouls.SLASH2_EVENT.get(), 1.0F);
            }
            case DARK_SWORD, DARK_BLADE -> {
                playSound(target, BlackSouls.SLASH1_EVENT.get(), 1.0F);
                scheduleSound(player, target, 2, BlackSouls.DARKNESS4_EVENT.get(), 1.0F);
            }
            case BROKEN_SWORD, GRUDGE_SWORD -> playSound(target, BlackSouls.SLASH1_EVENT.get(), 1.0F);
            case WARHAMMER, ABERRANT_WARHAMMER -> {
                playSound(target, BlackSouls.WIND7_EVENT.get(), 0.5F);
                playSound(target, BlackSouls.EARTH2_EVENT.get(), 1.0F);
                scheduleSound(player, target, 2, BlackSouls.EARTH1_EVENT.get(), 1.0F);
                scheduleSound(player, target, 2, BlackSouls.DAMAGE2_EVENT.get(), 1.0F);
            }
            case KNUCKLE_DUSTER, KAISER_GAUNTLET -> {
                playSound(target, BlackSouls.EVASION1_EVENT.get(), 1.0F);
                scheduleSound(player, target, 3, BlackSouls.BLOW3_EVENT.get(), 0.8F);
                scheduleSound(player, target, 4, BlackSouls.EVASION1_EVENT.get(), 0.9F);
                scheduleSound(player, target, 7, BlackSouls.BLOW4_EVENT.get(), 0.8F);
            }
            case UCHIGATANA, KISHIN_BLADE -> {
                playSound(target, BlackSouls.DAO2_EVENT.get(), 1.0F);
                scheduleSound(player, target, 2, BlackSouls.SWORD4_EVENT.get(), 1.0F);
                scheduleSound(player, target, 4, BlackSouls.SWORD5_EVENT.get(), 1.0F);
            }
            case GREAT_IRON_BALL -> {
                playSound(target, BlackSouls.DOWN2_EVENT.get(), 1.5F);
                scheduleSound(player, target, 7, BlackSouls.BLOW7_EVENT.get(), 0.5F);
            }
            case JUDGMENT_SCYTHE -> {
                playSound(target, BlackSouls.SWORD3_EVENT.get(), 1.0F);
                scheduleSound(player, target, 2, BlackSouls.SLASH2_EVENT.get(), 1.0F);
                scheduleSound(player, target, 3, BlackSouls.SWORD4_EVENT.get(), 1.0F);
            }
            case STORM_RULER -> {
                playSound(target, BlackSouls.SLASH1_EVENT.get(), 1.0F);
                scheduleSound(player, target, 2, BlackSouls.WIND10_EVENT.get(), 1.0F);
            }
            case DEMON_STAFF -> scheduleSound(player, target, 1, BlackSouls.SLASH4_EVENT.get(), 0.9F);
            case MOONLIGHT_GREATSWORD -> {
                playSound(target, BlackSouls.SLASH1_EVENT.get(), 1.0F);
                scheduleSound(player, target, 3, BlackSouls.FLASH1_EVENT.get(), 0.5F);
                scheduleSound(player, target, 3, BlackSouls.SAINT9_EVENT.get(), 0.5F);
            }
            case CORRUPT_JABBERWOCK_SCYTHE -> {
                playSound(target, BlackSouls.SWORD3_EVENT.get(), 0.8F);
                scheduleSound(player, target, 2, BlackSouls.SLASH2_EVENT.get(), 0.85F);
                scheduleSound(player, target, 3, BlackSouls.SWORD4_EVENT.get(), 1.2F);
            }
            case MIRANDA_AXE -> playSound(target, BlackSouls.SLASH9_EVENT.get(), 0.85F);
            case RLYEH_STAFF -> {
                playSound(target, BlackSouls.WIND7_EVENT.get(), 0.5F);
                scheduleSound(player, target, 4, BlackSouls.WATER1_EVENT.get(), 1.2F);
                scheduleSound(player, target, 4, BlackSouls.BLOW2_EVENT.get(), 1.0F);
            }
            case DEEP_SEA_KNIGHTS_ANCHOR -> {
                playSound(target, BlackSouls.WIND7_EVENT.get(), 0.5F);
                scheduleSound(player, target, 2, BlackSouls.METAL2_EVENT.get(), 0.6F);
                scheduleSound(player, target, 2, BlackSouls.DIVE_EVENT.get(), 1.0F);
                scheduleSound(player, target, 2, BlackSouls.WATER1_EVENT.get(), 1.3F);
                scheduleSound(player, target, 2, BlackSouls.METAL1_EVENT.get(), 0.6F);
            }
            case LOST_SWORD -> {
                playSound(target, BlackSouls.SLASH1_EVENT.get(), 1.0F);
                scheduleSound(player, target, 3, BlackSouls.SAINT9_EVENT.get(), 1.0F);
                scheduleSound(player, target, 3, BlackSouls.SAINT3_EVENT.get(), 1.0F);
            }
            case GLACHID -> {
                playSound(target, BlackSouls.ATTACK3_EVENT.get(), 1.0F);
                scheduleSound(player, target, 3, BlackSouls.SLASH8_EVENT.get(), 1.0F);
                scheduleSound(player, target, 3, BlackSouls.SLASH3_EVENT.get(), 1.0F);
            }
            case SLAUGHTERERS_CHAINSAW -> {
                playSound(target, BlackSouls.CHAINSAW_REV_EVENT.get(), 1.2F);
                playSound(target, BlackSouls.GUCHA004A_EVENT.get(), 1.5F);
                scheduleSound(player, target, 2, BlackSouls.GUCHA004A_EVENT.get(), 1.5F);
                scheduleSound(player, target, 4, BlackSouls.GUCHA004A_EVENT.get(), 1.5F);
            }
            case MOCK_TURTLE_SOUP_LADLE -> scheduleSound(player, target, 1, BlackSouls.BLOW7_EVENT.get(), 1.5F);
            case DIVINE_ANGEL_DUAL_SWORDS -> {
                int hits = getDualSwordAura(player) + 1;
                if (hits >= 8) {
                    for (int delay = 0; delay <= 14; delay += 2) scheduleSound(player, target, delay, BlackSouls.SLASH9_EVENT.get(), 1.0F);
                } else if (hits >= 4) {
                    playSound(target, BlackSouls.SWORD4_EVENT.get(), 0.8F);
                    playSound(target, BlackSouls.SWORD3_EVENT.get(), 0.8F);
                    playSound(target, BlackSouls.SLASH1_EVENT.get(), 1.0F);
                    scheduleSound(player, target, 2, BlackSouls.SLASH2_EVENT.get(), 1.0F);
                    scheduleSound(player, target, 4, BlackSouls.SLASH3_EVENT.get(), 1.0F);
                    scheduleSound(player, target, 6, BlackSouls.SLASH4_EVENT.get(), 1.0F);
                } else {
                    playSound(target, BlackSouls.SLASH2_EVENT.get(), 1.0F);
                    playSound(target, BlackSouls.SWORD4_EVENT.get(), 1.0F);
                }
            }
            case HOLY_GUNBLADE -> {
                playSound(target, BlackSouls.SLASH9_EVENT.get(), 1.0F);
                scheduleSound(player, target, 2, BlackSouls.SAINT9_EVENT.get(), 1.0F);
            }
            case MARY_SUES_BRANCH_STAFF -> {
            }
            case EUNICES_RAPIER -> {
                playSound(target, BlackSouls.WIND7_EVENT.get(), 0.5F);
                scheduleSound(player, target, 3, BlackSouls.SLASH11_EVENT.get(), 0.8F);
                scheduleSound(player, target, 3, BlackSouls.ICE4_EVENT.get(), 1.3F);
            }
            case RAIDENS_DUAL_AXES -> {
                playSound(target, BlackSouls.EARTH5_EVENT.get(), 0.75F);
                scheduleSound(player, target, 1, BlackSouls.SLASH9_EVENT.get(), 0.8F);
            }
        }
    }

    private static LivingEntity findRandomAttackTarget(ServerPlayer player, LivingEntity fallback, double range) {
        List<LivingEntity> targets = player.level().getEntitiesOfClass(
                LivingEntity.class,
                player.getBoundingBox().inflate(range),
                target -> target != player && target.isAlive() && !target.isSpectator()
        );
        return targets.isEmpty() ? fallback : targets.get(player.getRandom().nextInt(targets.size()));
    }

    private static void scheduleTask(ServerPlayer player, int delay, Runnable task) {
        net.minecraft.server.MinecraftServer server = player.serverLevel().getServer();
        server.tell(new net.minecraft.server.TickTask(server.getTickCount() + delay, task));
    }

    private static void scheduleSound(ServerPlayer player, LivingEntity target, int delay, SoundEvent sound, float pitch) {
        net.minecraft.server.MinecraftServer server = player.serverLevel().getServer();
        server.tell(new net.minecraft.server.TickTask(server.getTickCount() + delay, () -> {
            if (!target.isRemoved()) playSound(target, sound, pitch);
        }));
    }

    private static void playSound(LivingEntity target, SoundEvent sound, float pitch) {
        target.level().playSound(null, target.getX(), target.getY(), target.getZ(), sound, SoundSource.PLAYERS, 1.0F, pitch);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flagIn) {
        String firstLoreKey = getDescriptionId() + ".lore.1";
        if (profile == Profile.LOST_SWORD && getSpecialUpgradeLevel(stack) >= 5) firstLoreKey += ".max";
        tooltip.add(Component.translatable(firstLoreKey).withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
        if (profile == Profile.MEAT_CLEAVER_GREATAXE) {
            int upgrade = stack.hasTag() ? Math.max(0, Math.min(9, stack.getTag().getInt("bs2_upgrade_level"))) : 0;
            tooltip.add(Component.translatable(getDescriptionId() + ".lore.dynamic", 30 + upgrade * 5).withStyle(ChatFormatting.WHITE));
        } else if (profile == Profile.MACE) {
            int upgrade = stack.hasTag() ? Math.max(0, Math.min(9, stack.getTag().getInt("bs2_upgrade_level"))) : 0;
            int regen = upgrade <= 3 ? 20 : upgrade <= 6 ? 30 : 40;
            tooltip.add(Component.translatable(getDescriptionId() + ".lore.dynamic", regen).withStyle(ChatFormatting.WHITE));
        } else if (profile == Profile.SHIELD_GUARD_FORTRESS) {
            int upgrade = stack.hasTag() ? Math.max(0, Math.min(9, stack.getTag().getInt("bs2_upgrade_level"))) : 0;
            tooltip.add(Component.translatable(getDescriptionId() + ".lore.dynamic", 5 + upgrade * 5).withStyle(ChatFormatting.WHITE));
        } else if (profile == Profile.KNUCKLE_DUSTER) {
            int upgrade = stack.hasTag() ? Math.max(0, Math.min(9, stack.getTag().getInt("bs2_upgrade_level"))) : 0;
            tooltip.add(Component.translatable(getDescriptionId() + ".lore.dynamic", 5 + upgrade * 5).withStyle(ChatFormatting.WHITE));
        } else if (profile == Profile.GREAT_IRON_BALL) {
            int upgrade = getSpecialUpgradeLevel(stack);
            tooltip.add(Component.translatable(getDescriptionId() + (upgrade >= 5 ? ".lore.max" : ".lore.dynamic"), 10 + upgrade * 10).withStyle(ChatFormatting.WHITE));
        } else if (profile == Profile.JUDGMENT_SCYTHE || profile == Profile.STORM_RULER
                || profile == Profile.DEMON_STAFF || profile == Profile.MOONLIGHT_GREATSWORD
                || profile == Profile.CORRUPT_JABBERWOCK_SCYTHE || profile == Profile.RLYEH_STAFF
                || profile == Profile.DEEP_SEA_KNIGHTS_ANCHOR || profile == Profile.LOST_SWORD
                || profile == Profile.GLACHID || profile == Profile.SLAUGHTERERS_CHAINSAW
                || profile == Profile.HOLY_GUNBLADE || profile == Profile.EUNICES_RAPIER
                || profile == Profile.RAIDENS_DUAL_AXES) {
            int upgrade = getSpecialUpgradeLevel(stack);
            tooltip.add(Component.translatable(getDescriptionId() + (upgrade >= 5 ? ".lore.max" : ".lore.2")).withStyle(ChatFormatting.WHITE));
        } else if (profile == Profile.MIRANDA_AXE) {
            int upgrade = getSpecialUpgradeLevel(stack);
            int rate = upgrade >= 5 ? 40 : 30;
            tooltip.add(Component.translatable(getDescriptionId() + ".lore.dynamic", rate, rate).withStyle(ChatFormatting.WHITE));
        } else if (profile == Profile.MOCK_TURTLE_SOUP_LADLE) {
            int upgrade = getSpecialUpgradeLevel(stack);
            int[] luck = {5, 10, 20, 30, 40, 50};
            int stun = upgrade >= 5 ? 5 : 3;
            tooltip.add(Component.translatable(getDescriptionId() + (upgrade >= 5 ? ".lore.max" : ".lore.dynamic"), luck[upgrade], stun).withStyle(ChatFormatting.WHITE));
        } else if (profile == Profile.DIVINE_ANGEL_DUAL_SWORDS) {
            int stun = getSpecialUpgradeLevel(stack) >= 5 ? 5 : 3;
            tooltip.add(Component.translatable(getDescriptionId() + ".lore.dynamic", stun).withStyle(ChatFormatting.WHITE));
        } else {
            tooltip.add(Component.translatable(getDescriptionId() + ".lore.2").withStyle(ChatFormatting.WHITE));
        }
        super.appendHoverText(stack, level, tooltip, flagIn);
    }

    private static int getSpecialUpgradeLevel(ItemStack stack) {
        return stack.hasTag() ? Math.max(0, Math.min(5, stack.getTag().getInt("bs2_upgrade_level"))) : 0;
    }

    private static int getDualSwordAura(ServerPlayer player) {
        net.minecraft.world.effect.MobEffectInstance effect = player.getEffect(BlackSouls.BUFF_DUAL_SWORD_AURA.get());
        return effect == null ? 0 : Math.min(7, effect.getAmplifier() + 1);
    }
}
