package yuki135.blacksouls.story;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;

public final class OriginalNpcAchievements {
    private static final Pattern SCRIPT = Pattern.compile(
            "\\Asteam = SteamUserStatsLite\\.instance\\nsteam\\.set_achievement '([^'\\r\\n]+)'\\nsteam\\.update\\z");
    private static final Map<String, ResourceLocation> ENTRIES = load();

    private OriginalNpcAchievements() {
    }

    public static String parse(String script) {
        var matcher = SCRIPT.matcher(script.replace("\r\n", "\n"));
        if (!matcher.matches() || !ENTRIES.containsKey(matcher.group(1))) {
            throw new UnsupportedOperationException("command:355");
        }
        return matcher.group(1);
    }

    public static ResourceLocation advancement(String name) {
        return ENTRIES.get(name);
    }

    public static void grant(ServerPlayer player, String name) {
        ResourceLocation id = advancement(name);
        var advancement = id == null ? null : player.server.getAdvancements().getAdvancement(id);
        if (advancement == null || !advancement.getCriteria().containsKey("triggered")) {
            throw new UnsupportedOperationException("original_achievement:" + name);
        }
        player.getAdvancements().award(advancement, "triggered");
    }

    private static Map<String, ResourceLocation> load() {
        try (InputStream stream = OriginalNpcAchievements.class.getResourceAsStream(
                "/data/blacksouls/original_npc_achievements.json")) {
            if (stream == null) throw new IllegalStateException("Missing original NPC achievement mapping");
            Map<String, ResourceLocation> entries = new LinkedHashMap<>();
            for (JsonElement element : JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonArray()) {
                var json = element.getAsJsonObject();
                String name = json.get("name").getAsString();
                ResourceLocation id = ResourceLocation.tryParse(json.get("advancement").getAsString());
                if (name.isBlank() || id == null || !id.getNamespace().equals("blacksouls")
                        || !id.getPath().startsWith("original_achievements/") || entries.put(name, id) != null) {
                    throw new IllegalStateException("Invalid original achievement mapping");
                }
            }
            return Map.copyOf(entries);
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to load original NPC achievement mapping", exception);
        }
    }
}
