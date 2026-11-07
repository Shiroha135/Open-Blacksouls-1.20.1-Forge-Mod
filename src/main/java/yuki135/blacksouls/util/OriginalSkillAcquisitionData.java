package yuki135.blacksouls.util;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

public final class OriginalSkillAcquisitionData {
    private static final JsonObject DATA = load();

    private OriginalSkillAcquisitionData() {
    }

    public static void validate(int skill, int actorOperand) {
        if (skill != 340 || !DATA.getAsJsonObject("actorNames").has(Integer.toString(actorOperand))) {
            throw new UnsupportedOperationException("original_skill_info_identity:" + skill + ":" + actorOperand);
        }
    }

    public static String title(int actorOperand, boolean learned) {
        validate(340, actorOperand);
        return DATA.getAsJsonObject("actorNames").get(Integer.toString(actorOperand)).getAsString()
                + " / " + DATA.get(learned ? "learnText" : "forgetText").getAsString();
    }

    public static String name() {
        return DATA.getAsJsonObject("skill").get("name").getAsString();
    }

    public static String description() {
        return DATA.getAsJsonObject("skill").get("description").getAsString();
    }

    public static int iconIndex() {
        return DATA.getAsJsonObject("skill").get("iconIndex").getAsInt();
    }

    private static JsonObject load() {
        try (var stream = OriginalSkillAcquisitionData.class.getResourceAsStream("/data/blacksouls/original_skill_acquisition.json")) {
            if (stream == null) throw new IllegalStateException("Missing original skill acquisition data");
            JsonObject value = JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
            if (value.get("version").getAsInt() != 1 || value.get("displaySwitch").getAsInt() != 22
                    || value.get("sourceWidth").getAsInt() != 640 || value.get("sourceHeight").getAsInt() != 480
                    || value.get("windowX").getAsInt() != -16 || value.get("windowWidth").getAsInt() != 672
                    || value.get("windowHeight").getAsInt() != 70 || value.get("padding").getAsInt() != 12
                    || value.get("firstWindowY").getAsInt() != 422 || value.get("slotSpacing").getAsInt() != 40
                    || value.get("holdFrames").getAsInt() != 180 || value.get("opacityStep").getAsInt() != 32
                    || value.get("backgroundAlpha").getAsInt() != 160 || value.get("titleFontSize").getAsInt() != 14
                    || value.get("bodyFontSize").getAsInt() != 22 || value.getAsJsonObject("skill").get("id").getAsInt() != 340
                    || value.getAsJsonObject("skill").get("iconIndex").getAsInt() != 474
                    || value.getAsJsonObject("actorNames").size() != 2
                    || !value.getAsJsonObject("actorNames").has("2") || !value.getAsJsonObject("actorNames").has("14")) {
                throw new IllegalArgumentException("Invalid original skill acquisition data");
            }
            return value;
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to load original skill acquisition data", exception);
        }
    }
}
