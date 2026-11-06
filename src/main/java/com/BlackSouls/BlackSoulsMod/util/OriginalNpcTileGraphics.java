package com.BlackSouls.BlackSoulsMod.util;

import com.google.gson.JsonParser;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import net.minecraft.resources.ResourceLocation;

public final class OriginalNpcTileGraphics {
    private static final Map<Integer, Entry> ENTRIES = load();

    private OriginalNpcTileGraphics() {
    }

    public static Entry get(String npcId, int tileId) {
        Entry tile = ENTRIES.get(tileId);
        if (tile == null) return null;
        var source = BSOriginalNpcData.getEvent(npcId);
        if (source == null || source.mapId() != 37 || source.pages().stream().noneMatch(page -> page.graphic().has("tile_id")
                && page.graphic().get("tile_id").getAsInt() == tileId)) return null;
        return tile;
    }

    private static Map<Integer, Entry> load() {
        try (var stream = OriginalNpcTileGraphics.class.getResourceAsStream("/data/blacksouls/original_npc_tile_graphics.json")) {
            if (stream == null) throw new IllegalStateException("Missing original NPC tile graphics");
            var root = JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
            if (root.get("version").getAsInt() != 1) throw new IllegalArgumentException("Invalid original NPC tile graphics version");
            Map<Integer, Entry> entries = new LinkedHashMap<>();
            for (var value : root.getAsJsonArray("entries")) {
                var row = value.getAsJsonObject();
                int map = row.get("mapId").getAsInt(), tile = row.get("tileId").getAsInt();
                int width = row.get("width").getAsInt(), height = row.get("height").getAsInt();
                ResourceLocation texture = ResourceLocation.tryParse(row.get("texture").getAsString());
                if (map != 37 || !Set.of(41, 669).contains(tile) || width != 32 || height != 32
                        || texture == null || !texture.getNamespace().equals("blacksouls")
                        || !texture.getPath().equals("textures/entity/original_npc/tile_map037_" + tile + ".png")
                        || entries.put(tile, new Entry(texture, width, height)) != null) {
                    throw new IllegalArgumentException("Invalid original NPC tile graphic");
                }
            }
            if (!entries.keySet().equals(Set.of(41, 669))) throw new IllegalArgumentException("Incomplete original NPC tile graphics");
            return Map.copyOf(entries);
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to load original NPC tile graphics", exception);
        }
    }

    public record Entry(ResourceLocation texture, int width, int height) {
        public float blockWidth() {
            return width / 32.0F;
        }

        public float blockHeight() {
            return height / 32.0F;
        }
    }
}
