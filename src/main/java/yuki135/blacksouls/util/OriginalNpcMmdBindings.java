package yuki135.blacksouls.util;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.jetbrains.annotations.Nullable;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public final class OriginalNpcMmdBindings {
    private static final String RESOURCE = "/data/blacksouls/original_npc_mmd_bindings.json";
    private static final Map<Key, String> BINDINGS = load();

    private OriginalNpcMmdBindings() {
    }

    @Nullable
    public static String modelFor(String npcId, String currentCharacterName, int currentCharacterIndex) {
        if (npcId == null || currentCharacterName == null || currentCharacterName.isBlank()
                || currentCharacterIndex < 0 || currentCharacterIndex > 7) return null;
        return BINDINGS.get(new Key(npcId, currentCharacterName, currentCharacterIndex));
    }

    private static Map<Key, String> load() {
        try (InputStream stream = OriginalNpcMmdBindings.class.getResourceAsStream(RESOURCE)) {
            if (stream == null) throw new IllegalStateException("Missing original NPC MMD bindings");
            return parse(JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject());
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to load original NPC MMD bindings: " + RESOURCE, exception);
        }
    }

    static Map<Key, String> parse(JsonObject data) {
        require(data != null && data.keySet().equals(Set.of("version", "bindings")), "binding_root_fields");
        require(integer(data.get("version"), 1, 1) == 1, "binding_version");
        require(data.get("bindings").isJsonArray(), "binding_entries");
        require(data.getAsJsonArray("bindings").size() <= 4096, "binding_count");
        Map<Key, String> result = new LinkedHashMap<>();
        for (JsonElement value : data.getAsJsonArray("bindings")) {
            require(value.isJsonObject(), "binding_entry");
            JsonObject entry = value.getAsJsonObject();
            require(entry.keySet().equals(Set.of("npcId", "characterName", "characterIndex", "model")), "binding_fields");
            String npcId = text(entry.get("npcId"));
            String character = text(entry.get("characterName"));
            int index = integer(entry.get("characterIndex"), 0, 7);
            String model = text(entry.get("model"));
            require(npcId.matches("map[0-9]{3}_ev[0-9]{3}"), "binding_npc_id:" + npcId);
            require(!model.equals(".") && !model.equals("..") && model.indexOf('/') < 0
                    && model.indexOf('\\') < 0 && model.indexOf(':') < 0, "binding_model_key:" + model);
            BSOriginalNpcData.Entry definition = BSOriginalNpcData.get(npcId);
            require(definition != null, "binding_unknown_npc:" + npcId);
            require(definition.pages().stream().anyMatch(page -> {
                JsonObject graphic = page.graphic();
                return character.equals(graphic.get("character_name").getAsString())
                        && index == graphic.get("character_index").getAsInt();
            }), "binding_source_graphic:" + npcId + ":" + character + ":" + index);
            require(result.putIfAbsent(new Key(npcId, character, index), model) == null, "binding_duplicate:" + npcId);
        }
        return Map.copyOf(result);
    }

    private static String text(JsonElement value) {
        require(value != null && value.isJsonPrimitive() && value.getAsJsonPrimitive().isString(), "binding_string");
        String text = value.getAsString();
        require(!text.isBlank() && text.equals(text.strip()) && text.length() <= 256
                && text.codePoints().noneMatch(Character::isISOControl), "binding_string_value");
        return text;
    }

    private static int integer(JsonElement value, int minimum, int maximum) {
        require(value != null && value.isJsonPrimitive() && value.getAsJsonPrimitive().isNumber(), "binding_integer");
        int number;
        try {
            number = value.getAsBigDecimal().intValueExact();
        } catch (ArithmeticException exception) {
            throw new IllegalArgumentException("binding_integer", exception);
        }
        require(number >= minimum && number <= maximum, "binding_integer_range");
        return number;
    }

    private static void require(boolean accepted, String reason) {
        if (!accepted) throw new IllegalArgumentException(reason);
    }

    record Key(String npcId, String characterName, int characterIndex) {
    }
}
