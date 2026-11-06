package yuki135.blacksouls.network.packets;

import yuki135.blacksouls.story.OriginalNpcPictureState;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

public record ClientboundOriginalNpcPicturesPacket(ResourceLocation dimension, long revision,
                                                   boolean paused, boolean visible, List<JsonObject> pictures) {
    public ClientboundOriginalNpcPicturesPacket {
        Objects.requireNonNull(dimension);
        if (revision < 0 || pictures.size() > 100) throw new IllegalArgumentException("Invalid original picture snapshot header");
        var numbers = new HashSet<Integer>();
        var copies = new ArrayList<JsonObject>();
        for (JsonObject picture : pictures) {
            JsonObject copy = picture.deepCopy();
            if (!numbers.add(OriginalNpcPictureState.restore(copy).number())) {
                throw new IllegalArgumentException("Duplicate original picture number");
            }
            copies.add(copy);
        }
        pictures = List.copyOf(copies);
    }

    public ClientboundOriginalNpcPicturesPacket(FriendlyByteBuf buffer) {
        this(buffer.readResourceLocation(), buffer.readVarLong(), buffer.readBoolean(), buffer.readBoolean(), readPictures(buffer));
    }

    public void toBytes(FriendlyByteBuf buffer) {
        buffer.writeResourceLocation(dimension);
        buffer.writeVarLong(revision);
        buffer.writeBoolean(paused);
        buffer.writeBoolean(visible);
        buffer.writeVarInt(pictures.size());
        for (JsonObject picture : pictures) buffer.writeUtf(picture.toString(), 4096);
    }

    public void handle(Supplier<NetworkEvent.Context> context) {
        PacketHandlers.handleClient(context, () -> ClientHandler.apply(this));
    }

    private static List<JsonObject> readPictures(FriendlyByteBuf buffer) {
        int count = buffer.readVarInt();
        if (count < 0 || count > 100) throw new IllegalArgumentException("Invalid original picture count");
        List<JsonObject> result = new ArrayList<>();
        for (int index = 0; index < count; index++) {
            result.add(JsonParser.parseString(buffer.readUtf(4096)).getAsJsonObject());
        }
        return result;
    }

    private static final class ClientHandler {
        private static void apply(ClientboundOriginalNpcPicturesPacket packet) {
            yuki135.blacksouls.client.OriginalNpcPictureController.apply(packet.dimension(), packet.revision(),
                    packet.paused(), packet.visible(), packet.pictures());
        }
    }
}
