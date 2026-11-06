package yuki135.blacksouls.story;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

import java.util.Objects;

public record OriginalBattleMusic(boolean configured, String sound, double volume, double pitch) {
    public static final OriginalBattleMusic UNSET = new OriginalBattleMusic(false, "", 100.0D, 100.0D);

    public OriginalBattleMusic {
        sound = Objects.requireNonNull(sound);
        if (!Double.isFinite(volume) || volume < 0.0D || volume > 100.0D
                || !Double.isFinite(pitch) || pitch < 50.0D || pitch > 150.0D
                || !sound.isEmpty() && ResourceLocation.tryParse(sound) == null
                || !configured && (!sound.isEmpty() || volume != 100.0D || pitch != 100.0D)) {
            throw new IllegalArgumentException("Invalid original battle music setting");
        }
    }

    public boolean silent() {
        return configured && sound.isEmpty();
    }

    public void write(FriendlyByteBuf buffer) {
        buffer.writeBoolean(configured);
        if (configured) {
            buffer.writeUtf(sound, 512);
            buffer.writeDouble(volume);
            buffer.writeDouble(pitch);
        }
    }

    public static OriginalBattleMusic read(FriendlyByteBuf buffer) {
        return buffer.readBoolean()
                ? new OriginalBattleMusic(true, buffer.readUtf(512), buffer.readDouble(), buffer.readDouble()) : UNSET;
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putBoolean("Configured", configured);
        if (configured) {
            tag.putString("Sound", sound);
            tag.putDouble("Volume", volume);
            tag.putDouble("Pitch", pitch);
        }
        return tag;
    }

    public static OriginalBattleMusic load(CompoundTag tag) {
        return tag.getBoolean("Configured")
                ? new OriginalBattleMusic(true, tag.getString("Sound"), tag.getDouble("Volume"), tag.getDouble("Pitch")) : UNSET;
    }
}
