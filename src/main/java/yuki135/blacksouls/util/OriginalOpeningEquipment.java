package yuki135.blacksouls.util;

import yuki135.blacksouls.capability.BSPlayerStats;
import yuki135.blacksouls.story.OriginalOpeningProfile;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import top.theillusivec4.curios.api.CuriosApi;

public final class OriginalOpeningEquipment {
    private static final Map<String, Traits> DATA = load();

    private OriginalOpeningEquipment() {
    }

    public static boolean active(Player player) {
        if (player == null) return false;
        return player.getCapability(BSPlayerStats.CAPABILITY).map(stats -> stats.openingProfile().readState() == OriginalOpeningProfile.ReadState.REAL
                && stats.openingProfile().realOriginId() > 0).orElse(false);
    }

    public static List<OriginalEquipmentData.Equipment> equipment(Player player) {
        if (!active(player)) return List.of();
        var result = new ArrayList<OriginalEquipmentData.Equipment>();
        add(result, fitted(player.getMainHandItem(), 0));
        add(result, fitted(player.getOffhandItem(), 1));
        add(result, fitted(curio(player, "head", 0, player.getItemBySlot(EquipmentSlot.HEAD)), 2));
        add(result, fitted(curio(player, "body", 0, player.getItemBySlot(EquipmentSlot.CHEST)), 3));
        for (int index = 0; index < 4; index++) add(result, fitted(curio(player, "ring", index, ItemStack.EMPTY), 4));
        return List.copyOf(result);
    }

    public static boolean handled(Player player, ItemStack stack, int sourceSlot) {
        return active(player) && sourceSlot >= 0 && sourceSlot < 8 && fitted(stack, sourceSlot < 4 ? sourceSlot : 4) != null;
    }

    public static double rate(Player player, int code, int dataId) {
        return rate(equipment(player), code, dataId);
    }

    public static double sum(Player player, int code, int dataId) {
        return sum(equipment(player), code, dataId);
    }

    public static void apply(Player player, BSPlayerStats stats) {
        if (!active(player)) return;
        List<OriginalEquipmentData.Equipment> equipment = equipment(player);
        double[] values = parameters(new double[] {stats.hp, stats.maxMp, stats.attack, stats.defense,
                stats.magicAttack, stats.magicDefense, stats.speed, stats.luck}, equipment, stats.developerLimitBreak);
        stats.hp = values[0]; stats.maxMp = values[1]; stats.attack = values[2]; stats.defense = values[3];
        stats.magicAttack = values[4]; stats.magicDefense = values[5]; stats.speed = values[6]; stats.luck = values[7];
        stats.evasion += sum(equipment, 22, 1) * 100.0D;
        stats.critRate += sum(equipment, 22, 2) * 100.0D;
        stats.magicEvasion += sum(equipment, 22, 4) * 100.0D;
        stats.hpRegenRate += sum(equipment, 22, 7);
        stats.mpRegenRate += sum(equipment, 22, 8);
        stats.targetingRate *= rate(equipment, 23, 0);
        stats.mpCostRate *= rate(equipment, 23, 4);
        stats.physicalDamageRate *= rate(equipment, 23, 6);
        stats.magicDamageRate *= rate(equipment, 23, 7);
    }

    static double[] parameters(double[] base, List<OriginalEquipmentData.Equipment> equipment, boolean limitBreak) {
        if (base == null || base.length != 8) throw new IllegalArgumentException("Invalid opening parameter vector");
        double[] result = new double[8];
        for (int parameter = 0; parameter < 8; parameter++) {
            double value = base[parameter];
            for (var item : equipment) value += item.parameters().get(parameter);
            value *= rate(equipment, 21, parameter);
            if (!Double.isFinite(value)) throw new IllegalArgumentException("Nonfinite opening equipment parameter");
            double maximum = parameter == 0 ? BSPlayerStats.HARD_CAP_HP : parameter == 1 ? BSPlayerStats.HARD_CAP_MP : BSPlayerStats.HARD_CAP_OTHER;
            result[parameter] = Math.floor(Math.max(parameter == 1 ? 0.0D : 1.0D, limitBreak ? value : Math.min(maximum, value)));
        }
        return result;
    }

    static double rate(List<OriginalEquipmentData.Equipment> equipment, int code, int dataId) {
        double result = 1.0D;
        for (var item : equipment) for (var feature : traits(item).features) {
            if (feature.code == code && feature.id == dataId) result *= feature.value;
        }
        return result;
    }

    static double sum(List<OriginalEquipmentData.Equipment> equipment, int code, int dataId) {
        double result = 0.0D;
        for (var item : equipment) for (var feature : traits(item).features) {
            if (feature.code == code && feature.id == dataId) result += feature.value;
        }
        return result;
    }

    private static Traits traits(OriginalEquipmentData.Equipment item) {
        var result = DATA.get(key(item.kind(), item.id()));
        if (result == null) throw new IllegalArgumentException("Equipment outside opening source families");
        return result;
    }

    private static OriginalEquipmentData.Equipment fitted(ItemStack stack, int equipmentType) {
        if (stack == null || stack.isEmpty()) return null;
        if (equipmentType == 0) {
            int id = OriginalNpcItems.sourceId(stack);
            if (!DATA.containsKey(key(1, id))) return null;
            var item = OriginalEquipmentData.get(1, id);
            return item != null && item.equipmentType() == equipmentType ? item : null;
        }
        for (var item : DATA.values()) if (item.kind == 2 && OriginalNpcItems.matches(2, item.id, stack)) {
            var source = OriginalEquipmentData.get(2, item.id);
            return source != null && source.equipmentType() == equipmentType ? source : null;
        }
        return null;
    }

    private static ItemStack curio(Player player, String identifier, int index, ItemStack fallback) {
        return CuriosApi.getCuriosInventory(player).map(inventory -> inventory.getStacksHandler(identifier).map(group -> {
            if (index >= group.getStacks().getSlots()) return fallback;
            ItemStack stack = group.getStacks().getStackInSlot(index);
            return stack.isEmpty() ? fallback : stack;
        }).orElse(fallback)).orElse(fallback);
    }

    private static void add(List<OriginalEquipmentData.Equipment> result, OriginalEquipmentData.Equipment item) {
        if (item != null) result.add(item);
    }

    private static Map<String, Traits> load() {
        try (var input = OriginalOpeningEquipment.class.getResourceAsStream("/data/blacksouls/original_opening_equipment.json")) {
            if (input == null) throw new IllegalStateException("Missing opening equipment source features");
            JsonObject root = JsonParser.parseReader(new InputStreamReader(input, StandardCharsets.UTF_8)).getAsJsonObject();
            if (!root.keySet().equals(Set.of("version", "sourceFiles", "weaponFamilyBaseIds", "equipment")) || integer(root, "version") != 1) {
                throw new IllegalArgumentException("Invalid opening equipment schema");
            }
            JsonObject files = root.getAsJsonObject("sourceFiles");
            if (!files.keySet().equals(Set.of("Weapons.rvdata2", "Armors.rvdata2"))) throw new IllegalArgumentException("Invalid opening equipment source scope");
            for (String file : files.keySet()) if (!files.get(file).getAsString().equals(OriginalOpeningData.catalog().sourceFiles().get(file))) {
                throw new IllegalArgumentException("Opening equipment source revision mismatch");
            }
            Set<Integer> families = new LinkedHashSet<>();
            for (var value : root.getAsJsonArray("weaponFamilyBaseIds")) if (!families.add(value.getAsBigDecimal().intValueExact())) {
                throw new IllegalArgumentException("Duplicate opening weapon family");
            }
            Set<Integer> expectedFamilies = new LinkedHashSet<>();
            Set<String> expected = new LinkedHashSet<>();
            for (var profession : OriginalOpeningData.catalog().professions()) for (var slot : profession.initialEquipment()) {
                if (slot.kind() == 1 && slot.id() > 0) expectedFamilies.add(slot.id());
                if (slot.kind() == 2 && slot.id() > 0) expected.add(key(2, slot.id()));
            }
            if (!families.equals(expectedFamilies) || families.size() != 8) throw new IllegalArgumentException("Invalid opening weapon family scope");
            for (int id = 1; id <= 500; id++) {
                var form = OriginalWeaponForms.get(id);
                if (form != null && families.stream().anyMatch(base -> OriginalWeaponForms.get(base).registry().equals(form.registry()))) expected.add(key(1, id));
            }
            Map<String, Traits> result = new LinkedHashMap<>();
            for (var value : root.getAsJsonArray("equipment")) {
                JsonObject row = value.getAsJsonObject();
                if (!row.keySet().equals(Set.of("kind", "id", "baseId", "features"))) throw new IllegalArgumentException("Invalid opening equipment row");
                int kind = integer(row, "kind"), id = integer(row, "id"), base = integer(row, "baseId");
                var item = OriginalEquipmentData.get(kind, id);
                if (item == null || !expected.contains(key(kind, id)) || kind == 2 && base != id
                        || kind == 1 && (!families.contains(base) || !OriginalWeaponForms.get(base).registry().equals(OriginalWeaponForms.get(id).registry()))) {
                    throw new IllegalArgumentException("Invalid opening equipment identity");
                }
                var features = new ArrayList<Feature>();
                for (var raw : row.getAsJsonArray("features")) {
                    var feature = raw.getAsJsonObject();
                    if (!feature.keySet().equals(Set.of("code", "id", "value"))) throw new IllegalArgumentException("Invalid opening feature row");
                    double amount = feature.get("value").getAsDouble();
                    if (!Double.isFinite(amount)) throw new IllegalArgumentException("Invalid opening feature value");
                    features.add(new Feature(integer(feature, "code"), integer(feature, "id"), amount));
                }
                var identities = features.stream().map(feature -> new OriginalEquipmentData.Feature(feature.code, feature.id)).toList();
                if (!identities.equals(item.features()) || result.putIfAbsent(key(kind, id), new Traits(kind, id, List.copyOf(features))) != null) {
                    throw new IllegalArgumentException("Opening source feature identity mismatch");
                }
            }
            if (!result.keySet().equals(expected) || result.size() != 101) throw new IllegalArgumentException("Incomplete opening equipment source families");
            return Map.copyOf(result);
        } catch (java.io.IOException exception) {
            throw new IllegalStateException("Failed to load opening equipment source features", exception);
        }
    }

    private static int integer(JsonObject object, String field) { return object.get(field).getAsBigDecimal().intValueExact(); }
    private static String key(int kind, int id) { return kind + ":" + id; }
    private record Feature(int code, int id, double value) { }
    private record Traits(int kind, int id, List<Feature> features) { }
}
