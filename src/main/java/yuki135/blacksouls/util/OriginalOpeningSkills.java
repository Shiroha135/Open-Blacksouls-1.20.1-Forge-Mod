package yuki135.blacksouls.util;

import yuki135.blacksouls.capability.BSPlayerStats;
import java.util.Map;
import java.util.Optional;
import net.minecraft.world.entity.player.Player;

public final class OriginalOpeningSkills {
    private static final Map<String,Integer> SOURCE = Map.ofEntries(
            Map.entry("bs2_skill_dodge",12),Map.entry("bs2_skill_crush",13),Map.entry("bs2_skill_soul_arrow",14),
            Map.entry("bs2_skill_soul_light",15),Map.entry("bs2_skill_strong_crush",16),Map.entry("bs2_skill_gun_fire_break",20),
            Map.entry("bs2_skill_counter",21),Map.entry("bs2_skill_mana_recovery",26),Map.entry("bs2_skill_weapon_break",105),
            Map.entry("bs2_skill_armor_break",106),Map.entry("bs2_skill_struggle",110),Map.entry("bs2_skill_mental_break",111),
            Map.entry("bs2_skill_flesh_carve",117),Map.entry("bs2_skill_blood_trail",118),Map.entry("bs2_skill_decapitation",151),
            Map.entry("bs2_skill_self_harm",308),Map.entry("bs2_skill_hide_and_seek",624));

    private OriginalOpeningSkills() {
    }

    public static Optional<Boolean> availability(Player player,String skill) {
        Integer source=SOURCE.get(skill);
        if (source==null || !OriginalOpeningEquipment.active(player)) return Optional.empty();
        var equipment=OriginalOpeningEquipment.equipment(player);
        if (equipment.stream().anyMatch(item->item.features().stream().anyMatch(feature->feature.code()==44&&feature.id()==source))) return Optional.of(false);
        var stats=player.getCapability(BSPlayerStats.CAPABILITY).orElse(null);
        boolean learned=stats!=null&&stats.unlockedSkills.contains(skill);
        boolean granted=equipment.stream().anyMatch(item->item.features().stream().anyMatch(feature->feature.code()==43&&feature.id()==source));
        return Optional.of(learned||granted);
    }

    public static boolean available(Player player,String skill,boolean fallback) {
        return availability(player,skill).orElse(fallback);
    }
}
