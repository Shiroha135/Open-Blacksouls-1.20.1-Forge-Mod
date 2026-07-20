package com.BlackSouls.BlackSoulsMod.util;

import com.BlackSouls.BlackSoulsMod.BlackSouls;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.HashMap;
import java.util.Map;

public class BSMobStatManager {

    public static class MobStats {
        public final double maxHealth;
        public final double maxMana;
        public final double attack;
        public final double defense;
        public final double magicAttack;
        public final double magicDefense;
        public final double speed;
        public final double luck;
        public final long soulReward;

        public MobStats(double maxHealth, double maxMana, double attack, double defense,
                        double magicAttack, double magicDefense, double speed, double luck, long soulReward) {
            this.maxHealth = maxHealth;
            this.maxMana = maxMana;
            this.attack = attack;
            this.defense = defense;
            this.magicAttack = magicAttack;
            this.magicDefense = magicDefense;
            this.speed = speed;
            this.luck = luck;
            this.soulReward = soulReward;
        }

        public double getMovementSpeedAttribute() {
            return Math.max(0.12D, Math.min(0.40D, 0.06D + (this.speed * 0.004D)));
        }
    }

    private static final MobStats EMPTY_STATS = new MobStats(0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0L);
    private static final MobStats DEFAULT_HOSTILE_STATS = new MobStats(220.0, 0.0, 110.0, 18.0, 40.0, 16.0, 36.0, 10.0, 70L);
    private static final Map<ResourceLocation, MobStats> MOB_STATS_DB = new HashMap<>();

    static {
        register("zombie", 220.0, 0.0, 110.0, 18.0, 0.0, 10.0, 34.0, 8.0, 50L);
        register("zombie_villager", 230.0, 0.0, 115.0, 20.0, 0.0, 10.0, 33.0, 8.0, 55L);
        register("drowned", 240.0, 0.0, 120.0, 22.0, 18.0, 14.0, 32.0, 8.0, 60L);
        register("husk", 260.0, 0.0, 130.0, 24.0, 0.0, 14.0, 33.0, 8.0, 65L);

        register("skeleton", 180.0, 30.0, 105.0, 12.0, 48.0, 22.0, 35.0, 12.0, 55L);
        register("stray", 200.0, 40.0, 115.0, 14.0, 56.0, 24.0, 34.0, 12.0, 65L);
        register("spider", 170.0, 0.0, 100.0, 14.0, 0.0, 10.0, 52.0, 14.0, 45L);
        register("cave_spider", 150.0, 0.0, 92.0, 12.0, 0.0, 10.0, 56.0, 16.0, 40L);
        register("creeper", 200.0, 0.0, 150.0, 8.0, 0.0, 8.0, 33.0, 10.0, 70L);
        register("slime", 140.0, 0.0, 85.0, 8.0, 0.0, 6.0, 31.0, 4.0, 35L);
        register("magma_cube", 180.0, 0.0, 110.0, 14.0, 25.0, 16.0, 26.0, 4.0, 55L);
        register("silverfish", 110.0, 0.0, 80.0, 6.0, 0.0, 6.0, 54.0, 6.0, 30L);
        register("endermite", 120.0, 0.0, 90.0, 6.0, 0.0, 7.0, 56.0, 6.0, 35L);

        register("enderman", 420.0, 60.0, 180.0, 30.0, 72.0, 34.0, 60.0, 18.0, 180L);
        register("witch", 240.0, 150.0, 70.0, 12.0, 95.0, 34.0, 30.0, 20.0, 150L);
        register("blaze", 300.0, 120.0, 90.0, 18.0, 105.0, 38.0, 38.0, 16.0, 170L);
        register("ghast", 360.0, 180.0, 70.0, 12.0, 120.0, 38.0, 24.0, 14.0, 180L);
        register("phantom", 160.0, 0.0, 100.0, 10.0, 0.0, 12.0, 58.0, 14.0, 65L);
        register("guardian", 320.0, 50.0, 120.0, 24.0, 80.0, 28.0, 28.0, 12.0, 150L);
        register("elder_guardian", 700.0, 180.0, 180.0, 44.0, 135.0, 44.0, 24.0, 18.0, 480L);
        register("shulker", 420.0, 80.0, 90.0, 38.0, 70.0, 42.0, 12.0, 18.0, 200L);

        register("pillager", 240.0, 20.0, 110.0, 16.0, 0.0, 18.0, 34.0, 12.0, 90L);
        register("vindicator", 360.0, 0.0, 180.0, 26.0, 0.0, 18.0, 36.0, 10.0, 180L);
        register("evoker", 300.0, 220.0, 90.0, 18.0, 140.0, 40.0, 32.0, 18.0, 260L);
        register("ravager", 900.0, 0.0, 260.0, 40.0, 0.0, 22.0, 34.0, 8.0, 600L);
        register("piglin", 260.0, 20.0, 120.0, 20.0, 0.0, 16.0, 38.0, 12.0, 95L);
        register("piglin_brute", 500.0, 0.0, 210.0, 34.0, 0.0, 20.0, 36.0, 10.0, 280L);
        register("hoglin", 440.0, 0.0, 170.0, 28.0, 0.0, 14.0, 40.0, 6.0, 180L);
        register("zoglin", 520.0, 0.0, 190.0, 30.0, 0.0, 16.0, 40.0, 6.0, 210L);
        register("wither_skeleton", 380.0, 40.0, 165.0, 30.0, 35.0, 26.0, 34.0, 12.0, 220L);

        register("warden", 4200.0, 0.0, 420.0, 110.0, 0.0, 80.0, 38.0, 20.0, 3000L);
        register("wither", 5600.0, 300.0, 340.0, 95.0, 220.0, 92.0, 34.0, 20.0, 5000L);
        register("ender_dragon", 8000.0, 400.0, 380.0, 120.0, 260.0, 100.0, 42.0, 24.0, 8000L);
        registerMod("hail_caesar", 20000.0, 700.0, 500.0, 100.0, 100.0, 100.0, 400.0, 200.0, 15000L);
        
        registerMod("hell_prince", 1900000.0, 9999.0, 2400.0, 960.0, 2700.0, 990.0, 8550.0, 8100.0, 160000L);
    }

    private static void register(String id, double hp, double mp, double atk, double def,
                                 double matk, double mdef, double speed, double luck, long soulReward) {
        register(new ResourceLocation("minecraft", id), hp, mp, atk, def, matk, mdef, speed, luck, soulReward);
    }

    private static void registerMod(String id, double hp, double mp, double atk, double def,
                                    double matk, double mdef, double speed, double luck, long soulReward) {
        register(new ResourceLocation(BlackSouls.MODID, id), hp, mp, atk, def, matk, mdef, speed, luck, soulReward);
    }

    private static void register(ResourceLocation id, double hp, double mp, double atk, double def,
                                 double matk, double mdef, double speed, double luck, long soulReward) {
        MOB_STATS_DB.put(id, new MobStats(hp, mp, atk, def, matk, mdef, speed, luck, soulReward));
    }

    public static MobStats getStats(LivingEntity entity) {
        if (entity == null) {
            return EMPTY_STATS;
        }

        ResourceLocation key = ForgeRegistries.ENTITY_TYPES.getKey(entity.getType());
        if (key == null) {
            return EMPTY_STATS;
        }

        MobStats stats = MOB_STATS_DB.get(key);
        if (stats != null) {
            return stats;
        }

        if (entity instanceof Enemy) {
            return DEFAULT_HOSTILE_STATS;
        }

        return EMPTY_STATS;
    }

    public static long getSoulReward(LivingEntity entity) {
        return getStats(entity).soulReward;
    }

    public static boolean hasManagedStats(LivingEntity entity) {
        MobStats stats = getStats(entity);
        return stats.maxHealth > 0.0 || stats.attack > 0.0 || stats.defense > 0.0 || stats.magicDefense > 0.0;
    }
}
