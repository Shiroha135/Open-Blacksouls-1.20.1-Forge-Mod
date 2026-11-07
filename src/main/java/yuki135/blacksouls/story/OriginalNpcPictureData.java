package yuki135.blacksouls.story;

import com.google.gson.JsonParser;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.level.saveddata.SavedData;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;

public final class OriginalNpcPictureData extends SavedData {
    final Map<UUID, Screen> screens = new LinkedHashMap<>();
    int lastServerTick = Integer.MIN_VALUE;

    public static OriginalNpcPictureData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(OriginalNpcPictureData::load,
                OriginalNpcPictureData::new, "blacksouls_original_npc_pictures");
    }

    public static OriginalNpcPictureData load(CompoundTag saved) {
        OriginalNpcPictureData data = new OriginalNpcPictureData();
        for (Tag element : saved.getList("Screens", Tag.TAG_COMPOUND)) {
            CompoundTag value = (CompoundTag) element;
            UUID owner = value.getUUID("Owner");
            Screen screen = new Screen();
            screen.revision = value.getLong("Revision");
            if (screen.revision < 0L) throw new IllegalArgumentException("Invalid original picture revision");
            screen.paused = value.getBoolean("Paused");
            screen.visible = !value.contains("Visible") || value.getBoolean("Visible");
            for (Tag pictureElement : value.getList("Pictures", Tag.TAG_COMPOUND)) {
                CompoundTag picture = (CompoundTag) pictureElement;
                OriginalNpcPictureState state = OriginalNpcPictureState.restore(JsonParser.parseString(new String(
                        picture.getByteArray("State"), StandardCharsets.UTF_8)).getAsJsonObject());
                if (screen.pictures.put(state.number(), state) != null) {
                    throw new IllegalArgumentException("Duplicate original picture number");
                }
                if (picture.contains("SourceNpc")) {
                    if (!picture.contains("SourceNpc", Tag.TAG_STRING)) throw new IllegalArgumentException("Invalid original picture source type");
                    String source = picture.getString("SourceNpc");
                    if (!source.matches("map[0-9]{3}_ev[0-9]{3}")) throw new IllegalArgumentException("Invalid original picture source");
                    screen.sources.put(state.number(), source);
                }
            }
            if (data.screens.put(owner, screen) != null) throw new IllegalArgumentException("Duplicate original picture owner");
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag saved) {
        ListTag screens = new ListTag();
        for (Map.Entry<UUID, Screen> entry : this.screens.entrySet()) {
            Screen screen = entry.getValue();
            CompoundTag value = new CompoundTag();
            value.putUUID("Owner", entry.getKey());
            value.putLong("Revision", screen.revision);
            value.putBoolean("Paused", screen.paused);
            value.putBoolean("Visible", screen.visible);
            ListTag pictures = new ListTag();
            for (OriginalNpcPictureState state : screen.pictures.values()) {
                CompoundTag picture = new CompoundTag();
                picture.putByteArray("State", state.snapshot().toString().getBytes(StandardCharsets.UTF_8));
                String source = screen.sources.get(state.number());
                if (source != null) picture.putString("SourceNpc", source);
                pictures.add(picture);
            }
            value.put("Pictures", pictures);
            screens.add(value);
        }
        saved.put("Screens", screens);
        return saved;
    }

    static final class Screen {
        final Map<Integer, OriginalNpcPictureState> pictures = new TreeMap<>();
        final Map<Integer, String> sources = new TreeMap<>();
        long revision;
        boolean paused;
        boolean visible = true;
        ServerGamePacketListenerImpl connection;
        ResourceLocation dimension;
    }
}
