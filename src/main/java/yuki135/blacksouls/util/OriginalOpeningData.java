package yuki135.blacksouls.util;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.math.BigDecimal;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

public final class OriginalOpeningData {
    private static final String RESOURCE = "/data/blacksouls/original_opening_professions.json";
    private static final List<Integer> SLOT_TYPES = List.of(0, 1, 2, 3, 4, 4, 4, 4);
    private static final List<Integer> TABLE_HEADER = List.of(2, 8, 100, 1, 800);
    private static final Catalog CATALOG = load();

    private OriginalOpeningData() {
    }

    public static Catalog catalog() {
        return CATALOG;
    }

    public static Profession get(int professionId) {
        return CATALOG.get(professionId);
    }

    public static Profession forActor(int actorId) {
        return CATALOG.forActor(actorId);
    }

    public static Catalog parse(InputStream input) {
        if (input == null) throw invalid("Missing catalog input");
        return parse(new InputStreamReader(input, StandardCharsets.UTF_8.newDecoder()
                .onMalformedInput(CodingErrorAction.REPORT).onUnmappableCharacter(CodingErrorAction.REPORT)));
    }

    public static Catalog parse(Reader input) {
        if (input == null) throw invalid("Missing catalog reader");
        try {
            JsonReader reader = new JsonReader(input);
            reader.setLenient(false);
            JsonElement value = readValue(reader, 0, new int[1]);
            if (reader.peek() != JsonToken.END_DOCUMENT) throw invalid("Trailing catalog content");
            return parseCatalog(object(value, "version", "sourceMapId", "professionVariableId", "actorVariableId",
                    "minimumLevel", "maximumLevel", "runtimeSlotTypes", "experience", "growthContext",
                    "sourceFiles", "professions", "equipment", "openingCoda"));
        } catch (IOException | IllegalStateException | ArithmeticException failure) {
            throw new IllegalArgumentException("Invalid original opening catalog", failure);
        }
    }

    private static Catalog load() {
        try (InputStream input = OriginalOpeningData.class.getResourceAsStream(RESOURCE)) {
            if (input == null) throw new IllegalStateException("Missing original opening professions");
            return parse(input);
        } catch (IOException | IllegalArgumentException failure) {
            throw new IllegalStateException("Failed to load original opening professions", failure);
        }
    }

    private static Catalog parseCatalog(JsonObject root) {
        exact(root, "version", 1);
        exact(root, "sourceMapId", 7);
        exact(root, "professionVariableId", 6);
        exact(root, "actorVariableId", 14);
        exact(root, "minimumLevel", 1);
        exact(root, "maximumLevel", 999);
        if (!integers(root.get("runtimeSlotTypes"), 8, 0, 4).equals(SLOT_TYPES)
                || !text(root, "growthContext", false).equals("base_parameters_without_states_spirit_or_additional_overlevel_modifiers")) {
            throw invalid("Unsupported opening growth or slot context");
        }
        JsonObject exp = object(root.get("experience"), "basis", "extra", "accA", "accB");
        Experience experience = new Experience(integer(exp, "basis", 1, 100000), integer(exp, "extra", 0, 100000),
                integer(exp, "accA", 1, 100000), integer(exp, "accB", 1, 100000));
        if (!experience.equals(new Experience(30, 0, 10, 10))) throw invalid("Unexpected shared EXP configuration");
        JsonObject source = object(root.get("sourceFiles"), "Map007.rvdata2", "Map110.rvdata2", "Map099.rvdata2",
                "Actors.rvdata2", "Classes.rvdata2", "Weapons.rvdata2", "Armors.rvdata2", "Items.rvdata2", "Skills.rvdata2", "Scripts.rvdata2");
        Map<String, String> sources = new LinkedHashMap<>();
        for (String key : source.keySet()) {
            String hash = text(source, key, false);
            if (!hash.matches("[0-9a-f]{64}")) throw invalid("Invalid source SHA256");
            sources.put(key, hash);
        }
        Map<String, Equipment> equipment = new LinkedHashMap<>();
        for (JsonElement raw : array(root.get("equipment"), 29)) {
            JsonObject row = object(raw, "kind", "id", "name", "description", "note", "parameters", "equipmentType", "typeId", "features");
            Equipment item = new Equipment(integer(row, "kind", 1, 2), integer(row, "id", 1, 65535), text(row, "name", false),
                    text(row, "description", true), text(row, "note", true), integers(row.get("parameters"), 8, -65535, 65535),
                    integer(row, "equipmentType", 0, 4), integer(row, "typeId", 0, 65535), features(row.get("features")));
            if (item.kind() == 1 && item.equipmentType() != 0 || item.kind() == 2 && item.equipmentType() == 0
                    || equipment.putIfAbsent(key(item.kind(), item.id()), item) != null) throw invalid("Invalid or duplicate initial equipment");
        }
        Map<Integer, Profession> professions = new LinkedHashMap<>();
        Set<Integer> roleSwitches = new HashSet<>();
        Set<String> usedEquipment = new HashSet<>();
        for (JsonElement raw : array(root.get("professions"), 9)) {
            Profession profession = profession(object(raw, "id", "name", "sourceLabel", "sourceDescription", "sourceEventId", "roleSwitch",
                    "actor", "professionClass", "initialEquipment", "extraRewards", "baseParametersByLevel",
                    "sourceInitialEffectiveParameters", "permanentInitialSkills", "featureGrantedInitialSkills"), equipment, usedEquipment);
            if (!roleSwitches.add(profession.roleSwitch()) || professions.putIfAbsent(profession.id(), profession) != null) {
                throw invalid("Duplicate opening profession");
            }
        }
        if (!professions.keySet().equals(Set.of(1, 2, 3, 4, 5, 6, 7, 8, 9)) || !usedEquipment.equals(equipment.keySet())) {
            throw invalid("Incomplete opening profession equipment scope");
        }
        OpeningCoda coda = coda(object(root.get("openingCoda"), "sharedItem", "ringRemoval"));
        return new Catalog(professions, equipment, sources, experience, coda);
    }

    private static Profession profession(JsonObject row, Map<String, Equipment> equipment, Set<String> usedEquipment) {
        int id = integer(row, "id", 1, 9);
        int eventId = integer(row, "sourceEventId", 3, 11);
        if (eventId != id + 2) throw invalid("Source profession event mismatch");
        JsonObject a = object(row.get("actor"), "id", "classId", "initialLevel", "sourceMaximumLevel", "name", "nickname",
                "characterName", "characterIndex", "faceName", "faceIndex", "note", "features", "initialEquipIds");
        Actor actor = new Actor(integer(a, "id", 2, 10), integer(a, "classId", 1, 9), integer(a, "initialLevel", 1, 1),
                integer(a, "sourceMaximumLevel", 99, 99), text(a, "name", true), text(a, "nickname", true),
                text(a, "characterName", true), integer(a, "characterIndex", 0, 7), text(a, "faceName", true),
                integer(a, "faceIndex", 0, 7), text(a, "note", true), features(a.get("features")), integers(a.get("initialEquipIds"), 5, 0, 65535));
        if (actor.id() != id + 1 || actor.classId() != id) throw invalid("Source profession actor mapping mismatch");
        JsonObject c = object(row.get("professionClass"), "id", "name", "note", "features", "learnings", "parameters");
        JsonObject table = object(c.get("parameters"), "header", "valuesByLevel");
        List<Integer> header = integers(table.get("header"), 5, 0, 10000);
        List<List<Integer>> original = table(table.get("valuesByLevel"), 100, 0, 65535);
        if (!header.equals(TABLE_HEADER) || !original.get(0).equals(List.of(0, 0, 0, 0, 0, 0, 0, 0))) throw invalid("Invalid source parameter Table dimensions");
        List<Learning> learnings = new ArrayList<>();
        Set<Integer> learningIds = new HashSet<>();
        for (JsonElement raw : array(c.get("learnings"), -1)) {
            JsonObject learning = object(raw, "sourceLevel", "level", "skillId", "note");
            Learning item = new Learning(integer(learning, "sourceLevel", 1, 99), integer(learning, "level", 1, 999),
                    integer(learning, "skillId", 1, 65535), text(learning, "note", true));
            if (!learningIds.add(item.skillId())) throw invalid("Duplicate source class learning");
            learnings.add(item);
        }
        ProfessionClass professionClass = new ProfessionClass(integer(c, "id", 1, 9), text(c, "name", false), text(c, "note", true),
                features(c.get("features")), learnings, new ParameterTable(header, original));
        if (professionClass.id() != id || learnings.isEmpty()) throw invalid("Source profession class mismatch");
        List<List<Integer>> extended = table(row.get("baseParametersByLevel"), 999, 0, 10000000);
        for (int level = 1; level <= 999; level++) {
            for (int parameter = 0; parameter < 8; parameter++) {
                int expected = level <= 99 ? original.get(level).get(parameter)
                        : original.get(99).get(parameter) + (int) ((original.get(99).get(parameter) - original.get(98).get(parameter)) * (level - 99) * 1.05D);
                if (extended.get(level - 1).get(parameter) != expected) throw invalid("Original parameter table or exact overlevel result mismatch");
            }
        }
        List<EquipmentSlot> slots = new ArrayList<>();
        List<Equipment> equipped = new ArrayList<>();
        int slotIndex = 0;
        for (JsonElement raw : array(row.get("initialEquipment"), 8)) {
            JsonObject slot = object(raw, "slot", "equipmentType", "kind", "id");
            EquipmentSlot value = new EquipmentSlot(integer(slot, "slot", 0, 7), integer(slot, "equipmentType", 0, 4),
                    integer(slot, "kind", 0, 2), integer(slot, "id", 0, 65535));
            int originalId = slotIndex < 5 ? actor.initialEquipIds().get(slotIndex) : 0;
            if (value.slot() != slotIndex || value.equipmentType() != SLOT_TYPES.get(slotIndex) || value.id() != originalId
                    || value.id() == 0 && value.kind() != 0 || value.id() > 0 && value.kind() != (slotIndex == 0 ? 1 : 2)) throw invalid("Invalid original initial equipment slot");
            if (value.id() > 0) {
                Equipment item = equipment.get(key(value.kind(), value.id()));
                if (item == null || item.equipmentType() != value.equipmentType()) throw invalid("Unresolved original initial equipment");
                equipped.add(item);
                usedEquipment.add(key(value.kind(), value.id()));
            }
            slots.add(value);
            slotIndex++;
        }
        List<Integer> permanent = sortedIds(row.get("permanentInitialSkills"));
        List<Integer> granted = sortedIds(row.get("featureGrantedInitialSkills"));
        TreeSet<Integer> expectedPermanent = new TreeSet<>();
        for (Learning item : learnings) if (item.level() <= actor.initialLevel()) expectedPermanent.add(item.skillId());
        List<Feature> all = new ArrayList<>(actor.features());
        all.addAll(professionClass.features());
        for (Equipment item : equipped) all.addAll(item.features());
        TreeSet<Integer> expectedGranted = new TreeSet<>();
        for (Feature feature : all) if (feature.code() == 43) expectedGranted.add(feature.dataId());
        if (!permanent.equals(List.copyOf(expectedPermanent)) || !granted.equals(List.copyOf(expectedGranted))) throw invalid("Permanent and feature skill scopes differ from source definitions");
        List<Integer> effective = integers(row.get("sourceInitialEffectiveParameters"), 8, 0, 10000000);
        for (int parameter = 0; parameter < 8; parameter++) {
            int value = original.get(actor.initialLevel()).get(parameter);
            for (Equipment item : equipped) value += item.parameters().get(parameter);
            double rate = 1.0D;
            for (Feature feature : all) if (feature.code() == 21 && feature.dataId() == parameter) rate *= feature.value();
            if (effective.get(parameter) != (int) (value * rate)) throw invalid("Initial source effective parameter mismatch");
        }
        List<ExtraReward> rewards = new ArrayList<>();
        Set<String> rewardIds = new HashSet<>();
        for (JsonElement raw : array(row.get("extraRewards"), -1)) {
            JsonObject reward = object(raw, "kind", "id", "name", "amount", "source");
            String kind = text(reward, "kind", false);
            RewardKind type = switch (kind) { case "souls" -> RewardKind.SOULS; case "item" -> RewardKind.ITEM; default -> throw invalid("Invalid source reward kind"); };
            int itemId = integer(reward, "id", 0, 65535);
            SourceCommand command = command(reward.get("source"));
            if (type == RewardKind.SOULS && (itemId != 0 || command.opcode() != 125)
                    || type == RewardKind.ITEM && (itemId < 1 || command.opcode() != 126)
                    || command.mapId() != 7 || command.eventId() != eventId || command.page() != 1
                    || !rewardIds.add(kind + ":" + itemId)) throw invalid("Invalid original profession reward source");
            rewards.add(new ExtraReward(type, itemId, text(reward, "name", false), integer(reward, "amount", 1, 1000000000), command));
        }
        List<String> description = strings(row.get("sourceDescription"));
        String label = text(row, "sourceLabel", false);
        if (description.isEmpty() || !description.get(0).equals(label)) throw invalid("Source profession label mismatch");
        return new Profession(id, text(row, "name", false), label, description, eventId, integer(row, "roleSwitch", 1, 65535),
                actor, professionClass, slots, rewards, extended, effective, permanent, granted);
    }

    private static OpeningCoda coda(JsonObject row) {
        JsonObject grant = object(row.get("sharedItem"), "kind", "id", "name", "amount", "source", "scope");
        SourceCommand source = command(grant.get("source"));
        if (integer(grant, "kind", 0, 2) != 0 || integer(grant, "id", 1, 65535) != 67 || integer(grant, "amount", 1, 1000000) != 1
                || !source.equals(new SourceCommand(7, 1, 3, 54, 126)) || !text(grant, "scope", false).equals("shared_op_after_birth")) throw invalid("OP coda must describe source Item67, never Weapon67");
        OpeningGrant shared = new OpeningGrant(0, 67, text(grant, "name", false), 1, source, "shared_op_after_birth");
        JsonObject removal = object(row.get("ringRemoval"), "kind", "id", "name", "amount", "includeEquipped", "sources", "scope");
        List<SourceCommand> sources = new ArrayList<>();
        for (JsonElement raw : array(removal.get("sources"), 2)) sources.add(command(raw));
        if (integer(removal, "kind", 0, 2) != 2 || integer(removal, "id", 1, 65535) != 131 || integer(removal, "amount", 1, 1000000) != 1
                || !bool(removal, "includeEquipped") || !sources.equals(List.of(new SourceCommand(99, 2, 1, 28, 128), new SourceCommand(110, 8, 1, 20, 128)))
                || !text(removal, "scope", false).equals("op_normal_or_skip_finale")) throw invalid("Invalid source OP ring removal");
        return new OpeningCoda(shared, new OpeningRemoval(2, 131, text(removal, "name", false), 1, true, sources, "op_normal_or_skip_finale"));
    }

    private static SourceCommand command(JsonElement value) {
        JsonObject row = object(value, "mapId", "eventId", "page", "commandIndex", "opcode");
        return new SourceCommand(integer(row, "mapId", 1, 9999), integer(row, "eventId", 1, 65535), integer(row, "page", 1, 99),
                integer(row, "commandIndex", 0, 100000), integer(row, "opcode", 1, 9999));
    }

    private static List<Feature> features(JsonElement value) {
        List<Feature> result = new ArrayList<>();
        for (JsonElement raw : array(value, -1)) {
            JsonObject row = object(raw, "code", "dataId", "value");
            BigDecimal number = number(row.get("value"));
            double amount = number.doubleValue();
            if (!Double.isFinite(amount) || Math.abs(amount) > 1000000000D || amount == 0D && number.signum() != 0) throw invalid("Nonfinite or unrepresentable source feature value");
            result.add(new Feature(integer(row, "code", 1, 99), integer(row, "dataId", 0, 65535), amount));
        }
        return List.copyOf(result);
    }

    private static JsonElement readValue(JsonReader reader, int depth, int[] count) throws IOException {
        if (depth > 32 || ++count[0] > 200000) throw invalid("Opening catalog structure is too large");
        return switch (reader.peek()) {
            case BEGIN_OBJECT -> {
                JsonObject object = new JsonObject();
                Set<String> names = new HashSet<>();
                reader.beginObject();
                while (reader.hasNext()) {
                    String key = reader.nextName();
                    if (!names.add(key)) throw invalid("Duplicate JSON key: " + key);
                    object.add(key, readValue(reader, depth + 1, count));
                }
                reader.endObject();
                yield object;
            }
            case BEGIN_ARRAY -> {
                JsonArray values = new JsonArray();
                reader.beginArray();
                while (reader.hasNext()) values.add(readValue(reader, depth + 1, count));
                reader.endArray();
                yield values;
            }
            case STRING -> new JsonPrimitive(validText(reader.nextString()));
            case NUMBER -> new JsonPrimitive(new BigDecimal(reader.nextString()));
            case BOOLEAN -> new JsonPrimitive(reader.nextBoolean());
            case NULL -> { reader.nextNull(); yield JsonNull.INSTANCE; }
            default -> throw invalid("Invalid JSON token");
        };
    }

    private static JsonObject object(JsonElement value, String... fields) {
        if (value == null || !value.isJsonObject()) throw invalid("Expected catalog object");
        JsonObject row = value.getAsJsonObject();
        if (!row.keySet().equals(Set.of(fields))) throw invalid("Unexpected or missing catalog fields");
        return row;
    }

    private static JsonArray array(JsonElement value, int length) {
        if (value == null || !value.isJsonArray()) throw invalid("Expected catalog array");
        JsonArray values = value.getAsJsonArray();
        if (length >= 0 && values.size() != length) throw invalid("Invalid catalog array dimension");
        return values;
    }

    private static BigDecimal number(JsonElement value) {
        if (value == null || !value.isJsonPrimitive() || !value.getAsJsonPrimitive().isNumber()) throw invalid("Expected numeric catalog value");
        return value.getAsBigDecimal();
    }

    private static int integer(JsonObject row, String key, int minimum, int maximum) {
        return integer(row.get(key), minimum, maximum);
    }

    private static int integer(JsonElement value, int minimum, int maximum) {
        int integer = number(value).intValueExact();
        if (integer < minimum || integer > maximum) throw invalid("Catalog integer outside source range");
        return integer;
    }

    private static void exact(JsonObject row, String key, int expected) {
        if (integer(row, key, expected, expected) != expected) throw invalid("Unexpected source catalog constant");
    }

    private static String text(JsonObject row, String key, boolean empty) {
        JsonElement value = row.get(key);
        if (value == null || !value.isJsonPrimitive() || !value.getAsJsonPrimitive().isString()) throw invalid("Expected source text");
        String text = validText(value.getAsString());
        if (!empty && text.isEmpty()) throw invalid("Empty source text");
        return text;
    }

    private static String validText(String value) {
        if (value.length() > 32768 || value.indexOf('\0') >= 0) throw invalid("Invalid source text length or NUL");
        for (int index = 0; index < value.length(); index++) {
            char part = value.charAt(index);
            if (Character.isHighSurrogate(part)) {
                if (++index >= value.length() || !Character.isLowSurrogate(value.charAt(index))) throw invalid("Unpaired source surrogate");
            } else if (Character.isLowSurrogate(part)) throw invalid("Unpaired source surrogate");
        }
        return value;
    }

    private static boolean bool(JsonObject row, String key) {
        JsonElement value = row.get(key);
        if (value == null || !value.isJsonPrimitive() || !value.getAsJsonPrimitive().isBoolean()) throw invalid("Expected source boolean");
        return value.getAsBoolean();
    }

    private static List<Integer> integers(JsonElement value, int length, int minimum, int maximum) {
        List<Integer> result = new ArrayList<>();
        for (JsonElement raw : array(value, length)) result.add(integer(raw, minimum, maximum));
        return List.copyOf(result);
    }

    private static List<Integer> sortedIds(JsonElement value) {
        List<Integer> ids = integers(value, -1, 1, 65535);
        if (!ids.equals(List.copyOf(new TreeSet<>(ids)))) throw invalid("Expected unique sorted source skill IDs");
        return ids;
    }

    private static List<String> strings(JsonElement value) {
        List<String> result = new ArrayList<>();
        for (JsonElement raw : array(value, -1)) {
            if (!raw.isJsonPrimitive() || !raw.getAsJsonPrimitive().isString()) throw invalid("Expected source text array");
            result.add(validText(raw.getAsString()));
        }
        return List.copyOf(result);
    }

    private static List<List<Integer>> table(JsonElement value, int rows, int minimum, int maximum) {
        List<List<Integer>> result = new ArrayList<>();
        for (JsonElement raw : array(value, rows)) result.add(integers(raw, 8, minimum, maximum));
        return List.copyOf(result);
    }

    private static List<List<Integer>> immutableTable(List<List<Integer>> values) {
        return values.stream().map(List::copyOf).toList();
    }

    private static String key(int kind, int id) {
        return kind + ":" + id;
    }

    private static IllegalArgumentException invalid(String message) {
        return new IllegalArgumentException(message);
    }

    public static final class Catalog {
        private final List<Profession> professions;
        private final Map<Integer, Profession> byId;
        private final Map<String, Equipment> equipment;
        private final Map<String, String> sourceFiles;
        private final Experience experience;
        private final OpeningCoda openingCoda;

        private Catalog(Map<Integer, Profession> professions, Map<String, Equipment> equipment, Map<String, String> sourceFiles, Experience experience, OpeningCoda openingCoda) {
            this.professions = professions.values().stream().sorted(java.util.Comparator.comparingInt(Profession::id)).toList();
            this.byId = Map.copyOf(professions);
            this.equipment = Map.copyOf(equipment);
            this.sourceFiles = Map.copyOf(sourceFiles);
            this.experience = experience;
            this.openingCoda = openingCoda;
        }

        public List<Profession> professions() { return professions; }
        public Profession get(int professionId) { return byId.get(professionId); }
        public Profession forActor(int actorId) { return actorId >= 2 && actorId <= 10 ? get(actorId - 1) : null; }
        public Equipment initialEquipment(int kind, int id) { return equipment.get(key(kind, id)); }
        public Map<String, Equipment> initialEquipmentCatalog() { return equipment; }
        public Map<String, String> sourceFiles() { return sourceFiles; }
        public List<Integer> runtimeSlotTypes() { return SLOT_TYPES; }
        public Experience experience() { return experience; }
        public OpeningCoda openingCoda() { return openingCoda; }
        public int professionVariableId() { return 6; }
        public int actorVariableId() { return 14; }
        public int minimumLevel() { return 1; }
        public int maximumLevel() { return 999; }
    }

    public record Profession(int id, String name, String sourceLabel, List<String> sourceDescription, int sourceEventId, int roleSwitch,
                             Actor actor, ProfessionClass professionClass, List<EquipmentSlot> initialEquipment, List<ExtraReward> extraRewards,
                             List<List<Integer>> baseParametersByLevel, List<Integer> sourceInitialEffectiveParameters,
                             List<Integer> permanentInitialSkills, List<Integer> featureGrantedInitialSkills) {
        public Profession {
            sourceDescription = List.copyOf(sourceDescription);
            initialEquipment = List.copyOf(initialEquipment);
            extraRewards = List.copyOf(extraRewards);
            baseParametersByLevel = immutableTable(baseParametersByLevel);
            sourceInitialEffectiveParameters = List.copyOf(sourceInitialEffectiveParameters);
            permanentInitialSkills = List.copyOf(permanentInitialSkills);
            featureGrantedInitialSkills = List.copyOf(featureGrantedInitialSkills);
        }
        public int actorId() { return actor.id(); }
        public int classId() { return professionClass.id(); }
        public int sourceVariable6Value() { return id; }
        public int sourceVariable14Value() { return actor.id(); }
        public List<Integer> baseParameters(int level) {
            if (level < 1 || level > 999 || level > baseParametersByLevel.size()) throw invalid("Unsupported source opening level");
            return baseParametersByLevel.get(level - 1);
        }
        public int baseParameter(int level, int parameter) {
            if (parameter < 0 || parameter > 7) throw invalid("Unsupported source parameter ID");
            return baseParameters(level).get(parameter);
        }
        public List<Integer> initialSkillIds() {
            TreeSet<Integer> ids = new TreeSet<>(permanentInitialSkills);
            ids.addAll(featureGrantedInitialSkills);
            return List.copyOf(ids);
        }
        public long extraSouls() { return extraRewards.stream().filter(reward -> reward.kind() == RewardKind.SOULS).mapToLong(ExtraReward::amount).sum(); }
    }

    public record Actor(int id, int classId, int initialLevel, int sourceMaximumLevel, String name, String nickname,
                        String characterName, int characterIndex, String faceName, int faceIndex, String note,
                        List<Feature> features, List<Integer> initialEquipIds) {
        public Actor { features = List.copyOf(features); initialEquipIds = List.copyOf(initialEquipIds); }
    }
    public record ProfessionClass(int id, String name, String note, List<Feature> features, List<Learning> learnings, ParameterTable parameters) {
        public ProfessionClass { features = List.copyOf(features); learnings = List.copyOf(learnings); }
    }
    public record ParameterTable(List<Integer> header, List<List<Integer>> valuesByLevel) {
        public ParameterTable { header = List.copyOf(header); valuesByLevel = immutableTable(valuesByLevel); }
    }
    public record Feature(int code, int dataId, double value) { }
    public record Learning(int sourceLevel, int level, int skillId, String note) { }
    public record EquipmentSlot(int slot, int equipmentType, int kind, int id) { }
    public record Equipment(int kind, int id, String name, String description, String note, List<Integer> parameters, int equipmentType, int typeId, List<Feature> features) {
        public Equipment { parameters = List.copyOf(parameters); features = List.copyOf(features); }
    }
    public enum RewardKind { SOULS, ITEM }
    public record ExtraReward(RewardKind kind, int id, String name, int amount, SourceCommand source) { }
    public record SourceCommand(int mapId, int eventId, int page, int commandIndex, int opcode) { }
    public record Experience(int basis, int extra, int accA, int accB) { }
    public record OpeningGrant(int kind, int id, String name, int amount, SourceCommand source, String scope) { }
    public record OpeningRemoval(int kind, int id, String name, int amount, boolean includeEquipped, List<SourceCommand> sources, String scope) {
        public OpeningRemoval { sources = List.copyOf(sources); }
    }
    public record OpeningCoda(OpeningGrant sharedItem, OpeningRemoval ringRemoval) { }
}
