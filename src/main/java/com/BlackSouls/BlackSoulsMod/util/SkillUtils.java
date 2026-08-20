package com.BlackSouls.BlackSoulsMod.util;

import com.BlackSouls.BlackSoulsMod.BlackSouls;
import com.BlackSouls.BlackSoulsMod.capability.BSPlayerStats;
import com.BlackSouls.BlackSoulsMod.handler.StatEventHandler;
import com.BlackSouls.BlackSoulsMod.util.skill.SkillRegistry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;
import top.theillusivec4.curios.api.CuriosApi;

@SuppressWarnings("removal")
public class SkillUtils {

    public static final String TAG_CURRENT_MANA = "bs2_mana_current";
    public static final String TAG_MAX_MANA = "bs2_mana_max";

    public static final double ACTION_TURN_TICKS = 200.0D;
    public static final double BASIC_ATTACK_ACTION_COST = 0.0625D;
    public static final double DEFAULT_SKILL_ACTION_COST = 0.5D;

    public static CompoundTag getPersistedData(Player player) {
        CompoundTag data = player.getPersistentData();
        if (!data.contains(Player.PERSISTED_NBT_TAG)) {
            data.put(Player.PERSISTED_NBT_TAG, new CompoundTag());
        }
        return data.getCompound(Player.PERSISTED_NBT_TAG);
    }

    public static String getCooldownTag(String skillId) {
        return "bs2_cd_" + skillId;
    }

    public static float getMana(Player player) {
        BSPlayerStats stats = player.getCapability(BSPlayerStats.CAPABILITY).resolve().orElse(null);
        if (stats != null) {
            return (float) stats.mp;
        }
        if (getPersistedData(player).contains(TAG_CURRENT_MANA)) {
            return getPersistedData(player).getFloat(TAG_CURRENT_MANA);
        }
        return 0;
    }

    public static float getMaxMana(Player player) {
        BSPlayerStats stats = player.getCapability(BSPlayerStats.CAPABILITY).resolve().orElse(null);
        if (stats != null) {
            return (float) stats.maxMp;
        }
        if (getPersistedData(player).contains(TAG_MAX_MANA)) {
            return getPersistedData(player).getFloat(TAG_MAX_MANA);
        }
        return 100.0F;
    }

    public static void setMana(Player player, float amount) {
        BSPlayerStats stats = player.getCapability(BSPlayerStats.CAPABILITY).resolve().orElse(null);
        if (stats != null) {
            stats.mp = amount;
            return;
        }

        if (amount < 0) {
            amount = 0;
        }
        float max = getMaxMana(player);
        if (amount > max) {
            amount = max;
        }
        getPersistedData(player).putFloat(TAG_CURRENT_MANA, amount);
    }

    public static boolean shouldBypassManaCost(Player player) {
        return player != null && player.getAbilities().instabuild;
    }

    public static boolean consumeMana(Player player, float amount) {
        if (shouldBypassManaCost(player)) {
            return true;
        }

        BSPlayerStats stats = player.getCapability(BSPlayerStats.CAPABILITY).resolve().orElse(null);
        if (stats != null) {
            boolean success = stats.consumeMP(amount);
            if (success) {
                StatEventHandler.syncToClient(player);
                return true;
            }
        }
        return false;
    }

    public static boolean shouldBypassActionCost(Player player) {
        return shouldBypassManaCost(player);
    }

    public static double getActionCount(Player player) {
        BSPlayerStats stats = player.getCapability(BSPlayerStats.CAPABILITY).resolve().orElse(null);
        return getActionCount(player, stats);
    }

    public static double getActionCount(Player player, BSPlayerStats stats) {
        double accessoryActions = CuriosApi.getCuriosInventory(player).map(handler -> {
            double bonus = 0.0D;
            for (var result : handler.findCurios(stack -> stack.is(BlackSouls.RING_WHITE_RABBIT.get())
                    || stack.is(BlackSouls.MYSTERY_OF_NIGHT_SKY.get())
                    || stack.is(BlackSouls.RING_BLACK_RABBIT.get()))) {
                if (result.stack().is(BlackSouls.RING_BLACK_RABBIT.get())) {
                    bonus += 1.0D;
                } else {
                    bonus += 0.5D;
                }
            }
            return bonus;
        }).orElse(0.0D);
        return calculateActionCount(player, stats, accessoryActions);
    }

    public static double getActionCount(Player player, BSPlayerStats stats, int whiteRabbitCount, int mysteryOfNightSkyCount, int blackRabbitCount) {
        return calculateActionCount(player, stats,
                (whiteRabbitCount * 0.5D) + (mysteryOfNightSkyCount * 0.5D) + blackRabbitCount);
    }

    private static double calculateActionCount(Player player, BSPlayerStats stats, double accessoryActions) {
        double actions = 1.0D + accessoryActions;
        if (stats != null) {
            actions += stats.extraActionRate;
        }

        if (BlackSouls.BUFF_HELANRITH_WINE.isPresent() && player.hasEffect(BlackSouls.BUFF_HELANRITH_WINE.get())) {
            actions += 1.0;
        }

        return Math.max(1.0, actions);
    }

    public static double getMaxActionPoints(Player player) {
        return getActionCount(player);
    }

    public static double getMaxActionPoints(Player player, BSPlayerStats stats) {
        return getActionCount(player, stats);
    }

    public static double getCurrentActionPoints(Player player) {
        BSPlayerStats stats = player.getCapability(BSPlayerStats.CAPABILITY).resolve().orElse(null);
        return stats != null ? stats.getCurrentActionPoints() : getMaxActionPoints(player);
    }

    public static double getActionRegenPerTick(Player player) {
        return getMaxActionPoints(player) / ACTION_TURN_TICKS;
    }

    public static boolean hasEnoughActionPoints(Player player, double amount) {
        return shouldBypassActionCost(player) || getCurrentActionPoints(player) + 1.0E-6 >= amount;
    }

    public static boolean consumeActionPoints(Player player, double amount) {
        if (shouldBypassActionCost(player) || amount <= 0.0) {
            return true;
        }

        BSPlayerStats stats = player.getCapability(BSPlayerStats.CAPABILITY).resolve().orElse(null);
        if (stats != null && stats.consumeActionPoints(amount)) {
            StatEventHandler.syncToClient(player);
            return true;
        }
        return false;
    }

    public static void restoreActionPoints(Player player, double amount) {
        BSPlayerStats stats = player.getCapability(BSPlayerStats.CAPABILITY).resolve().orElse(null);
        if (stats != null) {
            stats.restoreActionPoints(amount, getMaxActionPoints(player));
        }
    }

    public static boolean hasInfiniteCooldownAccessory(Player player) {
        BSPlayerStats stats = player.getCapability(BSPlayerStats.CAPABILITY).resolve().orElse(null);
        return stats != null && stats.developerNoCooldown;
    }

    public static boolean hasLearnedSkill(Player player, String skillId) {
        BSPlayerStats stats = player.getCapability(BSPlayerStats.CAPABILITY).resolve().orElse(null);
        return stats != null && stats.unlockedSkills.contains(skillId);
    }

    public static void learnSkill(Player player, String skillId) {
        BSPlayerStats stats = player.getCapability(BSPlayerStats.CAPABILITY).resolve().orElse(null);
        if (stats != null && !stats.unlockedSkills.contains(skillId)) {
            stats.unlockedSkills.add(skillId);
            StatEventHandler.syncToClient(player);
        }
    }

    public static int getVorpalComboStage(Player player) {
        BSPlayerStats s = player.getCapability(BSPlayerStats.CAPABILITY).resolve().orElse(null);
        return s != null ? s.vorpalComboStage : 0;
    }

    public static void setVorpalComboStage(Player player, int stage) {
        BSPlayerStats s = player.getCapability(BSPlayerStats.CAPABILITY).resolve().orElse(null);
        if (s != null) {
            s.vorpalComboStage = stage;
            long time = player.level().getGameTime();
            s.vorpalLastTime = time;
            StatEventHandler.syncToClient(player);
        }
    }

    public static long getVorpalLastTime(Player player) {
        BSPlayerStats s = player.getCapability(BSPlayerStats.CAPABILITY).resolve().orElse(null);
        return s != null ? s.vorpalLastTime : 0L;
    }

    public static void reduceAllCooldowns(Player player, int seconds) {
        long reductionTicks = seconds * 20L;
        CompoundTag persisted = getPersistedData(player);

        for (String skillId : SkillRegistry.SKILLS.keySet()) {
            String cooldownTag = getCooldownTag(skillId);
            if (persisted.contains(cooldownTag)) {
                persisted.putLong(cooldownTag, persisted.getLong(cooldownTag) - reductionTicks);
            }
        }
    }

    public static void clearAllCooldowns(Player player) {
        CompoundTag persisted = getPersistedData(player);
        for (String skillId : SkillRegistry.SKILLS.keySet()) {
            String cooldownTag = getCooldownTag(skillId);
            if (persisted.contains(cooldownTag)) {
                persisted.putLong(cooldownTag, 0L);
            }
        }
    }

    public static long getSouls(Player player) {
        BSPlayerStats stats = player.getCapability(BSPlayerStats.CAPABILITY).resolve().orElse(null);
        return stats != null ? stats.souls : 0L;
    }

    public static long getLostSouls(Player player) {
        BSPlayerStats stats = player.getCapability(BSPlayerStats.CAPABILITY).resolve().orElse(null);
        return stats != null ? stats.lostSouls : 0L;
    }

    public static void addSouls(Player player, long amount) {
        if (amount <= 0L) {
            return;
        }
        BSPlayerStats stats = player.getCapability(BSPlayerStats.CAPABILITY).resolve().orElse(null);
        if (stats == null) {
            return;
        }
        stats.souls += amount;
        if (player instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
            StatEventHandler.syncToClient(serverPlayer);
        }
    }

    public static boolean consumeSouls(Player player, long amount) {
        if (amount < 0L) {
            return false;
        }
        BSPlayerStats stats = player.getCapability(BSPlayerStats.CAPABILITY).resolve().orElse(null);
        if (stats == null || stats.souls < amount) {
            return false;
        }
        stats.souls -= amount;
        if (player instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
            StatEventHandler.syncToClient(serverPlayer);
        }
        return true;
    }
}
