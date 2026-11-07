package yuki135.blacksouls.util;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import net.minecraft.resources.ResourceLocation;

public final class OriginalSenInfoData {
    private static final JsonObject DATA = load();

    private OriginalSenInfoData() {
    }

    public static String title(long delta) {
        validateDelta(delta);
        return DATA.get(delta > 0 ? "upText" : "downText").getAsString();
    }

    public static String note(int current, long delta) {
        validateDelta(delta);
        return DATA.get("notePrefix").getAsString() + current + " (" + (delta > 0 ? "+" : "") + delta + ")";
    }

    public static int colorIndex(int current) {
        return current > 70 ? 3 : current > 30 ? 6 : current > 1 ? 14 : 2;
    }

    public static int color(int current) {
        return rgb(colorIndex(current));
    }

    public static int titleColor() {
        return rgb(0);
    }

    public static int iconIndex() {
        return DATA.get("iconIndex").getAsInt();
    }

    public static ResourceLocation sound(long delta) {
        validateDelta(delta);
        return ResourceLocation.tryParse(DATA.get(delta > 0 ? "gainSound" : "lossSound").getAsString());
    }

    public static float soundVolume() {
        return DATA.get("soundVolume").getAsFloat() / 100.0F;
    }

    public static float soundPitch() {
        return DATA.get("soundPitch").getAsFloat() / 100.0F;
    }

    private static void validateDelta(long delta) {
        if (delta == 0) throw new IllegalArgumentException("Original SEN info requires a nonzero change");
    }

    private static int rgb(int index) {
        JsonArray color = DATA.getAsJsonObject("colors").getAsJsonArray(Integer.toString(index));
        return color.get(0).getAsInt() << 16 | color.get(1).getAsInt() << 8 | color.get(2).getAsInt();
    }

    private static JsonObject load() {
        try (var stream = OriginalSenInfoData.class.getResourceAsStream("/data/blacksouls/original_sen_info.json")) {
            if (stream == null) throw new IllegalStateException("Missing original SEN info data");
            JsonObject value = JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
            if (value.get("version").getAsInt() != 1 || value.get("windowType").getAsInt() != 7
                    || value.get("iconIndex").getAsInt() != 103 || value.get("soundVolume").getAsInt() != 80
                    || value.get("soundPitch").getAsInt() != 100
                    || !value.getAsJsonObject("colors").keySet().equals(Set.of("0", "2", "3", "6", "14"))
                    || ResourceLocation.tryParse(value.get("gainSound").getAsString()) == null
                    || ResourceLocation.tryParse(value.get("lossSound").getAsString()) == null) {
                throw new IllegalArgumentException("Invalid original SEN info data");
            }
            for (var entry : value.getAsJsonObject("colors").entrySet()) {
                JsonArray color = entry.getValue().getAsJsonArray();
                if (color.size() != 4 || color.get(3).getAsInt() != 255) throw new IllegalArgumentException("Invalid original SEN text color");
                for (var channel : color) {
                    if (channel.getAsInt() < 0 || channel.getAsInt() > 255) throw new IllegalArgumentException("Invalid original SEN color channel");
                }
            }
            return value;
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to load original SEN info data", exception);
        }
    }
}
