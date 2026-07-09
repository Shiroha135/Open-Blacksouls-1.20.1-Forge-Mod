package com.BlackSouls.BlackSoulsMod.util;

import com.BlackSouls.BlackSoulsMod.BlackSouls;
import com.BlackSouls.BlackSoulsMod.handler.StatEventHandler;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

import java.util.List;

public class BSAttributeManager {

    public static final String TYPELESS = "typeless";
    public static final String PHYSICAL = "physical";
    public static final String FIRE = "fire";
    public static final String ICE = "ice";
    public static final String THUNDER = "thunder";
    public static final String DARK = "dark";
    public static final String LIGHT = "light";

    public static final String JABBERWOCK_KILLER = "jabberwock_killer";
    public static final String BEAST_KILLER = "beAST_killer";

    public static float getResistance(LivingEntity victim, String attribute) {
        if (attribute.equals(TYPELESS)) return 1.0f;

        float res = 1.0f;

        if (victim instanceof Player player) {
            if (attribute.equals(PHYSICAL)) {
                int ringCount = StatEventHandler.getBaubleCount(player, BlackSouls.RING_IRON_PROTECTION.get());
                if (ringCount > 0) {
                    res *= (float) Math.pow(0.8D, ringCount);
                }
            }
            if (attribute.equals(FIRE)) {
                int ringCount = StatEventHandler.getBaubleCount(player, BlackSouls.RING_FIRE_STONE.get());
                if (ringCount > 0) {
                    res *= (float) Math.pow(0.5D, ringCount);
                }
                int matchGirlClothesCount = StatEventHandler.getBaubleCount(player, BlackSouls.MATCH_GIRL_CLOTHES.get());
                if (matchGirlClothesCount > 0) {
                    res *= (float) Math.pow(0.2D, matchGirlClothesCount);
                }
                int miltonArmorCount = StatEventHandler.getBaubleCount(player, BlackSouls.MILTON_ARMOR.get());
                if (miltonArmorCount > 0) {
                    res *= (float) Math.pow(0.75D, miltonArmorCount);
                }
                int miltonHelmetCount = StatEventHandler.getBaubleCount(player, BlackSouls.MILTON_HELMET.get());
                if (miltonHelmetCount > 0) {
                    res *= (float) Math.pow(0.75D, miltonHelmetCount);
                }
            }
            if (attribute.equals(THUNDER)) {
                int ringCount = StatEventHandler.getBaubleCount(player, BlackSouls.RING_THUNDER_STONE.get());
                if (ringCount > 0) {
                    res *= (float) Math.pow(0.5D, ringCount);
                }
            }
            if (attribute.equals(DARK)) {
                int ringCount = StatEventHandler.getBaubleCount(player, BlackSouls.RING_DARK_STONE.get());
                if (ringCount > 0) {
                    res *= (float) Math.pow(0.5D, ringCount);
                }
            }
            return Math.max(0.0f, res);
        } else {
            String mobName = victim.getType().getDescriptionId();

            if (mobName.contains("crazy_bird")) {
                if (attribute.equals(LIGHT) || attribute.equals(DARK)) return 0.5f;
                if (attribute.equals(THUNDER)) return 1.5f;
            }

            if (mobName.contains("corpse_dragon") || mobName.contains("jabberwock")) {
                if (attribute.equals(JABBERWOCK_KILLER)) return 5.0f;
            }

            if (mobName.contains("wolf") || mobName.contains("bear")) {
                if (attribute.equals(BEAST_KILLER)) return 1.5f;
            }
        }

        return res;
    }

    public static float getBestMultiplier(LivingEntity victim, List<String> attackAttributes) {
        if (attackAttributes == null || attackAttributes.isEmpty()) return 1.0f;

        float bestMultiplier = 0.0f;
        for (String attr : attackAttributes) {
            float res = getResistance(victim, attr);
            if (res > bestMultiplier) {
                bestMultiplier = res;
            }
        }

        return bestMultiplier;
    }
}
