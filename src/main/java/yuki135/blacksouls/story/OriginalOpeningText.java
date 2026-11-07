package yuki135.blacksouls.story;

import com.google.gson.JsonParser;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class OriginalOpeningText {
    private static final Map<Integer, String> SLEEP = new LinkedHashMap<>();
    private static final Map<Integer, String> SKILLS = new LinkedHashMap<>();
    private static final List<String> POEM;
    private static final String TITLE;

    static {
        try (var input = OriginalOpeningText.class.getResourceAsStream("/data/blacksouls/original_opening_text.json")) {
            if (input == null) throw new IllegalStateException("Missing original opening text");
            var root = JsonParser.parseReader(new InputStreamReader(input, StandardCharsets.UTF_8)).getAsJsonObject();
            if (root.get("version").getAsInt() != 1) throw new IllegalArgumentException("Unsupported opening text version");
            for (var element : root.getAsJsonArray("sleep")) {
                var row = element.getAsJsonObject();
                int command = row.get("command").getAsInt();
                String text = row.get("text").getAsString();
                if (!List.of(27,31,35,40,45).contains(command) || text.isBlank() || text.length() > 2048
                        || SLEEP.putIfAbsent(command, text) != null) throw new IllegalArgumentException("Invalid source sleep text");
            }
            if (SLEEP.size() != 5) throw new IllegalArgumentException("Incomplete source sleep text");
            root.getAsJsonObject("skillNames").entrySet().forEach(entry -> SKILLS.put(Integer.parseInt(entry.getKey()), entry.getValue().getAsString()));
            var poem = new java.util.ArrayList<String>();
            for (var element : root.getAsJsonArray("poem")) poem.add(element.getAsString());
            POEM = List.copyOf(poem);
            TITLE = root.get("title").getAsString();
            if (POEM.size() != 2 || POEM.stream().anyMatch(String::isBlank) || !TITLE.equals("BLACK SOULSⅡ")) {
                throw new IllegalArgumentException("Invalid source opening poem/title");
            }
        } catch (Exception failure) {
            throw new IllegalStateException("Unable to load original opening text", failure);
        }
    }

    private OriginalOpeningText() {
    }

    public static String sleep(int command) { return SLEEP.getOrDefault(command, ""); }
    public static String poem(int index) { return index >= 0 && index < POEM.size() ? POEM.get(index) : ""; }
    public static String title() { return TITLE; }
    public static String skillName(int skill) { return SKILLS.getOrDefault(skill, "未知技能"); }
    public static String plain(String text) { return text.replaceAll("\\\\[cC]\\[\\d+\\]", ""); }
}
