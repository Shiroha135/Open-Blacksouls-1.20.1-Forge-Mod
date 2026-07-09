package com.BlackSouls.BlackSoulsMod.util;

import com.BlackSouls.BlackSoulsMod.capability.BSWorldData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;

import java.util.UUID;

public class DifficultyManager {

    public static int currentDifficulty = 1;
    public static boolean revengeMode = false;
    public static boolean deathMode = false;
    public static boolean legendaryMode = false;
    public static boolean maliceMode = false;
    public static boolean eternityMode = false;
    public static boolean revengeUnlocked = false;
    public static boolean deathUnlocked = false;
    public static boolean legendaryUnlocked = false;
    public static boolean maliceUnlocked = false;
    public static boolean eternityUnlocked = false;
    private static final UUID DIFFICULTY_MODIFIER_ID = UUID.fromString("B5A2C19D-3E84-4E1F-9B5A-8C2A1B4E6D9F");

    public static double getMultiplier(int difficulty) {
        if (difficulty <= 1) return 1.0D;
        if (difficulty == 2) return 1.5D;
        if (difficulty == 3) return 2.0D;
        if (difficulty == 4) return 2.5D;
        if (difficulty == 5) return 3.0D;
        if (difficulty == 6) return 4.0D;
        if (difficulty == 7) return 5.0D;
        if (difficulty == 8) return 6.0D;
        if (difficulty == 9) return 7.0D;
        return 1.0D;
    }

    public static double getRevengeMultiplier(boolean enabled) {
        return enabled ? 1.5D : 1.0D;
    }

    public static double getDeathMultiplier(boolean enabled) {
        return enabled ? 2.0D : 1.0D;
    }

    public static double getLegendaryMultiplier(boolean enabled) {
        return enabled ? 2.5D : 1.0D;
    }

    public static double getMaliceMultiplier(boolean enabled) {
        return enabled ? 3.0D : 1.0D;
    }

    public static double getEternityMultiplier(boolean enabled) {
        return enabled ? 4.0D : 1.0D;
    }

    public static double getExtraModeMultiplier(BSWorldData data) {
        return getRevengeMultiplier(data.isRevengeMode())
                * getDeathMultiplier(data.isDeathMode())
                * getLegendaryMultiplier(data.isLegendaryMode())
                * getMaliceMultiplier(data.isMaliceMode())
                * getEternityMultiplier(data.isEternityMode());
    }

    public static double getTotalMultiplier(BSWorldData data) {
        return getMultiplier(data.difficulty) * getExtraModeMultiplier(data);
    }

    public static void applyClientState(BSWorldData data) {
        currentDifficulty = data.difficulty;
        revengeMode = data.isRevengeMode();
        deathMode = data.isDeathMode();
        legendaryMode = data.isLegendaryMode();
        maliceMode = data.isMaliceMode();
        eternityMode = data.isEternityMode();
        revengeUnlocked = data.isRevengeUnlocked();
        deathUnlocked = data.isDeathUnlocked();
        legendaryUnlocked = data.isLegendaryUnlocked();
        maliceUnlocked = data.isMaliceUnlocked();
        eternityUnlocked = data.isEternityUnlocked();
    }

    public static void applyModifierToSingleMonster(LivingEntity monster) {
        if (!BSMobStatManager.hasManagedStats(monster)) {
            return;
        }

        BSMobStatManager.MobStats baseStats = BSMobStatManager.getStats(monster);
        double multiplier = getCurrentTotalMultiplier(monster.level());
        double amount = multiplier - 1.0D;

        float oldMaxHp = monster.getMaxHealth();
        float oldHp = monster.getHealth();
        double hpPercent = oldMaxHp > 0 ? (oldHp / oldMaxHp) : 1.0D;

        setBaseAttribute(monster, Attributes.MAX_HEALTH, baseStats.maxHealth);
        setBaseAttribute(monster, Attributes.ATTACK_DAMAGE, baseStats.attack);
        setBaseAttribute(monster, Attributes.MOVEMENT_SPEED, baseStats.getMovementSpeedAttribute());

        applyDifficultyModifier(monster, Attributes.MAX_HEALTH, amount);
        applyDifficultyModifier(monster, Attributes.ATTACK_DAMAGE, amount);

        monster.setHealth((float) (monster.getMaxHealth() * hpPercent));
    }

    private static void setBaseAttribute(LivingEntity monster, Attribute attribute, double baseValue) {
        AttributeInstance attrInstance = monster.getAttribute(attribute);
        if (attrInstance != null && baseValue > 0.0D) {
            attrInstance.setBaseValue(baseValue);
        }
    }

    private static void applyDifficultyModifier(LivingEntity monster, Attribute attribute, double amount) {
        AttributeInstance attrInstance = monster.getAttribute(attribute);
        if (attrInstance != null) {
            attrInstance.removeModifier(DIFFICULTY_MODIFIER_ID);
            if (amount > 0.0D) {
                AttributeModifier modifier = new AttributeModifier(
                        DIFFICULTY_MODIFIER_ID,
                        "BlackSouls_Difficulty",
                        amount,
                        AttributeModifier.Operation.MULTIPLY_BASE
                );
                attrInstance.addPermanentModifier(modifier);
            }
        }
    }

    public static void updateAllMonstersInstant(Level level) {
        if (level.isClientSide() || !(level instanceof ServerLevel serverLevel)) {
            return;
        }

        for (Entity entity : serverLevel.getAllEntities()) {
            if (entity instanceof LivingEntity livingEntity && BSMobStatManager.hasManagedStats(livingEntity)) {
                applyModifierToSingleMonster(livingEntity);
            }
        }
    }

    private static double getCurrentTotalMultiplier(Level level) {
        if (level instanceof ServerLevel serverLevel) {
            BSWorldData data = BSWorldData.get(serverLevel.getServer().overworld());
            currentDifficulty = data.difficulty;
            return getTotalMultiplier(data);
        }
        return getMultiplier(currentDifficulty);
    }

    public static double getCurrentTotalMultiplierForLevel(Level level) {
        return getCurrentTotalMultiplier(level);
    }

    public static double scaleManagedStat(Level level, double baseValue) {
        if (baseValue <= 0.0D) {
            return baseValue;
        }
        return baseValue * getCurrentTotalMultiplier(level);
    }

    public static long scaleManagedSoulReward(Level level, long baseValue) {
        if (baseValue <= 0L) {
            return baseValue;
        }
        return Math.max(1L, Math.round(baseValue * getCurrentTotalMultiplier(level)));
    }
}
