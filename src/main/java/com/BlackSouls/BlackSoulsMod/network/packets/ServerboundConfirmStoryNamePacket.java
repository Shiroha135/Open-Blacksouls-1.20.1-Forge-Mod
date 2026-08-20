package com.BlackSouls.BlackSoulsMod.network.packets;

import com.BlackSouls.BlackSoulsMod.capability.BSPlayerStats;
import com.BlackSouls.BlackSoulsMod.network.NetworkHandler;
import com.BlackSouls.BlackSoulsMod.util.HokoniwaDestination;
import com.BlackSouls.BlackSoulsMod.util.KnightStartingKit;
import com.BlackSouls.BlackSoulsMod.util.StoryNameData;
import com.BlackSouls.BlackSoulsMod.util.skill.SkillSeekAdvice;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public final class ServerboundConfirmStoryNamePacket {
    private static final int MAX_NAME_BYTES = 64;
    private final String storyName;
    private final boolean useGameName;

    public ServerboundConfirmStoryNamePacket(String storyName) {
        this.storyName = storyName;
        this.useGameName = false;
    }

    public static ServerboundConfirmStoryNamePacket useGameName() {
        return new ServerboundConfirmStoryNamePacket("", true);
    }

    private ServerboundConfirmStoryNamePacket(String storyName, boolean useGameName) {
        this.storyName = storyName;
        this.useGameName = useGameName;
    }

    public ServerboundConfirmStoryNamePacket(FriendlyByteBuf buffer) {
        this.useGameName = buffer.readBoolean();
        this.storyName = buffer.readUtf(MAX_NAME_BYTES);
    }

    public void toBytes(FriendlyByteBuf buffer) {
        buffer.writeBoolean(useGameName);
        buffer.writeUtf(storyName, MAX_NAME_BYTES);
    }

    public void handle(Supplier<NetworkEvent.Context> supplier) {
        PacketHandlers.handleServer(supplier, context -> {
            ServerPlayer player = context.getSender();
            if (player == null || !StoryNameData.hasStarted(player) || StoryNameData.isConfirmed(player)) {
                return;
            }
            String normalized = useGameName
                    ? StoryNameData.normalizeGameName(player.getGameProfile().getName())
                    : StoryNameData.normalize(storyName);
            if (normalized.isEmpty()) {
                NetworkHandler.sendToPlayer(new ClientboundStoryNamePacket(true, ""), player);
                return;
            }
            if (useGameName) {
                StoryNameData.confirmGameName(player);
            } else {
                StoryNameData.confirm(player, normalized);
            }
            KnightStartingKit.grant(player);
            SkillSeekAdvice.resetVisibility(player);
            player.setHealth(player.getMaxHealth());
            NetworkHandler.sendToPlayer(new ClientboundStoryNamePacket(false, normalized), player);
            teleportToStoryStart(player);
        });
    }

    private static void teleportToStoryStart(ServerPlayer player) {
        if (player.getServer() == null) {
            return;
        }
        ServerLevel targetLevel = player.getServer().getLevel(HokoniwaDestination.DIMENSION);
        if (targetLevel == null) {
            return;
        }
        player.getCapability(BSPlayerStats.CAPABILITY).ifPresent(stats -> stats.hasVisitedLibrary = true);
        player.stopRiding();
        player.teleportTo(
                targetLevel,
                HokoniwaDestination.STORY_START_X,
                HokoniwaDestination.STORY_START_Y,
                HokoniwaDestination.STORY_START_Z,
                player.getYRot(),
                player.getXRot()
        );
        player.setDeltaMovement(Vec3.ZERO);
        player.fallDistance = 0.0F;
    }
}
