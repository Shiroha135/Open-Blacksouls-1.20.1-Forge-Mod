package com.BlackSouls.BlackSoulsMod.capability;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.BlackSouls.BlackSoulsMod.compat.scene.SceneSpawnerBossData;
import com.BlackSouls.BlackSoulsMod.util.PlayerStatService;
import com.BlackSouls.BlackSoulsMod.story.OriginalBattleMusic;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.nio.charset.StandardCharsets;

public final class OriginalNpcProgress extends SavedData {
    private final Set<Integer> switches = new HashSet<>();
    private final Map<Integer, Long> variables = new HashMap<>();
    private final Set<String> selfSwitches = new HashSet<>();
    private final Set<String> hiddenEvents = new HashSet<>();
    private final Map<String, byte[]> conversations = new HashMap<>();
    private final Map<String, Boolean> timedWaits = new HashMap<>();
    private final Set<UUID> cheshireLegacyMigrated = new HashSet<>();
    private Boolean nameWindowEnabled;
    private final Map<UUID, byte[]> nameWindowStates = new HashMap<>();
    private OriginalBattleMusic battleMusic = OriginalBattleMusic.UNSET;

    public boolean nameWindowEnabled() {
        return Boolean.TRUE.equals(nameWindowEnabled);
    }

    public void setNameWindowEnabled(boolean enabled) {
        if (nameWindowEnabled == null || nameWindowEnabled != enabled) {
            nameWindowEnabled = enabled;
            setDirty();
        }
    }

    public void bootstrapNameWindow() {
        if (nameWindowEnabled == null) setNameWindowEnabled(true);
    }

    public JsonObject nameWindowState(UUID player) {
        byte[] value = nameWindowStates.get(player);
        return value == null ? null : JsonParser.parseString(new String(value, StandardCharsets.UTF_8)).getAsJsonObject();
    }

    public void setNameWindowState(UUID player, JsonObject value) {
        nameWindowStates.put(player, value.toString().getBytes(StandardCharsets.UTF_8));
        setDirty();
    }

    public OriginalBattleMusic battleMusic() {
        return battleMusic;
    }

    public void setBattleMusic(OriginalBattleMusic value) {
        if (!battleMusic.equals(value)) {
            battleMusic = java.util.Objects.requireNonNull(value);
            setDirty();
        }
    }

    public boolean switchValue(int id) {
        return switches.contains(id);
    }

    public void setSwitch(int id, boolean enabled) {
        if (id > 0 && (enabled ? switches.add(id) : switches.remove(id))) {
            setDirty();
        }
    }

    public long variable(int id) {
        return variables.getOrDefault(id, 0L);
    }

    public void setVariable(int id, long value) {
        if (id > 0 && variable(id) != value) {
            variables.put(id, value);
            setDirty();
        }
    }

    public static long readVariable(ServerPlayer player, int id) {
        if (id == 11) {
            return SceneSpawnerBossData.get(player.server).getStoryProgress();
        }
        if (id == 38) {
            return player.getCapability(BSPlayerStats.CAPABILITY)
                    .orElseThrow(() -> new IllegalStateException("Missing original player stats")).sen;
        }
        return get(player.server).variable(id);
    }

    public static void writeVariable(ServerPlayer player, int id, long value) {
        if (id == 38) {
            BSPlayerStats stats = player.getCapability(BSPlayerStats.CAPABILITY)
                    .orElseThrow(() -> new IllegalStateException("Missing original player stats"));
            long target = Math.max(Integer.MIN_VALUE, Math.min(Integer.MAX_VALUE, value));
            PlayerStatService.addSen(player, (int) Math.max(Integer.MIN_VALUE,
                    Math.min(Integer.MAX_VALUE, target - stats.sen)));
        } else if (id == 11) {
            SceneSpawnerBossData.get(player.server).setStoryProgress((int) Math.max(0L,
                    Math.min(Integer.MAX_VALUE, value)));
        }
        get(player.server).setVariable(id, value);
    }

    public boolean selfSwitch(String eventId, String key) {
        return selfSwitches.contains(eventId + ":" + key);
    }

    public void setSelfSwitch(String eventId, String key, boolean enabled) {
        if (key.matches("[A-D]") && (enabled ? selfSwitches.add(eventId + ":" + key)
                : selfSwitches.remove(eventId + ":" + key))) {
            setDirty();
        }
    }

    public boolean isHidden(String eventId) {
        return hiddenEvents.contains(eventId);
    }

    public void setHidden(String eventId, boolean hidden) {
        if (hidden ? hiddenEvents.add(eventId) : hiddenEvents.remove(eventId)) {
            setDirty();
        }
    }

    public JsonObject conversation(UUID playerId, String eventId) {
        byte[] value = conversations.get(playerId + ":" + eventId);
        return value == null ? null : JsonParser.parseString(new String(value, StandardCharsets.UTF_8)).getAsJsonObject();
    }

    public Set<String> conversationIds(UUID playerId) {
        String prefix = playerId + ":";
        Set<String> result = new HashSet<>();
        for (String key : conversations.keySet()) {
            if (key.startsWith(prefix)) {
                result.add(key.substring(prefix.length()));
            }
        }
        return Set.copyOf(result);
    }

    public void setConversation(UUID playerId, String eventId, JsonObject snapshot) {
        String key = playerId + ":" + eventId;
        if (snapshot == null) {
            timedWaits.remove(key);
            if (conversations.remove(key) != null) {
                setDirty();
            }
        } else {
            conversations.put(key, snapshot.toString().getBytes(StandardCharsets.UTF_8));
            timedWaits.put(key, timedWait(snapshot));
            setDirty();
        }
    }

    public boolean hasOtherConversation(UUID playerId, Set<String> eventIds) {
        String ownPrefix = playerId + ":";
        for (String key : conversations.keySet()) {
            if (!key.startsWith(ownPrefix) && eventIds.stream().anyMatch(id -> key.endsWith(":" + id))) return true;
        }
        return false;
    }

    public boolean hasMigratedCheshireLegacy(UUID playerId) {
        return cheshireLegacyMigrated.contains(playerId);
    }

    public void markCheshireLegacyMigrated(UUID playerId) {
        if (cheshireLegacyMigrated.add(playerId)) setDirty();
    }

    public boolean hasSavedTimedWait(UUID playerId) {
        String prefix = playerId + ":";
        for (String key : conversations.keySet()) {
            if (key.startsWith(prefix) && timedWaits.computeIfAbsent(key, value -> {
                try {
                    return timedWait(JsonParser.parseString(new String(conversations.get(value),
                            StandardCharsets.UTF_8)).getAsJsonObject());
                } catch (RuntimeException exception) {
                    return false;
                }
            })) return true;
        }
        return false;
    }

    private static boolean timedWait(JsonObject snapshot) {
        var interpreter = snapshot.get("interpreter");
        if (interpreter == null || !interpreter.isJsonObject()) return false;
        var pending = interpreter.getAsJsonObject().get("pending");
        return pending != null && pending.isJsonObject() && pending.getAsJsonObject().has("type")
                && "WAIT".equals(pending.getAsJsonObject().get("type").getAsString());
    }

    public static OriginalNpcProgress get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(OriginalNpcProgress::load,
                OriginalNpcProgress::new, "blacksouls_original_npc_progress");
    }

    public static OriginalNpcProgress load(CompoundTag tag) {
        OriginalNpcProgress data = new OriginalNpcProgress();
        if (tag.contains("NameWindowEnabled", Tag.TAG_BYTE)) data.nameWindowEnabled = tag.getBoolean("NameWindowEnabled");
        CompoundTag names = tag.getCompound("NameWindowStates");
        for (String player : names.getAllKeys()) {
            if (names.contains(player, Tag.TAG_BYTE_ARRAY)) data.nameWindowStates.put(UUID.fromString(player), names.getByteArray(player));
        }
        for (int id : tag.getIntArray("Switches")) {
            data.switches.add(id);
        }
        CompoundTag variables = tag.getCompound("Variables");
        for (String id : variables.getAllKeys()) {
            try {
                data.variables.put(Integer.parseInt(id), variables.getLong(id));
            } catch (NumberFormatException ignored) {
            }
        }
        for (Tag entry : tag.getList("SelfSwitches", Tag.TAG_STRING)) {
            data.selfSwitches.add(entry.getAsString());
        }
        for (Tag entry : tag.getList("HiddenEvents", Tag.TAG_STRING)) {
            data.hiddenEvents.add(entry.getAsString());
        }
        for (Tag entry : tag.getList("CheshireLegacyMigrated", Tag.TAG_STRING)) {
            data.cheshireLegacyMigrated.add(UUID.fromString(entry.getAsString()));
        }
        CompoundTag conversations = tag.getCompound("Conversations");
        for (String key : conversations.getAllKeys()) {
            if (conversations.contains(key, Tag.TAG_BYTE_ARRAY)) {
                data.conversations.put(key, conversations.getByteArray(key));
            }
        }
        if (tag.contains("BattleMusic", Tag.TAG_COMPOUND)) {
            data.battleMusic = OriginalBattleMusic.load(tag.getCompound("BattleMusic"));
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        if (nameWindowEnabled != null) tag.putBoolean("NameWindowEnabled", nameWindowEnabled);
        CompoundTag names = new CompoundTag();
        nameWindowStates.forEach((player, value) -> names.putByteArray(player.toString(), value));
        tag.put("NameWindowStates", names);
        tag.putIntArray("Switches", switches.stream().sorted().mapToInt(Integer::intValue).toArray());
        CompoundTag variableTag = new CompoundTag();
        variables.forEach((id, value) -> variableTag.putLong(id.toString(), value));
        tag.put("Variables", variableTag);
        ListTag self = new ListTag();
        selfSwitches.stream().sorted().map(StringTag::valueOf).forEach(self::add);
        tag.put("SelfSwitches", self);
        ListTag hidden = new ListTag();
        hiddenEvents.stream().sorted().map(StringTag::valueOf).forEach(hidden::add);
        tag.put("HiddenEvents", hidden);
        ListTag migrated = new ListTag();
        cheshireLegacyMigrated.stream().map(UUID::toString).sorted().map(StringTag::valueOf).forEach(migrated::add);
        tag.put("CheshireLegacyMigrated", migrated);
        CompoundTag conversationsTag = new CompoundTag();
        conversations.forEach(conversationsTag::putByteArray);
        tag.put("Conversations", conversationsTag);
        if (battleMusic.configured()) {
            tag.put("BattleMusic", battleMusic.save());
        } else {
            tag.remove("BattleMusic");
        }
        return tag;
    }
}
