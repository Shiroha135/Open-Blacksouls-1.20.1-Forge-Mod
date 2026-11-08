package yuki135.blacksouls.story;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import yuki135.blacksouls.capability.BSPlayerStats;
import yuki135.blacksouls.capability.BSWorldData;
import yuki135.blacksouls.capability.BonfireEntry;
import yuki135.blacksouls.handler.BonfireStateHandler;
import yuki135.blacksouls.handler.StatEventHandler;
import yuki135.blacksouls.util.BonfireMetadata;
import yuki135.blacksouls.util.HokoniwaDestination;

public final class OriginalOpeningBonfireFlow {
    public static final BlockPos FALLING_BONFIRE = new BlockPos(59, -59, 158);
    public static final BlockPos FALLING_LANDING = new BlockPos(59, -60, 157);
    public static final int FALLING_HIDDEN = 1;
    public static final int LIBRARY_VISITED = 2;
    public static final int FALLING_REVEALED = 3;

    private OriginalOpeningBonfireFlow() {
    }

    public static boolean prepareFallingRoom(ServerPlayer player) {
        var stats = stats(player);
        if (stats == null) return false;
        if (stats.openingBonfireFlowPhase == 0) {
            stats.openingBonfireFlowPhase = FALLING_HIDDEN;
            stats.openingLibraryBonfireActivated = false;
            stats.hasVisitedLibrary = false;
        }
        StatEventHandler.syncToClient(player);
        return true;
    }

    public static boolean arriveAtFallingRoom(ServerPlayer player) {
        if (!HokoniwaDestination.isHokoniwa(player.level().dimension())
                || !player.level().getBlockState(FALLING_BONFIRE).is(BlockTags.CAMPFIRES)
                || !prepareFallingRoom(player)) return false;
        BonfireStateHandler.light(player.level(), FALLING_BONFIRE);
        player.setRespawnPosition(player.level().dimension(), FALLING_LANDING, 0, true, false);
        StatEventHandler.syncToClient(player);
        refreshBlock(player);
        return true;
    }

    public static boolean isFallingBonfire(ResourceKey<Level> dimension, BlockPos pos) {
        return HokoniwaDestination.isHokoniwa(dimension) && FALLING_BONFIRE.equals(pos);
    }

    public static boolean isHidden(Player player, BlockPos pos) {
        if (player == null || !isFallingBonfire(player.level().dimension(), pos)) return false;
        var stats = stats(player);
        return stats != null && stats.openingBonfireFlowPhase >= FALLING_HIDDEN
                && stats.openingBonfireFlowPhase < FALLING_REVEALED;
    }

    public static boolean ownsProgress(Player player) {
        var stats = stats(player);
        return stats != null && stats.openingBonfireFlowPhase > 0;
    }

    public static void visitLibrary(ServerPlayer player) {
        var stats = stats(player);
        if (stats == null) return;
        stats.hasVisitedLibrary = true;
        if (stats.openingBonfireFlowPhase == FALLING_HIDDEN) stats.openingBonfireFlowPhase = LIBRARY_VISITED;
        StatEventHandler.syncToClient(player);
    }

    public static void activateLibraryBonfire(ServerPlayer player, BlockPos pos) {
        if (!HokoniwaDestination.isHokoniwa(player.level().dimension())
                || !HokoniwaDestination.FIRST_BONFIRE_POS.equals(pos)) return;
        var stats = stats(player);
        if (stats == null || stats.openingBonfireFlowPhase == 0) return;
        stats.hasVisitedLibrary = true;
        stats.openingLibraryBonfireActivated = true;
        if (stats.openingBonfireFlowPhase < LIBRARY_VISITED) stats.openingBonfireFlowPhase = LIBRARY_VISITED;
        StatEventHandler.syncToClient(player);
    }

    public static boolean canUseLibraryMenu(ServerPlayer player, BlockPos pos) {
        if (!HokoniwaDestination.FIRST_BONFIRE_POS.equals(pos)) return true;
        var stats = stats(player);
        return stats == null || stats.openingBonfireFlowPhase == 0 || stats.openingLibraryBonfireActivated;
    }

    public static List<BonfireEntry> bonfires(ServerPlayer player) {
        var result = new ArrayList<BonfireEntry>(BSWorldData.get(player.server.overworld()).activatedBonfires);
        var stats = stats(player);
        if (stats != null && stats.openingBonfireFlowPhase > 0) {
            result.removeIf(entry -> isFallingBonfire(entry.pos.dimension(), entry.pos.pos()));
            var metadata = BonfireMetadata.fallingRoom();
            result.add(0, new BonfireEntry(GlobalPos.of(HokoniwaDestination.DIMENSION, FALLING_BONFIRE),
                    metadata.name(), metadata.description()));
        }
        return List.copyOf(result);
    }

    public static boolean canTeleport(ServerPlayer player, ResourceKey<Level> dimension, BlockPos pos) {
        if (!isFallingBonfire(dimension, pos)) {
            return BSWorldData.get(player.server.overworld()).activatedBonfires.stream()
                    .anyMatch(entry -> entry.pos.equals(GlobalPos.of(dimension, pos)));
        }
        var stats = stats(player);
        if (stats == null || stats.openingBonfireFlowPhase == 0) {
            return BSWorldData.get(player.server.overworld()).activatedBonfires.stream()
                    .anyMatch(entry -> entry.pos.equals(GlobalPos.of(dimension, pos)));
        }
        if (stats.openingBonfireFlowPhase >= FALLING_REVEALED) return true;
        return stats.openingBonfireFlowPhase == LIBRARY_VISITED && stats.openingLibraryBonfireActivated
                && HokoniwaDestination.isHokoniwa(player.level().dimension())
                && player.distanceToSqr(HokoniwaDestination.FIRST_BONFIRE_POS.getX() + .5D,
                HokoniwaDestination.FIRST_BONFIRE_POS.getY() + .5D,
                HokoniwaDestination.FIRST_BONFIRE_POS.getZ() + .5D) <= 64D;
    }

    public static void returnedByBonfire(ServerPlayer player, ResourceKey<Level> dimension, BlockPos pos) {
        if (!isFallingBonfire(dimension, pos) || !player.level().dimension().equals(dimension)) return;
        var stats = stats(player);
        if (stats == null || stats.openingBonfireFlowPhase != LIBRARY_VISITED
                || !stats.openingLibraryBonfireActivated) return;
        stats.openingBonfireFlowPhase = FALLING_REVEALED;
        BonfireStateHandler.light(player.level(), FALLING_BONFIRE);
        StatEventHandler.syncToClient(player);
        refreshBlock(player);
    }

    public static void refreshBlock(ServerPlayer player) {
        if (!HokoniwaDestination.isHokoniwa(player.level().dimension())
                || !player.level().hasChunkAt(FALLING_BONFIRE)) return;
        var state = isHidden(player, FALLING_BONFIRE) ? Blocks.AIR.defaultBlockState()
                : player.level().getBlockState(FALLING_BONFIRE);
        player.connection.send(new ClientboundBlockUpdatePacket(FALLING_BONFIRE, state));
        if (!isHidden(player, FALLING_BONFIRE)) {
            var entity = player.level().getBlockEntity(FALLING_BONFIRE);
            if (entity != null && entity.getUpdatePacket() != null) player.connection.send(entity.getUpdatePacket());
        }
    }

    private static BSPlayerStats stats(Player player) {
        return player.getCapability(BSPlayerStats.CAPABILITY).resolve().orElse(null);
    }
}
