package yuki135.blacksouls.util;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.resources.ResourceLocation;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

public final class OriginalNpcTextures {
    private static final JsonObject DATA = load();

    private OriginalNpcTextures() {
    }

    public static JsonObject character(String name) {
        return entry("characters", name);
    }

    public static JsonObject portrait(String name) {
        return entry("portraits", name);
    }

    public static String displayName(String portrait) {
        JsonObject names = DATA.getAsJsonObject("displayNames");
        return names != null && names.has(portrait) ? names.get(portrait).getAsString() : "";
    }

    public static ResourceLocation texture(JsonObject entry) {
        return entry == null || !entry.has("texture") ? null
                : ResourceLocation.tryParse(entry.get("texture").getAsString());
    }

    private static JsonObject entry(String category, String name) {
        JsonObject entries = DATA.getAsJsonObject(category);
        return entries == null || !entries.has(name) ? null : entries.getAsJsonObject(name).deepCopy();
    }

    private static JsonObject load() {
        try (InputStream stream = OriginalNpcTextures.class.getResourceAsStream(
                "/data/blacksouls/original_npc_textures.json")) {
            if (stream == null) {
                throw new IllegalStateException("Missing original NPC texture index");
            }
            return JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to load original NPC textures", exception);
        }
    }
}
