package com.BlackSouls.BlackSoulsMod.util;

import com.BlackSouls.BlackSoulsMod.capability.BSWorldData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.DefaultAttributes;
import net.minecraft.world.level.Level;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.WeakHashMap;

public class DifficultyManager {

    public static int currentDifficulty = 0;
    public static int deathCount = 0;
    public static int loopCount = 0;
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
    private static final Map<ServerLevel, Set<LivingEntity>> TRACKED_MONSTERS = new WeakHashMap<>();

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
        deathCount = data.deathCount;
        loopCount = data.loopCount;
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
        BSMobStatManager.MobStats baseStats = BSMobStatManager.getStats(monster);
        boolean managed = hasManagedStats(baseStats);
        boolean external = BSMobStatManager.isExternalEnemy(monster);
        if (!managed && !external) {
            return;
        }

        if (monster.level() instanceof ServerLevel serverLevel) {
            TRACKED_MONSTERS.computeIfAbsent(serverLevel, ignored ->
                    Collections.newSetFromMap(new WeakHashMap<>())).add(monster);
        }
        double multiplier = getCurrentTotalMultiplier(monster.level());
        if (managed) {
            applyModifier(monster, baseStats, multiplier);
        } else {
            applyExternalHealthModifier(monster, multiplier);
        }
    }

    private static void applyExternalHealthModifier(LivingEntity monster, double multiplier) {
        AttributeInstance health = monster.getAttribute(Attributes.MAX_HEALTH);
        if (health == null) {
            return;
        }

        AttributeModifier previous = health.getModifier(DIFFICULTY_MODIFIER_ID);
        float oldMaxHp = monster.getMaxHealth();
        float oldHp = monster.getHealth();
        double hpPercent = oldMaxHp > 0.0F ? oldHp / oldMaxHp : 1.0D;

        if (previous != null) {
            restoreExternalBaseHealth(monster, health);
        }

        health.removeModifier(DIFFICULTY_MODIFIER_ID);
        if (multiplier > 1.0D) {
            health.addPermanentModifier(new AttributeModifier(
                    DIFFICULTY_MODIFIER_ID,
                    "BlackSouls_Difficulty",
                    multiplier - 1.0D,
                    AttributeModifier.Operation.MULTIPLY_BASE
            ));
        }
        monster.setHealth((float) (monster.getMaxHealth() * hpPercent));
    }

    private static void restoreExternalBaseHealth(LivingEntity monster, AttributeInstance health) {
        if (DefaultAttributes.hasSupplier(monster.getType())) {
            @SuppressWarnings("unchecked")
            var type = (net.minecraft.world.entity.EntityType<? extends LivingEntity>) monster.getType();
            var supplier = DefaultAttributes.getSupplier(type);
            if (supplier.hasAttribute(Attributes.MAX_HEALTH)) {
                health.setBaseValue(supplier.getBaseValue(Attributes.MAX_HEALTH));
            }
        }
        health.removeModifier(DIFFICULTY_MODIFIER_ID);
    }

    private static void applyModifier(LivingEntity monster, BSMobStatManager.MobStats baseStats, double multiplier) {
        double amount = multiplier - 1.0D;

        float oldMaxHp = monster.getMaxHealth();
        float oldHp = monster.getHealth();
        double hpPercent = oldMaxHp > 0 ? (oldHp / oldMaxHp) : 1.0D;

        setBaseAttribute(monster, Attributes.MAX_HEALTH, baseStats.maxHealth);
        setBaseAttribute(monster, Attributes.ATTACK_DAMAGE, baseStats.attack);
        setBaseAttribute(monster, Attributes.MOVEMENT_SPEED, baseStats.getMovementSpeedAttribute());

        AttributeModifier modifier = amount > 0.0D ? new AttributeModifier(
                DIFFICULTY_MODIFIER_ID,
                "BlackSouls_Difficulty",
                amount,
                AttributeModifier.Operation.MULTIPLY_BASE
        ) : null;
        applyDifficultyModifier(monster, Attributes.MAX_HEALTH, modifier);
        applyDifficultyModifier(monster, Attributes.ATTACK_DAMAGE, modifier);

        monster.setHealth((float) (monster.getMaxHealth() * hpPercent));
    }

    private static void setBaseAttribute(LivingEntity monster, Attribute attribute, double baseValue) {
        AttributeInstance attrInstance = monster.getAttribute(attribute);
        if (attrInstance != null && baseValue > 0.0D) {
            attrInstance.setBaseValue(baseValue);
        }
    }

    private static void applyDifficultyModifier(LivingEntity monster, Attribute attribute, AttributeModifier modifier) {
        AttributeInstance attrInstance = monster.getAttribute(attribute);
        if (attrInstance != null) {
            attrInstance.removeModifier(DIFFICULTY_MODIFIER_ID);
            if (modifier != null) {
                attrInstance.addPermanentModifier(modifier);
            }
        }
    }

    public static void updateAllMonstersInstant(Level level) {
        if (level.isClientSide() || !(level instanceof ServerLevel serverLevel)) {
            return;
        }

        Set<LivingEntity> trackedMonsters = TRACKED_MONSTERS.get(serverLevel);
        if (trackedMonsters == null || trackedMonsters.isEmpty()) {
            return;
        }

        double multiplier = getCurrentTotalMultiplier(serverLevel);
        var iterator = trackedMonsters.iterator();
        while (iterator.hasNext()) {
            LivingEntity livingEntity = iterator.next();
            if (livingEntity == null || livingEntity.isRemoved() || livingEntity.level() != serverLevel) {
                iterator.remove();
                continue;
            }

            BSMobStatManager.MobStats baseStats = BSMobStatManager.getStats(livingEntity);
            boolean managed = hasManagedStats(baseStats);
            boolean external = BSMobStatManager.isExternalEnemy(livingEntity);
            if (!managed && !external) {
                iterator.remove();
                continue;
            }
            if (managed) {
                applyModifier(livingEntity, baseStats, multiplier);
            } else {
                applyExternalHealthModifier(livingEntity, multiplier);
            }
        }
    }

    public static void untrackMonster(Entity entity) {
        if (!(entity instanceof LivingEntity livingEntity) || !(entity.level() instanceof ServerLevel serverLevel)) {
            return;
        }

        Set<LivingEntity> trackedMonsters = TRACKED_MONSTERS.get(serverLevel);
        if (trackedMonsters != null) {
            trackedMonsters.remove(livingEntity);
            if (trackedMonsters.isEmpty()) {
                TRACKED_MONSTERS.remove(serverLevel);
            }
        }
    }

    private static boolean hasManagedStats(BSMobStatManager.MobStats stats) {
        return stats.maxHealth > 0.0 || stats.attack > 0.0 || stats.defense > 0.0 || stats.magicDefense > 0.0;
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
