package com.BlackSouls.BlackSoulsMod.util;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class OriginalNpcNameWindow {
    private static final Pattern COMMENT = Pattern.compile("^(\\S+)(.*)$", Pattern.DOTALL);
    private static final Pattern ACTOR = Pattern.compile("Actor\\[(\\d+)\\]");
    private static final Pattern INDEX = Pattern.compile("^[+-]?\\d+");
    private static final Map<String, String> FACE_NAMES = load();

    private OriginalNpcNameWindow() {
    }

    public interface Access {
        boolean enabled();
        void setEnabled(boolean enabled);
        String actorName(int actorId);
    }

    public static final class State {
        private String pendingName = "";
        private int pendingIndex;
        private String displayName = "";
        private boolean visible;

        public void comment(String groupedText, Access access) {
            Objects.requireNonNull(access);
            if (groupedText == null) throw new IllegalArgumentException("Missing original comment");
            Matcher match = COMMENT.matcher(groupedText);
            if (!match.matches()) return;
            String command = match.group(1);
            if (!command.equals("NW名前指定") && !command.equals("NWインデックス") && !command.equals("ネームウィンドウ")) return;
            if (command.equals("NW名前指定") && match.group(2).isEmpty()) {
                throw new UnsupportedOperationException("missing_name_window_parameter");
            }
            String parameter = strip(match.group(2));
            if (parameter.contains("\\") || parameter.toLowerCase(java.util.Locale.ROOT).contains("<s>")
                    || parameter.contains(",") || parameter.startsWith("[")) {
                throw new UnsupportedOperationException("unreviewed_name_window_expression");
            }
            if (parameter.length() > 512) throw new UnsupportedOperationException("name_window_parameter_length");
            switch (command) {
                case "NW名前指定" -> pendingName = parameter;
                case "NWインデックス" -> {
                    Matcher integer = INDEX.matcher(parameter);
                    try {
                        pendingIndex = integer.find() ? Integer.parseInt(integer.group()) : 0;
                    } catch (NumberFormatException exception) {
                        throw new UnsupportedOperationException("name_window_index_range", exception);
                    }
                }
                case "ネームウィンドウ" -> access.setEnabled(parameter.equals("on"));
                default -> throw new IllegalStateException(command);
            }
        }

        public void beginMessage(String faceName, int faceIndex, Access access) {
            Objects.requireNonNull(access);
            String resolved = "";
            boolean consume = false;
            if (access.enabled()) {
                if (!pendingName.isEmpty()) {
                    resolved = pendingName;
                    consume = true;
                } else if (faceName != null && !faceName.isEmpty()) {
                    resolved = FACE_NAMES.getOrDefault(faceName + "_" + faceIndex, FACE_NAMES.getOrDefault(faceName, ""));
                    Matcher actor = ACTOR.matcher(resolved);
                    if (actor.find()) {
                        try {
                            resolved = access.actorName(Integer.parseInt(actor.group(1)));
                        } catch (NumberFormatException exception) {
                            throw new UnsupportedOperationException("name_window_actor_range", exception);
                        }
                        if (resolved == null) throw new UnsupportedOperationException("unmapped_name_window_actor");
                    }
                    consume = true;
                }
            }
            if (resolved.length() > 512) throw new UnsupportedOperationException("name_window_display_length");
            displayName = resolved;
            visible = !resolved.isEmpty();
            if (consume) {
                pendingName = "";
                pendingIndex = 0;
            }
        }

        public String displayName() {
            return displayName;
        }

        public boolean visible() {
            return visible;
        }

        public void closeMessage() {
            displayName = "";
            visible = false;
        }

        public JsonObject snapshot() {
            JsonObject saved = new JsonObject();
            saved.addProperty("version", 1);
            saved.addProperty("pendingName", pendingName);
            saved.addProperty("pendingIndex", pendingIndex);
            saved.addProperty("displayName", displayName);
            saved.addProperty("visible", visible);
            return saved;
        }

        public static State restore(JsonObject saved) {
            Objects.requireNonNull(saved);
            if (!saved.has("version") || !saved.get("version").isJsonPrimitive()
                    || !saved.getAsJsonPrimitive("version").isNumber()
                    || saved.get("version").getAsBigDecimal().intValueExact() != 1) {
                throw new IllegalArgumentException("Unsupported original name window snapshot");
            }
            State state = new State();
            state.pendingName = string(saved, "pendingName");
            state.displayName = string(saved, "displayName");
            if (!saved.has("pendingIndex") || !saved.get("pendingIndex").isJsonPrimitive()
                    || !saved.getAsJsonPrimitive("pendingIndex").isNumber()
                    || !saved.has("visible") || !saved.get("visible").isJsonPrimitive()
                    || !saved.getAsJsonPrimitive("visible").isBoolean()) {
                throw new IllegalArgumentException("Invalid original name window snapshot");
            }
            state.pendingIndex = saved.get("pendingIndex").getAsBigDecimal().intValueExact();
            state.visible = saved.get("visible").getAsBoolean();
            if (state.visible == state.displayName.isEmpty()) throw new IllegalArgumentException("Inconsistent name window display");
            return state;
        }

        private static String string(JsonObject saved, String key) {
            if (!saved.has(key) || !saved.get(key).isJsonPrimitive() || !saved.getAsJsonPrimitive(key).isString()) {
                throw new IllegalArgumentException("Invalid original name window field: " + key);
            }
            String value = saved.get(key).getAsString();
            if (value.length() > 512) throw new IllegalArgumentException("Original name window field too long: " + key);
            return value;
        }
    }

    private static Map<String, String> load() {
        String path = "/data/blacksouls/original_npc_name_window.json";
        try (InputStream input = OriginalNpcNameWindow.class.getResourceAsStream(path)) {
            if (input == null) throw new IllegalStateException("Missing original name window data");
            JsonObject json = JsonParser.parseReader(new InputStreamReader(input, StandardCharsets.UTF_8)).getAsJsonObject();
            Map<String, String> names = new LinkedHashMap<>();
            for (Map.Entry<String, JsonElement> entry : json.entrySet()) {
                if (!entry.getValue().isJsonPrimitive() || !entry.getValue().getAsJsonPrimitive().isString()) {
                    throw new IllegalArgumentException("Unreviewed original name window value: " + entry.getKey());
                }
                names.put(entry.getKey(), entry.getValue().getAsString());
            }
            if (names.size() != 94) throw new IllegalArgumentException("Incomplete original name window data");
            return Map.copyOf(names);
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to load original name window data", exception);
        }
    }

    private static String strip(String value) {
        int start = 0;
        int end = value.length();
        while (start < end && stripCharacter(value.charAt(start))) start++;
        while (end > start && stripCharacter(value.charAt(end - 1))) end--;
        return value.substring(start, end);
    }

    private static boolean stripCharacter(char value) {
        return value == 0 || value == ' ' || value == '\t' || value == '\r' || value == '\n' || value == '\u000b' || value == '\f';
    }
}
