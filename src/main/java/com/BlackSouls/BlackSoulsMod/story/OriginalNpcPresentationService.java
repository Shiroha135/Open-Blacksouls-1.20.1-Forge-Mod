package com.BlackSouls.BlackSoulsMod.story;

import com.BlackSouls.BlackSoulsMod.entity.EntityOriginalNpc;
import com.BlackSouls.BlackSoulsMod.entity.EntityRabbitKnight;
import com.BlackSouls.BlackSoulsMod.network.NetworkHandler;
import com.BlackSouls.BlackSoulsMod.network.packets.ClientboundOriginalNpcPresentationPacket;
import com.BlackSouls.BlackSoulsMod.util.BSOriginalNpcData;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.registries.ForgeRegistries;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

public final class OriginalNpcPresentationService {
    private static final JsonObject AUDIO = loadAudio();

    private OriginalNpcPresentationService() {
    }

    public static void apply(ServerPlayer player, EntityOriginalNpc npc, int code, List<JsonElement> parameters) {
        NetworkHandler.sendToPlayer(createPacket(player, npc, code, parameters), player);
    }

    public static void apply(ServerPlayer player, Entity npc, int code, List<JsonElement> parameters) {
        NetworkHandler.sendToPlayer(createPacket(player, npc, code, parameters), player);
    }

    public static ClientboundOriginalNpcPresentationPacket createPacket(ServerPlayer player, EntityOriginalNpc npc,
                                                                       int code, List<JsonElement> parameters) {
        return createPacket(player, (Entity) npc, code, parameters);
    }

    public static ClientboundOriginalNpcPresentationPacket createPacket(ServerPlayer player, Entity subject,
                                                                       int code, List<JsonElement> parameters) {
        int target = subject.getId();
        List<Double> values = new ArrayList<>();
        String sound = "";
        switch (code) {
            case 221, 222, 243, 244 -> {
            }
            case 223, 224 -> {
                double[] channels = decodeChannels(object(parameters, 0), code == 223);
                for (double channel : channels) {
                    values.add(channel);
                }
                values.add((double) integer(parameters, 1, 0, 360000));
                bool(parameters, 2);
            }
            case 225 -> {
                values.add((double) integer(parameters, 0, 0, 9));
                values.add((double) integer(parameters, 1, 0, 9));
                values.add((double) integer(parameters, 2, 0, 360000));
                bool(parameters, 3);
            }
            case 213 -> {
                if (subject instanceof EntityRabbitKnight rabbit && OriginalRabbitKnightBinding.matches(rabbit)) {
                    int requested = integer(parameters, 0, -1, Integer.MAX_VALUE);
                    if (requested != -1 && requested != 0) throw unsupported(code, "Unsupported bound rabbit balloon target");
                    target = requested == -1 ? player.getId() : rabbit.getId();
                    values.add((double) integer(parameters, 1, 1, 10));
                    bool(parameters, 2);
                    break;
                }
                if (!(subject instanceof EntityOriginalNpc npc)) {
                    throw unsupported(code, "Original character balloon requires a map event context");
                }
                target = resolveCharacter(player, npc, integer(parameters, 0, -1, Integer.MAX_VALUE)).getId();
                values.add((double) integer(parameters, 1, 1, 10));
                bool(parameters, 2);
            }
            case 241, 250 -> {
                JsonObject audio = object(parameters, 0);
                String name = audio.has("name") ? audio.get("name").getAsString() : "";
                if (!name.isEmpty()) {
                    JsonObject category = AUDIO.getAsJsonObject(code == 241 ? "bgm" : "se");
                    if (!category.has(name)) {
                        throw unsupported(code, "Unmapped original audio " + name);
                    }
                    ResourceLocation id = ResourceLocation.tryParse(category.getAsJsonObject(name).get("sound").getAsString());
                    if (id == null || !ForgeRegistries.SOUND_EVENTS.containsKey(id)) {
                        throw unsupported(code, "Original sound event is unavailable " + name);
                    }
                    sound = id.toString();
                }
                values.add(audioNumber(audio, "volume", 0, 100));
                values.add(audioNumber(audio, "pitch", 50, 150));
            }
            case 242 -> values.add((double) integer(parameters, 0, 0, 3600));
            default -> throw unsupported(code, "Unsupported presentation command");
        }
        return new ClientboundOriginalNpcPresentationPacket(code, target,
                player.level().dimension().location(), values, sound);
    }

    public static int waitTicks(int code, List<JsonElement> parameters) {
        return switch (code) {
            case 221, 222 -> 10;
            case 223, 224 -> bool(parameters, 2) ? framesToTicks(integer(parameters, 1, 0, 360000)) : 0;
            case 225 -> bool(parameters, 3) ? framesToTicks(integer(parameters, 2, 0, 360000)) : 0;
            case 213 -> bool(parameters, 2) ? framesToTicks(76) : 0;
            case 241, 242, 243, 244, 250 -> 0;
            default -> throw unsupported(code, "Unsupported presentation wait");
        };
    }

    public static int framesToTicks(int frames) {
        if (frames < 0 || frames > 360000) {
            throw new IllegalArgumentException("Invalid original frame duration");
        }
        return (frames + 2) / 3;
    }

    public static double[] decodeChannels(JsonObject object, boolean tone) {
        String expected = tone ? "Tone" : "Color";
        if (!object.has("_class") || !expected.equals(object.get("_class").getAsString())
                || !object.has("marshal_data_base64")) {
            throw unsupported(tone ? 223 : 224, "Invalid original " + expected + " payload");
        }
        byte[] bytes;
        try {
            bytes = Base64.getDecoder().decode(object.get("marshal_data_base64").getAsString());
        } catch (IllegalArgumentException exception) {
            throw unsupported(tone ? 223 : 224, "Invalid original channel encoding");
        }
        if (bytes.length != 32) {
            throw unsupported(tone ? 223 : 224, "Original channels require four little-endian doubles");
        }
        ByteBuffer buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);
        double[] result = new double[4];
        for (int index = 0; index < 4; index++) {
            double value = buffer.getDouble();
            double minimum = tone && index < 3 ? -255.0D : 0.0D;
            if (!Double.isFinite(value) || value < minimum || value > 255.0D) {
                throw unsupported(tone ? 223 : 224, "Original channel is outside its valid range");
            }
            result[index] = value;
        }
        return result;
    }

    private static Entity resolveCharacter(ServerPlayer player, EntityOriginalNpc npc, int eventId) {
        if (eventId == -1) {
            return player;
        }
        if (eventId == 0) {
            return npc;
        }
        BSOriginalNpcData.Entry source = BSOriginalNpcData.get(npc.getNpcId());
        if (source == null) {
            throw unsupported(213, "NPC has no original map context");
        }
        EntityOriginalNpc nearest = null;
        for (Entity entity : player.serverLevel().getAllEntities()) {
            if (!(entity instanceof EntityOriginalNpc candidate)) {
                continue;
            }
            BSOriginalNpcData.Entry entry = BSOriginalNpcData.get(candidate.getNpcId());
            if (entry != null && entry.mapId() == source.mapId() && entry.eventId() == eventId
                    && (nearest == null || npc.distanceToSqr(candidate) < npc.distanceToSqr(nearest))) {
                nearest = candidate;
            }
        }
        if (nearest == null) {
            throw unsupported(213, "Referenced original character is not loaded " + eventId);
        }
        return nearest;
    }

    private static JsonObject object(List<JsonElement> parameters, int index) {
        if (index >= parameters.size() || !parameters.get(index).isJsonObject()) {
            throw unsupported(0, "Missing original object parameter");
        }
        return parameters.get(index).getAsJsonObject();
    }

    private static int integer(List<JsonElement> parameters, int index, int minimum, int maximum) {
        if (index >= parameters.size() || !parameters.get(index).isJsonPrimitive()
                || !parameters.get(index).getAsJsonPrimitive().isNumber()) {
            throw unsupported(0, "Missing original numeric parameter");
        }
        double value = parameters.get(index).getAsDouble();
        if (!Double.isFinite(value) || value != Math.rint(value) || value < minimum || value > maximum) {
            throw unsupported(0, "Invalid original numeric parameter");
        }
        return (int) value;
    }

    private static boolean bool(List<JsonElement> parameters, int index) {
        if (index >= parameters.size() || !parameters.get(index).isJsonPrimitive()
                || !parameters.get(index).getAsJsonPrimitive().isBoolean()) {
            throw unsupported(0, "Missing original wait flag");
        }
        return parameters.get(index).getAsBoolean();
    }

    private static double audioNumber(JsonObject object, String key, double minimum, double maximum) {
        double value = object.has(key) ? object.get(key).getAsDouble() : Double.NaN;
        if (!Double.isFinite(value) || value < minimum || value > maximum) {
            throw unsupported(250, "Invalid original audio " + key);
        }
        return value;
    }

    private static UnsupportedOperationException unsupported(int code, String reason) {
        return new UnsupportedOperationException(reason + " (command " + code + ")");
    }

    private static JsonObject loadAudio() {
        try (InputStream stream = OriginalNpcPresentationService.class.getResourceAsStream(
                "/data/blacksouls/original_npc_audio.json")) {
            if (stream == null) {
                throw new IllegalStateException("Missing original NPC audio index");
            }
            return JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to load original NPC audio index", exception);
        }
    }
}
