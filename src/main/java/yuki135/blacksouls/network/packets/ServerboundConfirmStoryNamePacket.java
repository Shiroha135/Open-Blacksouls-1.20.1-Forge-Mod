package yuki135.blacksouls.network.packets;

import yuki135.blacksouls.capability.BSPlayerStats;
import yuki135.blacksouls.network.NetworkHandler;
import yuki135.blacksouls.story.OriginalOpeningService;
import yuki135.blacksouls.util.StoryNameData;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
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
        PacketHandlers.handleOpeningServer(supplier, context -> {
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
            var stats = player.getCapability(BSPlayerStats.CAPABILITY).orElse(null);
            if (stats == null || stats.openingProfile().hasRejectedData() || stats.openingProfile().previewActive()) return;
            if (stats.openingProfile().readState() == yuki135.blacksouls.story.OriginalOpeningProfile.ReadState.REAL
                    && stats.openingProfile().realStage().orElse(null) != yuki135.blacksouls.story.OriginalOpeningProfile.Stage.NAME_PENDING) return;
            boolean legacyCompletion = yuki135.blacksouls.story.OriginalLegacyNameCompletion.eligible(player);
            if (legacyCompletion && !yuki135.blacksouls.story.OriginalLegacyNameCompletion.reserve(player)) {
                NetworkHandler.sendToPlayer(new ClientboundStoryNamePacket(true, ""), player);
                return;
            }
            if (useGameName) {
                StoryNameData.confirmGameName(player);
            } else {
                StoryNameData.confirm(player, normalized);
            }
            if (OriginalOpeningService.managed(player)) OriginalOpeningService.nameConfirmed(player);
            else if (legacyCompletion) yuki135.blacksouls.story.OriginalLegacyNameCompletion.confirmed(player);
            NetworkHandler.sendToPlayer(new ClientboundStoryNamePacket(false, normalized), player);

        });
    }

}
