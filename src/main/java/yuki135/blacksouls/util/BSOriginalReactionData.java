package yuki135.blacksouls.util;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class BSOriginalReactionData {
    private static final Database DATABASE = load();

    private BSOriginalReactionData() {
    }

    public static BSOriginalEnemyData.Action action(int skillId) {
        Entry entry = DATABASE.skills().get(skillId);
        if (entry != null) {
            return entry.action();
        }
        return BSOriginalEnemyData.values().stream().flatMap(enemy -> enemy.actions().stream())
                .filter(action -> action.skillId() == skillId).findFirst().orElse(null);
    }

    public static BSOriginalSkillData.Entry metadata(int skillId) {
        BSOriginalSkillData.Entry original = BSOriginalSkillData.get(skillId);
        Entry entry = DATABASE.skills().get(skillId);
        return original != null ? original : entry == null ? null : entry.metadata();
    }

    public static int playerHitType(String skillId, int fallback) {
        PlayerSource source = DATABASE.players().get(skillId);
        return source == null ? fallback : source.hitType();
    }

    public static BSOriginalSkillData.Entry playerMetadata(String skillId) {
        PlayerSource source = DATABASE.players().get(skillId);
        return source == null || source.skillIds().isEmpty() ? null : metadata(source.skillIds().get(0));
    }

    private static Database load() {
        String path = "/data/blacksouls/original_reaction_skills.json";
        try (InputStream stream = BSOriginalReactionData.class.getResourceAsStream(path)) {
            if (stream == null) {
                throw new IllegalStateException("Missing original reaction skill data");
            }
            Map<Integer, Entry> entries = new LinkedHashMap<>();
            JsonObject root = JsonParser.parseReader(
                    new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
            for (JsonElement element : root.getAsJsonArray("skills")) {
                JsonObject json = element.getAsJsonObject();
                List<BSOriginalSkillData.Effect> effects = new ArrayList<>();
                List<BSOriginalEnemyData.StateEffect> states = new ArrayList<>();
                for (JsonElement effectElement : json.getAsJsonArray("effects")) {
                    JsonObject effect = effectElement.getAsJsonObject();
                    int code = integer(effect, "code");
                    int dataId = integer(effect, "dataId");
                    double first = effect.get("value1").getAsDouble();
                    effects.add(new BSOriginalSkillData.Effect(code, dataId, first,
                            effect.get("value2").getAsDouble()));
                    if (code == 21 || code == 22) {
                        states.add(new BSOriginalEnemyData.StateEffect(code, dataId, first));
                    }
                }
                int id = integer(json, "id");
                BSOriginalEnemyData.Action action = new BSOriginalEnemyData.Action(id,
                        string(json, "name"), string(json, "text"), integer(json, "animationId"),
                        integer(json, "repeats"), 5, 0, 0.0D, 0.0D,
                        integer(json, "damageType"), string(json, "formula"), integer(json, "variance"),
                        json.get("critical").getAsBoolean(), integer(json, "hitType"),
                        integer(json, "successRate"), integer(json, "elementId"), integer(json, "scope"),
                        List.copyOf(states), 0, false);
                BSOriginalSkillData.Entry metadata = new BSOriginalSkillData.Entry(id,
                        integer(json, "mpCost"), integer(json, "tpCost"), integer(json, "speed"),
                        string(json, "note"), 0, 0.0D, false, effects);
                entries.put(id, new Entry(action, metadata));
            }
            Map<String, PlayerSource> players = new LinkedHashMap<>();
            for (Map.Entry<String, JsonElement> member : root.getAsJsonObject("playerSkillSources").entrySet()) {
                JsonObject source = member.getValue().getAsJsonObject();
                List<Integer> ids = new ArrayList<>();
                for (JsonElement id : source.getAsJsonArray("sourceSkillIds")) {
                    ids.add(id.getAsInt());
                }
                players.put(member.getKey(), new PlayerSource(integer(source, "hitType"), List.copyOf(ids)));
            }
            return new Database(Map.copyOf(entries), Map.copyOf(players));
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to load original reaction skill data", exception);
        }
    }

    private static int integer(JsonObject json, String name) {
        return json.get(name).getAsInt();
    }

    private static String string(JsonObject json, String name) {
        return json.get(name).getAsString();
    }

    private record Entry(BSOriginalEnemyData.Action action, BSOriginalSkillData.Entry metadata) {
    }

    private record PlayerSource(int hitType, List<Integer> skillIds) {
    }

    private record Database(Map<Integer, Entry> skills, Map<String, PlayerSource> players) {
    }
}
