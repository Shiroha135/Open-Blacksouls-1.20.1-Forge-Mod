package com.BlackSouls.BlackSoulsMod.network.packets;

import com.BlackSouls.BlackSoulsMod.capability.BSPlayerStats;
import com.BlackSouls.BlackSoulsMod.network.NetworkHandler;
import com.BlackSouls.BlackSoulsMod.util.LibraryDestination;
import com.BlackSouls.BlackSoulsMod.util.KnightStartingKit;
import com.BlackSouls.BlackSoulsMod.util.StoryNameData;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public final class ServerboundConfirmStoryNamePacket {
    private static final int MAX_NAME_BYTES = 64;
    private final String storyName;

    public ServerboundConfirmStoryNamePacket(String storyName) {
        this.storyName = storyName;
    }

    public ServerboundConfirmStoryNamePacket(FriendlyByteBuf buffer) {
        this.storyName = buffer.readUtf(MAX_NAME_BYTES);
    }

    public void toBytes(FriendlyByteBuf buffer) {
        buffer.writeUtf(storyName, MAX_NAME_BYTES);
    }

    public void handle(Supplier<NetworkEvent.Context> supplier) {
        PacketHandlers.handleServer(supplier, context -> {
            ServerPlayer player = context.getSender();
            if (player == null || !StoryNameData.hasStarted(player) || StoryNameData.isConfirmed(player)) {
                return;
            }
            String normalized = StoryNameData.normalize(storyName);
            if (normalized.isEmpty()) {
                NetworkHandler.sendToPlayer(new ClientboundStoryNamePacket(true, ""), player);
                return;
            }
            StoryNameData.confirm(player, normalized);
            KnightStartingKit.grant(player);
            NetworkHandler.sendToPlayer(new ClientboundStoryNamePacket(false, normalized), player);
            teleportToStoryStart(player);
        });
    }

    private static void teleportToStoryStart(ServerPlayer player) {
        if (player.getServer() == null) {
            return;
        }
        ServerLevel targetLevel = player.getServer().getLevel(LibraryDestination.DIMENSION);
        if (targetLevel == null) {
            return;
        }
        player.getCapability(BSPlayerStats.CAPABILITY).ifPresent(stats -> stats.hasVisitedLibrary = true);
        player.stopRiding();
        player.teleportTo(
                targetLevel,
                LibraryDestination.STORY_START_X,
                LibraryDestination.STORY_START_Y,
                LibraryDestination.STORY_START_Z,
                player.getYRot(),
                player.getXRot()
        );
        player.setDeltaMovement(Vec3.ZERO);
        player.fallDistance = 0.0F;
    }
}
