package com.BlackSouls.BlackSoulsMod.combat;

import net.minecraft.network.FriendlyByteBuf;

import java.util.List;
import java.util.UUID;

public record OriginalTroopPresentation(UUID token, long revision, int kind, String speaker,
                                        List<String> choices, int minimumTextTicks, int automaticTicks,
                                        List<Animation> animations) {
    public OriginalTroopPresentation {
        choices = List.copyOf(choices);
        animations = List.copyOf(animations);
    }

    public OriginalTroopPresentation(UUID token, long revision, int kind, String speaker,
                                      List<String> choices, int automaticTicks, List<Animation> animations) {
        this(token, revision, kind, speaker, choices, Math.max(0, automaticTicks), automaticTicks, animations);
    }

    public boolean animationsOnly() {
        return kind == 5;
    }

    public void write(FriendlyByteBuf buffer) {
        buffer.writeUUID(token);
        buffer.writeVarLong(revision);
        buffer.writeVarInt(kind);
        buffer.writeUtf(speaker);
        buffer.writeCollection(choices, FriendlyByteBuf::writeUtf);
        buffer.writeVarInt(minimumTextTicks);
        buffer.writeVarInt(automaticTicks);
        buffer.writeCollection(animations, (output, animation) -> {
            output.writeVarInt(animation.originalSlot());
            output.writeVarInt(animation.animationId());
        });
    }

    public static OriginalTroopPresentation read(FriendlyByteBuf buffer) {
        return new OriginalTroopPresentation(buffer.readUUID(), buffer.readVarLong(), buffer.readVarInt(),
                buffer.readUtf(), buffer.readList(FriendlyByteBuf::readUtf), buffer.readVarInt(), buffer.readVarInt(),
                buffer.readList(input -> new Animation(input.readVarInt(), input.readVarInt())));
    }

    public static int textWaitTicks(String text) {
        int frames = 0;
        for (int index = 0; index + 1 < text.length(); index++) {
            if (text.charAt(index) == '\\') {
                char code = text.charAt(++index);
                if (code == '.') frames += 15;
                if (code == '|') frames += 60;
            }
        }
        return (frames + 2) / 3;
    }

    public static String visibleText(String text) {
        return text.replace("\\.", "").replace("\\|", "").replace("\\^", "");
    }

    public record Animation(int originalSlot, int animationId) {
    }
}
