package com.BlackSouls.BlackSoulsMod.story;

import com.BlackSouls.BlackSoulsMod.capability.OriginalNpcProgress;
import com.BlackSouls.BlackSoulsMod.entity.EntityOriginalNpc;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import net.minecraft.server.level.ServerPlayer;

public final class OriginalSymbolMap37 {
    private static final JsonObject DATA = load();
    private static final byte[] MASKS = Base64.getDecoder().decode(DATA.get("basePassageMasks").getAsString());

    private OriginalSymbolMap37() {
    }

    private static JsonObject load() {
        try (var stream = OriginalSymbolMap37.class.getResourceAsStream("/data/blacksouls/original_symbol_map037.json")) {
            if (stream == null) throw new IllegalStateException("Missing source Map037 symbol passage");
            var data = JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
            if (data.get("version").getAsInt() != 1 || data.get("mapId").getAsInt() != 37
                    || data.get("minX").getAsInt() != 0 || data.get("maxX").getAsInt() != 64
                    || data.get("minY").getAsInt() != 34 || data.get("maxY").getAsInt() != 64
                    || Base64.getDecoder().decode(data.get("basePassageMasks").getAsString()).length != 2015) throw new IllegalArgumentException("Invalid source Map037 symbol passage");
            return data;
        } catch (java.io.IOException exception) {
            throw new IllegalStateException(exception);
        }
    }

    public static boolean allows(ServerPlayer player, EntityOriginalNpc npc, int x, int y, int direction) {
        if (player == null || !npc.getNpcId().equals("map037_ev053")) return false;
        int dx = direction == 4 ? -1 : direction == 6 ? 1 : 0;
        int dy = direction == 8 ? -1 : direction == 2 ? 1 : 0;
        if (!List.of(2,4,6,8).contains(direction) || x < 0 || x > 64 || y < 34 || y > 64
                || x+dx < 0 || x+dx > 64 || y+dy < 34 || y+dy > 64) return false;
        var context = new OriginalNpcService.EventContext(player, npc);
        if (!passage(player, npc, context, x, y, direction) || !passage(player, npc, context, x+dx, y+dy, 10-direction)) return false;
        if (OriginalPoolCrowdMovementService.blocksSourceTile(npc, player, x+dx, y+dy)) return false;
        return true;
    }

    private static boolean passage(ServerPlayer player, EntityOriginalNpc npc, OriginalNpcService.EventContext context, int x, int y, int direction) {
        int bit = 1 << (direction/2-1);
        for (var element : DATA.getAsJsonArray("events")) {
            var event = element.getAsJsonObject();
            if (event.get("id").getAsInt() == 53 || event.get("x").getAsInt() != x || event.get("y").getAsInt() != y) continue;
            var page = selected(player, context, event);
            if (page == null || page.get("through").getAsBoolean() || page.get("priority").getAsInt() != 0 || page.get("tileId").getAsInt() == 0) continue;
            int flag = page.get("tileFlag").getAsInt();
            if ((flag & 0x10) == 0) return (flag & bit) == 0;
        }
        return (MASKS[(y-34)*65+x] & bit) != 0;
    }

    private static JsonObject selected(ServerPlayer player, OriginalNpcService.EventContext context, JsonObject event) {
        JsonArray pages = event.getAsJsonArray("pages");
        String identity = "map037_ev%03d".formatted(event.get("id").getAsInt());
        for (int index = pages.size()-1; index >= 0; index--) {
            var page = pages.get(index).getAsJsonObject();
            var c = page.getAsJsonObject("condition");
            if (c.get("switch1_valid").getAsBoolean() && !context.switchValue(c.get("switch1_id").getAsInt())
                    || c.get("switch2_valid").getAsBoolean() && !context.switchValue(c.get("switch2_id").getAsInt())
                    || c.get("variable_valid").getAsBoolean() && context.variable(c.get("variable_id").getAsInt()) < c.get("variable_value").getAsLong()
                    || c.get("self_switch_valid").getAsBoolean() && !OriginalNpcProgress.get(player.server).selfSwitch(identity,c.get("self_switch_ch").getAsString())
                    || c.get("item_valid").getAsBoolean() && context.itemCount(0,c.get("item_id").getAsInt()) <= 0
                    || c.get("actor_valid").getAsBoolean() && !context.hasActor(c.get("actor_id").getAsInt())) continue;
            return page;
        }
        return null;
    }
}
